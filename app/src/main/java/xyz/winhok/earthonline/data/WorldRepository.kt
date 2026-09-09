package xyz.winhok.earthonline.data

import androidx.room.withTransaction
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.winhok.earthonline.core.*

data class CompletionResult(val completion: Completion, val changed: Boolean)

/** Room is the single source of truth, including preferences and the reward ledger. */
class WorldRepository(private val db: EarthDatabase, private val clock: Clock = Clock.systemUTC()) {
    private val dao = db.dao()
    private fun id(): String = UUID.randomUUID().toString()
    private fun ensure(ok: Boolean, error: RuleError) { if (!ok) throw RuleViolation(error) }

    fun observe(): Flow<World> = db.invalidationTracker.createFlow(
        "player", "quests", "goals", "completions", "journal", emitInitialState = true,
    ).map { snapshot() }

    suspend fun snapshot(): World = db.withTransaction { readSnapshot() }
    private suspend fun readSnapshot() = World(
        player = dao.player()?.value ?: Player(), goals = dao.goals().map { it.value },
        quests = dao.quests().map { it.value }, completions = dao.completions().map { it.value },
        events = dao.events().map { it.value },
    )
    suspend fun snapshotBackup(): BackupSnapshot = db.withTransaction { readBackupSnapshot() }
    private suspend fun readBackupSnapshot() = BackupSnapshot(
        world = readSnapshot(),
        narrativePreference = dao.narrativePreference()
            ?: NarrativePreferenceEntity(narrativeId = EarthDatabase.EARTH_NATIVE_NARRATIVE_ID),
        contracts = dao.contracts(),
        contractRevisions = dao.contractRevisions(),
        consequences = dao.consequences(),
        consequenceAdjustments = dao.consequenceAdjustments(),
        repaymentAllocations = dao.repaymentAllocations(),
        clockBoundaries = dao.clockBoundaries(),
        recoveryRoutes = dao.recoveryRoutes(),
        recoveryNodes = dao.recoveryNodes(),
    )
    private suspend fun player(): Player = dao.player()?.value ?: throw RuleViolation(RuleError.PROFILE)
    private suspend fun quest(id: String): Quest = dao.quest(id)?.value ?: throw RuleViolation(RuleError.MISSING_QUEST)
    private suspend fun event(kind: EventKind, text: String, questId: String? = null, xp: Int = 0) {
        ensure(dao.eventCount() < QuestRules.MAX_EVENTS, RuleError.LIMIT)
        dao.putEvent(EventEntity(JournalEvent(id(), kind, text, clock.millis(), questId, xp)))
        // Roll back the entire action rather than create a save that can no longer be exported.
        BackupSnapshotBudget.requireFits(readBackupSnapshot())
    }

    suspend fun join(name: String, server: String) = db.withTransaction {
        if (dao.player()?.value?.onboarded == true) return@withTransaction
        val value = Player(name = name.trim(), server = server.trim(), joinedAt = clock.millis(), onboarded = true)
        QuestRules.validatePlayer(value)
        dao.putPlayer(PlayerEntity(value = value))
        dao.putNarrativePreference(
            NarrativePreferenceEntity(narrativeId = EarthDatabase.EARTH_NATIVE_NARRATIVE_ID),
        )
        event(EventKind.JOINED, value.name)
        // No fake completions or hidden seeded tasks. The first quest is the user's own.
    }

    suspend fun updatePlayer(name: String, server: String, theme: ThemeMode, reminders: Boolean, hour: Int) = db.withTransaction {
        val value = player().copy(name = name.trim(), server = server.trim(), theme = theme,
            remindersEnabled = reminders, reminderHour = hour)
        QuestRules.validatePlayer(value)
        dao.putPlayer(PlayerEntity(value = value))
        BackupSnapshotBudget.requireFits(readBackupSnapshot())
    }

