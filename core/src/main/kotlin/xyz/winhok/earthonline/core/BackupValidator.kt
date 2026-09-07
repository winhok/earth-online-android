package xyz.winhok.earthonline.core

import java.time.LocalDate

/** Validate the entire untrusted backup BEFORE starting the replacement transaction. */
object BackupValidator {
    private fun validId(id: String) = id.length in 1..80 && id.all { it.isLetterOrDigit() || it in "-_:" }
    private fun unique(ids: List<String>) = ids.size == ids.toSet().size
    fun validate(world: World) {
        QuestRules.validatePlayer(world.player)
        requireRule(world.player.onboarded, RuleError.INVALID_BACKUP)
        requireRule(world.quests.size <= QuestRules.MAX_QUESTS && world.goals.size <= QuestRules.MAX_GOALS &&
            world.completions.size <= QuestRules.MAX_COMPLETIONS && world.events.size <= QuestRules.MAX_EVENTS,
            RuleError.LIMIT)
        requireRule(unique(world.quests.map { it.id }) && unique(world.goals.map { it.id }) &&
            unique(world.completions.map { it.id }) && unique(world.events.map { it.id }), RuleError.INVALID_BACKUP)
        val goalIds = world.goals.map { it.id }.toSet()
        val quests = world.quests.associateBy { it.id }
        world.goals.forEach {
            requireRule(validId(it.id) && it.title.trim().length in 1..120 && it.description.length <= 4_000 &&
                it.createdAt >= 0, RuleError.INVALID_BACKUP)
        }
        world.quests.forEach {
            QuestRules.validate(QuestDraft(it.id, it.title, it.description, it.kind, it.difficulty,
                it.skill, it.priority, it.estimatedMinutes, it.dueDay, it.goalId))
            requireRule(validId(it.id) && (it.goalId == null || it.goalId in goalIds) &&
                it.postponeCount in 0..1_000_000 && it.createdAt >= 0 && it.updatedAt >= it.createdAt &&
                (it.snoozedUntilDay == null || it.snoozedUntilDay in QuestRules.MIN_DAY..QuestRules.MAX_DAY),
                RuleError.INVALID_BACKUP)
        }
        world.completions.forEach {
            val quest = quests[it.questId] ?: throw RuleViolation(RuleError.INVALID_BACKUP)
            requireRule(it.kind == quest.kind && it.id == "${it.questId}:${it.occurrence}" &&
                it.xp in if (it.kind == QuestKind.BOSS) setOf(35, 50, 75) else setOf(10, 25, 50),
                RuleError.INVALID_BACKUP)
            requireRule(it.title.length in 1..120 && it.completedAt >= 0 &&
                (it.revokedAt == null || it.revokedAt >= 0) &&
                it.completedDay in QuestRules.MIN_DAY..QuestRules.MAX_DAY, RuleError.INVALID_BACKUP)
            requireRule(if (it.kind == QuestKind.DAILY)
                it.occurrence == LocalDate.ofEpochDay(it.completedDay).toString()
                else it.occurrence == "once", RuleError.INVALID_BACKUP)
        }
        world.events.forEach {
            requireRule(validId(it.id) && it.text.length in 1..4_000 && it.createdAt >= 0 &&
                it.xp in -75..75 && (it.questId == null || it.questId in quests), RuleError.INVALID_BACKUP)
        }
    }
}
