package xyz.winhok.earthonline

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.DestinationSemantic
import xyz.winhok.earthonline.core.NarrativePresentation
import xyz.winhok.earthonline.core.SemanticRequest
import xyz.winhok.earthonline.core.ThemeMode
import xyz.winhok.earthonline.core.ActionSemantic
import xyz.winhok.earthonline.core.Quest
import xyz.winhok.earthonline.core.ScreenSemantic
import xyz.winhok.earthonline.core.EventKind
import xyz.winhok.earthonline.core.JournalEvent
import xyz.winhok.earthonline.core.Player
import xyz.winhok.earthonline.core.World
import xyz.winhok.earthonline.ui.EarthTheme
import xyz.winhok.earthonline.ui.EarthUiState
import xyz.winhok.earthonline.ui.CharacterScreen
import xyz.winhok.earthonline.ui.JournalScreen
import xyz.winhok.earthonline.ui.NarrativeHost
import xyz.winhok.earthonline.ui.NarrativePresenter
import xyz.winhok.earthonline.ui.PrimaryNavigation
import xyz.winhok.earthonline.ui.QuestCard
import xyz.winhok.earthonline.ui.QuestsScreen

@RunWith(AndroidJUnit4::class)
class OperationalNarrativeUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun primaryNavigationRequestsStableDestinationSemanticsAndKeepsActions() {
        val presenter = RecordingPresenter()
        var selected = 0

        compose.setContent {
            EarthTheme(ThemeMode.DARK) {
                NarrativeHost(presenter) {
                    PrimaryNavigation(
                        selectedIndex = selected,
                        expanded = false,
                        onSelect = { selected = it },
                    )
                }
            }
        }

        DestinationSemantic.entries.forEach { destination ->
            compose.onNodeWithText(destination.wireId).assertIsDisplayed()
        }
        assertEquals(DestinationSemantic.entries.toSet(), presenter.requests.map { it.key }.toSet())
        compose.onNodeWithText(DestinationSemantic.QUESTS.wireId).performClick()
        assertEquals(1, selected)
    }

    @Test
    fun questCardRequestsStructuredSemanticsPreservesTitleAndCompletionAction() {
        val presenter = RecordingPresenter()
        var completions = 0
        val quest = Quest(
            id = "quest-1",
            title = "逐字玩家标题 😀",
            postponeCount = 3,
            createdAt = 1,
            updatedAt = 1,
        )

        compose.setContent {
            EarthTheme(ThemeMode.DARK) {
                NarrativeHost(presenter) {
                    QuestCard(
                        quest = quest,
                        day = 1,
                        done = false,
                        busy = false,
                        onOpen = {},
                        onComplete = { completions += 1 },
                    )
                }
            }
        }

        compose.onNodeWithText(quest.title).assertIsDisplayed()
        compose.onNodeWithText(ScreenSemantic.QUEST_META.wireId).assertIsDisplayed()
        compose.onNodeWithContentDescription(ActionSemantic.COMPLETE_QUEST.wireId).performClick()

        assertEquals(1, completions)
        assertTrue(ScreenSemantic.QUEST_REWARD in presenter.requests.map { it.key })
        assertTrue(ScreenSemantic.QUEST_PRIORITY_DIFFICULTY in presenter.requests.map { it.key })
        assertTrue(xyz.winhok.earthonline.core.StateSemantic.REVIEW_REQUIRED in presenter.requests.map { it.key })
    }

    @Test
    fun earthNativeNonEmptyQuestListKeepsCreateActionReachable() {
        val presenter = RecordingPresenter()
        var creates = 0
        val state = EarthUiState(
            world = World(
                player = Player(name = "Tester", zoneId = "UTC", onboarded = true),
                quests = listOf(Quest(id = "existing", title = "Existing", createdAt = 1, updatedAt = 1)),
            ),
            now = 1_757_325_600_000L,
            loading = false,
        )

        compose.setContent {
            EarthTheme(ThemeMode.LIGHT) {
                NarrativeHost(presenter) {
                    QuestsScreen(
                        state = state,
                        create = { creates += 1 },
                        open = {},
                        complete = {},
                        editGoal = {},
                        archiveGoal = {},
                        switchNarrative = {},
                        openLedger = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("quest-create").assertIsDisplayed().performClick()
        assertEquals(1, creates)
    }

    @Test
    fun profileAndJournalRequestStructuredSemanticsWhileKeepingOpaqueHistory() {
        val presenter = RecordingPresenter()
        val rawPlayer = "玩家 XP 原文 😀"
        val rawHistory = "任务与 XP 都是玩家标题 😀"
        val state = EarthUiState(
            world = World(
                player = Player(name = rawPlayer, zoneId = "Asia/Shanghai", onboarded = true),
                events = listOf(JournalEvent(
                    id = "event-1",
                    kind = EventKind.COMPLETED,
                    text = rawHistory,
                    createdAt = 1_757_325_600_000L,
                    xp = -25,
                )),
            ),
            now = 1_757_325_600_000L,
            loading = false,
        )

        compose.setContent {
            EarthTheme(ThemeMode.DARK) {
                NarrativeHost(presenter) {
                    CharacterScreen(state)
                    JournalScreen(state, note = {})
                }
            }
        }

        compose.onNodeWithText(rawPlayer).assertIsDisplayed()
        compose.onNodeWithText(rawHistory).assertIsDisplayed()
        val keys = presenter.requests.map { it.key }
        assertTrue(ScreenSemantic.CHARACTER_TITLE in keys)
        assertTrue(ScreenSemantic.EVENT_HEADER in keys)
        assertTrue(ScreenSemantic.JOURNAL_XP in keys)
    }

    private class RecordingPresenter : NarrativePresenter {
        val requests = mutableListOf<SemanticRequest>()

        override fun present(request: SemanticRequest): NarrativePresentation {
            requests += request
            return NarrativePresentation(request.key.wireId)
        }
    }
}
