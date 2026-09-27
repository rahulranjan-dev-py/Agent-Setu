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
        ReminderEntity::class,
        TargetEntity::class,
        InterestRateEntity::class,
    ],
    version = 2,
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
    abstract fun reminderDao(): ReminderDao
    abstract fun targetDao(): TargetDao
    abstract fun interestRateDao(): InterestRateDao

    companion object {
        const val FILE_NAME = "agentsetu.db"
    }
}
