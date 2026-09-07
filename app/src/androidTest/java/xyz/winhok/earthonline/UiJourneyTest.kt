package xyz.winhok.earthonline

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
    @Before fun reset() {
        runBlocking { repo.reset() }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("创建本地角色").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun join() {
        compose.onNode(hasSetTextAction() and hasText("玩家名")).performTextInput("冒险测试员")
        compose.onNodeWithText("创建本地角色").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("指挥台").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun createCompleteAndUndoQuest() {
        join()
        compose.onAllNodesWithText("接取任务").onFirst().performClick()
        compose.onNode(hasSetTextAction() and hasText("任务标题")).performTextInput("完成端到端测试")
        compose.onNodeWithText("保存").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("保存").fetchSemanticsNodes().isEmpty() &&
                runBlocking { repo.snapshot().quests.any { it.title == "完成端到端测试" } }
        }
        compose.onNodeWithText("任务").performClick()
        compose.onNodeWithContentDescription("完成任务：完成端到端测试").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("撤销").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(25L, runBlocking { ProgressRules.total(repo.snapshot().completions).xp })
        compose.onNodeWithText("撤销").performClick()
        compose.waitUntil(10_000) { runBlocking { ProgressRules.total(repo.snapshot().completions).xp == 0L } }
        compose.onNodeWithText("完成端到端测试").assertExists()
    }
    @Test fun editorDraftSurvivesActivityRecreation() {
        join()
        compose.onAllNodesWithText("接取任务").onFirst().performClick()
        compose.onNode(hasSetTextAction() and hasText("任务标题")).performTextInput("旋转后保留草稿")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("旋转后保留草稿").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("保存").performClick()
        compose.waitUntil(10_000) { runBlocking { repo.snapshot().quests.any { it.title == "旋转后保留草稿" } } }
        assertEquals(1, runBlocking { repo.snapshot().quests.size })
    }
}
