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
