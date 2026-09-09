package xyz.winhok.earthonline.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.winhok.earthonline.core.*

@Composable
fun ContractInfo(contract: TimeContract) {
    val presenter = LocalNarrativePresenter.current
    Text(presenter.text(ContractSemantic.INFO, semanticArguments {
        put(ContractParameters.CARD, ContractCardData(contract.status, contract.dueDay, contract.zoneId,
            contract.originalRewardXp, contract.currentRewardXp, contract.extensionCount,
            contract.fulfilledAt != null && contract.status != "FULFILLED"))
    }), Modifier.testTag("contract-info"), style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun ContractConfirmation(disclosure: ContractDisclosure, busy: Boolean, confirm: () -> Unit, cancel: () -> Unit) {
    val presenter = LocalNarrativePresenter.current
    AlertDialog(onDismissRequest = { if (!busy) cancel() },
        modifier = Modifier.testTag("contract-confirmation"),
        title = { Text(presenter.text(ContractSemantic.TITLE)) },
        text = { Text(presenter.text(ContractSemantic.DISCLOSURE, semanticArguments {
            put(ContractParameters.DISCLOSURE, disclosure)
        }), Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = confirm, enabled = !busy, modifier = Modifier.testTag("confirm-contract")) {
            Text(presenter.text(ContractSemantic.CONFIRM))
        } },
        dismissButton = { TextButton(onClick = cancel, enabled = !busy, modifier = Modifier.testTag("cancel-contract")) {
            Text(presenter.text(ActionSemantic.CANCEL))
        } })
}

/** Acknowledges facts already committed. Neither BACK nor motion can acknowledge them. */
@Composable
fun OverdueBatchDialog(state: EarthUiState, acknowledge: (Set<String>) -> Unit) {
    val presenter = LocalNarrativePresenter.current
    val pending = state.unacknowledged
    var expanded by rememberSaveable(pending.map { it.id }.joinToString()) { mutableStateOf(false) }
    val total = pending.sumOf { it.xp }
    val progress = state.progress
    val view = androidx.compose.ui.platform.LocalView.current
    // One short signal when this set of committed facts first appears, never on skin change.
    LaunchedEffect(pending.map { it.id }) {
        if (state.effects.haptics) view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        if (state.effects.sound) view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
    }
    val without = ProgressRules.fromXp((progress.current.xp - progress.debtXp + total).coerceAtLeast(0))
    Dialog(onDismissRequest = {}, properties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false,
        decorFitsSystemWindows = false,
    )) {
        BackHandler { /* Reading is explicit; no mutation on back. */ }
        Surface(Modifier.fillMaxSize().testTag("overdue-batch")) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
                Text(presenter.text(ContractSemantic.BATCH_TITLE), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item { Text(presenter.text(ContractSemantic.BATCH_TOTAL, semanticArguments {
                        put(ContractParameters.BATCH, OverdueBatchData(pending.size, total, without.level, progress.current.level))
                    }), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error) }
                    item { Text(presenter.text(ContractSemantic.BATCH_BODY)) }
                    items(if (expanded) pending else pending.take(3), key = { it.id }) { liability ->
                        val line = state.deadline.ledgerLines().first { it.id == liability.id }
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Text(presenter.text(ContractSemantic.LEDGER_ENTRY, semanticArguments {
                                put(ContractParameters.LEDGER, line)
                            }), Modifier.padding(16.dp))
                        }
                    }
                    if (pending.size > 3) item {
                        TextButton(onClick = { expanded = !expanded }) { Text(presenter.text(
                            if (expanded) ContractSemantic.COLLAPSE else ContractSemantic.EXPAND)) }
                    }
                    item { Text(presenter.text(ContractSemantic.RECOVERY_PENDING), style = MaterialTheme.typography.bodySmall) }
                }
                Button(onClick = { acknowledge(pending.map { it.id }.toSet()) }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("acknowledge-overdue")) {
                    Text(presenter.text(ContractSemantic.ACKNOWLEDGE))
                }
            }
        }
    }
}

