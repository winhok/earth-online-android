package xyz.winhok.earthonline.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrativeContractTest {
    @Test
    fun earthNativeInterpretsRepresentativeCompletionFromStructuredXp() {
        val presentation = NarrativeRegistry.builtIns().interpret(
            systemId = NarrativeSystemId.EARTH_NATIVE,
            request = SemanticRequest(
                key = NotificationSemantic.QUEST_COMPLETED,
                arguments = semanticArguments {
                    put(SemanticParameters.XP, XpAmount(25))
                },
            ),
            locale = NarrativeLocale.ZH_CN,
            scheme = NarrativeScheme.DARK,
        )

        assertEquals("任务完成 · +25 XP", presentation.text)
        assertEquals(IconRole.COMPLETION, presentation.iconRole)
        assertEquals(ColorRole.REWARD, presentation.colorRole)
        assertEquals(EffectRole.COMPLETION, presentation.effectRole)
    }

    @Test
    fun earthNativeInterpretsAllPrimaryDestinations() {
        val registry = NarrativeRegistry.builtIns()
        val expected = mapOf(
            DestinationSemantic.DASHBOARD to "指挥台",
            DestinationSemantic.QUESTS to "任务",
            DestinationSemantic.PROFILE to "角色",
            DestinationSemantic.JOURNAL to "日志",
        )

        expected.forEach { (key, text) ->
            assertEquals(
                text,
                registry.interpret(
                    NarrativeSystemId.EARTH_NATIVE,
                    SemanticRequest(key),
                    NarrativeLocale.ZH_CN,
                    NarrativeScheme.DARK,
                ).text,
            )
        }
    }

    @Test
    fun earthNativeInterpretsOperationalShellDashboardAndQuestCardCopy() {
        val registry = NarrativeRegistry.builtIns()
        fun text(key: SemanticKey, arguments: SemanticArguments = SemanticArguments.EMPTY) =
            registry.interpret(
                NarrativeSystemId.EARTH_NATIVE,
                SemanticRequest(key, arguments),
                NarrativeLocale.ZH_CN,
                NarrativeScheme.DARK,
            ).text

        assertEquals("地球 Online", text(ScreenSemantic.APP_NAME))
        assertEquals("现实没有重开键，\n但随时可以接取下一项任务。", text(ScreenSemantic.JOIN_TAGLINE))
        assertEquals("玩家名", text(FieldSemantic.PLAYER_NAME))
        assertEquals("创建本地角色", text(ActionSemantic.JOIN))
        assertEquals("重新读取", text(ActionSemantic.RETRY_LOAD))
        assertEquals("正在读取存档", text(StateSemantic.LOADING))
        assertEquals("所属任务", text(ActionSemantic.VIEW_GOAL_QUESTS))
        assertEquals(
            "Lv.4  现实探索者",
            text(ScreenSemantic.DASHBOARD_LEVEL, semanticArguments {
                put(SemanticParameters.LEVEL, LevelNumber(4))
            }),
        )
        assertEquals(
            "23 / 100 XP · 今日完成 2 项",
            text(ScreenSemantic.DASHBOARD_PROGRESS, semanticArguments {
                put(SemanticParameters.PROGRESS, LevelProgress(23, 100, 2))
            }),
        )
        assertEquals(
            "已过截止日 · 精力匹配",
            text(ScreenSemantic.RECOMMENDATION_REASONS, semanticArguments {
                put(
                    SemanticParameters.RECOMMENDATION_REASONS,
                    RecommendationReasons(listOf(RecommendationReason.OVERDUE, RecommendationReason.FITS_ENERGY)),
                )
            }),
        )
        assertEquals(
            "支线 · 25 分钟 · 学识 · 2026-09-09 截止 · 暂缓至 2026-09-10",
            text(ScreenSemantic.QUEST_META, semanticArguments {
                put(
                    SemanticParameters.QUEST_META,
                    QuestMetaPresentation(
                        kind = QuestKind.SIDE,
                        estimatedMinutes = 25,
                        skill = Skill.KNOWLEDGE,
                        dueDay = java.time.LocalDate.of(2026, 9, 9).toEpochDay(),
                        snoozedUntilDay = java.time.LocalDate.of(2026, 9, 10).toEpochDay(),
                        today = java.time.LocalDate.of(2026, 9, 9).toEpochDay(),
                    ),
                )
            }),
        )
        assertEquals(
            "完成任务：逐字标题 😀",
            text(ActionSemantic.COMPLETE_QUEST, semanticArguments {
                put(
                    SemanticParameters.COMPLETE_QUEST_LABEL,
                    CompleteQuestLabel(CompleteQuestLabelStyle.ACCESSIBILITY, OpaqueText("逐字标题 😀")),
                )
            }),
        )
        assertEquals(
            "P2 · 挑战",
            text(ScreenSemantic.QUEST_PRIORITY_DIFFICULTY, semanticArguments {
                put(SemanticParameters.QUEST_PRIORITY_DIFFICULTY, QuestPriorityDifficulty(2, Difficulty.HARD))
            }),
        )
        assertEquals(
            "一次性任务 2 / 5 · 日常贡献 7 次",
            text(ScreenSemantic.GOAL_PROGRESS, semanticArguments {
                put(SemanticParameters.GOAL_PROGRESS, GoalProgress(2, 5, 7))
            }),
        )
    }

    @Test
    fun earthNativeInterpretsProfileAndHistoryCopyFromStructuredFacts() {
        val registry = NarrativeRegistry.builtIns()
        fun text(key: SemanticKey, arguments: SemanticArguments = SemanticArguments.EMPTY) =
            registry.interpret(
                NarrativeSystemId.EARTH_NATIVE,
                SemanticRequest(key, arguments),
                NarrativeLocale.ZH_CN,
                NarrativeScheme.DARK,
            ).text

        assertEquals("角色档案", text(ScreenSemantic.CHARACTER_TITLE))
        assertEquals(
            "6 天",
            text(ScreenSemantic.CURRENT_STREAK, semanticArguments {
                put(SemanticParameters.COUNT, CountValue(6))
            }),
        )
        assertEquals(
            "学识 · Lv.3",
            text(ScreenSemantic.SKILL_LEVEL, semanticArguments {
                put(SemanticParameters.SKILL_LEVEL, SkillLevel(Skill.KNOWLEDGE, LevelNumber(3)))
            }),
        )
        assertEquals(
            "2147483648 XP",
            text(ScreenSemantic.SKILL_XP, semanticArguments {
                put(SemanticParameters.TOTAL_XP, TotalXpAmount(2_147_483_648L))
            }),
        )
        assertEquals("已解锁", text(StateSemantic.UNLOCKED))
        assertEquals("第一步 · 完成 1 项任务", text(AchievementSemantic.FIRST_STEP))
        assertEquals(
            "任务完成 · 09-08 18:00",
            text(ScreenSemantic.EVENT_HEADER, semanticArguments {
                put(
                    SemanticParameters.EVENT_HEADER,
                    EventHeader(
                        EventKind.COMPLETED,
                        CompletionMoment(1_757_325_600_000L, "Asia/Shanghai"),
                    ),
                )
            }),
        )
        assertEquals(
            "-25 XP",
            text(ScreenSemantic.JOURNAL_XP, semanticArguments {
                put(SemanticParameters.SIGNED_XP, SignedXpAmount(-25))
            }),
        )
    }

    @Test
    fun earthNativeInterpretsSettingsErrorsAndPrivateReminderCopy() {
        val registry = NarrativeRegistry.builtIns()
        fun text(key: SemanticKey, arguments: SemanticArguments = SemanticArguments.EMPTY) =
            registry.interpret(
                NarrativeSystemId.EARTH_NATIVE,
                SemanticRequest(key, arguments),
                NarrativeLocale.ZH_CN,
                NarrativeScheme.DARK,
            ).text

        assertEquals("通知权限未开启，任务功能不受影响。可在系统设置中手动开启。",
            text(NotificationSemantic.NOTIFICATION_PERMISSION_DENIED))
        assertEquals("标题需为 1–120 个字符。", text(RuleError.TITLE.semantic()))
        assertEquals("每日冒险提醒", text(NotificationSemantic.REMINDER_CHANNEL))
        assertEquals("冒险仍在继续", text(NotificationSemantic.REMINDER_TITLE))
        assertEquals(
            "还有 4 项可执行任务。选一件小事，继续前进。",
            text(NotificationSemantic.REMINDER_AVAILABLE, semanticArguments {
                put(SemanticParameters.COUNT, CountValue(4))
            }),
        )
        assertEquals(
            "地球 Online 2.0-test",
            text(ScreenSemantic.ABOUT_TITLE, semanticArguments {
                put(SemanticParameters.VERSION_NAME, VersionName("2.0-test"))
            }),
        )
        val restore = text(ScreenSemantic.RESTORE_SUMMARY, semanticArguments {
            put(SemanticParameters.RESTORE_SUMMARY, RestoreSummary(1, 2, 3, 4, 5, 6, 7, 8))
        })
        assertTrue(restore.contains("债务记录变化：4 项（5 XP → 6 XP）"))
        assertFalse(restore.contains("私人任务标题"))
    }

    @Test
    fun earthNativeManifestDeclaresVersionsMatrixInheritanceAndResources() {
        val registry = NarrativeRegistry.builtIns()

        registry.validate(NarrativeSystemId.EARTH_NATIVE)
        val manifest = registry.manifest(NarrativeSystemId.EARTH_NATIVE)

        assertEquals(NarrativeSystemId.EARTH_NATIVE, registry.defaultSystemId)
        assertEquals("earth-native", manifest.id.wireId)
        assertEquals(1, manifest.structureVersion)
        assertEquals(1, manifest.contentVersion)
        assertEquals(setOf(NarrativeLocale.ZH_CN), manifest.locales)
        assertEquals(NarrativeScheme.entries.toSet(), manifest.schemes)
        assertEquals(NarrativeCapability.entries.toSet(), manifest.capabilities.keys)
        assertEquals(CapabilitySupport.INHERITED, manifest.capabilities.getValue(NarrativeCapability.SURFACE))
        assertEquals(DestinationSemantic.entries.toSet(), manifest.inheritedSurfaces)
        assertTrue(manifest.inheritedCatalogEntries.isEmpty())
        assertEquals(NarrativeSemantics.all, manifest.semanticCapabilities.keys)
        assertEquals(
            SemanticAvailability.DIRECT_CATALOG,
            manifest.semanticCapabilities.getValue(NotificationSemantic.QUEST_COMPLETED),
        )
        assertEquals(
            SemanticAvailability.DIRECT_CATALOG,
            manifest.semanticCapabilities.getValue(ActionSemantic.COMPLETE_QUEST),
        )
        assertEquals(
            SemanticAvailability.UNAVAILABLE,
            manifest.semanticCapabilities.getValue(StateSemantic.CONTRACT_OVERDUE),
        )
        assertEquals(
            SemanticAvailability.UNAVAILABLE,
            manifest.semanticCapabilities.getValue(FieldSemantic.NARRATIVE_SYSTEM),
        )
        assertEquals(
            setOf(ResourceKind.BRAND, ResourceKind.COLOR, ResourceKind.EFFECT),
            manifest.resources.map { it.kind }.toSet(),
        )
        assertTrue(NotificationSemantic.QUEST_COMPLETED in manifest.catalogEntries)
    }

    @Test
    fun earthNativeHasNoImplicitSharedSemanticFallbacks() {
        val manifest = NarrativeRegistry.builtIns().manifest(NarrativeSystemId.EARTH_NATIVE)

        assertEquals(
            emptySet<SemanticKey>(),
            manifest.semanticCapabilities.filterValues {
                it == SemanticAvailability.SHARED_SURFACE
            }.keys,
        )
        assertEquals(
            NarrativeSemantics.all - manifest.semanticCapabilities
                .filterValues { it == SemanticAvailability.UNAVAILABLE }.keys,
            manifest.catalogEntries,
        )
    }

    @Test
    fun earthNativeInterpretsEveryCatalogEntryWithRepresentativeParameters() {
        val registry = NarrativeRegistry.builtIns()
        val definition = registry.definition(NarrativeSystemId.EARTH_NATIVE)
        val day = java.time.LocalDate.of(2026, 9, 10).toEpochDay()
        val samples = mapOf<SemanticParameter<*>, Any>(
            SemanticParameters.XP to XpAmount(25),
            SemanticParameters.PLAYER_NAME to OpaqueText("玩家"),
            SemanticParameters.SERVER_NAME to OpaqueText("现实服"),
            SemanticParameters.TITLE to OpaqueText("标题"),
            SemanticParameters.DESCRIPTION to OpaqueText("说明"),
            SemanticParameters.NOTE to OpaqueText("手记"),
            SemanticParameters.LEVEL to LevelNumber(2),
            SemanticParameters.DAY to EpochDay(day),
            SemanticParameters.COUNT to CountValue(2),
            SemanticParameters.MINUTES to CountValue(25),
            SemanticParameters.TEXT_LENGTH to CountValue(12),
            SemanticParameters.PROGRESS to LevelProgress(20, 100, 1),
            SemanticParameters.RECOMMENDATION_REASONS to
                RecommendationReasons(listOf(RecommendationReason.FITS_TIME)),
            SemanticParameters.QUEST_META to QuestMetaPresentation(
                QuestKind.SIDE, 25, Skill.KNOWLEDGE, day, null, day,
            ),
            SemanticParameters.QUEST_PRIORITY_DIFFICULTY to
                QuestPriorityDifficulty(2, Difficulty.NORMAL),
            SemanticParameters.GOAL_PROGRESS to GoalProgress(1, 2, 3),
            SemanticParameters.GOAL_TITLE to GoalTitlePresentation(OpaqueText("主线"), false),
            SemanticParameters.QUEST_DETAIL to QuestDetailPresentation(25, 1),
            SemanticParameters.COMPLETION_MOMENT to CompletionMoment(1_000L, "Asia/Shanghai"),
            SemanticParameters.DIFFICULTY_REWARD to DifficultyReward(Difficulty.NORMAL, 25),
            SemanticParameters.COMPLETE_QUEST_LABEL to CompleteQuestLabel(CompleteQuestLabelStyle.SHORT),
            SemanticParameters.UNDO_COMPLETION_LABEL to UndoCompletionLabel.SHORT,
            SemanticParameters.ARCHIVE_QUEST_LABEL to ArchiveQuestLabel.DETAIL,
            SemanticParameters.SKILL_LEVEL to SkillLevel(Skill.KNOWLEDGE, LevelNumber(2)),
            SemanticParameters.TOTAL_XP to TotalXpAmount(125),
            SemanticParameters.EVENT_HEADER to EventHeader(
                EventKind.COMPLETED,
                CompletionMoment(1_757_325_600_000L, "Asia/Shanghai"),
            ),
            SemanticParameters.SIGNED_XP to SignedXpAmount(-25),
            SemanticParameters.ZONE_ID to ZoneIdValue("Asia/Shanghai"),
            SemanticParameters.VERSION_NAME to VersionName("2.0-test"),
            SemanticParameters.RESTORE_SUMMARY to RestoreSummary(1, 2, 3, 4, 5, 6, 7, 8),
            SemanticParameters.DELETE_DATA_LABEL to DeleteDataLabel.SETTINGS,
        )

        NarrativeScheme.entries.forEach { scheme ->
            definition.manifest.catalogEntries.forEach { key ->
                val values = definition.catalog.parameters(key).associateWith(samples::getValue)
                val presentation = registry.interpret(
                    NarrativeSystemId.EARTH_NATIVE,
                    SemanticRequest(key, SemanticArguments.from(values)),
                    NarrativeLocale.ZH_CN,
                    scheme,
                )
                assertTrue("Blank presentation for ${key.wireId} in ${scheme.wireId}", presentation.text.isNotBlank())
            }
        }
    }

    @Test
    fun everySemanticCategoryUsesUniqueStableWireIds() {
        val expectedCategories = SemanticCategory.entries.toSet()
        val grouped = NarrativeSemantics.all.groupBy { it.category }

        assertEquals(expectedCategories, grouped.keys)
        grouped.forEach { (category, keys) ->
            assertTrue(keys.isNotEmpty())
            assertTrue(keys.all { it.wireId.startsWith("${category.wirePrefix}.") })
        }
        assertEquals(NarrativeSemantics.all.size, NarrativeSemantics.all.map { it.wireId }.toSet().size)
        assertEquals(RuleError.entries.toSet(), RuleError.entries.map { it.semantic().source }.toSet())
        assertEquals(EventKind.entries.toSet(), EventKind.entries.map { it.semantic().source }.toSet())
        assertEquals(
            setOf("first_step", "ten_quests", "hundred_quests", "three_days", "seven_days", "boss_clear", "all_rounder"),
            AchievementSemantic.entries.map { it.sourceId }.toSet(),
        )
    }

    @Test
    fun opaquePlayerContentIsPreservedAndInterpretationCannotChangeDomainFacts() {
        val world = World(
            player = Player(
                name = "玩家“任务 XP”😀",
                server = "服务器 XP任务",
                zoneId = "Asia/Shanghai",
                joinedAt = 1,
                onboarded = true,
            ),
            goals = listOf(Goal("goal-1", "主线任务 XP", "主线说明\n😀", createdAt = 1)),
            quests = listOf(Quest(
                id = "quest-1",
                title = "任务标题 XP 😀",
                description = "说明\n\t任务 XP \"原样\"",
                createdAt = 1,
                updatedAt = 1,
            )),
            events = listOf(JournalEvent(
                id = "event-1",
                kind = EventKind.NOTE,
                text = "手记任务 XP\n😀",
                createdAt = 1,
            )),
        )
        val before = world.copy()
        val registry = NarrativeRegistry.builtIns()
        val cases = listOf(
            Triple(
                ScreenSemantic.PLAYER_WELCOME,
                semanticArguments { put(SemanticParameters.PLAYER_NAME, OpaqueText(world.player.name)) },
                "${world.player.name}，欢迎上线",
            ),
            Triple(
                ScreenSemantic.PROFILE_IDENTITY,
                semanticArguments {
                    put(SemanticParameters.SERVER_NAME, OpaqueText(world.player.server))
                    put(SemanticParameters.LEVEL, LevelNumber(7))
                },
                "${world.player.server} · Lv.7",
            ),
            Triple(
                ScreenSemantic.QUEST_CONTENT,
                semanticArguments {
                    put(SemanticParameters.TITLE, OpaqueText(world.quests.single().title))
                    put(SemanticParameters.DESCRIPTION, OpaqueText(world.quests.single().description))
                },
                "${world.quests.single().title}\n${world.quests.single().description}",
            ),
            Triple(
                ScreenSemantic.GOAL_CONTENT,
                semanticArguments {
                    put(SemanticParameters.TITLE, OpaqueText(world.goals.single().title))
                    put(SemanticParameters.DESCRIPTION, OpaqueText(world.goals.single().description))
                },
                "${world.goals.single().title}\n${world.goals.single().description}",
            ),
            Triple(
                ScreenSemantic.NOTE_CONTENT,
                semanticArguments {
                    put(SemanticParameters.NOTE, OpaqueText(world.events.single().text))
                },
                world.events.single().text,
            ),
        )

        cases.forEach { (key, arguments, expected) ->
            assertEquals(
                expected,
                registry.interpret(
                    NarrativeSystemId.EARTH_NATIVE,
                    SemanticRequest(key, arguments),
                    NarrativeLocale.ZH_CN,
                    NarrativeScheme.DARK,
                ).text,
            )
        }
        assertEquals(before, world)
    }

    @Test
    fun optionalSurfaceProviderReadsProjectionAndDispatchesStableActionsOnly() {
        val state = NarrativeSurfaceState(
            destination = DestinationSemantic.QUESTS,
            busy = false,
            items = listOf(NarrativeSurfaceItem(
                id = "quest-1",
                presentation = NarrativePresentation("任务标题"),
                actions = setOf(ActionSemantic.VIEW_QUEST),
            )),
        )
        val dispatched = mutableListOf<NarrativeActionRequest>()
        val provider = object : NarrativeSurfaceProvider<String> {
            override val surfaces = setOf(DestinationSemantic.QUESTS)
            override fun provide(
                state: NarrativeSurfaceState,
                actions: NarrativeActionSink,
            ): String {
                val item = state.items.single()
                actions.dispatch(NarrativeActionRequest(ActionSemantic.VIEW_QUEST, item.id))
                return item.presentation.text
            }
        }

        val rendered = provider.provide(state, NarrativeActionSink(dispatched::add))

        assertEquals("任务标题", rendered)
        assertEquals(setOf(DestinationSemantic.QUESTS), provider.surfaces)
        assertEquals(
            listOf(NarrativeActionRequest(ActionSemantic.VIEW_QUEST, "quest-1")),
            dispatched,
        )
    }

    @Test
    fun incompleteDefinitionsFailWithDimensionSpecificMessages() {
        val registry = NarrativeRegistry.builtIns()
        val earthNative = registry.definition(NarrativeSystemId.EARTH_NATIVE)
        val manifest = earthNative.manifest
        val cases = mapOf(
            "locale" to earthNative.copy(manifest = manifest.copy(locales = emptySet())),
            "scheme" to earthNative.copy(manifest = manifest.copy(schemes = setOf(NarrativeScheme.DARK))),
            "capability" to earthNative.copy(manifest = manifest.copy(
                capabilities = manifest.capabilities - NarrativeCapability.EFFECT,
            )),
            "semantic" to earthNative.copy(manifest = manifest.copy(
                semanticCapabilities = manifest.semanticCapabilities - ActionSemantic.SAVE,
            )),
            "resource" to earthNative.copy(manifest = manifest.copy(resources = emptySet())),
            "catalog" to earthNative.copy(manifest = manifest.copy(catalogEntries = emptySet())),
            "color resources" to earthNative.copy(manifest = manifest.copy(
                resources = manifest.resources.mapTo(mutableSetOf()) {
                    if (it.kind == ResourceKind.COLOR) {
                        it.copy(schemes = setOf(NarrativeScheme.DARK))
                    } else {
                        it
                    }
                },
            )),
        )

        cases.forEach { (expectedMessage, definition) ->
            val error = expectIllegalArgument {
                NarrativeRegistry.create(NarrativeSystemId.EARTH_NATIVE, listOf(definition))
            }
            assertTrue(error.message.orEmpty().contains(expectedMessage, ignoreCase = true))
        }
    }

    @Test
    fun extensibleRegistryRejectsIncompleteDefinitionsBeforeRegistration() {
        val builtIns = NarrativeRegistry.builtIns()
        val earthNative = builtIns.definition(NarrativeSystemId.EARTH_NATIVE)
        val manifest = earthNative.manifest
        val invalidDefinitions = listOf(
            earthNative.copy(manifest = manifest.copy(structureVersion = 0)),
            earthNative.copy(manifest = manifest.copy(contentVersion = 0)),
            earthNative.copy(manifest = manifest.copy(locales = emptySet())),
            earthNative.copy(manifest = manifest.copy(schemes = setOf(NarrativeScheme.DARK))),
            earthNative.copy(manifest = manifest.copy(
                capabilities = manifest.capabilities - NarrativeCapability.EFFECT,
            )),
            earthNative.copy(manifest = manifest.copy(
                capabilities = manifest.capabilities +
                    (NarrativeCapability.TEXT to CapabilitySupport.UNSUPPORTED),
            )),
            earthNative.copy(manifest = manifest.copy(resources = emptySet())),
            earthNative.copy(manifest = manifest.copy(resources = setOf(
                NarrativeResource("duplicate", ResourceKind.BRAND),
                NarrativeResource("duplicate", ResourceKind.COLOR, NarrativeScheme.entries.toSet()),
                NarrativeResource("effect", ResourceKind.EFFECT),
            ))),
            earthNative.copy(manifest = manifest.copy(resources = manifest.resources.mapTo(mutableSetOf()) {
                if (it.kind == ResourceKind.COLOR) it.copy(schemes = setOf(NarrativeScheme.DARK)) else it
            })),
            earthNative.copy(manifest = manifest.copy(catalogEntries = emptySet())),
            earthNative.copy(manifest = manifest.copy(
                semanticCapabilities = manifest.semanticCapabilities - ActionSemantic.SAVE,
            )),
            earthNative.copy(manifest = manifest.copy(inheritedSurfaces = emptySet())),
        )

        invalidDefinitions.forEach { definition ->
            expectIllegalArgument {
                NarrativeRegistry.create(
                    defaultSystemId = NarrativeSystemId.EARTH_NATIVE,
                    definitions = listOf(definition),
                )
            }
        }
        expectIllegalArgument {
            NarrativeRegistry.create(NarrativeSystemId.of("missing"), listOf(earthNative))
        }
        expectIllegalArgument {
            NarrativeRegistry.create(
                NarrativeSystemId.EARTH_NATIVE,
                listOf(earthNative, earthNative),
            )
        }
    }

    @Test
    fun semanticRequestsRejectMissingOrUnexpectedParameters() {
        val registry = NarrativeRegistry.builtIns()
        expectIllegalArgument {
            registry.interpret(
                NarrativeSystemId.EARTH_NATIVE,
                SemanticRequest(NotificationSemantic.QUEST_COMPLETED),
                NarrativeLocale.ZH_CN,
                NarrativeScheme.LIGHT,
            )
        }
        expectIllegalArgument {
            registry.interpret(
                NarrativeSystemId.EARTH_NATIVE,
                SemanticRequest(
                    NotificationSemantic.QUEST_COMPLETED,
                    semanticArguments {
                        put(SemanticParameters.XP, XpAmount(25))
                        put(SemanticParameters.PLAYER_NAME, OpaqueText("额外参数"))
                    },
                ),
                NarrativeLocale.ZH_CN,
                NarrativeScheme.LIGHT,
            )
        }
    }

    @Test
    fun declaredCatalogInheritanceResolvesThroughTheDefaultSystemOnly() {
        val builtIns = NarrativeRegistry.builtIns()
        val earthNative = builtIns.definition(NarrativeSystemId.EARTH_NATIVE)
        val inheritedId = NarrativeSystemId.of("inherited-test")
        val inherited = NarrativeSystemDefinition(
            manifest = earthNative.manifest.copy(
                id = inheritedId,
                capabilities = NarrativeCapability.entries.associateWith { CapabilitySupport.INHERITED },
                semanticCapabilities = earthNative.manifest.semanticCapabilities.mapValues { (key, value) ->
                    when {
                        key == NotificationSemantic.QUEST_COMPLETED -> SemanticAvailability.INHERITED_CATALOG
                        value == SemanticAvailability.DIRECT_CATALOG -> SemanticAvailability.SHARED_SURFACE
                        else -> value
                    }
                },
                catalogEntries = emptySet(),
                inheritedCatalogEntries = setOf(NotificationSemantic.QUEST_COMPLETED),
            ),
            catalog = NarrativeCatalog(emptyMap()),
        )
        val registry = NarrativeRegistry.create(
            NarrativeSystemId.EARTH_NATIVE,
            listOf(earthNative, inherited),
        )

        val presentation = registry.interpret(
            inheritedId,
            SemanticRequest(
                NotificationSemantic.QUEST_COMPLETED,
                semanticArguments { put(SemanticParameters.XP, XpAmount(25)) },
            ),
            NarrativeLocale.ZH_CN,
            NarrativeScheme.LIGHT,
        )

        assertEquals("任务完成 · +25 XP", presentation.text)
    }

    @Test
    fun interpretationLeavesCompletionProgressAndV2FactsExactlyEqual() {
        val quest = Quest(id = "quest-1", title = "任务 XP", createdAt = 1, updatedAt = 1)
        val completion = Completion(
            id = "quest-1:once",
            questId = quest.id,
            occurrence = "once",
            title = quest.title,
            kind = quest.kind,
            skill = quest.skill,
            xp = 25,
            completedAt = 2,
            completedDay = 20_000,
        )
        val world = World(
            player = Player(
                name = "玩家",
                zoneId = "Asia/Shanghai",
                joinedAt = 1,
                onboarded = true,
            ),
            quests = listOf(quest),
            completions = listOf(completion),
        )
        val graph = BackupV2Graph(
            narrativePreference = TestNarrativePreference("earth-native"),
            contracts = listOf(TestContract(questId = quest.id)),
            contractRevisions = emptyList(),
            consequences = listOf(TestConsequence()),
            consequenceAdjustments = emptyList(),
            repaymentAllocations = listOf(TestAllocation(completionId = completion.id)),
            clockBoundaries = emptyList(),
            recoveryRoutes = listOf(TestRecoveryRoute()),
            recoveryNodes = listOf(TestRecoveryNode(questId = quest.id)),
        )
        val worldBefore = world.copy()
        val graphBefore = graph.copy()
        val progressBefore = ProgressRules.total(world.completions)
        val achievementsBefore = ProgressRules.achievements(world)

        NarrativeRegistry.builtIns().interpret(
            NarrativeSystemId.EARTH_NATIVE,
            SemanticRequest(
                NotificationSemantic.QUEST_COMPLETED,
                semanticArguments { put(SemanticParameters.XP, XpAmount(completion.xp)) },
            ),
            NarrativeLocale.ZH_CN,
            NarrativeScheme.DARK,
        )

        assertEquals(worldBefore, world)
        assertEquals(graphBefore, graph)
        assertEquals(progressBefore, ProgressRules.total(world.completions))
        assertEquals(achievementsBefore, ProgressRules.achievements(world))
    }

    private data class TestNarrativePreference(
        override val narrativeId: String,
        override val playerId: Int = 1,
    ) : NarrativePreferenceRecord

    private data class TestContract(
        override val questId: String,
        override val id: String = "contract-1",
        override val occurrence: String = "once",
        override val zoneId: String = "Asia/Shanghai",
        override val dueDay: Long = 20_001,
        override val originalRewardXp: Long = 25,
        override val currentRewardXp: Long = 25,
        override val status: String = "OVERDUE",
        override val signedAt: Long = 1,
        override val closedAt: Long? = 2,
    ) : ContractRecord

    private data class TestConsequence(
        override val id: String = "consequence-1",
        override val contractId: String = "contract-1",
        override val kind: String = "OVERDUE",
        override val xp: Long = 25,
        override val effectiveDay: Long = 20_002,
        override val createdAt: Long = 2,
        override val idempotencyKey: String = "consequence:1",
    ) : ConsequenceRecord

    private data class TestAllocation(
        override val completionId: String,
        override val id: String = "allocation-1",
        override val consequenceId: String = "consequence-1",
        override val xp: Long = 20,
        override val createdAt: Long = 3,
        override val idempotencyKey: String = "allocation:1",
    ) : RepaymentAllocationRecord

    private data class TestRecoveryRoute(
        override val id: String = "route-1",
        override val status: String = "CLOSED",
        override val triggerKind: String = "DEBT_LEVEL",
        override val targetDebtXp: Long = 20,
        override val openedAt: Long = 4,
        override val closedAt: Long? = 5,
        override val idempotencyKey: String = "route:1",
    ) : RecoveryRouteRecord

    private data class TestRecoveryNode(
        override val questId: String,
        override val routeId: String = "route-1",
        override val position: Int = 0,
        override val addedAt: Long = 4,
        override val completedAt: Long? = 5,
    ) : RecoveryNodeRecord

    private fun expectIllegalArgument(block: () -> Unit): IllegalArgumentException {
        try {
            block()
        } catch (error: IllegalArgumentException) {
            return error
        }
        throw AssertionError("Expected IllegalArgumentException")
    }
}
