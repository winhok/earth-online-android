package xyz.winhok.earthonline.core

import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/** A stale confirmation is never permission to accept a different price or deadline. */
class DisclosureRequired(val disclosure: ContractDisclosure) : IllegalStateException("Contract confirmation required")

/**
 * Pure, deterministic-by-input accounting. The repository commits the resulting world and
 * ledger together. Presentation, timers and notification delivery never award or deduct XP.
 */
object DeadlineEngine {
    fun reconcile(state: DeadlineState, now: Long, newId: () -> String = { UUID.randomUUID().toString() }): DeadlineState =
        Ledger(state, now, newId).apply { settleDue(); balance(); recover() }.finish()

    fun preview(state: DeadlineState, command: DeadlineCommand, now: Long): ContractDisclosure? =
        Ledger(reconcile(state, now), now) { UUID.randomUUID().toString() }.disclosure(command)

    fun execute(state: DeadlineState, command: DeadlineCommand, now: Long,
                accepted: ContractDisclosure? = null, newId: () -> String = { UUID.randomUUID().toString() }): DeadlineOutcome {
        val ledger = Ledger(state, now, newId)
        ledger.settleDue(); ledger.balance(); ledger.recover()
        val required = ledger.disclosure(command)
        if (required != null && required != accepted) throw DisclosureRequired(required)
        val before = ledger.book.liabilities.sumOf { it.xp }
        val previous = ledger.world.completions.associateBy { it.id }
        ledger.apply(command)
        ledger.balance(); ledger.recover()
        val final = ledger.finish()
        val completion = when (command) {
            is DeadlineCommand.Complete -> final.world.completions.first { it.id == QuestRules.completionId(ledger.quest(command.questId), ledger.day) }
            else -> null
        }
        val changed = completion == null || previous[completion.id]?.revokedAt != null || previous[completion.id] == null
        val allocated = completion?.let { c -> final.book.activeAllocations().filter { it.completionId == c.id }.sumOf { it.xp } } ?: 0
        return DeadlineOutcome(final, completion, changed, if (changed) completion?.xp ?: 0 else 0,
            allocated, final.book.liabilities.sumOf { it.xp } - before)
    }

    private class Ledger(state: DeadlineState, rawNow: Long, val id: () -> String) {
        var world = state.world
        var book = state.book.copy(highestLevel = maxOf(state.book.highestLevel, state.book.progress(state.world).current.level))
        val now = book.effectiveNow(rawNow)
        val day get() = world.dayAt(now)
        private val original = state
        init {
            val warningId = "clock-warning:${world.dayAt(now)}"
            if(rawNow < now && book.clocks.none { it.id==warningId }) book=book.copy(clocks=book.clocks +
                BusinessClock(warningId,world.player.zoneId,world.dayAt(now),rawNow.coerceAtLeast(0)))
        }

        fun quest(id: String) = world.quests.firstOrNull { it.id == id } ?: throw RuleViolation(RuleError.MISSING_QUEST)
        private fun put(q: Quest) { world = world.copy(quests = world.quests.filterNot { it.id == q.id } + q) }
        private fun put(c: TimeContract) { book = book.copy(contracts = if (book.contracts.any { it.id == c.id }) book.contracts.map { if (it.id == c.id) c else it } else book.contracts + c) }
        private fun done(q: Quest) = QuestRules.activeCompletion(q, world.completions, day)
        private fun active(q: Quest) = book.active(q.id)
        private fun hasHistory(q: Quest) = world.completions.any { it.questId == q.id }
        private fun dated(d: QuestDraft) = d.kind != QuestKind.DAILY && d.dueDay != null
        private fun currentReward(q: Quest): Int = (if (q.kind == QuestKind.DAILY) null else book.latest(q.id))?.currentRewardXp?.toInt() ?: QuestRules.reward(q)
        private fun draft(q: Quest) = QuestDraft(q.id, q.title, q.description, q.kind, q.difficulty, q.skill, q.priority,
            q.estimatedMinutes, q.dueDay, q.goalId)
        private fun postDraft(q: Quest): QuestDraft = draft(q).copy(dueDay = (q.dueDay ?: day) + 1)

        fun settleDue() {
            book.contracts.filter { it.status == "ACTIVE" && now >= it.boundary() }.forEach { c -> assess(c, "OVERDUE") }
        }

