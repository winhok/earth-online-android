package xyz.winhok.earthonline.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import xyz.winhok.earthonline.EarthApplication
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.data.*
import xyz.winhok.earthonline.reminder.Reminders

data class EarthUiState(
    val world: World = World(), val now: Long = System.currentTimeMillis(),
    val loading: Boolean = true, val loadError: Boolean = false, val busy: Boolean = false,
    val requestedNarrativeId: String = NarrativeSystemId.EARTH_NATIVE.wireId,
    val narrativeSystemId: NarrativeSystemId = NarrativeSystemId.EARTH_NATIVE,
    val book: DeadlineBook = DeadlineBook(),
    val effects: EffectPreferencesEntity = EffectPreferencesEntity(),
    val presentationPreferences: List<PresentationPreferenceEntity> = emptyList(),
    val acknowledgedAssessments: Set<String> = emptySet(),
) {
    val day: Long get() = world.dayAt(book.effectiveNow(now))
    val progress: EffectiveProgress get() = book.progress(world)
    val deadline: DeadlineState get() = DeadlineState(world, book)
    val unacknowledged: List<Liability> get() = book.liabilities.filter {
        it.kind == "OVERDUE" && it.id !in acknowledgedAssessments
    }
    fun preference(key: String): Boolean = presentationPreferences.firstOrNull {
        it.narrativeId == narrativeSystemId.wireId && it.preferenceKey == key
    }?.enabled ?: false
    fun reward(quest: Quest): Int = QuestRules.activeCompletion(quest, world.completions, day)?.xp
        ?: (if (quest.kind == QuestKind.DAILY) null else book.latest(quest.id))?.currentRewardXp?.toInt() ?: QuestRules.reward(quest)
}
enum class SettlementFeedback { COMPLETE, ACHIEVEMENT, LEVEL_UP, LEVEL_DOWN, REPAID, RECOVERED, UNDONE }
data class UiMessage(
    val request: SemanticRequest,
    val undoId: String? = null,
    val feedback: SettlementFeedback? = null,
    val closeEditor: Boolean = false,
)
private data class LoadedWorld(val snapshot: BackupSnapshot = BackupSnapshot(World()), val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class EarthViewModel(private val app: EarthApplication) : ViewModel() {
    private val repo = app.repository
    private val narrativeRegistry = NarrativeRegistry.builtIns()
    private val retry = MutableStateFlow(0)
    private val working = MutableStateFlow(false)
    private val channel = Channel<UiMessage>(Channel.BUFFERED)
    val messages = channel.receiveAsFlow()
    private val restoreDraft = MutableStateFlow<PendingRestore?>(null)
    val pendingRestore = restoreDraft.asStateFlow()
    private val planned = MutableStateFlow<PlannedCommand?>(null)
    val pendingCommand = planned.asStateFlow()
    private var pendingSuccess: (() -> Unit)? = null
    private val world = retry.flatMapLatest {
        // One Room transaction supplies facts AND presentation preferences. Never combine
        // independent flows into an impossible old-ledger/new-system frame.
        repo.observeSnapshot().map { LoadedWorld(it) }.catch { error ->
            if (error is CancellationException) throw error
            emit(LoadedWorld(failed = true))
        }
    }
    private val ticker = flow {
        while (true) {
            // Foreground midnight reconciliation; correctness also lives in every command.
            try { repo.reconcile() } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { retry.value++ }
            emit(System.currentTimeMillis())
            delay(15_000)
        }
    }
    val state = combine(world, ticker, working) { loaded, now, busy ->
        val snapshot = loaded.snapshot
        val requested = snapshot.narrativePreference.narrativeId
        EarthUiState(
            world = snapshot.world, now = now, loading = false,
            loadError = loaded.failed, busy = busy,
            requestedNarrativeId = requested,
            narrativeSystemId = narrativeRegistry.resolveSystemId(requested),
            book = snapshot.deadlineState().book, effects = snapshot.effects,
            presentationPreferences = snapshot.presentationPreferences,
            acknowledgedAssessments = snapshot.settlementReceipts.map { it.consequenceId }.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EarthUiState())

    fun retryLoad() { retry.value++ }
    fun notify(key: SemanticKey, arguments: SemanticArguments = SemanticArguments.EMPTY) {
        viewModelScope.launch { channel.send(UiMessage(SemanticRequest(key, arguments))) }
    }
    fun switchNarrative(systemId: NarrativeSystemId) = change {
        narrativeRegistry.validate(systemId)
        repo.setNarrativeSystem(systemId)
        configureReminder(repo.snapshot().player.remindersEnabled)
    }
    private fun change(action: suspend () -> Unit) {
        viewModelScope.launch {
            if (working.value) return@launch
            working.value = true
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                channel.send(UiMessage(SemanticRequest(when (error) {
                    is RuleViolation -> error.reason.semantic()
                    is IOException -> NotificationSemantic.FILE_IO_FAILED
                    else -> NotificationSemantic.OPERATION_FAILED
                })))
            } finally { working.value = false }
        }
    }
    fun join(name: String, server: String) = change { repo.join(name, server) }
    /** Preview is not authorization: execute rechecks the disclosure in the write transaction. */
    private fun request(command: DeadlineCommand, success: (() -> Unit)? = null) = change {
        if (planned.value != null) return@change
        val plan = repo.plan(command)
        if (plan.disclosure != null) {
            pendingSuccess = success
            planned.value = plan
        } else perform(plan, success)
    }
    private suspend fun perform(plan: PlannedCommand, success: (() -> Unit)?) {
        val before = repo.reconciledSnapshot().deadlineState()
        try {
            val result = repo.execute(plan.command, plan.disclosure)
            planned.value = null
            pendingSuccess = null
            success?.invoke()
            if (!result.changed) return
            val old = before.book.progress(before.world)
            val after = result.state.book.progress(result.state.world)
            val feedback = when {
                old.debtXp > 0 && after.debtXp == 0L -> SettlementFeedback.RECOVERED
                after.current.level < old.current.level -> SettlementFeedback.LEVEL_DOWN
                after.current.level > old.current.level -> SettlementFeedback.LEVEL_UP
                result.repaidXp > 0 -> SettlementFeedback.REPAID
                plan.command is DeadlineCommand.Undo -> SettlementFeedback.UNDONE
                (ProgressRules.achievements(result.state.world) - ProgressRules.achievements(before.world)).isNotEmpty() -> SettlementFeedback.ACHIEVEMENT
                else -> SettlementFeedback.COMPLETE
            }
            when (plan.command) {
                is DeadlineCommand.Complete -> channel.send(UiMessage(
                    SemanticRequest(ContractSemantic.SETTLEMENT, semanticArguments {
                        put(ContractParameters.SETTLEMENT, SettlementData(result.rewardXp, result.repaidXp,
                            after.current.level, old.current.level,
                            (ProgressRules.achievements(result.state.world) - ProgressRules.achievements(before.world)).size))
                    }), result.completion?.id, feedback,
                ))
                is DeadlineCommand.Undo -> channel.send(UiMessage(SemanticRequest(NotificationSemantic.COMPLETION_UNDONE), feedback = feedback))
                is DeadlineCommand.Postpone -> channel.send(UiMessage(SemanticRequest(NotificationSemantic.QUEST_POSTPONED)))
                is DeadlineCommand.Waive -> channel.send(UiMessage(SemanticRequest(ContractSemantic.SUMMARY,
                    semanticArguments { put(ContractParameters.SUMMARY, after) }), feedback = feedback))
                is DeadlineCommand.Save -> channel.send(UiMessage(SemanticRequest(ActionSemantic.SAVE), closeEditor = true))
                else -> Unit
            }
        } catch (changed: DisclosureRequired) {
            // A day boundary or another writer changed the stakes. Show fresh numbers;
            // never reuse the previously accepted price or silently retry the write.
            pendingSuccess = success
            planned.value = PlannedCommand(plan.command, changed.disclosure)
        }
    }
    fun dismissCommand() { if (!working.value) { planned.value = null; pendingSuccess = null } }
    fun confirmCommand() = change {
        val plan = planned.value ?: return@change
        perform(plan, pendingSuccess)
    }
    fun saveQuest(draft: QuestDraft, success: () -> Unit) = request(
        DeadlineCommand.Save(draft, draft.id ?: UUID.randomUUID().toString()), success)
    fun calibrate(quest: Quest) = request(DeadlineCommand.Save(QuestDraft(quest.id, quest.title,
        quest.description, quest.kind, quest.difficulty, quest.skill, quest.priority,
        quest.estimatedMinutes, quest.dueDay, quest.goalId), quest.id, calibrate = true))
    fun complete(id: String) = request(DeadlineCommand.Complete(id))
    fun undo(id: String) = request(DeadlineCommand.Undo(id))
    fun postpone(id: String) = request(DeadlineCommand.Postpone(id))
    fun setQuestState(id: String, status: QuestState) = request(DeadlineCommand.SetState(id, status))
    fun waive(id: String, reason: ForceMajeureReason) = request(DeadlineCommand.Waive(id, reason))
    fun selectRecovery(ids: List<String>, success: () -> Unit) = request(DeadlineCommand.SelectRecovery(ids), success)
    fun acknowledgeAssessments(ids: Set<String>) = change { repo.acknowledgeAssessments(ids.toList()) }
    fun saveEffects(sound: Boolean, haptics: Boolean, reducedMotion: Boolean) = change {
        repo.saveEffects(sound, haptics, reducedMotion)
    }
    fun setPresentationPreference(key: String, value: Boolean) = change {
        repo.setPresentationPreference(state.value.narrativeSystemId.wireId, key, value)
    }
    fun saveGoal(id: String?, title: String, description: String, success: () -> Unit) = change {
        repo.saveGoal(id, title, description); success()
    }
    fun archiveGoal(id: String) = change { repo.archiveGoal(id) }
    fun addNote(text: String, success: () -> Unit) = change { repo.addNote(text); success() }
    private suspend fun configureReminder(enabled: Boolean) {
        try {
            val systemId = narrativeRegistry.resolveSystemId(repo.requestedNarrativeSystemId())
            Reminders.configure(app, enabled, systemId)
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            channel.send(UiMessage(SemanticRequest(NotificationSemantic.REMINDER_CONFIG_FAILED)))
        }
    }
    fun savePlayer(name: String, server: String, theme: ThemeMode, reminders: Boolean, hour: Int, success: () -> Unit) = change {
        repo.updatePlayer(name, server, theme, reminders, hour)
        configureReminder(reminders)
        success()
    }
    fun export(uri: Uri) = change {
        withContext(Dispatchers.IO) {
            val text = BackupCodec.encode(repo.reconciledSnapshot(), System.currentTimeMillis())
            val output = app.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
            output.use { it.write(text.toByteArray(Charsets.UTF_8)); it.flush() }
        }
        channel.send(UiMessage(SemanticRequest(NotificationSemantic.BACKUP_EXPORTED)))
    }
    fun prepareRestore(uri: Uri) = change {
        restoreDraft.value = withContext(Dispatchers.IO) {
            val stream = app.contentResolver.openInputStream(uri) ?: throw IOException("No input stream")
            val bytes = stream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buffer)
                    if (n == -1) break
                    require(out.size() + n <= BackupCodec.MAX_BYTES) { "Backup exceeds size limit" }
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            }
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            val snapshot = BackupCodec.decodeSnapshot(text)
            PendingRestore(snapshot, RestorePreview.between(repo.snapshotBackup(), snapshot))
        }
    }
    fun dismissRestore() { if (!working.value) restoreDraft.value = null }
    fun confirmRestore(success: () -> Unit) = change {
        val value = restoreDraft.value ?: return@change
        repo.restore(value.snapshot)
        restoreDraft.value = null
        configureReminder(false)
        success()
        channel.send(UiMessage(SemanticRequest(NotificationSemantic.BACKUP_RESTORED)))
    }
    fun reset(success: () -> Unit) = change {
        repo.reset(); configureReminder(false); restoreDraft.value = null; planned.value = null; pendingSuccess = null; success()
    }
    class Factory(private val app: EarthApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(EarthViewModel::class.java))
            return EarthViewModel(app) as T
        }
    }
}
