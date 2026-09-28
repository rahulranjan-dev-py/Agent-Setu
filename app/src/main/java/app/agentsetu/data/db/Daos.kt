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

    @Query("SELECT * FROM customer WHERE id = :id")
    fun observe(id: String): Flow<CustomerEntity?>

    @Query("SELECT COUNT(*) FROM customer WHERE deleted = 0")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(customer: CustomerEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM product WHERE deleted = 0 AND active = 1 ORDER BY productGroup, code")
    fun observeActive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun get(id: String): ProductEntity?

    @Query("SELECT * FROM product WHERE code = :code")
    suspend fun byCode(code: String): ProductEntity?

    @Query("SELECT * FROM product")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product")
    suspend fun all(): List<ProductEntity>

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

    @Query("SELECT * FROM sales_lead WHERE deleted = 0 AND stage NOT IN ('ISSUED', 'LOST')")
    suspend fun open(): List<LeadEntity>

    @Query("SELECT * FROM sales_lead WHERE id = :id")
    suspend fun get(id: String): LeadEntity?

    @Query("UPDATE sales_lead SET deleted = 1, updatedAt = :now WHERE customerId = :customerId")
    suspend fun softDeleteForCustomer(customerId: String, now: Long)

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

    @Query("UPDATE holding SET deleted = 1, updatedAt = :now WHERE customerId = :customerId")
    suspend fun softDeleteForCustomer(customerId: String, now: Long)

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

    @Query("SELECT * FROM commission_entry WHERE id = :id")
    suspend fun get(id: String): CommissionEntryEntity?

    @Query("SELECT * FROM commission_entry WHERE holdingId = :holdingId AND period = :period")
    suspend fun find(holdingId: String, period: String): CommissionEntryEntity?

    /** Ledger lines for one month with the customer and product names the screen shows. */
    @Query(
        "SELECT e.id, e.period, e.policyYear, e.status, e.baseAmountPaise, e.expectedPaise, e.receivedPaise, " +
            "e.receivedDate, e.rateApplied, c.name AS customerName, p.code AS productCode, " +
            "p.nameEn AS productNameEn, p.nameHi AS productNameHi " +
            "FROM commission_entry e " +
            "JOIN holding h ON h.id = e.holdingId " +
            "JOIN customer c ON c.id = h.customerId " +
            "JOIN product p ON p.id = h.productId " +
            "WHERE e.deleted = 0 AND e.period = :period ORDER BY c.name COLLATE NOCASE",
    )
    fun observeLedger(period: String): Flow<List<LedgerRow>>

    /** Every ledger line of one customer, newest month first (the customer's page). */
    @Query(
        "SELECT e.id, e.period, e.policyYear, e.status, e.baseAmountPaise, e.expectedPaise, e.receivedPaise, " +
            "e.receivedDate, e.rateApplied, c.name AS customerName, p.code AS productCode, " +
            "p.nameEn AS productNameEn, p.nameHi AS productNameHi " +
            "FROM commission_entry e " +
            "JOIN holding h ON h.id = e.holdingId " +
            "JOIN customer c ON c.id = h.customerId " +
            "JOIN product p ON p.id = h.productId " +
            "WHERE e.deleted = 0 AND h.customerId = :customerId ORDER BY e.period DESC, p.code",
    )
    fun observeLedgerForCustomer(customerId: String): Flow<List<LedgerRow>>

    @Query("SELECT * FROM commission_entry WHERE deleted = 0 AND period = :period AND status IN ('EXPECTED', 'PARTLY_RECEIVED')")
    suspend fun pendingForPeriod(period: String): List<CommissionEntryEntity>

    @Upsert
    suspend fun upsert(entry: CommissionEntryEntity)
}

@Dao
interface CommissionReceiptDao {
    @Query("SELECT * FROM commission_receipt WHERE deleted = 0 AND entryId = :entryId ORDER BY date, createdAt")
    fun observeForEntry(entryId: String): Flow<List<CommissionReceiptEntity>>

    @Query("SELECT * FROM commission_receipt WHERE deleted = 0 AND entryId = :entryId ORDER BY date, createdAt")
    suspend fun forEntry(entryId: String): List<CommissionReceiptEntity>

    @Query("SELECT * FROM commission_receipt WHERE id = :id")
    suspend fun get(id: String): CommissionReceiptEntity?

    @Upsert
    suspend fun upsert(receipt: CommissionReceiptEntity)
}

@Dao
interface IncentiveStatementDao {
    @Query("SELECT * FROM incentive_statement WHERE deleted = 0 AND month = :month ORDER BY date, createdAt")
    fun observeForMonth(month: String): Flow<List<IncentiveStatementEntity>>

    @Query("SELECT * FROM incentive_statement WHERE id = :id")
    suspend fun get(id: String): IncentiveStatementEntity?

    @Upsert
    suspend fun upsert(statement: IncentiveStatementEntity)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder WHERE id = :id")
    suspend fun get(id: String): ReminderEntity?

    @Query("UPDATE reminder SET deleted = 1, updatedAt = :now WHERE subjectId = :subjectId")
    suspend fun softDeleteForSubject(subjectId: String, now: Long)

    /** Run before the customer's holdings and leads are soft-deleted (it looks them up). */
    @Query(
        "UPDATE reminder SET deleted = 1, updatedAt = :now WHERE " +
            "subjectId IN (SELECT id FROM holding WHERE customerId = :customerId) OR " +
            "subjectId IN (SELECT id FROM sales_lead WHERE customerId = :customerId)",
    )
    suspend fun softDeleteForCustomer(customerId: String, now: Long)

    /**
     * Open reminders up to [until] with the names the Today screen and notifications show.
     * A reminder whose customer was deleted, or whose policy/account is no longer active, is hidden.
     */
    @Query(REMINDER_ROWS)
    fun observeOpen(until: LocalDate): Flow<List<ReminderRow>>

    @Query(REMINDER_ROWS)
    suspend fun open(until: LocalDate): List<ReminderRow>

    @Query("SELECT * FROM reminder WHERE deleted = 0 AND done = 0 AND dueDate <= :until ORDER BY dueDate")
    fun observeOpenDueBy(until: LocalDate): Flow<List<ReminderEntity>>

    /** Nightly generation may produce the same reminder again; the unique index turns that into a no-op. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(reminders: List<ReminderEntity>)

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)
}

private const val REMINDER_ROWS =
    "SELECT r.id, r.type, r.subjectId, r.dueDate, c.id AS customerId, c.name AS customerName, c.mobile AS mobile, " +
        "p.nameEn AS productNameEn, p.nameHi AS productNameHi, h.instalmentPaise AS instalmentPaise, " +
        "h.amountPaise AS amountPaise, l.source AS note " +
        "FROM reminder r " +
        "LEFT JOIN holding h ON h.id = r.subjectId AND r.type != 'FOLLOW_UP' " +
        "LEFT JOIN sales_lead l ON l.id = r.subjectId AND r.type = 'FOLLOW_UP' " +
        "JOIN customer c ON c.id = COALESCE(h.customerId, l.customerId) " +
        "LEFT JOIN product p ON p.id = COALESCE(h.productId, l.productId) " +
        "WHERE r.deleted = 0 AND r.done = 0 AND r.dueDate <= :until AND c.deleted = 0 " +
        "AND (h.id IS NULL OR (h.deleted = 0 AND h.status = 'ACTIVE')) " +
        "AND (l.id IS NULL OR l.deleted = 0) " +
        "ORDER BY r.dueDate, c.name COLLATE NOCASE"

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
