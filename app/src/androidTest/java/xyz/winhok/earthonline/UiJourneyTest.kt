package xyz.winhok.earthonline

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import android.graphics.Bitmap
import java.io.File
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.ProgressRules

@RunWith(AndroidJUnit4::class)
class UiJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val app get() = ApplicationProvider.getApplicationContext<EarthApplication>()
    private val repo get() = app.repository
    private fun await(matcher: SemanticsMatcher) {
        try {
            compose.waitUntil(15_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: Exception) {
            val world = runBlocking { repo.snapshot() }
            try {
                val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                if (image != null) {
                    val dir = File(app.getExternalFilesDir(null), "acceptance").apply { mkdirs() }
                    File(dir, "ui-journey-failure.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    image.recycle()
                }
            } catch (_: Exception) { /* Retain the original failure even if screenshot capture fails. */ }
            throw AssertionError("UI wait failed: ${matcher.description}; onboarded=${world.player.onboarded}; tasks=${world.quests.size}", failure)
        }
    }
    @Before fun reset() {
        runBlocking { repo.reset() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        await(hasTestTag("join-form"))
        compose.onNodeWithTag("join-form").performScrollToNode(hasTestTag("player-name"))
        await(hasTestTag("player-name") and isEnabled())
    }
    @After fun closeActivity() { scenario.close() }
    private fun join() {
        compose.onNodeWithTag("player-name").performTextReplacement("冒险测试员")
        // Finish IME resizing before scrolling a lazy item into view. Otherwise a
        // pending bring-into-view request can remove the button between assertion and click.
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag("join-form").performScrollToNode(hasTestTag("join-submit"))
        await(hasTestTag("join-submit") and isEnabled())
        compose.onNodeWithTag("join-submit").assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick) { it() }
        await(hasTestTag("create-quest"))
    }
    private fun openEditor(title: String) {
        compose.onNodeWithTag("create-quest").performClick()
        await(hasTestTag("quest-title") and isEnabled())
        compose.onNodeWithTag("quest-title").performTextInput(title)
        compose.onNodeWithText("保存").assertIsEnabled()
    }
    @Test fun createCompleteAndUndoQuest() {
        join()
        openEditor("完成端到端测试")
        compose.onNodeWithText("保存").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("quest-title").fetchSemanticsNodes().isEmpty() &&
                runBlocking { repo.snapshot().quests.any { it.title == "完成端到端测试" } }
        }
        compose.onNodeWithText("任务").performClick()
        await(hasContentDescription("完成任务：完成端到端测试") and isEnabled())
        compose.onNodeWithContentDescription("完成任务：完成端到端测试").performClick()
        await(hasText("撤销") and hasClickAction())
        assertEquals(25L, runBlocking { ProgressRules.total(repo.snapshot().completions).xp })
        compose.onNodeWithText("撤销").performClick()
        compose.waitUntil(15_000) { runBlocking { ProgressRules.total(repo.snapshot().completions).xp == 0L } }
        compose.onNodeWithText("完成端到端测试").assertExists()
    }
    @Test fun editorDraftSurvivesActivityRecreation() {
        join()
        openEditor("旋转后保留草稿")
        scenario.recreate()
        await(hasTestTag("quest-title") and hasText("旋转后保留草稿"))
        compose.onNodeWithTag("quest-title").assertTextContains("旋转后保留草稿")
        compose.onNodeWithText("保存").performClick()
        compose.waitUntil(15_000) { runBlocking { repo.snapshot().quests.any { it.title == "旋转后保留草稿" } } }
        assertEquals(1, runBlocking { repo.snapshot().quests.size })
    }
}
