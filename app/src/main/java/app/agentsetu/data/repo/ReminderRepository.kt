package app.agentsetu.data.repo

import androidx.room.withTransaction
import app.agentsetu.core.commission.CommissionQuery
import app.agentsetu.core.ledger.PolicyDates
import app.agentsetu.core.model.HoldingStatus
import app.agentsetu.core.model.LeadStage
import app.agentsetu.core.model.Money
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.ProductGroup
import app.agentsetu.core.reminders.PlannedReminder
import app.agentsetu.core.reminders.PlannerHolding
import app.agentsetu.core.reminders.PlannerLead
import app.agentsetu.core.reminders.ReminderPlanner
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.LeadEntity
import app.agentsetu.data.db.ReminderEntity
import app.agentsetu.data.db.ReminderRow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

enum class MaturityOutcome { REINVESTED, WITHDRAWN, UNDECIDED }

@Singleton
class ReminderRepository @Inject constructor(
    private val db: AgentSetuDatabase,
    private val commission: CommissionRepository,
) {
    /** Horizon of the Today screen: the maturity radar looks 30 days ahead. */
    fun horizon(today: LocalDate): LocalDate = today.plusDays(ReminderPlanner.MATURITY_AHEAD_DAYS)

    /** Creates any reminders that should now exist. Safe to call as often as needed. */
    suspend fun regenerate(today: LocalDate = LocalDate.now()) {
        val products = db.productDao().all().associateBy { it.id }
        val holdings = db.holdingDao().active().mapNotNull { h ->
            val product = products[h.productId] ?: return@mapNotNull null
            PlannerHolding(
                id = h.id,
                isInsurance = product.productGroup == ProductGroup.PLI || product.productGroup == ProductGroup.RPLI,
                isRecurringDeposit = product.code.startsWith("RD"),
                frequency = h.frequency,
                startDate = h.startDate,
                maturityDate = h.maturityDate,
                premiumTermYears = h.premiumTermYears,
                active = h.status == HoldingStatus.ACTIVE,
            )
        }
        val leads = db.leadDao().open().map { PlannerLead(it.id, it.nextFollowUp, open = true) }
        val now = System.currentTimeMillis()
        val planned = ReminderPlanner.plan(holdings, leads, today).map {
            ReminderEntity(type = it.type, subjectId = it.subjectId, dueDate = it.dueDate, createdAt = now, updatedAt = now)
        }
        db.reminderDao().insertIgnore(planned)
    }

    /** Reminders worth a notification today, with names for the notification text. */
    suspend fun alertsToday(today: LocalDate = LocalDate.now()): List<ReminderRow> {
        val open = db.reminderDao().open(horizon(today))
        val alertIds = ReminderPlanner
            .alertsToday(open.map { PlannedReminder(it.type, it.id, it.dueDate) }, today)
            .map { it.subjectId }
            .toSet()
        return open.filter { it.id in alertIds }
    }

    suspend fun markDone(reminderId: String) {
        val reminder = db.reminderDao().get(reminderId) ?: return
        db.reminderDao().upsert(reminder.copy(done = true, updatedAt = System.currentTimeMillis()))
    }

    /**
     * Premium or RD instalment collected: adds the expected commission for that period to the
     * ledger (with the rule and rate in force on the due date), then closes the reminder.
     */
    suspend fun markCollected(reminderId: String) = db.withTransaction {
        val reminder = db.reminderDao().get(reminderId) ?: return@withTransaction
        val holding = db.holdingDao().get(reminder.subjectId)
        val product = holding?.let { db.productDao().get(it.productId) }
        val staffType = db.userProfileDao().current()?.staffType
        val base = holding?.instalmentPaise
        if (holding != null && product != null && staffType != null && base != null) {
            val insurance = product.productGroup == ProductGroup.PLI || product.productGroup == ProductGroup.RPLI
            val query = CommissionQuery(
                productCode = product.code,
                policyCategory = if (product.productGroup == ProductGroup.PLI) holding.policyCategory else PolicyCategory.ANY,
                staffType = staffType,
                premiumTermYears = holding.premiumTermYears,
                policyYear = if (insurance) PolicyDates.policyYear(holding.startDate, reminder.dueDate) else null,
                date = reminder.dueDate,
            )
            commission.recordExpected(holding.id, YearMonth.from(reminder.dueDate), query, Money.fromPaise(base))
        }
        markDone(reminderId)
    }

    /** Follow-up done. With [next], the lead gets a new follow-up date; without, it has none. */
    suspend fun followUpDone(reminderId: String, next: LocalDate?) {
        val reminder = db.reminderDao().get(reminderId) ?: return
        db.leadDao().get(reminder.subjectId)?.let {
            db.leadDao().upsert(it.copy(nextFollowUp = next, updatedAt = System.currentTimeMillis()))
        }
        markDone(reminderId)
        if (next != null) regenerate()
    }

    /** Maturity handled. Reinvested/withdrawn closes the holding; undecided asks again in 7 days. */
    suspend fun maturityHandled(reminderId: String, outcome: MaturityOutcome) {
        val reminder = db.reminderDao().get(reminderId) ?: return
        val holding = db.holdingDao().get(reminder.subjectId) ?: return
        val now = System.currentTimeMillis()
        when (outcome) {
            MaturityOutcome.REINVESTED, MaturityOutcome.WITHDRAWN ->
                db.holdingDao().upsert(holding.copy(status = HoldingStatus.MATURED, updatedAt = now))
            MaturityOutcome.UNDECIDED ->
                addFollowUp(holding.customerId, LocalDate.now().plusDays(7), note = "", productId = holding.productId)
        }
        markDone(reminderId)
    }

    suspend fun addFollowUp(customerId: String, date: LocalDate, note: String, productId: String = "") {
        val now = System.currentTimeMillis()
        db.leadDao().upsert(
            LeadEntity(
                customerId = customerId,
                productId = productId,
                stage = LeadStage.CONTACTED,
                nextFollowUp = date,
                source = note.trim(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        regenerate()
    }

}
