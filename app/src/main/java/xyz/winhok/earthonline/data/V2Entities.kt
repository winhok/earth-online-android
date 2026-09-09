package xyz.winhok.earthonline.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import xyz.winhok.earthonline.core.ClockBoundaryRecord
import xyz.winhok.earthonline.core.ConsequenceAdjustmentRecord
import xyz.winhok.earthonline.core.ConsequenceRecord
import xyz.winhok.earthonline.core.ContractRecord
import xyz.winhok.earthonline.core.ContractRevisionRecord
import xyz.winhok.earthonline.core.NarrativePreferenceRecord
import xyz.winhok.earthonline.core.RecoveryNodeRecord
import xyz.winhok.earthonline.core.RecoveryRouteRecord
import xyz.winhok.earthonline.core.RepaymentAllocationRecord

@Entity(
    tableName = "narrative_preferences",
    primaryKeys = ["playerId"],
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class NarrativePreferenceEntity(
    override val playerId: Int = 1,
    override val narrativeId: String,
) : NarrativePreferenceRecord

@Entity(
    tableName = "contracts",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["questId", "occurrence"], unique = true),
        Index("dueDay"),
    ],
)
data class ContractEntity(
    override val id: String,
    override val questId: String,
    override val occurrence: String,
    override val zoneId: String,
    override val dueDay: Long,
    override val originalRewardXp: Long,
    override val currentRewardXp: Long,
    override val status: String,
    override val signedAt: Long,
    override val closedAt: Long? = null,
) : ContractRecord

@Entity(
    tableName = "contract_revisions",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = ContractEntity::class,
            parentColumns = ["id"],
            childColumns = ["contractId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["contractId", "sequence"], unique = true),
        Index(value = ["idempotencyKey"], unique = true),
    ],
)
data class ContractRevisionEntity(
    override val id: String,
    override val contractId: String,
    override val sequence: Int,
    override val kind: String,
    override val previousDueDay: Long?,
    override val newDueDay: Long?,
    override val previousRewardXp: Long,
    override val newRewardXp: Long,
    override val createdAt: Long,
    override val idempotencyKey: String,
) : ContractRevisionRecord

@Entity(
    tableName = "consequence_events",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = ContractEntity::class,
            parentColumns = ["id"],
            childColumns = ["contractId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("contractId"),
        Index(value = ["idempotencyKey"], unique = true),
    ],
)
data class ConsequenceEventEntity(
    override val id: String,
    override val contractId: String,
    override val kind: String,
    override val xp: Long,
    override val effectiveDay: Long,
    override val createdAt: Long,
    override val idempotencyKey: String,
) : ConsequenceRecord

@Entity(
    tableName = "consequence_adjustments",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = ConsequenceEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["consequenceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("consequenceId"),
        Index(value = ["idempotencyKey"], unique = true),
    ],
)
data class ConsequenceAdjustmentEntity(
    override val id: String,
    override val consequenceId: String,
    override val kind: String,
    override val xp: Long,
    override val reasonCategory: String?,
    override val createdAt: Long,
    override val idempotencyKey: String,
) : ConsequenceAdjustmentRecord

@Entity(
    tableName = "repayment_allocations",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = ConsequenceEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["consequenceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CompletionEntity::class,
            parentColumns = ["id"],
            childColumns = ["completionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["completionId", "consequenceId"], unique = true),
        Index("consequenceId"),
        Index(value = ["idempotencyKey"], unique = true),
    ],
)
data class RepaymentAllocationEntity(
    override val id: String,
    override val consequenceId: String,
    override val completionId: String,
    override val xp: Long,
    override val createdAt: Long,
    override val idempotencyKey: String,
) : RepaymentAllocationRecord

@Entity(tableName = "clock_boundaries", primaryKeys = ["id"])
data class ClockBoundaryEntity(
    override val id: String,
    override val zoneId: String,
    override val lastSettledDay: Long,
    override val lastSettledAt: Long,
) : ClockBoundaryRecord

@Entity(
    tableName = "recovery_routes",
    primaryKeys = ["id"],
    indices = [Index(value = ["idempotencyKey"], unique = true)],
)
data class RecoveryRouteEntity(
    override val id: String,
    override val status: String,
    override val triggerKind: String,
    override val targetDebtXp: Long,
    override val openedAt: Long,
    override val closedAt: Long?,
    override val idempotencyKey: String,
) : RecoveryRouteRecord

@Entity(
    tableName = "recovery_nodes",
    primaryKeys = ["routeId", "questId"],
    foreignKeys = [
        ForeignKey(
            entity = RecoveryRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("questId")],
)
data class RecoveryNodeEntity(
    override val routeId: String,
    override val questId: String,
    override val position: Int,
    override val addedAt: Long,
    override val completedAt: Long?,
) : RecoveryNodeRecord
