package xyz.winhok.earthonline.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.time.LocalDate
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.winhok.earthonline.BuildConfig
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.reminder.Reminders

private const val DELETE_CONFIRMATION = "删除" // narrative-copy-guard: allow-domain-token

@Composable
fun SettingsScreen(model: EarthViewModel, state: EarthUiState, onDismiss: () -> Unit) {
    val presenter = LocalNarrativePresenter.current
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
        if (!notificationAllowed) model.notify(NotificationSemantic.NOTIFICATION_PERMISSION_DENIED)
    }
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(model::export) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(model::prepareRestore) }
    val dirty = name != player.name || server != player.server || theme != player.theme ||
        reminders != player.remindersEnabled || hour != player.reminderHour.toString()
    val valid = name.isNotBlank() && server.isNotBlank() && hour.toIntOrNull()?.let { it in 0..23 } == true
    EditorFrame(presenter.text(ActionSemantic.OPEN_SETTINGS), state.busy, valid,
        { if (dirty) discard = true else onDismiss() },
        { model.savePlayer(name, server, theme, reminders, hour.toInt(), onDismiss) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("settings-list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { SectionTitle(presenter.text(ScreenSemantic.PLAYER_SETTINGS_TITLE)) }
            item { OutlinedTextField(name, { name = it.take(32) }, label = { Text(presenter.text(FieldSemantic.PLAYER_NAME)) },
                enabled = !state.busy, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings-player-name")) }
            item { OutlinedTextField(server, { server = it.take(40) }, label = { Text(presenter.text(FieldSemantic.SERVER_NAME)) },
                enabled = !state.busy, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item {
                Text(presenter.text(ScreenSemantic.INTERFACE_STYLE), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { value -> FilterChip(selected = theme == value,
                        onClick = { theme = value }, enabled = !state.busy,
                        label = { Text(presenter.text(value.narrativeKey())) }) }
                }
            }
            item {
                HorizontalDivider()
                NarrativeSystemPicker(
                    selectedSystemId = state.narrativeSystemId,
                    requestedSystemId = state.requestedNarrativeId,
                    previewQuest = state.world.quests.firstOrNull {
                        it.state == QuestState.ACTIVE &&
                            QuestRules.available(it, state.day) &&
                            QuestRules.activeCompletion(it, state.world.completions, state.day) == null
                    }
                        ?: state.world.quests.firstOrNull(),
                    day = state.day,
                    enabled = !state.busy,
                    onSelect = model::switchNarrative,
                )
            }
            item { HorizontalDivider(); SectionTitle(
                presenter.text(ScreenSemantic.REMINDERS_TITLE),
                presenter.text(ScreenSemantic.REMINDERS_BODY),
            ) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(presenter.text(ScreenSemantic.ENABLE_DAILY_REMINDER), Modifier.weight(1f))
                    Switch(checked = reminders, enabled = !state.busy, onCheckedChange = { enabled ->
                        when {
                            !enabled -> reminders = false
                            Reminders.allowed(context) -> reminders = true
                            Build.VERSION.SDK_INT >= 33 -> requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else -> model.notify(NotificationSemantic.NOTIFICATION_PERMISSION_DENIED)
                        }
                    })
                }
                if (!notificationAllowed) Text(presenter.text(ScreenSemantic.NOTIFICATIONS_DISABLED), color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall)
            }
            item { OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) },
                label = { Text(presenter.text(FieldSemantic.REMINDER_HOUR)) }, singleLine = true, enabled = !state.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = hour.toIntOrNull()?.let { it in 0..23 } != true, modifier = Modifier.fillMaxWidth()) }
            item {
                Text(presenter.text(ScreenSemantic.TIMEZONE_INFO, semanticArguments {
                    put(SemanticParameters.ZONE_ID, ZoneIdValue(player.zoneId))
                }),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    } catch (_: Exception) {
                        try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}"))) }
                        catch (_: Exception) { model.notify(NotificationSemantic.OPEN_SETTINGS_FAILED) }
                    }
                }) { Text(presenter.text(ActionSemantic.OPEN_NOTIFICATION_SETTINGS)) }
            }
            item { HorizontalDivider(); SectionTitle(
                presenter.text(ScreenSemantic.BACKUP_TITLE),
                presenter.text(ScreenSemantic.BACKUP_BODY),
            ) }
            item {
                Text(presenter.text(ScreenSemantic.BACKUP_SECURITY_BODY),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { exportFile.launch("earth-online-${LocalDate.ofEpochDay(state.day)}.json") }, enabled = !state.busy) { Text(presenter.text(ActionSemantic.EXPORT_BACKUP)) }
                    OutlinedButton(onClick = { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !state.busy) { Text(presenter.text(ActionSemantic.IMPORT_BACKUP)) }
                }
            }
            item { TextButton(onClick = { deleteText = ""; deleteOpen = true }, enabled = !state.busy) {
                Text(presenter.text(ActionSemantic.DELETE_ALL_DATA, semanticArguments {
                    put(SemanticParameters.DELETE_DATA_LABEL, DeleteDataLabel.SETTINGS)
                }), color = MaterialTheme.colorScheme.error)
            } }
            item { HorizontalDivider(); Text(presenter.text(ScreenSemantic.ABOUT_TITLE, semanticArguments {
                    put(SemanticParameters.VERSION_NAME, VersionName(BuildConfig.VERSION_NAME))
                }), style = MaterialTheme.typography.titleMedium)
                Text(presenter.text(ScreenSemantic.ABOUT_BODY), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { privacyOpen = true }) { Text(presenter.text(ActionSemantic.OPEN_PRIVACY)) }
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
    if (deleteOpen) AlertDialog(onDismissRequest = { if (!state.busy) deleteOpen = false }, title = { Text(presenter.text(ScreenSemantic.DELETE_DATA_TITLE)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(presenter.text(ScreenSemantic.DELETE_DATA_BODY))
            OutlinedTextField(deleteText, { deleteText = it.take(8) }, label = { Text(presenter.text(ScreenSemantic.DELETE_CONFIRM_FIELD)) }, enabled = !state.busy)
        } },
        confirmButton = { TextButton(onClick = { model.reset { deleteOpen = false; onDismiss() } },
            enabled = deleteText == DELETE_CONFIRMATION && !state.busy) { Text(presenter.text(ActionSemantic.DELETE_ALL_DATA, semanticArguments {
                put(SemanticParameters.DELETE_DATA_LABEL, DeleteDataLabel.CONFIRM)
            }), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deleteOpen = false }, enabled = !state.busy) { Text(presenter.text(ActionSemantic.CANCEL)) } })
    if (privacyOpen) AlertDialog(onDismissRequest = { privacyOpen = false }, title = { Text(presenter.text(ScreenSemantic.PRIVACY_TITLE)) },
        text = { Text(presenter.text(ScreenSemantic.PRIVACY_BODY), modifier = Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { privacyOpen = false }) { Text(presenter.text(ActionSemantic.CLOSE)) } })
}

@Composable
fun RestorePreviewDialog(
    preview: RestorePreview,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val presenter = LocalNarrativePresenter.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(presenter.text(ScreenSemantic.RESTORE_TITLE)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(presenter.text(ScreenSemantic.RESTORE_VALIDATED))
                Text(presenter.text(ScreenSemantic.RESTORE_PLAYER))
                Text(preview.snapshot.world.player.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                presenter.text(ScreenSemantic.RESTORE_SUMMARY, semanticArguments {
                    put(SemanticParameters.RESTORE_SUMMARY, RestoreSummary(
                        preview.questChanges,
                        preview.completionChanges,
                        preview.contractChanges,
                        preview.debtRecordChanges,
                        preview.currentDebtXp,
                        preview.importedDebtXp,
                        preview.recoveryRouteChanges,
                        preview.narrativeChanges,
                    ))
                }).lineSequence().forEach { Text(it) }
                Spacer(Modifier.height(6.dp))
                Text(presenter.text(ScreenSemantic.RESTORE_WARNING))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) { Text(presenter.text(ActionSemantic.CONFIRM_RESTORE)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(presenter.text(ActionSemantic.CANCEL)) }
        },
    )
}
