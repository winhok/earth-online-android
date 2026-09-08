package xyz.winhok.earthonline.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
    val playerId: Int = 1,
    val narrativeId: String,
)

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
    val id: String,
    val questId: String,
    val occurrence: String,
    val zoneId: String,
    val dueDay: Long,
    val originalRewardXp: Long,
    val currentRewardXp: Long,
    val status: String,
    val signedAt: Long,
    val closedAt: Long? = null,
)

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
    val id: String,
    val contractId: String,
    val sequence: Int,
    val kind: String,
    val previousDueDay: Long?,
    val newDueDay: Long?,
    val previousRewardXp: Long,
    val newRewardXp: Long,
    val createdAt: Long,
    val idempotencyKey: String,
)

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
    val id: String,
    val contractId: String,
    val kind: String,
    val xp: Long,
    val effectiveDay: Long,
    val createdAt: Long,
    val idempotencyKey: String,
)

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
    val id: String,
    val consequenceId: String,
    val kind: String,
    val xp: Long,
    val reasonCategory: String?,
    val createdAt: Long,
    val idempotencyKey: String,
)

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
    val id: String,
    val consequenceId: String,
    val completionId: String,
    val xp: Long,
    val createdAt: Long,
    val idempotencyKey: String,
)

@Entity(tableName = "clock_boundaries", primaryKeys = ["id"])
data class ClockBoundaryEntity(
    val id: String,
    val zoneId: String,
    val lastSettledDay: Long,
    val lastSettledAt: Long,
)

@Entity(
    tableName = "recovery_routes",
    primaryKeys = ["id"],
    indices = [Index(value = ["idempotencyKey"], unique = true)],
)
data class RecoveryRouteEntity(
    val id: String,
    val status: String,
    val triggerKind: String,
    val targetDebtXp: Long,
    val openedAt: Long,
    val closedAt: Long?,
    val idempotencyKey: String,
)

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
    val routeId: String,
    val questId: String,
    val position: Int,
    val addedAt: Long,
    val completedAt: Long?,
)
