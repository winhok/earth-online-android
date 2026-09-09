package xyz.winhok.earthonline.data

import androidx.room.withTransaction
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.winhok.earthonline.core.*

data class CompletionResult(val completion: Completion, val changed: Boolean, val repaidXp: Long = 0)

/** Room is the single source of truth, including preferences and the reward ledger. */
class WorldRepository(private val db: EarthDatabase, private val clock: Clock = Clock.systemUTC()) {
    private val dao = db.dao()
    private fun id(): String = UUID.randomUUID().toString()
    private fun ensure(ok: Boolean, error: RuleError) { if (!ok) throw RuleViolation(error) }

    fun observeSnapshot(): Flow<BackupSnapshot> = db.invalidationTracker.createFlow(
        "player", "quests", "goals", "completions", "journal", "contracts", "contract_revisions",
        "consequence_events", "consequence_adjustments", "repayment_allocations", "clock_boundaries",
        "recovery_routes", "recovery_nodes", "progress_history", "narrative_preferences",
        "effect_preferences", "presentation_preferences", "settlement_receipts", emitInitialState = true,
    ).map { reconciledSnapshot() }

    fun observe(): Flow<World> = db.invalidationTracker.createFlow(
        "player", "quests", "goals", "completions", "journal", emitInitialState = true,
    ).map { snapshot() }

    fun observeNarrativeSystemId(): Flow<String> = db.invalidationTracker.createFlow(
        "narrative_preferences", emitInitialState = true,
    ).map { requestedNarrativeSystemId() }

