package app.agentsetu.core.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Shape of the bundled sample rate files in data/seed. They are loaded into the editable
 * CommissionRule / InterestRate tables on first launch; the app never reads rates from code.
 */
@Serializable
data class CommissionSeedFile(
    val schemaVersion: Int,
    val generatedOn: String,
    val warning: String,
    val commissionRules: List<CommissionRuleSeed>,
)

@Serializable
data class CommissionRuleSeed(
    val id: String,
    val productGroup: String,
    val productCode: String,
    val policyCategory: String,
    val staffTypes: List<String>,
    val basis: String,
    val rate: Double,
    val yearOfPolicy: Int? = null,
    val minPremiumTermYears: Int? = null,
    val maxPremiumTermYears: Int? = null,
    val effectiveFrom: String,
    val effectiveTo: String? = null,
    val orderRef: String,
    val sourceUrls: List<String> = emptyList(),
    val confidence: String,
    val notes: String = "",
    val isSample: Boolean,
    val verified: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val deleted: Boolean,
)

@Serializable
data class InterestSeedFile(
    val schemaVersion: Int,
    val generatedOn: String,
    val warning: String,
    val interestRates: List<InterestRateSeed>,
)

@Serializable
data class InterestRateSeed(
    val id: String,
    val scheme: String,
    val rate: Double,
    val compounding: String,
    val effectiveFrom: String,
    val effectiveTo: String? = null,
    val source: String,
    val sourceUrls: List<String> = emptyList(),
    val confidence: String,
    val notes: String = "",
    val isSample: Boolean,
    val verified: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val deleted: Boolean,
)

object SeedJson {
    const val COMMISSION_FILE = "commission_rules.sample.json"
    const val INTEREST_FILE = "interest_rates.sample.json"

    /** Strict: unknown keys fail, so a typo in a hand-edited seed file is caught by tests. */
    val json = Json { ignoreUnknownKeys = false }

    fun parseCommission(text: String): CommissionSeedFile = json.decodeFromString(text)
    fun parseInterest(text: String): InterestSeedFile = json.decodeFromString(text)
}