        private fun assess(c: TimeContract, kind: String) {
            if (book.liabilities.any { it.contractId == c.id }) return
            val cost = if (kind == "OVERDUE") c.originalRewardXp else ContractMath.abandonment(c.originalRewardXp)
            put(c.copy(status = kind, closedAt = now))
            val assessment = Liability(id(), c.id, kind, cost,
                if (kind == "OVERDUE") c.dueDay + 1 else c.dayAt(now), now, "assess:${c.id}")
            book = book.copy(liabilities = book.liabilities + assessment)
        }

        private fun revision(c: TimeContract, kind: String, due: Long?, reward: Long) {
            val sequence = (book.revisions.filter { it.contractId == c.id }.maxOfOrNull { it.sequence } ?: 0) + 1
            book = book.copy(revisions = book.revisions + ContractRevision(id(), c.id, sequence, kind,
                c.dueDay, due, c.currentRewardXp, reward, now, "revision:${c.id}:$sequence"))
        }

        private fun sign(q: Quest) {
            val due = requireNotNull(q.dueDay)
            requireRule(q.kind != QuestKind.DAILY && due >= day, RuleError.DATE)
            val reward = QuestRules.reward(q).toLong()
            val c = TimeContract(id(), q.id, zoneId = world.player.zoneId, dueDay = due,
                originalRewardXp = reward, currentRewardXp = reward, signedAt = now,
                signedKind = q.kind, signedSkill = q.skill, titleSnapshot = q.title)
            put(c); revision(c, "SIGNED", due, reward)
        }

        private fun editDisclosure(command: DeadlineCommand.Save): ContractDisclosure? {
            val d = command.draft
            QuestRules.validate(d)
            val q = d.id?.let(::quest)
            if (q != null && done(q) != null) return null // Fulfilled history is not a live promise.
            val c = q?.let(::active)
            val zone = c?.zoneId ?: world.player.zoneId
            val today = Instant.ofEpochMilli(now).atZone(ZoneId.of(zone)).toLocalDate().toEpochDay()
            val lockedChanged = c != null && (d.kind != c.signedKind || d.skill != c.signedSkill ||
                QuestRules.reward(QuestRules.edit(q, d, command.newId, now, false)).toLong() != c.originalRewardXp)
            val later = c != null && d.dueDay != null && d.dueDay > c.dueDay
            val resign = c != null && (lockedChanged || (later && c.extensionCount >= 3))
            val abandon = c != null && (!dated(d) || resign)
            val unsignedDateChanged = q == null || command.calibrate || d.dueDay != q.dueDay
            val signing = dated(d) && (resign || (c == null && unsignedDateChanged))
            if ((signing || later || (c != null && d.dueDay != c.dueDay)) && dated(d)) {
                requireRule(d.dueDay!! >= today, RuleError.DATE)
            }
            if (!abandon && !signing && !later) return null
            val r = if (signing) (d.difficulty.xp + if (d.kind == QuestKind.BOSS) 25 else 0).toLong()
                else c?.originalRewardXp ?: 0
            val count = if (later && !resign) c.extensionCount + 1 else 0
            return ContractDisclosure(buildSet {
                if (abandon) add(DisclosureKind.ABANDON)
                if (signing) add(if (q == null || book.latest(q.id) == null) DisclosureKind.SIGN else DisclosureKind.RECOMMIT)
                if (later && !resign) add(DisclosureKind.EXTEND)
            }, q?.id ?: command.newId, d.title.trim(), if (abandon) ContractMath.abandonment(c.originalRewardXp) else 0,
                if (dated(d)) ContractMath.attainable(r, count) else 0, if (dated(d)) r else 0, d.dueDay,
                zone, d.dueDay == today, count, contractId = c?.id)
        }

