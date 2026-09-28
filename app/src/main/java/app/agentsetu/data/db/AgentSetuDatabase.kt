package app.agentsetu.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserProfileEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
        LeadEntity::class,
        HoldingEntity::class,
        CommissionRuleEntity::class,
        CommissionEntryEntity::class,
        CommissionReceiptEntity::class,
        IncentiveStatementEntity::class,
        ReminderEntity::class,
        TargetEntity::class,
        InterestRateEntity::class,
    ],
    version = AgentSetuDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AgentSetuDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun leadDao(): LeadDao
    abstract fun holdingDao(): HoldingDao
    abstract fun commissionRuleDao(): CommissionRuleDao
    abstract fun commissionEntryDao(): CommissionEntryDao
    abstract fun commissionReceiptDao(): CommissionReceiptDao
    abstract fun incentiveStatementDao(): IncentiveStatementDao
    abstract fun reminderDao(): ReminderDao
    abstract fun targetDao(): TargetDao
    abstract fun interestRateDao(): InterestRateDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val FILE_NAME = "agentsetu.db"
        /** Bump together with a Migration in Migrations.kt; users' data must survive every update. */
        const val VERSION = 4

        /** Oldest schema a backup file may have and still be restored (restore upgrades it). */
        const val OLDEST_RESTORABLE_VERSION = 2
    }
}
