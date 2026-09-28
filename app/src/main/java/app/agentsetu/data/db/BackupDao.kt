package app.agentsetu.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Whole-database read and replace, used only by backup and restore. Includes soft-deleted rows. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM user_profile") suspend fun profiles(): List<UserProfileEntity>
    @Query("SELECT * FROM customer") suspend fun customers(): List<CustomerEntity>
    @Query("SELECT * FROM product") suspend fun products(): List<ProductEntity>
    @Query("SELECT * FROM sales_lead") suspend fun leads(): List<LeadEntity>
    @Query("SELECT * FROM holding") suspend fun holdings(): List<HoldingEntity>
    @Query("SELECT * FROM commission_rule") suspend fun rules(): List<CommissionRuleEntity>
    @Query("SELECT * FROM commission_entry") suspend fun entries(): List<CommissionEntryEntity>
    @Query("SELECT * FROM commission_receipt") suspend fun receipts(): List<CommissionReceiptEntity>
    @Query("SELECT * FROM incentive_statement") suspend fun statements(): List<IncentiveStatementEntity>
    @Query("SELECT * FROM reminder") suspend fun reminders(): List<ReminderEntity>
    @Query("SELECT * FROM target") suspend fun targets(): List<TargetEntity>
    @Query("SELECT * FROM interest_rate") suspend fun interestRates(): List<InterestRateEntity>

    @Query("DELETE FROM user_profile") suspend fun clearProfiles()
    @Query("DELETE FROM customer") suspend fun clearCustomers()
    @Query("DELETE FROM product") suspend fun clearProducts()
    @Query("DELETE FROM sales_lead") suspend fun clearLeads()
    @Query("DELETE FROM holding") suspend fun clearHoldings()
    @Query("DELETE FROM commission_rule") suspend fun clearRules()
    @Query("DELETE FROM commission_entry") suspend fun clearEntries()
    @Query("DELETE FROM commission_receipt") suspend fun clearReceipts()
    @Query("DELETE FROM incentive_statement") suspend fun clearStatements()
    @Query("DELETE FROM reminder") suspend fun clearReminders()
    @Query("DELETE FROM target") suspend fun clearTargets()
    @Query("DELETE FROM interest_rate") suspend fun clearInterestRates()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProfiles(rows: List<UserProfileEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCustomers(rows: List<CustomerEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProducts(rows: List<ProductEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLeads(rows: List<LeadEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHoldings(rows: List<HoldingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRules(rows: List<CommissionRuleEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEntries(rows: List<CommissionEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReceipts(rows: List<CommissionReceiptEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStatements(rows: List<IncentiveStatementEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReminders(rows: List<ReminderEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTargets(rows: List<TargetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertInterestRates(rows: List<InterestRateEntity>)
}
