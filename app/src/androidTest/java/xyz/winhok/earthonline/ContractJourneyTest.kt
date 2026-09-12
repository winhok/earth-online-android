package xyz.winhok.earthonline

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.data.*

/** User-facing commands run through the actual Activity/ViewModel/Room stack. */
@RunWith(AndroidJUnit4::class)
class ContractJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var activity: ActivityScenario<MainActivity>
    private val app get() = ApplicationProvider.getApplicationContext<EarthApplication>()
    private val repo get() = app.repository
    private val now get() = System.currentTimeMillis()
    private fun day(t: Long) = Instant.ofEpochMilli(t).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
    @Before fun reset() { runBlocking { repo.reset() } }
    @After fun close() { if (::activity.isInitialized) activity.close() }
    private fun waitFor(m: SemanticsMatcher) = compose.waitUntil(20_000) {
        compose.onAllNodes(m).fetchSemanticsNodes().isNotEmpty()
    }
    private fun awaitDb(test: (BackupSnapshot) -> Boolean) = compose.waitUntil(20_000) {
        runBlocking { test(repo.snapshotBackup()) }
    }
    private fun start(snapshot: BackupSnapshot) {
        runBlocking { repo.restore(snapshot) }
        activity = ActivityScenario.launch(MainActivity::class.java)
        waitFor(hasText("指挥台") or hasTestTag("overdue-batch"))
    }
    private fun base(t: Long = now) = DeadlineState(World(player = Player("契约验收员", "验收服", "UTC", t, true)))
    private fun apply(s: DeadlineState, c: DeadlineCommand, t: Long) = DeadlineEngine.execute(s,c,t,
        DeadlineEngine.preview(s,c,t)).state
    private fun signed(s: DeadlineState, id: String, t: Long, due: Long = day(t)) = apply(s,
        DeadlineCommand.Save(QuestDraft(title=id,dueDay=due),id),t)
    private fun snapshot(s: DeadlineState) = BackupSnapshot(s.world).withDeadline(s)
    private fun openTask(title: String) {
        compose.onNodeWithText("任务").performClick()
        compose.onNodeWithTag("quests-list").performScrollToNode(hasText(title))
        compose.onNodeWithText(title).performClick()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val dir = File(app.getExternalFilesDir(null), "acceptance").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
    @Test fun legacyCalibrationCancelsSafelyThenSignsExactlyOnceAcrossRecreation() {
        val t=now
        val old=Quest("legacy","逐项校准真实任务",dueDay=day(t),createdAt=t-1000,updatedAt=t-1000)
        start(BackupSnapshot(base(t).world.copy(quests=listOf(old))))
        openTask(old.title)
        compose.onNodeWithTag("calibrate-contract").performScrollTo().performClick()
        waitFor(hasTestTag("contract-confirmation"))
        compose.onNodeWithText("逾期全额代价 25 XP",substring=true).assertExists()
        compose.onNodeWithTag("cancel-contract").performClick()
        assertTrue(runBlocking { repo.snapshotBackup().contracts.isEmpty() })
        compose.onNodeWithTag("calibrate-contract").performScrollTo().performClick()
        compose.onNodeWithTag("confirm-contract", useUnmergedTree = true).performClick()
        awaitDb { it.contracts.size==1 }
        activity.recreate()
        awaitDb { it.contracts.size==1 }
        assertEquals(0,runBlocking { repo.snapshotBackup().world.completions.size })
        capture("v15-legacy-signed")
    }
    @Test fun extensionNeedsASeparateConfirmationAndPersistsAttenuatedReward() {
        val t=now;start(snapshot(signed(base(t),"确认后再延期",t)))
        openTask("确认后再延期")
        compose.onNodeWithTag("postpone-contract").performScrollTo().performClick()
        waitFor(hasTestTag("contract-confirmation"))
        compose.onNodeWithTag("cancel-contract").performClick()
        assertEquals(day(t),runBlocking { repo.snapshotBackup().contracts.single().dueDay })
        compose.onNodeWithTag("postpone-contract").performScrollTo().performClick()
        waitFor(hasTestTag("contract-confirmation"))
        compose.onNodeWithTag("confirm-contract", useUnmergedTree = true).performClick()
        awaitDb { it.contracts.single().extensionCount==1 }
        val after=runBlocking { repo.snapshotBackup() }
        assertEquals(20L,after.contracts.single().currentRewardXp)
        assertEquals(day(t)+1,after.contracts.single().dueDay)
        assertEquals(day(t)+1,after.world.quests.single().snoozedUntilDay)
    }
    @Test fun batchAcknowledgementCannotBeSkippedAndDoesNotRepeatAfterRestart() {
        val t=now-2*86_400_000
        var s=base(t);repeat(4) { s=signed(s,"离线承诺-$it",t) }
        s=DeadlineEngine.reconcile(s,now)
        start(snapshot(s))
        waitFor(hasTestTag("overdue-batch")); capture("v15-overdue-batch")
        Espresso.pressBack()
        compose.onNodeWithTag("overdue-batch").assertExists()
        compose.onNodeWithTag("acknowledge-overdue").performClick()
        awaitDb { it.settlementReceipts.size==4 }
        activity.recreate()
        waitFor(hasText("指挥台") or hasTestTag("overdue-batch"))
        compose.onNodeWithTag("overdue-batch").assertDoesNotExist()
        val after=runBlocking { repo.snapshotBackup() }
        assertEquals(4,after.consequences.size)
        assertEquals(100L,after.deadlineState().book.progress(after.world).debtXp)
    }
    @Test fun overdueCultivationTrackOffersRecoveryInsteadOfOrdinaryPostpone() {
        val t=now-2*86_400_000
        val s=DeadlineEngine.reconcile(signed(base(t),"逾期修行",t),now)
        start(snapshot(s))
        waitFor(hasTestTag("acknowledge-overdue"))
        compose.onNodeWithTag("acknowledge-overdue").performClick()
        awaitDb { it.settlementReceipts.size==1 }
        runBlocking { repo.setNarrativeSystem(NarrativeSystemId.CULTIVATION) }
        waitFor(hasText("历练") and hasClickAction())
        compose.onNodeWithText("历练").performClick()
        compose.onNodeWithTag("quests-list").performScrollToNode(hasTestTag("quest-next-recovery"))
        compose.onNodeWithTag("quest-next-recovery").assertIsDisplayed()
        compose.onNodeWithText("改命一日", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("quest-next-recovery").performClick()
        waitFor(hasTestTag("contract-ledger"))
    }
    @Test fun forceMajeureUsesCategoryAndExplicitPriceWithoutCreatingCompletion() {
        val t=now-2*86_400_000
        val s=DeadlineEngine.reconcile(signed(base(t),"现实变故",t),now)
        start(snapshot(s))
        waitFor(hasTestTag("acknowledge-overdue"));compose.onNodeWithTag("acknowledge-overdue").performClick()
        awaitDb { it.settlementReceipts.size==1 }
        openTask("现实变故")
        compose.onNodeWithTag("waive-contract").performScrollTo().performClick()
        waitFor(hasTestTag("force-majeure-dialog"))
        compose.onNodeWithTag("confirm-force-majeure").performClick()
        waitFor(hasTestTag("contract-confirmation"))
        compose.onNodeWithText("不发放任务完成奖励",substring=true).assertExists()
        compose.onNodeWithTag("confirm-contract", useUnmergedTree = true).performClick()
        awaitDb { it.consequenceAdjustments.isNotEmpty() }
        val after=runBlocking { repo.snapshotBackup() }
        assertEquals(0L,after.deadlineState().book.progress(after.world).debtXp)
        assertTrue(after.world.completions.isEmpty())
        assertEquals(1,after.consequences.size)
    }
    @Test fun recoveryRequiresSufficientRealTasksAndClosesWhenOrdinaryRewardsRepayDebt() {
        val t=now-2*86_400_000
        var s=base(t);repeat(4) { s=signed(s,"恢复节点-$it",t) }
        s=DeadlineEngine.reconcile(s,now)
        start(snapshot(s))
        waitFor(hasTestTag("acknowledge-overdue"));compose.onNodeWithTag("acknowledge-overdue").performClick()
        awaitDb { it.settlementReceipts.size==4 }
        compose.onNodeWithTag("ledger-navigation").performClick()
        waitFor(hasTestTag("contract-ledger"))
        compose.onNodeWithTag("contract-ledger-list").performScrollToNode(hasTestTag("choose-recovery"))
        compose.onNodeWithTag("choose-recovery").performClick()
        compose.onNodeWithTag("contract-ledger-list").performScrollToNode(hasTestTag("save-recovery"))
        compose.onNodeWithTag("save-recovery").assertIsNotEnabled()
        repeat(4) { index ->
            compose.onNodeWithTag("contract-ledger-list").performScrollToNode(hasTestTag("recovery-choice-恢复节点-$index"))
            compose.onNodeWithTag("recovery-choice-恢复节点-$index").performClick()
        }
        compose.onNodeWithTag("contract-ledger-list").performScrollToNode(hasTestTag("save-recovery"))
        compose.onNodeWithTag("save-recovery").assertIsEnabled().performClick()
        awaitDb { it.recoveryNodes.size==4 }
        assertEquals(4,runBlocking { repo.snapshotBackup().world.quests.size })
        // Each uses the same ordinary repository command exercised through UI elsewhere.
        runBlocking { repeat(4) { repo.complete("恢复节点-$it") } }
        awaitDb { it.recoveryRoutes.single().status=="CLOSED" }
        val after=runBlocking { repo.snapshotBackup() }
        assertEquals(0L,after.deadlineState().book.progress(after.world).current.xp)
        assertEquals(0L,after.deadlineState().book.progress(after.world).debtXp)
        assertEquals(4,after.world.completions.size)
        capture("v15-recovery-closed")
    }
    @Test fun swipeAndVisibleButtonUseOneCompletionIdentity() {
        val t=now
        val q=Quest("swipe","一次真实行动",createdAt=t,updatedAt=t)
        start(BackupSnapshot(base(t).world.copy(quests=listOf(q))))
        compose.onNodeWithText("任务").performClick()
        compose.onNodeWithTag("quests-list").performScrollToNode(hasText(q.title))
        compose.onNodeWithTag("quest-card-${q.id}").performTouchInput { swipeRight() }
        awaitDb { it.world.completions.size==1 }
        assertEquals(25L,runBlocking { repo.snapshotBackup().deadlineState().book.progress(repo.snapshot()).current.xp })
        activity.recreate();waitFor(hasText("任务"))
        assertEquals(1,runBlocking { repo.snapshotBackup().world.completions.size })
    }
    @Test fun acknowledgementBackupRoundTripCannotManufactureOrWaiveDebt() {
        val t=now-2*86_400_000
        val s=DeadlineEngine.reconcile(signed(base(t),"收据不是账本",t),now)
        runBlocking {
            repo.restore(snapshot(s));val before=repo.snapshotBackup()
            repo.acknowledgeAssessments(before.consequences.map { it.id })
            repo.acknowledgeAssessments(before.consequences.map { it.id })
            val after=repo.snapshotBackup()
            assertEquals(1,after.settlementReceipts.size)
            assertEquals(before.deadlineState(),after.deadlineState())
            assertEquals(after,BackupCodec.decodeSnapshot(BackupCodec.encode(after,now)))
            try { BackupCodec.encode(after.copy(settlementReceipts=listOf(SettlementReceiptEntity("missing",now))),now);fail("Orphan receipt") }
            catch (_:RuleViolation) { }
            assertEquals(after,repo.snapshotBackup())
            repo.reset();assertTrue(repo.snapshotBackup().settlementReceipts.isEmpty())
        }
    }
    @Test fun archivedFulfillmentCanBeUndoneWithoutLosingMetadata() {
        val t=now;val s=signed(base(t),"已履约可归档",t,day(t)+2)
        runBlocking {
            repo.restore(snapshot(s));repo.complete("已履约可归档")
            repo.setQuestState("已履约可归档",QuestState.ARCHIVED)
            val cmd=DeadlineCommand.Undo("已履约可归档:once")
            val plan=repo.plan(cmd);assertTrue(DisclosureKind.REOPEN in plan.disclosure!!.kinds)
            repo.execute(cmd,plan.disclosure)
            val after=repo.snapshotBackup()
            assertEquals(QuestState.ARCHIVED,after.world.quests.single().state)
            assertEquals("ACTIVE",after.contracts.single().status)
            assertEquals(after,BackupCodec.decodeSnapshot(BackupCodec.encode(after,now)))
        }
    }
}
