package xyz.winhok.earthonline.data

import xyz.winhok.earthonline.core.DebtRules
import xyz.winhok.earthonline.core.World

data class BackupSnapshot(
    val world: World,
    val narrativePreference: NarrativePreferenceEntity = NarrativePreferenceEntity(
        narrativeId = EarthDatabase.EARTH_NATIVE_NARRATIVE_ID,
    ),
    val contracts: List<ContractEntity> = emptyList(),
    val contractRevisions: List<ContractRevisionEntity> = emptyList(),
    val consequences: List<ConsequenceEventEntity> = emptyList(),
    val consequenceAdjustments: List<ConsequenceAdjustmentEntity> = emptyList(),
    val repaymentAllocations: List<RepaymentAllocationEntity> = emptyList(),
    val clockBoundaries: List<ClockBoundaryEntity> = emptyList(),
    val recoveryRoutes: List<RecoveryRouteEntity> = emptyList(),
    val recoveryNodes: List<RecoveryNodeEntity> = emptyList(),
    val progressHistory: ProgressHistoryEntity = ProgressHistoryEntity(),
    val effects: EffectPreferencesEntity = EffectPreferencesEntity(),
    val presentationPreferences: List<PresentationPreferenceEntity> = emptyList(),
    val settlementReceipts: List<SettlementReceiptEntity> = emptyList(),
) {
    fun displayNarrativeId(knownNarrativeIds: Set<String>): String =
        narrativePreference.narrativeId.takeIf { it in knownNarrativeIds }
            ?: EarthDatabase.EARTH_NATIVE_NARRATIVE_ID

    fun outstandingDebtXp(): Long = DebtRules.outstandingXp(
        consequences,
        consequenceAdjustments,
        repaymentAllocations,
    )
}
