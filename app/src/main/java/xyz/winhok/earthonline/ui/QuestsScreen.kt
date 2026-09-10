package xyz.winhok.earthonline.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

@Composable
fun QuestsScreen(state: EarthUiState, create: () -> Unit, open: (String) -> Unit, complete: (String) -> Unit,
                 editGoal: (String?) -> Unit, archiveGoal: (String) -> Unit,
                 switchNarrative: (NarrativeSystemId) -> Unit, openLedger: () -> Unit) {
    val presenter = LocalNarrativePresenter.current
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var goalFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveId by rememberSaveable { mutableStateOf<String?>(null) }
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    val world = state.world
    val done = remember(world.completions) { world.completions.filter { it.revokedAt == null }.associateBy { it.id } }
    val filters = listOf(
        StateSemantic.ACTIVE,
        ScreenSemantic.QUEST_KIND_DAILY,
        StateSemantic.COMPLETED,
        StateSemantic.PAUSED,
        StateSemantic.ARCHIVED,
        StateSemantic.REVIEW_REQUIRED,
        ScreenSemantic.FILTER_GOALS,
    )
    val quests = world.quests.filter { q ->
        val completed = QuestRules.completionId(q, state.day) in done
        val statusMatch = when (filter) {
            0 -> q.state == QuestState.ACTIVE && !completed
            1 -> q.kind == QuestKind.DAILY && q.state == QuestState.ACTIVE
            2 -> completed && q.state != QuestState.ARCHIVED
            3 -> q.state == QuestState.PAUSED
            4 -> q.state == QuestState.ARCHIVED
            5 -> q.state == QuestState.ACTIVE && q.postponeCount >= 3 && !completed
            else -> false
        }
        statusMatch && (goalFilter == null || goalFilter == q.goalId) &&
            (q.title.contains(query, true) || q.description.contains(query, true))
    }.sortedWith(compareByDescending<Quest> { it.priority }.thenBy { it.dueDay ?: Long.MAX_VALUE }.thenBy { it.createdAt })
    val cultivationSurface = state.narrativeSystemId == NarrativeSystemId.CULTIVATION &&
        filter == 0 && query.isBlank() && goalFilter == null
    val nextQuest = remember(world, state.day) {
        NextActionRules.rank(world, state.day, 25, 2).firstOrNull()?.quest
    }
    val trackQuests = remember(world, state.day, nextQuest?.id) {
        val completed = world.completions.filter { it.revokedAt == null }.associateBy { it.id }
        world.quests.filter { it.state != QuestState.ARCHIVED && it.id != nextQuest?.id }
            .sortedWith(
                compareBy<Quest> { QuestRules.completionId(it, state.day) !in completed }
                    .thenBy { it.dueDay ?: Long.MAX_VALUE }
                    .thenByDescending { it.priority },
            )
    }
    val listState = rememberLazyListState()
    var narrativeAnchorQuestId by remember { mutableStateOf<String?>(null) }
    DisposableEffect(state.narrativeSystemId) {
        onDispose {
            val questIds = world.quests.mapTo(hashSetOf()) { it.id }
            narrativeAnchorQuestId = listState.layoutInfo.visibleItemsInfo.asReversed()
                .mapNotNull { it.key as? String }
                .firstOrNull { it in questIds }
        }
    }
    LaunchedEffect(state.narrativeSystemId) {
        val questId = narrativeAnchorQuestId ?: return@LaunchedEffect
        val target = if (cultivationSurface) {
            val position = trackQuests.indexOfFirst { it.id == questId }
            if (position < 0) -1 else 2 + (if (toolsExpanded) 2 else 0) + position
        } else {
            val position = quests.indexOfFirst { it.id == questId }
            if (position < 0) -1 else 4 + (if (world.goals.isNotEmpty()) 1 else 0) + position
        }
        if (target >= 0) listState.scrollToItem(target)
        narrativeAnchorQuestId = null
    }
    LazyColumn(Modifier.fillMaxSize().testTag("quests-list"), state = listState,
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (cultivationSurface) {
            item {
                BoxWithConstraints(Modifier.fillMaxWidth().testTag("quest-battle-pass")) {
                    val wide = maxWidth >= 720.dp
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NarrativeQuickSwitch(state.narrativeSystemId, state.busy, switchNarrative)
                        if (wide) {
                            Row(
                                Modifier.fillMaxWidth().testTag("quest-battle-pass-wide"),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(Modifier.weight(.42f)) { CultivationRealmStatus(state) }
                                Box(Modifier.weight(.58f)) {
                                    nextQuest?.let { quest ->
                                        CultivationNextQuest(
                                            quest, state, { open(quest.id) }, { complete(quest.id) }, openLedger,
                                        )
                                    }
                                }
                            }
                        } else {
                            CultivationRealmStatus(state)
                            nextQuest?.let { quest ->
                                CultivationNextQuest(
                                    quest, state, { open(quest.id) }, { complete(quest.id) }, openLedger,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Box(Modifier.testTag("quest-track-heading")) {
                    SectionTitle(
                        presenter.text(ScreenSemantic.QUEST_TRACK_TITLE),
                        presenter.text(ScreenSemantic.QUEST_TRACK_BODY),
                        action = {
                            Row {
                                TextButton(
                                    onClick = { toolsExpanded = !toolsExpanded },
                                    modifier = Modifier.testTag("quest-search-toggle"),
                                ) {
                                    Text(presenter.text(FieldSemantic.QUEST_SEARCH))
                                }
                                TextButton(onClick = create, modifier = Modifier.testTag("quest-create")) {
                                    Text(presenter.text(ActionSemantic.CREATE_QUEST))
                                }
                            }
                        },
                    )
                }
            }
            if (toolsExpanded) {
                item { OutlinedTextField(query, { query = it.take(120) }, label = {
                    Text(presenter.text(FieldSemantic.QUEST_SEARCH))
                }, modifier = Modifier.fillMaxWidth().testTag("quest-search-field"), singleLine = true) }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        filters.forEachIndexed { index, key ->
                            FilterChip(
                                selected = index == filter,
                                onClick = { filter = index },
                                modifier = Modifier.testTag("quest-filter-${key.wireId}"),
                                label = { Text(presenter.text(key)) },
                            )
                        }
                    }
                }
            }
            if (trackQuests.isEmpty()) {
                item { EmptyState(
                    presenter.text(ScreenSemantic.WORK_DONE_TITLE),
                    presenter.text(ScreenSemantic.WORK_DONE_BODY),
                ) }
            } else {
                items(trackQuests, key = { it.id }) { quest ->
                    CultivationTrackQuest(
                        quest,
                        state,
                        QuestRules.completionId(quest, state.day) in done,
                        { open(quest.id) },
                        { complete(quest.id) },
                        openLedger,
                    )
                }
            }
        }
        if (!cultivationSurface) {
            item { NarrativeQuickSwitch(state.narrativeSystemId, state.busy, switchNarrative) }
            item {
                SectionTitle(
                    presenter.text(ScreenSemantic.QUESTS_TITLE),
                    presenter.text(ScreenSemantic.QUESTS_BODY),
                    action = {
                        TextButton(
                            onClick = create,
                            enabled = !state.busy,
                            modifier = Modifier.testTag("quest-create"),
                        ) {
                            Text(presenter.text(ActionSemantic.CREATE_QUEST))
                        }
                    },
                )
            }
            item { OutlinedTextField(query, { query = it.take(120) }, label = { Text(presenter.text(FieldSemantic.QUEST_SEARCH)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEachIndexed { index, key -> FilterChip(selected = index == filter,
                        onClick = { filter = index }, modifier = Modifier.testTag("quest-filter-${key.wireId}"),
                        label = { Text(presenter.text(key)) }) }
                }
            }
        }
        if (!cultivationSurface && filter != 6 && world.goals.isNotEmpty()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = goalFilter == null, onClick = { goalFilter = null },
                    label = { Text(presenter.text(ScreenSemantic.ALL_GOALS)) })
                world.goals.forEach { g -> FilterChip(selected = goalFilter == g.id, onClick = { goalFilter = g.id },
                    label = { Text(presenter.text(ScreenSemantic.GOAL_TITLE, semanticArguments {
                        put(SemanticParameters.GOAL_TITLE, GoalTitlePresentation(OpaqueText(g.title.take(20)), g.archived, compact = true))
                    })) }) }
            }
        }
        if (!cultivationSurface && filter == 6) {
            item { Button(onClick = { editGoal(null) }) { Text(presenter.text(ActionSemantic.CREATE_GOAL)) } }
            val goals = world.goals.filter { it.title.contains(query, true) || it.description.contains(query, true) }
            if (goals.isEmpty()) item { EmptyState(
                presenter.text(ScreenSemantic.NO_GOALS_TITLE), presenter.text(ScreenSemantic.NO_GOALS_BODY),
            ) }
            items(goals, key = { it.id }) { g ->
                val children = world.quests.filter { it.goalId == g.id && it.kind != QuestKind.DAILY &&
                    (it.state != QuestState.ARCHIVED || QuestRules.completionId(it, state.day) in done) }
                val count = children.count { QuestRules.completionId(it, state.day) in done }
                val dailyIds = world.quests.filter { it.goalId == g.id && it.kind == QuestKind.DAILY }.map { it.id }.toSet()
                val contributions = done.values.count { it.questId in dailyIds }
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(presenter.text(ScreenSemantic.GOAL_TITLE, semanticArguments {
                            put(SemanticParameters.GOAL_TITLE, GoalTitlePresentation(OpaqueText(g.title), g.archived))
                        }), style = MaterialTheme.typography.titleLarge)
                        if (g.description.isNotBlank()) Text(g.description, style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(progress = { if (children.isEmpty()) 0f else count.toFloat() / children.size },
                            modifier = Modifier.fillMaxWidth())
                        Text(presenter.text(ScreenSemantic.GOAL_PROGRESS, semanticArguments {
                            put(SemanticParameters.GOAL_PROGRESS, GoalProgress(count, children.size, contributions))
                        }), style = MaterialTheme.typography.labelMedium)
                        Row {
                            TextButton(onClick = { editGoal(g.id) }, enabled = !state.busy) { Text(presenter.text(ActionSemantic.EDIT_GOAL)) }
                            TextButton(onClick = { goalFilter = g.id; filter = 0 }) {
                                Text(presenter.text(ActionSemantic.VIEW_GOAL_QUESTS))
                            }
                            if (!g.archived) TextButton(onClick = { archiveId = g.id }, enabled = !state.busy) {
                                Text(presenter.text(ActionSemantic.ARCHIVE_GOAL))
                            }
                        }
                    }
                }
            }
        } else if (!cultivationSurface) {
            if (quests.isEmpty()) item { EmptyState(
                presenter.text(ScreenSemantic.QUEST_EMPTY_TITLE), presenter.text(ScreenSemantic.QUEST_EMPTY_BODY),
                presenter.text(ActionSemantic.CREATE_QUEST), create,
            ) }
            items(quests, key = { it.id }) { q -> QuestCard(q, state.day,
                QuestRules.completionId(q, state.day) in done, state.busy, { open(q.id) }, { complete(q.id) }) }
        }
    }
    if (archiveId != null) AlertDialog(onDismissRequest = { archiveId = null },
        title = { Text(presenter.text(ScreenSemantic.ARCHIVE_GOAL_TITLE)) },
        text = { Text(presenter.text(ScreenSemantic.ARCHIVE_GOAL_BODY)) },
        confirmButton = { TextButton(onClick = { archiveId?.let(archiveGoal); archiveId = null }, enabled = !state.busy) {
            Text(presenter.text(ActionSemantic.ARCHIVE_GOAL))
        } },
        dismissButton = { TextButton(onClick = { archiveId = null }) { Text(presenter.text(ActionSemantic.CANCEL)) } })
}

@Composable
private fun NarrativeQuickSwitch(
    selected: NarrativeSystemId,
    busy: Boolean,
    switchNarrative: (NarrativeSystemId) -> Unit,
) {
    val presenter = LocalNarrativePresenter.current
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            NarrativeSystemId.EARTH_NATIVE to ScreenSemantic.EARTH_NATIVE_SYSTEM_NAME,
            NarrativeSystemId.CULTIVATION to ScreenSemantic.CULTIVATION_SYSTEM_NAME,
        ).forEach { (systemId, key) ->
            FilterChip(
                selected = selected == systemId,
                onClick = { if (selected != systemId) switchNarrative(systemId) },
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp).testTag("quest-narrative-${systemId.wireId}"),
                label = { Text(presenter.text(key)) },
            )
        }
    }
}

