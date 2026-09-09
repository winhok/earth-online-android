package xyz.winhok.earthonline

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.Completion
import xyz.winhok.earthonline.core.Difficulty
import xyz.winhok.earthonline.core.EventKind
import xyz.winhok.earthonline.core.JournalEvent
import xyz.winhok.earthonline.core.Player
import xyz.winhok.earthonline.core.Quest
import xyz.winhok.earthonline.core.QuestKind
import xyz.winhok.earthonline.core.Skill
import xyz.winhok.earthonline.core.ThemeMode
import xyz.winhok.earthonline.core.World
import xyz.winhok.earthonline.data.BackupSnapshot
import xyz.winhok.earthonline.data.BackupCodec
import xyz.winhok.earthonline.data.ClockBoundaryEntity
import xyz.winhok.earthonline.data.ConsequenceAdjustmentEntity
import xyz.winhok.earthonline.data.ConsequenceEventEntity
import xyz.winhok.earthonline.data.ContractEntity
import xyz.winhok.earthonline.data.ContractRevisionEntity
import xyz.winhok.earthonline.data.EarthDatabase
import xyz.winhok.earthonline.data.NarrativePreferenceEntity
import xyz.winhok.earthonline.data.RecoveryNodeEntity
import xyz.winhok.earthonline.data.RecoveryRouteEntity
import xyz.winhok.earthonline.data.RepaymentAllocationEntity
import xyz.winhok.earthonline.data.WorldRepository

