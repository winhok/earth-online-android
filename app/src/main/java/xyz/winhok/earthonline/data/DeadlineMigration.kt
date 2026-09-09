package xyz.winhok.earthonline.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Preserve the user's v2 schema, then explicitly add the irreversible ledger metadata. */
object DeadlineMigration {
    val V2_TO_V3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE contracts ADD COLUMN signedKind TEXT NOT NULL DEFAULT 'SIDE'")
            db.execSQL("ALTER TABLE contracts ADD COLUMN signedSkill TEXT NOT NULL DEFAULT 'DISCIPLINE'")
            db.execSQL("ALTER TABLE contracts ADD COLUMN extensionCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE contracts ADD COLUMN fulfilledAt INTEGER")
            db.execSQL("ALTER TABLE contracts ADD COLUMN activeQuestId TEXT")
            db.execSQL("UPDATE contracts SET signedKind=(SELECT kind FROM quests WHERE quests.id=contracts.questId), signedSkill=(SELECT skill FROM quests WHERE quests.id=contracts.questId)")
            db.execSQL("UPDATE contracts SET extensionCount=CASE WHEN currentRewardXp=originalRewardXp THEN 0 WHEN currentRewardXp=(originalRewardXp*80+99)/100 THEN 1 WHEN currentRewardXp=(originalRewardXp*60+99)/100 THEN 2 ELSE 3 END")
            db.execSQL("UPDATE contracts SET fulfilledAt=closedAt WHERE status='FULFILLED'")
            db.execSQL("UPDATE contracts SET activeQuestId=questId WHERE status='ACTIVE'")
            db.execSQL("DROP INDEX index_contracts_questId_occurrence")
            db.execSQL("CREATE INDEX index_contracts_questId_occurrence ON contracts(questId, occurrence)")
            db.execSQL("CREATE UNIQUE INDEX index_contracts_activeQuestId ON contracts(activeQuestId)")
            db.execSQL("DROP INDEX index_consequence_events_contractId")
            db.execSQL("CREATE UNIQUE INDEX index_consequence_events_contractId ON consequence_events(contractId)")
            db.execSQL("ALTER TABLE repayment_allocations ADD COLUMN reversalOf TEXT")
            db.execSQL("DROP INDEX index_repayment_allocations_completionId_consequenceId")
            db.execSQL("CREATE INDEX index_repayment_allocations_completionId_consequenceId ON repayment_allocations(completionId, consequenceId)")
            db.execSQL("CREATE UNIQUE INDEX index_repayment_allocations_reversalOf ON repayment_allocations(reversalOf)")
            db.execSQL("ALTER TABLE recovery_routes ADD COLUMN closedThroughAssessmentCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE recovery_routes SET closedThroughAssessmentCount=(SELECT COUNT(*) FROM consequence_events WHERE kind='OVERDUE' AND createdAt<=recovery_routes.closedAt) WHERE status='CLOSED'")
            db.execSQL("CREATE TABLE IF NOT EXISTS progress_history (id INTEGER NOT NULL PRIMARY KEY, highestLevel INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS effect_preferences (id INTEGER NOT NULL PRIMARY KEY, sound INTEGER NOT NULL, haptics INTEGER NOT NULL, reducedMotion INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS presentation_preferences (narrativeId TEXT NOT NULL, preferenceKey TEXT NOT NULL, enabled INTEGER NOT NULL, PRIMARY KEY(narrativeId, preferenceKey))")
            // No contracts or liabilities are created for legacy dated tasks. Calibration is explicit.
        }
    }
}
