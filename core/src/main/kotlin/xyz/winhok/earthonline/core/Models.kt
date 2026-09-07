package xyz.winhok.earthonline.core

import java.time.Instant
import java.time.ZoneId

enum class QuestKind { SIDE, DAILY, BOSS }
enum class Difficulty(val xp: Int, val energy: Int) { EASY(10, 1), NORMAL(25, 2), HARD(50, 3) }
enum class Skill { KNOWLEDGE, VITALITY, CREATION, CONNECTION, DISCIPLINE }
enum class QuestState { ACTIVE, PAUSED, ARCHIVED }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class EventKind { JOINED, CREATED, EDITED, COMPLETED, UNDONE, POSTPONED, PAUSED, RESUMED, ARCHIVED, GOAL_CREATED, GOAL_EDITED, GOAL_ARCHIVED, NOTE, RESTORED }

data class Player(
    val name: String = "",
    val server: String = "现实服",
    val zoneId: String = ZoneId.systemDefault().id,
    val joinedAt: Long = 0,
    val onboarded: Boolean = false,
    val theme: ThemeMode = ThemeMode.DARK,
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val lastReminderDay: Long? = null,
)

data class Goal(
    val id: String,
    val title: String,
    val description: String = "",
    val archived: Boolean = false,
    val createdAt: Long,
)

data class Quest(
    val id: String,
    val title: String,
    val description: String = "",
    val kind: QuestKind = QuestKind.SIDE,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val skill: Skill = Skill.DISCIPLINE,
    val priority: Int = 2,
    val estimatedMinutes: Int = 25,
    // For DAILY this is the first available date, otherwise it is a deadline.
    val dueDay: Long? = null,
    val goalId: String? = null,
    val state: QuestState = QuestState.ACTIVE,
    val snoozedUntilDay: Long? = null,
    val postponeCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

/** One persistent row per occurrence. Undo revokes the row, never invents negative XP. */
data class Completion(
    val id: String,
    val questId: String,
    val occurrence: String,
    val title: String,
    val kind: QuestKind,
    val skill: Skill,
    val xp: Int,
    val completedAt: Long,
    val completedDay: Long,
    val revokedAt: Long? = null,
)

data class JournalEvent(
    val id: String,
    val kind: EventKind,
    val text: String,
    val createdAt: Long,
    val questId: String? = null,
    val xp: Int = 0,
)

data class World(
    val player: Player = Player(),
    val goals: List<Goal> = emptyList(),
    val quests: List<Quest> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val events: List<JournalEvent> = emptyList(),
) {
    fun dayAt(millis: Long): Long = Instant.ofEpochMilli(millis)
        .atZone(ZoneId.of(player.zoneId)).toLocalDate().toEpochDay()
}

data class QuestDraft(
    val id: String? = null,
    val title: String,
    val description: String = "",
    val kind: QuestKind = QuestKind.SIDE,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val skill: Skill = Skill.DISCIPLINE,
    val priority: Int = 2,
    val estimatedMinutes: Int = 25,
    val dueDay: Long? = null,
    val goalId: String? = null,
)

enum class RuleError {
    TITLE, DESCRIPTION, MINUTES, PRIORITY, DATE, PROFILE, MISSING_QUEST, INACTIVE,
    NOT_AVAILABLE, REPEAT_LOCKED, MISSING_GOAL, LIMIT, INVALID_BACKUP,
}
class RuleViolation(val reason: RuleError) : IllegalArgumentException(reason.name)
internal fun requireRule(value: Boolean, error: RuleError) { if (!value) throw RuleViolation(error) }
