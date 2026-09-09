package xyz.winhok.earthonline.core

import java.time.DateTimeException
import java.time.ZoneId

interface NarrativePreferenceRecord {
    val playerId: Int
    val narrativeId: String
}

interface ContractRecord {
    val id: String
    val questId: String
    val occurrence: String
    val zoneId: String
    val dueDay: Long
    val originalRewardXp: Long
    val currentRewardXp: Long
    val status: String
    val signedAt: Long
    val closedAt: Long?
}

interface ContractRevisionRecord {
    val id: String
    val contractId: String
    val sequence: Int
    val kind: String
    val previousDueDay: Long?
    val newDueDay: Long?
    val previousRewardXp: Long
    val newRewardXp: Long
    val createdAt: Long
    val idempotencyKey: String
}

interface ConsequenceRecord {
    val id: String
    val contractId: String
    val kind: String
    val xp: Long
    val effectiveDay: Long
    val createdAt: Long
    val idempotencyKey: String
}

interface ConsequenceAdjustmentRecord {
    val id: String
    val consequenceId: String
    val kind: String
    val xp: Long
    val reasonCategory: String?
    val createdAt: Long
    val idempotencyKey: String
}

interface RepaymentAllocationRecord {
    val reversalOf: String? get() = null
    val id: String
    val consequenceId: String
    val completionId: String
    val xp: Long
    val createdAt: Long
    val idempotencyKey: String
}

interface ClockBoundaryRecord {
    val id: String
    val zoneId: String
    val lastSettledDay: Long
    val lastSettledAt: Long
}

interface RecoveryRouteRecord {
    val id: String
    val status: String
    val triggerKind: String
    val targetDebtXp: Long
    val openedAt: Long
    val closedAt: Long?
    val idempotencyKey: String
}

interface RecoveryNodeRecord {
    val routeId: String
    val questId: String
    val position: Int
    val addedAt: Long
    val completedAt: Long?
}

data class BackupV2Graph(
    val narrativePreference: NarrativePreferenceRecord,
    val contracts: List<ContractRecord>,
    val contractRevisions: List<ContractRevisionRecord>,
    val consequences: List<ConsequenceRecord>,
    val consequenceAdjustments: List<ConsequenceAdjustmentRecord>,
    val repaymentAllocations: List<RepaymentAllocationRecord>,
    val clockBoundaries: List<ClockBoundaryRecord>,
    val recoveryRoutes: List<RecoveryRouteRecord>,
    val recoveryNodes: List<RecoveryNodeRecord>,
)

object DebtRules {
    fun outstandingXp(
        consequences: List<ConsequenceRecord>,
        adjustments: List<ConsequenceAdjustmentRecord>,
        allocations: List<RepaymentAllocationRecord>,
    ): Long {
        val adjustmentsByConsequence = adjustments.groupBy { it.consequenceId }
        val reversed = allocations.mapNotNull { it.reversalOf }.toSet()
        val allocationsByConsequence = allocations.filter { it.reversalOf == null && it.id !in reversed }.groupBy { it.consequenceId }
        return consequences.fold(0L) { total, consequence ->
            val changes = adjustmentsByConsequence[consequence.id].orEmpty()
            val waived = safeSum(changes.asSequence().filter { it.kind in setOf("WAIVER", "RESTITUTION") }.map { it.xp })
            val refunded = safeSum(changes.asSequence().filter { it.kind == "REFUND" }.map { it.xp })
            val repaid = safeSum(
                allocationsByConsequence[consequence.id].orEmpty().asSequence().map { it.xp },
            )
            val settled = safeSubtract(safeSubtract(consequence.xp, waived), repaid)
            val remaining = safeAdd(settled, refunded)
            requireBackup(remaining >= 0)
            safeAdd(total, remaining)
        }
    }

    internal fun safeSum(values: Sequence<Long>): Long = values.fold(0L, ::safeAdd)

    internal fun safeAdd(left: Long, right: Long): Long = try {
        Math.addExact(left, right)
    } catch (_: ArithmeticException) {
        throw RuleViolation(RuleError.INVALID_BACKUP)
    }

    private fun safeSubtract(left: Long, right: Long): Long = try {
        Math.subtractExact(left, right)
    } catch (_: ArithmeticException) {
        throw RuleViolation(RuleError.INVALID_BACKUP)
    }
}

