package xyz.winhok.earthonline.data

import xyz.winhok.earthonline.core.BackupV2Graph
import xyz.winhok.earthonline.core.BackupV2Rules
import xyz.winhok.earthonline.core.*

/** Bridges Room records to the pure :core backup rules. */
object BackupSnapshotValidator {
    fun validate(snapshot: BackupSnapshot) {
        val state = snapshot.deadlineState()
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
        DeadlineValidation.validate(state)
        BackupValidator.validate(snapshot.world, DeadlineValidation.rewardAllowList(state.book))
        if (snapshot.contracts.any { it.activeQuestId != if(it.status=="ACTIVE") it.questId else null })
            throw RuleViolation(RuleError.INVALID_BACKUP)
        if(snapshot.progressHistory.id!=1 || snapshot.effects.id!=1 || snapshot.presentationPreferences.size>128)
            throw RuleViolation(RuleError.INVALID_BACKUP)
        val prefs=snapshot.presentationPreferences
        if(prefs.map { it.narrativeId to it.preferenceKey }.distinct().size!=prefs.size || prefs.any {
            !it.narrativeId.matches(Regex("[A-Za-z0-9_.:-]{1,120}")) || it.preferenceKey !in setOf("intro_seen","story_collapsed")
        }) throw RuleViolation(RuleError.INVALID_BACKUP)
    }
}
