package xyz.winhok.earthonline.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object DatabaseMigrations {
    val V1_TO_V2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `narrative_preferences` (
                    `playerId` INTEGER NOT NULL,
                    `narrativeId` TEXT NOT NULL,
                    PRIMARY KEY(`playerId`),
                    FOREIGN KEY(`playerId`) REFERENCES `player`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""".trimIndent(),
            )
            db.execSQL(
                """INSERT OR IGNORE INTO `narrative_preferences` (`playerId`, `narrativeId`)
                    SELECT `id`, '${EarthDatabase.EARTH_NATIVE_NARRATIVE_ID}' FROM `player` WHERE `id` = 1
                """.trimIndent(),
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `contracts` (
                    `id` TEXT NOT NULL,
                    `questId` TEXT NOT NULL,
                    `occurrence` TEXT NOT NULL,
                    `zoneId` TEXT NOT NULL,
                    `dueDay` INTEGER NOT NULL,
                    `originalRewardXp` INTEGER NOT NULL,
                    `currentRewardXp` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `signedAt` INTEGER NOT NULL,
                    `closedAt` INTEGER,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`questId`) REFERENCES `quests`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""".trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_contracts_questId_occurrence` ON `contracts` (`questId`, `occurrence`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_contracts_dueDay` ON `contracts` (`dueDay`)",
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `contract_revisions` (
                    `id` TEXT NOT NULL,
                    `contractId` TEXT NOT NULL,
                    `sequence` INTEGER NOT NULL,
                    `kind` TEXT NOT NULL,
                    `previousDueDay` INTEGER,
                    `newDueDay` INTEGER,
                    `previousRewardXp` INTEGER NOT NULL,
                    `newRewardXp` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `idempotencyKey` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`contractId`) REFERENCES `contracts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""".trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contract_revisions_contractId_sequence` ON `contract_revisions` (`contractId`, `sequence`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contract_revisions_idempotencyKey` ON `contract_revisions` (`idempotencyKey`)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `consequence_events` (
                    `id` TEXT NOT NULL,
                    `contractId` TEXT NOT NULL,
                    `kind` TEXT NOT NULL,
                    `xp` INTEGER NOT NULL,
                    `effectiveDay` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `idempotencyKey` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`contractId`) REFERENCES `contracts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""".trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_consequence_events_contractId` ON `consequence_events` (`contractId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_consequence_events_idempotencyKey` ON `consequence_events` (`idempotencyKey`)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `consequence_adjustments` (
                    `id` TEXT NOT NULL,
                    `consequenceId` TEXT NOT NULL,
                    `kind` TEXT NOT NULL,
                    `xp` INTEGER NOT NULL,
                    `reasonCategory` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `idempotencyKey` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`consequenceId`) REFERENCES `consequence_events`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""".trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_consequence_adjustments_consequenceId` ON `consequence_adjustments` (`consequenceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_consequence_adjustments_idempotencyKey` ON `consequence_adjustments` (`idempotencyKey`)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `repayment_allocations` (
                    `id` TEXT NOT NULL,
                    `consequenceId` TEXT NOT NULL,
                    `completionId` TEXT NOT NULL,
                    `xp` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `idempotencyKey` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`consequenceId`) REFERENCES `consequence_events`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(`completionId`) REFERENCES `completions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""".trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_repayment_allocations_completionId_consequenceId` ON `repayment_allocations` (`completionId`, `consequenceId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_repayment_allocations_consequenceId` ON `repayment_allocations` (`consequenceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_repayment_allocations_idempotencyKey` ON `repayment_allocations` (`idempotencyKey`)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `clock_boundaries` (
                    `id` TEXT NOT NULL,
                    `zoneId` TEXT NOT NULL,
                    `lastSettledDay` INTEGER NOT NULL,
                    `lastSettledAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )""".trimIndent(),
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `recovery_routes` (
                    `id` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `triggerKind` TEXT NOT NULL,
                    `targetDebtXp` INTEGER NOT NULL,
                    `openedAt` INTEGER NOT NULL,
                    `closedAt` INTEGER,
                    `idempotencyKey` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )""".trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recovery_routes_idempotencyKey` ON `recovery_routes` (`idempotencyKey`)")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `recovery_nodes` (
                    `routeId` TEXT NOT NULL,
                    `questId` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    `completedAt` INTEGER,
                    PRIMARY KEY(`routeId`, `questId`),
                    FOREIGN KEY(`routeId`) REFERENCES `recovery_routes`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(`questId`) REFERENCES `quests`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""".trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recovery_nodes_questId` ON `recovery_nodes` (`questId`)")
        }
    }
}
