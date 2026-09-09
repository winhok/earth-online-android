package xyz.winhok.earthonline.core

enum class SemanticCategory(val wirePrefix: String) {
    DESTINATION("destination"),
    SCREEN("screen"),
    ACTION("action"),
    FIELD("field"),
    STATE("state"),
    ERROR("error"),
    NOTIFICATION("notification"),
    ACHIEVEMENT("achievement"),
    EVENT("event"),
}

enum class ScreenSemantic(override val wireId: String) : SemanticKey {
    APP_NAME("screen.app-name"),
    JOIN_TAGLINE("screen.join-tagline"),
    JOIN_PRIVACY_NOTICE("screen.join-privacy-notice"),
    DEFAULT_SERVER_NAME("screen.default-server-name"),
    LOAD_ERROR_TITLE("screen.load-error-title"),
    LOAD_ERROR_BODY("screen.load-error-body"),
    DASHBOARD_DAY_MODE("screen.dashboard-day-mode"),
    DASHBOARD_LEVEL("screen.dashboard-level"),
    DASHBOARD_PROGRESS("screen.dashboard-progress"),
    DASHBOARD_PROMPT_TITLE("screen.dashboard-prompt-title"),
    DASHBOARD_PROMPT_BODY("screen.dashboard-prompt-body"),
    MINUTE_OPTION("screen.minute-option"),
    ENERGY_LOW("screen.energy-low"),
    ENERGY_MEDIUM("screen.energy-medium"),
    ENERGY_HIGH("screen.energy-high"),
    RECOMMENDATION_EMPTY_TITLE("screen.recommendation-empty-title"),
    RECOMMENDATION_EMPTY_BODY("screen.recommendation-empty-body"),
    NEXT_QUEST("screen.next-quest"),
    RECOMMENDATION_REASONS("screen.recommendation-reasons"),
    RECOMMENDATION_NOTICE("screen.recommendation-notice"),
    GOAL_EMPTY_TITLE("screen.goal-empty-title"),
    GOAL_EMPTY_BODY("screen.goal-empty-body"),
    REVISIT_TITLE("screen.revisit-title"),
    REVISIT_BODY("screen.revisit-body"),
    EXECUTABLE_QUESTS("screen.executable-quests"),
    WORK_DONE_TITLE("screen.work-done-title"),
    WORK_DONE_BODY("screen.work-done-body"),
    QUESTS_TITLE("screen.quests-title"),
    QUESTS_BODY("screen.quests-body"),
    FILTER_GOALS("screen.filter-goals"),
    ALL_GOALS("screen.all-goals"),
    NO_GOALS_TITLE("screen.no-goals-title"),
    NO_GOALS_BODY("screen.no-goals-body"),
    QUEST_EMPTY_TITLE("screen.quest-empty-title"),
    QUEST_EMPTY_BODY("screen.quest-empty-body"),
    GOAL_TITLE("screen.goal-title"),
    GOAL_PROGRESS("screen.goal-progress"),
    ARCHIVE_GOAL_TITLE("screen.archive-goal-title"),
    ARCHIVE_GOAL_BODY("screen.archive-goal-body"),
    QUEST_META("screen.quest-meta"),
    QUEST_REWARD("screen.quest-reward"),
    QUEST_PRIORITY_DIFFICULTY("screen.quest-priority-difficulty"),
    QUEST_CREATE_TITLE("screen.quest-create-title"),
    QUEST_EDIT_TITLE("screen.quest-edit-title"),
    QUEST_TITLE_HELPER("screen.quest-title-helper"),
    QUEST_KIND_LOCKED_HELPER("screen.quest-kind-locked-helper"),
    QUEST_KIND_HELPER("screen.quest-kind-helper"),
    QUEST_KIND_SIDE("screen.quest-kind-side"),
    QUEST_KIND_DAILY("screen.quest-kind-daily"),
    QUEST_KIND_BOSS("screen.quest-kind-boss"),
    DIFFICULTY_EASY("screen.difficulty-easy"),
    DIFFICULTY_NORMAL("screen.difficulty-normal"),
    DIFFICULTY_HARD("screen.difficulty-hard"),
    DIFFICULTY_REWARD("screen.difficulty-reward"),
    SKILL_KNOWLEDGE("screen.skill-knowledge"),
    SKILL_VITALITY("screen.skill-vitality"),
    SKILL_CREATION("screen.skill-creation"),
    SKILL_CONNECTION("screen.skill-connection"),
    SKILL_DISCIPLINE("screen.skill-discipline"),
    PRIORITY_NORMAL("screen.priority-normal"),
    PRIORITY_IMPORTANT("screen.priority-important"),
    PRIORITY_URGENT("screen.priority-urgent"),
    DUE_START("screen.due-start"),
    DUE_END("screen.due-end"),
    INDEPENDENT_QUEST("screen.independent-quest"),
    SELECTED_DATE("screen.selected-date"),
    DESCRIPTION_COUNT("screen.description-count"),
    GOAL_CREATE_TITLE("screen.goal-create-title"),
    GOAL_EDIT_TITLE("screen.goal-edit-title"),
    GOAL_EDITOR_HELPER("screen.goal-editor-helper"),
    NOTE_EDITOR_TITLE("screen.note-editor-title"),
    NOTE_HELPER("screen.note-helper"),
    DISCARD_TITLE("screen.discard-title"),
    DISCARD_BODY("screen.discard-body"),
    QUEST_DETAIL_STATS("screen.quest-detail-stats"),
    QUEST_GOAL("screen.quest-goal"),
    QUEST_REVIEW_GUIDANCE("screen.quest-review-guidance"),
    QUEST_COMPLETED_AT("screen.quest-completed-at"),
    ARCHIVE_QUEST_TITLE("screen.archive-quest-title"),
    ARCHIVE_QUEST_BODY("screen.archive-quest-body"),
    CLOSE_EDITOR_ACCESSIBILITY("screen.close-editor-accessibility"),
    CHARACTER_TITLE("screen.character-title"),
    CHARACTER_BODY("screen.character-body"),
    PROFILE_PROGRESS("screen.profile-progress"),
    TOTAL_XP("screen.total-xp"),
    COMPLETION_COUNT("screen.completion-count"),
    CURRENT_STREAK("screen.current-streak"),
    STREAK_BODY("screen.streak-body"),
    SKILLS_TITLE("screen.skills-title"),
    SKILLS_BODY("screen.skills-body"),
    SKILL_LEVEL("screen.skill-level"),
    SKILL_XP("screen.skill-xp"),
    ACHIEVEMENTS_TITLE("screen.achievements-title"),
    ACHIEVEMENTS_BODY("screen.achievements-body"),
    JOURNAL_TITLE("screen.journal-title"),
    JOURNAL_BODY("screen.journal-body"),
    ALL_EVENTS("screen.all-events"),
    JOURNAL_NOTES("screen.journal-notes"),
    JOURNAL_EMPTY_TITLE("screen.journal-empty-title"),
    JOURNAL_EMPTY_BODY("screen.journal-empty-body"),
    EVENT_HEADER("screen.event-header"),
    JOURNAL_XP("screen.journal-xp"),
    PLAYER_SETTINGS_TITLE("screen.player-settings-title"),
    INTERFACE_STYLE("screen.interface-style"),
    THEME_SYSTEM("screen.theme-system"),
    THEME_LIGHT("screen.theme-light"),
    THEME_DARK("screen.theme-dark"),
    REMINDERS_TITLE("screen.reminders-title"),
    REMINDERS_BODY("screen.reminders-body"),
    ENABLE_DAILY_REMINDER("screen.enable-daily-reminder"),
    NOTIFICATIONS_DISABLED("screen.notifications-disabled"),
    TIMEZONE_INFO("screen.timezone-info"),
    BACKUP_TITLE("screen.backup-title"),
    BACKUP_BODY("screen.backup-body"),
    BACKUP_SECURITY_BODY("screen.backup-security-body"),
    ABOUT_TITLE("screen.about-title"),
    ABOUT_BODY("screen.about-body"),
    PRIVACY_TITLE("screen.privacy-title"),
    PRIVACY_BODY("screen.privacy-body"),
    DELETE_DATA_TITLE("screen.delete-data-title"),
    DELETE_DATA_BODY("screen.delete-data-body"),
    DELETE_CONFIRM_FIELD("screen.delete-confirm-field"),
    RESTORE_TITLE("screen.restore-title"),
    RESTORE_VALIDATED("screen.restore-validated"),
    RESTORE_PLAYER("screen.restore-player"),
    RESTORE_SUMMARY("screen.restore-summary"),
    RESTORE_WARNING("screen.restore-warning"),
    PLAYER_WELCOME("screen.player-welcome"),
    PROFILE_IDENTITY("screen.profile-identity"),
    QUEST_CONTENT("screen.quest-content"),
    GOAL_CONTENT("screen.goal-content"),
    NOTE_CONTENT("screen.note-content"),
    ;