    suspend fun requestedNarrativeSystemId(): String = db.withTransaction {
        dao.narrativePreference()?.narrativeId ?: EarthDatabase.EARTH_NATIVE_NARRATIVE_ID
    }

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
        progressHistory = dao.progressHistory() ?: ProgressHistoryEntity(),
        effects = dao.effects() ?: EffectPreferencesEntity(),
        presentationPreferences = dao.presentationPreferences(),
        settlementReceipts = dao.settlementReceipts(),
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
        reconcileInside()
        val value = player().copy(name = name.trim(), server = server.trim(), theme = theme,
            remindersEnabled = reminders, reminderHour = hour)
        QuestRules.validatePlayer(value)
        dao.putPlayer(PlayerEntity(value = value))
        BackupSnapshotBudget.requireFits(readBackupSnapshot())
    }

    suspend fun setNarrativeSystem(systemId: NarrativeSystemId) = db.withTransaction {
        player()
        dao.putNarrativePreference(NarrativePreferenceEntity(narrativeId = systemId.wireId))
        BackupSnapshotBudget.requireFits(readBackupSnapshot())
    }

    /** All financial writes happen under the same Room transaction as quest and audit writes. */
    suspend fun plan(command: DeadlineCommand): PlannedCommand = db.withTransaction {
        reconcileInside()
        PlannedCommand(command, DeadlineEngine.preview(readBackupSnapshot().deadlineState(), command, clock.millis()))
    }

    suspend fun execute(command: DeadlineCommand, accepted: ContractDisclosure? = null): DeadlineOutcome = db.withTransaction {
        player()
        val before = readBackupSnapshot()
        val outcome = DeadlineEngine.execute(before.deadlineState(), command, clock.millis(), accepted, ::id)
        persistDeadline(before, before.withDeadline(outcome.state))
        outcome
    }

    suspend fun reconciledSnapshot(): BackupSnapshot = db.withTransaction { reconcileInside(); readBackupSnapshot() }
    suspend fun reconcile() = db.withTransaction { reconcileInside(); Unit }
    private suspend fun reconcileInside() {
        val before = readBackupSnapshot()
        if (!before.world.player.onboarded) return
        val after = before.withDeadline(DeadlineEngine.reconcile(before.deadlineState(), clock.millis(), ::id))
        if (after != before) persistDeadline(before, after)
    }

    private suspend fun persistDeadline(before: BackupSnapshot, after: BackupSnapshot) {
        BackupSnapshotBudget.requireFits(after)
        BackupSnapshotValidator.validate(after)
        val quests=before.world.quests.associateBy { it.id }
        after.world.quests.filter { quests[it.id]!=it }.forEach { dao.putQuest(QuestEntity(it)) }
        val completions=before.world.completions.associateBy { it.id }
        after.world.completions.filter { completions[it.id]!=it }.forEach { dao.putCompletion(CompletionEntity(it)) }
        val contracts=before.contracts.associateBy { it.id }
        // Close previous promises before activating a replacement protected by a unique index.
        after.contracts.filter { contracts[it.id]!=it }.sortedBy { if(it.status=="ACTIVE") 1 else 0 }.forEach { dao.putContract(it) }
        val revisions=before.contractRevisions.map { it.id }.toSet()
        after.contractRevisions.filterNot { it.id in revisions }.forEach { dao.putContractRevision(it) }
        val consequences=before.consequences.map { it.id }.toSet()
        after.consequences.filterNot { it.id in consequences }.forEach { dao.putConsequence(it) }
        val adjustments=before.consequenceAdjustments.map { it.id }.toSet()
        after.consequenceAdjustments.filterNot { it.id in adjustments }.forEach { dao.putConsequenceAdjustment(it) }
        val allocations=before.repaymentAllocations.map { it.id }.toSet()
        after.repaymentAllocations.filterNot { it.id in allocations }.forEach { dao.putRepaymentAllocation(it) }
        if(before.clockBoundaries!=after.clockBoundaries) { dao.clearClockBoundaries(); after.clockBoundaries.forEach { dao.putClockBoundary(it) } }
        val routes=before.recoveryRoutes.associateBy { it.id }
        after.recoveryRoutes.filter { routes[it.id]!=it }.forEach { dao.putRecoveryRoute(it) }
        if(before.recoveryNodes!=after.recoveryNodes) { dao.clearRecoveryNodes(); after.recoveryNodes.forEach { dao.putRecoveryNode(it) } }
        if(before.progressHistory!=after.progressHistory) dao.putProgressHistory(after.progressHistory)
        val events=before.world.events.map { it.id }.toSet()
        after.world.events.filterNot { it.id in events }.forEach { dao.putEvent(EventEntity(it)) }
    }

    suspend fun saveQuest(draft: QuestDraft, accepted: ContractDisclosure? = null): Quest {
        val newId=accepted?.questId ?: id()
        val result=execute(DeadlineCommand.Save(draft,newId),accepted)
        return result.state.world.quests.first { it.id==(draft.id ?: newId) }
    }
    suspend fun complete(questId: String): CompletionResult {
        val result=execute(DeadlineCommand.Complete(questId))
        return CompletionResult(requireNotNull(result.completion),result.changed,result.repaidXp)
    }
    suspend fun undo(completionId: String, accepted: ContractDisclosure? = null): Boolean {
        val before = snapshot().completions.firstOrNull { it.id==completionId && it.revokedAt==null }
        execute(DeadlineCommand.Undo(completionId),accepted)
        return before!=null
    }
    suspend fun postpone(questId: String, accepted: ContractDisclosure? = null) { execute(DeadlineCommand.Postpone(questId),accepted) }
    suspend fun setQuestState(questId: String, state: QuestState, accepted: ContractDisclosure? = null) { execute(DeadlineCommand.SetState(questId,state),accepted) }

    suspend fun acknowledgeAssessments(ids: List<String>) = db.withTransaction {
        val current = readBackupSnapshot()
        val liabilities = current.consequences.associateBy { it.id }
        ensure(ids.distinct().size == ids.size && ids.all { liabilities[it]?.kind == "OVERDUE" }, RuleError.NOT_AVAILABLE)
        val acknowledgedAt = current.deadlineState().book.effectiveNow(clock.millis())
        val receipts = ids.map { SettlementReceiptEntity(it, acknowledgedAt) }
        BackupSnapshotBudget.requireFits(current.copy(settlementReceipts =
            (current.settlementReceipts + receipts).distinctBy { it.consequenceId }))
        receipts.forEach { dao.putSettlementReceipt(it) }
    }

    suspend fun saveEffects(sound: Boolean, haptics: Boolean, reducedMotion: Boolean) = db.withTransaction {
        player()
        dao.putEffects(EffectPreferencesEntity(sound=sound,haptics=haptics,reducedMotion=reducedMotion))
    }
    suspend fun setPresentationPreference(narrativeId: String, key: String, enabled: Boolean) = db.withTransaction {
        player()
        ensure(key in setOf("intro_seen","story_collapsed") && narrativeId.matches(Regex("[A-Za-z0-9_.:-]{1,120}")),RuleError.INVALID_BACKUP)
        val before=readBackupSnapshot()
        val preference=PresentationPreferenceEntity(narrativeId,key,enabled)
        val after=before.copy(presentationPreferences=before.presentationPreferences.filterNot { it.narrativeId==narrativeId && it.preferenceKey==key }+preference)
        BackupSnapshotValidator.validate(after); BackupSnapshotBudget.requireFits(after)
        dao.putPresentationPreference(preference)
    }

    suspend fun saveGoal(goalId: String?, title: String, description: String) = db.withTransaction {
        reconcileInside()
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
        reconcileInside()
        val old = dao.goal(goalId)?.value ?: throw RuleViolation(RuleError.MISSING_GOAL)
        dao.putGoal(GoalEntity(old.copy(archived = true)))
        event(EventKind.GOAL_ARCHIVED, old.title)
    }

    suspend fun addNote(text: String) = db.withTransaction {
        reconcileInside()
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
            dao.putProgressHistory(snapshot.progressHistory)
            dao.putEffects(snapshot.effects)
            snapshot.presentationPreferences.forEach { dao.putPresentationPreference(it) }
            snapshot.settlementReceipts.forEach { dao.putSettlementReceipt(it) }
        }
    }

    suspend fun reset() = db.withTransaction { clearTables() }
    private suspend fun clearTables() {
        dao.clearSettlementReceipts()
        dao.clearPresentationPreferences()
        dao.clearEffects()
        dao.clearProgressHistory()
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
