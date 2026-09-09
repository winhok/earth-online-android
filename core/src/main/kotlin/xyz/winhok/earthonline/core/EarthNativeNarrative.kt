package xyz.winhok.earthonline.core

import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object EarthNativeNarrative {
    val catalogEntries: Set<SemanticKey> get() = catalog().keys

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
            DestinationSemantic.DASHBOARD to textEntry("指挥台", IconRole.COMMAND),
            DestinationSemantic.QUESTS to textEntry("任务", IconRole.QUEST),
            DestinationSemantic.PROFILE to textEntry("角色", IconRole.CHARACTER),
            DestinationSemantic.JOURNAL to textEntry("日志", IconRole.JOURNAL),
            ScreenSemantic.APP_NAME to textEntry("地球 Online"),
            ScreenSemantic.JOIN_TAGLINE to textEntry("现实没有重开键，\n但随时可以接取下一项任务。"),
            ScreenSemantic.JOIN_PRIVACY_NOTICE to textEntry(
                "无需账号 · 无广告 · 无云端上传\n任务保存在本机。卸载前请导出存档；备份文件为明文。\n" +
                    "这里不因拖延扣血，也不评价你的现实价值。",
            ),
            ScreenSemantic.DEFAULT_SERVER_NAME to textEntry("现实服"),
            ScreenSemantic.LOAD_ERROR_TITLE to textEntry("无法读取存档"),
            ScreenSemantic.LOAD_ERROR_BODY to textEntry("请重试。应用不会为了启动而清空数据库。"),
            FieldSemantic.PLAYER_NAME to textEntry("玩家名"),
            FieldSemantic.SERVER_NAME to textEntry("服务器名（自己取一个）"),
            ActionSemantic.JOIN to textEntry("创建本地角色"),
            ActionSemantic.OPEN_SETTINGS to textEntry("设置与存档", IconRole.SETTINGS),
            ActionSemantic.CREATE_QUEST to textEntry("接取任务", IconRole.QUEST),
            ActionSemantic.WRITE_NOTE to textEntry("写冒险手记", IconRole.JOURNAL),
            ActionSemantic.RETRY_LOAD to textEntry("重新读取"),
            ActionSemantic.VIEW_QUEST to textEntry("查看任务"),
            ActionSemantic.CREATE_GOAL to textEntry("开启主线", IconRole.GOAL),
            ActionSemantic.EDIT_GOAL to textEntry("编辑主线"),
            ActionSemantic.ARCHIVE_GOAL to textEntry("归档"),
            ActionSemantic.VIEW_ALL to textEntry("全部"),
            ActionSemantic.VIEW_GOAL_QUESTS to textEntry("所属任务"),
            ActionSemantic.EDIT_QUEST to textEntry("编辑任务"),
            ActionSemantic.POSTPONE_QUEST to textEntry("暂缓一天（不修改截止日）"),
            ActionSemantic.PAUSE_QUEST to textEntry("暂停这项任务"),
            ActionSemantic.RESUME_QUEST to textEntry("重新接取 / 清除暂缓"),
            ActionSemantic.ARCHIVE_QUEST to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.ARCHIVE_QUEST_LABEL),
                render = { arguments -> NarrativePresentation(
                    when (arguments.require(SemanticParameters.ARCHIVE_QUEST_LABEL)) {
                        ArchiveQuestLabel.DETAIL -> "归档任务"
                        ArchiveQuestLabel.CONFIRM -> "归档"
                    },
                ) },
            ),
            ActionSemantic.SAVE to textEntry("保存"),
            ActionSemantic.CANCEL to textEntry("取消"),
            ActionSemantic.DISCARD to textEntry("放弃修改"),
            ActionSemantic.CLEAR_DATE to textEntry("清除日期"),
            ActionSemantic.SELECT_DATE to textEntry("选择日期"),
            ActionSemantic.CONFIRM to textEntry("确定"),
            ActionSemantic.CONTINUE_EDITING to textEntry("继续编辑"),
            ActionSemantic.CLOSE to textEntry("关闭"),
            ActionSemantic.COMPLETE_QUEST to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.COMPLETE_QUEST_LABEL),
                render = { arguments -> arguments.require(SemanticParameters.COMPLETE_QUEST_LABEL).let { label ->
                    NarrativePresentation(
                        text = when (label.style) {
                            CompleteQuestLabelStyle.SHORT -> "已完成"
                            CompleteQuestLabelStyle.CONFIRM -> "确认完成"
                            CompleteQuestLabelStyle.ACCESSIBILITY -> "完成任务：${label.title!!.value}"
                        },
                        iconRole = IconRole.COMPLETION,
                    )
                } },
            ),
            ActionSemantic.UNDO_COMPLETION to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.UNDO_COMPLETION_LABEL),
                render = { arguments -> NarrativePresentation(
                    when (arguments.require(SemanticParameters.UNDO_COMPLETION_LABEL)) {
                        UndoCompletionLabel.SHORT -> "撤销"
                        UndoCompletionLabel.DETAIL -> "撤销本次完成"
                    },
                ) },
            ),
            ScreenSemantic.DASHBOARD_DAY_MODE to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.DAY),
                render = { arguments -> NarrativePresentation(
                    "${date(arguments.require(SemanticParameters.DAY).value)} · 单人冒险模式",
                ) },
            ),
            ScreenSemantic.DASHBOARD_LEVEL to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.LEVEL),
                render = { arguments -> NarrativePresentation(
                    "Lv.${arguments.require(SemanticParameters.LEVEL).value}  现实探索者",
                ) },
            ),
            ScreenSemantic.DASHBOARD_PROGRESS to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.PROGRESS),
                render = { arguments -> arguments.require(SemanticParameters.PROGRESS).let { progress ->
                    NarrativePresentation(
                        "${progress.intoLevel} / ${progress.needed} XP · 今日完成 ${progress.completedToday} 项",
                    )
                } },
            ),
            ScreenSemantic.DASHBOARD_PROMPT_TITLE to textEntry("现在做什么"),
            ScreenSemantic.DASHBOARD_PROMPT_BODY to textEntry("由你选择时间与精力，系统给出可解释的推荐。"),
            ScreenSemantic.MINUTE_OPTION to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.MINUTES),
                render = { arguments -> NarrativePresentation(
                    "${arguments.require(SemanticParameters.MINUTES).value} 分钟",
                ) },
            ),
            ScreenSemantic.ENERGY_LOW to textEntry("低精力"),
            ScreenSemantic.ENERGY_MEDIUM to textEntry("中精力"),
            ScreenSemantic.ENERGY_HIGH to textEntry("高精力"),
            ScreenSemantic.RECOMMENDATION_EMPTY_TITLE to textEntry("这段时间不必硬塞任务"),
            ScreenSemantic.RECOMMENDATION_EMPTY_BODY to textEntry("可以调整时间与精力，或接取一项更小的任务。"),
            ScreenSemantic.NEXT_QUEST to textEntry("NEXT QUEST"),
            ScreenSemantic.RECOMMENDATION_REASONS to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.RECOMMENDATION_REASONS),
                render = { arguments -> NarrativePresentation(
                    arguments.require(SemanticParameters.RECOMMENDATION_REASONS).values
                        .joinToString(" · ", transform = ::recommendationLabel),
                ) },
            ),
            ScreenSemantic.RECOMMENDATION_NOTICE to textEntry("本地规则推荐，不是 AI 判断。"),
            ScreenSemantic.GOAL_EMPTY_TITLE to textEntry("给冒险一条主线"),
            ScreenSemantic.GOAL_EMPTY_BODY to textEntry("把重要目标设为主线，再用小任务一步步推进。"),
            ScreenSemantic.REVISIT_TITLE to textEntry("重新决策"),
            ScreenSemantic.REVISIT_BODY to textEntry("多次暂缓不等于失败。拆小、暂停，或者放下它。"),
            ScreenSemantic.EXECUTABLE_QUESTS to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.COUNT),
                render = { arguments -> NarrativePresentation(
                    "可执行任务 · ${arguments.require(SemanticParameters.COUNT).value}",
                ) },
            ),
            ScreenSemantic.WORK_DONE_TITLE to textEntry("今日可以收工了"),
            ScreenSemantic.WORK_DONE_BODY to textEntry("没有需要现在执行的任务。休息也是正常的冒险节奏。"),
            ScreenSemantic.QUESTS_TITLE to textEntry("任务日志"),
            ScreenSemantic.QUESTS_BODY to textEntry("主线指引方向，支线负责行动。"),
            FieldSemantic.QUEST_SEARCH to textEntry("搜索任务或主线"),
            StateSemantic.LOADING to textEntry("正在读取存档"),
            StateSemantic.ACTIVE to textEntry("进行中"),
            StateSemantic.COMPLETED to textEntry("已完成", IconRole.COMPLETION),
            StateSemantic.PAUSED to textEntry("暂停"),
            StateSemantic.ARCHIVED to textEntry("归档"),
            StateSemantic.REVIEW_REQUIRED to textEntry("待重审", IconRole.WARNING),
            ScreenSemantic.FILTER_GOALS to textEntry("主线"),
            ScreenSemantic.ALL_GOALS to textEntry("所有主线"),
            ScreenSemantic.NO_GOALS_TITLE to textEntry("还没有主线"),
            ScreenSemantic.NO_GOALS_BODY to textEntry("例如：完成一个独立产品、准备一次旅行。不要把它写成今天必须完成的大任务。"),
            ScreenSemantic.QUEST_EMPTY_TITLE to textEntry("这里暂时没有任务"),
            ScreenSemantic.QUEST_EMPTY_BODY to textEntry("可以切换筛选、清空搜索，或接取新的任务。"),
            ScreenSemantic.GOAL_TITLE to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.GOAL_TITLE),
                render = { arguments -> arguments.require(SemanticParameters.GOAL_TITLE).let { goal ->
                    NarrativePresentation(
                        goal.title.value + if (!goal.archived) "" else if (goal.compact) "（归档）" else " · 已归档",
                    )
                } },
            ),
            ScreenSemantic.GOAL_PROGRESS to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.GOAL_PROGRESS),
                render = { arguments -> arguments.require(SemanticParameters.GOAL_PROGRESS).let { progress ->
                    NarrativePresentation(
                        "一次性任务 ${progress.completed} / ${progress.total} · " +
                            "日常贡献 ${progress.dailyContributions} 次",
                    )
                } },
            ),
            ScreenSemantic.ARCHIVE_GOAL_TITLE to textEntry("归档这条主线？"),
            ScreenSemantic.ARCHIVE_GOAL_BODY to textEntry("历史记录和所属任务都会保留。已归档主线不再增加推荐权重。"),
            ScreenSemantic.QUEST_META to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.QUEST_META),
                render = { arguments -> NarrativePresentation(questMeta(arguments.require(SemanticParameters.QUEST_META))) },
            ),
            ScreenSemantic.QUEST_REWARD to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.XP),
                render = { arguments -> NarrativePresentation(
                    "+${arguments.require(SemanticParameters.XP).value} XP",
                    iconRole = IconRole.REWARD,
                    colorRole = ColorRole.REWARD,
                ) },
            ),
            ScreenSemantic.QUEST_PRIORITY_DIFFICULTY to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.QUEST_PRIORITY_DIFFICULTY),
                render = { arguments -> arguments.require(SemanticParameters.QUEST_PRIORITY_DIFFICULTY).let { value ->
                    NarrativePresentation("P${value.displayPriority} · ${difficultyLabel(value.difficulty)}")
                } },
            ),
            ScreenSemantic.QUEST_CREATE_TITLE to textEntry("接取新任务"),
            ScreenSemantic.QUEST_EDIT_TITLE to textEntry("编辑任务"),
            ScreenSemantic.QUEST_TITLE_HELPER to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.TEXT_LENGTH),
                render = { arguments -> NarrativePresentation(
                    "${arguments.require(SemanticParameters.TEXT_LENGTH).value}/120 · 用一个可执行的动词开头",
                ) },
            ),
            ScreenSemantic.QUEST_KIND_LOCKED_HELPER to textEntry("已有完成记录，类型已锁定。难度调整只影响新的结算轮次。"),
            ScreenSemantic.QUEST_KIND_HELPER to textEntry("日常按存档时区每天结算一次；支线和 Boss 是一次性任务。"),
            ScreenSemantic.QUEST_KIND_SIDE to textEntry("支线"),
            ScreenSemantic.QUEST_KIND_DAILY to textEntry("日常"),
            ScreenSemantic.QUEST_KIND_BOSS to textEntry("Boss"),
            ScreenSemantic.DIFFICULTY_EASY to textEntry("轻量"),
            ScreenSemantic.DIFFICULTY_NORMAL to textEntry("标准"),
            ScreenSemantic.DIFFICULTY_HARD to textEntry("挑战"),
            ScreenSemantic.DIFFICULTY_REWARD to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.DIFFICULTY_REWARD),
                render = { arguments -> arguments.require(SemanticParameters.DIFFICULTY_REWARD).let { value ->
                    NarrativePresentation("${difficultyLabel(value.difficulty)} · ${value.xp} XP")
                } },
            ),
            ScreenSemantic.SKILL_KNOWLEDGE to textEntry("学识"),
            ScreenSemantic.SKILL_VITALITY to textEntry("活力"),
            ScreenSemantic.SKILL_CREATION to textEntry("创造"),
            ScreenSemantic.SKILL_CONNECTION to textEntry("连接"),
            ScreenSemantic.SKILL_DISCIPLINE to textEntry("自律"),
            ScreenSemantic.PRIORITY_NORMAL to textEntry("普通"),
            ScreenSemantic.PRIORITY_IMPORTANT to textEntry("重要"),
            ScreenSemantic.PRIORITY_URGENT to textEntry("紧急"),
            ScreenSemantic.DUE_START to textEntry("起始日期（可选）"),
            ScreenSemantic.DUE_END to textEntry("截止日期（可选）"),
            ScreenSemantic.INDEPENDENT_QUEST to textEntry("独立任务"),
            ScreenSemantic.SELECTED_DATE to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.DAY),
                render = { arguments -> NarrativePresentation(date(arguments.require(SemanticParameters.DAY).value)) },
            ),
            ScreenSemantic.DESCRIPTION_COUNT to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.TEXT_LENGTH),
                render = { arguments -> NarrativePresentation(
                    "${arguments.require(SemanticParameters.TEXT_LENGTH).value}/4000",
                ) },
            ),
            ScreenSemantic.GOAL_CREATE_TITLE to textEntry("开启主线"),
            ScreenSemantic.GOAL_EDIT_TITLE to textEntry("编辑主线"),
            ScreenSemantic.GOAL_EDITOR_HELPER to textEntry("主线是方向，不是一天必须完成的巨型任务。保存后，在任务编辑页关联到这条主线。"),
            ScreenSemantic.NOTE_EDITOR_TITLE to textEntry("冒险手记"),
            ScreenSemantic.NOTE_HELPER to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.TEXT_LENGTH),
                render = { arguments -> NarrativePresentation(
                    "${arguments.require(SemanticParameters.TEXT_LENGTH).value}/4000 · 手记保存在本机",
                ) },
            ),
            ScreenSemantic.DISCARD_TITLE to textEntry("放弃未保存的修改？"),
            ScreenSemantic.DISCARD_BODY to textEntry("已经保存的数据不会改变。"),
            ScreenSemantic.QUEST_DETAIL_STATS to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.QUEST_DETAIL),
                render = { arguments -> arguments.require(SemanticParameters.QUEST_DETAIL).let { detail ->
                    NarrativePresentation("本轮奖励：${detail.xp} XP · 暂缓 ${detail.postponeCount} 次")
                } },
            ),
            ScreenSemantic.QUEST_GOAL to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.TITLE),
                render = { arguments -> NarrativePresentation("主线：${arguments.require(SemanticParameters.TITLE).value}") },
            ),
            ScreenSemantic.QUEST_REVIEW_GUIDANCE to textEntry("已经多次暂缓。可以编辑成一个更小的动作，或暂时放下。"),
            ScreenSemantic.QUEST_COMPLETED_AT to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.COMPLETION_MOMENT),
                render = { arguments -> arguments.require(SemanticParameters.COMPLETION_MOMENT).let { moment ->
                    NarrativePresentation(
                        "已完成：" + Instant.ofEpochMilli(moment.epochMillis).atZone(java.time.ZoneId.of(moment.zoneId))
                            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                    )
                } },
            ),
            ScreenSemantic.ARCHIVE_QUEST_TITLE to textEntry("归档任务？"),
            ScreenSemantic.ARCHIVE_QUEST_BODY to textEntry("任务将移至归档列表。已有经验和日志保留，之后可以重新接取。"),
            ScreenSemantic.CLOSE_EDITOR_ACCESSIBILITY to textEntry("关闭编辑"),
            FieldSemantic.QUEST_TITLE to textEntry("任务标题"),
            FieldSemantic.QUEST_KIND to textEntry("任务类型"),
            FieldSemantic.DIFFICULTY to textEntry("挑战强度"),
            FieldSemantic.SKILL to textEntry("成长技能"),
            FieldSemantic.ESTIMATED_MINUTES to textEntry("预计分钟数（1–480）"),
            FieldSemantic.PRIORITY to textEntry("优先级"),
            FieldSemantic.GOAL to textEntry("所属主线"),
            FieldSemantic.DESCRIPTION to textEntry("行动说明（可选）"),
            FieldSemantic.GOAL_TITLE to textEntry("主线名称"),
            FieldSemantic.GOAL_DESCRIPTION to textEntry("完成标准 / 为什么重要"),
            FieldSemantic.NOTE to textEntry("今天发生了什么？"),
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

    private fun textEntry(text: String, iconRole: IconRole = IconRole.NEUTRAL) =
        NarrativeSemanticEntry(
            parameters = emptySet(),
            render = { NarrativePresentation(text = text, iconRole = iconRole) },
        )

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun date(day: Long): String = LocalDate.ofEpochDay(day).format(dateFormatter)

    private fun questMeta(value: QuestMetaPresentation): String = buildList {
        add(questKindLabel(value.kind))
        add("${value.estimatedMinutes} 分钟")
        add(skillLabel(value.skill))
        value.dueDay?.let { due ->
            add(if (value.kind == QuestKind.DAILY) "${date(due)} 起" else "${date(due)} 截止")
        }
        value.snoozedUntilDay?.let { until -> if (until > value.today) add("暂缓至 ${date(until)}") }
    }.joinToString(" · ")

    private fun questKindLabel(kind: QuestKind) = when (kind) {
        QuestKind.SIDE -> "支线"
        QuestKind.DAILY -> "日常"
        QuestKind.BOSS -> "Boss"
    }

    private fun difficultyLabel(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> "轻量"
        Difficulty.NORMAL -> "标准"
        Difficulty.HARD -> "挑战"
    }

    private fun skillLabel(skill: Skill) = when (skill) {
        Skill.KNOWLEDGE -> "学识"
        Skill.VITALITY -> "活力"
        Skill.CREATION -> "创造"
        Skill.CONNECTION -> "连接"
        Skill.DISCIPLINE -> "自律"
    }

    private fun recommendationLabel(reason: RecommendationReason) = when (reason) {
        RecommendationReason.OVERDUE -> "已过截止日"
        RecommendationReason.DUE_TODAY -> "今天截止"
        RecommendationReason.HIGH_PRIORITY -> "高优先级"
        RecommendationReason.GOAL -> "推进主线"
        RecommendationReason.FITS_TIME -> "时间足够"
        RecommendationReason.FITS_ENERGY -> "精力匹配"
    }
}