    override val category = SemanticCategory.SCREEN
}

enum class ActionSemantic(override val wireId: String) : SemanticKey {
    JOIN("action.join"),
    CREATE_QUEST("action.create-quest"),
    VIEW_QUEST("action.view-quest"),
    COMPLETE_QUEST("action.complete-quest"),
    UNDO_COMPLETION("action.undo-completion"),
    POSTPONE_QUEST("action.postpone-quest"),
    PAUSE_QUEST("action.pause-quest"),
    RESUME_QUEST("action.resume-quest"),
    ARCHIVE_QUEST("action.archive-quest"),
    CREATE_GOAL("action.create-goal"),
    EDIT_GOAL("action.edit-goal"),
    ARCHIVE_GOAL("action.archive-goal"),
    WRITE_NOTE("action.write-note"),
    SAVE("action.save"),
    CANCEL("action.cancel"),
    DISCARD("action.discard"),
    EXPORT_BACKUP("action.export-backup"),
    IMPORT_BACKUP("action.import-backup"),
    CONFIRM_RESTORE("action.confirm-restore"),
    DELETE_ALL_DATA("action.delete-all-data"),
    OPEN_SETTINGS("action.open-settings"),
    OPEN_NOTIFICATION_SETTINGS("action.open-notification-settings"),
    RETRY_LOAD("action.retry-load"),
    RESCHEDULE_CONTRACT("action.reschedule-contract"),
    ABANDON_CONTRACT("action.abandon-contract"),
    RECOMMIT_CONTRACT("action.recommit-contract"),
    SETTLE_OVERDUE("action.settle-overdue"),
    VIEW_DEBT("action.view-debt"),
    OPEN_RECOVERY("action.open-recovery"),
    SELECT_RECOVERY_NODE("action.select-recovery-node"),
    APPLY_FORCE_MAJEURE("action.apply-force-majeure"),
    VIEW_ALL("action.view-all"),
    VIEW_GOAL_QUESTS("action.view-goal-quests"),
    EDIT_QUEST("action.edit-quest"),
    CLEAR_DATE("action.clear-date"),
    SELECT_DATE("action.select-date"),
    CONFIRM("action.confirm"),
    CONTINUE_EDITING("action.continue-editing"),
    CLOSE("action.close"),
    OPEN_PRIVACY("action.open-privacy"),
    ;