    suspend fun saveQuest(draft: QuestDraft): Quest = db.withTransaction {
        player()
        val old = draft.id?.let { quest(it) }
        if (old == null) ensure(dao.questCount() < QuestRules.MAX_QUESTS, RuleError.LIMIT)
        // Snapshot the public nullable property from :core before checking it.
        val goalId = draft.goalId
        if (goalId != null) {
            val goal = dao.goal(goalId)?.value
            ensure(goal != null && (!goal.archived || old?.goalId == goal.id), RuleError.MISSING_GOAL)
        }
        val value = QuestRules.edit(old, draft, id(), maxOf(clock.millis(), old?.createdAt ?: 0),
            old != null && dao.hasHistory(old.id))
        dao.putQuest(QuestEntity(value))
        event(if (old == null) EventKind.CREATED else EventKind.EDITED, value.title, value.id)
        value
    }

    suspend fun complete(questId: String): CompletionResult = db.withTransaction {
        val value = quest(questId)
        val now = clock.millis()
        val day = World(player = player()).dayAt(now)
        val key = QuestRules.completionId(value, day)
        val previous = dao.completion(key)?.value
        val completed = QuestRules.complete(value, previous, now, day)
        if (previous?.revokedAt == null && previous != null) return@withTransaction CompletionResult(previous, false)
        if (previous == null) ensure(dao.completionCount() < QuestRules.MAX_COMPLETIONS, RuleError.LIMIT)
        dao.putCompletion(CompletionEntity(completed))
        event(EventKind.COMPLETED, completed.title, value.id, completed.xp)
        CompletionResult(completed, true)
    }

    /** Uses the exact occurrence ID, so a snackbar after midnight cannot undo another day. */
    suspend fun undo(completionId: String): Boolean = db.withTransaction {
        val old = dao.completion(completionId)?.value ?: return@withTransaction false
        if (old.revokedAt != null) return@withTransaction false
        dao.putCompletion(CompletionEntity(old.copy(revokedAt = clock.millis())))
        event(EventKind.UNDONE, old.title, old.questId, -old.xp)
        true
    }

    suspend fun postpone(questId: String) = db.withTransaction {
        val old = quest(questId)
        ensure(old.state == QuestState.ACTIVE, RuleError.INACTIVE)
        val day = World(player = player()).dayAt(clock.millis())
        ensure(dao.completion(QuestRules.completionId(old, day))?.value?.revokedAt != null ||
            dao.completion(QuestRules.completionId(old, day)) == null, RuleError.NOT_AVAILABLE)
        val until = maxOf(day, old.snoozedUntilDay ?: day, if (old.kind == QuestKind.DAILY) old.dueDay ?: day else day) + 1
        ensure(until <= QuestRules.MAX_DAY && old.postponeCount < 1_000_000, RuleError.LIMIT)
        dao.putQuest(QuestEntity(old.copy(snoozedUntilDay = until, postponeCount = old.postponeCount + 1,
            updatedAt = maxOf(clock.millis(), old.createdAt))))
        event(EventKind.POSTPONED, old.title, old.id)
    }

    suspend fun setQuestState(questId: String, state: QuestState) = db.withTransaction {
        val old = quest(questId)
        val value = old.copy(state = state, updatedAt = maxOf(clock.millis(), old.createdAt),
            postponeCount = if (state == QuestState.ACTIVE) 0 else old.postponeCount,
            snoozedUntilDay = if (state == QuestState.ACTIVE) null else old.snoozedUntilDay)
        dao.putQuest(QuestEntity(value))
        event(when (state) { QuestState.ACTIVE -> EventKind.RESUMED; QuestState.PAUSED -> EventKind.PAUSED;
            QuestState.ARCHIVED -> EventKind.ARCHIVED }, old.title, old.id)
    }