        fun disclosure(command: DeadlineCommand): ContractDisclosure? = when (command) {
            is DeadlineCommand.Save -> editDisclosure(command)
            is DeadlineCommand.Postpone -> {
                val q = quest(command.questId)
                if (active(q) != null) editDisclosure(DeadlineCommand.Save(postDraft(q), q.id)) else null
            }
            is DeadlineCommand.SetState -> {
                val q = quest(command.questId); val c = active(q)
                if (c != null && command.state != QuestState.ACTIVE && done(q) == null)
                    ContractDisclosure(setOf(DisclosureKind.ABANDON), q.id, q.title,
                        immediateCost = ContractMath.abandonment(c.originalRewardXp), zoneId = c.zoneId, contractId = c.id)
                else null
            }
            is DeadlineCommand.Undo -> {
                val completion = world.completions.firstOrNull { it.id == command.completionId && it.revokedAt == null }
                val c = completion?.takeIf { it.kind != QuestKind.DAILY }?.let { book.latest(it.questId) }
                if (c != null && c.status == "FULFILLED" && now >= c.boundary())
                    ContractDisclosure(setOf(DisclosureKind.POST_DEADLINE_UNDO), c.questId, quest(c.questId).title,
                        immediateCost = c.originalRewardXp, reward = completion.xp.toLong(), zoneId = c.zoneId, contractId = c.id)
                else if (c != null && c.status == "FULFILLED") {
                    val q = quest(c.questId)
                    if (q.state != QuestState.ACTIVE || q.dueDay != c.dueDay || q.skill != c.signedSkill)
                        ContractDisclosure(setOf(DisclosureKind.REOPEN), c.questId, q.title,
                            reward = completion.xp.toLong(), overdueCost = c.originalRewardXp,
                            dueDay = c.dueDay, zoneId = c.zoneId, contractId = c.id)
                    else null
                } else null
            }
            is DeadlineCommand.Waive -> {
                val c = book.contracts.firstOrNull { it.id == command.contractId } ?: throw RuleViolation(RuleError.MISSING_QUEST)
                val cost = book.liabilities.firstOrNull { it.contractId == c.id }
                val remainingCost = cost?.let { it.xp - book.adjustments.filter { a -> a.consequenceId == it.id && a.kind in setOf("WAIVER", "RESTITUTION") }.sumOf { a -> a.xp } } ?: 0
                if (c.status == "EXEMPTED" || c.status == "FULFILLED" || (cost != null && remainingCost == 0L)) null
                else {
                    val paid = cost?.let { l -> book.activeAllocations().filter { it.consequenceId == l.id }.sumOf { it.xp } } ?: 0
                    ContractDisclosure(setOf(DisclosureKind.FORCE_MAJEURE), c.questId, quest(c.questId).title,
                        zoneId = c.zoneId, waivedXp = (remainingCost - paid).coerceAtLeast(0), restitutionXp = paid, contractId = c.id)
                }
            }
            else -> null
        }

        private fun save(command: DeadlineCommand.Save) {
            val d = command.draft
            val old = d.id?.let(::quest)
            requireRule(old != null || world.quests.size < QuestRules.MAX_QUESTS, RuleError.LIMIT)
            requireRule(d.goalId == null || world.goals.any { it.id == d.goalId && (!it.archived || old?.goalId == it.id) }, RuleError.MISSING_GOAL)
            val q = QuestRules.edit(old, d, command.newId, now, old?.let(::hasHistory) == true)
            val c = old?.let(::active)
            val closed = old?.let(::done) != null
            if (!closed) {
                if (c == null) {
                    // Old unsigned tasks remain free until the player explicitly saves reviewed terms.
                    if (dated(d) && q.state == QuestState.ACTIVE &&
                        (old == null || command.calibrate || d.dueDay != old.dueDay)) sign(q)
                } else {
                    val lockedChanged = q.kind != c.signedKind || q.skill != c.signedSkill || QuestRules.reward(q).toLong() != c.originalRewardXp
                    val later = q.dueDay != null && q.dueDay > c.dueDay
                    if (!dated(d) || lockedChanged || (later && c.extensionCount >= 3)) {
                        assess(c, "ABANDONED"); revision(c, "ABANDONED", c.dueDay, c.currentRewardXp)
                        if (dated(d) && q.state == QuestState.ACTIVE) sign(q)
                    } else {
                        val count = c.extensionCount + if (later) 1 else 0
                        val changed = c.copy(dueDay = q.dueDay!!, currentRewardXp = ContractMath.attainable(c.originalRewardXp, count), extensionCount = count)
                        revision(c, if (later) "POSTPONED" else if (c.dueDay != q.dueDay) "RESCHEDULED" else "EDITED", changed.dueDay, changed.currentRewardXp)
                        put(changed)
                    }
                }
            }
            put(q)
            event(if (old == null) EventKind.CREATED else EventKind.EDITED, q.title, q.id)
        }

        private fun reverse(allocation: DebtAllocation) {
            if (book.allocations.any { it.reversalOf == allocation.id }) return
            book = book.copy(allocations = book.allocations + allocation.copy(id = id(), createdAt = now,
                idempotencyKey = "reverse:${allocation.id}", reversalOf = allocation.id))
        }

