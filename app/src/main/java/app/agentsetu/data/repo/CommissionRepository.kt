package app.agentsetu.data.repo

import androidx.room.withTransaction
import app.agentsetu.core.commission.CommissionCalculator
import app.agentsetu.core.commission.CommissionQuery
import app.agentsetu.core.commission.CommissionResult
import app.agentsetu.core.commission.RuleRevision
import app.agentsetu.core.commission.RuleSpec
import app.agentsetu.core.model.CommissionStatus
import app.agentsetu.core.model.Money
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.CommissionEntryEntity
import app.agentsetu.data.db.toSpec
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommissionRepository @Inject constructor(
    private val db: AgentSetuDatabase,
) {
    private val rules get() = db.commissionRuleDao()

    suspend fun liveRules(): List<RuleSpec> = rules.live().map { it.toSpec() }

    /** "Expected commission shown instantly" on the Add business screen; nothing is saved. */
    suspend fun preview(query: CommissionQuery, baseAmount: BigDecimal): CommissionResult =
        CommissionCalculator.expected(liveRules(), query, baseAmount)

    /**
     * Records the expected commission for one holding and month. The rule id and rate are copied
     * into the entry, so later rate changes never rewrite the ledger. If the month already has an
     * entry for this holding, that entry is returned unchanged. With no (or no single) rule,
     * the entry is saved as NO_RULE with no amount, and the user is asked to add or fix a rule.
     */
    suspend fun recordExpected(
        holdingId: String,
        period: YearMonth,
        query: CommissionQuery,
        baseAmount: BigDecimal,
    ): CommissionEntryEntity {
        // One entry per holding per month: marking the same premium twice must not double-count.
        db.commissionEntryDao().find(holdingId, period.toString())?.let { return it }
        val now = System.currentTimeMillis()
        val result = preview(query, baseAmount)
        val found = result as? CommissionResult.Expected
        val entry = CommissionEntryEntity(
            holdingId = holdingId,
            period = period.toString(),
            policyYear = query.policyYear,
            baseAmountPaise = Money.toPaise(baseAmount),
            ruleId = found?.rule?.id,
            rateApplied = found?.rule?.rate?.toPlainString(),
            expectedPaise = found?.amount?.let(Money::toPaise),
            status = if (found != null) CommissionStatus.EXPECTED else CommissionStatus.NO_RULE,
            createdAt = now,
            updatedAt = now,
        )
        db.commissionEntryDao().upsert(entry)
        return entry
    }

    /**
     * Changes a rule's rate from [from] onwards without losing history (see RuleRevision).
     * The new version is the user's own rule: no longer a sample, and verified only if they say so.
     */
    suspend fun changeRate(
        ruleId: String,
        newRate: BigDecimal,
        from: LocalDate,
        orderRef: String,
        verified: Boolean,
    ): String = db.withTransaction {
        val current = requireNotNull(rules.get(ruleId)) { "No rule $ruleId" }
        require(!current.deleted) { "Rule $ruleId was deleted" }
        val outcome = RuleRevision.changeRate(current.toSpec(), newRate, from, UUID.randomUUID().toString())
        val now = System.currentTimeMillis()
        rules.upsert(current.copy(effectiveTo = outcome.old.effectiveTo, deleted = outcome.oldDeleted, updatedAt = now))
        rules.upsert(
            current.copy(
                id = outcome.new.id,
                rate = outcome.new.rate.toPlainString(),
                effectiveFrom = outcome.new.effectiveFrom,
                effectiveTo = outcome.new.effectiveTo,
                orderRef = orderRef,
                isSample = false,
                verified = verified,
                createdAt = now,
                updatedAt = now,
                deleted = false,
            ),
        )
        outcome.new.id
    }
}
