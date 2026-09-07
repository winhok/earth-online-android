package xyz.winhok.earthonline

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.*

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
        compose.waitUntil(15_000) { compose.onAllNodesWithText("指挥台").fetchSemanticsNodes().isNotEmpty() }
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
        compose.onNodeWithText("完成今天的一个小目标").assertExists()
        screenshot("02-quests")
        compose.onNodeWithText("角色").performClick()
        screenshot("03-character")
        compose.onNodeWithText("日志").performClick()
        screenshot("04-journal")
        compose.onNodeWithContentDescription("设置与存档").performClick()
        compose.onNodeWithText("玩家设置").assertExists()
        screenshot("05-settings")
    }
    @Test fun activityRecreationRetainsCompletedLedger() {
        val before = runBlocking { repo.snapshot() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("指挥台").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(before, runBlocking { repo.snapshot() })
        assertEquals(10L, runBlocking { ProgressRules.total(repo.snapshot().completions).xp })
    }
    @Test fun lightThemeIsPersistedAndRendered() {
        runBlocking { repo.updatePlayer("冒险测试员", "地球·测试服", ThemeMode.LIGHT, false, 20) }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("指挥台").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(ThemeMode.LIGHT, runBlocking { repo.snapshot().player.theme })
        screenshot("06-light-theme")
    }
}
