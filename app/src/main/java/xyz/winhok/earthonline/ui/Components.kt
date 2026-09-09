package xyz.winhok.earthonline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import android.animation.ValueAnimator
import android.view.HapticFeedbackConstants
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import xyz.winhok.earthonline.core.*

// Full-screen editors are separate windows; expose feedback in the active window.
val LocalEarthState = staticCompositionLocalOf { EarthUiState() }
val LocalQuestPostpone = staticCompositionLocalOf<(String) -> Unit> { {} }
val LocalEditorSnackbar = staticCompositionLocalOf<SnackbarHostState?> { null }

@Composable
fun PlanetMark(modifier: Modifier = Modifier) {
    val cultivation = LocalNarrativeSystemId.current == NarrativeSystemId.CULTIVATION
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
        if (cultivation) {
            drawArc(ring, 196f, 238f, false, Offset(size.width*.18f, size.height*.04f),
                Size(size.width*.64f, size.height*.92f), style = Stroke(size.width*.018f))
            drawCircle(ring, size.width * .045f, Offset(size.width * .82f, size.height * .34f))
            drawCircle(land, size.width * .03f, Offset(size.width * .26f, size.height * .12f))
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestCard(quest: Quest, day: Long, done: Boolean, busy: Boolean,
              onOpen: () -> Unit, onComplete: () -> Unit) {
    val presenter = LocalNarrativePresenter.current
    val state = LocalEarthState.current
    val postpone = LocalQuestPostpone.current
    val cultivation = LocalNarrativeSystemId.current == NarrativeSystemId.CULTIVATION
    val view = LocalView.current
    val density = LocalDensity.current
    val threshold = with(density) { 88.dp.toPx() }
    val reduced = state.effects.reducedMotion || !ValueAnimator.areAnimatorsEnabled()
    val actionable = !done && !busy && QuestRules.available(quest, day)
    val currentComplete by rememberUpdatedState(onComplete)
    val currentOpen by rememberUpdatedState(onOpen)
    val currentActionable by rememberUpdatedState(actionable)
    val haptics by rememberUpdatedState(state.effects.haptics)
    var drag by remember { mutableFloatStateOf(0f) }
    var signalled by remember { mutableStateOf(false) }
    var actionsOpen by rememberSaveable(quest.id) { mutableStateOf(false) }
    val completeLabel = presenter.text(ActionSemantic.COMPLETE_QUEST, semanticArguments {
        put(SemanticParameters.COMPLETE_QUEST_LABEL,
            CompleteQuestLabel(CompleteQuestLabelStyle.ACCESSIBILITY, OpaqueText(quest.title)))
    })
    Column(Modifier.fillMaxWidth()) {
        OutlinedCard(onClick = onOpen,
            shape = if (cultivation) CutCornerShape(topEnd = 16.dp, bottomStart = 10.dp) else RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (cultivation) MaterialTheme.colorScheme.secondary.copy(alpha = .65f) else MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
                .offset { IntOffset(if (reduced) 0 else (drag * .18f).roundToInt(), 0) }
                .semantics {
                    customActions = buildList {
                        if (actionable) add(CustomAccessibilityAction(completeLabel) { currentComplete(); true })
                        add(CustomAccessibilityAction(presenter.text(ActionSemantic.EDIT_QUEST)) { currentOpen(); true })
                    }
                }
                .pointerInput(quest.id, threshold) {
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f; signalled = false },
                        onDragCancel = { drag = 0f; signalled = false },
                        onDragEnd = {
                            if (drag >= threshold && currentActionable) currentComplete()
                            else if (drag <= -threshold) actionsOpen = true
                            drag = 0f; signalled = false
                        },
                    ) { change, amount ->
                        change.consume()
                        drag = (drag + amount).coerceIn(-threshold * 1.5f, threshold * 1.5f)
                        if (abs(drag) >= threshold && !signalled) {
                            signalled = true
                            if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                }) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(quest.title, style = MaterialTheme.typography.titleLarge,
                            maxLines = 4, overflow = TextOverflow.Ellipsis)
                        Text(presenter.questMeta(quest, day), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!done && quest.state == QuestState.ACTIVE) {
                        FilledTonalIconButton(onClick = onComplete, enabled = actionable, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Check, contentDescription = completeLabel)
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (done) presenter.text(StateSemantic.COMPLETED) else presenter.text(ScreenSemantic.QUEST_REWARD,
                        semanticArguments { put(SemanticParameters.XP, XpAmount(state.reward(quest))) }),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(presenter.text(ScreenSemantic.QUEST_PRIORITY_DIFFICULTY, semanticArguments {
                        put(SemanticParameters.QUEST_PRIORITY_DIFFICULTY, QuestPriorityDifficulty(4 - quest.priority, quest.difficulty))
                    }), style = MaterialTheme.typography.labelMedium)
                    if (quest.postponeCount >= 3 && !done) Text(presenter.text(StateSemantic.REVIEW_REQUIRED),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                if (actionable) Text(presenter.text(ContractSemantic.GESTURE_HINT),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actionsOpen) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (actionable) TextButton(onClick = { actionsOpen = false; postpone(quest.id) }) {
                Text(presenter.text(if (state.book.active(quest.id) != null) ContractSemantic.POSTPONE_SIGNED else ActionSemantic.POSTPONE_QUEST))
            }
            TextButton(onClick = { actionsOpen = false; onOpen() }) { Text(presenter.text(ActionSemantic.EDIT_QUEST)) }
            TextButton(onClick = { actionsOpen = false }) { Text(presenter.text(ActionSemantic.CLOSE)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorFrame(title: String, busy: Boolean, canSave: Boolean, onDismiss: () -> Unit, onSave: () -> Unit,
                content: @Composable (PaddingValues) -> Unit) {
    val presenter = LocalNarrativePresenter.current
    val snackbar = LocalEditorSnackbar.current
    // Opt in to insets so Android 8-11 use ADJUST_RESIZE rather than covering the form.
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
    )) {
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val view = LocalView.current
        val lightBars = MaterialTheme.colorScheme.background.luminance() > .5f
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        val finishEditing: (() -> Unit) -> Unit = { action ->
            focus.clearFocus(force = true)
            keyboard?.hide()
            action()
        }
        Surface(Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { snackbar?.let { SnackbarHost(it) } },
                topBar = {
                    TopAppBar(title = { Text(title) }, navigationIcon = {
                        IconButton(onClick = { finishEditing(onDismiss) }, enabled = !busy) {
                            Icon(Icons.Default.Close, presenter.text(ScreenSemantic.CLOSE_EDITOR_ACCESSIBILITY))
                        }
                    }, actions = {
                        TextButton(onClick = { finishEditing(onSave) }, enabled = canSave && !busy) {
                            Text(presenter.text(ActionSemantic.SAVE))
                        }
                    })
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
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
