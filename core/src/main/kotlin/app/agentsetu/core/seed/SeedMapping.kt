package app.agentsetu.core.seed

import app.agentsetu.core.commission.RuleSpec
import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.StaffType
import java.math.BigDecimal
import java.time.LocalDate

fun CommissionRuleSeed.toSpec(): RuleSpec = RuleSpec(
    id = id,
    productCode = productCode,
    policyCategory = PolicyCategory.valueOf(policyCategory),
    staffTypes = staffTypes.map { StaffType.valueOf(it) }.toSet(),
    basis = CommissionBasis.valueOf(basis),
    // Via toString so 2.5 stays 2.5, not 2.5000000000000000001.
    rate = BigDecimal(rate.toString()),
    yearOfPolicy = yearOfPolicy,
    minPremiumTermYears = minPremiumTermYears,
    maxPremiumTermYears = maxPremiumTermYears,
    effectiveFrom = LocalDate.parse(effectiveFrom),
    effectiveTo = effectiveTo?.let { LocalDate.parse(it) },
)
