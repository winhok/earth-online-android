package xyz.winhok.earthonline.data

import xyz.winhok.earthonline.core.BackupV2Graph
import xyz.winhok.earthonline.core.BackupV2Rules
import xyz.winhok.earthonline.core.BackupValidator

/** Bridges Room records to the pure :core backup rules. */
object BackupSnapshotValidator {
    fun validate(snapshot: BackupSnapshot) {
        BackupValidator.validate(snapshot.world)
        BackupV2Rules.validate(
            snapshot.world,
            BackupV2Graph(
                narrativePreference = snapshot.narrativePreference,
                contracts = snapshot.contracts,
                contractRevisions = snapshot.contractRevisions,
                consequences = snapshot.consequences,
                consequenceAdjustments = snapshot.consequenceAdjustments,
                repaymentAllocations = snapshot.repaymentAllocations,
                clockBoundaries = snapshot.clockBoundaries,
                recoveryRoutes = snapshot.recoveryRoutes,
                recoveryNodes = snapshot.recoveryNodes,
            ),
        )
    }
}
