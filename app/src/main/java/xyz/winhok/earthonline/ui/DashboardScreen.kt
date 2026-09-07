package xyz.winhok.earthonline.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

@Composable
fun JoinScreen(busy: Boolean, join: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var server by rememberSaveable { mutableStateOf("现实服") }
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Spacer(Modifier.height(16.dp)); PlanetMark() }
        item {
            Text("地球 Online", style = MaterialTheme.typography.headlineLarge)
            Text("现实没有重开键，\n但随时可以接取下一项任务。", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp))
        }
        item { OutlinedTextField(name, { name = it.take(32) }, label = { Text("玩家名") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy) }
        item { OutlinedTextField(server, { server = it.take(40) }, label = { Text("服务器名（自己取一个）") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy) }
        item { Button(onClick = { join(name, server) }, enabled = !busy && name.isNotBlank() && server.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("创建本地角色") } }
        item { Text("无需账号 · 无广告 · 无云端上传\n任务保存在本机。卸载前请导出存档；备份文件为明文。\n这里不因拖延扣血，也不评价你的现实价值。",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun DashboardScreen(state: EarthUiState, create: () -> Unit, open: (String) -> Unit, complete: (String) -> Unit,
                    allQuests: () -> Unit, goal: (String?) -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var energy by rememberSaveable { mutableIntStateOf(2) }
    val world = state.world
    val day = state.day
    val done = remember(world.completions) { world.completions.filter { it.revokedAt == null }.associateBy { it.id } }
    val active = world.quests.filter { QuestRules.available(it, day) && QuestRules.completionId(it, day) !in done }
    val recommendation = NextActionRules.rank(world, day, minutes, energy).firstOrNull()
    val progress = ProgressRules.total(world.completions)
    val todayDone = done.values.count { it.completedDay == day }
    val revisit = world.quests.filter { it.state == QuestState.ACTIVE && it.postponeCount >= 3 &&
        QuestRules.completionId(it, day) !in done }.take(3)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${world.player.name}，欢迎上线", style = MaterialTheme.typography.titleLarge)
                            Text("${dateLabel(day)} · 单人冒险模式", style = MaterialTheme.typography.bodySmall)
                        }
                        PlanetMark(Modifier.size(64.dp))
                    }
                    Text("Lv.${progress.level}  现实探索者", style = MaterialTheme.typography.headlineSmall)
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                    Text("${progress.intoLevel} / ${progress.needed} XP · 今日完成 $todayDone 项",
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item { SectionTitle("现在做什么", "由你选择时间与精力，系统给出可解释的推荐。") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 90).forEach { value -> FilterChip(selected = minutes == value,
                        onClick = { minutes = value }, label = { Text("$value 分钟") }) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("低精力", "中精力", "高精力").forEachIndexed { index, label -> FilterChip(selected = energy == index + 1,
                        onClick = { energy = index + 1 }, label = { Text(label) }) }
                }
            }
        }
        item {
            if (recommendation == null) EmptyState("这段时间不必硬塞任务", "可以调整时间与精力，或接取一项更小的任务。", "接取任务", create)
            else Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("NEXT QUEST", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(recommendation.quest.title, style = MaterialTheme.typography.headlineSmall)
                    Text(recommendation.quest.meta(day), style = MaterialTheme.typography.bodyMedium)
                    Text(recommendation.reasons.joinToString(" · ") { it.label() }, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { open(recommendation.quest.id) }) { Text("查看任务") }
                        TextButton(onClick = { complete(recommendation.quest.id) }, enabled = !state.busy) { Text("已完成") }
                    }
                    Text("本地规则推荐，不是 AI 判断。", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (world.goals.none { !it.archived }) item {
            EmptyState("给冒险一条主线", "把重要目标设为主线，再用小任务一步步推进。", "开启主线", { goal(null) })
        }
        if (revisit.isNotEmpty()) {
            item { SectionTitle("重新决策", "多次暂缓不等于失败。拆小、暂停，或者放下它。") }
            items(revisit, key = { "review-${it.id}" }) { q -> QuestCard(q, day, false, state.busy, { open(q.id) }, { complete(q.id) }) }
        }
        item { SectionTitle("可执行任务 · ${active.size}", action = { TextButton(onClick = allQuests) { Text("全部") } }) }
        if (active.isEmpty()) item { EmptyState("今日可以收工了", "没有需要现在执行的任务。休息也是正常的冒险节奏。") }
        items(active.sortedWith(compareByDescending<Quest> { it.priority }.thenBy { it.dueDay ?: Long.MAX_VALUE }).take(5), key = { it.id }) {
            q -> QuestCard(q, day, false, state.busy, { open(q.id) }, { complete(q.id) })
        }
    }
}
