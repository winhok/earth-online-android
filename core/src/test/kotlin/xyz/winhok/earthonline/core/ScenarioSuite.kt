package xyz.winhok.earthonline.core

import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone

/** Runs identically under JUnit and the offline kotlinc runner; assertions call production rules. */
object ScenarioSuite {
    private val today = LocalDate.of(2026, 9, 8).toEpochDay()
    private val now = Instant.parse("2026-09-08T10:00:00Z").toEpochMilli()
    private fun quest(id: String = "quest-1", kind: QuestKind = QuestKind.SIDE) = Quest(
        id, "写出一个真实动作", kind = kind, createdAt = now - 1_000, updatedAt = now,
    )
    private fun player() = Player("测试玩家", "测试服", "Asia/Shanghai", now - 1_000, true)
    private fun world(quests: List<Quest> = listOf(quest()), completions: List<Completion> = emptyList()) =
        World(player = player(), quests = quests, completions = completions)
    private fun expect(error: RuleError, block: () -> Unit) {
        try { block(); error("Expected $error") }
        catch (failure: RuleViolation) { check(failure.reason == error) { "Expected $error, got ${failure.reason}" } }
    }
    fun run(report: (String) -> Unit = {}): Int {
        var count = 0
        fun scenario(name: String, block: () -> Unit) { block(); count++; report("PASS $name") }
        scenario("level boundaries") {
            mapOf(0L to 1, 99L to 1, 100L to 2, 299L to 2, 300L to 3).forEach { (xp, level) ->
                check(ProgressRules.fromXp(xp).level == level)
            }
        }
        scenario("level arithmetic property over 12001 inputs") {
            for (xp in 0L..120_000L step 10) {
                val p = ProgressRules.fromXp(xp)
                check(p.intoLevel >= 0 && p.intoLevel < p.needed)
                check(ProgressRules.threshold(p.level) + p.intoLevel == xp)
                check(p.fraction in 0f..1f)
            }
        }
        scenario("negative and extreme XP cannot overflow") {
            check(ProgressRules.fromXp(-1).xp == 0L)
            check(ProgressRules.fromXp(Long.MAX_VALUE).xp == 3_750_000L)
        }
        scenario("difficulty reward does not depend on inflated duration") {
            val q = quest().copy(difficulty = Difficulty.EASY, estimatedMinutes = 480)
            check(QuestRules.reward(q) == 10)
            check(QuestRules.reward(q.copy(kind = QuestKind.BOSS)) == 35)
        }
        scenario("blank title rejected") { expect(RuleError.TITLE) { QuestRules.validate(QuestDraft(title = "  ")) } }
        scenario("oversized title rejected") { expect(RuleError.TITLE) { QuestRules.validate(QuestDraft(title = "a".repeat(121))) } }
        scenario("duration range rejected") {
            listOf(0, -1, 481).forEach { value -> expect(RuleError.MINUTES) {
                QuestRules.validate(QuestDraft(title = "ok", estimatedMinutes = value))
            } }
        }
        scenario("priority range rejected") { expect(RuleError.PRIORITY) { QuestRules.validate(QuestDraft(title = "ok", priority = 4)) } }
        scenario("invalid dates rejected") { expect(RuleError.DATE) { QuestRules.validate(QuestDraft(title = "ok", dueDay = Long.MAX_VALUE)) } }
        scenario("timezone is validated") { expect(RuleError.PROFILE) { QuestRules.validatePlayer(player().copy(zoneId = "Invalid/Nowhere")) } }
        scenario("once occurrence is stable across dates") {
            check(QuestRules.completionId(quest(), today) == QuestRules.completionId(quest(), today + 1))
        }
        scenario("daily occurrence changes at calendar boundary") {
            val q = quest(kind = QuestKind.DAILY)
            check(QuestRules.completionId(q, today) != QuestRules.completionId(q, today + 1))
            check(QuestRules.occurrence(q, today) == "2026-09-08")
        }
        scenario("double completion is idempotent") {
            val q = quest()
            val first = QuestRules.complete(q, null, now, today)
            check(QuestRules.complete(q, first, now + 1_000, today) == first)
        }
        scenario("undo redo preserves reward and skill snapshots") {
            val q = quest().copy(difficulty = Difficulty.EASY, skill = Skill.KNOWLEDGE)
            val first = QuestRules.complete(q, null, now, today)
            val undone = first.copy(revokedAt = now + 1)
            val edited = q.copy(difficulty = Difficulty.HARD, skill = Skill.VITALITY, title = "改名")
            val redone = QuestRules.complete(edited, undone, now + 2, today)
            check(redone.xp == 10 && redone.skill == Skill.KNOWLEDGE && redone.title == first.title)
            check(redone.revokedAt == null)
        }
        scenario("daily future start is unavailable") {
            val q = quest(kind = QuestKind.DAILY).copy(dueDay = today + 1)
            expect(RuleError.NOT_AVAILABLE) { QuestRules.complete(q, null, now, today) }
        }
        scenario("paused and archived tasks cannot earn XP") {
            listOf(QuestState.PAUSED, QuestState.ARCHIVED).forEach { state ->
                expect(RuleError.INACTIVE) { QuestRules.complete(quest().copy(state = state), null, now, today) }
            }
        }
        scenario("snooze expires on the specified date") {
            val q = quest().copy(snoozedUntilDay = today + 1)
            check(!QuestRules.available(q, today)); check(QuestRules.available(q, today + 1))
        }
        scenario("single tasks may finish before deadline") {
            check(QuestRules.complete(quest().copy(dueDay = today + 10), null, now, today).xp == 25)
        }
        scenario("edit keeps identity state and creation time") {
            val old = quest().copy(state = QuestState.PAUSED, postponeCount = 4)
            val edited = QuestRules.edit(old, QuestDraft(title = "  更小的动作  "), "unused", now + 1, false)
            check(edited.id == old.id && edited.title == "更小的动作" && edited.createdAt == old.createdAt)
            check(edited.state == QuestState.PAUSED && edited.postponeCount == 4)
        }
        scenario("history locks repeat type") {
            expect(RuleError.REPEAT_LOCKED) { QuestRules.edit(quest(), QuestDraft(title = "ok", kind = QuestKind.DAILY), "new", now, true) }
        }
        scenario("before first completion repeat type is editable") {
            check(QuestRules.edit(quest(), QuestDraft(title = "ok", kind = QuestKind.DAILY), "new", now, false).kind == QuestKind.DAILY)
        }
        scenario("revoked records contribute zero XP") {
            val c = QuestRules.complete(quest(), null, now, today)
            check(ProgressRules.total(listOf(c.copy(revokedAt = now))).xp == 0L)
        }
        scenario("skill totals are ledger based") {
            val q = quest().copy(skill = Skill.CREATION)
            val c = QuestRules.complete(q, null, now, today)
            check(ProgressRules.total(listOf(c), Skill.CREATION).xp == 25L)
            check(ProgressRules.total(listOf(c), Skill.KNOWLEDGE).xp == 0L)
        }
        scenario("current streak deduplicates dates and tolerates unfinished today") {
            val q = quest(kind = QuestKind.DAILY)
            val c = (1L..3L).map { delta -> QuestRules.complete(q, null, now, today - delta) }
            check(ProgressRules.streak(c + c.first(), today) == 3)
            check(ProgressRules.streak(c, today + 1) == 0)
        }
        scenario("historical streak achievement survives a rest day") {
            val q = quest(kind = QuestKind.DAILY)
            val c = (10L..12L).map { delta -> QuestRules.complete(q, null, now, today - delta) }
            check(ProgressRules.bestStreak(c) == 3)
            check("three_days" in ProgressRules.achievements(world(listOf(q), c)))
        }
        scenario("recommendation excludes insufficient time and energy") {
            val q = quest().copy(estimatedMinutes = 90, difficulty = Difficulty.HARD)
            check(NextActionRules.rank(world(listOf(q)), today, 25, 3).isEmpty())
            check(NextActionRules.rank(world(listOf(q)), today, 90, 1).isEmpty())
        }
        scenario("recommendation excludes completed and inactive quests") {
            val q = quest()
            val w = world(listOf(q, quest("paused").copy(state = QuestState.PAUSED)),
                listOf(QuestRules.complete(q, null, now, today)))
            check(NextActionRules.rank(w, today, 90, 3).isEmpty())
        }
        scenario("overdue reason and priority are explainable") {
            val urgent = quest("urgent").copy(priority = 1, dueDay = today - 1)
            val other = quest("other").copy(priority = 3)
            val r = NextActionRules.rank(world(listOf(other, urgent)), today, 90, 3).first()
            check(r.quest.id == "urgent" && RecommendationReason.OVERDUE in r.reasons)
        }
        scenario("mainline bonus is removed for archived goals") {
            val q = quest().copy(goalId = "goal")
            val g = Goal("goal", "主线", createdAt = now)
            val a = NextActionRules.rank(world(listOf(q)).copy(goals = listOf(g)), today, 25, 2).first()
            val b = NextActionRules.rank(world(listOf(q)).copy(goals = listOf(g.copy(archived = true))), today, 25, 2).first()
            check(a.score == b.score + 20)
        }
        scenario("recommendations have deterministic tie breaks") {
            val qs = listOf(quest("c"), quest("a"), quest("b"))
            check(NextActionRules.rank(world(qs), today, 25, 2).map { it.quest.id } == listOf("a", "b", "c"))
        }
        scenario("fixed save timezone handles midnight exactly") {
            val w = world()
            check(w.dayAt(Instant.parse("2026-09-07T15:59:59Z").toEpochMilli()) == today - 1)
            check(w.dayAt(Instant.parse("2026-09-07T16:00:00Z").toEpochMilli()) == today)
        }
        scenario("travelling does not change save timezone") {
            val old = TimeZone.getDefault()
            try {
                val w = world()
                val before = w.dayAt(now)
                TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
                check(w.dayAt(now) == before)
            } finally { TimeZone.setDefault(old) }
        }
        scenario("valid snapshot passes backup validation") {
            val q = quest()
            BackupValidator.validate(world(listOf(q), listOf(QuestRules.complete(q, null, now, today))))
        }
        scenario("duplicate backup identity is rejected") {
            expect(RuleError.INVALID_BACKUP) { BackupValidator.validate(world(listOf(quest(), quest()))) }
        }
        scenario("orphan mainline is rejected") {
            expect(RuleError.INVALID_BACKUP) { BackupValidator.validate(world(listOf(quest().copy(goalId = "missing")))) }
        }
        scenario("orphan reward row is rejected") {
            val c = QuestRules.complete(quest(), null, now, today)
            expect(RuleError.INVALID_BACKUP) { BackupValidator.validate(world(emptyList(), listOf(c))) }
        }
        scenario("reward outside known schedule is rejected") {
            val c = QuestRules.complete(quest(), null, now, today).copy(xp = 999)
            expect(RuleError.INVALID_BACKUP) { BackupValidator.validate(world(completions = listOf(c))) }
        }
        scenario("daily occurrence date mismatch is rejected") {
            val q = quest(kind = QuestKind.DAILY)
            val c = QuestRules.complete(q, null, now, today).copy(completedDay = today - 1)
            expect(RuleError.INVALID_BACKUP) { BackupValidator.validate(world(listOf(q), listOf(c))) }
        }
        scenario("changed task kind with old ledger is rejected") {
            val q = quest()
            val c = QuestRules.complete(q, null, now, today)
            expect(RuleError.INVALID_BACKUP) { BackupValidator.validate(world(listOf(q.copy(kind = QuestKind.DAILY)), listOf(c))) }
        }
        scenario("archiving retains historical XP") {
            val q = quest()
            val c = QuestRules.complete(q, null, now, today)
            val w = world(listOf(q.copy(state = QuestState.ARCHIVED)), listOf(c))
            BackupValidator.validate(w)
            check(ProgressRules.total(w.completions).xp == 25L)
        }
        scenario("undo after midnight targets the old occurrence ID") {
            val q = quest(kind = QuestKind.DAILY)
            val before = QuestRules.complete(q, null, now, today)
            val next = QuestRules.complete(q, null, now + 86_400_000, today + 1)
            val rows = listOf(before.copy(revokedAt = now + 86_400_001), next)
            check(QuestRules.activeCompletion(q, rows, today + 1) == next)
            check(ProgressRules.total(rows).xp == 25L)
        }
        report("$count scenarios passed")
        return count
    }
}

fun main() { ScenarioSuite.run(::println) }
