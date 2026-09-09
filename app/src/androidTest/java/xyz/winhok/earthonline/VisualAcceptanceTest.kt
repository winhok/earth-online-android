package xyz.winhok.earthonline

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
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
    private lateinit var activity: ActivityScenario<MainActivity>
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
            repo.saveQuest(QuestDraft(title="整理一个真实的下一步",description="这是验收测试数据，不是产品默认任务。"))
            val done=repo.saveQuest(QuestDraft(title="已完成的真实行动",difficulty=Difficulty.EASY));repo.complete(done.id)
            repo.setNarrativeSystem(NarrativeSystemId.CULTIVATION)
        }
    }
    @After fun restoreDevice() {
        if (::activity.isInitialized) activity.close()
        shell("wm size reset");shell("wm density reset")
        shell("settings put system font_scale ${oldFont.toFloatOrNull() ?: 1f}")
    }
    private fun show(mode: ThemeMode, width: Int, height: Int, density: Int, font: Float, expectedWide: Boolean) {
        if (::activity.isInitialized) activity.close()
        shell("wm size ${width}x${height}");shell("wm density $density")
        shell("settings put system font_scale $font")
        runBlocking { repo.updatePlayer("验收员","合成验收存档",mode,false,20) }
        activity=ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(25_000) { compose.onAllNodesWithTag("dashboard-grid").fetchSemanticsNodes().isNotEmpty() }
        activity.onActivity { a ->
            assertEquals(font,a.resources.configuration.fontScale,0.02f)
            assertEquals(expectedWide,a.resources.configuration.screenWidthDp>=720)
        }
        compose.onNodeWithTag("create-quest").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("ledger-navigation").assertIsDisplayed().assertHasClickAction()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val bitmap=requireNotNull(automation.takeScreenshot())
        val dir=File(app.getExternalFilesDir(null),"acceptance").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        File(dir,"$name-display.txt").writeText(shell("wm size")+shell("wm density")+"font="+shell("settings get system font_scale"))
    }
    @Test fun phoneCultivationDarkLightAndTwoHundredPercentFontRemainOperable() {
        show(ThemeMode.DARK,1080,1920,420,1f,false)
        compose.onNodeWithTag("next-quest-action").assertIsDisplayed()
        capture("v15-phone-cultivation-dark")
        show(ThemeMode.LIGHT,1080,1920,420,1f,false)
        compose.onNodeWithTag("next-quest-action").assertIsDisplayed()
        capture("v15-phone-cultivation-light")
        show(ThemeMode.LIGHT,1080,1920,420,2f,false)
        capture("v15-phone-cultivation-200pct-top")
        compose.onNodeWithTag("dashboard-grid").performScrollToNode(hasTestTag("next-quest-action"))
        compose.onNodeWithTag("next-quest-action").assertIsDisplayed().performClick()
        compose.onNodeWithText("确认达成").performScrollTo().assertIsDisplayed()
        capture("v15-phone-cultivation-200pct-action")
    }
    @Test fun tabletBothSchemesUseActualWideConfigurationAndKeepDraftOnRotation() {
        show(ThemeMode.DARK,1920,1200,240,1f,true)
        capture("v15-tablet-cultivation-dark")
        show(ThemeMode.LIGHT,1920,1200,240,1f,true)
        capture("v15-tablet-cultivation-light")
        compose.onNodeWithTag("create-quest").performClick()
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
