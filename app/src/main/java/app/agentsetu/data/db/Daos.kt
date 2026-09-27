package app.agentsetu.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customer WHERE deleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query(
        "SELECT * FROM customer WHERE deleted = 0 AND " +
            "(name LIKE '%' || :text || '%' OR mobile LIKE '%' || :text || '%' OR village LIKE '%' || :text || '%') " +
            "ORDER BY name COLLATE NOCASE",
    )
    fun search(text: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customer WHERE id = :id")
    suspend fun get(id: String): CustomerEntity?

    @Upsert
    suspend fun upsert(customer: CustomerEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM product WHERE deleted = 0 AND active = 1 ORDER BY productGroup, code")
    fun observeActive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun get(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(products: List<ProductEntity>)

    @Upsert
    suspend fun upsert(product: ProductEntity)
}

@Dao
interface LeadDao {
    @Query("SELECT * FROM sales_lead WHERE deleted = 0 AND stage NOT IN ('ISSUED', 'LOST') ORDER BY nextFollowUp IS NULL, nextFollowUp")
    fun observeOpen(): Flow<List<LeadEntity>>

    @Query("SELECT * FROM sales_lead WHERE deleted = 0 AND customerId = :customerId ORDER BY updatedAt DESC")
    fun observeForCustomer(customerId: String): Flow<List<LeadEntity>>

    @Query("SELECT * FROM sales_lead WHERE deleted = 0 AND stage NOT IN ('ISSUED', 'LOST') AND nextFollowUp <= :until")
    suspend fun followUpsDueBy(until: LocalDate): List<LeadEntity>

    @Upsert
    suspend fun upsert(lead: LeadEntity)
}

@Dao
interface HoldingDao {
    @Query("SELECT * FROM holding WHERE deleted = 0 AND customerId = :customerId ORDER BY startDate DESC")
    fun observeForCustomer(customerId: String): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holding WHERE deleted = 0 AND status = 'ACTIVE'")
    suspend fun active(): List<HoldingEntity>

    @Query(
        "SELECT * FROM holding WHERE deleted = 0 AND status = 'ACTIVE' " +
            "AND maturityDate BETWEEN :from AND :to ORDER BY maturityDate",
    )
    suspend fun maturingBetween(from: LocalDate, to: LocalDate): List<HoldingEntity>

    @Query("SELECT * FROM holding WHERE id = :id")
    suspend fun get(id: String): HoldingEntity?

    @Upsert
    suspend fun upsert(holding: HoldingEntity)
}

@Dao
interface CommissionRuleDao {
    /** Every version, including closed and soft-deleted ones: the history screen. */
    @Query("SELECT * FROM commission_rule ORDER BY productCode, effectiveFrom DESC")
    fun observeAllVersions(): Flow<List<CommissionRuleEntity>>

    @Query("SELECT * FROM commission_rule WHERE deleted = 0")
    suspend fun live(): List<CommissionRuleEntity>

    @Query("SELECT * FROM commission_rule WHERE id = :id")
    suspend fun get(id: String): CommissionRuleEntity?

    /** Adds new sample rules from an app update without touching rules the user already has or edited. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(rules: List<CommissionRuleEntity>)

    @Upsert
    suspend fun upsert(rule: CommissionRuleEntity)

    @Upsert
    suspend fun upsertAll(rules: List<CommissionRuleEntity>)

    @Query("UPDATE commission_rule SET deleted = 1, updatedAt = :now WHERE deleted = 0 AND id NOT IN (:keepIds)")
    suspend fun softDeleteAllExcept(keepIds: List<String>, now: Long)
}

@Dao
interface CommissionEntryDao {
    @Query("SELECT * FROM commission_entry WHERE deleted = 0 AND period = :period ORDER BY createdAt")
    fun observeForPeriod(period: String): Flow<List<CommissionEntryEntity>>

    @Query("SELECT * FROM commission_entry WHERE deleted = 0 AND status IN ('EXPECTED', 'PARTLY_RECEIVED', 'NO_RULE') ORDER BY period")
    fun observePending(): Flow<List<CommissionEntryEntity>>

    @Upsert
    suspend fun upsert(entry: CommissionEntryEntity)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder WHERE deleted = 0 AND done = 0 AND dueDate <= :until ORDER BY dueDate")
    fun observeOpenDueBy(until: LocalDate): Flow<List<ReminderEntity>>

    /** Nightly generation may produce the same reminder again; the unique index turns that into a no-op. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(reminders: List<ReminderEntity>)

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)
}

@Dao
interface TargetDao {
    @Query("SELECT * FROM target WHERE deleted = 0 AND month = :month")
    fun observeForMonth(month: String): Flow<List<TargetEntity>>

    @Upsert
    suspend fun upsert(target: TargetEntity)
}

@Dao
interface InterestRateDao {
    @Query("SELECT * FROM interest_rate ORDER BY scheme, effectiveFrom DESC")
    fun observeAllVersions(): Flow<List<InterestRateEntity>>

    @Query("SELECT * FROM interest_rate WHERE id = :id")
    suspend fun get(id: String): InterestRateEntity?

    /** The rate in force for a scheme on a date (latest start wins if the user left an overlap). */
    @Query(
        "SELECT * FROM interest_rate WHERE deleted = 0 AND scheme = :scheme AND effectiveFrom <= :date " +
            "AND (effectiveTo IS NULL OR effectiveTo >= :date) ORDER BY effectiveFrom DESC LIMIT 1",
    )
    suspend fun inForce(scheme: String, date: LocalDate): InterestRateEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(rates: List<InterestRateEntity>)

    @Upsert
    suspend fun upsertAll(rates: List<InterestRateEntity>)

    @Query("UPDATE interest_rate SET deleted = 1, updatedAt = :now WHERE deleted = 0 AND id NOT IN (:keepIds)")
    suspend fun softDeleteAllExcept(keepIds: List<String>, now: Long)
}