    override val category = SemanticCategory.ACTION
}

enum class FieldSemantic(override val wireId: String) : SemanticKey {
    PLAYER_NAME("field.player-name"),
    SERVER_NAME("field.server-name"),
    NARRATIVE_SYSTEM("field.narrative-system"),
    THEME("field.theme"),
    REMINDERS("field.reminders"),
    REMINDER_HOUR("field.reminder-hour"),
    QUEST_TITLE("field.quest-title"),
    QUEST_KIND("field.quest-kind"),
    DIFFICULTY("field.difficulty"),
    SKILL("field.skill"),
    ESTIMATED_MINUTES("field.estimated-minutes"),
    PRIORITY("field.priority"),
    DUE_DAY("field.due-day"),
    GOAL("field.goal"),
    DESCRIPTION("field.description"),
    NOTE("field.note"),
    QUEST_SEARCH("field.quest-search"),
    GOAL_TITLE("field.goal-title"),
    GOAL_DESCRIPTION("field.goal-description"),
    ;

    override val category = SemanticCategory.FIELD
}

enum class StateSemantic(override val wireId: String) : SemanticKey {
    LOADING("state.loading"),
    LOAD_ERROR("state.load-error"),
    EMPTY("state.empty"),
    ACTIVE("state.active"),
    PAUSED("state.paused"),
    ARCHIVED("state.archived"),
    COMPLETED("state.completed"),
    SNOOZED("state.snoozed"),
    REVIEW_REQUIRED("state.review-required"),
    UNLOCKED("state.unlocked"),
    LOCKED("state.locked"),
    CONTRACT_ACTIVE("state.contract-active"),
    CONTRACT_FULFILLED("state.contract-fulfilled"),
    CONTRACT_OVERDUE("state.contract-overdue"),
    CONTRACT_ABANDONED("state.contract-abandoned"),
    CONTRACT_EXEMPTED("state.contract-exempted"),
    DEBT("state.debt"),
    OUT_OF_ORDER("state.out-of-order"),
    RECOVERY_OPEN("state.recovery-open"),
    RECOVERY_CLOSED("state.recovery-closed"),
    ;

