package xyz.winhok.earthonline

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.Completion
import xyz.winhok.earthonline.core.QuestKind
import xyz.winhok.earthonline.core.Skill
import xyz.winhok.earthonline.core.Player
import xyz.winhok.earthonline.core.Quest
import xyz.winhok.earthonline.core.World
import xyz.winhok.earthonline.data.BackupSnapshot
import xyz.winhok.earthonline.data.ConsequenceEventEntity
import xyz.winhok.earthonline.data.ContractEntity
import xyz.winhok.earthonline.data.NarrativePreferenceEntity
import xyz.winhok.earthonline.data.RecoveryRouteEntity
import xyz.winhok.earthonline.ui.RestorePreview
import xyz.winhok.earthonline.ui.RestorePreviewDialog

@RunWith(AndroidJUnit4::class)
class RestorePreviewDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun previewShowsAllRequiredChangeCountsAndWiresActions() {
        val current = BackupSnapshot(
            world = World(player = Player(name = "当前玩家", onboarded = true)),
        )
        val imported = BackupSnapshot(
            world = World(
                player = Player(name = "旧时间线玩家", onboarded = true),
                quests = listOf(
                    Quest(id = "quest-1", title = "任务一", createdAt = 1, updatedAt = 1),
                    Quest(id = "quest-2", title = "任务二", createdAt = 2, updatedAt = 2),
                ),
                completions = listOf(Completion(
                    id = "quest-1:once",
                    questId = "quest-1",
                    occurrence = "once",
                    title = "任务一",
                    kind = QuestKind.SIDE,
                    skill = Skill.DISCIPLINE,
                    xp = 25,
                    completedAt = 3,
                    completedDay = 20_000,
                )),
            ),
            narrativePreference = NarrativePreferenceEntity(narrativeId = "cultivation"),
            contracts = listOf(ContractEntity(
                id = "contract-1",
                questId = "quest-1",
                occurrence = "once",
                zoneId = "Asia/Shanghai",
                dueDay = 20_001,
                originalRewardXp = 25,
                currentRewardXp = 25,
                status = "OVERDUE",
                signedAt = 1,
                closedAt = 2,
            )),
            consequences = listOf(ConsequenceEventEntity(
                id = "consequence-1",
                contractId = "contract-1",
                kind = "OVERDUE",
                xp = 10,
                effectiveDay = 20_002,
                createdAt = 2,
                idempotencyKey = "consequence:1",
            )),
            recoveryRoutes = listOf(RecoveryRouteEntity(
                id = "route-1",
                status = "OPEN",
                triggerKind = "DEBT_LEVEL",
                targetDebtXp = 10,
                openedAt = 3,
                closedAt = null,
                idempotencyKey = "route:1",
            )),
        )
        val preview = RestorePreview.between(current, imported)
        var confirms = 0
        var dismisses = 0

        compose.setContent {
            MaterialTheme {
                RestorePreviewDialog(
                    preview = preview,
                    busy = false,
                    onConfirm = { confirms++ },
                    onDismiss = { dismisses++ },
                )
            }
        }

        listOf(
            "任务变化：2 项",
            "完成变化：1 项",
            "契约变化：1 项",
            "债务记录变化：1 项（0 XP → 10 XP）",
            "恢复路线变化：1 项",
            "叙事体系变化：1 项",
        ).forEach { text -> compose.onNodeWithText(text).assertIsDisplayed() }
        compose.onNodeWithText("旧时间线玩家").assertIsDisplayed()

        compose.onNodeWithText("确认覆盖").performClick()
        compose.onNodeWithText("取消").performClick()
        assertEquals(1, confirms)
        assertEquals(1, dismisses)
    }
}