        fun apply(command: DeadlineCommand) {
            when (command) {
                is DeadlineCommand.Save -> save(command)
                is DeadlineCommand.Complete -> {
                    val q = quest(command.questId)
                    val key = QuestRules.completionId(q, day)
                    val old = world.completions.firstOrNull { it.id == key }
                    var value = QuestRules.complete(q, old, now, day)
                    if (old != null && old.revokedAt == null) return
                    requireRule(old != null || world.completions.size < QuestRules.MAX_COMPLETIONS, RuleError.LIMIT)
                    val c = if (q.kind == QuestKind.DAILY) null else book.latest(q.id)
                    if (old == null && c != null) value = value.copy(xp = c.currentRewardXp.toInt(), skill = c.signedSkill)
                    world = world.copy(completions = world.completions.filterNot { it.id == key } + value)
                    if (c != null) put(c.copy(status = if (c.status == "ACTIVE") "FULFILLED" else c.status,
                        closedAt = c.closedAt ?: now, fulfilledAt = now))
                    event(EventKind.COMPLETED, value.title, q.id, value.xp)
                    book = book.copy(nodes = book.nodes.map { if (it.questId == q.id && it.completedAt == null && book.routes.any { r -> r.id == it.routeId && r.status == "OPEN" }) it.copy(completedAt = now) else it })
                }
                is DeadlineCommand.Undo -> {
                    val c = world.completions.firstOrNull { it.id == command.completionId && it.revokedAt == null } ?: return
                    book.activeAllocations().filter { it.completionId == c.id }.forEach(::reverse)
                    world = world.copy(completions = world.completions.map { if (it.id == c.id) it.copy(revokedAt = now) else it })
                    (if (c.kind == QuestKind.DAILY) null else book.latest(c.questId))?.let { contract ->
                        if (contract.status == "FULFILLED") {
                            val reopen = contract.copy(status = "ACTIVE", closedAt = null, fulfilledAt = null)
                            put(reopen)
                            if (now >= reopen.boundary()) assess(reopen, "OVERDUE")
                        } else put(contract.copy(fulfilledAt = null))
                    }
                    book = book.copy(nodes = book.nodes.map { if (it.questId == c.questId && it.completedAt == c.completedAt && book.routes.any { r -> r.id == it.routeId && r.status == "OPEN" }) it.copy(completedAt = null) else it })
                    event(EventKind.UNDONE, c.title, c.questId, -c.xp)
                }
                is DeadlineCommand.Postpone -> {
                    val old = quest(command.questId)
                    requireRule(old.state == QuestState.ACTIVE, RuleError.INACTIVE)
                    requireRule(done(old) == null, RuleError.NOT_AVAILABLE)
                    val until = if (old.kind == QuestKind.DAILY) day + 1
                        else maxOf(day, old.snoozedUntilDay ?: day) + 1
                    requireRule(until <= QuestRules.MAX_DAY && old.postponeCount < 1_000_000, RuleError.LIMIT)
                    if (active(old) != null) save(DeadlineCommand.Save(postDraft(old), old.id))
                    put(quest(old.id).copy(snoozedUntilDay = until, postponeCount = old.postponeCount + 1, updatedAt = now))
                    event(EventKind.POSTPONED, old.title, old.id)
                }
                is DeadlineCommand.SetState -> {
                    val old = quest(command.questId)
                    if (command.state != QuestState.ACTIVE) active(old)?.let { assess(it, "ABANDONED") }
                    put(old.copy(state = command.state, updatedAt = now,
                        postponeCount = if (command.state == QuestState.ACTIVE) 0 else old.postponeCount,
                        snoozedUntilDay = if (command.state == QuestState.ACTIVE) null else old.snoozedUntilDay))
                    event(when (command.state) { QuestState.ACTIVE -> EventKind.RESUMED; QuestState.PAUSED -> EventKind.PAUSED; QuestState.ARCHIVED -> EventKind.ARCHIVED }, old.title, old.id)
                }
                is DeadlineCommand.Waive -> {
                    val c = book.contracts.first { it.id == command.contractId }
                    val disclosure = disclosure(command) ?: return
                    val l = book.liabilities.firstOrNull { it.contractId == c.id }
                    if (l == null) {
                        put(c.copy(status = "EXEMPTED", closedAt = now))
                        revision(c, "EXEMPTED_${command.reason.name}", c.dueDay, c.currentRewardXp)
                    } else {
                        book.activeAllocations().filter { it.consequenceId == l.id }.forEach(::reverse)
                        fun add(kind: String, xp: Long) { if (xp > 0) book = book.copy(adjustments = book.adjustments +
                            LiabilityAdjustment(id(), l.id, kind, xp, command.reason.name, now, "${kind.lowercase()}:${l.id}")) }
                        add("WAIVER", disclosure.waivedXp); add("RESTITUTION", disclosure.restitutionXp)
                    }
                }
                is DeadlineCommand.SelectRecovery -> {
                    val route = book.routes.singleOrNull { it.status == "OPEN" } ?: throw RuleViolation(RuleError.NOT_AVAILABLE)
                    requireRule(command.questIds.distinct().size == command.questIds.size, RuleError.NOT_AVAILABLE)
                    val quests = command.questIds.map(::quest)
                    requireRule(quests.all { QuestRules.available(it, day) && done(it) == null &&
                        book.nodes.none { node -> node.routeId == route.id && node.questId == it.id && node.completedAt != null }
                    }, RuleError.NOT_AVAILABLE)
                    requireRule(quests.sumOf { currentReward(it).toLong() } >= book.progress(world).debtXp, RuleError.NOT_AVAILABLE)
                    book = book.copy(nodes = book.nodes.filterNot { it.routeId == route.id && it.completedAt == null } +
                        quests.mapIndexed { index, q -> RecoveryNode(route.id, q.id, index + (book.nodes.maxOfOrNull { it.position } ?: 0) + 1, now) })
                }
            }
        }

