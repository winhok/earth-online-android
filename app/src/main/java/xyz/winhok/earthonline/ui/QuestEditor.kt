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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestEditor(quest: Quest?, goals: List<Goal>, hasHistory: Boolean, busy: Boolean,
                onDismiss: () -> Unit, onSave: (QuestDraft) -> Unit) {
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
    EditorFrame(if (quest == null) "接取新任务" else "编辑任务", busy, valid,
        onDismiss = { if (dirty) discard = true else onDismiss() },
        onSave = { onSave(QuestDraft(quest?.id, title, description, kind, difficulty, skill, priority,
            minutes.toInt(), dueDay, goalId)) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { OutlinedTextField(title, { title = it.take(120) }, label = { Text("任务标题") },
                supportingText = { Text("${title.length}/120 · 用一个可执行的动词开头") },
                modifier = Modifier.fillMaxWidth(), maxLines = 3, enabled = !busy) }
            item {
                Text("任务类型", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuestKind.entries.forEach { value -> FilterChip(selected = kind == value, onClick = { kind = value },
                        enabled = !busy && !hasHistory, label = { Text(value.label()) }) }
                }
                Text(if (hasHistory) "已有完成记录，类型已锁定。难度调整只影响新的结算轮次。"
                    else "日常按存档时区每天结算一次；支线和 Boss 是一次性任务。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text("挑战强度", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { value -> FilterChip(selected = difficulty == value,
                        onClick = { difficulty = value }, enabled = !busy,
                        label = { Text("${value.label()} · ${value.xp + if (kind == QuestKind.BOSS) 25 else 0} XP") }) }
                }
            }
            item {
                Text("成长技能", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Skill.entries.forEach { value -> FilterChip(selected = skill == value,
                        onClick = { skill = value }, enabled = !busy, label = { Text(value.label()) }) }
                }
            }
            item { OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) },
                label = { Text("预计分钟数（1–480）") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !validMinutes, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth()) }
            item {
                Text("优先级", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("普通", "重要", "紧急").forEachIndexed { index, label ->
                        FilterChip(selected = priority == index + 1, onClick = { priority = index + 1 }, enabled = !busy,
                            label = { Text(label) })
                    }
                }
            }
            item {
                Text(if (kind == QuestKind.DAILY) "起始日期（可选）" else "截止日期（可选）", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { dateOpen = true }, enabled = !busy) { Text(dueDay?.let(::dateLabel) ?: "选择日期") }
                    if (dueDay != null) TextButton(onClick = { dueDay = null }, enabled = !busy) { Text("清除日期") }
                }
            }
            item {
                Text("所属主线", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = goalId == null, onClick = { goalId = null }, enabled = !busy, label = { Text("独立任务") })
                    goals.filter { !it.archived || it.id == goalId }.forEach { g -> FilterChip(selected = goalId == g.id,
                        onClick = { goalId = g.id }, enabled = !busy, label = { Text(g.title.take(24)) }) }
                }
            }
            item { OutlinedTextField(description, { description = it.take(4_000) }, label = { Text("行动说明（可选）") },
                supportingText = { Text("${description.length}/4000") }, minLines = 4, maxLines = 10,
                enabled = !busy, modifier = Modifier.fillMaxWidth()) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    if (dateOpen) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = dueDay?.times(86_400_000L), yearRange = 1900..2200)
        DatePickerDialog(onDismissRequest = { dateOpen = false }, confirmButton = {
            TextButton(onClick = { dueDay = picker.selectedDateMillis?.div(86_400_000L); dateOpen = false },
                enabled = picker.selectedDateMillis != null) { Text("确定") }
        }, dismissButton = { TextButton(onClick = { dateOpen = false }) { Text("取消") } }) { DatePicker(state = picker) }
    }
    if (discard) DiscardDialog({ discard = false }, onDismiss)
}

@Composable
fun DiscardDialog(cancel: () -> Unit, discard: () -> Unit) {
    AlertDialog(onDismissRequest = cancel, title = { Text("放弃未保存的修改？") }, text = { Text("已经保存的数据不会改变。") },
        confirmButton = { TextButton(onClick = discard) { Text("放弃修改") } },
        dismissButton = { TextButton(onClick = cancel) { Text("继续编辑") } })
}

