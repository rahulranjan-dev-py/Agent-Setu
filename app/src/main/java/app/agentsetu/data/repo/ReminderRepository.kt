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

/** What "Undo" has to put back after a reminder action. */
sealed interface UndoAction {
    val reminderId: String

    data class Dismissed(override val reminderId: String) : UndoAction
    data class Collected(override val reminderId: String, val createdEntryId: String?) : UndoAction
    data class FollowUp(override val reminderId: String, val leadId: String, val previousNext: LocalDate?) : UndoAction
    data class Maturity(
        override val reminderId: String,
        val holdingId: String,
        val previousStatus: HoldingStatus,
        val createdLeadId: String?,
    ) : UndoAction
}

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

    suspend fun markDone(reminderId: String): UndoAction {
        setDone(reminderId, true)
        return UndoAction.Dismissed(reminderId)
    }

    private suspend fun setDone(reminderId: String, done: Boolean) {
        val reminder = db.reminderDao().get(reminderId) ?: return
        db.reminderDao().upsert(reminder.copy(done = done, updatedAt = System.currentTimeMillis()))
    }

    /** Reverses one action taken from the Today screen. */
    suspend fun undo(action: UndoAction) = db.withTransaction {
        val now = System.currentTimeMillis()
        when (action) {
            is UndoAction.Dismissed -> Unit
            is UndoAction.Collected -> action.createdEntryId?.let { id ->
                db.commissionEntryDao().get(id)?.let { db.commissionEntryDao().upsert(it.copy(deleted = true, updatedAt = now)) }
            }
            is UndoAction.FollowUp -> db.leadDao().get(action.leadId)?.let {
                db.leadDao().upsert(it.copy(nextFollowUp = action.previousNext, updatedAt = now))
            }
            is UndoAction.Maturity -> {
                db.holdingDao().get(action.holdingId)?.let {
                    db.holdingDao().upsert(it.copy(status = action.previousStatus, updatedAt = now))
                }
                action.createdLeadId?.let { leadId ->
                    db.leadDao().get(leadId)?.let { db.leadDao().upsert(it.copy(deleted = true, updatedAt = now)) }
                    db.reminderDao().softDeleteForSubject(leadId, now)
                }
            }
        }
        setDone(action.reminderId, false)
    }

    /**
     * Premium or RD instalment collected: adds the expected commission for that period to the
     * ledger (with the rule and rate in force on the due date), then closes the reminder.
     */
    suspend fun markCollected(reminderId: String): UndoAction = db.withTransaction {
        val reminder = db.reminderDao().get(reminderId) ?: return@withTransaction UndoAction.Dismissed(reminderId)
        val holding = db.holdingDao().get(reminder.subjectId)
        val product = holding?.let { db.productDao().get(it.productId) }
        val staffType = db.userProfileDao().current()?.staffType
        val base = holding?.instalmentPaise
        var createdEntryId: String? = null
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
            val period = YearMonth.from(reminder.dueDate)
            val existed = db.commissionEntryDao().find(holding.id, period.toString()) != null
            val entry = commission.recordExpected(holding.id, period, query, Money.fromPaise(base))
            if (!existed) createdEntryId = entry.id
        }
        setDone(reminderId, true)
        UndoAction.Collected(reminderId, createdEntryId)
    }

    /** Follow-up done. With [next], the lead gets a new follow-up date; without, it has none. */
    suspend fun followUpDone(reminderId: String, next: LocalDate?): UndoAction {
        val reminder = db.reminderDao().get(reminderId) ?: return UndoAction.Dismissed(reminderId)
        val lead = db.leadDao().get(reminder.subjectId)
        lead?.let { db.leadDao().upsert(it.copy(nextFollowUp = next, updatedAt = System.currentTimeMillis())) }
        setDone(reminderId, true)
        if (next != null) regenerate()
        return if (lead != null) UndoAction.FollowUp(reminderId, lead.id, lead.nextFollowUp) else UndoAction.Dismissed(reminderId)
    }

    /** Maturity handled. Reinvested/withdrawn closes the holding; undecided asks again in 7 days. */
    suspend fun maturityHandled(reminderId: String, outcome: MaturityOutcome): UndoAction {
        val reminder = db.reminderDao().get(reminderId) ?: return UndoAction.Dismissed(reminderId)
        val holding = db.holdingDao().get(reminder.subjectId) ?: return markDone(reminderId)
        val now = System.currentTimeMillis()
        var createdLeadId: String? = null
        when (outcome) {
            MaturityOutcome.REINVESTED, MaturityOutcome.WITHDRAWN ->
                db.holdingDao().upsert(holding.copy(status = HoldingStatus.MATURED, updatedAt = now))
            MaturityOutcome.UNDECIDED ->
                createdLeadId = addFollowUp(holding.customerId, LocalDate.now().plusDays(7), note = "", productId = holding.productId)
        }
        setDone(reminderId, true)
        return UndoAction.Maturity(reminderId, holding.id, holding.status, createdLeadId)
    }

    /** Returns the new lead's id. */
    suspend fun addFollowUp(customerId: String, date: LocalDate, note: String, productId: String = ""): String {
        val now = System.currentTimeMillis()
        val lead = LeadEntity(
            customerId = customerId,
            productId = productId,
            stage = LeadStage.CONTACTED,
            nextFollowUp = date,
            source = note.trim(),
            createdAt = now,
            updatedAt = now,
        )
        db.leadDao().upsert(lead)
        regenerate()
        return lead.id
    }

    /** Removes a policy/account (soft delete) and its reminders; ledger entries stay. */
    suspend fun deleteHolding(holdingId: String) = db.withTransaction {
        val now = System.currentTimeMillis()
        db.holdingDao().get(holdingId)?.let { db.holdingDao().upsert(it.copy(deleted = true, updatedAt = now)) }
        db.reminderDao().softDeleteForSubject(holdingId, now)
    }

}