@Composable
fun ForceMajeureDialog(contract: TimeContract, busy: Boolean, dismiss: () -> Unit,
                       choose: (ForceMajeureReason) -> Unit) {
    val presenter = LocalNarrativePresenter.current
    var reason by rememberSaveable(contract.id) { mutableStateOf(ForceMajeureReason.HEALTH) }
    val keys = mapOf(ForceMajeureReason.HEALTH to ContractSemantic.REASON_HEALTH,
        ForceMajeureReason.FAMILY to ContractSemantic.REASON_FAMILY,
        ForceMajeureReason.EXTERNAL to ContractSemantic.REASON_EXTERNAL,
        ForceMajeureReason.OTHER to ContractSemantic.REASON_OTHER)
    AlertDialog(onDismissRequest = dismiss, modifier = Modifier.testTag("force-majeure-dialog"), title = { Text(presenter.text(ActionSemantic.APPLY_FORCE_MAJEURE)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(presenter.text(ContractSemantic.FORCE_REASON))
            keys.forEach { (value, key) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                    value = reason == value, role = Role.RadioButton, enabled = !busy,
                    onValueChange = { reason = value }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = reason == value, onClick = null)
                    Text(presenter.text(key), Modifier.padding(start = 8.dp))
                }
            }
        } },
        confirmButton = { TextButton(onClick = { choose(reason) }, enabled = !busy, modifier = Modifier.testTag("confirm-force-majeure")) { Text(presenter.text(ActionSemantic.CONFIRM)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(presenter.text(ActionSemantic.CANCEL)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractLedgerScreen(state: EarthUiState, model: EarthViewModel, close: () -> Unit,
                         openQuest: (String) -> Unit, create: () -> Unit) {
    val presenter = LocalNarrativePresenter.current
    val route = state.book.routes.singleOrNull { it.status == "OPEN" }
    val selectedNodes = state.book.nodes.filter { it.routeId == route?.id }.sortedBy { it.position }
    val done = state.world.completions.filter { it.revokedAt == null }.map { it.id }.toSet()
    val candidates = state.world.quests.filter { QuestRules.available(it, state.day) &&
        QuestRules.completionId(it, state.day) !in done &&
        selectedNodes.none { n -> n.questId == it.id && n.completedAt != null } }
    var selected by rememberSaveable(route?.id) { mutableStateOf(selectedNodes.filter { it.completedAt == null }.map { it.questId }) }
    val availableSelected = selected.filter { id -> candidates.any { it.id == id } }
    val selectedXp = candidates.filter { it.id in availableSelected }.sumOf { state.reward(it).toLong() }
    var choosing by rememberSaveable(route?.id) { mutableStateOf(false) }
    var waiveId by rememberSaveable { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().testTag("contract-ledger")) {
            Scaffold(topBar = { TopAppBar(title = { Text(presenter.text(ContractSemantic.LEDGER_TITLE)) },
                navigationIcon = { TextButton(onClick = close) { Text(presenter.text(ActionSemantic.CLOSE)) } }) }) { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("contract-ledger-list"), contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { ContractBalance(state) }
                    item { Text(presenter.text(ContractSemantic.RULES), style = MaterialTheme.typography.bodySmall) }
                    if (route != null) {
                        item { SectionTitle(presenter.text(StateSemantic.RECOVERY_OPEN), presenter.text(ContractSemantic.RECOVERY_BODY)) }
                        val activeNodes = selectedNodes.filter { it.completedAt == null }.mapNotNull { n ->
                            candidates.firstOrNull { it.id == n.questId }
                        }.take(3)
                        items(activeNodes, key = { "route-${it.id}" }) { q ->
                            QuestCard(q, state.day, false, state.busy, { openQuest(q.id) }, { model.complete(q.id) })
                        }
                        item { OutlinedButton(onClick = { choosing = !choosing }, modifier = Modifier.testTag("choose-recovery")) {
                            Text(presenter.text(ContractSemantic.RECOVERY_SELECT))
                        } }
                        if (choosing) {
                            item { Text(presenter.text(ContractSemantic.RECOVERY_SELECTION, semanticArguments {
                                put(ContractParameters.SELECTION, RecoverySelectionData(selectedXp, state.progress.debtXp, availableSelected.size))
                            })) }
                            if (candidates.isEmpty()) item { Text(presenter.text(ContractSemantic.RECOVERY_EMPTY)) }
                            items(candidates, key = { "choose-${it.id}" }) { q ->
                                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("recovery-choice-${q.id}").toggleable(value = q.id in selected,
                                    role = Role.Checkbox, enabled = !state.busy, onValueChange = { checked ->
                                        selected = if (checked) (selected + q.id).distinct() else selected - q.id
                                    }), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = q.id in selected, onCheckedChange = null)
                                    Text(q.title, Modifier.weight(1f).padding(8.dp))
                                    Text(state.reward(q).toString())
                                }
                            }
                            item { Button(onClick = { model.selectRecovery(availableSelected) { choosing = false } },
                                enabled = availableSelected.isNotEmpty() && selectedXp >= state.progress.debtXp && !state.busy,
                                modifier = Modifier.fillMaxWidth().testTag("save-recovery")) {
                                Text(presenter.text(ActionSemantic.SELECT_RECOVERY_NODE))
                            } }
                        }
                    } else item { Text(presenter.text(ContractSemantic.RECOVERY_PENDING)) }
                    item { OutlinedButton(onClick = create) { Text(presenter.text(ActionSemantic.CREATE_QUEST)) } }
                    // Retain an actionable path for every old contract, not only the latest revision.
                    item { SectionTitle(presenter.text(ContractSemantic.CONTRACT_HISTORY)) }
                    items(state.book.contracts.asReversed(), key = { "contract-${it.id}" }) { contract ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(contract.titleSnapshot.ifBlank { state.world.quests.firstOrNull { it.id == contract.questId }?.title.orEmpty() })
                                ContractInfo(contract)
                                if (contract.status !in setOf("FULFILLED", "EXEMPTED") && state.book.adjustments.none {
                                        a -> a.consequenceId in state.book.liabilities.filter { it.contractId == contract.id }.map { it.id } &&
                                            a.kind in setOf("WAIVER", "RESTITUTION") }) {
                                    TextButton(onClick = { waiveId = contract.id }, enabled = !state.busy) {
                                        Text(presenter.text(ActionSemantic.APPLY_FORCE_MAJEURE))
                                    }
                                }
                            }
                        }
                    }
                    val lines = state.deadline.ledgerLines()
                    if (lines.isEmpty()) item { Text(presenter.text(ContractSemantic.LEDGER_EMPTY)) }
                    items(lines, key = { "line-${it.id}" }) { line ->
                        Text(presenter.text(ContractSemantic.LEDGER_ENTRY, semanticArguments { put(ContractParameters.LEDGER, line) }),
                            style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
    }
    state.book.contracts.firstOrNull { it.id == waiveId }?.let { contract ->
        ForceMajeureDialog(contract, state.busy, { waiveId = null }) { reason ->
            model.waive(contract.id, reason); waiveId = null
        }
    }
}

@Composable
fun ContractBalance(state: EarthUiState, open: (() -> Unit)? = null) {
    val presenter = LocalNarrativePresenter.current
    OutlinedCard(Modifier.fillMaxWidth().testTag("contract-balance"), colors = CardDefaults.outlinedCardColors(
        containerColor = if (state.progress.outOfOrder) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.progress.outOfOrder) Text(presenter.text(StateSemantic.OUT_OF_ORDER), style = MaterialTheme.typography.titleMedium)
            Text(presenter.text(ContractSemantic.SUMMARY, semanticArguments { put(ContractParameters.SUMMARY, state.progress) }))
            if (open != null) TextButton(onClick = open, modifier = Modifier.testTag("open-ledger")) {
                Text(presenter.text(ActionSemantic.VIEW_DEBT))
            }
        }
    }
}

@Composable
fun WorldStory(state: EarthUiState, model: EarthViewModel) {
    val presenter = LocalNarrativePresenter.current
    val collapsed = state.preference("story_collapsed")
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(presenter.text(ContractSemantic.STORY_TITLE), style = MaterialTheme.typography.titleMedium)
            if (!collapsed) {
                Text(presenter.text(ContractSemantic.STORY_BODY), style = MaterialTheme.typography.bodyMedium)
                if (state.world.completions.any { it.revokedAt == null }) Text(presenter.text(ContractSemantic.STORY_FIRST_ACTION))
                if (state.book.contracts.isNotEmpty()) Text(presenter.text(ContractSemantic.STORY_FIRST_PROMISE))
                if (state.book.routes.any { it.closedAt != null }) Text(presenter.text(ContractSemantic.STORY_RETURN))
            }
            TextButton(onClick = { model.setPresentationPreference("story_collapsed", !collapsed) }, enabled = !state.busy) {
                Text(presenter.text(if (collapsed) ContractSemantic.STORY_EXPAND else ContractSemantic.STORY_COLLAPSE))
            }
            if (!state.preference("intro_seen")) TextButton(onClick = { model.setPresentationPreference("intro_seen", true) }, enabled = !state.busy) {
                Text(presenter.text(ContractSemantic.STORY_READ))
            }
        }
    }
}
