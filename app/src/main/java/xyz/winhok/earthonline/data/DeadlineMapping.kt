package xyz.winhok.earthonline.data

import xyz.winhok.earthonline.core.*

fun BackupSnapshot.deadlineState() = DeadlineState(world, DeadlineBook(
    contracts.map { TimeContract(it.id,it.questId,it.occurrence,it.zoneId,it.dueDay,it.originalRewardXp,it.currentRewardXp,it.status,it.signedAt,it.closedAt,it.signedKind,it.signedSkill,it.extensionCount,it.fulfilledAt) },
    contractRevisions.map { ContractRevision(it.id,it.contractId,it.sequence,it.kind,it.previousDueDay,it.newDueDay,it.previousRewardXp,it.newRewardXp,it.createdAt,it.idempotencyKey) },
    consequences.map { Liability(it.id,it.contractId,it.kind,it.xp,it.effectiveDay,it.createdAt,it.idempotencyKey) },
    consequenceAdjustments.map { LiabilityAdjustment(it.id,it.consequenceId,it.kind,it.xp,it.reasonCategory,it.createdAt,it.idempotencyKey) },
    repaymentAllocations.map { DebtAllocation(it.id,it.consequenceId,it.completionId,it.xp,it.createdAt,it.idempotencyKey,it.reversalOf) },
    clockBoundaries.map { BusinessClock(it.id,it.zoneId,it.lastSettledDay,it.lastSettledAt) },
    recoveryRoutes.map { RecoveryRoute(it.id,it.status,it.triggerKind,it.targetDebtXp,it.openedAt,it.closedAt,it.idempotencyKey,it.closedThroughAssessmentCount) },
    recoveryNodes.map { RecoveryNode(it.routeId,it.questId,it.position,it.addedAt,it.completedAt) },
    progressHistory.highestLevel,
))

fun BackupSnapshot.withDeadline(state: DeadlineState): BackupSnapshot = copy(
    world=state.world,
    contracts=state.book.contracts.map { ContractEntity(it.id,it.questId,it.occurrence,it.zoneId,it.dueDay,it.originalRewardXp,it.currentRewardXp,it.status,it.signedAt,it.closedAt,it.signedKind,it.signedSkill,it.extensionCount,it.fulfilledAt,if(it.status=="ACTIVE")it.questId else null) },
    contractRevisions=state.book.revisions.map { ContractRevisionEntity(it.id,it.contractId,it.sequence,it.kind,it.previousDueDay,it.newDueDay,it.previousRewardXp,it.newRewardXp,it.createdAt,it.idempotencyKey) },
    consequences=state.book.liabilities.map { ConsequenceEventEntity(it.id,it.contractId,it.kind,it.xp,it.effectiveDay,it.createdAt,it.idempotencyKey) },
    consequenceAdjustments=state.book.adjustments.map { ConsequenceAdjustmentEntity(it.id,it.consequenceId,it.kind,it.xp,it.reasonCategory,it.createdAt,it.idempotencyKey) },
    repaymentAllocations=state.book.allocations.map { RepaymentAllocationEntity(it.id,it.consequenceId,it.completionId,it.xp,it.createdAt,it.idempotencyKey,it.reversalOf) },
    clockBoundaries=state.book.clocks.map { ClockBoundaryEntity(it.id,it.zoneId,it.lastSettledDay,it.lastSettledAt) },
    recoveryRoutes=state.book.routes.map { RecoveryRouteEntity(it.id,it.status,it.triggerKind,it.targetDebtXp,it.openedAt,it.closedAt,it.idempotencyKey,it.closedThroughAssessmentCount) },
    recoveryNodes=state.book.nodes.map { RecoveryNodeEntity(it.routeId,it.questId,it.position,it.addedAt,it.completedAt) },
    progressHistory=ProgressHistoryEntity(highestLevel=state.book.highestLevel),
)
