package xyz.winhok.earthonline.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object CultivationNarrative {
    private val directEntries = mapOf<SemanticKey, NarrativeSemanticEntry>(
        DestinationSemantic.DASHBOARD to textEntry("洞天", IconRole.COMMAND),
        DestinationSemantic.QUESTS to textEntry("历练", IconRole.QUEST),
        DestinationSemantic.PROFILE to textEntry("境界", IconRole.CHARACTER),
        DestinationSemantic.JOURNAL to textEntry("修行志", IconRole.JOURNAL),
        ScreenSemantic.APP_NAME to textEntry("地球 Online"),
        ActionSemantic.OPEN_SETTINGS to textEntry("设置与存档", IconRole.SETTINGS),
        ActionSemantic.CREATE_QUEST to textEntry("接取历练", IconRole.QUEST),
        ActionSemantic.CREATE_GOAL to textEntry("开辟仙途", IconRole.GOAL),
        ActionSemantic.WRITE_NOTE to textEntry("写修行手记", IconRole.JOURNAL),
        ActionSemantic.COMPLETE_QUEST to NarrativeSemanticEntry(
            parameters = setOf(SemanticParameters.COMPLETE_QUEST_LABEL),
            render = { arguments ->
                val label = arguments.require(SemanticParameters.COMPLETE_QUEST_LABEL)
                NarrativePresentation(
                    text = when (label.style) {
                        CompleteQuestLabelStyle.SHORT -> "已达成"
                        CompleteQuestLabelStyle.CONFIRM -> "确认达成"
                        CompleteQuestLabelStyle.ACCESSIBILITY -> "完成历练：${label.title!!.value}"
                    },
                    iconRole = IconRole.COMPLETION,
                )
            },
        ),
        ActionSemantic.POSTPONE_QUEST to textEntry("暂缓一日（不改天命）"),
        ActionSemantic.PAUSE_QUEST to textEntry("暂歇此项历练"),
        ActionSemantic.RESUME_QUEST to textEntry("重新接取 / 清除暂缓"),
        ScreenSemantic.DASHBOARD_DAY_MODE to NarrativeSemanticEntry(
            parameters = setOf(SemanticParameters.DAY),
            render = { arguments -> NarrativePresentation(
                "${date(arguments.require(SemanticParameters.DAY).value)} · 宗门战令",
            ) },
        ),
        ScreenSemantic.DASHBOARD_LEVEL to NarrativeSemanticEntry(
            parameters = setOf(SemanticParameters.LEVEL),
            render = { arguments -> NarrativePresentation(
                "Lv.${arguments.require(SemanticParameters.LEVEL).value} · 修行者",
            ) },
        ),
        ScreenSemantic.DASHBOARD_PROGRESS to NarrativeSemanticEntry(
            parameters = setOf(SemanticParameters.PROGRESS),
            render = { arguments ->
                val progress = arguments.require(SemanticParameters.PROGRESS)
                NarrativePresentation(
                    "${progress.intoLevel} / ${progress.needed} 修为 · 今日达成 ${progress.completedToday} 项",
                )
            },
        ),
        ScreenSemantic.DASHBOARD_PROMPT_TITLE to textEntry("下一项宗门任务"),
        ScreenSemantic.DASHBOARD_PROMPT_BODY to textEntry("量力而行，择一项当下可完成的历练。"),
        ScreenSemantic.NEXT_QUEST to textEntry("NEXT CULTIVATION"),
        ScreenSemantic.RECOMMENDATION_NOTICE to textEntry("本地规则推演，不是 AI 占卜。"),
        ScreenSemantic.QUESTS_TITLE to textEntry("历练玉简"),
        ScreenSemantic.QUESTS_BODY to textEntry("仙途指引方向，历练落于现实行动。"),
        ScreenSemantic.QUEST_KIND_SIDE to textEntry("支线历练"),
        ScreenSemantic.QUEST_KIND_DAILY to textEntry("日课"),
        ScreenSemantic.QUEST_KIND_BOSS to textEntry("渡劫"),
        ScreenSemantic.SKILL_KNOWLEDGE to textEntry("悟性"),
        ScreenSemantic.SKILL_VITALITY to textEntry("根骨"),
        ScreenSemantic.SKILL_CREATION to textEntry("造化"),
        ScreenSemantic.SKILL_CONNECTION to textEntry("因缘"),
        ScreenSemantic.SKILL_DISCIPLINE to textEntry("道心"),
        ScreenSemantic.QUEST_META to NarrativeSemanticEntry(
            parameters = setOf(SemanticParameters.QUEST_META),
            render = { arguments -> NarrativePresentation(
                questMeta(arguments.require(SemanticParameters.QUEST_META)),
            ) },
        ),
        ScreenSemantic.QUEST_REWARD to NarrativeSemanticEntry(
            parameters = setOf(SemanticParameters.XP),
            render = { arguments -> NarrativePresentation(
                "+${arguments.require(SemanticParameters.XP).value} 修为",
                iconRole = IconRole.REWARD,
                colorRole = ColorRole.REWARD,
            ) },
        ),
        ScreenSemantic.GOAL_EMPTY_TITLE to textEntry("开辟第一条仙途"),
        ScreenSemantic.GOAL_EMPTY_BODY to textEntry("以真实方向为仙途，再拆成今日能做的历练。"),
        ScreenSemantic.CHARACTER_TITLE to textEntry("修行境界"),
        ScreenSemantic.CHARACTER_BODY to textEntry("境界来自真实行动，不来自清单长度。"),
        ScreenSemantic.TOTAL_XP to textEntry("累计修为"),
        ScreenSemantic.COMPLETION_COUNT to textEntry("历练达成"),
        ScreenSemantic.STREAK_BODY to textEntry("连续行功 · 休息不会折损修为"),
        ScreenSemantic.SKILLS_TITLE to textEntry("五脉根基"),
        ScreenSemantic.SKILLS_BODY to textEntry("只记录应用内行动，不评价现实中的你。"),
        ScreenSemantic.SKILL_LEVEL to NarrativeSemanticEntry(
            parameters = setOf(SemanticParameters.SKILL_LEVEL),
            render = { arguments ->
                val value = arguments.require(SemanticParameters.SKILL_LEVEL)
                NarrativePresentation("${skillLabel(value.skill)} · Lv.${value.level.value}")
            },
        ),
        ScreenSemantic.SKILL_XP to NarrativeSemanticEntry(
            parameters = setOf(SemanticParameters.TOTAL_XP),
            render = { arguments -> NarrativePresentation(
                "${arguments.require(SemanticParameters.TOTAL_XP).value} 修为",
            ) },
        ),
        ScreenSemantic.ACHIEVEMENTS_TITLE to textEntry("道果档案"),
        ScreenSemantic.JOURNAL_TITLE to textEntry("修行志"),
        ScreenSemantic.JOURNAL_BODY to textEntry("每次行动与选择都留下可追溯的道痕。"),
        ScreenSemantic.JOURNAL_NOTES to textEntry("修行手记"),
        ScreenSemantic.JOURNAL_EMPTY_TITLE to textEntry("写下第一篇修行手记"),
        FieldSemantic.NARRATIVE_SYSTEM to textEntry("叙事体系"),
        ScreenSemantic.NARRATIVE_SYSTEM_BODY to textEntry("同一份真实任务、进度与存档，只改变应用内的表达和材质。"),
        ScreenSemantic.EARTH_NATIVE_SYSTEM_NAME to textEntry("地球原生"),
        ScreenSemantic.EARTH_NATIVE_SYSTEM_BODY to textEntry("清晰、克制的现实探索终端"),
        ScreenSemantic.CULTIVATION_SYSTEM_NAME to textEntry("修仙 · 宗门战令"),
        ScreenSemantic.CULTIVATION_SYSTEM_BODY to textEntry("黑漆玉简、稀缺金箔与朱砂劫痕"),
        ScreenSemantic.NARRATIVE_PREVIEW_EMPTY to textEntry("接取第一项真实历练后，可在这里对照两套体系。"),
        StateSemantic.ACTIVE to textEntry("行功中"),
        StateSemantic.COMPLETED to textEntry("已圆满", IconRole.COMPLETION),
        StateSemantic.REVIEW_REQUIRED to textEntry("待静思", IconRole.WARNING),
        NotificationSemantic.QUEST_COMPLETED to NarrativeSemanticEntry(
            parameters = setOf(SemanticParameters.XP),
            render = { arguments -> NarrativePresentation(
                text = "历练达成 · +${arguments.require(SemanticParameters.XP).value} 修为",
                iconRole = IconRole.COMPLETION,
                colorRole = ColorRole.REWARD,
                effectRole = EffectRole.COMPLETION,
            ) },
        ),
        NotificationSemantic.REMINDER_CHANNEL to textEntry("每日修行提醒"),
        NotificationSemantic.REMINDER_TITLE to textEntry("修行仍在继续"),
        NotificationSemantic.REMINDER_AVAILABLE to NarrativeSemanticEntry(
            parameters = setOf(SemanticParameters.COUNT),
            render = { arguments -> NarrativePresentation(
                "还有 ${arguments.require(SemanticParameters.COUNT).value} 项可行历练。择一件小事，继续前进。",
            ) },
        ),
    )

    private val inheritedEntries = EarthNativeNarrative.catalogEntries - directEntries.keys

    fun definition() = NarrativeSystemDefinition(
        manifest = NarrativeManifest(
            id = NarrativeSystemId.CULTIVATION,
            structureVersion = 1,
            contentVersion = 1,
            locales = setOf(NarrativeLocale.ZH_CN),
            schemes = NarrativeScheme.entries.toSet(),
            capabilities = NarrativeCapability.entries.associateWith { capability ->
                if (capability == NarrativeCapability.SURFACE) {
                    CapabilitySupport.INHERITED
                } else {
                    CapabilitySupport.MIXED
                }
            },
            semanticCapabilities = NarrativeSemantics.all.associateWith { key ->
                when {
                    key in directEntries -> SemanticAvailability.DIRECT_CATALOG
                    key in inheritedEntries -> SemanticAvailability.INHERITED_CATALOG
                    key in EarthNativeNarrative.unavailable -> SemanticAvailability.UNAVAILABLE
                    else -> error("Cultivation semantic is not classified: ${key.wireId}")
                }
            },
            inheritedSurfaces = DestinationSemantic.entries.toSet(),
            inheritedCatalogEntries = inheritedEntries,
            resources = setOf(
                NarrativeResource("brand.earth", ResourceKind.BRAND),
                NarrativeResource("color.cultivation", ResourceKind.COLOR, NarrativeScheme.entries.toSet()),
                NarrativeResource("effect.cultivation", ResourceKind.EFFECT),
            ),
            catalogEntries = directEntries.keys,
        ),
        catalog = NarrativeCatalog(directEntries),
    )

    private fun questMeta(meta: QuestMetaPresentation): String = buildList {
        add(questKindLabel(meta.kind))
        add("${meta.estimatedMinutes} 分钟")
        add(skillLabel(meta.skill))
        meta.dueDay?.let { due ->
            add(if (meta.kind == QuestKind.DAILY) "${date(due)} 起" else "${date(due)} 天命")
        }
        meta.snoozedUntilDay?.let { until ->
            if (until > meta.today) add("暂缓至 ${date(until)}")
        }
    }.joinToString(" · ")

    private fun questKindLabel(kind: QuestKind) = when (kind) {
        QuestKind.SIDE -> "支线历练"
        QuestKind.DAILY -> "日课"
        QuestKind.BOSS -> "渡劫"
    }

    private fun skillLabel(skill: Skill) = when (skill) {
        Skill.KNOWLEDGE -> "悟性"
        Skill.VITALITY -> "根骨"
        Skill.CREATION -> "造化"
        Skill.CONNECTION -> "因缘"
        Skill.DISCIPLINE -> "道心"
    }

    private fun date(day: Long): String =
        LocalDate.ofEpochDay(day).format(DateTimeFormatter.ISO_LOCAL_DATE)

    private fun textEntry(text: String, iconRole: IconRole = IconRole.NEUTRAL) =
        NarrativeSemanticEntry(
            parameters = emptySet(),
            render = { NarrativePresentation(text = text, iconRole = iconRole) },
        )
}
