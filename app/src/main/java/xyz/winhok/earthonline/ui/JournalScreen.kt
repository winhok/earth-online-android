package xyz.winhok.earthonline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.winhok.earthonline.core.*

@Composable
fun JournalScreen(state: EarthUiState, note: () -> Unit) {
    val presenter = LocalNarrativePresenter.current
    var notesOnly by rememberSaveable { mutableStateOf(false) }
    val events = state.world.events.filter { !notesOnly || it.kind == EventKind.NOTE }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle(
            presenter.text(ScreenSemantic.JOURNAL_TITLE),
            presenter.text(ScreenSemantic.JOURNAL_BODY),
        ) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !notesOnly, onClick = { notesOnly = false }, label = {
                Text(presenter.text(ScreenSemantic.ALL_EVENTS))
            })
            FilterChip(selected = notesOnly, onClick = { notesOnly = true }, label = {
                Text(presenter.text(ScreenSemantic.JOURNAL_NOTES))
            })
        } }
        if (events.isEmpty()) item { EmptyState(
            presenter.text(ScreenSemantic.JOURNAL_EMPTY_TITLE),
            presenter.text(ScreenSemantic.JOURNAL_EMPTY_BODY),
            presenter.text(ActionSemantic.WRITE_NOTE),
            note,
        ) }
        items(events, key = { it.id }) { event ->
            var expanded by rememberSaveable(event.id) { mutableStateOf(false) }
            OutlinedCard(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(presenter.text(ScreenSemantic.EVENT_HEADER, semanticArguments {
                        put(
                            SemanticParameters.EVENT_HEADER,
                            EventHeader(
                                event.kind,
                                CompletionMoment(event.createdAt, state.world.player.zoneId),
                            ),
                        )
                    }),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(event.text, style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis)
                    if (event.xp != 0) Text(presenter.text(ScreenSemantic.JOURNAL_XP, semanticArguments {
                        put(SemanticParameters.SIGNED_XP, SignedXpAmount(event.xp))
                    }),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
