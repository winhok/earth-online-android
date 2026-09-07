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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthApp(model: EarthViewModel, state: EarthUiState) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var goalOpen by rememberSaveable { mutableStateOf(false) }
    var goalId by rememberSaveable { mutableStateOf<String?>(null) }
    var noteOpen by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val holder = rememberSaveableStateHolder()
    val labels = listOf("指挥台", "任务", "角色", "日志")
    val icons = listOf(Icons.Default.Explore, Icons.Default.TaskAlt, Icons.Default.PersonOutline, Icons.Default.History)
    val ready = !state.loading && !state.loadError && state.world.player.onboarded
    val create: () -> Unit = { editId = null; editorOpen = true }
    val openGoal: (String?) -> Unit = { goalId = it; goalOpen = true }
    LaunchedEffect(model) {
        model.messages.collect { message ->
            val result = snackbar.showSnackbar(message.text, actionLabel = message.undoId?.let { "撤销" },
                withDismissAction = true, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) message.undoId?.let(model::undo)
        }
    }
    LaunchedEffect(state.loading, state.loadError, state.world.player.onboarded) {
        if (!state.loading && !state.loadError && !state.world.player.onboarded) {
            tab = 0; settings = false; editorOpen = false; detailId = null; goalOpen = false; noteOpen = false
        }
    }
    BackHandler(enabled = ready && tab != 0 && !settings && !editorOpen && !goalOpen && detailId == null && !noteOpen) { tab = 0 }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Scaffold(
            topBar = { if (ready) TopAppBar(title = {
                Column {
                    Text("地球 Online", style = MaterialTheme.typography.titleLarge)
                    Text(state.world.player.server, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }, actions = {
                IconButton(onClick = { settings = true }, enabled = !state.busy) { Icon(Icons.Default.Settings, "设置与存档") }
            }) },
            bottomBar = {
                if (ready && !wide) NavigationBar {
                    labels.forEachIndexed { index, label ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index },
                            icon = { Icon(icons[index], null) }, label = { Text(label) })
                    }
                }
            },
            floatingActionButton = {
                if (ready && (tab == 0 || tab == 1)) ExtendedFloatingActionButton(onClick = create,
                    modifier = Modifier.testTag("create-quest").semantics { contentDescription = "接取任务" },
                    icon = { Icon(Icons.Default.Add, null) }, text = { Text("接取任务") })
                if (ready && tab == 3) FloatingActionButton(onClick = { noteOpen = true }) { Icon(Icons.Default.EditNote, "写冒险手记") }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (ready && wide) NavigationRail {
                    labels.forEachIndexed { index, label ->
                        NavigationRailItem(selected = tab == index, onClick = { tab = index },
                            icon = { Icon(icons[index], null) }, label = { Text(label) })
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 880.dp).fillMaxSize()) {
                        when {
                            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            state.loadError -> Column(Modifier.padding(24.dp)) {
                                EmptyState("无法读取存档", "请重试。应用不会为了启动而清空数据库。", "重新读取", model::retryLoad)
                            }
                            !state.world.player.onboarded -> JoinScreen(state.busy, model::join)
                            else -> holder.SaveableStateProvider(tab) {
                                when (tab) {
                                    0 -> DashboardScreen(state, create, { detailId = it }, model::complete, { tab = 1 }, openGoal)
                                    1 -> QuestsScreen(state, create, { detailId = it }, model::complete, openGoal, model::archiveGoal)
                                    2 -> CharacterScreen(state)
                                    3 -> JournalScreen(state, { noteOpen = true })
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
    val detailedQuest = state.world.quests.firstOrNull { it.id == detailId }
    if (detailedQuest != null && ready) QuestDetail(detailedQuest, state,
        onDismiss = { detailId = null }, onEdit = { editId = detailedQuest.id; detailId = null; editorOpen = true },
        onComplete = { model.complete(detailedQuest.id) }, onUndo = model::undo,
        onPostpone = { model.postpone(detailedQuest.id) },
        onState = { model.setQuestState(detailedQuest.id, it); detailId = null },
    )
}
