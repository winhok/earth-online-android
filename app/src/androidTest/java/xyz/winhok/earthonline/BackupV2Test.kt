package xyz.winhok.earthonline

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.data.*

@RunWith(AndroidJUnit4::class)
class BackupV2Test {
    @Test
    fun v2RoundTripIncludesEveryPersistentRecord() {
        val snapshot = completeSnapshot()

        val encoded = BackupCodec.encode(snapshot, now = 2_000)
        val decoded = BackupCodec.decodeSnapshot(encoded)

        assertEquals(3, BackupCodec.VERSION)
        assertEquals(snapshot, decoded)
        assertEquals("任务 中文 😀 \\ \"", decoded.world.quests.single().title)
        assertEquals(listOf("contract-1"), decoded.contracts.map { it.id })
        assertEquals(listOf("WAIVER", "REFUND"), decoded.consequenceAdjustments.map { it.kind })
        assertEquals(listOf("recovery-1"), decoded.recoveryRoutes.map { it.id })
        assertEquals(20L, decoded.outstandingDebtXp())
    }

    @Test
    fun duplicateContractIdsAreRejectedBeforeEncoding() {
        val original = completeSnapshot()
        val duplicate = original.contracts.single().copy(questId = "quest-duplicate")

        try {
            BackupCodec.encode(original.copy(contracts = original.contracts + duplicate), now = 2_000)
            fail("Expected duplicate ID rejection")
        } catch (error: RuleViolation) {
            assertEquals(RuleError.INVALID_BACKUP, error.reason)
        }
    }

    @Test
    fun invalidV2RelationsStatesAndAllocationsAreRejected() {
        val valid = completeSnapshot()
        val consequence = valid.consequences.single()
        val invalidSnapshots = listOf(
            "contract quest" to valid.copy(
                contracts = valid.contracts.map { it.copy(questId = "missing") },
            ),
            "contract occurrence" to valid.copy(
                contracts = valid.contracts.map { it.copy(occurrence = "2026-09-08") },
            ),
            "contract status" to valid.copy(
                contracts = valid.contracts.map { it.copy(status = "MYSTERY") },
            ),
            "active contract closed" to valid.copy(
                contracts = valid.contracts.map { it.copy(status = "ACTIVE", closedAt = 1_200) },
            ),
            "terminal contract open" to valid.copy(
                contracts = valid.contracts.map { it.copy(status = "FULFILLED", closedAt = null) },
            ),
            "contract zone" to valid.copy(
                contracts = valid.contracts.map { it.copy(zoneId = "Not/AZone") },
            ),
            "revision contract" to valid.copy(
                contractRevisions = valid.contractRevisions.map { it.copy(contractId = "missing") },
            ),
            "consequence contract" to valid.copy(
                consequences = valid.consequences.map { it.copy(contractId = "missing") },
            ),
            "adjustment consequence" to valid.copy(
                consequenceAdjustments = valid.consequenceAdjustments.map { it.copy(consequenceId = "missing") },
            ),
            "allocation completion" to valid.copy(
                repaymentAllocations = valid.repaymentAllocations.map { it.copy(completionId = "missing") },
            ),
            "allocation revoked completion" to valid.copy(
                world = valid.world.copy(
                    completions = valid.world.completions.map { it.copy(revokedAt = 1_200) },
                ),
            ),
            "over allocation" to valid.copy(
                repaymentAllocations = valid.repaymentAllocations.map { it.copy(xp = consequence.xp + 1) },
            ),
            "over waiver" to valid.copy(
                consequenceAdjustments = valid.consequenceAdjustments.map {
                    if (it.kind == "WAIVER") it.copy(xp = consequence.xp) else it
                },
            ),
            "over refund" to valid.copy(
                consequenceAdjustments = valid.consequenceAdjustments.map {
                    if (it.kind == "REFUND") it.copy(xp = consequence.xp) else it
                },
            ),
            "recovery status" to valid.copy(
                recoveryRoutes = valid.recoveryRoutes.map { it.copy(status = "MYSTERY") },
            ),
            "recovery route" to valid.copy(
                recoveryNodes = valid.recoveryNodes.map { it.copy(routeId = "missing") },
            ),
            "recovery quest" to valid.copy(
                recoveryNodes = valid.recoveryNodes.map { it.copy(questId = "missing") },
            ),
            "overflowing waiver total" to valid.copy(
                consequences = valid.consequences.map { it.copy(xp = Long.MAX_VALUE) },
                consequenceAdjustments = listOf(
                    valid.consequenceAdjustments.first().copy(xp = Long.MAX_VALUE),
                    valid.consequenceAdjustments.first().copy(
                        id = "adjustment-overflow",
                        xp = Long.MAX_VALUE,
                        idempotencyKey = "waiver:overflow",
                    ),
                ),
            ),
        )

        invalidSnapshots.forEach { (case, snapshot) ->
            try {
                BackupCodec.encode(snapshot, now = 2_000)
                fail("Expected rejection: $case")
            } catch (error: RuleViolation) {
                assertEquals("Wrong error for $case", RuleError.INVALID_BACKUP, error.reason)
            }
        }
    }

