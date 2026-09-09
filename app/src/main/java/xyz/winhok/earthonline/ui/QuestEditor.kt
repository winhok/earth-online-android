package xyz.winhok.earthonline.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestEditor(quest: Quest?, goals: List<Goal>, hasHistory: Boolean, busy: Boolean,
                onDismiss: () -> Unit, onSave: (QuestDraft) -> Unit) {
    val presenter = LocalNarrativePresenter.current
    var title by rememberSaveable(quest?.id) { mutableStateOf(quest?.title ?: "") }
    var description by rememberSaveable(quest?.id) { mutableStateOf(quest?.description ?: "") }
    var kind by rememberSaveable(quest?.id) { mutableStateOf(quest?.kind ?: QuestKind.SIDE) }
    var difficulty by rememberSaveable(quest?.id) { mutableStateOf(quest?.difficulty ?: Difficulty.NORMAL) }
    var skill by rememberSaveable(quest?.id) { mutableStateOf(quest?.skill ?: Skill.DISCIPLINE) }
    var minutes by rememberSaveable(quest?.id) { mutableStateOf((quest?.estimatedMinutes ?: 25).toString()) }
    var priority by rememberSaveable(quest?.id) { mutableIntStateOf(quest?.priority ?: 2) }
    var dueDay by rememberSaveable(quest?.id) { mutableStateOf(quest?.dueDay) }
    var goalId by rememberSaveable(quest?.id) { mutableStateOf(quest?.goalId) }
    var dateOpen by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val validMinutes = minutes.toIntOrNull()?.let { it in 1..480 } == true
    val valid = title.isNotBlank() && title.length <= 120 && description.length <= 4_000 && validMinutes
    val dirty = title != (quest?.title ?: "") || description != (quest?.description ?: "") ||
        kind != (quest?.kind ?: QuestKind.SIDE) || difficulty != (quest?.difficulty ?: Difficulty.NORMAL) ||
        skill != (quest?.skill ?: Skill.DISCIPLINE) || minutes != (quest?.estimatedMinutes ?: 25).toString() ||
        priority != (quest?.priority ?: 2) || dueDay != quest?.dueDay || goalId != quest?.goalId
    EditorFrame(presenter.text(if (quest == null) ScreenSemantic.QUEST_CREATE_TITLE else ScreenSemantic.QUEST_EDIT_TITLE), busy, valid,
        onDismiss = { if (dirty) discard = true else onDismiss() },
        onSave = { onSave(QuestDraft(quest?.id, title, description, kind, difficulty, skill, priority,
            minutes.toInt(), dueDay, goalId)) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { OutlinedTextField(title, { title = it.take(120) }, label = { Text(presenter.text(FieldSemantic.QUEST_TITLE)) },
                supportingText = { Text(presenter.text(ScreenSemantic.QUEST_TITLE_HELPER, semanticArguments {
                    put(SemanticParameters.TEXT_LENGTH, CountValue(title.length))
                })) },
                modifier = Modifier.fillMaxWidth().testTag("quest-title"), maxLines = 3, enabled = !busy) }
            item {
                Text(presenter.text(FieldSemantic.QUEST_KIND), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuestKind.entries.forEach { value -> FilterChip(selected = kind == value, onClick = { kind = value },
                        enabled = !busy && !hasHistory, label = { Text(presenter.text(value.narrativeKey())) }) }
                }
                Text(presenter.text(if (hasHistory) ScreenSemantic.QUEST_KIND_LOCKED_HELPER else ScreenSemantic.QUEST_KIND_HELPER),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text(presenter.text(FieldSemantic.DIFFICULTY), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { value -> FilterChip(selected = difficulty == value,
                        onClick = { difficulty = value }, enabled = !busy,
                        label = { Text(presenter.text(ScreenSemantic.DIFFICULTY_REWARD, semanticArguments {
                            put(SemanticParameters.DIFFICULTY_REWARD,
                                DifficultyReward(value, value.xp + if (kind == QuestKind.BOSS) 25 else 0))
                        })) }) }
                }
            }
            item {
                Text(presenter.text(FieldSemantic.SKILL), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Skill.entries.forEach { value -> FilterChip(selected = skill == value,
                        onClick = { skill = value }, enabled = !busy, label = { Text(presenter.text(value.narrativeKey())) }) }
                }
            }
            item { OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) },
                label = { Text(presenter.text(FieldSemantic.ESTIMATED_MINUTES)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !validMinutes, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth()) }
            item {
                Text(presenter.text(FieldSemantic.PRIORITY), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ScreenSemantic.PRIORITY_NORMAL, ScreenSemantic.PRIORITY_IMPORTANT, ScreenSemantic.PRIORITY_URGENT)
                        .forEachIndexed { index, key ->
                        FilterChip(selected = priority == index + 1, onClick = { priority = index + 1 }, enabled = !busy,
                            label = { Text(presenter.text(key)) })
                    }
                }
            }
            item {
                Text(presenter.text(if (kind == QuestKind.DAILY) ScreenSemantic.DUE_START else ScreenSemantic.DUE_END),
                    style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { dateOpen = true }, enabled = !busy) { Text(dueDay?.let { day ->
                        presenter.text(ScreenSemantic.SELECTED_DATE, semanticArguments {
                            put(SemanticParameters.DAY, EpochDay(day))
                        })
                    } ?: presenter.text(ActionSemantic.SELECT_DATE)) }
                    if (dueDay != null) TextButton(onClick = { dueDay = null }, enabled = !busy) {
                        Text(presenter.text(ActionSemantic.CLEAR_DATE))
                    }
                }
            }
            item {
                Text(presenter.text(FieldSemantic.GOAL), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = goalId == null, onClick = { goalId = null }, enabled = !busy,
                        label = { Text(presenter.text(ScreenSemantic.INDEPENDENT_QUEST)) })
                    goals.filter { !it.archived || it.id == goalId }.forEach { g -> FilterChip(selected = goalId == g.id,
                        onClick = { goalId = g.id }, enabled = !busy, label = { Text(g.title.take(24)) }) }
                }
            }
            item { OutlinedTextField(description, { description = it.take(4_000) }, label = { Text(presenter.text(FieldSemantic.DESCRIPTION)) },
                supportingText = { Text(presenter.text(ScreenSemantic.DESCRIPTION_COUNT, semanticArguments {
                    put(SemanticParameters.TEXT_LENGTH, CountValue(description.length))
                })) }, minLines = 4, maxLines = 10,
                enabled = !busy, modifier = Modifier.fillMaxWidth()) }
            if (kind != QuestKind.DAILY && dueDay != null) item {
                Text(presenter.text(ContractSemantic.RULES), style = MaterialTheme.typography.bodySmall)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    if (dateOpen) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = dueDay?.times(86_400_000L), yearRange = 1900..2200)
        DatePickerDialog(onDismissRequest = { dateOpen = false }, confirmButton = {
            TextButton(onClick = { dueDay = picker.selectedDateMillis?.div(86_400_000L); dateOpen = false },
                enabled = picker.selectedDateMillis != null) { Text(presenter.text(ActionSemantic.CONFIRM)) }
        }, dismissButton = { TextButton(onClick = { dateOpen = false }) { Text(presenter.text(ActionSemantic.CANCEL)) } }) { DatePicker(state = picker) }
    }
    if (discard) DiscardDialog({ discard = false }, onDismiss)
}

