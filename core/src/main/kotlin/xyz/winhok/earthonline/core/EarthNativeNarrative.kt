package xyz.winhok.earthonline.core

internal object EarthNativeNarrative {
    val catalogEntries = setOf<SemanticKey>(
        NotificationSemantic.QUEST_COMPLETED,
        ScreenSemantic.PLAYER_WELCOME,
        ScreenSemantic.PROFILE_IDENTITY,
        ScreenSemantic.QUEST_CONTENT,
        ScreenSemantic.GOAL_CONTENT,
        ScreenSemantic.NOTE_CONTENT,
    )

    val unavailable = setOf<SemanticKey>(
        FieldSemantic.NARRATIVE_SYSTEM,
        ActionSemantic.RESCHEDULE_CONTRACT,
        ActionSemantic.ABANDON_CONTRACT,
        ActionSemantic.RECOMMIT_CONTRACT,
        ActionSemantic.SETTLE_OVERDUE,
        ActionSemantic.VIEW_DEBT,
        ActionSemantic.OPEN_RECOVERY,
        ActionSemantic.SELECT_RECOVERY_NODE,
        ActionSemantic.APPLY_FORCE_MAJEURE,
        StateSemantic.CONTRACT_ACTIVE,
        StateSemantic.CONTRACT_FULFILLED,
        StateSemantic.CONTRACT_OVERDUE,
        StateSemantic.CONTRACT_ABANDONED,
        StateSemantic.CONTRACT_EXEMPTED,
        StateSemantic.DEBT,
        StateSemantic.OUT_OF_ORDER,
        StateSemantic.RECOVERY_OPEN,
        StateSemantic.RECOVERY_CLOSED,
    )

    fun definition() = NarrativeSystemDefinition(
        manifest = NarrativeManifest(
            id = NarrativeSystemId.EARTH_NATIVE,
            structureVersion = 1,
            contentVersion = 1,
            locales = setOf(NarrativeLocale.ZH_CN),
            schemes = NarrativeScheme.entries.toSet(),
            capabilities = NarrativeCapability.entries.associateWith { capability ->
                if (capability == NarrativeCapability.SURFACE) CapabilitySupport.INHERITED
                else CapabilitySupport.NATIVE
            },
            semanticCapabilities = NarrativeSemantics.all.associateWith { key ->
                when (key) {
                    in catalogEntries -> SemanticAvailability.DIRECT_CATALOG
                    in unavailable -> SemanticAvailability.UNAVAILABLE
                    else -> SemanticAvailability.SHARED_SURFACE
                }
            },
            inheritedSurfaces = DestinationSemantic.entries.toSet(),
            inheritedCatalogEntries = emptySet(),
            resources = setOf(
                NarrativeResource("brand.earth", ResourceKind.BRAND),
                NarrativeResource("color.earth", ResourceKind.COLOR, NarrativeScheme.entries.toSet()),
                NarrativeResource("effect.completion", ResourceKind.EFFECT),
            ),
            catalogEntries = catalogEntries,
        ),
        catalog = catalog(),
    )

    fun catalog() = NarrativeCatalog(
        mapOf<SemanticKey, NarrativeSemanticEntry>(
            NotificationSemantic.QUEST_COMPLETED to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.XP),
                render = { arguments ->
                    val xp = arguments.require(SemanticParameters.XP)
                    NarrativePresentation(
                        text = "任务完成 · +${xp.value} XP",
                        iconRole = IconRole.COMPLETION,
                        colorRole = ColorRole.REWARD,
                        effectRole = EffectRole.COMPLETION,
                    )
                },
            ),
            ScreenSemantic.PLAYER_WELCOME to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.PLAYER_NAME),
                render = { arguments -> NarrativePresentation(
                    text = "${arguments.require(SemanticParameters.PLAYER_NAME).value}，欢迎上线",
                ) },
            ),
            ScreenSemantic.PROFILE_IDENTITY to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.SERVER_NAME, SemanticParameters.LEVEL),
                render = { arguments -> NarrativePresentation(
                    text = "${arguments.require(SemanticParameters.SERVER_NAME).value} · " +
                        "Lv.${arguments.require(SemanticParameters.LEVEL).value}",
                ) },
            ),
            ScreenSemantic.QUEST_CONTENT to contentEntry(),
            ScreenSemantic.GOAL_CONTENT to contentEntry(),
            ScreenSemantic.NOTE_CONTENT to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.NOTE),
                render = { arguments -> NarrativePresentation(
                    text = arguments.require(SemanticParameters.NOTE).value,
                ) },
            ),
        ),
    )

    private fun contentEntry() = NarrativeSemanticEntry(
        parameters = setOf(SemanticParameters.TITLE, SemanticParameters.DESCRIPTION),
        render = { arguments -> NarrativePresentation(
            text = arguments.require(SemanticParameters.TITLE).value + "\n" +
                arguments.require(SemanticParameters.DESCRIPTION).value,
        ) },
    )
}
