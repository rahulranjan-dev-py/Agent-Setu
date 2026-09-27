package app.agentsetu.core.commission

import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.StaffType
import java.math.BigDecimal
import java.time.LocalDate

internal fun rule(
    id: String,
    productCode: String,
    rate: String,
    policyCategory: PolicyCategory = PolicyCategory.ANY,
    staffTypes: Set<StaffType> = setOf(StaffType.GDS, StaffType.DEPARTMENTAL),
    basis: CommissionBasis = CommissionBasis.PERCENT_OF_PREMIUM,
    yearOfPolicy: Int? = null,
    minTerm: Int? = null,
    maxTerm: Int? = null,
    from: String = "2025-04-01",
    to: String? = null,
) = RuleSpec(
    id = id,
    productCode = productCode,
    policyCategory = policyCategory,
    staffTypes = staffTypes,
    basis = basis,
    rate = BigDecimal(rate),
    yearOfPolicy = yearOfPolicy,
    minPremiumTermYears = minTerm,
    maxPremiumTermYears = maxTerm,
    effectiveFrom = LocalDate.parse(from),
    effectiveTo = to?.let { LocalDate.parse(it) },
)

internal fun query(
    productCode: String,
    staffType: StaffType = StaffType.GDS,
    policyCategory: PolicyCategory = PolicyCategory.ANY,
    term: Int? = null,
    policyYear: Int? = null,
    date: String = "2026-09-01",
) = CommissionQuery(productCode, policyCategory, staffType, term, policyYear, LocalDate.parse(date))
