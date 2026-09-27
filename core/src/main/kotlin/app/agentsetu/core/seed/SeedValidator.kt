package app.agentsetu.core.seed

import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.Confidence
import app.agentsetu.core.model.ProductGroup
import app.agentsetu.core.model.StaffType
import java.time.LocalDate
import java.util.UUID

/** Checks a seed file for mistakes before it can reach users. Returns human-readable problems. */
object SeedValidator {

    fun validate(file: CommissionSeedFile): List<String> {
        val problems = mutableListOf<String>()
        problems += duplicateIds(file.commissionRules.map { it.id })
        for (r in file.commissionRules) {
            val at = "rule ${r.id} (${r.productCode})"
            if (!isUuid(r.id)) problems += "$at: id is not a UUID"
            if (!isEnum<ProductGroup>(r.productGroup)) problems += "$at: unknown productGroup ${r.productGroup}"
            if (!isEnum<CommissionBasis>(r.basis)) problems += "$at: unknown basis ${r.basis}"
            if (!isEnum<Confidence>(r.confidence.uppercase())) problems += "$at: unknown confidence ${r.confidence}"
            if (r.staffTypes.isEmpty()) problems += "$at: no staffTypes"
            r.staffTypes.filterNot { isEnum<StaffType>(it) }.forEach { problems += "$at: unknown staffType $it" }
            if (r.basis != CommissionBasis.FLAT_PER_CASE.name && r.rate !in 0.0..100.0) {
                problems += "$at: percentage ${r.rate} outside 0-100"
            }
            if (r.rate < 0) problems += "$at: negative rate"
            val min = r.minPremiumTermYears
            val max = r.maxPremiumTermYears
            if (min != null && max != null && min > max) problems += "$at: term band $min > $max"
            problems += dateRange(at, r.effectiveFrom, r.effectiveTo)
            if (r.orderRef.isBlank()) problems += "$at: orderRef is empty"
            if (r.isSample && r.verified) problems += "$at: sample data cannot be marked verified"
        }
        return problems
    }

    fun validate(file: InterestSeedFile): List<String> {
        val problems = mutableListOf<String>()
        problems += duplicateIds(file.interestRates.map { it.id })
        for (r in file.interestRates) {
            val at = "rate ${r.id} (${r.scheme})"
            if (!isUuid(r.id)) problems += "$at: id is not a UUID"
            if (r.rate !in 0.0..25.0) problems += "$at: interest ${r.rate}% looks wrong"
            if (!isEnum<Confidence>(r.confidence.uppercase())) problems += "$at: unknown confidence ${r.confidence}"
            problems += dateRange(at, r.effectiveFrom, r.effectiveTo)
            if (r.isSample && r.verified) problems += "$at: sample data cannot be marked verified"
        }
        return problems
    }

    private fun duplicateIds(ids: List<String>): List<String> =
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.map { "duplicate id $it" }

    private fun dateRange(at: String, from: String, to: String?): List<String> {
        val start = parseIso(from) ?: return listOf("$at: bad effectiveFrom $from")
        if (to == null) return emptyList()
        val end = parseIso(to) ?: return listOf("$at: bad effectiveTo $to")
        return if (end.isBefore(start)) listOf("$at: effectiveTo before effectiveFrom") else emptyList()
    }

    private fun parseIso(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

    private fun isUuid(text: String): Boolean = runCatching { UUID.fromString(text) }.isSuccess

    private inline fun <reified E : Enum<E>> isEnum(name: String): Boolean =
        enumValues<E>().any { it.name == name }
}