@Composable
fun GoalEditor(goal: Goal?, busy: Boolean, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by rememberSaveable(goal?.id) { mutableStateOf(goal?.title ?: "") }
    var description by rememberSaveable(goal?.id) { mutableStateOf(goal?.description ?: "") }
    var discard by rememberSaveable { mutableStateOf(false) }
    val dirty = title != (goal?.title ?: "") || description != (goal?.description ?: "")
    EditorFrame(if (goal == null) "开启主线" else "编辑主线", busy, title.isNotBlank(),
        { if (dirty) discard = true else onDismiss() }, { onSave(title, description) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { Text("主线是方向，不是一天必须完成的巨型任务。保存后，在任务编辑页关联到这条主线。",
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { OutlinedTextField(title, { title = it.take(120) }, label = { Text("主线名称") },
                modifier = Modifier.fillMaxWidth(), enabled = !busy) }
            item { OutlinedTextField(description, { description = it.take(4_000) }, label = { Text("完成标准 / 为什么重要") },
                modifier = Modifier.fillMaxWidth(), minLines = 5, maxLines = 10, enabled = !busy) }
        }
    }
    if (discard) DiscardDialog({ discard = false }, onDismiss)
}

@Composable
fun NoteEditor(busy: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var discard by rememberSaveable { mutableStateOf(false) }
    EditorFrame("冒险手记", busy, text.isNotBlank(), { if (text.isNotBlank()) discard = true else onDismiss() },
        { onSave(text) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(text, { text = it.take(4_000) }, label = { Text("今天发生了什么？") },
                supportingText = { Text("${text.length}/4000 · 手记保存在本机") },
                modifier = Modifier.fillMaxWidth(), minLines = 8, maxLines = 16, enabled = !busy)
        }
    }
    if (discard) DiscardDialog({ discard = false }, onDismiss)
}

@Composable
fun QuestDetail(quest: Quest, state: EarthUiState, onDismiss: () -> Unit, onEdit: () -> Unit,
                onComplete: () -> Unit, onUndo: (String) -> Unit, onPostpone: () -> Unit, onState: (QuestState) -> Unit) {
    var archive by rememberSaveable(quest.id) { mutableStateOf(false) }
    val completion = QuestRules.activeCompletion(quest, state.world.completions, state.day)
    val goal = state.world.goals.firstOrNull { it.id == quest.goalId }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(quest.title, maxLines = 4) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(quest.meta(state.day))
                if (goal != null) Text("主线：${goal.title}")
                if (quest.description.isNotBlank()) Text(quest.description)
                Text("本轮奖励：${completion?.xp ?: QuestRules.reward(quest)} XP · 暂缓 ${quest.postponeCount} 次")
                if (quest.postponeCount >= 3) Text("已经多次暂缓。可以编辑成一个更小的动作，或暂时放下。",
                    color = MaterialTheme.colorScheme.secondary)
                if (completion != null) {
                    Text("已完成：${timeLabel(completion.completedAt, state.world.player.zoneId)}")
                    OutlinedButton(onClick = { onUndo(completion.id) }, enabled = !state.busy) { Text("撤销本次完成") }
                } else if (QuestRules.available(quest, state.day)) {
                    Button(onClick = onComplete, enabled = !state.busy) { Text("确认完成") }
                }
                OutlinedButton(onClick = onEdit, enabled = !state.busy) { Text("编辑任务") }
                if (quest.state == QuestState.ACTIVE && completion == null) {
                    TextButton(onClick = onPostpone, enabled = !state.busy) { Text("暂缓一天（不修改截止日）") }
                    TextButton(onClick = { onState(QuestState.PAUSED) }, enabled = !state.busy) { Text("暂停这项任务") }
                }
                if (quest.state != QuestState.ACTIVE || quest.postponeCount > 0 || quest.snoozedUntilDay != null) {
                    TextButton(onClick = { onState(QuestState.ACTIVE) }, enabled = !state.busy) { Text("重新接取 / 清除暂缓") }
                }
                if (quest.state != QuestState.ARCHIVED) TextButton(onClick = { archive = true }, enabled = !state.busy) { Text("归档任务") }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
    if (archive) AlertDialog(onDismissRequest = { archive = false }, title = { Text("归档任务？") },
        text = { Text("任务将移至归档列表。已有经验和日志保留，之后可以重新接取。") },
        confirmButton = { TextButton(onClick = { onState(QuestState.ARCHIVED) }, enabled = !state.busy) { Text("归档") } },
        dismissButton = { TextButton(onClick = { archive = false }) { Text("取消") } })
}
