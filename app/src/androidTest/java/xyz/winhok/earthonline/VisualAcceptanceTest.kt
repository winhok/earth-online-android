package xyz.winhok.earthonline

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import org.json.JSONObject
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.data.deadlineState

/** Real emulator display configurations; captures are not renders of a mock/design image. */
@RunWith(AndroidJUnit4::class)
class VisualAcceptanceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var activity: ActivityScenario<MainActivity>? = null
    private val app get() = ApplicationProvider.getApplicationContext<EarthApplication>()
    private val repo get() = app.repository
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText() }
    private var oldFont = "1.0"
    @Before fun setUp() {
        oldFont=shell("settings get system font_scale").trim()
        runBlocking {
            repo.reset();repo.join("验收员","合成验收存档")
            val today = repo.snapshot().dayAt(System.currentTimeMillis())
            val signing = DeadlineCommand.Save(
                QuestDraft(
                    title = "整理一个真实的下一步",
                    description = "这是验收测试数据，不是产品默认任务。",
                    dueDay = today + 3,
                ),
                "visual-next",
            )
            val plan = repo.plan(signing)
            repo.execute(signing, requireNotNull(plan.disclosure))
            val done=repo.saveQuest(QuestDraft(title="已完成的真实行动",difficulty=Difficulty.EASY));repo.complete(done.id)
            repo.setNarrativeSystem(NarrativeSystemId.CULTIVATION)
        }
    }
    @After fun restoreDevice() {
        try {
            activity?.let { scenario ->
                shell("am start -W -n ${app.packageName}/.MainActivity")
                scenario.close()
            }
        } finally {
            activity = null
            shell("wm size reset");shell("wm density reset")
            shell("settings put system font_scale ${oldFont.toFloatOrNull() ?: 1f}")
        }
    }
    private fun show(mode: ThemeMode, width: Int, height: Int, density: Int, font: Float, expectedWide: Boolean) {
        activity?.close()
        activity = null
        shell("wm size ${width}x${height}");shell("wm density $density")
        shell("settings put system font_scale $font")
        runBlocking { repo.updatePlayer("验收员","合成验收存档",mode,false,20) }
        val launched = ActivityScenario.launch<MainActivity>(MainActivity::class.java)
        activity = launched
        compose.waitUntil(25_000) { compose.onAllNodesWithTag("dashboard-grid").fetchSemanticsNodes().isNotEmpty() }
        launched.onActivity { a ->
            assertEquals(font,a.resources.configuration.fontScale,0.02f)
            assertEquals(expectedWide,a.resources.configuration.screenWidthDp>=720)
        }
        compose.onNodeWithTag("create-quest").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("ledger-navigation").assertIsDisplayed().assertHasClickAction()
    }
    private fun openCultivationQuestSurface() {
        compose.onNodeWithText("历练").performClick()
        compose.waitUntil(25_000) {
            compose.onAllNodesWithTag("quest-battle-pass").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("quests-list").performScrollToNode(hasTestTag("quest-realm-status"))
        compose.onNodeWithTag("quest-realm-status").assertIsDisplayed()
        compose.onNodeWithTag("quests-list").performScrollToNode(hasTestTag("quest-next-contract"))
        compose.onNodeWithTag("quest-next-contract").assertIsDisplayed()
        compose.onNodeWithTag("contract-info").assertExists()
        compose.onNodeWithTag("quest-next-complete").performScrollTo().assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("quests-list").performScrollToNode(hasTestTag("quest-cultivation-track"))
        compose.onNodeWithTag("quest-cultivation-track").assertIsDisplayed()
        compose.onNodeWithTag("quests-list").performScrollToNode(hasTestTag("quest-battle-pass"))
        compose.onNodeWithTag("quest-battle-pass").assertIsDisplayed()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val expected = when {
            name.endsWith("rotated-draft") -> "旋转保留草稿"
            name.endsWith("200pct-action") -> "确认达成"
            else -> "地球 Online"
        }
        fun expectedContentVisible(): Boolean = runCatching {
            compose.onNodeWithText(expected, substring = true, useUnmergedTree = true).assertIsDisplayed()
            true
        }.getOrDefault(false)
        fun appWindowVisible(): Boolean =
            automation.rootInActiveWindow?.packageName?.toString() == app.packageName
        // Display/font changes can schedule a second Activity recreation after Compose
        // first becomes idle. Require both the expected Compose content and the target
        // package's native accessibility window before and after events settle.
        val deadline = SystemClock.uptimeMillis() + 15_000
        var nextForegroundRetry = 0L
        while ((!expectedContentVisible() || !appWindowVisible()) && SystemClock.uptimeMillis() < deadline) {
            if (!appWindowVisible() && SystemClock.uptimeMillis() >= nextForegroundRetry) {
                shell("am start -W -n ${app.packageName}/.MainActivity")
                nextForegroundRetry = SystemClock.uptimeMillis() + 2_000
            }
            SystemClock.sleep(80)
        }
        assertTrue("Expected content missing before capture: $name", expectedContentVisible())
        assertTrue("App window missing before capture: $name", appWindowVisible())
        automation.waitForIdle(700, 10_000)
        assertTrue("Expected content was replaced during configuration change: $name", expectedContentVisible())
        assertTrue("App window was replaced during configuration change: $name", appWindowVisible())
        val bitmap=requireNotNull(automation.takeScreenshot())
        val dir=File(app.getExternalFilesDir(null),"acceptance").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        File(dir,"$name-capture-state.json").writeText(JSONObject()
            .put("expected_text", expected).put("compose_expected_text_visible", true)
            .put("native_app_window_visible", true)
            .put("package", app.packageName).put("settled_accessibility_idle_ms", 700).toString())
        File(dir,"$name-display.txt").writeText(shell("wm size")+shell("wm density")+"font="+shell("settings get system font_scale"))
    }
    @Test fun phoneCultivationDarkLightAndTwoHundredPercentFontRemainOperable() {
        show(ThemeMode.DARK,1080,1920,420,1f,false)
        openCultivationQuestSurface()
        compose.onNodeWithTag("quest-track-heading").assertIsDisplayed()
        capture("v15-phone-cultivation-dark")
        show(ThemeMode.LIGHT,1080,1920,420,1f,false)
        openCultivationQuestSurface()
        capture("v15-phone-cultivation-light")
        show(ThemeMode.LIGHT,1080,1920,420,2f,false)
        openCultivationQuestSurface()
        capture("v15-phone-cultivation-200pct-top")
        compose.onNodeWithTag("quests-list").performScrollToNode(hasTestTag("quest-next-open"))
        compose.onNodeWithTag("quest-next-open").assertIsDisplayed().performClick()
        compose.onNodeWithText("确认达成").performScrollTo().assertIsDisplayed()
        capture("v15-phone-cultivation-200pct-action")
    }
    @Test fun battlePassSearchIsImmediateAndHighlightedQuestSupportsSwipeCompletion() {
        show(ThemeMode.LIGHT,1080,1920,420,1f,false)
        compose.onNodeWithText("历练").performClick()
        compose.waitUntil(25_000) {
            compose.onAllNodesWithTag("quest-next-contract").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("quest-next-contract").performTouchInput { swipeRight() }
        compose.waitUntil(20_000) {
            runBlocking { repo.snapshot().completions.any { it.questId == "visual-next" && it.revokedAt == null } }
        }

        runBlocking {
            repo.saveQuest(QuestDraft(title = "重新出现的下一步", difficulty = Difficulty.NORMAL))
        }
        compose.onNodeWithTag("quest-search-toggle").performClick()
        compose.onNodeWithTag("quest-search-field").assertIsDisplayed().performTextInput("重新出现")
        compose.onNodeWithText("重新出现的下一步").assertExists()
    }
    @Test fun tabletBothSchemesUseActualWideConfigurationAndKeepDraftOnRotation() {
        show(ThemeMode.DARK,1920,1200,240,1f,true)
        openCultivationQuestSurface()
        compose.onNodeWithTag("quest-battle-pass-wide").assertIsDisplayed()
        capture("v15-tablet-cultivation-dark")
        show(ThemeMode.LIGHT,1920,1200,240,1f,true)
        openCultivationQuestSurface()
        capture("v15-tablet-cultivation-light")
        compose.onNodeWithTag("quests-list").performScrollToNode(hasTestTag("quest-create"))
        compose.onNodeWithTag("quest-create").assertIsDisplayed().performClick()
        compose.onNodeWithTag("quest-title").performTextInput("旋转保留草稿")
        shell("wm size 1200x1920")
        compose.waitUntil(25_000) { compose.onAllNodesWithTag("quest-title").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("quest-title").assertTextContains("旋转保留草稿")
        capture("v15-tablet-rotated-draft")
    }
    @Test fun earthNativeUsesSameFactsAndVisibleNonGestureActions() {
        runBlocking { repo.setNarrativeSystem(NarrativeSystemId.EARTH_NATIVE) }
        show(ThemeMode.LIGHT,1080,1920,420,1f,false)
        capture("v15-phone-earth-light")
        compose.onNodeWithTag("next-quest-action").assertIsDisplayed().performClick()
        compose.onNodeWithText("确认完成").assertIsDisplayed().assertHasClickAction()
        val before=runBlocking { repo.snapshotBackup().deadlineState() }
        runBlocking { repo.setNarrativeSystem(NarrativeSystemId.CULTIVATION) }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("确认达成").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(before,runBlocking { repo.snapshotBackup().deadlineState() })
    }
}
