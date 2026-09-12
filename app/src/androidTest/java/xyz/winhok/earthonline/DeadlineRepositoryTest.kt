package xyz.winhok.earthonline

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.data.*

@RunWith(AndroidJUnit4::class)
class DeadlineRepositoryTest {
    private lateinit var db: EarthDatabase
    private lateinit var repo: WorldRepository
    private val time = MutableClock()
    private class MutableClock(var current: Long = Instant.parse("2026-09-09T10:00:00Z").toEpochMilli()) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(current)
    }
    @Before fun setUp() = runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),EarthDatabase::class.java).build()
        repo=WorldRepository(db,time);repo.join("Tester","Reality")
    }
    @After fun close() { db.close() }
    private suspend fun create(title: String="contract"): Quest {
        val day=repo.snapshot().dayAt(time.current)
        val cmd=DeadlineCommand.Save(QuestDraft(title=title,dueDay=day),title)
        val plan=repo.plan(cmd)
        assertNotNull(plan.disclosure)
        return repo.execute(cmd,plan.disclosure).state.world.quests.first { it.id==title }
    }
    @Test fun undisclosedSigningCannotWriteAnyPartialData() = runBlocking {
        val before=repo.snapshotBackup()
        try { repo.saveQuest(QuestDraft(title="not signed",dueDay=before.world.dayAt(time.current)));fail("Confirmation required") }
        catch (_: DisclosureRequired) { }
        assertEquals(before,repo.snapshotBackup())
    }
    @Test fun concurrentAssessmentAndCompletionHaveOneOutcome() = runBlocking {
        create(); time.current+=86_400_000
        coroutineScope { repeat(12) { launch(Dispatchers.IO) { if(it%2==0) repo.reconcile() else repo.complete("contract") } } }
        val s=repo.snapshotBackup()
        assertEquals(1,s.consequences.size);assertEquals(1,s.world.completions.size)
        assertEquals(25L,s.repaymentAllocations.filter { it.reversalOf==null }.sumOf { it.xp })
        assertEquals(0L,s.deadlineState().book.progress(s.world).debtXp)
    }
    @Test fun extensionAndRecommitPersistAllHistoryWithoutDuplicateRewardSlot() = runBlocking {
        create()
        repeat(4) { val cmd=DeadlineCommand.Postpone("contract");repo.execute(cmd,repo.plan(cmd).disclosure) }
        val s=repo.snapshotBackup()
        assertEquals(2,s.contracts.size);assertEquals(13L,s.consequences.single().xp)
        assertEquals(1,s.contracts.count { it.status=="ACTIVE" })
        assertEquals(s,BackupCodec.decodeSnapshot(BackupCodec.encode(s,time.current)))
    }
    @Test fun undoAfterBoundaryIsConfirmedAndReversalsRoundTrip() = runBlocking {
        create();repo.complete("contract");time.current+=86_400_000
        val cmd=DeadlineCommand.Undo("contract:once")
        val p=repo.plan(cmd);assertEquals(25L,p.disclosure!!.immediateCost)
        repo.execute(cmd,p.disclosure);repo.complete("contract")
        repo.undo("contract:once")
        val s=repo.snapshotBackup()
        assertTrue(s.repaymentAllocations.any { it.reversalOf!=null })
        assertEquals(s,BackupCodec.decodeSnapshot(BackupCodec.encode(s,time.current)))
    }
    @Test fun failedAssessmentRollsBackQuestCompletionLedgerAndJournal() = runBlocking {
        create();val before=repo.snapshotBackup();time.current+=86_400_000
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_liability BEFORE INSERT ON consequence_events BEGIN SELECT RAISE(ABORT,'injected ledger failure'); END")
        try { repo.complete("contract");fail("Expected injected failure") } catch (_: SQLiteException) { }
        assertEquals(before,repo.snapshotBackup())
    }
    @Test fun reducedRewardIsAcceptedOnlyWithContractEvidence() = runBlocking {
        create();val cmd=DeadlineCommand.Postpone("contract");repo.execute(cmd,repo.plan(cmd).disclosure)
        time.current+=86_400_000;repo.complete("contract")
        val s=repo.snapshotBackup();assertEquals(20,s.world.completions.single().xp)
        assertEquals(s,BackupCodec.decodeSnapshot(BackupCodec.encode(s,time.current)))
        try { BackupCodec.encode(s.copy(contracts=emptyList(),contractRevisions=emptyList()),time.current);fail("Orphan reduced reward") }
        catch (_: RuleViolation) { }
    }
    @Test fun waiverAfterPaymentRestoresOnlyCostAndSurvivesReopen() = runBlocking {
        create();time.current+=86_400_000;repo.complete("contract")
        val cmd=DeadlineCommand.Waive(repo.snapshotBackup().contracts.single().id,ForceMajeureReason.HEALTH)
        repo.execute(cmd,repo.plan(cmd).disclosure)
        val s=repo.snapshotBackup();assertEquals(25L,s.deadlineState().book.progress(s.world).current.xp)
        assertEquals("RESTITUTION",s.consequenceAdjustments.single().kind)
        assertEquals(1,s.world.completions.size)
        repo.restore(s);assertEquals(s,repo.snapshotBackup())
    }
    @Test fun presentationPreferencesDoNotAffectBusinessAndRestoreExactly() = runBlocking {
        create();val before=repo.snapshotBackup()
        repo.saveEffects(true,false,true)
        repo.setPresentationPreference("cultivation","intro_seen",true)
        repo.setNarrativeSystem(NarrativeSystemId.CULTIVATION)
        val after=repo.snapshotBackup()
        assertEquals(before.contracts,after.contracts);assertEquals(before.world,after.world)
        assertEquals(after,BackupCodec.decodeSnapshot(BackupCodec.encode(after,time.current)))
        try { repo.setPresentationPreference("cultivation","xp",true);fail("Domain key masquerading as presentation") }
        catch (_: RuleViolation) { }
        repo.restore(before);assertEquals(before,repo.snapshotBackup())
    }
    @Test fun restoreOlderSaveRestoresItsClockInsteadOfHiddenAntiCheat() = runBlocking {
        create();val before=repo.snapshotBackup();time.current+=86_400_000;repo.reconcile()
        assertEquals(1,repo.snapshotBackup().consequences.size)
        repo.restore(before)
        assertEquals(before,repo.snapshotBackup())
        repo.reset();assertTrue(repo.snapshotBackup().contracts.isEmpty());assertTrue(repo.snapshotBackup().clockBoundaries.isEmpty())
    }
    @Test fun clockRollbackWarningIsBoundedAndCannotChangeOutcome() = runBlocking {
        create();time.current+=86_400_000;repo.reconcile();time.current-=2*86_400_000
        repeat(5) { repo.reconcile() }
        val s=repo.snapshotBackup();assertEquals(1,s.consequences.size)
        assertEquals(1,s.clockBoundaries.count { it.id.startsWith("clock-warning:") })
    }
}
