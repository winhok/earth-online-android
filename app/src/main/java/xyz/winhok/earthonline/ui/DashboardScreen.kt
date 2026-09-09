package xyz.winhok.earthonline.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

@Composable
fun JoinScreen(busy: Boolean, join: (String, String) -> Unit) {
    val presenter = LocalNarrativePresenter.current
    var name by rememberSaveable { mutableStateOf("") }
    var server by rememberSaveable { mutableStateOf(presenter.text(ScreenSemantic.DEFAULT_SERVER_NAME)) }
    LazyColumn(Modifier.fillMaxSize().imePadding().testTag("join-form"), contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Spacer(Modifier.height(16.dp)); PlanetMark() }
        item {
            Text(presenter.text(ScreenSemantic.APP_NAME), style = MaterialTheme.typography.headlineLarge)
            Text(presenter.text(ScreenSemantic.JOIN_TAGLINE), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp))
        }
        item { OutlinedTextField(name, { name = it.take(32) }, label = { Text(presenter.text(FieldSemantic.PLAYER_NAME)) },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("player-name"), enabled = !busy) }
        item { OutlinedTextField(server, { server = it.take(40) }, label = { Text(presenter.text(FieldSemantic.SERVER_NAME)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy) }
        item { Button(onClick = { join(name, server) }, enabled = !busy && name.isNotBlank() && server.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("join-submit")) {
            Text(presenter.text(ActionSemantic.JOIN))
        } }
        item { Text(presenter.text(ScreenSemantic.JOIN_PRIVACY_NOTICE),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun DashboardScreen(state: EarthUiState, create: () -> Unit, open: (String) -> Unit, complete: (String) -> Unit,
                    allQuests: () -> Unit, goal: (String?) -> Unit, ledger: () -> Unit = {}) {
    val presenter = LocalNarrativePresenter.current
    val cultivation = LocalNarrativeSystemId.current == NarrativeSystemId.CULTIVATION
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var energy by rememberSaveable { mutableIntStateOf(2) }
    val world = state.world
    val day = state.day
    val done = remember(world.completions) { world.completions.filter { it.revokedAt == null }.associateBy { it.id } }
    val active = world.quests.filter { QuestRules.available(it, day) && QuestRules.completionId(it, day) !in done }
    val recommendation = NextActionRules.rank(world, day, minutes, energy).firstOrNull()
    val progress = state.progress.current
    val todayDone = done.values.count { it.completedDay == day }
    val revisit = world.quests.filter { it.state == QuestState.ACTIVE && it.postponeCount >= 3 &&
        QuestRules.completionId(it, day) !in done }.take(3)
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val columns = if (maxWidth >= 700.dp) 2 else 1
    LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize().testTag("dashboard-grid"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(colors = CardDefaults.cardColors(containerColor = if (cultivation) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(presenter.text(ScreenSemantic.PLAYER_WELCOME, semanticArguments {
                                put(SemanticParameters.PLAYER_NAME, OpaqueText(world.player.name))
                            }), style = MaterialTheme.typography.titleLarge)
                            Text(presenter.text(ScreenSemantic.DASHBOARD_DAY_MODE, semanticArguments {
                                put(SemanticParameters.DAY, EpochDay(day))
                            }), style = MaterialTheme.typography.bodySmall)
                        }
                        PlanetMark(Modifier.size(96.dp))
                    }
                    Text(presenter.text(ScreenSemantic.DASHBOARD_LEVEL, semanticArguments {
                        put(SemanticParameters.LEVEL, LevelNumber(progress.level))
                    }), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.secondary)
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                    Text(presenter.text(ScreenSemantic.DASHBOARD_PROGRESS, semanticArguments {
                        put(SemanticParameters.PROGRESS, LevelProgress(progress.intoLevel, progress.needed, todayDone))
                    }),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item { ContractBalance(state, ledger) }
        val unsigned = world.quests.filter { it.kind != QuestKind.DAILY && it.dueDay != null &&
            state.book.latest(it.id) == null && QuestRules.activeCompletion(it, world.completions, day) == null }
        if (unsigned.isNotEmpty()) item {
            EmptyState(presenter.text(ContractSemantic.CALIBRATE), presenter.text(ContractSemantic.UNSIGNED),
                presenter.text(ActionSemantic.VIEW_QUEST), { open(unsigned.first().id) })
        }
        val route = state.book.routes.singleOrNull { it.status == "OPEN" }
        if (route != null) item {
            EmptyState(presenter.text(StateSemantic.RECOVERY_OPEN), presenter.text(ContractSemantic.RECOVERY_BODY),
                presenter.text(ActionSemantic.OPEN_RECOVERY), ledger)
        }
        if (state.book.clocks.any { it.id.startsWith("clock-warning:") }) item { Text(presenter.text(ContractSemantic.CLOCK_WARNING)) }
        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(
            presenter.text(ScreenSemantic.DASHBOARD_PROMPT_TITLE),
            presenter.text(ScreenSemantic.DASHBOARD_PROMPT_BODY),
        ) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 90).forEach { value -> FilterChip(selected = minutes == value,
                        onClick = { minutes = value }, label = { Text(presenter.text(ScreenSemantic.MINUTE_OPTION,
                            semanticArguments { put(SemanticParameters.MINUTES, CountValue(value)) })) }) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ScreenSemantic.ENERGY_LOW, ScreenSemantic.ENERGY_MEDIUM, ScreenSemantic.ENERGY_HIGH)
                        .forEachIndexed { index, key -> FilterChip(selected = energy == index + 1,
                            onClick = { energy = index + 1 }, label = { Text(presenter.text(key)) }) }
                }
            }
        }
        item {
            if (recommendation == null) EmptyState(
                presenter.text(ScreenSemantic.RECOMMENDATION_EMPTY_TITLE),
                presenter.text(ScreenSemantic.RECOMMENDATION_EMPTY_BODY),
                presenter.text(ActionSemantic.CREATE_QUEST),
                create,
            )
            else Card(
                modifier = if (cultivation) Modifier.fillMaxWidth() else Modifier,
                colors = CardDefaults.cardColors(containerColor = if (cultivation) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }),
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(presenter.text(ScreenSemantic.NEXT_QUEST), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(recommendation.quest.title, style = MaterialTheme.typography.headlineSmall)
                    Text(presenter.questMeta(recommendation.quest, day), style = MaterialTheme.typography.bodyMedium)
                    Text(presenter.text(ScreenSemantic.RECOMMENDATION_REASONS, semanticArguments {
                        put(SemanticParameters.RECOMMENDATION_REASONS, RecommendationReasons(recommendation.reasons))
                    }), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { open(recommendation.quest.id) }) { Text(presenter.text(ActionSemantic.VIEW_QUEST)) }
                        TextButton(onClick = { complete(recommendation.quest.id) }, enabled = !state.busy) {
                            Text(presenter.text(ActionSemantic.COMPLETE_QUEST, semanticArguments {
                                put(SemanticParameters.COMPLETE_QUEST_LABEL,
                                    CompleteQuestLabel(CompleteQuestLabelStyle.SHORT))
                            }))
                        }
                    }
                    Text(presenter.text(ScreenSemantic.RECOMMENDATION_NOTICE), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (world.goals.none { !it.archived }) item {
            EmptyState(presenter.text(ScreenSemantic.GOAL_EMPTY_TITLE), presenter.text(ScreenSemantic.GOAL_EMPTY_BODY),
                presenter.text(ActionSemantic.CREATE_GOAL), { goal(null) })
        }
        if (revisit.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(presenter.text(ScreenSemantic.REVISIT_TITLE), presenter.text(ScreenSemantic.REVISIT_BODY)) }
            items(revisit, key = { "review-${it.id}" }) { q -> QuestCard(q, day, false, state.busy, { open(q.id) }, { complete(q.id) }) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(presenter.text(ScreenSemantic.EXECUTABLE_QUESTS, semanticArguments {
            put(SemanticParameters.COUNT, CountValue(active.size))
        }), action = { TextButton(onClick = allQuests) { Text(presenter.text(ActionSemantic.VIEW_ALL)) } }) }
        if (active.isEmpty()) item { EmptyState(
            presenter.text(ScreenSemantic.WORK_DONE_TITLE), presenter.text(ScreenSemantic.WORK_DONE_BODY),
        ) }
        items(active.sortedWith(compareByDescending<Quest> { it.priority }.thenBy { it.dueDay ?: Long.MAX_VALUE }).take(5), key = { it.id }) {
            q -> QuestCard(q, day, false, state.busy, { open(q.id) }, { complete(q.id) })
        }
    }
    }
}
