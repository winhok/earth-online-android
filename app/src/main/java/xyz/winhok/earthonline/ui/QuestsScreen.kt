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
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var goalFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveId by rememberSaveable { mutableStateOf<String?>(null) }
    val world = state.world
    val done = remember(world.completions) { world.completions.filter { it.revokedAt == null }.associateBy { it.id } }
    val filters = listOf("进行中", "日常", "已完成", "暂停", "归档", "待重审", "主线")
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
        item { SectionTitle("任务日志", "主线指引方向，支线负责行动。") }
        item { OutlinedTextField(query, { query = it.take(120) }, label = { Text("搜索任务或主线") },
            modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEachIndexed { index, label -> FilterChip(selected = index == filter,
                    onClick = { filter = index }, label = { Text(label) }) }
            }
        }
        if (filter != 6 && world.goals.isNotEmpty()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = goalFilter == null, onClick = { goalFilter = null }, label = { Text("所有主线") })
                world.goals.forEach { g -> FilterChip(selected = goalFilter == g.id, onClick = { goalFilter = g.id },
                    label = { Text(g.title.take(20) + if (g.archived) "（归档）" else "") }) }
            }
        }
        if (filter == 6) {
            item { Button(onClick = { editGoal(null) }) { Text("开启主线") } }
            val goals = world.goals.filter { it.title.contains(query, true) || it.description.contains(query, true) }
            if (goals.isEmpty()) item { EmptyState("还没有主线", "例如：完成一个独立产品、准备一次旅行。不要把它写成今天必须完成的大任务。") }
            items(goals, key = { it.id }) { g ->
                val children = world.quests.filter { it.goalId == g.id && it.kind != QuestKind.DAILY &&
                    (it.state != QuestState.ARCHIVED || QuestRules.completionId(it, state.day) in done) }
                val count = children.count { QuestRules.completionId(it, state.day) in done }
                val dailyIds = world.quests.filter { it.goalId == g.id && it.kind == QuestKind.DAILY }.map { it.id }.toSet()
                val contributions = done.values.count { it.questId in dailyIds }
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(g.title + if (g.archived) " · 已归档" else "", style = MaterialTheme.typography.titleLarge)
                        if (g.description.isNotBlank()) Text(g.description, style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(progress = { if (children.isEmpty()) 0f else count.toFloat() / children.size },
                            modifier = Modifier.fillMaxWidth())
                        Text("一次性任务 $count / ${children.size} · 日常贡献 $contributions 次", style = MaterialTheme.typography.labelMedium)
                        Row {
                            TextButton(onClick = { editGoal(g.id) }, enabled = !state.busy) { Text("编辑主线") }
                            TextButton(onClick = { goalFilter = g.id; filter = 0 }) { Text("所属任务") }
                            if (!g.archived) TextButton(onClick = { archiveId = g.id }, enabled = !state.busy) { Text("归档") }
                        }
                    }
                }
            }
        } else {
            if (quests.isEmpty()) item { EmptyState("这里暂时没有任务", "可以切换筛选、清空搜索，或接取新的任务。", "接取任务", create) }
            items(quests, key = { it.id }) { q -> QuestCard(q, state.day,
                QuestRules.completionId(q, state.day) in done, state.busy, { open(q.id) }, { complete(q.id) }) }
        }
    }
    if (archiveId != null) AlertDialog(onDismissRequest = { archiveId = null }, title = { Text("归档这条主线？") },
        text = { Text("历史记录和所属任务都会保留。已归档主线不再增加推荐权重。") },
        confirmButton = { TextButton(onClick = { archiveId?.let(archiveGoal); archiveId = null }, enabled = !state.busy) { Text("归档") } },
        dismissButton = { TextButton(onClick = { archiveId = null }) { Text("取消") } })
}
