@file:UseSerializers(LocalDateIsoSerializer::class)

package app.agentsetu.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.agentsetu.core.backup.LocalDateIsoSerializer
import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.CommissionStatus
import app.agentsetu.core.model.Confidence
import app.agentsetu.core.model.HoldingStatus
import app.agentsetu.core.model.LeadStage
import app.agentsetu.core.model.PaymentFrequency
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.ProductGroup
import app.agentsetu.core.model.ReminderType
import app.agentsetu.core.model.RefLast4
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

// Conventions for every table (see CLAUDE.md):
//  - UUID `id`, epoch-millis `createdAt` / `updatedAt`, soft-delete `deleted`;
//  - money as whole paise (Long); rates as decimal text so 2.5 stays exactly 2.5;
//  - dates as ISO text (yyyy-MM-dd), which sorts and compares correctly in SQL.
// No foreign-key constraints: rows are soft-deleted, and a later cloud sync may deliver rows out of order.

private fun newId() = UUID.randomUUID().toString()

/** No Aadhaar, PAN or account numbers - by design there is no column for them. */
@Serializable
@Entity(tableName = "customer", indices = [Index("village"), Index("name")])
data class CustomerEntity(
    @PrimaryKey override val id: String = newId(),
    val name: String,
    val mobile: String?,
    val village: String?,
    val dob: LocalDate? = null,
    val tags: List<String> = emptyList(),
    val consentGiven: Boolean,
    val notes: String = "",
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncEntity

@Serializable
@Entity(tableName = "product", indices = [Index(value = ["code"], unique = true)])
data class ProductEntity(
    @PrimaryKey override val id: String = newId(),
    /** Shared with rate rules; "TD" rules cover TD_1Y ... TD_5Y. */
    val code: String,
    val productGroup: ProductGroup,
    val nameEn: String,
    val nameHi: String,
    val isCustom: Boolean,
    val active: Boolean = true,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncEntity

@Serializable
@Entity(tableName = "sales_lead", indices = [Index("customerId"), Index("nextFollowUp"), Index("stage")])
data class LeadEntity(
    @PrimaryKey override val id: String = newId(),
    val customerId: String,
    /** Empty for a general follow-up not yet tied to a product. */
    val productId: String,
    val stage: LeadStage,
    val nextFollowUp: LocalDate?,
    /** Where the lead came from, or the user's note about the follow-up. */
    val source: String = "",
    val lostReason: String? = null,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncEntity

/** One row per policy or account the user services. */
@Serializable
@Entity(tableName = "holding", indices = [Index("customerId"), Index("productId"), Index("maturityDate")])
data class HoldingEntity(
    @PrimaryKey override val id: String = newId(),
    val customerId: String,
    val productId: String,
    /** At most the last 4 digits of the policy/account number; enforced below. */
    val refLast4: String?,
    /** AEA / NON_AEA for PLI; ANY for everything else. */
    val policyCategory: PolicyCategory = PolicyCategory.ANY,
    val premiumTermYears: Int? = null,
    /** Sum assured (insurance) or deposit amount (savings), in paise. */
    val amountPaise: Long,
    /** Premium or instalment per period, in paise; null for one-time deposits. */
    val instalmentPaise: Long? = null,
    val frequency: PaymentFrequency,
    val startDate: LocalDate,
    val maturityDate: LocalDate?,
    val status: HoldingStatus = HoldingStatus.ACTIVE,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncEntity {
    init {
        RefLast4.require(refLast4)
    }
}

/** Editable, dated rate rule. Never hard-coded; seeded from data/seed and revised via RuleRevision. */
@Serializable
@Entity(tableName = "commission_rule", indices = [Index("productCode"), Index("effectiveFrom")])
data class CommissionRuleEntity(
    @PrimaryKey override val id: String = newId(),
    val productGroup: ProductGroup,
    val productCode: String,
    val policyCategory: PolicyCategory,
    /** StaffType names. */
    val staffTypes: List<String>,
    val basis: CommissionBasis,
    val rate: String,
    val yearOfPolicy: Int?,
    val minPremiumTermYears: Int?,
    val maxPremiumTermYears: Int?,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val orderRef: String,
    val sourceUrls: List<String> = emptyList(),
    val confidence: Confidence,
    val notes: String = "",
    val isSample: Boolean,
    val verified: Boolean,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncEntity

/**
 * One expected (and later received) commission for a holding and period. The rule id and rate used
 * are copied in, so editing a rule later never changes what was already recorded.
 */
@Serializable
@Entity(
    tableName = "commission_entry",
    // One entry per holding per month. (Not including nullable policyYear: SQLite treats NULLs as distinct.)
    indices = [Index("period"), Index(value = ["holdingId", "period"], unique = true)],
)
data class CommissionEntryEntity(
    @PrimaryKey override val id: String = newId(),
    val holdingId: String,
    /** yyyy-MM of the premium/deposit this commission is for. */
    val period: String,
    val policyYear: Int?,
    val baseAmountPaise: Long,
    val ruleId: String?,
    val rateApplied: String?,
    /** Null when no rule matched (status NO_RULE). */
    val expectedPaise: Long?,
    val receivedPaise: Long? = null,
    val receivedDate: LocalDate? = null,
    val status: CommissionStatus,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncEntity

/**
 * Generated nightly; the unique index makes regeneration idempotent. [subjectId] is a lead id for
 * FOLLOW_UP and a holding id for every other type (one non-null column, because SQLite unique
 * indexes treat NULLs as distinct and would let duplicates through).
 */
@Serializable
@Entity(
    tableName = "reminder",
    indices = [Index("dueDate"), Index(value = ["type", "subjectId", "dueDate"], unique = true)],
)
data class ReminderEntity(
    @PrimaryKey override val id: String = newId(),
    val type: ReminderType,
    val subjectId: String,
    val dueDate: LocalDate,
    val done: Boolean = false,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncEntity

/** Achieved value is computed from holdings when shown, so it is not stored. */
@Serializable
@Entity(tableName = "target", indices = [Index(value = ["month", "productGroup"], unique = true)])
data class TargetEntity(
    @PrimaryKey override val id: String = newId(),
    /** yyyy-MM */
    val month: String,
    val productGroup: ProductGroup,
    val targetPaise: Long,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncEntity

/** Editable, dated interest rate per scheme; feeds the calculators. Updated quarterly by the user. */
@Serializable
@Entity(tableName = "interest_rate", indices = [Index("scheme"), Index("effectiveFrom")])
data class InterestRateEntity(
    @PrimaryKey override val id: String = newId(),
    /** Product code, e.g. TD_5Y. */
    val scheme: String,
    val rate: String,
    val compounding: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val source: String,
    val sourceUrls: List<String> = emptyList(),
    val confidence: Confidence,
    val notes: String = "",
    val isSample: Boolean,
    val verified: Boolean,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
) : SyncEntity
