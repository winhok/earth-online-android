package xyz.winhok.earthonline.core

@JvmInline
value class NarrativeSystemId private constructor(val wireId: String) {
    companion object {
        val EARTH_NATIVE = NarrativeSystemId("earth-native")

        fun of(wireId: String): NarrativeSystemId {
            require(wireId.length in 1..80)
            require(wireId.all { it.isLetterOrDigit() || it in "-_.:" })
            return NarrativeSystemId(wireId)
        }
    }
}

enum class NarrativeLocale(val wireId: String) { ZH_CN("zh-CN") }

enum class NarrativeScheme(val wireId: String) { LIGHT("light"), DARK("dark") }

sealed interface SemanticKey {
    val wireId: String
    val category: SemanticCategory
}

enum class DestinationSemantic(override val wireId: String) : SemanticKey {
    DASHBOARD("destination.dashboard"),
    QUESTS("destination.quests"),
    PROFILE("destination.profile"),
    JOURNAL("destination.journal"),

    ;

    override val category = SemanticCategory.DESTINATION
}

enum class NarrativeCapability { TEXT, ICON, COLOR, EFFECT, SURFACE }

enum class CapabilitySupport { NATIVE, INHERITED, MIXED, UNSUPPORTED }

enum class SemanticAvailability { DIRECT_CATALOG, INHERITED_CATALOG, SHARED_SURFACE, UNAVAILABLE }

enum class ResourceKind { BRAND, COLOR, EFFECT }

data class NarrativeResource(
    val id: String,
    val kind: ResourceKind,
    val schemes: Set<NarrativeScheme> = emptySet(),
)

enum class NotificationSemantic(override val wireId: String) : SemanticKey {
    REMINDER_CHANNEL("notification.reminder-channel"),
    REMINDER_TITLE("notification.reminder-title"),
    QUEST_COMPLETED("notification.quest-completed"),
    COMPLETION_UNDONE("notification.completion-undone"),
    QUEST_POSTPONED("notification.quest-postponed"),
    REMINDER_AVAILABLE("notification.reminder-available"),
    REMINDER_CONFIG_FAILED("notification.reminder-config-failed"),
    NOTIFICATION_PERMISSION_DENIED("notification.permission-denied"),
    BACKUP_EXPORTED("notification.backup-exported"),
    BACKUP_RESTORED("notification.backup-restored"),
    FILE_IO_FAILED("notification.file-io-failed"),
    OPERATION_FAILED("notification.operation-failed"),
    OPEN_SETTINGS_FAILED("notification.open-settings-failed"),

    ;

    override val category = SemanticCategory.NOTIFICATION
}

class SemanticParameter<T : Any> internal constructor(val wireId: String)

object SemanticParameters {
    val XP = SemanticParameter<XpAmount>("xp")
    val PLAYER_NAME = SemanticParameter<OpaqueText>("player-name")
    val SERVER_NAME = SemanticParameter<OpaqueText>("server-name")
    val TITLE = SemanticParameter<OpaqueText>("title")
    val DESCRIPTION = SemanticParameter<OpaqueText>("description")
    val NOTE = SemanticParameter<OpaqueText>("note")
    val LEVEL = SemanticParameter<LevelNumber>("level")
    val DAY = SemanticParameter<EpochDay>("day")
    val COUNT = SemanticParameter<CountValue>("count")
    val MINUTES = SemanticParameter<CountValue>("minutes")
    val TEXT_LENGTH = SemanticParameter<CountValue>("text-length")
    val PROGRESS = SemanticParameter<LevelProgress>("progress")
    val RECOMMENDATION_REASONS = SemanticParameter<RecommendationReasons>("recommendation-reasons")
    val QUEST_META = SemanticParameter<QuestMetaPresentation>("quest-meta")
    val QUEST_PRIORITY_DIFFICULTY = SemanticParameter<QuestPriorityDifficulty>("quest-priority-difficulty")
    val GOAL_PROGRESS = SemanticParameter<GoalProgress>("goal-progress")
    val GOAL_TITLE = SemanticParameter<GoalTitlePresentation>("goal-title")
    val QUEST_DETAIL = SemanticParameter<QuestDetailPresentation>("quest-detail")
    val COMPLETION_MOMENT = SemanticParameter<CompletionMoment>("completion-moment")
    val DIFFICULTY_REWARD = SemanticParameter<DifficultyReward>("difficulty-reward")
    val COMPLETE_QUEST_LABEL = SemanticParameter<CompleteQuestLabel>("complete-quest-label")
    val UNDO_COMPLETION_LABEL = SemanticParameter<UndoCompletionLabel>("undo-completion-label")
    val ARCHIVE_QUEST_LABEL = SemanticParameter<ArchiveQuestLabel>("archive-quest-label")
    val SKILL_LEVEL = SemanticParameter<SkillLevel>("skill-level")
    val TOTAL_XP = SemanticParameter<TotalXpAmount>("total-xp")
    val EVENT_HEADER = SemanticParameter<EventHeader>("event-header")
    val SIGNED_XP = SemanticParameter<SignedXpAmount>("signed-xp")
    val ZONE_ID = SemanticParameter<ZoneIdValue>("zone-id")
    val VERSION_NAME = SemanticParameter<VersionName>("version-name")
    val RESTORE_SUMMARY = SemanticParameter<RestoreSummary>("restore-summary")
    val DELETE_DATA_LABEL = SemanticParameter<DeleteDataLabel>("delete-data-label")
}

