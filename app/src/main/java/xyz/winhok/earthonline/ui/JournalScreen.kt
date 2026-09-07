package xyz.winhok.earthonline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.EventKind

@Composable
fun JournalScreen(state: EarthUiState, note: () -> Unit) {
    var notesOnly by rememberSaveable { mutableStateOf(false) }
    val events = state.world.events.filter { !notesOnly || it.kind == EventKind.NOTE }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("冒险日志", "发生过的行动，都有迹可循。") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !notesOnly, onClick = { notesOnly = false }, label = { Text("全部事件") })
            FilterChip(selected = notesOnly, onClick = { notesOnly = true }, label = { Text("冒险手记") })
        } }
        if (events.isEmpty()) item { EmptyState("写下第一篇手记", "今天发现了什么、被什么卡住，都值得记录。", "写手记", note) }
        items(events, key = { it.id }) { event ->
            var expanded by rememberSaveable(event.id) { mutableStateOf(false) }
            OutlinedCard(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${event.kind.label()} · ${timeLabel(event.createdAt, state.world.player.zoneId)}",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(event.text, style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis)
                    if (event.xp != 0) Text("${if (event.xp > 0) "+" else ""}${event.xp} XP",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
