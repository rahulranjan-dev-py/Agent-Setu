package app.agentsetu.core.commission

import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.StaffType
import java.math.BigDecimal
import java.time.LocalDate

/** The parts of a CommissionRule that decide whether it applies and how much it pays. */
data class RuleSpec(
    val id: String,
    val productCode: String,
    val policyCategory: PolicyCategory,
    val staffTypes: Set<StaffType>,
    val basis: CommissionBasis,
    /** Percent for PERCENT_* bases; rupees per case for FLAT_PER_CASE. */
    val rate: BigDecimal,
    /** 1 = procurement (first year), 2 = renewal (any later year), null = not year-based. */
    val yearOfPolicy: Int?,
    val minPremiumTermYears: Int?,
    val maxPremiumTermYears: Int?,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
) {
    fun isInForceOn(date: LocalDate): Boolean =
        !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo))
}

/** What is being paid for. */
data class CommissionQuery(
    val productCode: String,
    val policyCategory: PolicyCategory,
    val staffType: StaffType,
    val premiumTermYears: Int?,
    /** 1 for the first policy year, 2, 3, ... for renewals; null for non-insurance products. */
    val policyYear: Int?,
    /** Date the premium/deposit was received; picks the rule version in force. */
    val date: LocalDate,
)
