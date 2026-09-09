package xyz.winhok.earthonline.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

@Composable
fun QuestsScreen(state: EarthUiState, create: () -> Unit, open: (String) -> Unit, complete: (String) -> Unit,
                 editGoal: (String?) -> Unit, archiveGoal: (String) -> Unit) {
    val presenter = LocalNarrativePresenter.current
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var goalFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveId by rememberSaveable { mutableStateOf<String?>(null) }
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle(presenter.text(ScreenSemantic.QUESTS_TITLE), presenter.text(ScreenSemantic.QUESTS_BODY)) }
        item { OutlinedTextField(query, { query = it.take(120) }, label = { Text(presenter.text(FieldSemantic.QUEST_SEARCH)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEachIndexed { index, key -> FilterChip(selected = index == filter,
                    onClick = { filter = index }, label = { Text(presenter.text(key)) }) }
            }
        }
        if (filter != 6 && world.goals.isNotEmpty()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = goalFilter == null, onClick = { goalFilter = null },
                    label = { Text(presenter.text(ScreenSemantic.ALL_GOALS)) })
                world.goals.forEach { g -> FilterChip(selected = goalFilter == g.id, onClick = { goalFilter = g.id },
                    label = { Text(presenter.text(ScreenSemantic.GOAL_TITLE, semanticArguments {
                        put(SemanticParameters.GOAL_TITLE, GoalTitlePresentation(OpaqueText(g.title.take(20)), g.archived, compact = true))
                    })) }) }
            }
        }
        if (filter == 6) {
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
        } else {
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
