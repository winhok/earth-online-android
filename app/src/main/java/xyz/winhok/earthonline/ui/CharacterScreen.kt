package xyz.winhok.earthonline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

@Composable
fun CharacterScreen(state: EarthUiState, model: EarthViewModel? = null, ledger: () -> Unit = {}) {
    val presenter = LocalNarrativePresenter.current
    val world = state.world
    val progress = state.progress.current
    val done = world.completions.count { it.revokedAt == null }
    val badges = ProgressRules.achievements(world)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { SectionTitle(
            presenter.text(ScreenSemantic.CHARACTER_TITLE),
            presenter.text(ScreenSemantic.CHARACTER_BODY),
        ) }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlanetMark()
                    Text(world.player.name, style = MaterialTheme.typography.headlineLarge)
                    Text(presenter.text(ScreenSemantic.PROFILE_IDENTITY, semanticArguments {
                        put(SemanticParameters.SERVER_NAME, OpaqueText(world.player.server))
                        put(SemanticParameters.LEVEL, LevelNumber(progress.level))
                    }), style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                    Text(presenter.text(ScreenSemantic.PROFILE_PROGRESS, semanticArguments {
                        put(SemanticParameters.PROGRESS, LevelProgress(progress.intoLevel, progress.needed, 0))
                    }), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item { ContractBalance(state, ledger) }
        if (model != null) item { WorldStory(state, model) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStat(progress.xp.toString(), presenter.text(ScreenSemantic.TOTAL_XP), Modifier.weight(1f))
            SmallStat(done.toString(), presenter.text(ScreenSemantic.COMPLETION_COUNT), Modifier.weight(1f))
        } }
        item { SmallStat(
            presenter.text(ScreenSemantic.CURRENT_STREAK, semanticArguments {
                put(SemanticParameters.COUNT, CountValue(ProgressRules.streak(world.completions, state.day)))
            }),
            presenter.text(ScreenSemantic.STREAK_BODY),
            Modifier.fillMaxWidth(),
        ) }
        item { SectionTitle(
            presenter.text(ScreenSemantic.SKILLS_TITLE),
            presenter.text(ScreenSemantic.SKILLS_BODY),
        ) }
        items(Skill.entries) { skill ->
            val p = ProgressRules.total(world.completions, skill)
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(presenter.text(ScreenSemantic.SKILL_LEVEL, semanticArguments {
                        put(SemanticParameters.SKILL_LEVEL, SkillLevel(skill, LevelNumber(p.level)))
                    }), style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth())
                    Text(presenter.text(ScreenSemantic.SKILL_XP, semanticArguments {
                        put(SemanticParameters.TOTAL_XP, TotalXpAmount(p.xp))
                    }), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { SectionTitle(
            presenter.text(ScreenSemantic.ACHIEVEMENTS_TITLE),
            presenter.text(ScreenSemantic.ACHIEVEMENTS_BODY),
        ) }
        items(AchievementSemantic.entries) { achievementSemantic ->
            val id = achievementSemantic.sourceId
            val achievement = presenter.text(achievementSemantic)
            OutlinedCard(Modifier.fillMaxWidth()) {
                Text(presenter.text(if (id in badges) StateSemantic.UNLOCKED else StateSemantic.LOCKED) +
                    "   " + achievement,
                    modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium,
                    color = if (id in badges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
