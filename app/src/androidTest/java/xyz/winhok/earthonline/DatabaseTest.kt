package xyz.winhok.earthonline

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.data.*

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: EarthDatabase
    private lateinit var repo: WorldRepository
    private val clock = Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneOffset.UTC)
    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EarthDatabase::class.java).build()
        repo = WorldRepository(db, clock)
        repo.join("测试玩家", "测试服")
    }
    @After fun tearDown() { db.close() }

    @Test fun concurrentCompleteGrantsOnlyOnce() = runBlocking {
        val q = repo.saveQuest(QuestDraft(title = "并发完成"))
        val results = coroutineScope { List(20) { async(Dispatchers.Default) { repo.complete(q.id) } }.awaitAll() }
        val world = repo.snapshot()
        assertEquals(1, results.count { it.changed })
        assertEquals(1, world.completions.size)
        assertEquals(25L, ProgressRules.total(world.completions).xp)
        assertEquals(1, world.events.count { it.kind == EventKind.COMPLETED })
    }
    @Test fun systemEventPersistsNeutralKindAndStructuredFactsWithoutRenderedSentence() = runBlocking {
        val title = "玩家原始标题 😀"
        val quest = repo.saveQuest(QuestDraft(title = title))
        repo.complete(quest.id)

        val event = repo.snapshot().events.single { it.kind == EventKind.COMPLETED }
        assertEquals(EventKind.COMPLETED, event.kind)
        assertEquals(quest.id, event.questId)
        assertEquals(25, event.xp)
        assertEquals(title, event.text)
        assertNotEquals("任务完成 · +25 XP", event.text)
    }
    @Test fun undoAndEditedRedoDoesNotFarmXp() = runBlocking {
        val q = repo.saveQuest(QuestDraft(title = "单次任务", difficulty = Difficulty.EASY))
        val first = repo.complete(q.id)
        assertTrue(repo.undo(first.completion.id))
        assertFalse(repo.undo(first.completion.id))
        repo.saveQuest(QuestDraft(id = q.id, title = "修改难度", difficulty = Difficulty.HARD))
        repo.complete(q.id)
        assertEquals(10L, ProgressRules.total(repo.snapshot().completions).xp)
    }
    @Test fun nextDailyOccurrenceGetsNewReward() = runBlocking {
        val q = repo.saveQuest(QuestDraft(title = "每日任务", kind = QuestKind.DAILY))
        repo.complete(q.id)
        val tomorrow = WorldRepository(db, Clock.offset(clock, java.time.Duration.ofDays(1)))
        tomorrow.complete(q.id)
        assertEquals(2, repo.snapshot().completions.size)
        assertEquals(50L, ProgressRules.total(repo.snapshot().completions).xp)
    }
    @Test fun backupRoundTripAndRestoreDisablesReminder() = runBlocking {
        val q = repo.saveQuest(QuestDraft(title = "包含中文与\"引号\"的任务"))
        repo.complete(q.id)
        repo.updatePlayer("测试玩家", "测试服", ThemeMode.DARK, true, 20)
        val before = repo.snapshot()
        val decoded = BackupCodec.decode(BackupCodec.encode(before, clock.millis()))
        assertEquals(before, decoded)
        repo.restore(decoded)
        assertEquals(before.quests, repo.snapshot().quests)
        assertEquals(before.completions, repo.snapshot().completions)
        assertFalse(repo.snapshot().player.remindersEnabled)
    }
    @Test fun invalidRestoreNeverDeletesExistingData() = runBlocking {
        repo.saveQuest(QuestDraft(title = "必须保留"))
        val before = repo.snapshot()
        try { repo.restore(before.copy(quests = before.quests.map { it.copy(goalId = "missing") })); fail("Expected rejection") }
        catch (_: RuleViolation) { /* expected */ }
        assertEquals(before, repo.snapshot())
    }
    @Test fun checksumFailureIsRejected() = runBlocking {
        val text = BackupCodec.encode(repo.snapshot(), clock.millis())
        val bad = JSONObject(text).put("sha256", "0".repeat(64)).toString()
        try { BackupCodec.decode(bad); fail("Expected checksum rejection") }
        catch (_: IllegalArgumentException) { /* expected */ }
    }
    @Test fun fractionalProtocolVersionIsRejected() = runBlocking {
        val text = BackupCodec.encode(repo.snapshot(), clock.millis())
        val bad = JSONObject(text).put("version", 1.5).toString()
        try { BackupCodec.decode(bad); fail("Expected exact integer version") }
        catch (_: IllegalArgumentException) { /* expected */ }
    }
    @Test fun sqliteRollsBackInterruptedReplacement() = runBlocking {
        repo.saveQuest(QuestDraft(title = "事务回滚"))
        val before = repo.snapshot()
        try {
            db.withTransaction { db.dao().clearEvents(); db.dao().clearQuests(); error("Injected failure") }
        } catch (_: IllegalStateException) { /* expected */ }
        assertEquals(before, repo.snapshot())
    }
    @Test fun reminderClaimIsAtomic() = runBlocking {
        repo.updatePlayer("测试玩家", "测试服", ThemeMode.DARK, true, 20)
        val day = repo.snapshot().dayAt(clock.millis())
        val results = coroutineScope { List(10) { async { repo.claimReminder(day) } }.awaitAll() }
        assertEquals(1, results.count { it })
    }
}