/** Validates the v2 business graph before Room replacement begins. */
object BackupV2Rules {
    private val contractStatuses = setOf("ACTIVE", "FULFILLED", "OVERDUE", "ABANDONED", "EXEMPTED")
    private val terminalContractStatuses = contractStatuses - "ACTIVE"
    private val originalRewards = setOf(10L, 25L, 35L, 50L, 75L)
    private val revisionKinds = setOf("SIGNED", "POSTPONED", "RESCHEDULED", "ABANDONED", "RECOMMITTED", "EDITED") + ForceMajeureReason.entries.map { "EXEMPTED_${it.name}" }
    private val consequenceKinds = setOf("OVERDUE", "ABANDONED")
    private val adjustmentKinds = setOf("WAIVER", "REFUND", "RESTITUTION")
    private val recoveryStatuses = setOf("OPEN", "CLOSED")
    private val recoveryTriggers = setOf("FULL_BREACH_COUNT", "DEBT_LEVEL")

    fun validate(world: World, graph: BackupV2Graph) {
        requireBackup(
            graph.narrativePreference.playerId == 1 &&
                validWireValue(graph.narrativePreference.narrativeId),
        )
        requireBackup(unique(graph.contracts.map { it.id }))
        requireBackup(unique(graph.contractRevisions.map { it.id }))
        requireBackup(unique(graph.consequences.map { it.id }))
        requireBackup(unique(graph.consequenceAdjustments.map { it.id }))
        requireBackup(unique(graph.repaymentAllocations.map { it.id }))
        requireBackup(unique(graph.clockBoundaries.map { it.id }))
        requireBackup(unique(graph.recoveryRoutes.map { it.id }))
        requireBackup(unique(graph.recoveryNodes.map { it.routeId to it.questId }))

        val quests = world.quests.associateBy { it.id }
        val completions = world.completions.associateBy { it.id }
        val contracts = graph.contracts.associateBy { it.id }
        val consequences = graph.consequences.associateBy { it.id }
        val routes = graph.recoveryRoutes.associateBy { it.id }

        requireBackup(unique(graph.contracts.filter { it.status == "ACTIVE" }.map { it.questId to it.occurrence }))
        graph.contracts.forEach { contract ->
            val quest = quests[contract.questId]
            requireBackup(
                validId(contract.id) && quest != null && (contract.status != "ACTIVE" || quest.kind != QuestKind.DAILY) &&
                    contract.occurrence == "once" && validZone(contract.zoneId) &&
                    validDay(contract.dueDay) && contract.originalRewardXp in originalRewards &&
                    contract.currentRewardXp in 1..contract.originalRewardXp &&
                    contract.status in contractStatuses && contract.signedAt >= 0 &&
                    when (contract.status) {
                        "ACTIVE" -> contract.closedAt == null
                        in terminalContractStatuses -> contract.closedAt != null && contract.closedAt!! >= contract.signedAt
                        else -> false
                    },
            )
        }

        requireBackup(unique(graph.contractRevisions.map { it.contractId to it.sequence }))
        requireBackup(unique(graph.contractRevisions.map { it.idempotencyKey }))
        graph.contractRevisions.forEach { revision ->
            requireBackup(
                validId(revision.id) && revision.contractId in contracts && revision.sequence > 0 &&
                    revision.kind in revisionKinds && validNullableDay(revision.previousDueDay) &&
                    validNullableDay(revision.newDueDay) && revision.previousRewardXp in 1..75 &&
                    revision.newRewardXp in 1..75 && revision.createdAt >= 0 &&
                    validIdempotencyKey(revision.idempotencyKey),
            )
        }

        requireBackup(unique(graph.consequences.map { it.idempotencyKey }))
        requireBackup(unique(graph.consequences.map { it.contractId }))
        graph.consequences.forEach { consequence ->
            val contract = contracts[consequence.contractId]
            requireBackup(
                validId(consequence.id) && contract != null && consequence.kind in consequenceKinds &&
                    consequence.kind == contract.status &&
                    consequence.xp in 1..contract.originalRewardXp && validDay(consequence.effectiveDay) &&
                    consequence.createdAt >= 0 && validIdempotencyKey(consequence.idempotencyKey),
            )
        }

        requireBackup(unique(graph.consequenceAdjustments.map { it.idempotencyKey }))
        graph.consequenceAdjustments.forEach { adjustment ->
            val consequence = consequences[adjustment.consequenceId]
            requireBackup(
                validId(adjustment.id) && consequence != null && adjustment.kind in adjustmentKinds &&
                    adjustment.xp in 1..consequence.xp &&
                    (adjustment.reasonCategory?.let(::validWireValue) ?: true) &&
                    adjustment.createdAt >= 0 && validIdempotencyKey(adjustment.idempotencyKey),
            )
        }

        requireBackup(unique(graph.repaymentAllocations.map { it.idempotencyKey }))
        val allocationIndex = graph.repaymentAllocations.associateBy { it.id }
        val reversed = graph.repaymentAllocations.mapNotNull { it.reversalOf }
        requireBackup(reversed.size == reversed.toSet().size)
        val activeAllocations = graph.repaymentAllocations.filter { it.reversalOf == null && it.id !in reversed }
        graph.repaymentAllocations.forEach { allocation ->
            val completion = completions[allocation.completionId]
            val consequence = consequences[allocation.consequenceId]
            requireBackup(
                validId(allocation.id) && consequence != null && completion != null &&
                    (allocation !in activeAllocations || completion.revokedAt == null) && allocation.xp in 1..consequence.xp &&
                    allocation.createdAt >= 0 && validIdempotencyKey(allocation.idempotencyKey),
            )
        }
        graph.repaymentAllocations.filter { it.reversalOf != null }.forEach { reversal ->
            val original = allocationIndex[reversal.reversalOf]
            requireBackup(original != null && original.reversalOf == null && original.id != reversal.id &&
                original.consequenceId == reversal.consequenceId && original.completionId == reversal.completionId &&
                original.xp == reversal.xp && reversal.createdAt >= original.createdAt)
        }
        val allocationsByConsequence = activeAllocations.groupBy { it.consequenceId }
        val adjustmentsByConsequence = graph.consequenceAdjustments.groupBy { it.consequenceId }
        activeAllocations.groupBy { it.completionId }.forEach { (completionId, allocations) ->
            val allocated = DebtRules.safeSum(allocations.asSequence().map { it.xp })
            requireBackup(allocated <= requireNotNull(completions[completionId]).xp)
        }
        graph.consequences.forEach { consequence ->
            val allocated = DebtRules.safeSum(
                allocationsByConsequence[consequence.id].orEmpty().asSequence().map { it.xp },
            )
            val changes = adjustmentsByConsequence[consequence.id].orEmpty()
            val waived = DebtRules.safeSum(changes.asSequence().filter { it.kind in setOf("WAIVER", "RESTITUTION") }.map { it.xp })
            val refunded = DebtRules.safeSum(changes.asSequence().filter { it.kind == "REFUND" }.map { it.xp })
            val historicalPaid = DebtRules.safeSum(graph.repaymentAllocations.filter { it.consequenceId == consequence.id && it.reversalOf == null }.asSequence().map { it.xp })
            requireBackup(waived <= consequence.xp && DebtRules.safeAdd(allocated, waived) <= consequence.xp && refunded <= historicalPaid && refunded <= consequence.xp)
        }
        DebtRules.outstandingXp(graph.consequences, graph.consequenceAdjustments, graph.repaymentAllocations)

        graph.clockBoundaries.forEach { boundary ->
            requireBackup(
                validId(boundary.id) && validZone(boundary.zoneId) &&
                    validDay(boundary.lastSettledDay) && boundary.lastSettledAt >= 0,
            )
        }
        requireBackup(unique(graph.recoveryRoutes.map { it.idempotencyKey }))
        graph.recoveryRoutes.forEach { route ->
            requireBackup(
                validId(route.id) && route.status in recoveryStatuses && route.triggerKind in recoveryTriggers &&
                    route.targetDebtXp > 0 && route.openedAt >= 0 &&
                    (if (route.status == "OPEN") route.closedAt == null
                    else route.closedAt != null && route.closedAt!! >= route.openedAt) &&
                    validIdempotencyKey(route.idempotencyKey),
            )
        }
        graph.recoveryNodes.forEach { node ->
            requireBackup(
                node.routeId in routes && node.questId in quests && node.position >= 0 && node.addedAt >= 0 &&
                    (node.completedAt == null || node.completedAt!! >= node.addedAt),
            )
        }
    }

    private fun <T> unique(values: List<T>): Boolean = values.size == values.toSet().size

    private fun validId(value: String): Boolean = value.length in 1..80 &&
        value.all { it.isLetterOrDigit() || it in "-_:" }

    private fun validWireValue(value: String): Boolean = value.length in 1..120 &&
        value.all { it.isLetterOrDigit() || it in "-_.:" }

    private fun validIdempotencyKey(value: String): Boolean = value.length in 1..200 &&
        value.all { !it.isWhitespace() && !it.isISOControl() }

    private fun validDay(value: Long): Boolean = value in QuestRules.MIN_DAY..QuestRules.MAX_DAY

    private fun validNullableDay(value: Long?): Boolean = value == null || validDay(value)

    private fun validZone(value: String): Boolean = try {
        ZoneId.of(value)
        true
    } catch (_: DateTimeException) {
        false
    }
}

private fun requireBackup(value: Boolean) {
    if (!value) throw RuleViolation(RuleError.INVALID_BACKUP)
}