/** Player-authored text is inserted verbatim and never sent through narrative translation. */
@JvmInline
value class OpaqueText(val value: String)

@JvmInline
value class XpAmount(val value: Int) {
    init {
        require(value >= 0)
    }
}

@JvmInline
value class LevelNumber(val value: Int) {
    init {
        require(value > 0)
    }
}

@JvmInline
value class EpochDay(val value: Long)

@JvmInline
value class CountValue(val value: Int) {
    init { require(value >= 0) }
}

data class LevelProgress(val intoLevel: Long, val needed: Long, val completedToday: Int) {
    init { require(intoLevel >= 0 && needed > 0 && completedToday >= 0) }
}

data class RecommendationReasons(val values: List<RecommendationReason>) {
    init { require(values.isNotEmpty()) }
}

data class QuestMetaPresentation(
    val kind: QuestKind,
    val estimatedMinutes: Int,
    val skill: Skill,
    val dueDay: Long?,
    val snoozedUntilDay: Long?,
    val today: Long,
) {
    init { require(estimatedMinutes > 0) }
}

data class QuestPriorityDifficulty(val displayPriority: Int, val difficulty: Difficulty) {
    init { require(displayPriority in 1..3) }
}

data class GoalProgress(val completed: Int, val total: Int, val dailyContributions: Int) {
    init { require(completed >= 0 && total >= 0 && completed <= total && dailyContributions >= 0) }
}

data class GoalTitlePresentation(val title: OpaqueText, val archived: Boolean, val compact: Boolean = false)

data class QuestDetailPresentation(val xp: Int, val postponeCount: Int) {
    init { require(xp >= 0 && postponeCount >= 0) }
}

data class DifficultyReward(val difficulty: Difficulty, val xp: Int) {
    init { require(xp >= 0) }
}

enum class CompleteQuestLabelStyle { SHORT, CONFIRM, ACCESSIBILITY }

data class CompleteQuestLabel(val style: CompleteQuestLabelStyle, val title: OpaqueText? = null) {
    init { require((style == CompleteQuestLabelStyle.ACCESSIBILITY) == (title != null)) }
}

enum class UndoCompletionLabel { SHORT, DETAIL }

enum class ArchiveQuestLabel { DETAIL, CONFIRM }

data class CompletionMoment(val epochMillis: Long, val zoneId: String) {
    init { java.time.ZoneId.of(zoneId) }
}

data class SkillLevel(val skill: Skill, val level: LevelNumber)

@JvmInline
value class TotalXpAmount(val value: Long) {
    init { require(value >= 0) }
}

data class EventHeader(val kind: EventKind, val moment: CompletionMoment)

@JvmInline
value class SignedXpAmount(val value: Int)

@JvmInline
value class ZoneIdValue(val value: String) {
    init { java.time.ZoneId.of(value) }
}

@JvmInline
value class VersionName(val value: String) {
    init { require(value.isNotBlank()) }
}

data class RestoreSummary(
    val questChanges: Int,
    val completionChanges: Int,
    val contractChanges: Int,
    val debtRecordChanges: Int,
    val currentDebtXp: Long,
    val importedDebtXp: Long,
    val recoveryRouteChanges: Int,
    val narrativeChanges: Int,
) {
    init {
        require(
            listOf(
                questChanges,
                completionChanges,
                contractChanges,
                debtRecordChanges,
                recoveryRouteChanges,
                narrativeChanges,
            ).all { it >= 0 },
        )
    }
}

enum class DeleteDataLabel { SETTINGS, CONFIRM }

class SemanticArguments private constructor(
    internal val values: Map<SemanticParameter<*>, Any>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> require(parameter: SemanticParameter<T>): T =
        values[parameter] as? T ?: throw IllegalArgumentException("Missing parameter: ${parameter.wireId}")

    companion object {
        val EMPTY = SemanticArguments(emptyMap())

        internal fun from(values: Map<SemanticParameter<*>, Any>): SemanticArguments =
            SemanticArguments(values.toMap())
    }

    class Builder {
        private val values = linkedMapOf<SemanticParameter<*>, Any>()

        fun <T : Any> put(parameter: SemanticParameter<T>, value: T) {
            require(parameter !in values) { "Duplicate parameter: ${parameter.wireId}" }
            values[parameter] = value
        }

        internal fun build(): SemanticArguments = SemanticArguments(values.toMap())
    }
}

