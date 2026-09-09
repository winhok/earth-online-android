package xyz.winhok.earthonline.core

import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object EarthNativeNarrative {
    val catalogEntries: Set<SemanticKey> get() = catalog().keys

    val unavailable = setOf<SemanticKey>(
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
                    else -> error("Earth Native semantic is not classified: ${key.wireId}")
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
            FieldSemantic.THEME to textEntry("界面风格"),
            FieldSemantic.NARRATIVE_SYSTEM to textEntry("叙事体系"),
            FieldSemantic.REMINDERS to textEntry("开启每日提醒"),
            FieldSemantic.REMINDER_HOUR to textEntry("提醒小时（0–23）"),
            FieldSemantic.DUE_DAY to textEntry("日期"),
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
            ActionSemantic.OPEN_PRIVACY to textEntry("隐私说明"),
            ActionSemantic.EXPORT_BACKUP to textEntry("导出存档"),
            ActionSemantic.IMPORT_BACKUP to textEntry("导入存档"),
            ActionSemantic.CONFIRM_RESTORE to textEntry("确认覆盖"),
            ActionSemantic.DELETE_ALL_DATA to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.DELETE_DATA_LABEL),
                render = { arguments -> NarrativePresentation(
                    when (arguments.require(SemanticParameters.DELETE_DATA_LABEL)) {
                        DeleteDataLabel.SETTINGS -> "删除本机全部存档"
                        DeleteDataLabel.CONFIRM -> "永久删除"
                    },
                ) },
            ),
            ActionSemantic.OPEN_NOTIFICATION_SETTINGS to textEntry("打开系统通知设置"),
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
            StateSemantic.LOAD_ERROR to textEntry("存档读取失败", IconRole.ERROR),
            StateSemantic.EMPTY to textEntry("暂无内容"),
            StateSemantic.ACTIVE to textEntry("进行中"),
            StateSemantic.COMPLETED to textEntry("已完成", IconRole.COMPLETION),
            StateSemantic.PAUSED to textEntry("暂停"),
            StateSemantic.ARCHIVED to textEntry("归档"),
            StateSemantic.REVIEW_REQUIRED to textEntry("待重审", IconRole.WARNING),
            StateSemantic.SNOOZED to textEntry("暂缓中"),
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
            ScreenSemantic.CHARACTER_TITLE to textEntry("角色档案"),
            ScreenSemantic.CHARACTER_BODY to textEntry("成长来自行动，而不是清单的长度。"),
            ScreenSemantic.PROFILE_PROGRESS to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.PROGRESS),
                render = { arguments -> arguments.require(SemanticParameters.PROGRESS).let { progress ->
                    NarrativePresentation("${progress.intoLevel} / ${progress.needed} XP")
                } },
            ),
            ScreenSemantic.TOTAL_XP to textEntry("累计经验"),
            ScreenSemantic.COMPLETION_COUNT to textEntry("完成次数"),
            ScreenSemantic.CURRENT_STREAK to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.COUNT),
                render = { arguments -> NarrativePresentation(
                    "${arguments.require(SemanticParameters.COUNT).value} 天",
                ) },
            ),
            ScreenSemantic.STREAK_BODY to textEntry("当前连续行动 · 休息不会扣经验"),
            ScreenSemantic.SKILLS_TITLE to textEntry("技能成长"),
            ScreenSemantic.SKILLS_BODY to textEntry("这些是游戏内行动记录，不是现实能力评分。"),
            ScreenSemantic.SKILL_LEVEL to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.SKILL_LEVEL),
                render = { arguments -> arguments.require(SemanticParameters.SKILL_LEVEL).let { value ->
                    NarrativePresentation("${skillLabel(value.skill)} · Lv.${value.level.value}")
                } },
            ),
            ScreenSemantic.SKILL_XP to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.TOTAL_XP),
                render = { arguments -> NarrativePresentation(
                    "${arguments.require(SemanticParameters.TOTAL_XP).value} XP",
                ) },
            ),
            ScreenSemantic.ACHIEVEMENTS_TITLE to textEntry("成就档案"),
            ScreenSemantic.ACHIEVEMENTS_BODY to textEntry("成就按有效完成记录计算；撤销完成会同步重新计算。"),
            ScreenSemantic.JOURNAL_TITLE to textEntry("冒险日志"),
            ScreenSemantic.JOURNAL_BODY to textEntry("发生过的行动，都有迹可循。"),
            ScreenSemantic.ALL_EVENTS to textEntry("全部事件"),
            ScreenSemantic.JOURNAL_NOTES to textEntry("冒险手记"),
            ScreenSemantic.JOURNAL_EMPTY_TITLE to textEntry("写下第一篇手记"),
            ScreenSemantic.JOURNAL_EMPTY_BODY to textEntry("今天发现了什么、被什么卡住，都值得记录。"),
            ScreenSemantic.EVENT_HEADER to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.EVENT_HEADER),
                render = { arguments -> arguments.require(SemanticParameters.EVENT_HEADER).let { header ->
                    NarrativePresentation(
                        eventLabel(header.kind) + " · " + Instant.ofEpochMilli(header.moment.epochMillis)
                            .atZone(java.time.ZoneId.of(header.moment.zoneId))
                            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                    )
                } },
            ),
            ScreenSemantic.JOURNAL_XP to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.SIGNED_XP),
                render = { arguments -> arguments.require(SemanticParameters.SIGNED_XP).let { xp ->
                    NarrativePresentation("${if (xp.value > 0) "+" else ""}${xp.value} XP")
                } },
            ),
            ScreenSemantic.PLAYER_SETTINGS_TITLE to textEntry("玩家设置"),
            ScreenSemantic.NARRATIVE_SYSTEM_BODY to textEntry("同一份真实任务、进度与存档，只改变应用内的表达和材质。"),
            ScreenSemantic.EARTH_NATIVE_SYSTEM_NAME to textEntry("地球原生"),
            ScreenSemantic.EARTH_NATIVE_SYSTEM_BODY to textEntry("清晰、克制的现实探索终端"),
            ScreenSemantic.CULTIVATION_SYSTEM_NAME to textEntry("修仙 · 宗门战令"),
            ScreenSemantic.CULTIVATION_SYSTEM_BODY to textEntry("黑漆玉简、稀缺金箔与朱砂劫痕"),
            ScreenSemantic.NARRATIVE_PREVIEW_EMPTY to textEntry("接取第一项真实任务后，可在这里对照两套体系。"),
            ScreenSemantic.NARRATIVE_FALLBACK to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.NARRATIVE_ID),
                render = { arguments -> NarrativePresentation(
                    "请求的体系 ${fallbackNarrativeLabel(arguments.require(SemanticParameters.NARRATIVE_ID))} 当前不可用；" +
                        "已使用地球原生，原请求 ID 仍保留在存档中。",
                    iconRole = IconRole.WARNING,
                    colorRole = ColorRole.WARNING,
                ) },
            ),
            ScreenSemantic.INTERFACE_STYLE to textEntry("界面风格"),
            ScreenSemantic.THEME_SYSTEM to textEntry("跟随系统"),
            ScreenSemantic.THEME_LIGHT to textEntry("明亮"),
            ScreenSemantic.THEME_DARK to textEntry("深色"),
            ScreenSemantic.REMINDERS_TITLE to textEntry("冒险提醒"),
            ScreenSemantic.REMINDERS_BODY to textEntry("每日约定时间之后提醒，系统省电可能导致延迟。"),
            ScreenSemantic.ENABLE_DAILY_REMINDER to textEntry("开启每日提醒"),
            ScreenSemantic.NOTIFICATIONS_DISABLED to textEntry("当前系统通知未开启。"),
            ScreenSemantic.TIMEZONE_INFO to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.ZONE_ID),
                render = { arguments -> NarrativePresentation(
                    "存档结算时区：${arguments.require(SemanticParameters.ZONE_ID).value}\n" +
                        "日常按此时区的自然日刷新，旅行或修改设备时区不会切换结算时区。1.0 不提供时区迁移。",
                ) },
            ),
            ScreenSemantic.BACKUP_TITLE to textEntry("本地存档"),
            ScreenSemantic.BACKUP_BODY to textEntry("导出的是已保存的数据；导入会先校验，再请求覆盖确认。"),
            ScreenSemantic.BACKUP_SECURITY_BODY to textEntry(
                "备份为明文 JSON，包含任务和手记。请勿公开分享；文件上限 8 MiB。" +
                    "卸载或清除应用数据会删除本机存档。",
            ),
            ScreenSemantic.ABOUT_TITLE to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.VERSION_NAME),
                render = { arguments -> NarrativePresentation(
                    "地球 Online ${arguments.require(SemanticParameters.VERSION_NAME).value}",
                ) },
            ),
            ScreenSemantic.ABOUT_BODY to textEntry(
                "离线单人版 · Kotlin + Jetpack Compose\n不接入广告、分析 SDK、账号或 AI 云服务。",
            ),
            ScreenSemantic.PRIVACY_TITLE to textEntry("隐私说明 · 离线 1.0"),
            ScreenSemantic.PRIVACY_BODY to textEntry(
                "本应用在应用沙盒的 Room 数据库中保存玩家设置、任务、主线、完成记录及手记。\n\n" +
                    "应用没有联网、定位、广告、埋点和远程 AI 功能。系统通知仅在你主动开启后使用；通知不显示任务标题。\n\n" +
                    "存档依靠设备本身的存储保护，数据库未另加应用层加密。系统自动备份已在清单中禁用；厂商行为仍需真机验证。\n\n" +
                    "导出会把明文数据写入你选择的位置，所选文件提供商可能是云盘。导入仅在本机解析。分享和保管导出文件由你控制。\n\n" +
                    "删除本机存档不会删除你之前导出的文件。",
            ),
            ScreenSemantic.DELETE_DATA_TITLE to textEntry("删除本机全部存档"),
            ScreenSemantic.DELETE_DATA_BODY to textEntry(
                "任务、主线、经验和日志都会删除。无法在应用内撤回。请先导出存档，再输入“删除”确认。",
            ),
            ScreenSemantic.DELETE_CONFIRM_FIELD to textEntry("输入：删除"),
            ScreenSemantic.RESTORE_TITLE to textEntry("覆盖本机存档？"),
            ScreenSemantic.RESTORE_VALIDATED to textEntry("已通过格式、摘要、容量与关联校验。"),
            ScreenSemantic.RESTORE_PLAYER to textEntry("玩家"),
            ScreenSemantic.RESTORE_SUMMARY to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.RESTORE_SUMMARY),
                render = { arguments -> arguments.require(SemanticParameters.RESTORE_SUMMARY).let { summary ->
                    NarrativePresentation(
                        "任务变化：${summary.questChanges} 项\n" +
                            "完成变化：${summary.completionChanges} 项\n" +
                            "契约变化：${summary.contractChanges} 项\n" +
                            "债务记录变化：${summary.debtRecordChanges} 项" +
                            "（${summary.currentDebtXp} XP → ${summary.importedDebtXp} XP）\n" +
                            "恢复路线变化：${summary.recoveryRouteChanges} 项\n" +
                            "叙事体系变化：${summary.narrativeChanges} 项",
                    )
                } },
            ),
            ScreenSemantic.RESTORE_WARNING to textEntry(
                "当前存档将被完整替换，不会自动合并，也不会保留快照外的债务或体系偏好。" +
                    "建议先取消并导出旧存档。导入后提醒默认关闭。",
            ),
            ErrorSemantic.TITLE to textEntry("标题需为 1–120 个字符。"),
            ErrorSemantic.DESCRIPTION to textEntry("内容不能为空，且不能超过 4000 个字符。"),
            ErrorSemantic.MINUTES to textEntry("预计时长需为 1–480 分钟。"),
            ErrorSemantic.PRIORITY to textEntry("优先级需为 1–3。"),
            ErrorSemantic.DATE to textEntry("日期超出支持范围，请使用 1900–2200 年的日期。"),
            ErrorSemantic.PROFILE to textEntry("请检查玩家名、服务器名称和提醒时间。"),
            ErrorSemantic.MISSING_QUEST to textEntry("任务已不存在，请刷新后重试。"),
            ErrorSemantic.INACTIVE to textEntry("请先重新接取这个任务。"),
            ErrorSemantic.NOT_AVAILABLE to textEntry("任务尚未开始、正在暂缓或已经完成。"),
            ErrorSemantic.REPEAT_LOCKED to textEntry("已有完成记录，不能更改任务类型。请归档后创建新任务。"),
            ErrorSemantic.MISSING_GOAL to textEntry("主线不存在或已归档，请重新选择。"),
            ErrorSemantic.LIMIT to textEntry("存档已达安全容量上限，本次修改未保存。请导出备份后开启新存档。"),
            ErrorSemantic.INVALID_BACKUP to textEntry("存档校验失败，原有数据没有被修改。"),
            NotificationSemantic.REMINDER_CHANNEL to textEntry("每日冒险提醒"),
            NotificationSemantic.REMINDER_TITLE to textEntry("冒险仍在继续"),
            NotificationSemantic.COMPLETION_UNDONE to textEntry("已撤销，经验也已恢复至完成前。"),
            NotificationSemantic.QUEST_POSTPONED to textEntry("已暂缓一天。原截止日期保留，不扣经验。"),
            NotificationSemantic.REMINDER_AVAILABLE to NarrativeSemanticEntry(
                parameters = setOf(SemanticParameters.COUNT),
                render = { arguments -> NarrativePresentation(
                    "还有 ${arguments.require(SemanticParameters.COUNT).value} 项可执行任务。选一件小事，继续前进。",
                ) },
            ),
            NotificationSemantic.REMINDER_CONFIG_FAILED to textEntry(
                "存档已保存，但提醒调度失败。请重新打开应用后检查提醒设置。",
            ),
            NotificationSemantic.NOTIFICATION_PERMISSION_DENIED to textEntry(
                "通知权限未开启，任务功能不受影响。可在系统设置中手动开启。",
            ),
            NotificationSemantic.BACKUP_EXPORTED to textEntry("存档已导出。此文件为明文，请妥善保存。"),
            NotificationSemantic.BACKUP_RESTORED to textEntry("存档已恢复，提醒已关闭。需要时请重新开启。"),
            NotificationSemantic.FILE_IO_FAILED to textEntry("文件读写失败。请检查存储空间和文件访问权限。"),
            NotificationSemantic.OPERATION_FAILED to textEntry(
                "操作遇到错误，请检查当前状态后重试。导入文件必须完整且版本兼容。",
            ),
            NotificationSemantic.OPEN_SETTINGS_FAILED to textEntry("无法打开系统设置，请手动进入应用通知设置。"),
            StateSemantic.UNLOCKED to textEntry("已解锁"),
            StateSemantic.LOCKED to textEntry("未解锁"),
            AchievementSemantic.FIRST_STEP to textEntry("第一步 · 完成 1 项任务"),
            AchievementSemantic.TEN_QUESTS to textEntry("初露锋芒 · 完成 10 项"),
            AchievementSemantic.HUNDRED_QUESTS to textEntry("冒险家 · 完成 100 项"),
            AchievementSemantic.THREE_DAYS to textEntry("渐入佳境 · 连续 3 天"),
            AchievementSemantic.SEVEN_DAYS to textEntry("稳定前行 · 连续 7 天"),
            AchievementSemantic.BOSS_CLEAR to textEntry("突破时刻 · 完成 Boss"),
            AchievementSemantic.ALL_ROUNDER to textEntry("全面探索 · 涉及 5 种技能"),
            EventSemantic.JOINED to textEntry("玩家上线"),
            EventSemantic.CREATED to textEntry("接取任务"),
            EventSemantic.EDITED to textEntry("调整任务"),
            EventSemantic.COMPLETED to textEntry("任务完成"),
            EventSemantic.UNDONE to textEntry("撤销完成"),
            EventSemantic.POSTPONED to textEntry("暂缓任务"),
            EventSemantic.PAUSED to textEntry("暂停任务"),
            EventSemantic.RESUMED to textEntry("重新接取"),
            EventSemantic.ARCHIVED to textEntry("归档任务"),
            EventSemantic.GOAL_CREATED to textEntry("开启主线"),
            EventSemantic.GOAL_EDITED to textEntry("调整主线"),
            EventSemantic.GOAL_ARCHIVED to textEntry("主线归档"),
            EventSemantic.NOTE to textEntry("冒险手记"),
            EventSemantic.RESTORED to textEntry("恢复存档"),
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

    private fun fallbackNarrativeLabel(requestedId: RequestedNarrativeId): String =
        requestedId.value
            .replace('\n', ' ')
            .replace('\r', ' ')
            .take(80)
            .ifBlank { "（空 ID）" }

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

    private fun eventLabel(kind: EventKind) = when (kind) {
        EventKind.JOINED -> "玩家上线"
        EventKind.CREATED -> "接取任务"
        EventKind.EDITED -> "调整任务"
        EventKind.COMPLETED -> "任务完成"
        EventKind.UNDONE -> "撤销完成"
        EventKind.POSTPONED -> "暂缓任务"
        EventKind.PAUSED -> "暂停任务"
        EventKind.RESUMED -> "重新接取"
        EventKind.ARCHIVED -> "归档任务"
        EventKind.GOAL_CREATED -> "开启主线"
        EventKind.GOAL_EDITED -> "调整主线"
        EventKind.GOAL_ARCHIVED -> "主线归档"
        EventKind.NOTE -> "冒险手记"
        EventKind.RESTORED -> "恢复存档"
    }
}
