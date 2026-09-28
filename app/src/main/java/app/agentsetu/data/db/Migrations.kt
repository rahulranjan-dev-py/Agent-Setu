package app.agentsetu.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Hand-written schema migrations. Every statement that creates a table or index must be exactly what
 * Room expects for the current entities; `MigrationSqlTest` compares these constants with the schema
 * Room exports at build time (app/schemas), so a mismatch fails the build instead of crashing users.
 */
object Migrations {

    /** 2 -> 3 (1.1.0): receipts against ledger entries, and monthly incentive statements. */
    object V3 {
        const val CREATE_RECEIPT =
            "CREATE TABLE IF NOT EXISTS `commission_receipt` (`id` TEXT NOT NULL, `entryId` TEXT NOT NULL, " +
                "`amountPaise` INTEGER NOT NULL, `date` TEXT NOT NULL, `mode` TEXT NOT NULL, `reference` TEXT NOT NULL, " +
                "`note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        const val INDEX_RECEIPT_ENTRY =
            "CREATE INDEX IF NOT EXISTS `index_commission_receipt_entryId` ON `commission_receipt` (`entryId`)"
        const val CREATE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS `incentive_statement` (`id` TEXT NOT NULL, `month` TEXT NOT NULL, " +
                "`amountPaise` INTEGER NOT NULL, `date` TEXT NOT NULL, `mode` TEXT NOT NULL, `reference` TEXT NOT NULL, " +
                "`note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        const val INDEX_STATEMENT_MONTH =
            "CREATE INDEX IF NOT EXISTS `index_incentive_statement_month` ON `incentive_statement` (`month`)"

        /**
         * Amounts marked received in 1.0.x become one receipt each (mode OTHER, dated as recorded or
         * the first of the month), so every received rupee has a receipt behind it.
         */
        const val BACKFILL_RECEIPTS =
            "INSERT INTO commission_receipt (id, entryId, amountPaise, date, mode, reference, note, createdAt, updatedAt, deleted) " +
                "SELECT lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || " +
                "substr('89ab', abs(random()) % 4 + 1, 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6))), " +
                "id, receivedPaise, COALESCE(receivedDate, period || '-01'), 'OTHER', '', '', updatedAt, updatedAt, 0 " +
                "FROM commission_entry WHERE deleted = 0 AND receivedPaise IS NOT NULL AND receivedPaise > 0"
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(V3.CREATE_RECEIPT)
            db.execSQL(V3.INDEX_RECEIPT_ENTRY)
            db.execSQL(V3.CREATE_STATEMENT)
            db.execSQL(V3.INDEX_STATEMENT_MONTH)
            db.execSQL(V3.BACKFILL_RECEIPTS)
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_2_3)
}
