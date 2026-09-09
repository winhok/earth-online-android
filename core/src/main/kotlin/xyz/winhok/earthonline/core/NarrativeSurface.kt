package xyz.winhok.earthonline.core

/** Presentation-ready projection: deliberately contains no World, repository or persistence handle. */
data class NarrativeSurfaceState(
    val destination: DestinationSemantic,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val selectedItemId: String? = null,
    val items: List<NarrativeSurfaceItem> = emptyList(),
)

data class NarrativeSurfaceItem(
    val id: String,
    val presentation: NarrativePresentation,
    val actions: Set<ActionSemantic> = emptySet(),
)

data class NarrativeActionRequest(
    val action: ActionSemantic,
    val targetId: String? = null,
)

fun interface NarrativeActionSink {
    fun dispatch(request: NarrativeActionRequest)
}

/**
 * Optional system-specific surface boundary. Implementations receive only an immutable UI projection
 * and may request mutations only through the stable action port.
 */
interface NarrativeSurfaceProvider<out Surface> {
    val surfaces: Set<DestinationSemantic>

    fun provide(state: NarrativeSurfaceState, actions: NarrativeActionSink): Surface
}
