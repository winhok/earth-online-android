package xyz.winhok.earthonline

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.data.*

@RunWith(AndroidJUnit4::class)
class StorageAcceptanceTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: EarthDatabase
    private lateinit var repo: WorldRepository
    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, EarthDatabase::class.java).build()
        repo = WorldRepository(db, clock)
        repo.join("存档测试员", "测试服")
    }
    @After fun tearDown() { db.close() }

    @Test fun fileDatabaseSurvivesCloseAndReopen() = runBlocking {
        val name = "acceptance-${System.nanoTime()}.db"
        var disk = Room.databaseBuilder(context, EarthDatabase::class.java, name).build()
        try {
            val first = WorldRepository(disk, clock)
            first.join("重启测试员", "测试服")
            val q = first.saveQuest(QuestDraft(title = "重启不能丢失"))
            first.complete(q.id)
            val saved = first.snapshot()
            disk.close()
            disk = Room.databaseBuilder(context, EarthDatabase::class.java, name).build()
            assertEquals(saved, WorldRepository(disk, clock).snapshot())
        } finally { disk.close(); context.deleteDatabase(name) }
    }

    @Test fun oversizedSaveIsRejectedBeforeReplacement() = runBlocking {
        repo.saveQuest(QuestDraft(title = "保留原存档"))
        val before = repo.snapshot()
        val many = List(1_000) { i -> Quest("large-$i", "大任务", description = "中".repeat(4_000),
            createdAt = clock.millis(), updatedAt = clock.millis()) }
        try { repo.restore(before.copy(quests = many)); fail("Expected capacity rejection") }
        catch (e: RuleViolation) { assertEquals(RuleError.LIMIT, e.reason) }
        assertEquals(before, repo.snapshot())
    }

    @Test fun oneThousandTasksRoundTripAndRecommend() = runBlocking {
        val base = repo.snapshot()
        val tasks = List(1_000) { i -> Quest("q-$i", "行动 $i", createdAt = clock.millis(), updatedAt = clock.millis()) }
        repo.restore(base.copy(quests = tasks))
        val world = repo.snapshot()
        val start = System.nanoTime()
        val encoded = BackupCodec.encode(world, clock.millis())
        assertEquals(BackupCodec.MAX_BYTES, SnapshotBudget.MAX_BYTES)
        assertTrue(encoded.toByteArray().size <= SnapshotBudget.upperBound(world))
        assertEquals(world, BackupCodec.decode(encoded))
        assertEquals(1_000, NextActionRules.rank(world, world.dayAt(clock.millis()), 25, 2).size)
        println("ACCEPTANCE 1000_tasks_roundtrip_ms=" + (System.nanoTime() - start) / 1_000_000)
    }

    @Test fun nestedJsonEscapingStaysUnderConservativeBudget() = runBlocking {
        val weird = "中文😀\n\t\"\\\u0001\u2028".repeat(20)
        val q = repo.saveQuest(QuestDraft(title = "特殊字符", description = weird))
        repo.addNote(weird)
        repo.complete(q.id)
        val world = repo.snapshot()
        val encoded = BackupCodec.encode(world, clock.millis())
        assertTrue(encoded.toByteArray(Charsets.UTF_8).size <= SnapshotBudget.upperBound(world))
        assertEquals(world, BackupCodec.decode(encoded))
    }

    @Test fun exactOldOccurrenceCanBeUndoneAfterMidnight() = runBlocking {
        val q = repo.saveQuest(QuestDraft(title = "日常", kind = QuestKind.DAILY))
        val old = repo.complete(q.id).completion
        val tomorrow = WorldRepository(db, Clock.offset(clock, java.time.Duration.ofDays(1)))
        val next = tomorrow.complete(q.id).completion
        tomorrow.undo(old.id)
        val world = tomorrow.snapshot()
        assertNotNull(world.completions.first { it.id == old.id }.revokedAt)
        assertNull(world.completions.first { it.id == next.id }.revokedAt)
        assertEquals(25L, ProgressRules.total(world.completions).xp)
    }

    @Test fun missingGoalCannotPartiallyCreateTask() = runBlocking {
        val before = repo.snapshot()
        try { repo.saveQuest(QuestDraft(title = "关联无效主线", goalId = "missing")); fail("Expected rejection") }
        catch (e: RuleViolation) { assertEquals(RuleError.MISSING_GOAL, e.reason) }
        assertEquals(before, repo.snapshot())
    }
}
