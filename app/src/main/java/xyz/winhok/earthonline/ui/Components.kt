package xyz.winhok.earthonline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.winhok.earthonline.core.*

// Full-screen editors are separate windows; expose feedback in the active window.
val LocalEditorSnackbar = staticCompositionLocalOf<SnackbarHostState?> { null }

@Composable
fun PlanetMark(modifier: Modifier = Modifier) {
    val land = MaterialTheme.colorScheme.primary
    val water = MaterialTheme.colorScheme.primaryContainer
    val ring = MaterialTheme.colorScheme.secondary
    Canvas(modifier.size(76.dp)) {
        val r = size.minDimension * .34f
        drawCircle(water, r)
        drawArc(land, 20f, 115f, true, Offset(center.x-r, center.y-r), Size(r*2,r*2))
        drawCircle(land, r * .27f, Offset(center.x - r * .4f, center.y - r * .35f))
        drawArc(ring, 5f, 310f, false, Offset(size.width*.06f, size.height*.25f),
            Size(size.width*.88f, size.height*.5f), style = Stroke(size.width*.025f))
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
fun EmptyState(title: String, body: String, actionText: String? = null, action: () -> Unit = {}) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionText != null) Button(onClick = action) { Text(actionText) }
        }
    }
}

@Composable
fun QuestCard(quest: Quest, day: Long, done: Boolean, busy: Boolean,
              onOpen: () -> Unit, onComplete: () -> Unit) {
    OutlinedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(quest.title, style = MaterialTheme.typography.titleMedium,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(quest.meta(day), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!done && quest.state == QuestState.ACTIVE) {
                    FilledTonalIconButton(onClick = onComplete, enabled = !busy && QuestRules.available(quest, day)) {
                        Icon(Icons.Default.Check, contentDescription = "完成任务：${quest.title}")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (done) "已完成" else "+${QuestRules.reward(quest)} XP", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text("P${4 - quest.priority} · ${quest.difficulty.label()}", style = MaterialTheme.typography.labelMedium)
                if (quest.postponeCount >= 3 && !done) Text("待重审", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorFrame(title: String, busy: Boolean, canSave: Boolean, onDismiss: () -> Unit, onSave: () -> Unit,
                content: @Composable (PaddingValues) -> Unit) {
    val snackbar = LocalEditorSnackbar.current
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { snackbar?.let { SnackbarHost(it) } },
                topBar = {
                    TopAppBar(title = { Text(title) }, navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !busy) { Icon(Icons.Default.Close, "关闭编辑") }
                    }, actions = { TextButton(onClick = onSave, enabled = canSave && !busy) { Text("保存") } })
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().imePadding(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 760.dp).fillMaxWidth()) { content(padding) }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun SmallStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
