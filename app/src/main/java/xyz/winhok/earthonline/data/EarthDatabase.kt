package xyz.winhok.earthonline.data

import android.content.Context
import androidx.room.*
import xyz.winhok.earthonline.core.*

@Entity(tableName = "player")
data class PlayerEntity(@PrimaryKey val id: Int = 1, @Embedded val value: Player)

@Entity(tableName = "goals", primaryKeys = ["id"])
data class GoalEntity(@Embedded val value: Goal)

@Entity(
    tableName = "quests", primaryKeys = ["id"],
    foreignKeys = [ForeignKey(entity = GoalEntity::class, parentColumns = ["id"],
        childColumns = ["goalId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("goalId"), Index(value = ["state", "dueDay"])],
)
data class QuestEntity(@Embedded val value: Quest)

@Entity(
    tableName = "completions", primaryKeys = ["id"],
    foreignKeys = [ForeignKey(entity = QuestEntity::class, parentColumns = ["id"],
        childColumns = ["questId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["questId", "occurrence"], unique = true), Index("completedDay")],
)
data class CompletionEntity(@Embedded val value: Completion)

@Entity(
    tableName = "journal", primaryKeys = ["id"],
    foreignKeys = [ForeignKey(entity = QuestEntity::class, parentColumns = ["id"],
        childColumns = ["questId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("questId"), Index("createdAt")],
)
data class EventEntity(@Embedded val value: JournalEvent)

class Converters {
    @TypeConverter fun kindToString(value: QuestKind): String = value.name
    @TypeConverter fun stringToKind(value: String): QuestKind = QuestKind.valueOf(value)
    @TypeConverter fun difficultyToString(value: Difficulty): String = value.name
    @TypeConverter fun stringToDifficulty(value: String): Difficulty = Difficulty.valueOf(value)
    @TypeConverter fun skillToString(value: Skill): String = value.name
    @TypeConverter fun stringToSkill(value: String): Skill = Skill.valueOf(value)
    @TypeConverter fun stateToString(value: QuestState): String = value.name
    @TypeConverter fun stringToState(value: String): QuestState = QuestState.valueOf(value)
    @TypeConverter fun themeToString(value: ThemeMode): String = value.name
    @TypeConverter fun stringToTheme(value: String): ThemeMode = ThemeMode.valueOf(value)
    @TypeConverter fun eventToString(value: EventKind): String = value.name
    @TypeConverter fun stringToEvent(value: String): EventKind = EventKind.valueOf(value)
}

@Dao
interface EarthDao {
    @Query("SELECT * FROM player WHERE id = 1") suspend fun player(): PlayerEntity?
    @Query("SELECT * FROM goals ORDER BY createdAt DESC, id") suspend fun goals(): List<GoalEntity>
    @Query("SELECT * FROM quests ORDER BY createdAt DESC, id") suspend fun quests(): List<QuestEntity>
    @Query("SELECT * FROM completions ORDER BY completedAt DESC, id") suspend fun completions(): List<CompletionEntity>
    @Query("SELECT * FROM journal ORDER BY createdAt DESC, id") suspend fun events(): List<EventEntity>
    @Query("SELECT * FROM quests WHERE id = :id") suspend fun quest(id: String): QuestEntity?
    @Query("SELECT * FROM goals WHERE id = :id") suspend fun goal(id: String): GoalEntity?
    @Query("SELECT * FROM completions WHERE id = :id") suspend fun completion(id: String): CompletionEntity?
    @Query("SELECT EXISTS(SELECT 1 FROM completions WHERE questId = :questId)") suspend fun hasHistory(questId: String): Boolean
    @Query("SELECT COUNT(*) FROM quests") suspend fun questCount(): Int
    @Query("SELECT COUNT(*) FROM goals") suspend fun goalCount(): Int
    @Query("SELECT COUNT(*) FROM journal") suspend fun eventCount(): Int
    @Query("SELECT COUNT(*) FROM completions") suspend fun completionCount(): Int
    @Upsert suspend fun putPlayer(value: PlayerEntity)
    @Upsert suspend fun putQuest(value: QuestEntity)
    @Upsert suspend fun putGoal(value: GoalEntity)
    @Upsert suspend fun putCompletion(value: CompletionEntity)
    @Insert suspend fun putEvent(value: EventEntity)
    @Query("DELETE FROM journal") suspend fun clearEvents()
    @Query("DELETE FROM completions") suspend fun clearCompletions()
    @Query("DELETE FROM quests") suspend fun clearQuests()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM player") suspend fun clearPlayer()
}

@Database(
    entities = [PlayerEntity::class, GoalEntity::class, QuestEntity::class, CompletionEntity::class, EventEntity::class],
    version = 1, exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class EarthDatabase : RoomDatabase() {
    abstract fun dao(): EarthDao
    companion object {
        fun open(context: Context): EarthDatabase = Room.databaseBuilder(
            context.applicationContext, EarthDatabase::class.java, "earth-online.db"
        ).build() // Deliberately no fallbackToDestructiveMigration.
    }
}
