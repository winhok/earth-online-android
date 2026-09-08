package xyz.winhok.earthonline

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.data.*

@RunWith(AndroidJUnit4::class)
class MigrationV1ToV2Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "migration-v1-v2-${System.nanoTime()}.db"

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EarthDatabase::class.java,
    )

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun v1DataMigratesWithoutRetroactiveContracts(): Unit = runBlocking {
        val v1 = migrationHelper.createDatabase(databaseName, 1)
        try {
            v1.execSQL(
                """INSERT INTO player
                    (id, name, server, zoneId, joinedAt, onboarded, theme,
                     remindersEnabled, reminderHour, lastReminderDay)
                    VALUES (1, '旧玩家', '现实服', 'Asia/Shanghai', 1000, 1, 'LIGHT', 1, 20, 20000)
                """.trimIndent(),
            )
            v1.execSQL(
                """INSERT INTO goals (id, title, description, archived, createdAt)
                    VALUES ('goal-1', '旧主线', '保持不变', 0, 1100)
                """.trimIndent(),
            )
            insertQuest(v1, id = "quest-once", kind = "SIDE", dueDay = 21000, goalId = "goal-1")
            insertQuest(v1, id = "quest-boss", kind = "BOSS", dueDay = 21001, goalId = null)
            v1.execSQL(
                """INSERT INTO completions
                    (id, questId, occurrence, title, kind, skill, xp, completedAt, completedDay, revokedAt)
                    VALUES ('quest-once:once', 'quest-once', 'once', '旧任务', 'SIDE', 'DISCIPLINE', 25, 1300, 20000, NULL)
                """.trimIndent(),
            )
            v1.execSQL(
                """INSERT INTO journal (id, kind, text, createdAt, questId, xp)
                    VALUES ('event-1', 'COMPLETED', '旧日志原文', 1300, 'quest-once', 25)
                """.trimIndent(),
            )
        } finally {
            v1.close()
        }

        migrationHelper.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            EarthDatabase.MIGRATION_1_2,
        ).close()

        val migrated = openDatabase()
        try {
            val dao = migrated.dao()
            assertEquals("旧玩家", dao.player()?.value?.name)
            assertEquals(ThemeMode.LIGHT, dao.player()?.value?.theme)
            assertEquals(listOf("goal-1"), dao.goals().map { it.value.id })
            assertEquals(
                listOf("quest-boss", "quest-once"),
                dao.quests().map { it.value.id }.sorted(),
            )
            assertEquals(listOf("quest-once:once"), dao.completions().map { it.value.id })
            assertEquals(listOf("旧日志原文"), dao.events().map { it.value.text })
            assertEquals(EarthDatabase.EARTH_NATIVE_NARRATIVE_ID, dao.narrativePreference()?.narrativeId)
            assertTrue(dao.contracts().isEmpty())
        } finally {
            migrated.close()
        }

        val reopened = openDatabase()
        try {
            assertEquals(EarthDatabase.EARTH_NATIVE_NARRATIVE_ID, reopened.dao().narrativePreference()?.narrativeId)
            assertTrue(reopened.dao().contracts().isEmpty())
        } finally {
            reopened.close()
        }
    }

    @Test
    fun v2LedgerPersistsWithForeignKeysAndIdempotency(): Unit = runBlocking {
        val database = openDatabase()
        try {
            val dao = database.dao()
            dao.putPlayer(
                PlayerEntity(value = Player(name = "账本玩家", server = "现实服", onboarded = true)),
            )
            dao.putNarrativePreference(
                NarrativePreferenceEntity(narrativeId = EarthDatabase.EARTH_NATIVE_NARRATIVE_ID),
            )
            dao.putQuest(
                QuestEntity(
                    Quest(id = "quest-1", title = "现实行动", createdAt = 800, updatedAt = 800),
                ),
            )
            dao.putCompletion(
                CompletionEntity(
                    Completion(
                        id = "quest-1:once",
                        questId = "quest-1",
                        occurrence = "once",
                        title = "现实行动",
                        kind = QuestKind.SIDE,
                        skill = Skill.DISCIPLINE,
                        xp = 25,
                        completedAt = 1000,
                        completedDay = 20000,
                    ),
                ),
            )
            dao.putContract(
                ContractEntity(
                    id = "contract-1",
                    questId = "quest-1",
                    occurrence = "once",
                    zoneId = "Asia/Shanghai",
                    dueDay = 20001,
                    originalRewardXp = 25,
                    currentRewardXp = 20,
                    status = "ACTIVE",
                    signedAt = 900,
                ),
            )
            dao.putContractRevision(
                ContractRevisionEntity(
                    id = "revision-1",
                    contractId = "contract-1",
                    sequence = 1,
                    kind = "POSTPONED",
                    previousDueDay = 20000,
                    newDueDay = 20001,
                    previousRewardXp = 25,
                    newRewardXp = 20,
                    createdAt = 950,
                    idempotencyKey = "postpone:contract-1:1",
                ),
            )
            dao.putConsequence(
                ConsequenceEventEntity(
                    id = "consequence-1",
                    contractId = "contract-1",
                    kind = "OVERDUE",
                    xp = 25,
                    effectiveDay = 20002,
                    createdAt = 1100,
                    idempotencyKey = "overdue:contract-1",
                ),
            )
            dao.putConsequenceAdjustment(
                ConsequenceAdjustmentEntity(
                    id = "adjustment-1",
                    consequenceId = "consequence-1",
                    kind = "WAIVER",
                    xp = 5,
                    reasonCategory = "FORCE_MAJEURE",
                    createdAt = 1200,
                    idempotencyKey = "waiver:consequence-1:1",
                ),
            )
            dao.putRepaymentAllocation(
                RepaymentAllocationEntity(
                    id = "allocation-1",
                    consequenceId = "consequence-1",
                    completionId = "quest-1:once",
                    xp = 20,
                    createdAt = 1300,
                    idempotencyKey = "allocation:quest-1:once:consequence-1",
                ),
            )
            dao.putClockBoundary(
                ClockBoundaryEntity(
                    id = "contract-reconcile",
                    zoneId = "Asia/Shanghai",
                    lastSettledDay = 20002,
                    lastSettledAt = 1400,
                ),
            )
            dao.putRecoveryRoute(
                RecoveryRouteEntity(
                    id = "recovery-1",
                    status = "OPEN",
                    triggerKind = "DEBT_LEVEL",
                    targetDebtXp = 20,
                    openedAt = 1500,
                    closedAt = null,
                    idempotencyKey = "recovery:debt-level:20002",
                ),
            )
            dao.putRecoveryNode(
                RecoveryNodeEntity(
                    routeId = "recovery-1",
                    questId = "quest-1",
                    position = 0,
                    addedAt = 1600,
                    completedAt = null,
                ),
            )

            try {
                dao.putConsequence(
                    dao.consequences().single().copy(id = "consequence-duplicate"),
                )
                fail("Expected duplicate idempotency key rejection")
            } catch (_: SQLiteConstraintException) {
                // Expected: idempotency key is authoritative, not the display log.
            }
            try {
                dao.putRecoveryNode(
                    RecoveryNodeEntity("recovery-1", "missing-quest", 1, 1700, null),
                )
                fail("Expected foreign-key rejection")
            } catch (_: SQLiteConstraintException) {
                // Expected: recovery state can only reference real quests.
            }
        } finally {
            database.close()
        }

        val reopened = openDatabase()
        try {
            val dao = reopened.dao()
            assertEquals(EarthDatabase.EARTH_NATIVE_NARRATIVE_ID, dao.narrativePreference()?.narrativeId)
            assertEquals(listOf("revision-1"), dao.contractRevisions().map { it.id })
            assertEquals(listOf("consequence-1"), dao.consequences().map { it.id })
            assertEquals(listOf("adjustment-1"), dao.consequenceAdjustments().map { it.id })
            assertEquals(listOf("allocation-1"), dao.repaymentAllocations().map { it.id })
            assertEquals(listOf("contract-reconcile"), dao.clockBoundaries().map { it.id })
            assertEquals(listOf("recovery-1"), dao.recoveryRoutes().map { it.id })
            assertEquals(listOf("quest-1"), dao.recoveryNodes().map { it.questId })

            val repository = WorldRepository(reopened)
            repository.restore(repository.snapshot())
            assertEquals(EarthDatabase.EARTH_NATIVE_NARRATIVE_ID, dao.narrativePreference()?.narrativeId)
            assertTrue(dao.contracts().isEmpty())
            assertTrue(dao.contractRevisions().isEmpty())
            assertTrue(dao.consequences().isEmpty())
            assertTrue(dao.consequenceAdjustments().isEmpty())
            assertTrue(dao.repaymentAllocations().isEmpty())
            assertTrue(dao.clockBoundaries().isEmpty())
            assertTrue(dao.recoveryRoutes().isEmpty())
            assertTrue(dao.recoveryNodes().isEmpty())

            repository.reset()
            assertEquals(null, dao.player())
            assertEquals(null, dao.narrativePreference())
            repository.join("新玩家", "现实服")
            assertEquals(EarthDatabase.EARTH_NATIVE_NARRATIVE_ID, dao.narrativePreference()?.narrativeId)
        } finally {
            reopened.close()
        }
    }

    @Test
    fun failedMigrationLeavesTheV1DatabaseUntouched() {
        val v1 = migrationHelper.createDatabase(databaseName, 1)
        try {
            v1.execSQL(
                """INSERT INTO player
                    (id, name, server, zoneId, joinedAt, onboarded, theme,
                     remindersEnabled, reminderHour, lastReminderDay)
                    VALUES (1, '回滚玩家', '现实服', 'Asia/Shanghai', 1000, 1, 'DARK', 0, 20, NULL)
                """.trimIndent(),
            )
            // A corrupt pre-existing target table makes the official migration fail
            // after Room has opened its upgrade transaction.
            v1.execSQL(
                "CREATE TABLE narrative_preferences (playerId INTEGER NOT NULL PRIMARY KEY)",
            )
        } finally {
            v1.close()
        }

        try {
            migrationHelper.runMigrationsAndValidate(
                databaseName,
                2,
                true,
                EarthDatabase.MIGRATION_1_2,
            ).close()
            fail("Expected migration failure")
        } catch (_: Exception) {
            // Expected: the malformed v1 fixture cannot be upgraded.
        }

        val raw = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        try {
            assertEquals(1, raw.version)
            raw.rawQuery("SELECT name FROM player WHERE id = 1", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("回滚玩家", cursor.getString(0))
            }
            raw.rawQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'contracts'",
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        } finally {
            raw.close()
        }
    }

    private fun openDatabase() = Room.databaseBuilder(context, EarthDatabase::class.java, databaseName)
        .addMigrations(EarthDatabase.MIGRATION_1_2)
        .build()

    private fun insertQuest(
        database: androidx.sqlite.db.SupportSQLiteDatabase,
        id: String,
        kind: String,
        dueDay: Long,
        goalId: String?,
    ) {
        database.execSQL(
            """INSERT INTO quests
                (id, title, description, kind, difficulty, skill, priority, estimatedMinutes,
                 dueDay, goalId, state, snoozedUntilDay, postponeCount, createdAt, updatedAt)
                VALUES (?, '旧任务', '保持不变', ?, 'NORMAL', 'DISCIPLINE', 2, 25,
                        ?, ?, 'ACTIVE', NULL, 0, 1200, 1200)
            """.trimIndent(),
            arrayOf<Any?>(id, kind, dueDay, goalId),
        )
    }
}
