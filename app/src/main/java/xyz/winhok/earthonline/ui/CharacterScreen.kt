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
fun CharacterScreen(state: EarthUiState) {
    val world = state.world
    val progress = ProgressRules.total(world.completions)
    val done = world.completions.count { it.revokedAt == null }
    val badges = ProgressRules.achievements(world)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { SectionTitle("角色档案", "成长来自行动，而不是清单的长度。") }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlanetMark()
                    Text(world.player.name, style = MaterialTheme.typography.headlineLarge)
                    Text("${world.player.server} · Lv.${progress.level}", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                    Text("${progress.intoLevel} / ${progress.needed} XP", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStat(progress.xp.toString(), "累计经验", Modifier.weight(1f))
            SmallStat(done.toString(), "完成次数", Modifier.weight(1f))
        } }
        item { SmallStat("${ProgressRules.streak(world.completions, state.day)} 天", "当前连续行动 · 休息不会扣经验", Modifier.fillMaxWidth()) }
        item { SectionTitle("技能成长", "这些是游戏内行动记录，不是现实能力评分。") }
        items(Skill.entries) { skill ->
            val p = ProgressRules.total(world.completions, skill)
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${skill.label()} · Lv.${p.level}", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth())
                    Text("${p.xp} XP", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { SectionTitle("成就档案", "成就按有效完成记录计算；撤销完成会同步重新计算。") }
        items(achievementIds) { id ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Text((if (id in badges) "已解锁   " else "未解锁   ") + achievementLabel(id),
                    modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium,
                    color = if (id in badges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
