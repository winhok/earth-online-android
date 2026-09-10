package xyz.winhok.earthonline

import android.graphics.Bitmap
import android.app.NotificationManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.ui.EarthViewModel
import xyz.winhok.earthonline.reminder.Reminders

@RunWith(AndroidJUnit4::class)
class ScreenAcceptanceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val repo get() = (compose.activity.application as EarthApplication).repository
    @Before fun seed() {
        runBlocking {
            repo.reset()
            repo.join("冒险测试员", "地球·测试服")
            repo.saveQuest(QuestDraft(title = "完成今天的一个小目标", skill = Skill.CREATION))
            val done = repo.saveQuest(QuestDraft(title = "阅读十五分钟", difficulty = Difficulty.EASY, skill = Skill.KNOWLEDGE))
            repo.complete(done.id)
            repo.addNote("今天开始自己的冒险。")
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("冒险测试员，欢迎上线").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val dir = File(compose.activity.getExternalFilesDir(null), "acceptance").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun allMainScreensAndSettingsRender() {
        screenshot("01-dashboard")
        compose.onNodeWithText("任务").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("完成今天的一个小目标").fetchSemanticsNodes().isNotEmpty() }
        screenshot("02-quests")
        compose.onNodeWithText("角色").performClick()
        screenshot("03-character")
        compose.onNodeWithText("日志").performClick()
        screenshot("04-journal")
        compose.waitUntil(15_000) { compose.onAllNodes(hasContentDescription("设置与存档") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("设置与存档").performClick()
        compose.onNodeWithText("玩家设置").assertExists()
        screenshot("05-settings")
    }
    @Test fun activityRecreationRetainsCompletedLedger() {
        val before = runBlocking { repo.snapshot() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("冒险测试员，欢迎上线").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(before, runBlocking { repo.snapshot() })
        assertEquals(10L, runBlocking { ProgressRules.total(repo.snapshot().completions).xp })
    }
    @Test fun lightThemeIsPersistedAndRendered() {
        runBlocking { repo.updatePlayer("冒险测试员", "地球·测试服", ThemeMode.LIGHT, false, 20) }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("冒险测试员，欢迎上线").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(ThemeMode.LIGHT, runBlocking { repo.snapshot().player.theme })
        screenshot("06-light-theme")
    }
    @Test fun settingsSwitchPersistsCultivationAndKeepsUnsavedSettingsDraft() {
        runBlocking {
            repo.updatePlayer("冒险测试员", "地球·测试服", ThemeMode.LIGHT, false, 20)
        }
        val before = runBlocking { repo.snapshot() }
        compose.onNodeWithContentDescription("设置与存档").performClick()
        compose.onNodeWithTag("settings-player-name").performTextReplacement("尚未保存的名字")
        val previewTitle = "完成今天的一个小目标"
        compose.onNodeWithTag("settings-list").performScrollToNode(
            hasTestTag("narrative-cultivation"),
        )
        compose.onNode(
            hasTestTag("narrative-earth-native") and hasAnyDescendant(hasText(previewTitle)),
            useUnmergedTree = true,
        ).assertExists()
        compose.onNode(
            hasTestTag("narrative-cultivation") and hasAnyDescendant(hasText(previewTitle)),
            useUnmergedTree = true,
        ).assertExists()
        compose.onNodeWithTag("narrative-cultivation").performScrollTo().performClick()
        compose.waitUntil(30_000) {
            runBlocking { repo.snapshotBackup().narrativePreference.narrativeId } == "cultivation" &&
                compose.onAllNodes(
                    hasContentDescription("关闭编辑") and isEnabled(),
                ).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals("cultivation", runBlocking { repo.snapshotBackup().narrativePreference.narrativeId })
        val notificationManager = compose.activity.getSystemService(NotificationManager::class.java)
        compose.waitUntil(30_000) {
            notificationManager.getNotificationChannel(Reminders.CHANNEL)?.name?.toString() ==
                "每日修行提醒"
        }

        compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("settings-player-name"))
        compose.onNodeWithTag("settings-player-name").assertTextContains("尚未保存的名字")
        assertEquals(before, runBlocking { repo.snapshot() })
        compose.onNodeWithContentDescription("关闭编辑").performClick()
        compose.onNodeWithText("放弃修改").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("历练").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(ThemeMode.LIGHT, runBlocking { repo.snapshot().player.theme })
        listOf(
            "历练" to "修行轨迹",
            "境界" to "修行境界",
            "修行志" to "每次行动与选择都留下可追溯的道痕。",
            "洞天" to "下一项宗门任务",
        ).forEach { (destination, expectedContent) ->
            compose.onNodeWithText(destination).performClick()
            compose.onNodeWithText(expectedContent).assertExists()
        }

        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("洞天").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(
            "cultivation",
            runBlocking { repo.snapshotBackup().narrativePreference.narrativeId },
        )
    }

    @Test fun narrativeRecompositionKeepsDestinationFilterScrollSelectionAndEditorDraft() {
        val lastTitle = "保持滚动锚点的真实任务"
        runBlocking {
            repeat(16) { index -> repo.saveQuest(QuestDraft(title = "滚动任务 $index")) }
            repo.saveQuest(QuestDraft(title = lastTitle))
        }
        compose.onNodeWithText("任务").performClick()
        compose.onNodeWithTag("quests-list").performScrollToNode(hasText(lastTitle))
        compose.onNodeWithText(lastTitle).assertIsDisplayed()

        runBlocking { repo.setNarrativeSystem(NarrativeSystemId.CULTIVATION) }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("历练").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(lastTitle).assertIsDisplayed()
        compose.onNodeWithTag("quests-list").performScrollToNode(hasTestTag("quest-search-toggle"))
        compose.onNodeWithTag("quest-search-toggle").performClick()
        compose.onNodeWithTag("quests-list").performScrollToNode(
            hasTestTag("quest-filter-state.active"),
        )
        compose.onNodeWithTag("quest-filter-state.active").assertIsSelected()
        compose.onNodeWithTag("quests-list").performScrollToNode(hasText(lastTitle))
        compose.onNodeWithText(lastTitle).assertIsDisplayed().performClick()
        compose.onNodeWithText("确认达成").assertIsDisplayed()

        runBlocking { repo.setNarrativeSystem(NarrativeSystemId.EARTH_NATIVE) }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("确认完成").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("编辑任务").performClick()
        compose.onNodeWithTag("quest-title").performTextReplacement("未保存编辑草稿")

        runBlocking { repo.setNarrativeSystem(NarrativeSystemId.CULTIVATION) }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("支线历练").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("quest-title").assertTextContains("未保存编辑草稿")
        assertEquals(lastTitle, runBlocking {
            repo.snapshot().quests.first { it.title == lastTitle }.title
        })
    }

    @Test fun invalidTargetKeepsEarthWhileUnknownStoredRequestFallsBackWithoutOverwrite() {
        val model = ViewModelProvider(compose.activity)[EarthViewModel::class.java]
        val before = runBlocking { repo.snapshotBackup() }

        model.switchNarrative(NarrativeSystemId.of("missing-target"))
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("操作遇到错误", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(before, runBlocking { repo.snapshotBackup() })

        val requestedId = "future-compatible-system"
        runBlocking { repo.setNarrativeSystem(NarrativeSystemId.of(requestedId)) }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("任务").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(
            requestedId,
            runBlocking { repo.snapshotBackup().narrativePreference.narrativeId },
        )
        compose.onNodeWithContentDescription("设置与存档").performClick()
        compose.onNodeWithTag("settings-list").performScrollToNode(
            hasText(requestedId, substring = true),
        )
        compose.onNodeWithText(requestedId, substring = true).assertIsDisplayed()
    }
}
