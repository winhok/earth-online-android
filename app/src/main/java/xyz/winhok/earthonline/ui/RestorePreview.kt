package xyz.winhok.earthonline.ui

import xyz.winhok.earthonline.data.BackupSnapshot

data class RestorePreview(
    val snapshot: BackupSnapshot,
    val questChanges: Int,
    val completionChanges: Int,
    val contractChanges: Int,
    val debtRecordChanges: Int,
    val recoveryRouteChanges: Int,
    val narrativeChanges: Int,
    val currentDebtXp: Long,
    val importedDebtXp: Long,
) {
    companion object {
        fun between(current: BackupSnapshot, imported: BackupSnapshot) = RestorePreview(
            snapshot = imported,
            questChanges = changed(current.world.quests, imported.world.quests) { it.id },
            completionChanges = changed(current.world.completions, imported.world.completions) { it.id },
            contractChanges =
                changed(current.contracts, imported.contracts) { it.id } +
                changed(current.contractRevisions, imported.contractRevisions) { it.id },
            debtRecordChanges =
                changed(current.consequences, imported.consequences) { it.id } +
                changed(current.consequenceAdjustments, imported.consequenceAdjustments) { it.id } +
                changed(current.repaymentAllocations, imported.repaymentAllocations) { it.id },
            recoveryRouteChanges =
                changed(current.recoveryRoutes, imported.recoveryRoutes) { it.id } +
                changed(current.recoveryNodes, imported.recoveryNodes) { it.routeId to it.questId },
            narrativeChanges = if (current.narrativePreference == imported.narrativePreference) 0 else 1,
            currentDebtXp = current.outstandingDebtXp(),
            importedDebtXp = imported.outstandingDebtXp(),
        )

        private fun <K, V> changed(before: List<V>, after: List<V>, key: (V) -> K): Int {
            val old = before.associateBy(key)
            val new = after.associateBy(key)
            return (old.keys + new.keys).count { old[it] != new[it] }
        }
    }
}

data class PendingRestore(
    val snapshot: BackupSnapshot,
    val preview: RestorePreview,
)