@Composable
fun DiscardDialog(cancel: () -> Unit, discard: () -> Unit) {
    val presenter = LocalNarrativePresenter.current
    AlertDialog(onDismissRequest = cancel, title = { Text(presenter.text(ScreenSemantic.DISCARD_TITLE)) },
        text = { Text(presenter.text(ScreenSemantic.DISCARD_BODY)) },
        confirmButton = { TextButton(onClick = discard) { Text(presenter.text(ActionSemantic.DISCARD)) } },
        dismissButton = { TextButton(onClick = cancel) { Text(presenter.text(ActionSemantic.CONTINUE_EDITING)) } })
}

@Composable
fun GoalEditor(goal: Goal?, busy: Boolean, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val presenter = LocalNarrativePresenter.current
    var title by rememberSaveable(goal?.id) { mutableStateOf(goal?.title ?: "") }
    var description by rememberSaveable(goal?.id) { mutableStateOf(goal?.description ?: "") }
    var discard by rememberSaveable { mutableStateOf(false) }
    val dirty = title != (goal?.title ?: "") || description != (goal?.description ?: "")
    EditorFrame(presenter.text(if (goal == null) ScreenSemantic.GOAL_CREATE_TITLE else ScreenSemantic.GOAL_EDIT_TITLE),
        busy, title.isNotBlank(),
        { if (dirty) discard = true else onDismiss() }, { onSave(title, description) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { Text(presenter.text(ScreenSemantic.GOAL_EDITOR_HELPER),
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { OutlinedTextField(title, { title = it.take(120) }, label = { Text(presenter.text(FieldSemantic.GOAL_TITLE)) },
                modifier = Modifier.fillMaxWidth(), enabled = !busy) }
            item { OutlinedTextField(description, { description = it.take(4_000) }, label = { Text(presenter.text(FieldSemantic.GOAL_DESCRIPTION)) },
                modifier = Modifier.fillMaxWidth(), minLines = 5, maxLines = 10, enabled = !busy) }
        }
    }
    if (discard) DiscardDialog({ discard = false }, onDismiss)
}

@Composable
fun NoteEditor(busy: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val presenter = LocalNarrativePresenter.current
    var text by rememberSaveable { mutableStateOf("") }
    var discard by rememberSaveable { mutableStateOf(false) }
    EditorFrame(presenter.text(ScreenSemantic.NOTE_EDITOR_TITLE), busy, text.isNotBlank(),
        { if (text.isNotBlank()) discard = true else onDismiss() },
        { onSave(text) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(text, { text = it.take(4_000) }, label = { Text(presenter.text(FieldSemantic.NOTE)) },
                supportingText = { Text(presenter.text(ScreenSemantic.NOTE_HELPER, semanticArguments {
                    put(SemanticParameters.TEXT_LENGTH, CountValue(text.length))
                })) },
                modifier = Modifier.fillMaxWidth(), minLines = 8, maxLines = 16, enabled = !busy)
        }
    }
    if (discard) DiscardDialog({ discard = false }, onDismiss)
}

@Composable
fun QuestDetail(quest: Quest, state: EarthUiState, onDismiss: () -> Unit, onEdit: () -> Unit,
                onComplete: () -> Unit, onUndo: (String) -> Unit, onPostpone: () -> Unit, onState: (QuestState) -> Unit,
                onCalibrate: () -> Unit = {}, onLedger: () -> Unit = {},
                onWaive: (String, ForceMajeureReason) -> Unit = { _, _ -> }) {
    val presenter = LocalNarrativePresenter.current
    var archive by rememberSaveable(quest.id) { mutableStateOf(false) }
    var forceOpen by rememberSaveable(quest.id) { mutableStateOf(false) }
    val contract = state.book.latest(quest.id)
    val completion = QuestRules.activeCompletion(quest, state.world.completions, state.day)
    val goal = state.world.goals.firstOrNull { it.id == quest.goalId }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(quest.title, maxLines = 4) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(presenter.questMeta(quest, state.day))
                if (contract != null) {
                    ContractInfo(contract)
                    if (contract.extensionCount >= 3 && contract.status == "ACTIVE") Text(presenter.text(ContractSemantic.REVIEW))
                    TextButton(onClick = onLedger) { Text(presenter.text(ContractSemantic.CONTRACT_HISTORY)) }
                    if (contract.status !in setOf("FULFILLED", "EXEMPTED")) TextButton(onClick = { forceOpen = true },
                        enabled = !state.busy, modifier = Modifier.testTag("waive-contract")) {
                        Text(presenter.text(ActionSemantic.APPLY_FORCE_MAJEURE))
                    }
                } else if (quest.kind != QuestKind.DAILY && quest.dueDay != null && completion == null) {
                    Text(presenter.text(ContractSemantic.UNSIGNED), Modifier.testTag("legacy-unsigned"))
                    if ((quest.dueDay ?: Long.MIN_VALUE) >= state.day) OutlinedButton(onClick = onCalibrate,
                        enabled = !state.busy, modifier = Modifier.testTag("calibrate-contract")) {
                        Text(presenter.text(ContractSemantic.CALIBRATE))
                    }
                    TextButton(onClick = onEdit) { Text(presenter.text(ActionSemantic.RESCHEDULE_CONTRACT)) }
                }
                if (goal != null) Text(presenter.text(ScreenSemantic.QUEST_GOAL, semanticArguments {
                    put(SemanticParameters.TITLE, OpaqueText(goal.title))
                }))
                if (quest.description.isNotBlank()) Text(quest.description)
                Text(presenter.text(ScreenSemantic.QUEST_DETAIL_STATS, semanticArguments {
                    put(SemanticParameters.QUEST_DETAIL,
                        QuestDetailPresentation(completion?.xp ?: state.reward(quest), quest.postponeCount))
                }))
                if (quest.postponeCount >= 3) Text(presenter.text(ScreenSemantic.QUEST_REVIEW_GUIDANCE),
                    color = MaterialTheme.colorScheme.secondary)
                if (completion != null) {
                    Text(presenter.text(ScreenSemantic.QUEST_COMPLETED_AT, semanticArguments {
                        put(SemanticParameters.COMPLETION_MOMENT,
                            CompletionMoment(completion.completedAt, state.world.player.zoneId))
                    }))
                    OutlinedButton(onClick = { onUndo(completion.id) }, enabled = !state.busy) {
                        Text(presenter.text(ActionSemantic.UNDO_COMPLETION, semanticArguments {
                            put(SemanticParameters.UNDO_COMPLETION_LABEL, UndoCompletionLabel.DETAIL)
                        }))
                    }
                } else if (QuestRules.available(quest, state.day)) {
                    Button(onClick = onComplete, enabled = !state.busy) { Text(presenter.text(
                        ActionSemantic.COMPLETE_QUEST,
                        semanticArguments { put(
                            SemanticParameters.COMPLETE_QUEST_LABEL,
                            CompleteQuestLabel(CompleteQuestLabelStyle.CONFIRM),
                        ) },
                    )) }
                }
                OutlinedButton(onClick = onEdit, enabled = !state.busy) { Text(presenter.text(ActionSemantic.EDIT_QUEST)) }
                if (quest.state == QuestState.ACTIVE && completion == null) {
                    TextButton(onClick = onPostpone, enabled = !state.busy) { Text(presenter.text(if (contract?.status == "ACTIVE") ContractSemantic.POSTPONE_SIGNED else ActionSemantic.POSTPONE_QUEST)) }
                    TextButton(onClick = { onState(QuestState.PAUSED) }, enabled = !state.busy) {
                        Text(presenter.text(ActionSemantic.PAUSE_QUEST))
                    }
                }
                if (quest.state != QuestState.ACTIVE || quest.postponeCount > 0 || quest.snoozedUntilDay != null) {
                    TextButton(onClick = { onState(QuestState.ACTIVE) }, enabled = !state.busy) {
                        Text(presenter.text(ActionSemantic.RESUME_QUEST))
                    }
                }
                if (quest.state != QuestState.ARCHIVED) TextButton(onClick = { archive = true }, enabled = !state.busy) {
                    Text(presenter.text(ActionSemantic.ARCHIVE_QUEST, semanticArguments {
                        put(SemanticParameters.ARCHIVE_QUEST_LABEL, ArchiveQuestLabel.DETAIL)
                    }))
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text(presenter.text(ActionSemantic.CLOSE)) } })
    if (forceOpen && contract != null) ForceMajeureDialog(contract, state.busy, { forceOpen = false }) { reason ->
        forceOpen = false; onWaive(contract.id, reason)
    }
    if (archive) AlertDialog(onDismissRequest = { archive = false },
        title = { Text(presenter.text(ScreenSemantic.ARCHIVE_QUEST_TITLE)) },
        text = { Text(presenter.text(ScreenSemantic.ARCHIVE_QUEST_BODY)) },
        confirmButton = { TextButton(onClick = { archive = false; onState(QuestState.ARCHIVED) }, enabled = !state.busy) {
            Text(presenter.text(ActionSemantic.ARCHIVE_QUEST, semanticArguments {
                put(SemanticParameters.ARCHIVE_QUEST_LABEL, ArchiveQuestLabel.CONFIRM)
            }))
        } },
        dismissButton = { TextButton(onClick = { archive = false }) { Text(presenter.text(ActionSemantic.CANCEL)) } })
}
