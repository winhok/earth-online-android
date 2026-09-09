package xyz.winhok.earthonline.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.winhok.earthonline.BuildConfig
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.reminder.Reminders

@Composable
fun SettingsScreen(model: EarthViewModel, state: EarthUiState, onDismiss: () -> Unit) {
    val player = state.world.player
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var name by rememberSaveable { mutableStateOf(player.name) }
    var server by rememberSaveable { mutableStateOf(player.server) }
    var theme by rememberSaveable { mutableStateOf(player.theme) }
    var reminders by rememberSaveable { mutableStateOf(player.remindersEnabled) }
    var hour by rememberSaveable { mutableStateOf(player.reminderHour.toString()) }
    var notificationAllowed by remember { mutableStateOf(Reminders.allowed(context)) }
    var deleteOpen by rememberSaveable { mutableStateOf(false) }
    var deleteText by rememberSaveable { mutableStateOf("") }
    var privacyOpen by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val pending by model.pendingRestore.collectAsStateWithLifecycle()
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) notificationAllowed = Reminders.allowed(context) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationAllowed = granted && Reminders.allowed(context)
        reminders = notificationAllowed
        if (!notificationAllowed) model.notify("通知权限未开启，任务功能不受影响。可在系统设置中手动开启。")
    }
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(model::export) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(model::prepareRestore) }
    val dirty = name != player.name || server != player.server || theme != player.theme ||
        reminders != player.remindersEnabled || hour != player.reminderHour.toString()
    val valid = name.isNotBlank() && server.isNotBlank() && hour.toIntOrNull()?.let { it in 0..23 } == true
    EditorFrame("设置与存档", state.busy, valid,
        { if (dirty) discard = true else onDismiss() },
        { model.savePlayer(name, server, theme, reminders, hour.toInt(), onDismiss) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { SectionTitle("玩家设置") }
            item { OutlinedTextField(name, { name = it.take(32) }, label = { Text("玩家名") },
                enabled = !state.busy, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(server, { server = it.take(40) }, label = { Text("服务器名") },
                enabled = !state.busy, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item {
                Text("界面风格", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { value -> FilterChip(selected = theme == value,
                        onClick = { theme = value }, enabled = !state.busy, label = { Text(value.label()) }) }
                }
            }
            item { HorizontalDivider(); SectionTitle("冒险提醒", "每日约定时间之后提醒，系统省电可能导致延迟。") }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("开启每日提醒", Modifier.weight(1f))
                    Switch(checked = reminders, enabled = !state.busy, onCheckedChange = { enabled ->
                        when {
                            !enabled -> reminders = false
                            Reminders.allowed(context) -> reminders = true
                            Build.VERSION.SDK_INT >= 33 -> requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else -> model.notify("系统通知已关闭，请在系统设置中开启。")
                        }
                    })
                }
                if (!notificationAllowed) Text("当前系统通知未开启。", color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall)
            }
            item { OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) },
                label = { Text("提醒小时（0–23）") }, singleLine = true, enabled = !state.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = hour.toIntOrNull()?.let { it in 0..23 } != true, modifier = Modifier.fillMaxWidth()) }
            item {
                Text("存档结算时区：${player.zoneId}\n日常按此时区的自然日刷新，旅行或修改设备时区不会切换结算时区。1.0 不提供时区迁移。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    } catch (_: Exception) {
                        try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}"))) }
                        catch (_: Exception) { model.notify("无法打开系统设置，请手动进入应用通知设置。") }
                    }
                }) { Text("打开系统通知设置") }
            }
            item { HorizontalDivider(); SectionTitle("本地存档", "导出的是已保存的数据；导入会先校验，再请求覆盖确认。") }
            item {
                Text("备份为明文 JSON，包含任务和手记。请勿公开分享；文件上限 8 MiB。卸载或清除应用数据会删除本机存档。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { exportFile.launch("earth-online-${dateLabel(state.day)}.json") }, enabled = !state.busy) { Text("导出存档") }
                    OutlinedButton(onClick = { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !state.busy) { Text("导入存档") }
                }
            }
            item { TextButton(onClick = { deleteText = ""; deleteOpen = true }, enabled = !state.busy) {
                Text("删除本机全部存档", color = MaterialTheme.colorScheme.error)
            } }
            item { HorizontalDivider(); Text("地球 Online ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                Text("离线单人版 · Kotlin + Jetpack Compose\n不接入广告、分析 SDK、账号或 AI 云服务。", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { privacyOpen = true }) { Text("隐私说明") }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    if (discard) DiscardDialog({ discard = false }, onDismiss)
    pending?.let { value ->
        RestorePreviewDialog(
            preview = value.preview,
            busy = state.busy,
            onConfirm = { model.confirmRestore(onDismiss) },
            onDismiss = model::dismissRestore,
        )
    }
    if (deleteOpen) AlertDialog(onDismissRequest = { if (!state.busy) deleteOpen = false }, title = { Text("删除本机全部存档") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("任务、主线、经验和日志都会删除。无法在应用内撤回。请先导出存档，再输入“删除”确认。")
            OutlinedTextField(deleteText, { deleteText = it.take(8) }, label = { Text("输入：删除") }, enabled = !state.busy)
        } },
        confirmButton = { TextButton(onClick = { model.reset { deleteOpen = false; onDismiss() } },
            enabled = deleteText == "删除" && !state.busy) { Text("永久删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deleteOpen = false }, enabled = !state.busy) { Text("取消") } })
    if (privacyOpen) AlertDialog(onDismissRequest = { privacyOpen = false }, title = { Text("隐私说明 · 离线 1.0") },
        text = { Text("本应用在应用沙盒的 Room 数据库中保存玩家设置、任务、主线、完成记录及手记。\n\n应用没有联网、定位、广告、埋点和远程 AI 功能。系统通知仅在你主动开启后使用；通知不显示任务标题。\n\n存档依靠设备本身的存储保护，数据库未另加应用层加密。系统自动备份已在清单中禁用；厂商行为仍需真机验证。\n\n导出会把明文数据写入你选择的位置，所选文件提供商可能是云盘。导入仅在本机解析。分享和保管导出文件由你控制。\n\n删除本机存档不会删除你之前导出的文件。", modifier = Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { privacyOpen = false }) { Text("关闭") } })
}

@Composable
fun RestorePreviewDialog(
    preview: RestorePreview,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("覆盖本机存档？") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("已通过格式、摘要、容量与关联校验。")
                Text("玩家")
                Text(preview.snapshot.world.player.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text("任务变化：${preview.questChanges} 项")
                Text("完成变化：${preview.completionChanges} 项")
                Text("契约变化：${preview.contractChanges} 项")
                Text("债务记录变化：${preview.debtRecordChanges} 项（${preview.currentDebtXp} XP → ${preview.importedDebtXp} XP）")
                Text("恢复路线变化：${preview.recoveryRouteChanges} 项")
                Text("叙事体系变化：${preview.narrativeChanges} 项")
                Spacer(Modifier.height(6.dp))
                Text("当前存档将被完整替换，不会自动合并，也不会保留快照外的债务或体系偏好。建议先取消并导出旧存档。导入后提醒默认关闭。")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) { Text("确认覆盖") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") }
        },
    )
}
