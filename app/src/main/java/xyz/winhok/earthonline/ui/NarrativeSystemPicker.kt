package xyz.winhok.earthonline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

@Composable
fun NarrativeSystemPicker(
    selectedSystemId: NarrativeSystemId,
    requestedSystemId: String,
    previewQuest: Quest?,
    day: Long,
    enabled: Boolean,
    onSelect: (NarrativeSystemId) -> Unit,
) {
    val currentPresenter = LocalNarrativePresenter.current
    val registry = remember { NarrativeRegistry.builtIns() }
    val scheme = if (MaterialTheme.colorScheme.background.luminance() < .5f) {
        NarrativeScheme.DARK
    } else {
        NarrativeScheme.LIGHT
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            currentPresenter.text(FieldSemantic.NARRATIVE_SYSTEM),
            currentPresenter.text(ScreenSemantic.NARRATIVE_SYSTEM_BODY),
        )
        if (requestedSystemId != selectedSystemId.wireId) {
            Text(
                currentPresenter.text(ScreenSemantic.NARRATIVE_FALLBACK, semanticArguments {
                    put(SemanticParameters.NARRATIVE_ID, RequestedNarrativeId(requestedSystemId))
                }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        NarrativeSystemId.entriesForUi().forEach { systemId ->
            val previewPresenter = remember(systemId, scheme) {
                RegistryNarrativePresenter(
                    registry = registry,
                    systemId = systemId,
                    locale = NarrativeLocale.ZH_CN,
                    scheme = scheme,
                )
            }
            val selected = selectedSystemId == systemId
            val nameKey = if (systemId == NarrativeSystemId.EARTH_NATIVE) {
                ScreenSemantic.EARTH_NATIVE_SYSTEM_NAME
            } else {
                ScreenSemantic.CULTIVATION_SYSTEM_NAME
            }
            val bodyKey = if (systemId == NarrativeSystemId.EARTH_NATIVE) {
                ScreenSemantic.EARTH_NATIVE_SYSTEM_BODY
            } else {
                ScreenSemantic.CULTIVATION_SYSTEM_BODY
            }
            ElevatedCard(
                onClick = { onSelect(systemId) },
                enabled = enabled && !selected,
                modifier = Modifier.fillMaxWidth().testTag("narrative-${systemId.wireId}"),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(previewPresenter.text(nameKey), style = MaterialTheme.typography.titleMedium)
                            Text(
                                previewPresenter.text(bodyKey),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            enabled = enabled,
                        )
                    }
                    if (previewQuest == null) {
                        Text(
                            currentPresenter.text(ScreenSemantic.NARRATIVE_PREVIEW_EMPTY),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        HorizontalDivider()
                        Text(previewQuest.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            previewPresenter.questMeta(previewQuest, day),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            previewPresenter.text(ScreenSemantic.QUEST_REWARD, semanticArguments {
                                put(SemanticParameters.XP, XpAmount(QuestRules.reward(previewQuest)))
                            }),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

private fun NarrativeSystemId.Companion.entriesForUi(): List<NarrativeSystemId> =
    listOf(NarrativeSystemId.EARTH_NATIVE, NarrativeSystemId.CULTIVATION)
