package xyz.winhok.earthonline.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

val LocalSettlementFeedback = staticCompositionLocalOf<SettlementFeedback?> { null }

/** Effects only display an already-committed result. No callback here can issue a domain command. */
@Composable
fun SettlementSnackbarHost(host: SnackbarHostState) {
    val state = LocalEarthState.current
    val presenter = LocalNarrativePresenter.current
    val feedback = LocalSettlementFeedback.current
    SnackbarHost(host) { data ->
        val settledFeedback = remember(data) { feedback }
        val reduced = state.effects.reducedMotion || !ValueAnimator.areAnimatorsEnabled()
        val entrance = remember(data) { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(data) {
            if (reduced) entrance.snapTo(1f) else entrance.animateTo(1f, tween(180))
        }
        val title = when (settledFeedback) {
            SettlementFeedback.COMPLETE -> ContractSemantic.FEEDBACK_COMPLETE
            SettlementFeedback.ACHIEVEMENT -> ContractSemantic.FEEDBACK_ACHIEVEMENT
            SettlementFeedback.LEVEL_UP -> ContractSemantic.FEEDBACK_UP
            SettlementFeedback.LEVEL_DOWN -> ContractSemantic.FEEDBACK_DOWN
            SettlementFeedback.REPAID -> ContractSemantic.FEEDBACK_REPAID
            SettlementFeedback.RECOVERED -> ContractSemantic.FEEDBACK_RECOVERED
            SettlementFeedback.UNDONE -> ContractSemantic.FEEDBACK_UNDONE
            null -> null
        }
        val icon = when (settledFeedback) {
            SettlementFeedback.LEVEL_UP -> Icons.AutoMirrored.Filled.TrendingUp
            SettlementFeedback.LEVEL_DOWN -> Icons.AutoMirrored.Filled.TrendingDown
            SettlementFeedback.REPAID -> Icons.Default.AccountBalanceWallet
            SettlementFeedback.RECOVERED, SettlementFeedback.ACHIEVEMENT -> Icons.Default.AutoAwesome
            SettlementFeedback.UNDONE -> Icons.AutoMirrored.Filled.Undo
            else -> Icons.Default.CheckCircle
        }
        val surface = when (settledFeedback) {
            SettlementFeedback.LEVEL_DOWN, SettlementFeedback.UNDONE -> MaterialTheme.colorScheme.errorContainer
            SettlementFeedback.LEVEL_UP, SettlementFeedback.RECOVERED -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        }
        val foreground = when (settledFeedback) {
            SettlementFeedback.LEVEL_DOWN, SettlementFeedback.UNDONE -> MaterialTheme.colorScheme.onErrorContainer
            SettlementFeedback.LEVEL_UP, SettlementFeedback.RECOVERED -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }
        Snackbar(modifier = Modifier.padding(12.dp).testTag("settlement-feedback").graphicsLayer {
            alpha = entrance.value
            translationY = if (reduced) 0f else (1 - entrance.value) * 12.dp.toPx()
        }, containerColor = surface, contentColor = foreground,
            action = data.visuals.actionLabel?.let { label -> {
                TextButton(onClick = data::performAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(label, color = foreground) }
            } },
            dismissAction = {
                IconButton(onClick = data::dismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, presenter.text(ActionSemantic.CLOSE), tint = foreground)
                }
            }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, Modifier.size(24.dp), tint = foreground)
                Column(Modifier.weight(1f).heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (title != null) Text(presenter.text(title), style = MaterialTheme.typography.titleSmall)
                    Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