    override val category = SemanticCategory.STATE
}

enum class ErrorSemantic(
    val source: RuleError,
    override val wireId: String,
) : SemanticKey {
    TITLE(RuleError.TITLE, "error.title"),
    DESCRIPTION(RuleError.DESCRIPTION, "error.description"),
    MINUTES(RuleError.MINUTES, "error.minutes"),
    PRIORITY(RuleError.PRIORITY, "error.priority"),
    DATE(RuleError.DATE, "error.date"),
    PROFILE(RuleError.PROFILE, "error.profile"),
    MISSING_QUEST(RuleError.MISSING_QUEST, "error.missing-quest"),
    INACTIVE(RuleError.INACTIVE, "error.inactive"),
    NOT_AVAILABLE(RuleError.NOT_AVAILABLE, "error.not-available"),
    REPEAT_LOCKED(RuleError.REPEAT_LOCKED, "error.repeat-locked"),
    MISSING_GOAL(RuleError.MISSING_GOAL, "error.missing-goal"),
    LIMIT(RuleError.LIMIT, "error.limit"),
    INVALID_BACKUP(RuleError.INVALID_BACKUP, "error.invalid-backup"),
    ;

    override val category = SemanticCategory.ERROR
}

enum class AchievementSemantic(
    val sourceId: String,
    override val wireId: String,
) : SemanticKey {
    FIRST_STEP("first_step", "achievement.first-step"),
    TEN_QUESTS("ten_quests", "achievement.ten-quests"),
    HUNDRED_QUESTS("hundred_quests", "achievement.hundred-quests"),
    THREE_DAYS("three_days", "achievement.three-days"),
    SEVEN_DAYS("seven_days", "achievement.seven-days"),
    BOSS_CLEAR("boss_clear", "achievement.boss-clear"),
    ALL_ROUNDER("all_rounder", "achievement.all-rounder"),
    ;

    override val category = SemanticCategory.ACHIEVEMENT
}

enum class EventSemantic(
    val source: EventKind,
    override val wireId: String,
) : SemanticKey {
    JOINED(EventKind.JOINED, "event.joined"),
    CREATED(EventKind.CREATED, "event.created"),
    EDITED(EventKind.EDITED, "event.edited"),
    COMPLETED(EventKind.COMPLETED, "event.completed"),
    UNDONE(EventKind.UNDONE, "event.undone"),
    POSTPONED(EventKind.POSTPONED, "event.postponed"),
    PAUSED(EventKind.PAUSED, "event.paused"),
    RESUMED(EventKind.RESUMED, "event.resumed"),
    ARCHIVED(EventKind.ARCHIVED, "event.archived"),
    GOAL_CREATED(EventKind.GOAL_CREATED, "event.goal-created"),
    GOAL_EDITED(EventKind.GOAL_EDITED, "event.goal-edited"),
    GOAL_ARCHIVED(EventKind.GOAL_ARCHIVED, "event.goal-archived"),
    NOTE(EventKind.NOTE, "event.note"),
    RESTORED(EventKind.RESTORED, "event.restored"),
    ;

    override val category = SemanticCategory.EVENT
}

object NarrativeSemantics {
    val all: Set<SemanticKey> = buildSet {
        addAll(DestinationSemantic.entries)
        addAll(ScreenSemantic.entries)
        addAll(ActionSemantic.entries)
        addAll(FieldSemantic.entries)
        addAll(StateSemantic.entries)
        addAll(ErrorSemantic.entries)
        addAll(NotificationSemantic.entries)
        addAll(AchievementSemantic.entries)
        addAll(EventSemantic.entries)
    }
}

fun RuleError.semantic(): ErrorSemantic = ErrorSemantic.entries.first { it.source == this }

fun EventKind.semantic(): EventSemantic = EventSemantic.entries.first { it.source == this }

fun achievementSemantic(sourceId: String): AchievementSemantic? =
    AchievementSemantic.entries.firstOrNull { it.sourceId == sourceId }
