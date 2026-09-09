package xyz.winhok.earthonline.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Neutral, immutable domain DTOs. Android entities only map these values to Room. */
data class TimeContract(
    override val id: String,
    override val questId: String,
    override val occurrence: String = "once",
    override val zoneId: String,
    override val dueDay: Long,
    override val originalRewardXp: Long,
    override val currentRewardXp: Long,
    override val status: String = "ACTIVE",
    override val signedAt: Long,
    override val closedAt: Long? = null,
    val signedKind: QuestKind = QuestKind.SIDE,
    val signedSkill: Skill = Skill.DISCIPLINE,
    val extensionCount: Int = 0,
    val fulfilledAt: Long? = null,
    val titleSnapshot: String = "",
) : ContractRecord {
    fun boundary(): Long = LocalDate.ofEpochDay(dueDay).plusDays(1)
        .atStartOfDay(ZoneId.of(zoneId)).toInstant().toEpochMilli()
    fun dayAt(now: Long): Long = Instant.ofEpochMilli(now).atZone(ZoneId.of(zoneId)).toLocalDate().toEpochDay()
}

data class ContractRevision(
    override val id: String, override val contractId: String, override val sequence: Int,
    override val kind: String, override val previousDueDay: Long?, override val newDueDay: Long?,
    override val previousRewardXp: Long, override val newRewardXp: Long,
    override val createdAt: Long, override val idempotencyKey: String,
) : ContractRevisionRecord

data class Liability(
    override val id: String, override val contractId: String, override val kind: String,
    override val xp: Long, override val effectiveDay: Long, override val createdAt: Long,
    override val idempotencyKey: String,
) : ConsequenceRecord

data class LiabilityAdjustment(
    override val id: String, override val consequenceId: String, override val kind: String,
    override val xp: Long, override val reasonCategory: String?, override val createdAt: Long,
    override val idempotencyKey: String,
) : ConsequenceAdjustmentRecord

data class DebtAllocation(
    override val id: String, override val consequenceId: String, override val completionId: String,
    override val xp: Long, override val createdAt: Long, override val idempotencyKey: String,
    override val reversalOf: String? = null,
) : RepaymentAllocationRecord

data class BusinessClock(
    override val id: String = "business-clock", override val zoneId: String,
    override val lastSettledDay: Long, override val lastSettledAt: Long,
) : ClockBoundaryRecord

data class RecoveryRoute(
    override val id: String, override val status: String = "OPEN", override val triggerKind: String,
    override val targetDebtXp: Long, override val openedAt: Long, override val closedAt: Long? = null,
    override val idempotencyKey: String, val closedThroughAssessmentCount: Int = 0,
) : RecoveryRouteRecord

data class RecoveryNode(
    override val routeId: String, override val questId: String, override val position: Int,
    override val addedAt: Long, override val completedAt: Long? = null,
) : RecoveryNodeRecord