@Composable
private fun CultivationRealmStatus(state: EarthUiState) {
    val presenter = LocalNarrativePresenter.current
    val progress = state.progress.current
    val todayDone = state.world.completions.count { it.revokedAt == null && it.completedDay == state.day }
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("quest-realm-status"),
        shape = CutCornerShape(topEnd = 24.dp, bottomStart = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = .72f)),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    presenter.text(ScreenSemantic.DASHBOARD_LEVEL, semanticArguments {
                        put(SemanticParameters.LEVEL, LevelNumber(progress.level))
                    }),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                PlanetMark(Modifier.size(32.dp))
            }
            LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
            Text(presenter.text(ScreenSemantic.DASHBOARD_PROGRESS, semanticArguments {
                put(SemanticParameters.PROGRESS, LevelProgress(progress.intoLevel, progress.needed, todayDone))
            }), style = MaterialTheme.typography.labelLarge)
            Text(
                presenter.text(ContractSemantic.SUMMARY, semanticArguments {
                    put(ContractParameters.SUMMARY, state.progress)
                }),
                style = MaterialTheme.typography.bodySmall,
                color = if (state.progress.debtXp > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CultivationNextQuest(
    quest: Quest,
    state: EarthUiState,
    open: () -> Unit,
    complete: () -> Unit,
    openLedger: () -> Unit,
) {
    val presenter = LocalNarrativePresenter.current
    val postpone = LocalQuestPostpone.current
    val overdue = state.book.latest(quest.id)?.status == "OVERDUE"
    val completeLabel = presenter.text(ActionSemantic.COMPLETE_QUEST, semanticArguments {
        put(
            SemanticParameters.COMPLETE_QUEST_LABEL,
            CompleteQuestLabel(CompleteQuestLabelStyle.ACCESSIBILITY, OpaqueText(quest.title)),
        )
    })
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("quest-next-contract")
            .semantics {
                customActions = if (!state.busy && QuestRules.available(quest, state.day)) listOf(
                    CustomAccessibilityAction(completeLabel) { complete(); true },
                ) else emptyList()
            }
            .questSwipeGesture(quest.id, !state.busy && QuestRules.available(quest, state.day), complete),
        shape = CutCornerShape(topEnd = 28.dp, bottomStart = 18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    quest.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                FilledTonalIconButton(
                    onClick = complete,
                    enabled = !state.busy && QuestRules.available(quest, state.day),
                    modifier = Modifier.size(56.dp).testTag("quest-next-complete"),
                ) { Icon(Icons.Default.Check, contentDescription = completeLabel) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    presenter.questMeta(quest, state.day),
                    modifier = (if (state.book.latest(quest.id) != null) Modifier.testTag("contract-info") else Modifier)
                        .weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(presenter.text(ScreenSemantic.QUEST_REWARD, semanticArguments {
                    put(SemanticParameters.XP, XpAmount(state.reward(quest)))
                }), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = open,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("quest-next-open"),
                ) { Text(presenter.text(ActionSemantic.VIEW_QUEST)) }
                if (overdue) {
                    TextButton(onClick = openLedger, modifier = Modifier.heightIn(min = 48.dp).testTag("quest-next-recovery")) {
                        Text(presenter.text(ActionSemantic.OPEN_RECOVERY))
                    }
                } else {
                    TextButton(
                        onClick = { postpone(quest.id) },
                        enabled = !state.busy && QuestRules.available(quest, state.day),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(presenter.text(if (state.book.active(quest.id) != null) {
                            ContractSemantic.POSTPONE_SIGNED
                        } else {
                            ActionSemantic.POSTPONE_QUEST
                        }))
                    }
                }
            }
        }
    }
}

@Composable
private fun CultivationTrackQuest(
    quest: Quest,
    state: EarthUiState,
    done: Boolean,
    open: () -> Unit,
    complete: () -> Unit,
    openLedger: () -> Unit,
) {
    val presenter = LocalNarrativePresenter.current
    val overdue = !done && state.book.latest(quest.id)?.status == "OVERDUE"
    val marker = when {
        done -> MaterialTheme.colorScheme.primary
        state.book.latest(quest.id)?.status == "OVERDUE" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    Row(
        modifier = Modifier.fillMaxWidth().testTag("quest-cultivation-track"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.padding(top = 20.dp).size(18.dp),
            shape = MaterialTheme.shapes.small,
            color = marker,
        ) {}
        Box(Modifier.weight(1f)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (overdue) Row(
                    Modifier.fillMaxWidth().testTag("quest-overdue-state"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        presenter.text(StateSemantic.CONTRACT_OVERDUE),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = openLedger, modifier = Modifier.testTag("quest-track-recovery")) {
                        Text(presenter.text(ActionSemantic.OPEN_RECOVERY))
                    }
                }
                QuestCard(quest, state.day, done, state.busy, open, complete, if (overdue) openLedger else null)
            }
        }
    }
}
