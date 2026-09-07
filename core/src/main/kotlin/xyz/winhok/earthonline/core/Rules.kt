package xyz.winhok.earthonline.core

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.sqrt

object QuestRules {
    const val MAX_QUESTS = 10_000
    const val MAX_COMPLETIONS = 50_000
    const val MAX_EVENTS = 100_000
    const val MAX_GOALS = 200
    const val MIN_DAY = -25_567L // 1900-01-01
    const val MAX_DAY = 84_370L  // 2200-12-31; generous but bounded import/input range

    fun validate(draft: QuestDraft) {
        requireRule(draft.title.trim().length in 1..120, RuleError.TITLE)
        requireRule(draft.description.length <= 4_000, RuleError.DESCRIPTION)
        requireRule(draft.estimatedMinutes in 1..480, RuleError.MINUTES)
        requireRule(draft.priority in 1..3, RuleError.PRIORITY)
        requireRule(draft.dueDay == null || draft.dueDay in MIN_DAY..MAX_DAY, RuleError.DATE)
    }

    fun validatePlayer(player: Player) {
        requireRule(player.name.trim().length in 1..32 && player.server.trim().length in 1..40, RuleError.PROFILE)
        requireRule(player.reminderHour in 0..23 && player.joinedAt >= 0, RuleError.PROFILE)
        try { ZoneId.of(player.zoneId) } catch (_: Exception) { throw RuleViolation(RuleError.PROFILE) }
    }

    fun occurrence(quest: Quest, day: Long): String =
        if (quest.kind == QuestKind.DAILY) LocalDate.ofEpochDay(day).toString() else "once"

    fun completionId(quest: Quest, day: Long) = "${quest.id}:${occurrence(quest, day)}"
    fun reward(quest: Quest) = quest.difficulty.xp + if (quest.kind == QuestKind.BOSS) 25 else 0

    fun activeCompletion(quest: Quest, completions: Collection<Completion>, day: Long): Completion? {
        val key = completionId(quest, day)
        return completions.firstOrNull { it.id == key && it.revokedAt == null }
    }

    fun available(quest: Quest, day: Long): Boolean =
        quest.state == QuestState.ACTIVE &&
            (quest.snoozedUntilDay == null || quest.snoozedUntilDay <= day) &&
            (quest.kind != QuestKind.DAILY || quest.dueDay == null || quest.dueDay <= day)

    /** Must run inside the repository transaction together with the unique-key write. */
    fun complete(quest: Quest, previous: Completion?, now: Long, day: Long): Completion {
        requireRule(quest.state == QuestState.ACTIVE, RuleError.INACTIVE)
        requireRule(available(quest, day), RuleError.NOT_AVAILABLE)
        val id = completionId(quest, day)
        requireRule(previous == null || previous.id == id, RuleError.INVALID_BACKUP)
        if (previous != null) {
            if (previous.revokedAt == null) return previous // Idempotent double tap.
            // A revoked occurrence retains its original reward and skill snapshot.
            return previous.copy(completedAt = now, completedDay = day, revokedAt = null)
        }
        return Completion(id, quest.id, occurrence(quest, day), quest.title,
            quest.kind, quest.skill, reward(quest), now, day)
    }

    fun edit(old: Quest?, draft: QuestDraft, newId: String, now: Long, hasHistory: Boolean): Quest {
        validate(draft)
        requireRule(old == null || !hasHistory || old.kind == draft.kind, RuleError.REPEAT_LOCKED)
        return Quest(
            id = old?.id ?: newId, title = draft.title.trim(), description = draft.description.trim(),
            kind = draft.kind, difficulty = draft.difficulty, skill = draft.skill,
            priority = draft.priority, estimatedMinutes = draft.estimatedMinutes, dueDay = draft.dueDay,
            goalId = draft.goalId, state = old?.state ?: QuestState.ACTIVE,
            snoozedUntilDay = old?.snoozedUntilDay, postponeCount = old?.postponeCount ?: 0,
            createdAt = old?.createdAt ?: now, updatedAt = now,
        )
    }
}