    @Test
    fun v1BackupUpgradesToEarthNativeWithoutContracts() {
        val decoded = BackupCodec.decodeSnapshot(legacyV1Backup())

        assertEquals("旧玩家 😀", decoded.world.player.name)
        assertEquals(ThemeMode.LIGHT, decoded.world.player.theme)
        assertEquals(20_001L, decoded.world.quests.single().dueDay)
        assertEquals(EarthDatabase.EARTH_NATIVE_NARRATIVE_ID, decoded.narrativePreference.narrativeId)
        assertEquals(emptyList<ContractEntity>(), decoded.contracts)
    }

    @Test
    fun unknownNarrativeIdIsPreservedButFallsBackForDisplay() {
        val unknownId = "third-party.future"
        val snapshot = completeSnapshot().copy(
            narrativePreference = NarrativePreferenceEntity(narrativeId = unknownId),
        )

        val decoded = BackupCodec.decodeSnapshot(BackupCodec.encode(snapshot, now = 2_000))

        assertEquals(unknownId, decoded.narrativePreference.narrativeId)
        assertEquals(
            EarthDatabase.EARTH_NATIVE_NARRATIVE_ID,
            decoded.displayNarrativeId(setOf(EarthDatabase.EARTH_NATIVE_NARRATIVE_ID)),
        )
    }

    @Test
    fun futureProtocolVersionIsRejected() {
        val root = JSONObject(BackupCodec.encode(completeSnapshot(), now = 2_000))
            .put("version", BackupCodec.VERSION + 1)

        try {
            BackupCodec.decodeSnapshot(root.toString())
            fail("Expected future protocol rejection")
        } catch (_: IllegalArgumentException) {
            // Expected before any snapshot can be offered for replacement.
        }
    }

    @Test
    fun validChecksumCannotHideABrokenV2Relation() {
        val root = JSONObject(BackupCodec.encode(completeSnapshot(), now = 2_000))
        val payload = JSONObject(root.getString("payload"))
        payload.getJSONArray("contracts").getJSONObject(0).put("questId", "missing")
        val raw = payload.toString()
        root.put("payload", raw).put("sha256", sha256(raw))

        try {
            BackupCodec.decodeSnapshot(root.toString())
            fail("Expected relational rejection")
        } catch (error: RuleViolation) {
            assertEquals(RuleError.INVALID_BACKUP, error.reason)
        }
    }

    @Test
    fun v2CapacityBudgetCoversEncodedBytesAndRejectsBeforeExport() {
        val snapshot = completeSnapshot()
        val encodedBytes = BackupCodec.encode(snapshot, now = 2_000).toByteArray().size

        assertTrue(BackupSnapshotBudget.upperBound(snapshot) >= encodedBytes)

        val oversized = snapshot.copy(
            recoveryRoutes = snapshot.recoveryRoutes + List(20_000) { index ->
                RecoveryRouteEntity(
                    id = "route-${index.toString().padStart(6, '0')}-${"r".repeat(60)}",
                    status = "CLOSED",
                    triggerKind = "DEBT_LEVEL",
                    targetDebtXp = 1,
                    openedAt = 1,
                    closedAt = 2,
                    idempotencyKey = "recovery:$index:${"k".repeat(170)}",
                )
            },
        )
        try {
            BackupCodec.encode(oversized, now = 2_000)
            fail("Expected v2 capacity rejection")
        } catch (error: RuleViolation) {
            assertEquals(RuleError.LIMIT, error.reason)
        }
    }

