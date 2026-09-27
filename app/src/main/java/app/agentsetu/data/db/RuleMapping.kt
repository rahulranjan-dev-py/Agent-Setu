package app.agentsetu.data.db

import app.agentsetu.core.commission.RuleSpec
import app.agentsetu.core.model.StaffType
import java.math.BigDecimal

fun CommissionRuleEntity.toSpec(): RuleSpec = RuleSpec(
    id = id,
    productCode = productCode,
    policyCategory = policyCategory,
    // Unknown names (e.g. from a newer app version's rules file) are skipped, not fatal.
    staffTypes = staffTypes.mapNotNull { name -> StaffType.entries.firstOrNull { it.name == name } }.toSet(),
    basis = basis,
    rate = BigDecimal(rate),
    yearOfPolicy = yearOfPolicy,
    minPremiumTermYears = minPremiumTermYears,
    maxPremiumTermYears = maxPremiumTermYears,
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
)
