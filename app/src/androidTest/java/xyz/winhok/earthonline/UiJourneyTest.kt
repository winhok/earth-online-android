package xyz.winhok.earthonline

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.ProgressRules

@RunWith(AndroidJUnit4::class)
class UiJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val repo get() = (compose.activity.application as EarthApplication).repository
    private fun await(matcher: SemanticsMatcher) {
        compose.waitUntil(15_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }
    @Before fun reset() {
        runBlocking { repo.reset() }
        // Await the repository emission, not a stale button from the previous activity.
        await(hasTestTag("join-form"))
        compose.onNodeWithTag("join-form").performScrollToNode(hasTestTag("player-name"))
        await(hasTestTag("player-name") and isEnabled())
    }
    private fun join() {
        compose.onNodeWithTag("player-name").performTextReplacement("冒险测试员")
        // The IME changes LazyColumn composition; scroll the form before locating its button.
        compose.onNodeWithTag("join-form").performScrollToNode(hasTestTag("join-submit"))
        await(hasTestTag("join-submit") and isEnabled())
        // Verify visibility, then invoke the UI accessibility action without an IME-animation tap race.
        // The signed-release black-box suite independently exercises physical ADB taps.
        compose.onNodeWithTag("join-submit").assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick) { it() }
        await(hasTestTag("create-quest"))
    }
    private fun openEditor(title: String) {
        // The dashboard also has an off-screen empty-state button with the same text.
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
        compose.activityRule.scenario.recreate()
        await(hasTestTag("quest-title") and hasText("旋转后保留草稿"))
        compose.onNodeWithTag("quest-title").assertTextContains("旋转后保留草稿")
        compose.onNodeWithText("保存").performClick()
        compose.waitUntil(15_000) { runBlocking { repo.snapshot().quests.any { it.title == "旋转后保留草稿" } } }
        assertEquals(1, runBlocking { repo.snapshot().quests.size })
    }
}
