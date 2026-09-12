package xyz.winhok.earthonline.data

import xyz.winhok.earthonline.core.RuleError
import xyz.winhok.earthonline.core.RuleViolation
import xyz.winhok.earthonline.core.SnapshotBudget

/** Conservative upper bound for the nested v2 JSON envelope. */
object BackupSnapshotBudget {
    const val MAX_BYTES = SnapshotBudget.MAX_BYTES

    fun upperBound(snapshot: BackupSnapshot): Long {
        fun text(vararg values: String?): Long = values.sumOf { (it?.length ?: 0).toLong() * 8 }

        var bytes = SnapshotBudget.upperBound(snapshot.world) + 4_096L
        bytes += 512 + text(snapshot.narrativePreference.narrativeId)
        snapshot.contracts.forEach {
            bytes += 1_024 + text(it.id, it.questId, it.occurrence, it.zoneId, it.status)
        }
        snapshot.contractRevisions.forEach {
            bytes += 1_024 + text(it.id, it.contractId, it.kind, it.idempotencyKey)
        }
        snapshot.consequences.forEach {
            bytes += 768 + text(it.id, it.contractId, it.kind, it.idempotencyKey)
        }
        snapshot.consequenceAdjustments.forEach {
            bytes += 768 + text(it.id, it.consequenceId, it.kind, it.reasonCategory, it.idempotencyKey)
        }
        snapshot.repaymentAllocations.forEach {
            bytes += 768 + text(it.id, it.consequenceId, it.completionId, it.idempotencyKey)
        }
        snapshot.clockBoundaries.forEach {
            bytes += 512 + text(it.id, it.zoneId)
        }
        snapshot.recoveryRoutes.forEach {
            bytes += 768 + text(it.id, it.status, it.triggerKind, it.idempotencyKey)
        }
        snapshot.recoveryNodes.forEach {
            bytes += 512 + text(it.routeId, it.questId)
        }
        bytes += snapshot.settlementReceipts.sumOf { 256 + text(it.consequenceId) }
        bytes += snapshot.contracts.sumOf { text(it.titleSnapshot) }
        bytes += snapshot.presentationPreferences.sumOf { 512 + text(it.narrativeId,it.preferenceKey) }
        return bytes
    }

    fun requireFits(snapshot: BackupSnapshot) {
        if (upperBound(snapshot) > MAX_BYTES) throw RuleViolation(RuleError.LIMIT)
    }
}
