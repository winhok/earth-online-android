package xyz.winhok.earthonline.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import xyz.winhok.earthonline.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthApp(model: EarthViewModel, state: EarthUiState) {
    val presenter = LocalNarrativePresenter.current
    val latestPresenter by rememberUpdatedState(presenter)
    val latestState by rememberUpdatedState(state)
    val view = LocalView.current
    val pendingCommand by model.pendingCommand.collectAsState()
    var ledgerOpen by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var goalOpen by rememberSaveable { mutableStateOf(false) }
    var goalId by rememberSaveable { mutableStateOf<String?>(null) }
    var noteOpen by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    var feedback by remember { mutableStateOf<SettlementFeedback?>(null) }
    val holder = rememberSaveableStateHolder()
    val ready = !state.loading && !state.loadError && state.world.player.onboarded
    val create: () -> Unit = { editId = null; editorOpen = true }
    val openGoal: (String?) -> Unit = { goalId = it; goalOpen = true }
    LaunchedEffect(model) {
        model.messages.collectLatest { message ->
            if (message.closeEditor) editorOpen = false
            feedback = message.feedback
            message.feedback?.let { feedback ->
                if (latestState.effects.haptics) view.performHapticFeedback(when (feedback) {
                    SettlementFeedback.LEVEL_UP, SettlementFeedback.RECOVERED -> HapticFeedbackConstants.LONG_PRESS
                    SettlementFeedback.LEVEL_DOWN, SettlementFeedback.UNDONE -> if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
                    else -> HapticFeedbackConstants.VIRTUAL_KEY
                })
                if (latestState.effects.sound) view.playSoundEffect(SoundEffectConstants.CLICK)
            }

            val result = snackbar.showSnackbar(
                latestPresenter.present(message.request).text,
                actionLabel = message.undoId?.let { latestPresenter.text(ActionSemantic.UNDO_COMPLETION, semanticArguments {
                    put(SemanticParameters.UNDO_COMPLETION_LABEL, UndoCompletionLabel.SHORT)
                }) },
                withDismissAction = true, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) message.undoId?.let(model::undo)
        }
    }
    LaunchedEffect(state.loading, state.loadError, state.world.player.onboarded) {
        if (!state.loading && !state.loadError && !state.world.player.onboarded) {
            tab = 0; settings = false; editorOpen = false; detailId = null; goalOpen = false; noteOpen = false; ledgerOpen = false
        }
    }
    BackHandler(enabled = ready && tab != 0 && !settings && !editorOpen && !goalOpen && detailId == null && !noteOpen && !ledgerOpen) { tab = 0 }
    CompositionLocalProvider(LocalEditorSnackbar provides snackbar, LocalEarthState provides state,
        LocalQuestPostpone provides model::postpone, LocalSettlementFeedback provides feedback) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Scaffold(
            topBar = { if (ready) TopAppBar(title = {
                Column {
                    Text(presenter.text(ScreenSemantic.APP_NAME), style = MaterialTheme.typography.titleLarge)
                    Text(state.world.player.server, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }, actions = {
                IconButton(onClick = { ledgerOpen = true }, enabled = !state.busy, modifier = Modifier.testTag("ledger-navigation")) {
                    Icon(Icons.Default.AccountBalanceWallet, presenter.text(ActionSemantic.VIEW_DEBT))
                }
                IconButton(onClick = { settings = true }, enabled = !state.busy) {
                    Icon(Icons.Default.Settings, presenter.text(ActionSemantic.OPEN_SETTINGS))
                }
            }) },
            bottomBar = {
                if (ready && !wide) PrimaryNavigation(tab, expanded = false) { tab = it }
            },
            floatingActionButton = {
                if (ready && tab == 0) ExtendedFloatingActionButton(onClick = create,
                    modifier = Modifier.testTag("create-quest").semantics {
                        contentDescription = presenter.text(ActionSemantic.CREATE_QUEST)
                    },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(presenter.text(ActionSemantic.CREATE_QUEST)) })
                if (ready && tab == 3) FloatingActionButton(onClick = { noteOpen = true }) {
                    Icon(Icons.Default.EditNote, presenter.text(ActionSemantic.WRITE_NOTE))
                }
            },
            snackbarHost = { SettlementSnackbarHost(snackbar) },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (ready && wide) PrimaryNavigation(tab, expanded = true) { tab = it }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 1200.dp).fillMaxSize()) {
                        when {
                            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.semantics {
                                    contentDescription = presenter.text(StateSemantic.LOADING)
                                })
                            }
                            state.loadError -> Column(Modifier.padding(24.dp)) {
                                EmptyState(
                                    presenter.text(ScreenSemantic.LOAD_ERROR_TITLE),
                                    presenter.text(ScreenSemantic.LOAD_ERROR_BODY),
                                    presenter.text(ActionSemantic.RETRY_LOAD),
                                    model::retryLoad,
                                )
                            }
                            !state.world.player.onboarded -> JoinScreen(state.busy, model::join)
                            else -> holder.SaveableStateProvider(tab) {
                                when (tab) {
                                    0 -> DashboardScreen(state, create, { detailId = it }, model::complete, { tab = 1 }, openGoal, { ledgerOpen = true })
                                    1 -> QuestsScreen(
                                        state,
                                        create,
                                        { detailId = it },
                                        model::complete,
                                        openGoal,
                                        model::archiveGoal,
                                        model::switchNarrative,
                                        { ledgerOpen = true },
                                    )
                                    2 -> CharacterScreen(state, model, { ledgerOpen = true })
                                    3 -> JournalScreen(state, { noteOpen = true }, { ledgerOpen = true })
                                }
                            }
                        }
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
    if (settings && ready) SettingsScreen(model, state) { settings = false }
    if (editorOpen && ready) QuestEditor(
        quest = state.world.quests.firstOrNull { it.id == editId }, goals = state.world.goals,
        hasHistory = state.world.completions.any { it.questId == editId }, busy = state.busy,
        onDismiss = { editorOpen = false }, onSave = { model.saveQuest(it) { editorOpen = false } },
    )
    if (goalOpen && ready) GoalEditor(state.world.goals.firstOrNull { it.id == goalId }, state.busy,
        { goalOpen = false }) { title, description -> model.saveGoal(goalId, title, description) { goalOpen = false } }
    if (noteOpen && ready) NoteEditor(state.busy, { noteOpen = false }) { text -> model.addNote(text) { noteOpen = false } }
    if (ledgerOpen && ready) ContractLedgerScreen(state, model, { ledgerOpen = false },
        { ledgerOpen = false; detailId = it }, { ledgerOpen = false; create() })
    val detailedQuest = state.world.quests.firstOrNull { it.id == detailId }
    if (detailedQuest != null && ready) QuestDetail(detailedQuest, state,
        onDismiss = { detailId = null }, onEdit = { editId = detailedQuest.id; detailId = null; editorOpen = true },
        onComplete = { model.complete(detailedQuest.id) }, onUndo = model::undo,
        onPostpone = { model.postpone(detailedQuest.id) },
        onState = { model.setQuestState(detailedQuest.id, it) },
        onCalibrate = { model.calibrate(detailedQuest) },
        onLedger = { detailId = null; ledgerOpen = true },
        onWaive = model::waive,
    )
    if (ready) pendingCommand?.disclosure?.let { disclosure ->
        ContractConfirmation(disclosure, state.busy, model::confirmCommand, model::dismissCommand)
    }
    // The batch is topmost, even when a midnight boundary occurred in an editor.
    if (ready && state.unacknowledged.isNotEmpty()) OverdueBatchDialog(state, model::acknowledgeAssessments)
    }
}

@Composable
fun PrimaryNavigation(
    selectedIndex: Int,
    expanded: Boolean,
    onSelect: (Int) -> Unit,
) {
    val presenter = LocalNarrativePresenter.current
    val presentations = DestinationSemantic.entries.map { presenter.present(it) }
    val showEveryLabel = androidx.compose.ui.platform.LocalDensity.current.fontScale <= 1.5f
    if (expanded) {
        NavigationRail {
            presentations.forEachIndexed { index, presentation ->
                NavigationRailItem(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    icon = { Icon(presentation.iconRole.navigationIcon(), null) },
                    label = { Text(presentation.text) },
                )
            }
        }
    } else {
        NavigationBar {
            presentations.forEachIndexed { index, presentation ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    icon = { Icon(presentation.iconRole.navigationIcon(), null) },
                    label = { Text(presentation.text) },
                    alwaysShowLabel = showEveryLabel,
                )
            }
        }
    }
}

private fun IconRole.navigationIcon(): ImageVector = when (this) {
    IconRole.QUEST -> Icons.Default.TaskAlt
    IconRole.CHARACTER -> Icons.Default.PersonOutline
    IconRole.JOURNAL -> Icons.Default.History
    else -> Icons.Default.Explore
}
