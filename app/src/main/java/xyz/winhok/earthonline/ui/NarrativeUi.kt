package xyz.winhok.earthonline.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import xyz.winhok.earthonline.core.NarrativeLocale
import xyz.winhok.earthonline.core.NarrativePresentation
import xyz.winhok.earthonline.core.NarrativeRegistry
import xyz.winhok.earthonline.core.NarrativeScheme
import xyz.winhok.earthonline.core.NarrativeSystemId
import xyz.winhok.earthonline.core.SemanticArguments
import xyz.winhok.earthonline.core.SemanticKey
import xyz.winhok.earthonline.core.SemanticRequest
import xyz.winhok.earthonline.core.Quest
import xyz.winhok.earthonline.core.QuestMetaPresentation
import xyz.winhok.earthonline.core.ScreenSemantic
import xyz.winhok.earthonline.core.SemanticParameters
import xyz.winhok.earthonline.core.semanticArguments
import xyz.winhok.earthonline.core.Difficulty
import xyz.winhok.earthonline.core.QuestKind
import xyz.winhok.earthonline.core.Skill

fun interface NarrativePresenter {
    fun present(request: SemanticRequest): NarrativePresentation
}

class RegistryNarrativePresenter(
    private val registry: NarrativeRegistry,
    private val systemId: NarrativeSystemId,
    private val locale: NarrativeLocale,
    private val scheme: NarrativeScheme,
) : NarrativePresenter {
    override fun present(request: SemanticRequest): NarrativePresentation =
        registry.interpret(systemId, request, locale, scheme)
}

val LocalNarrativePresenter = staticCompositionLocalOf<NarrativePresenter> {
    error("NarrativeHost is required")
}

@Composable
fun NarrativeHost(presenter: NarrativePresenter, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNarrativePresenter provides presenter, content = content)
}

fun NarrativePresenter.present(
    key: SemanticKey,
    arguments: SemanticArguments = SemanticArguments.EMPTY,
): NarrativePresentation = present(SemanticRequest(key, arguments))

fun NarrativePresenter.text(
    key: SemanticKey,
    arguments: SemanticArguments = SemanticArguments.EMPTY,
): String = present(key, arguments).text

fun NarrativePresenter.questMeta(quest: Quest, day: Long): String = text(
    ScreenSemantic.QUEST_META,
    semanticArguments {
        put(
            SemanticParameters.QUEST_META,
            QuestMetaPresentation(
                kind = quest.kind,
                estimatedMinutes = quest.estimatedMinutes,
                skill = quest.skill,
                dueDay = quest.dueDay,
                snoozedUntilDay = quest.snoozedUntilDay,
                today = day,
            ),
        )
    },
)

fun QuestKind.narrativeKey(): ScreenSemantic = when (this) {
    QuestKind.SIDE -> ScreenSemantic.QUEST_KIND_SIDE
    QuestKind.DAILY -> ScreenSemantic.QUEST_KIND_DAILY
    QuestKind.BOSS -> ScreenSemantic.QUEST_KIND_BOSS
}

fun Difficulty.narrativeKey(): ScreenSemantic = when (this) {
    Difficulty.EASY -> ScreenSemantic.DIFFICULTY_EASY
    Difficulty.NORMAL -> ScreenSemantic.DIFFICULTY_NORMAL
    Difficulty.HARD -> ScreenSemantic.DIFFICULTY_HARD
}

fun Skill.narrativeKey(): ScreenSemantic = when (this) {
    Skill.KNOWLEDGE -> ScreenSemantic.SKILL_KNOWLEDGE
    Skill.VITALITY -> ScreenSemantic.SKILL_VITALITY
    Skill.CREATION -> ScreenSemantic.SKILL_CREATION
    Skill.CONNECTION -> ScreenSemantic.SKILL_CONNECTION
    Skill.DISCIPLINE -> ScreenSemantic.SKILL_DISCIPLINE
}