fun semanticArguments(block: SemanticArguments.Builder.() -> Unit): SemanticArguments =
    SemanticArguments.Builder().apply(block).build()

data class SemanticRequest(
    val key: SemanticKey,
    val arguments: SemanticArguments = SemanticArguments.EMPTY,
)

enum class IconRole {
    NEUTRAL,
    COMMAND,
    QUEST,
    GOAL,
    CHARACTER,
    JOURNAL,
    COMPLETION,
    REWARD,
    WARNING,
    ERROR,
    RECOVERY,
    SETTINGS,
    BACKUP,
    DELETE,
}

enum class ColorRole { NEUTRAL, SURFACE, ACTION, REWARD, WARNING, ERROR, RECOVERY, MUTED }

enum class EffectRole { NONE, COMPLETION, LEVEL_UP, ACHIEVEMENT, WARNING, RECOVERY, SYSTEM_SWITCH }

data class NarrativePresentation(
    val text: String,
    val iconRole: IconRole = IconRole.NEUTRAL,
    val colorRole: ColorRole = ColorRole.NEUTRAL,
    val effectRole: EffectRole = EffectRole.NONE,
)

data class NarrativeSemanticEntry(
    val parameters: Set<SemanticParameter<*>>,
    val render: (SemanticArguments) -> NarrativePresentation,
)

class NarrativeCatalog(entries: Map<SemanticKey, NarrativeSemanticEntry>) {
    private val entries = entries.toMap()
    val keys: Set<SemanticKey> get() = entries.keys

    internal fun parameters(key: SemanticKey): Set<SemanticParameter<*>> =
        entries[key]?.parameters ?: throw IllegalArgumentException("Missing semantic: ${key.wireId}")

    fun interpret(request: SemanticRequest): NarrativePresentation {
        val entry = entries[request.key] ?: throw IllegalArgumentException("Missing semantic: ${request.key.wireId}")
        require(request.arguments.values.keys == entry.parameters) {
            "Parameters do not match semantic: ${request.key.wireId}"
        }
        return entry.render(request.arguments)
    }
}

data class NarrativeManifest(
    val id: NarrativeSystemId,
    val structureVersion: Int,
    val contentVersion: Int,
    val locales: Set<NarrativeLocale>,
    val schemes: Set<NarrativeScheme>,
    val capabilities: Map<NarrativeCapability, CapabilitySupport>,
    val semanticCapabilities: Map<SemanticKey, SemanticAvailability>,
    val inheritedSurfaces: Set<DestinationSemantic>,
    val inheritedCatalogEntries: Set<SemanticKey>,
    val resources: Set<NarrativeResource>,
    val catalogEntries: Set<SemanticKey>,
)

data class NarrativeSystemDefinition(
    val manifest: NarrativeManifest,
    val catalog: NarrativeCatalog,
)