data class Progress(val level: Int, val xp: Long, val intoLevel: Long, val needed: Long) {
    val fraction: Float get() = (intoLevel.toDouble() / needed).coerceIn(0.0, 1.0).toFloat()
}

object ProgressRules {
    // Level 1 starts at 0; level 2 at 100; level 3 at 300; no arbitrary "real ability" scores.
    fun threshold(level: Int): Long = 50L * (level - 1).coerceAtLeast(0) * level
    fun fromXp(value: Long): Progress {
        // A valid world is bounded at 50,000 completions * 75 XP.
        val xp = value.coerceIn(0, 3_750_000)
        var level = ((1.0 + sqrt(1.0 + 0.08 * xp)) / 2).toInt().coerceAtLeast(1)
        while (threshold(level + 1) <= xp) level++
        while (threshold(level) > xp) level--
        return Progress(level, xp, xp - threshold(level), 100L * level)
    }
    fun total(completions: Collection<Completion>, skill: Skill? = null): Progress = fromXp(
        completions.asSequence().filter { it.revokedAt == null && (skill == null || it.skill == skill) }
            .sumOf { it.xp.toLong() }
    )
    fun streak(completions: Collection<Completion>, today: Long): Int {
        val days = completions.filter { it.revokedAt == null }.map { it.completedDay }.toHashSet()
        var day = if (today in days) today else today - 1
        var count = 0
        while (day in days) { count++; day-- }
        return count
    }
    fun bestStreak(completions: Collection<Completion>): Int {
        val days = completions.filter { it.revokedAt == null }.map { it.completedDay }.distinct().sorted()
        var previous: Long? = null
        var current = 0
        var best = 0
        for (day in days) {
            current = if (previous != null && day == previous + 1) current + 1 else 1
            best = maxOf(best, current)
            previous = day
        }
        return best
    }
    fun achievements(world: World): Set<String> {
        val done = world.completions.filter { it.revokedAt == null }
        val longestStreak = bestStreak(done)
        return buildSet {
            if (done.isNotEmpty()) add("first_step")
            if (done.size >= 10) add("ten_quests")
            if (done.size >= 100) add("hundred_quests")
            if (longestStreak >= 3) add("three_days")
            if (longestStreak >= 7) add("seven_days")
            if (done.any { it.kind == QuestKind.BOSS }) add("boss_clear")
            if (done.map { it.skill }.distinct().size == Skill.entries.size) add("all_rounder")
        }
    }
}

enum class RecommendationReason { OVERDUE, DUE_TODAY, HIGH_PRIORITY, GOAL, FITS_TIME, FITS_ENERGY }
data class Recommendation(val quest: Quest, val score: Int, val reasons: List<RecommendationReason>)

object NextActionRules {
    fun rank(world: World, today: Long, minutes: Int, energy: Int): List<Recommendation> {
        val completedIds = world.completions.filter { it.revokedAt == null }.map { it.id }.toHashSet()
        val goals = world.goals.filterNot { it.archived }.map { it.id }.toHashSet()
        return world.quests.asSequence().filter {
            QuestRules.available(it, today) && QuestRules.completionId(it, today) !in completedIds &&
                it.estimatedMinutes <= minutes && it.difficulty.energy <= energy
        }.map { q ->
            val reasons = mutableListOf(RecommendationReason.FITS_TIME, RecommendationReason.FITS_ENERGY)
            var score = q.priority * 30
            if (q.kind != QuestKind.DAILY && q.dueDay != null) {
                val distance = q.dueDay - today
                when {
                    distance < 0 -> { score += 110; reasons.add(0, RecommendationReason.OVERDUE) }
                    distance == 0L -> { score += 90; reasons.add(0, RecommendationReason.DUE_TODAY) }
                    distance <= 3 -> score += 40
                }
            }
            if (q.priority == 3) reasons.add(0, RecommendationReason.HIGH_PRIORITY)
            if (q.goalId in goals) { score += 20; reasons.add(0, RecommendationReason.GOAL) }
            if (q.kind == QuestKind.DAILY) score += 5
            Recommendation(q, score, reasons)
        }.sortedWith(compareByDescending<Recommendation> { it.score }
            .thenBy { it.quest.createdAt }.thenBy { it.quest.id }).toList()
    }
}