data class DeadlineBook(
    val contracts: List<TimeContract> = emptyList(),
    val revisions: List<ContractRevision> = emptyList(),
    val liabilities: List<Liability> = emptyList(),
    val adjustments: List<LiabilityAdjustment> = emptyList(),
    val allocations: List<DebtAllocation> = emptyList(),
    val clocks: List<BusinessClock> = emptyList(),
    val routes: List<RecoveryRoute> = emptyList(),
    val nodes: List<RecoveryNode> = emptyList(),
    val highestLevel: Int = 1,
) {
    fun active(questId: String): TimeContract? = contracts.singleOrNull { it.questId == questId && it.status == "ACTIVE" }
    fun latest(questId: String): TimeContract? = contracts.filter { it.questId == questId }
        .maxWithOrNull(compareBy<TimeContract> { it.signedAt }.thenBy { contracts.indexOf(it) })
    fun effectiveNow(rawNow: Long): Long = maxOf(rawNow, clocks.maxOfOrNull { it.lastSettledAt } ?: 0L)
    fun activeAllocations(): List<DebtAllocation> {
        val reversed = allocations.mapNotNull { it.reversalOf }.toHashSet()
        return allocations.filter { it.reversalOf == null && it.id !in reversed }
    }
    fun outstanding(): Map<String, Long> {
        val repaid = activeAllocations().groupBy { it.consequenceId }.mapValues { (_, a) -> a.sumOf { it.xp } }
        val changes = adjustments.groupBy { it.consequenceId }
        return liabilities.associate { c ->
            val a = changes[c.id].orEmpty()
            // REFUND is retained for the already-defined v2 protocol. New commands use explicit reversals.
            val cost = c.xp - a.filter { it.kind in setOf("WAIVER", "RESTITUTION") }.sumOf { it.xp } +
                a.filter { it.kind == "REFUND" }.sumOf { it.xp }
            c.id to (cost - (repaid[c.id] ?: 0)).coerceAtLeast(0)
        }
    }
    fun progress(world: World): EffectiveProgress {
        val earned = world.completions.filter { it.revokedAt == null }.sumOf { it.xp.toLong() }
        val waived = adjustments.filter { it.kind in setOf("WAIVER", "RESTITUTION") }.sumOf { it.xp }
        val legacyRefunds = adjustments.filter { it.kind == "REFUND" }.sumOf { it.xp }
        val balance = earned - liabilities.sumOf { it.xp } + waived - legacyRefunds
        val current = ProgressRules.fromXp(balance.coerceAtLeast(0))
        return EffectiveProgress(current, earned, (-balance).coerceAtLeast(0), maxOf(highestLevel, current.level))
    }
}

data class EffectiveProgress(val current: Progress, val earnedXp: Long, val debtXp: Long, val highestLevel: Int) {
    val outOfOrder: Boolean get() = debtXp > 0
}

data class DeadlineState(val world: World, val book: DeadlineBook = DeadlineBook())

enum class ForceMajeureReason { HEALTH, FAMILY, EXTERNAL, OTHER }

sealed interface DeadlineCommand {
    data class Save(val draft: QuestDraft, val newId: String, val calibrate: Boolean = false) : DeadlineCommand
    data class Complete(val questId: String) : DeadlineCommand
    data class Undo(val completionId: String) : DeadlineCommand
    data class Postpone(val questId: String) : DeadlineCommand
    data class SetState(val questId: String, val state: QuestState) : DeadlineCommand
    data class Waive(val contractId: String, val reason: ForceMajeureReason) : DeadlineCommand
    data class SelectRecovery(val questIds: List<String>) : DeadlineCommand
}

enum class DisclosureKind { SIGN, EXTEND, ABANDON, RECOMMIT, POST_DEADLINE_UNDO, FORCE_MAJEURE }

data class ContractDisclosure(
    val kinds: Set<DisclosureKind>, val questId: String, val title: String,
    val immediateCost: Long = 0, val reward: Long = 0, val overdueCost: Long = 0,
    val dueDay: Long? = null, val zoneId: String, val dueToday: Boolean = false,
    val extensionCount: Int = 0, val waivedXp: Long = 0, val restitutionXp: Long = 0,
    val contractId: String? = null,
)

data class PlannedCommand(val command: DeadlineCommand, val disclosure: ContractDisclosure?)

data class DeadlineOutcome(
    val state: DeadlineState, val completion: Completion? = null, val changed: Boolean = true,
    val rewardXp: Int = 0, val repaidXp: Long = 0, val assessedXp: Long = 0,
)

object ContractMath {
    fun attainable(original: Long, extensions: Int): Long {
        require(original in setOf(10L, 25L, 35L, 50L, 75L) && extensions >= 0)
        return (original * (100L - 20L * extensions.coerceAtMost(3)) + 99L) / 100L
    }
    fun abandonment(original: Long): Long = (original + 1) / 2
    fun inferExtensions(original: Long, current: Long): Int =
        (0..3).firstOrNull { attainable(original, it) == current } ?: 0
}
