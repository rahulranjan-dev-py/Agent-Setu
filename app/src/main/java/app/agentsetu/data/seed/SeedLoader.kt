package app.agentsetu.data.seed

import android.content.Context
import androidx.room.withTransaction
import app.agentsetu.core.catalogue.DefaultProducts
import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.Confidence
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.ProductGroup
import app.agentsetu.core.seed.CommissionRuleSeed
import app.agentsetu.core.seed.InterestRateSeed
import app.agentsetu.core.seed.SeedJson
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.CommissionRuleEntity
import app.agentsetu.data.db.InterestRateEntity
import app.agentsetu.data.db.ProductEntity
import app.agentsetu.data.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the bundled product catalogue and sample rate tables (assets from data/seed).
 *
 * After an app update: new sample rows are added, and sample rows the user never touched are
 * refreshed if the bundled copy is newer (a corrected sample rate reaches everyone). Any row the
 * user edited, verified, revised or deleted has a newer `updatedAt` and is left exactly as it is.
 */
@Singleton
class SeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AgentSetuDatabase,
    private val settings: AppSettings,
) {

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val seed = readSeed()
        if (settings.seedMarker == seed.marker) return@withContext
        val now = System.currentTimeMillis()
        db.withTransaction {
            db.productDao().insertIgnore(catalogue(now))
            val ruleDao = db.commissionRuleDao()
            ruleDao.insertIgnore(seed.rules)
            seed.rules.forEach { bundled ->
                val existing = ruleDao.get(bundled.id)
                if (existing != null && existing.isSample && existing.updatedAt < bundled.updatedAt) ruleDao.upsert(bundled)
            }
            val rateDao = db.interestRateDao()
            rateDao.insertIgnore(seed.rates)
            seed.rates.forEach { bundled ->
                val existing = rateDao.get(bundled.id)
                if (existing != null && existing.isSample && existing.updatedAt < bundled.updatedAt) {
                    rateDao.upsertAll(listOf(bundled))
                }
            }
        }
        settings.seedMarker = seed.marker
    }

    /** Settings -> Rates & rules -> "Reset to sample". Customers, holdings and the ledger are untouched. */
    suspend fun resetRatesToSample() = withContext(Dispatchers.IO) {
        val seed = readSeed()
        val now = System.currentTimeMillis()
        db.withTransaction {
            db.commissionRuleDao().softDeleteAllExcept(seed.rules.map { it.id }, now)
            db.commissionRuleDao().upsertAll(seed.rules.map { it.copy(updatedAt = now) })
            db.interestRateDao().softDeleteAllExcept(seed.rates.map { it.id }, now)
            db.interestRateDao().upsertAll(seed.rates.map { it.copy(updatedAt = now) })
        }
    }

    private class Seed(val rules: List<CommissionRuleEntity>, val rates: List<InterestRateEntity>, val marker: String)

    private fun readSeed(): Seed {
        val commission = SeedJson.parseCommission(asset(SeedJson.COMMISSION_FILE))
        val interest = SeedJson.parseInterest(asset(SeedJson.INTEREST_FILE))
        return Seed(
            rules = commission.commissionRules.map { it.toEntity() },
            rates = interest.interestRates.map { it.toEntity() },
            marker = listOf(commission.generatedOn, interest.generatedOn, DefaultProducts.all.size).joinToString("|"),
        )
    }

    private fun asset(name: String): String =
        context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun catalogue(now: Long) = DefaultProducts.all.map {
        ProductEntity(
            id = it.id,
            code = it.code,
            productGroup = it.group,
            nameEn = it.nameEn,
            nameHi = it.nameHi,
            isCustom = false,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun CommissionRuleSeed.toEntity() = CommissionRuleEntity(
        id = id,
        productGroup = ProductGroup.valueOf(productGroup),
        productCode = productCode,
        policyCategory = PolicyCategory.valueOf(policyCategory),
        staffTypes = staffTypes,
        basis = CommissionBasis.valueOf(basis),
        rate = rate.toString(),
        yearOfPolicy = yearOfPolicy,
        minPremiumTermYears = minPremiumTermYears,
        maxPremiumTermYears = maxPremiumTermYears,
        effectiveFrom = LocalDate.parse(effectiveFrom),
        effectiveTo = effectiveTo?.let(LocalDate::parse),
        orderRef = orderRef,
        sourceUrls = sourceUrls,
        confidence = Confidence.valueOf(confidence.uppercase()),
        notes = notes,
        isSample = isSample,
        verified = verified,
        createdAt = Instant.parse(createdAt).toEpochMilli(),
        updatedAt = Instant.parse(updatedAt).toEpochMilli(),
        deleted = deleted,
    )

    private fun InterestRateSeed.toEntity() = InterestRateEntity(
        id = id,
        scheme = scheme,
        rate = rate.toString(),
        compounding = compounding,
        effectiveFrom = LocalDate.parse(effectiveFrom),
        effectiveTo = effectiveTo?.let(LocalDate::parse),
        source = source,
        sourceUrls = sourceUrls,
        confidence = Confidence.valueOf(confidence.uppercase()),
        notes = notes,
        isSample = isSample,
        verified = verified,
        createdAt = Instant.parse(createdAt).toEpochMilli(),
        updatedAt = Instant.parse(updatedAt).toEpochMilli(),
        deleted = deleted,
    )
}