    private fun legacyV1Backup(): String {
        val payload = """
            {
              "player":{"name":"旧玩家 😀","server":"现实服","zoneId":"Asia/Shanghai","joinedAt":100,"onboarded":true,"theme":"LIGHT","remindersEnabled":true,"reminderHour":20,"lastReminderDay":null},
              "goals":[],
              "quests":[{"id":"quest-old","title":"旧日期任务","description":"","kind":"SIDE","difficulty":"NORMAL","skill":"DISCIPLINE","priority":2,"estimatedMinutes":25,"dueDay":20001,"goalId":null,"state":"ACTIVE","snoozedUntilDay":null,"postponeCount":0,"createdAt":200,"updatedAt":200}],
              "completions":[],
              "events":[]
            }
        """.trimIndent()
        return JSONObject()
            .put("format", BackupCodec.FORMAT)
            .put("version", BackupCodec.LEGACY_VERSION)
            .put("exportedAt", 1_000)
            .put("sha256", sha256(payload))
            .put("payload", payload)
            .toString()
    }

    private fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun completeSnapshot(): BackupSnapshot {
        val world = World(
            player = Player(
                name = "玩家 中文 😀",
                server = "现实服",
                zoneId = "Asia/Shanghai",
                joinedAt = 100,
                onboarded = true,
                theme = ThemeMode.LIGHT,
                remindersEnabled = true,
                reminderHour = 20,
                lastReminderDay = 20_000,
            ),
            quests = listOf(
                Quest(
                    id = "quest-1",
                    title = "任务 中文 😀 \\ \"",
                    kind = QuestKind.SIDE,
                    difficulty = Difficulty.NORMAL,
                    skill = Skill.DISCIPLINE,
                    dueDay = 20_001,
                    createdAt = 200,
                    updatedAt = 200,
                ),
            ),
            completions = listOf(
                Completion(
                    id = "quest-1:once",
                    questId = "quest-1",
                    occurrence = "once",
                    title = "任务 中文 😀 \\ \"",
                    kind = QuestKind.SIDE,
                    skill = Skill.DISCIPLINE,
                    xp = 25,
                    completedAt = 1_000,
                    completedDay = 20_000,
                ),
            ),
            events = listOf(
                JournalEvent("event-1", EventKind.COMPLETED, "日志 中文 😀\n\\\"", 1_000, "quest-1", 25),
            ),
        )
        return BackupSnapshot(
            world = world,
            narrativePreference = NarrativePreferenceEntity(narrativeId = "cultivation"),
            contracts = listOf(
                ContractEntity(
                    "contract-1", "quest-1", "once", "Asia/Shanghai", 20_001,
                    25, 20, "OVERDUE", 300, 500,
                ),
            ),
            contractRevisions = listOf(
                ContractRevisionEntity(
                    "revision-1", "contract-1", 1, "POSTPONED", 20_000, 20_001,
                    25, 20, 400, "revision:contract-1:1",
                ),
            ),
            consequences = listOf(
                ConsequenceEventEntity("consequence-1", "contract-1", "OVERDUE", 25, 20_002, 500, "overdue:contract-1"),
            ),
            consequenceAdjustments = listOf(
                ConsequenceAdjustmentEntity(
                    "adjustment-waiver", "consequence-1", "WAIVER", 5,
                    "FORCE_MAJEURE", 600, "waiver:consequence-1",
                ),
                ConsequenceAdjustmentEntity(
                    "adjustment-refund", "consequence-1", "REFUND", 20,
                    "FORCE_MAJEURE", 700, "refund:consequence-1",
                ),
            ),
            repaymentAllocations = listOf(
                RepaymentAllocationEntity(
                    "allocation-1", "consequence-1", "quest-1:once", 20,
                    550, "allocation:consequence-1:quest-1:once",
                ),
            ),
            clockBoundaries = listOf(
                ClockBoundaryEntity("contract-reconcile", "Asia/Shanghai", 20_002, 800),
            ),
            recoveryRoutes = listOf(
                RecoveryRouteEntity(
                    "recovery-1", "CLOSED", "DEBT_LEVEL", 20, 900, 1_100,
                    "recovery:debt-level:20002",
                ),
            ),
            recoveryNodes = listOf(
                RecoveryNodeEntity("recovery-1", "quest-1", 0, 950, 1_000),
            ),
        )
    }
}