    suspend fun saveGoal(goalId: String?, title: String, description: String) = db.withTransaction {
        player()
        ensure(title.trim().length in 1..120, RuleError.TITLE)
        ensure(description.length <= 4_000, RuleError.DESCRIPTION)
        val old = goalId?.let { dao.goal(it)?.value ?: throw RuleViolation(RuleError.MISSING_GOAL) }
        if (old == null) ensure(dao.goalCount() < QuestRules.MAX_GOALS, RuleError.LIMIT)
        val value = Goal(old?.id ?: id(), title.trim(), description.trim(), old?.archived ?: false,
            old?.createdAt ?: clock.millis())
        dao.putGoal(GoalEntity(value))
        event(if (old == null) EventKind.GOAL_CREATED else EventKind.GOAL_EDITED, value.title)
    }

    suspend fun archiveGoal(goalId: String) = db.withTransaction {
        val old = dao.goal(goalId)?.value ?: throw RuleViolation(RuleError.MISSING_GOAL)
        dao.putGoal(GoalEntity(old.copy(archived = true)))
        event(EventKind.GOAL_ARCHIVED, old.title)
    }

    suspend fun addNote(text: String) = db.withTransaction {
        player()
        ensure(text.trim().length in 1..4_000, RuleError.DESCRIPTION)
        event(EventKind.NOTE, text.trim())
    }

    suspend fun restore(world: World) = restore(BackupSnapshot(world))

    suspend fun restore(snapshot: BackupSnapshot) {
        // Parsing, digest, capacity and every relation are checked before the first mutation.
        BackupSnapshotValidator.validate(snapshot)
        BackupSnapshotBudget.requireFits(snapshot)
        db.withTransaction {
            clearTables()
            val world = snapshot.world
            dao.putPlayer(PlayerEntity(value = world.player.copy(remindersEnabled = false, lastReminderDay = null)))
            dao.putNarrativePreference(snapshot.narrativePreference)
            world.goals.forEach { dao.putGoal(GoalEntity(it)) }
            world.quests.forEach { dao.putQuest(QuestEntity(it)) }
            world.completions.forEach { dao.putCompletion(CompletionEntity(it)) }
            world.events.forEach { dao.putEvent(EventEntity(it)) }
            snapshot.contracts.forEach { dao.putContract(it) }
            snapshot.contractRevisions.forEach { dao.putContractRevision(it) }
            snapshot.consequences.forEach { dao.putConsequence(it) }
            snapshot.consequenceAdjustments.forEach { dao.putConsequenceAdjustment(it) }
            snapshot.repaymentAllocations.forEach { dao.putRepaymentAllocation(it) }
            snapshot.clockBoundaries.forEach { dao.putClockBoundary(it) }
            snapshot.recoveryRoutes.forEach { dao.putRecoveryRoute(it) }
            snapshot.recoveryNodes.forEach { dao.putRecoveryNode(it) }
        }
    }

    suspend fun reset() = db.withTransaction { clearTables() }
    private suspend fun clearTables() {
        dao.clearRecoveryNodes()
        dao.clearRecoveryRoutes()
        dao.clearRepaymentAllocations()
        dao.clearConsequenceAdjustments()
        dao.clearConsequences()
        dao.clearContractRevisions()
        dao.clearContracts()
        dao.clearClockBoundaries()
        dao.clearNarrativePreferences()
        dao.clearEvents()
        dao.clearCompletions()
        dao.clearQuests()
        dao.clearGoals()
        dao.clearPlayer()
    }

    suspend fun claimReminder(day: Long): Boolean = db.withTransaction {
        val value = dao.player()?.value ?: return@withTransaction false
        if (!value.remindersEnabled || value.lastReminderDay == day) return@withTransaction false
        dao.putPlayer(PlayerEntity(value = value.copy(lastReminderDay = day)))
        true
    }
    suspend fun releaseReminder(day: Long) = db.withTransaction {
        val value = dao.player()?.value ?: return@withTransaction
        if (value.lastReminderDay == day) dao.putPlayer(PlayerEntity(value = value.copy(lastReminderDay = null)))
    }
}