        private fun event(kind: EventKind, text: String, questId: String?, xp: Int = 0) {
            requireRule(world.events.size < QuestRules.MAX_EVENTS, RuleError.LIMIT)
            world = world.copy(events = world.events + JournalEvent(id(), kind, text, now, questId, xp))
        }

        /** Spend only real, unallocated completion credit; prefer a completion's own contract debt. */
        fun balance() {
            val contractQuest = book.contracts.associate { it.id to it.questId }
            world.completions.filter { it.revokedAt == null }.sortedWith(compareBy<Completion> { it.completedAt }.thenBy { it.id }).forEach { c ->
                var available = c.xp - book.activeAllocations().filter { it.completionId == c.id }.sumOf { it.xp }
                val liabilities = book.liabilities.sortedWith(compareBy<Liability> { if (contractQuest[it.contractId] == c.questId) 0 else 1 }
                    .thenBy { it.createdAt }.thenBy { it.id })
                for (l in liabilities) {
                    val pay = minOf(available, book.outstanding().getValue(l.id))
                    if (pay <= 0) continue
                    val key = id()
                    book = book.copy(allocations = book.allocations + DebtAllocation(key, l.id, c.id, pay, now, "allocation:$key"))
                    available -= pay
                    if (available == 0L) break
                }
            }
            book = book.copy(highestLevel = maxOf(book.highestLevel, book.progress(world).current.level))
        }

        fun recover() {
            val debt = book.progress(world).debtXp
            val full = book.liabilities.filter { it.kind == "OVERDUE" && book.adjustments.none { a -> a.consequenceId == it.id && a.kind in setOf("WAIVER", "RESTITUTION") } }
            val open = book.routes.singleOrNull { it.status == "OPEN" }
            if (open != null && debt == 0L) {
                book = book.copy(routes = book.routes.map { if (it.id == open.id) it.copy(status = "CLOSED", closedAt = now,
                    closedThroughAssessmentCount = book.liabilities.count { l -> l.kind == "OVERDUE" }) else it })
                return
            }
            if (open != null || debt == 0L) return
            val last = book.routes.filter { it.status == "CLOSED" }.maxByOrNull { it.closedAt ?: 0 }
            if (last != null && book.liabilities.count { it.kind == "OVERDUE" } <= last.closedThroughAssessmentCount) return
            val recent = full.count { l -> book.contracts.first { it.id == l.contractId }.let { c -> c.dayAt(now) - l.effectiveDay in 0..29 } }
            val trigger = when { recent >= 3 -> "FULL_BREACH_COUNT"; debt >= book.progress(world).current.needed -> "DEBT_LEVEL"; else -> return }
            val key = id()
            book = book.copy(routes = book.routes + RecoveryRoute(key, triggerKind = trigger, targetDebtXp = debt,
                openedAt = now, idempotencyKey = "recovery:$key"))
        }

        fun finish(): DeadlineState {
            // Persist a high-water instant only when an operation or a business day actually changed.
            // A repeated refresh does not generate events, allocations or continuous DB invalidations.
            val last = book.clocks.maxByOrNull { it.lastSettledAt }
            if (world != original.world || book != original.book || last == null || day > last.lastSettledDay) {
                book = book.copy(clocks = book.clocks.filter { it.id.startsWith("clock-warning:") }.takeLast(1000) + BusinessClock(zoneId = world.player.zoneId, lastSettledDay = day, lastSettledAt = now))
            }
            return DeadlineState(world, book)
        }
    }
}