class NarrativeRegistry private constructor(
    val defaultSystemId: NarrativeSystemId,
    private val systems: Map<NarrativeSystemId, NarrativeSystemDefinition>,
) {
    fun interpret(
        systemId: NarrativeSystemId,
        request: SemanticRequest,
        locale: NarrativeLocale,
        scheme: NarrativeScheme,
    ): NarrativePresentation {
        val system = systems[systemId] ?: throw IllegalArgumentException("Unknown narrative: ${systemId.wireId}")
        require(locale in system.manifest.locales) { "Unsupported locale: ${locale.wireId}" }
        require(scheme in system.manifest.schemes) { "Unsupported scheme: ${scheme.wireId}" }
        val source = when {
            request.key in system.manifest.catalogEntries -> system
            request.key in system.manifest.inheritedCatalogEntries -> systems.getValue(defaultSystemId)
            else -> system
        }
        return source.catalog.interpret(request)
    }

    fun manifest(systemId: NarrativeSystemId): NarrativeManifest =
        systems[systemId]?.manifest ?: throw IllegalArgumentException("Unknown narrative: ${systemId.wireId}")

    fun definition(systemId: NarrativeSystemId): NarrativeSystemDefinition =
        systems[systemId] ?: throw IllegalArgumentException("Unknown narrative: ${systemId.wireId}")

    fun validate(systemId: NarrativeSystemId) {
        val system = systems[systemId] ?: throw IllegalArgumentException("Unknown narrative: ${systemId.wireId}")
        val manifest = system.manifest
        require(manifest.structureVersion > 0 && manifest.contentVersion > 0) {
            "Narrative versions must be positive"
        }
        require(manifest.locales.isNotEmpty()) { "Narrative locale declaration is empty" }
        require(manifest.schemes == NarrativeScheme.entries.toSet()) {
            "Narrative scheme declaration must cover light and dark"
        }
        require(manifest.capabilities.keys == NarrativeCapability.entries.toSet()) {
            "Narrative capability declaration is incomplete"
        }
        require(manifest.semanticCapabilities.keys == NarrativeSemantics.all) {
            "Narrative semantic declaration is incomplete"
        }
        require(manifest.catalogEntries == system.catalog.keys) {
            "Narrative catalog declaration does not match catalog entries"
        }
        require(
            manifest.semanticCapabilities.filterValues { it == SemanticAvailability.DIRECT_CATALOG }.keys ==
                manifest.catalogEntries,
        )
        require(
            manifest.semanticCapabilities.filterValues { it == SemanticAvailability.INHERITED_CATALOG }.keys ==
                manifest.inheritedCatalogEntries,
        )
        require(manifest.inheritedCatalogEntries.intersect(manifest.catalogEntries).isEmpty())
        if (manifest.id == defaultSystemId) {
            require(manifest.inheritedCatalogEntries.isEmpty())
            require(manifest.semanticCapabilities.values.none { it == SemanticAvailability.SHARED_SURFACE }) {
                "Default narrative cannot rely on shared semantic fallbacks"
            }
        } else {
            require(manifest.inheritedCatalogEntries.all {
                it in systems.getValue(defaultSystemId).manifest.catalogEntries
            })
        }
        require(manifest.resources.isNotEmpty()) { "Narrative resource declaration is empty" }
        require(manifest.resources.map { it.id }.distinct().size == manifest.resources.size)
        require(manifest.resources.all { resource ->
            resource.id.length in 1..120 &&
                resource.id.all { it.isLetterOrDigit() || it in "-_.:" } &&
                resource.schemes.all { it in manifest.schemes }
        })
        require(manifest.resources.map { it.kind }.toSet().containsAll(
            setOf(ResourceKind.BRAND, ResourceKind.COLOR, ResourceKind.EFFECT),
        )) { "Narrative resources must include brand, color, and effect declarations" }
        require(
            manifest.resources.filter { it.kind == ResourceKind.COLOR }
                .flatMapTo(mutableSetOf()) { it.schemes } == manifest.schemes,
        ) { "Narrative color resources must cover every declared scheme" }
        val hasDirectCatalog = manifest.catalogEntries.isNotEmpty()
        val hasInheritedCatalog = manifest.inheritedCatalogEntries.isNotEmpty()
        listOf(
            NarrativeCapability.TEXT,
            NarrativeCapability.ICON,
            NarrativeCapability.COLOR,
            NarrativeCapability.EFFECT,
        ).forEach { capability ->
            when (manifest.capabilities.getValue(capability)) {
                CapabilitySupport.NATIVE -> require(hasDirectCatalog && !hasInheritedCatalog)
                CapabilitySupport.INHERITED -> require(!hasDirectCatalog && hasInheritedCatalog)
                CapabilitySupport.MIXED -> require(hasDirectCatalog && hasInheritedCatalog)
                CapabilitySupport.UNSUPPORTED -> require(!hasDirectCatalog && !hasInheritedCatalog)
            }
        }
        when (manifest.capabilities.getValue(NarrativeCapability.SURFACE)) {
            CapabilitySupport.INHERITED -> require(manifest.inheritedSurfaces == DestinationSemantic.entries.toSet())
            CapabilitySupport.NATIVE -> require(manifest.inheritedSurfaces.isEmpty())
            CapabilitySupport.MIXED -> require(
                manifest.inheritedSurfaces.isNotEmpty() &&
                    manifest.inheritedSurfaces != DestinationSemantic.entries.toSet(),
            )
            CapabilitySupport.UNSUPPORTED -> require(manifest.inheritedSurfaces.isEmpty())
        }
    }

    companion object {
        fun create(
            defaultSystemId: NarrativeSystemId,
            definitions: List<NarrativeSystemDefinition>,
        ): NarrativeRegistry {
            require(definitions.isNotEmpty())
            require(definitions.map { it.manifest.id }.distinct().size == definitions.size) {
                "Duplicate narrative system ID"
            }
            val registry = NarrativeRegistry(
                defaultSystemId = defaultSystemId,
                systems = definitions.associateBy { it.manifest.id },
            )
            require(defaultSystemId in registry.systems) { "Missing default narrative system" }
            definitions.forEach { registry.validate(it.manifest.id) }
            return registry
        }

        fun builtIns(): NarrativeRegistry = create(
            NarrativeSystemId.EARTH_NATIVE,
            listOf(EarthNativeNarrative.definition()),
        )
    }
}
