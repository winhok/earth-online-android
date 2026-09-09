package xyz.winhok.earthonline.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Executable production-rule scenarios, also called by JUnit on CI. */
object DeadlineScenarios {
    private val start = Instant.parse("2026-09-09T10:00:00Z").toEpochMilli()
    private val today = LocalDate.of(2026, 9, 9).toEpochDay()
    private fun initial() = DeadlineState(World(player = Player("Tester", "Reality", "UTC", start, true)))
    private fun apply(s: DeadlineState, c: DeadlineCommand, t: Long = start) =
        DeadlineEngine.execute(s, c, t, DeadlineEngine.preview(s, c, t)).state
    private fun save(s: DeadlineState, name: String, due: Long? = today, kind: QuestKind = QuestKind.SIDE,
                     difficulty: Difficulty = Difficulty.NORMAL): DeadlineState =
        apply(s, DeadlineCommand.Save(QuestDraft(title = name, dueDay = due, kind = kind, difficulty = difficulty), name))
    private fun completion(s: DeadlineState, q: String, t: Long = start) = apply(s, DeadlineCommand.Complete(q), t)
    private fun draft(q: Quest) = QuestDraft(q.id, q.title, q.description, q.kind, q.difficulty, q.skill,
        q.priority, q.estimatedMinutes, q.dueDay, q.goalId)
    private fun invalid(block: () -> Unit) {
        try { block() } catch (_: RuleViolation) { return }
        error("Expected a rule violation")
    }
    fun run(report: (String) -> Unit = {}): Int {
        var count = 0
        fun test(name: String, body: () -> Unit) { body(); count++; report("PASS $name") }
        test("new dated task requires an exact confirmation") {
            val command = DeadlineCommand.Save(QuestDraft(title = "a", dueDay = today), "a")
            val p = DeadlineEngine.preview(initial(), command, start)!!
            check(p.reward == 25L && p.overdueCost == 25L && p.dueToday)
            try { DeadlineEngine.execute(initial(), command, start); error("No confirmation") } catch (_: DisclosureRequired) { }
            try { DeadlineEngine.execute(initial(), command, start, p.copy(overdueCost = 0)); error("Wrong confirmation") } catch (_: DisclosureRequired) { }
        }
        test("undated and daily never sign contracts") {
            val s = save(save(initial(), "a", null), "b", today, QuestKind.DAILY)
            check(s.book.contracts.isEmpty())
            check(DeadlineEngine.reconcile(s, start + 500 * 86_400_000L).book.liabilities.isEmpty())
        }
        test("past deadline rejected before signing") { invalid { save(initial(), "a", today - 1) } }
        test("boundary is exclusive and assessment runs once") {
            val s = save(initial(), "a"); val boundary = s.book.contracts.single().boundary()
            check(DeadlineEngine.reconcile(s, boundary - 1).book.liabilities.isEmpty())
            val r = DeadlineEngine.reconcile(s, boundary)
            check(r.book.liabilities.single().xp == 25L)
            val r2 = DeadlineEngine.reconcile(r, boundary + 300 * 86_400_000L)
            check(r2.book.liabilities == r.book.liabilities)
        }
        test("offline batch has no aggregate daily cap") {
            var s = initial(); repeat(30) { s = save(s, "q$it") }
            s = DeadlineEngine.reconcile(s, start + 86_400_000)
            check(s.book.liabilities.size == 30 && s.book.progress(s.world).debtXp == 750L)
        }
        test("DST boundary uses captured zone") {
            val day = LocalDate.of(2026,3,8).toEpochDay()
            val c = TimeContract("a", "q", zoneId="America/New_York", dueDay=day, originalRewardXp=25, currentRewardXp=25, signedAt=0)
            check(c.boundary() == Instant.parse("2026-03-09T04:00:00Z").toEpochMilli())
        }
        test("on-time completion is fulfilled without liability") {
            val s = completion(save(initial(), "a"), "a")
            check(s.book.contracts.single().status == "FULFILLED")
            check(DeadlineEngine.reconcile(s, start+86_400_000).book.liabilities.isEmpty())
        }
        test("late completion keeps assessment and repays its own debt") {
            val s = completion(save(initial(), "a"), "a", start+86_400_000)
            check(s.book.contracts.single().status == "OVERDUE" && s.book.contracts.single().fulfilledAt != null)
            check(s.book.progress(s.world).current.xp == 0L && s.book.progress(s.world).debtXp == 0L)
            check(s.book.activeAllocations().sumOf { it.xp } == 25L)
        }
        test("extension schedule rounds upward and bottoms at forty percent") {
            for (r in listOf(10L,25L,35L,50L,75L)) for (n in 0..20) {
                check(ContractMath.attainable(r,n) == kotlin.math.ceil(r * maxOf(.4,1-.2*n)).toLong())
            }
        }
        test("third extension requires review fourth abandons and recommits") {
            var s=save(initial(),"a")
            repeat(3) { s=apply(s,DeadlineCommand.Postpone("a")) }
            check(s.book.active("a")!!.extensionCount==3 && s.book.active("a")!!.currentRewardXp==10L)
            val p=DeadlineEngine.preview(s,DeadlineCommand.Postpone("a"),start)!!
            check(p.immediateCost==13L && DisclosureKind.RECOMMIT in p.kinds)
            s=apply(s,DeadlineCommand.Postpone("a"))
            check(s.book.contracts.size==2 && s.book.liabilities.single().xp==13L)
            check(s.book.active("a")!!.currentRewardXp==25L)
        }
        test("shortening deadline retains the reduced reward") {
            var s=save(initial(),"a",today+4); s=apply(s,DeadlineCommand.Postpone("a"))
            val d=draft(s.world.quests.single()).copy(dueDay=today+2)
            s=apply(s,DeadlineCommand.Save(d,"unused"))
            check(s.book.active("a")!!.currentRewardXp==20L && s.book.active("a")!!.extensionCount==1)
        }
        test("title priority duration do not reprice a promise") {
            var s=save(initial(),"a")
            val d=draft(s.world.quests.single()).copy(title="renamed",priority=3,estimatedMinutes=400)
            check(DeadlineEngine.preview(s,DeadlineCommand.Save(d,"unused"),start)==null)
            s=apply(s,DeadlineCommand.Save(d,"unused")); check(s.book.contracts.size==1 && s.book.liabilities.isEmpty())
        }
        test("clearing deadline charges only disclosed half original") {
            var s=save(initial(),"a")
            s=apply(s,DeadlineCommand.Save(draft(s.world.quests.single()).copy(dueDay=null),"unused"))
            check(s.book.liabilities.single().xp==13L && s.world.quests.single().dueDay==null)
        }
        test("pause before boundary is abandonment") {
            val s=apply(save(initial(),"a"),DeadlineCommand.SetState("a",QuestState.PAUSED))
            check(s.book.liabilities.single().kind=="ABANDONED")
        }
        test("archive after boundary cannot charge a second cost") {
            var s=apply(save(initial(),"a"),DeadlineCommand.SetState("a",QuestState.ARCHIVED),start+86_400_000)
            s=apply(s,DeadlineCommand.SetState("a",QuestState.PAUSED),start+86_400_000)
            check(s.book.liabilities.size==1 && s.book.liabilities.single().xp==25L)
        }
        test("changing difficulty abandons before new terms") {
            var s=save(initial(),"a")
            s=apply(s,DeadlineCommand.Save(draft(s.world.quests.single()).copy(difficulty=Difficulty.HARD),"unused"))
            check(s.book.contracts.size==2 && s.book.active("a")!!.originalRewardXp==50L && s.book.liabilities.single().xp==13L)
        }
        test("undo after boundary requires full assessment disclosure") {
            var s=completion(save(initial(),"a"),"a")
            val command=DeadlineCommand.Undo("a:once")
            check(DeadlineEngine.preview(s,command,start+86_400_000)!!.immediateCost==25L)
            s=apply(s,command,start+86_400_000)
            check(s.book.progress(s.world).debtXp==25L && s.book.liabilities.size==1)
        }
        test("undo before deadline reopens without penalty") {
            val s=apply(completion(save(initial(),"a"),"a"),DeadlineCommand.Undo("a:once"))
            check(s.book.active("a")!=null && s.book.liabilities.isEmpty())
        }
        test("undo redo retains immutable reward snapshot and reverses allocations") {
            var s=completion(save(initial(),"a"),"a",start+86_400_000)
            s=apply(s,DeadlineCommand.Undo("a:once"),start+86_400_000)
            check(s.book.activeAllocations().isEmpty() && s.book.allocations.any { it.reversalOf!=null })
            s=completion(s,"a",start+86_400_000)
            check(s.world.completions.single().xp==25 && s.book.activeAllocations().sumOf { it.xp }==25L)
        }
        test("repeated complete has no extra allocation or XP") {
            val s=completion(save(initial(),"a"),"a",start+86_400_000)
            val again=DeadlineEngine.execute(s,DeadlineCommand.Complete("a"),start+86_400_000)
            check(!again.changed && again.state.book.allocations==s.book.allocations)
        }
        test("penalty reduces global balance but never skill contribution") {
            var s=completion(save(initial(),"a",null),"a")
            s=save(s,"b");s=DeadlineEngine.reconcile(s,start+86_400_000)
            check(s.book.progress(s.world).current.xp==0L)
            check(ProgressRules.total(s.world.completions,Skill.DISCIPLINE).xp==25L)
        }
        test("existing positive balance absorbs new liability with allocation evidence") {
            var s=completion(save(initial(),"credit",null,difficulty=Difficulty.HARD),"credit")
            s=save(s,"a");s=DeadlineEngine.reconcile(s,start+86_400_000)
            check(s.book.progress(s.world).current.xp==25L && s.book.outstanding().values.sum()==0L)
            check(s.book.activeAllocations().single().completionId=="credit:once")
        }
        test("all new earned XP services debt before growth") {
            var s=save(initial(),"a",difficulty=Difficulty.HARD)
            s=save(s,"b",null);s=completion(s,"b",start+86_400_000)
            check(s.book.progress(s.world).debtXp==25L && s.book.progress(s.world).current.xp==0L)
        }
        test("own quest debt comes before older debt") {
            var s=save(save(initial(),"old"),"own")
            s=completion(s,"own",start+86_400_000)
            val own=s.book.contracts.first { it.questId=="own" }.id
            val cost=s.book.liabilities.first { it.contractId==own }.id
            check(s.book.activeAllocations().single().consequenceId==cost)
        }
        test("force majeure before expiry grants no reward and no future cost") {
            var s=save(initial(),"a");s=apply(s,DeadlineCommand.Waive(s.book.contracts.single().id,ForceMajeureReason.HEALTH))
            s=DeadlineEngine.reconcile(s,start+86_400_000)
            check(s.book.liabilities.isEmpty() && s.world.completions.isEmpty() && s.book.contracts.single().status=="EXEMPTED")
        }
        test("force majeure after payment restitutes exactly the paid amount") {
            var s=completion(save(initial(),"a"),"a",start+86_400_000)
            s=apply(s,DeadlineCommand.Waive(s.book.contracts.single().id,ForceMajeureReason.FAMILY),start+86_400_000)
            check(s.book.progress(s.world).current.xp==25L && s.book.liabilities.size==1)
            check(s.book.adjustments.single().kind=="RESTITUTION" && s.book.activeAllocations().isEmpty())
        }
        test("force majeure partially paid splits waiver and restitution") {
            var s=save(initial(),"a",difficulty=Difficulty.HARD);s=save(s,"b",null);s=completion(s,"b",start+86_400_000)
            s=apply(s,DeadlineCommand.Waive(s.book.contracts.single().id,ForceMajeureReason.EXTERNAL),start+86_400_000)
            check(s.book.adjustments.map { it.xp }==listOf(25L,25L) && s.book.progress(s.world).current.xp==25L)
        }
        test("waiver is idempotent and never deletes the assessment") {
            var s=DeadlineEngine.reconcile(save(initial(),"a"),start+86_400_000)
            val command=DeadlineCommand.Waive(s.book.contracts.single().id,ForceMajeureReason.OTHER)
            s=apply(s,command,start+86_400_000);val twice=apply(s,command,start+86_400_000)
            check(s.book.adjustments==twice.book.adjustments && twice.book.liabilities.size==1)
        }
        test("three full breaches open one recovery route") {
            var s=initial();repeat(3){s=save(s,"q$it")};s=DeadlineEngine.reconcile(s,start+86_400_000)
            check(s.book.routes.single().triggerKind=="FULL_BREACH_COUNT")
            check(DeadlineEngine.reconcile(s,start+86_400_000).book.routes==s.book.routes)
        }
        test("abandonments do not count as three full breaches") {
            var s=initial();repeat(3){s=apply(save(s,"q$it"),DeadlineCommand.SetState("q$it",QuestState.PAUSED))}
            check(s.book.routes.isEmpty() && s.book.progress(s.world).outOfOrder)
        }
        test("recovery selection must cover debt and use real executable quests") {
            var s=initial();repeat(3){s=save(s,"q$it")};s=DeadlineEngine.reconcile(s,start+86_400_000)
            invalid { apply(s,DeadlineCommand.SelectRecovery(listOf("q0")),start+86_400_000) }
            s=apply(s,DeadlineCommand.SelectRecovery(listOf("q0","q1","q2")),start+86_400_000)
            check(s.book.nodes.size==3)
        }
        test("paid recovery closes and does not reopen from old frequency") {
            var s=initial();repeat(3){s=save(s,"q$it")};s=DeadlineEngine.reconcile(s,start+86_400_000)
            repeat(3){s=completion(s,"q$it",start+86_400_000)}
            check(s.book.routes.single().status=="CLOSED")
            s=apply(s,DeadlineCommand.Undo("q0:once"),start+86_400_000)
            check(s.book.routes.none { it.status=="OPEN" })
        }
        test("debt crossing one full level opens a route") {
            var s=initial();repeat(2){s=save(s,"q$it",difficulty=Difficulty.HARD)};s=DeadlineEngine.reconcile(s,start+86_400_000)
            check(s.book.routes.single().triggerKind=="DEBT_LEVEL")
        }
        test("legacy dated task is unsigned and remains completable") {
            val q=Quest("old","old",dueDay=today-100,createdAt=start-100,updatedAt=start)
            var s=initial().copy(world=initial().world.copy(quests=listOf(q)))
            s=DeadlineEngine.reconcile(s,start);check(s.book.liabilities.isEmpty())
            s=completion(s,"old");check(s.world.completions.single().xp==25 && s.book.contracts.isEmpty())
        }
        test("legacy calibration is explicit and rejects the old past date") {
            val q=Quest("old","old",dueDay=today-100,createdAt=start-100,updatedAt=start)
            val s=initial().copy(world=initial().world.copy(quests=listOf(q)))
            invalid { apply(s,DeadlineCommand.Save(draft(q),"old",true)) }
            val p=DeadlineEngine.preview(s,DeadlineCommand.Save(draft(q).copy(dueDay=today),"old",true),start)
            check(p!=null && DisclosureKind.SIGN in p.kinds)
        }
        test("backward clock cannot undo an assessment or award more contract time") {
            var s=DeadlineEngine.reconcile(save(initial(),"a"),start+86_400_000)
            s=DeadlineEngine.reconcile(s,start-86_400_000)
            check(s.book.liabilities.size==1 && s.book.effectiveNow(start)>=start+86_400_000)
            invalid { save(s,"b",today) }
        }
        test("highest historical level survives decline") {
            var s=initial(); repeat(4){s=completion(save(s,"c$it",null),"c$it")}
            s=save(s,"a");s=DeadlineEngine.reconcile(s,start+86_400_000)
            check(s.book.progress(s.world).current.level==1 && s.book.progress(s.world).highestLevel==2)
        }
        test("late extended quest pays reduced reward without erasing full cost") {
            var s=apply(save(initial(),"a"),DeadlineCommand.Postpone("a"))
            s=completion(s,"a",start+3*86_400_000)
            check(s.world.completions.single().xp==20 && s.book.progress(s.world).debtXp==5L)
        }
        test("one-time reward slot stays unique across recommitment") {
            var s=DeadlineEngine.reconcile(save(initial(),"a"),start+86_400_000)
            s=apply(s,DeadlineCommand.Save(draft(s.world.quests.single()).copy(dueDay=today+2),"a"),start+86_400_000)
            s=completion(s,"a",start+86_400_000);s=completion(s,"a",start+86_400_000)
            check(s.book.contracts.size==2 && s.world.completions.size==1)
        }
        test("neutral state is independent of narrative IDs") {
            val a=save(initial(),"a");val b=save(initial(),"a")
            check(a.book.progress(a.world)==b.book.progress(b.world))
        }
        test("editing a legacy title never opts it into an old deadline") {
            val q = Quest("old", "original", dueDay = today - 100, createdAt = start - 100, updatedAt = start)
            val s = initial().copy(world = initial().world.copy(quests = listOf(q)))
            val command = DeadlineCommand.Save(draft(q).copy(title = "clarified"), "old")
            check(DeadlineEngine.preview(s, command, start) == null)
            val after = DeadlineEngine.execute(s, command, start).state
            check(after.book.contracts.isEmpty() && after.world.quests.single().title == "clarified")
        }
        test("an already overdue promise may be clarified without resigning") {
            val s = DeadlineEngine.reconcile(save(initial(), "a"), start + 86_400_000)
            val command = DeadlineCommand.Save(draft(s.world.quests.single()).copy(title = "smaller action"), "a")
            check(DeadlineEngine.preview(s, command, start + 86_400_000) == null)
            val after = DeadlineEngine.execute(s, command, start + 86_400_000).state
            check(after.book.contracts.size == 1 && after.book.liabilities.size == 1)
            check(after.book.contracts.single().titleSnapshot == s.world.quests.single().title)
        }
        test("daily postpone skips only the present day even on repeated requests") {
            val q = Quest("daily", "daily", kind = QuestKind.DAILY, createdAt = start, updatedAt = start)
            var s = initial().copy(world = initial().world.copy(quests = listOf(q)))
            repeat(4) { s = apply(s, DeadlineCommand.Postpone("daily")) }
            check(s.world.quests.single().snoozedUntilDay == today + 1)
            check(s.book.liabilities.isEmpty())
        }
        report("$count deadline scenarios passed")
        return count
    }
    @JvmStatic fun main(args: Array<String>) { run(::println); ScenarioSuite.run(::println) }
}
