package app.agentsetu.data.repo

import androidx.room.withTransaction
import app.agentsetu.core.calc.DatedRevision
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.InterestRateEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterestRateRepository @Inject constructor(private val db: AgentSetuDatabase) {

    suspend fun inForce(scheme: String, date: LocalDate = LocalDate.now()): InterestRateEntity? =
        db.interestRateDao().inForce(scheme, date)

    /** New rate from [from] onwards; the old value is kept for earlier dates (DatedRevision). */
    suspend fun changeRate(id: String, newRate: BigDecimal, from: LocalDate, source: String, verified: Boolean) =
        db.withTransaction {
            val dao = db.interestRateDao()
            val current = requireNotNull(dao.get(id)) { "No rate $id" }
            require(!current.deleted) { "Rate $id was replaced" }
            val now = System.currentTimeMillis()
            val old = when (val plan = DatedRevision.plan(current.effectiveFrom, current.effectiveTo, from)) {
                is DatedRevision.Old.CloseOn -> current.copy(effectiveTo = plan.effectiveTo, updatedAt = now)
                DatedRevision.Old.SoftDelete -> current.copy(deleted = true, updatedAt = now)
            }
            val new = current.copy(
                id = UUID.randomUUID().toString(),
                rate = newRate.stripTrailingZeros().toPlainString(),
                effectiveFrom = from,
                effectiveTo = current.effectiveTo,
                source = source,
                isSample = false,
                verified = verified,
                createdAt = now,
                updatedAt = now,
                deleted = false,
            )
            dao.upsertAll(listOf(old, new))
        }
}