@RunWith(AndroidJUnit4::class)
class BackupV2RepositoryTest {
    private lateinit var db: EarthDatabase
    private lateinit var repository: WorldRepository
    private val clock = Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            EarthDatabase::class.java,
        ).build()
        repository = WorldRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun fullSnapshotRestoreReplacesEveryTimelineTable() = runBlocking {
        repository.restore(snapshot("local", joinedAt = 9_000))
        val imported = snapshot("older", joinedAt = 100)

        repository.restore(imported)

        val restored = repository.snapshotBackup()
        assertEquals(imported.copy(world = imported.world.copy(
            player = imported.world.player.copy(remindersEnabled = false, lastReminderDay = null),
        )), restored)
        assertEquals("narrative-older", restored.narrativePreference.narrativeId)
        assertEquals(listOf("contract-older"), restored.contracts.map { it.id })
        assertEquals(listOf("consequence-older"), restored.consequences.map { it.id })
        assertEquals(listOf("route-older"), restored.recoveryRoutes.map { it.id })
        assertFalse(restored.world.player.remindersEnabled)
    }

    @Test
    fun sqlFailureRollsBackEveryReplacedTable() = runBlocking {
        repository.restore(snapshot("local", joinedAt = 9_000))
        val before = repository.snapshotBackup()
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER reject_imported_route
            BEFORE INSERT ON recovery_routes
            WHEN NEW.id = 'route-older'
            BEGIN
                SELECT RAISE(ABORT, 'injected restore failure');
            END
            """.trimIndent(),
        )

        try {
            repository.restore(snapshot("older", joinedAt = 100))
            fail("Expected injected replacement failure")
        } catch (error: SQLiteException) {
            // The trigger fires after the old rows have been deleted, so equality proves rollback.
            assertTrue(error.message.orEmpty().contains("injected restore failure"))
        }

        assertEquals(before, repository.snapshotBackup())
    }

    @Test
    fun largeV2GraphRoundTripsAndRestoresWithoutLoss() = runBlocking {
        val size = 500
        val quests = List(size) { index ->
            Quest(
                id = "quest-$index",
                title = "大规模任务 $index 中文 😀",
                kind = QuestKind.SIDE,
                difficulty = Difficulty.NORMAL,
                skill = Skill.DISCIPLINE,
                dueDay = 20_001,
                createdAt = index.toLong() + 1,
                updatedAt = index.toLong() + 1,
            )
        }
        val completions = quests.mapIndexed { index, quest ->
            Completion(
                id = "${quest.id}:once",
                questId = quest.id,
                occurrence = "once",
                title = quest.title,
                kind = quest.kind,
                skill = quest.skill,
                xp = 25,
                completedAt = index.toLong() + 1_000,
                completedDay = 20_000,
            )
        }
        val contracts = quests.mapIndexed { index, quest ->
            ContractEntity(
                id = "contract-$index",
                questId = quest.id,
                occurrence = "once",
                zoneId = "Asia/Shanghai",
                dueDay = 20_001,
                originalRewardXp = 25,
                currentRewardXp = 25,
                status = "OVERDUE",
                signedAt = index.toLong() + 1,
                closedAt = index.toLong() + 2,
            )
        }
        val consequences = contracts.mapIndexed { index, contract ->
            ConsequenceEventEntity(
                id = "consequence-$index",
                contractId = contract.id,
                kind = "OVERDUE",
                xp = 25,
                effectiveDay = 20_002,
                createdAt = index.toLong() + 3,
                idempotencyKey = "consequence:$index",
            )
        }
        val snapshot = BackupSnapshot(
            world = World(
                player = Player(
                    name = "大规模玩家",
                    server = "现实服",
                    zoneId = "Asia/Shanghai",
                    joinedAt = 1,
                    onboarded = true,
                    remindersEnabled = false,
                ),
                quests = quests,
                completions = completions,
            ),
            narrativePreference = NarrativePreferenceEntity(narrativeId = "earth-native"),
            contracts = contracts,
            contractRevisions = contracts.mapIndexed { index, contract ->
                ContractRevisionEntity(
                    id = "revision-$index",
                    contractId = contract.id,
                    sequence = 1,
                    kind = "SIGNED",
                    previousDueDay = null,
                    newDueDay = contract.dueDay,
                    previousRewardXp = 25,
                    newRewardXp = 25,
                    createdAt = index.toLong() + 1,
                    idempotencyKey = "revision:$index",
                )
            },
            consequences = consequences,
            consequenceAdjustments = consequences.mapIndexed { index, consequence ->
                ConsequenceAdjustmentEntity(
                    id = "adjustment-$index",
                    consequenceId = consequence.id,
                    kind = "WAIVER",
                    xp = 5,
                    reasonCategory = "FORCE_MAJEURE",
                    createdAt = index.toLong() + 4,
                    idempotencyKey = "adjustment:$index",
                )
            },
            repaymentAllocations = consequences.mapIndexed { index, consequence ->
                RepaymentAllocationEntity(
                    id = "allocation-$index",
                    consequenceId = consequence.id,
                    completionId = "quest-$index:once",
                    xp = 20,
                    createdAt = index.toLong() + 4,
                    idempotencyKey = "allocation:$index",
                )
            },
            recoveryRoutes = List(size) { index ->
                RecoveryRouteEntity(
                    id = "route-$index",
                    status = "CLOSED",
                    triggerKind = "DEBT_LEVEL",
                    targetDebtXp = 25,
                    openedAt = index.toLong() + 5,
                    closedAt = index.toLong() + 6,
                    idempotencyKey = "route:$index",
                )
            },
            recoveryNodes = List(size) { index ->
                RecoveryNodeEntity(
                    routeId = "route-$index",
                    questId = "quest-$index",
                    position = 0,
                    addedAt = index.toLong() + 5,
                    completedAt = index.toLong() + 6,
                )
            },
        )

        val decoded = BackupCodec.decodeSnapshot(BackupCodec.encode(snapshot, now = 10_000))
        repository.restore(decoded)

        assertEquals(snapshot, decoded)
        val stored = repository.snapshotBackup()
        assertEquals(snapshot.world.player, stored.world.player)
        assertEquals(snapshot.world.quests.toSet(), stored.world.quests.toSet())
        assertEquals(snapshot.world.completions.toSet(), stored.world.completions.toSet())
        assertEquals(snapshot.narrativePreference, stored.narrativePreference)
        assertEquals(snapshot.contracts.toSet(), stored.contracts.toSet())
        assertEquals(snapshot.contractRevisions.toSet(), stored.contractRevisions.toSet())
        assertEquals(snapshot.consequences.toSet(), stored.consequences.toSet())
        assertEquals(snapshot.consequenceAdjustments.toSet(), stored.consequenceAdjustments.toSet())
        assertEquals(snapshot.repaymentAllocations.toSet(), stored.repaymentAllocations.toSet())
        assertEquals(snapshot.recoveryRoutes.toSet(), stored.recoveryRoutes.toSet())
        assertEquals(snapshot.recoveryNodes.toSet(), stored.recoveryNodes.toSet())
    }

    private fun snapshot(suffix: String, joinedAt: Long): BackupSnapshot {
        val questId = "quest-$suffix"
        val completionId = "$questId:once"
        val contractId = "contract-$suffix"
        val consequenceId = "consequence-$suffix"
        val routeId = "route-$suffix"
        return BackupSnapshot(
            world = World(
                player = Player(
                    name = "玩家-$suffix",
                    server = "现实服",
                    zoneId = "Asia/Shanghai",
                    joinedAt = joinedAt,
                    onboarded = true,
                    theme = ThemeMode.DARK,
                    remindersEnabled = true,
                    reminderHour = 20,
                    lastReminderDay = 20_000,
                ),
                quests = listOf(Quest(
                    id = questId,
                    title = "任务-$suffix",
                    kind = QuestKind.SIDE,
                    difficulty = Difficulty.NORMAL,
                    skill = Skill.DISCIPLINE,
                    dueDay = 20_001,
                    createdAt = joinedAt,
                    updatedAt = joinedAt,
                )),
                completions = listOf(Completion(
                    id = completionId,
                    questId = questId,
                    occurrence = "once",
                    title = "任务-$suffix",
                    kind = QuestKind.SIDE,
                    skill = Skill.DISCIPLINE,
                    xp = 25,
                    completedAt = joinedAt + 10,
                    completedDay = 20_000,
                )),
                events = listOf(JournalEvent(
                    id = "event-$suffix",
                    kind = EventKind.COMPLETED,
                    text = "任务-$suffix",
                    createdAt = joinedAt + 10,
                    questId = questId,
                    xp = 25,
                )),
            ),
            narrativePreference = NarrativePreferenceEntity(narrativeId = "narrative-$suffix"),
            contracts = listOf(ContractEntity(
                id = contractId,
                questId = questId,
                occurrence = "once",
                zoneId = "Asia/Shanghai",
                dueDay = 20_001,
                originalRewardXp = 25,
                currentRewardXp = 20,
                status = "OVERDUE",
                signedAt = joinedAt,
                closedAt = joinedAt + 2,
            )),
            contractRevisions = listOf(ContractRevisionEntity(
                id = "revision-$suffix",
                contractId = contractId,
                sequence = 1,
                kind = "POSTPONED",
                previousDueDay = 20_000,
                newDueDay = 20_001,
                previousRewardXp = 25,
                newRewardXp = 20,
                createdAt = joinedAt + 1,
                idempotencyKey = "revision:$suffix",
            )),
            consequences = listOf(ConsequenceEventEntity(
                id = consequenceId,
                contractId = contractId,
                kind = "OVERDUE",
                xp = 25,
                effectiveDay = 20_002,
                createdAt = joinedAt + 2,
                idempotencyKey = "consequence:$suffix",
            )),
            consequenceAdjustments = listOf(ConsequenceAdjustmentEntity(
                id = "adjustment-$suffix",
                consequenceId = consequenceId,
                kind = "WAIVER",
                xp = 5,
                reasonCategory = "FORCE_MAJEURE",
                createdAt = joinedAt + 3,
                idempotencyKey = "adjustment:$suffix",
            )),
            repaymentAllocations = listOf(RepaymentAllocationEntity(
                id = "allocation-$suffix",
                consequenceId = consequenceId,
                completionId = completionId,
                xp = 20,
                createdAt = joinedAt + 4,
                idempotencyKey = "allocation:$suffix",
            )),
            clockBoundaries = listOf(ClockBoundaryEntity(
                id = "boundary-$suffix",
                zoneId = "Asia/Shanghai",
                lastSettledDay = 20_002,
                lastSettledAt = joinedAt + 5,
            )),
            recoveryRoutes = listOf(RecoveryRouteEntity(
                id = routeId,
                status = "OPEN",
                triggerKind = "DEBT_LEVEL",
                targetDebtXp = 20,
                openedAt = joinedAt + 6,
                closedAt = null,
                idempotencyKey = "route:$suffix",
            )),
            recoveryNodes = listOf(RecoveryNodeEntity(
                routeId = routeId,
                questId = questId,
                position = 0,
                addedAt = joinedAt + 7,
                completedAt = null,
            )),
        )
    }
}
