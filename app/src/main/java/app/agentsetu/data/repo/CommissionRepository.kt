package app.agentsetu.data.repo

import androidx.room.withTransaction
import app.agentsetu.core.commission.CommissionCalculator
import app.agentsetu.core.commission.CommissionQuery
import app.agentsetu.core.commission.CommissionResult
import app.agentsetu.core.commission.RuleRevision
import app.agentsetu.core.commission.RuleSpec
import app.agentsetu.core.ledger.Ledger
import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.CommissionStatus
import app.agentsetu.core.model.Confidence
import app.agentsetu.core.model.Money
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.ProductGroup
import app.agentsetu.core.model.ReceiptMode
import app.agentsetu.core.model.StaffType
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.CommissionEntryEntity
import app.agentsetu.data.db.CommissionReceiptEntity
import app.agentsetu.data.db.CommissionRuleEntity
import app.agentsetu.data.db.IncentiveStatementEntity
import app.agentsetu.data.db.ProductEntity
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
        // A soft-deleted entry (left by Undo) is revived under its own id: the unique index on
        // (holdingId, period) would refuse a second row anyway.
        val existing = db.commissionEntryDao().find(holdingId, period.toString())
        if (existing != null && !existing.deleted) return existing
        // Removed by the user as "not mine": stays removed even if the premium is collected again.
        if (existing != null && existing.status == CommissionStatus.SKIPPED) return existing
        val now = System.currentTimeMillis()
        val result = preview(query, baseAmount)
        val found = result as? CommissionResult.Expected
        val entry = CommissionEntryEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            holdingId = holdingId,
            period = period.toString(),
            policyYear = query.policyYear,
            baseAmountPaise = Money.toPaise(baseAmount),
            ruleId = found?.rule?.id,
            rateApplied = found?.rule?.rate?.toPlainString(),
            expectedPaise = found?.amount?.let(Money::toPaise),
            status = if (found != null) CommissionStatus.EXPECTED else CommissionStatus.NO_RULE,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            deleted = false,
        )
        db.commissionEntryDao().upsert(entry)
        return entry
    }

    /**
     * Entries recorded while no rule matched get another chance after the user adds or changes a
     * rule. The date used is the holding's due day in that month. Returns how many were resolved.
     */
    suspend fun recomputeNoRuleEntries(): Int = db.withTransaction {
        val staffType = db.userProfileDao().current()?.staffType ?: return@withTransaction 0
        val specs = liveRules()
        var resolved = 0
        for (entry in db.commissionEntryDao().noRule()) {
            val holding = db.holdingDao().get(entry.holdingId) ?: continue
            val product = db.productDao().get(holding.productId) ?: continue
            val month = YearMonth.parse(entry.period)
            val date = month.atDay(minOf(holding.startDate.dayOfMonth, month.lengthOfMonth()))
            val query = CommissionQuery(
                productCode = product.code,
                policyCategory = if (product.productGroup == ProductGroup.PLI) holding.policyCategory else PolicyCategory.ANY,
                staffType = staffType,
                premiumTermYears = holding.premiumTermYears,
                policyYear = entry.policyYear,
                date = date,
            )
            val found = CommissionCalculator.expected(specs, query, Money.fromPaise(entry.baseAmountPaise)) as? CommissionResult.Expected
                ?: continue
            val received = entry.receivedPaise
            val expected = Money.toPaise(found.amount)
            db.commissionEntryDao().upsert(
                entry.copy(
                    ruleId = found.rule.id,
                    rateApplied = found.rule.rate.toPlainString(),
                    expectedPaise = expected,
                    status = Ledger.statusAfterReceipt(expected, received ?: 0),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            resolved++
        }
        resolved
    }

    /**
     * Ends a rule: it stops applying after [lastDay]. Entries already recorded keep their rate.
     * A rule that has not started yet by then is soft-deleted instead (nothing to keep).
     */
    suspend fun endRule(ruleId: String, lastDay: LocalDate) {
        val current = requireNotNull(rules.get(ruleId)) { "No rule $ruleId" }
        require(!current.deleted) { "Rule $ruleId was deleted" }
        val now = System.currentTimeMillis()
        val ended = if (lastDay.isBefore(current.effectiveFrom)) {
            current.copy(deleted = true, updatedAt = now)
        } else {
            current.copy(effectiveTo = lastDay, isSample = false, updatedAt = now)
        }
        rules.upsert(ended)
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
        // The closed row is now the user's history, not a sample: a later seed update must not revive it.
        rules.upsert(current.copy(effectiveTo = outcome.old.effectiveTo, deleted = outcome.oldDeleted, isSample = false, updatedAt = now))
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

    /**
     * A new rule written by the user (for a bundled or custom product). It is their own: not a
     * sample, verified only if they say so, and it stays until they change or end it.
     */
    suspend fun addRule(
        product: ProductEntity,
        policyCategory: PolicyCategory,
        staffTypes: Set<StaffType>,
        basis: CommissionBasis,
        rate: BigDecimal,
        yearOfPolicy: Int?,
        minTermYears: Int?,
        maxTermYears: Int?,
        from: LocalDate,
        orderRef: String,
        verified: Boolean,
        notes: String = "",
    ): String {
        require(staffTypes.isNotEmpty()) { "A rule needs at least one staff type" }
        val now = System.currentTimeMillis()
        val rule = CommissionRuleEntity(
            productGroup = product.productGroup,
            productCode = product.code,
            policyCategory = if (product.productGroup == ProductGroup.PLI) policyCategory else PolicyCategory.ANY,
            staffTypes = staffTypes.map { it.name },
            basis = basis,
            rate = rate.toPlainString(),
            yearOfPolicy = yearOfPolicy,
            minPremiumTermYears = minTermYears,
            maxPremiumTermYears = maxTermYears,
            effectiveFrom = from,
            effectiveTo = null,
            orderRef = orderRef.trim(),
            confidence = if (verified) Confidence.HIGH else Confidence.MEDIUM,
            notes = notes.trim(),
            isSample = false,
            verified = verified,
            createdAt = now,
            updatedAt = now,
        )
        rules.upsert(rule)
        return rule.id
    }

    /**
     * A product the user sells that is not in the bundled catalogue (e.g. a new scheme or a local
     * incentive). Its code is unique and never collides with catalogue codes or their families.
     */
    suspend fun addCustomProduct(nameEn: String, nameHi: String, group: ProductGroup): ProductEntity {
        require(nameEn.isNotBlank()) { "A product needs a name" }
        val now = System.currentTimeMillis()
        val product = ProductEntity(
            code = CUSTOM_CODE_PREFIX + UUID.randomUUID().toString().substring(0, 8).uppercase(),
            productGroup = group,
            nameEn = nameEn.trim(),
            nameHi = nameHi.trim().ifBlank { nameEn.trim() },
            isCustom = true,
            createdAt = now,
            updatedAt = now,
        )
        db.productDao().upsert(product)
        return product
    }

    /**
     * "Not eligible / not mine": the entry stays in the ledger for the record but counts for nothing.
     * [skipped] = false brings it back with the status its receipts imply.
     */
    suspend fun setSkipped(entryId: String, skipped: Boolean) {
        val entry = db.commissionEntryDao().get(entryId) ?: return
        val status = if (skipped) CommissionStatus.SKIPPED else Ledger.statusAfterReceipt(entry.expectedPaise, entry.receivedPaise ?: 0)
        db.commissionEntryDao().upsert(entry.copy(status = status, updatedAt = System.currentTimeMillis()))
    }

    /**
     * Removes an entry and its receipts from the ledger. Marked SKIPPED as well, so collecting the
     * same premium again does not revive it (a plain Undo-delete would).
     */
    suspend fun deleteEntry(entryId: String) = db.withTransaction {
        val now = System.currentTimeMillis()
        val entry = db.commissionEntryDao().get(entryId) ?: return@withTransaction
        db.commissionReceiptDao().forEntry(entryId).forEach {
            db.commissionReceiptDao().upsert(it.copy(deleted = true, reference = "", note = "", updatedAt = now))
        }
        db.commissionEntryDao().upsert(entry.copy(status = CommissionStatus.SKIPPED, deleted = true, updatedAt = now))
    }

    /** Records one payment against an entry and refreshes the entry's received total and status. */
    suspend fun addReceipt(
        entryId: String,
        amount: BigDecimal,
        date: LocalDate,
        mode: ReceiptMode,
        reference: String,
        note: String,
    ): String = db.withTransaction {
        val now = System.currentTimeMillis()
        val receipt = CommissionReceiptEntity(
            entryId = entryId,
            amountPaise = Money.toPaise(amount),
            date = date,
            mode = mode,
            reference = reference.trim(),
            note = note.trim(),
            createdAt = now,
            updatedAt = now,
        )
        db.commissionReceiptDao().upsert(receipt)
        refreshEntry(entryId, now)
        receipt.id
    }

    suspend fun deleteReceipt(receiptId: String) = db.withTransaction {
        val now = System.currentTimeMillis()
        val receipt = db.commissionReceiptDao().get(receiptId) ?: return@withTransaction
        db.commissionReceiptDao().upsert(receipt.copy(deleted = true, reference = "", note = "", updatedAt = now))
        refreshEntry(receipt.entryId, now)
    }

    /** receivedPaise / receivedDate / status are derived from the entry's live receipts. */
    private suspend fun refreshEntry(entryId: String, now: Long) {
        val entry = db.commissionEntryDao().get(entryId) ?: return
        val receipts = db.commissionReceiptDao().forEntry(entryId)
        val received = Ledger.receivedTotal(receipts.map { it.amountPaise })
        db.commissionEntryDao().upsert(
            entry.copy(
                receivedPaise = received,
                receivedDate = receipts.maxOfOrNull { it.date },
                status = Ledger.statusAfterReceipt(entry.expectedPaise, received ?: 0),
                updatedAt = now,
            ),
        )
    }

    suspend fun saveStatement(
        id: String?,
        month: YearMonth,
        amount: BigDecimal,
        date: LocalDate,
        mode: ReceiptMode,
        reference: String,
        note: String,
    ): String {
        val now = System.currentTimeMillis()
        val existing = id?.let { db.incentiveStatementDao().get(it) }
        val statement = (existing ?: IncentiveStatementEntity(
            month = month.toString(), amountPaise = 0, date = date, mode = mode, createdAt = now, updatedAt = now,
        )).copy(
            amountPaise = Money.toPaise(amount),
            date = date,
            mode = mode,
            reference = reference.trim(),
            note = note.trim(),
            updatedAt = now,
            deleted = false,
        )
        db.incentiveStatementDao().upsert(statement)
        return statement.id
    }

    suspend fun deleteStatement(statementId: String) {
        val s = db.incentiveStatementDao().get(statementId) ?: return
        db.incentiveStatementDao().upsert(s.copy(deleted = true, reference = "", note = "", updatedAt = System.currentTimeMillis()))
    }

    /**
     * "Received as per this statement": every entry of the month still expecting money gets a
     * receipt for the outstanding amount, dated and referenced like the statement.
     * Returns how many entries were settled.
     */
    suspend fun settleMonthFromStatement(statementId: String): Int = db.withTransaction {
        val statement = db.incentiveStatementDao().get(statementId) ?: return@withTransaction 0
        val pending = db.commissionEntryDao().pendingForPeriod(statement.month)
        var settled = 0
        for (entry in pending) {
            val outstanding = Ledger.outstanding(entry.expectedPaise, entry.receivedPaise)
            if (outstanding <= 0) continue
            addReceipt(
                entryId = entry.id,
                amount = Money.fromPaise(outstanding),
                date = statement.date,
                mode = statement.mode,
                reference = statement.reference,
                note = "",
            )
            settled++
        }
        settled
    }

    companion object {
        const val CUSTOM_CODE_PREFIX = "CUSTOM_"
    }
}
