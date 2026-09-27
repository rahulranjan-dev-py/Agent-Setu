package app.agentsetu.core.reminders

import app.agentsetu.core.model.PaymentFrequency
import app.agentsetu.core.model.ReminderType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class PlannerHolding(
    val id: String,
    val isInsurance: Boolean,
    /** RD and other instalment schemes; one-time deposits have no dues. */
    val isRecurringDeposit: Boolean,
    val frequency: PaymentFrequency,
    val startDate: LocalDate,
    val maturityDate: LocalDate?,
    val premiumTermYears: Int?,
    val active: Boolean,
)

data class PlannerLead(val id: String, val nextFollowUp: LocalDate?, val open: Boolean)

data class PlannedReminder(val type: ReminderType, val subjectId: String, val dueDate: LocalDate)

/**
 * Decides which reminders should exist. Runs nightly; the reminder table's unique index
 * (type, subject, due date) makes repeated runs harmless, and a reminder the user marked done is
 * never recreated.
 */
object ReminderPlanner {
    /** Missed while the phone was off: still worth a reminder. */
    const val LOOKBACK_DAYS = 30L

    /** "This week's collections" and follow-ups. */
    const val DUE_AHEAD_DAYS = 7L

    /** Maturity radar. */
    const val MATURITY_AHEAD_DAYS = 30L

    fun plan(holdings: List<PlannerHolding>, leads: List<PlannerLead>, today: LocalDate): List<PlannedReminder> {
        val from = today.minusDays(LOOKBACK_DAYS)
        val result = mutableListOf<PlannedReminder>()
        for (h in holdings.filter { it.active }) {
            val type = when {
                h.isInsurance -> ReminderType.PREMIUM_DUE
                h.isRecurringDeposit -> ReminderType.INSTALMENT_DUE
                else -> null
            }
            if (type != null) {
                dueDates(h, from, today.plusDays(DUE_AHEAD_DAYS)).forEach { result += PlannedReminder(type, h.id, it) }
            }
            h.maturityDate?.let { m ->
                if (!m.isBefore(from) && !m.isAfter(today.plusDays(MATURITY_AHEAD_DAYS))) {
                    result += PlannedReminder(ReminderType.MATURITY, h.id, m)
                }
            }
        }
        for (lead in leads.filter { it.open }) {
            val next = lead.nextFollowUp ?: continue
            if (!next.isBefore(from) && !next.isAfter(today.plusDays(DUE_AHEAD_DAYS))) {
                result += PlannedReminder(ReminderType.FOLLOW_UP, lead.id, next)
            }
        }
        return result
    }

    /**
     * Premium/instalment dates after the first payment (which is recorded when the business is
     * added), within [from, to]. Stops at the end of the premium-paying term and before maturity.
     * Dates are computed from the start each time, so a 31 January start gives 28/29 February and
     * then 31 March again, never drifting.
     */
    fun dueDates(h: PlannerHolding, from: LocalDate, to: LocalDate): List<LocalDate> {
        val step = h.frequency.monthsBetween ?: return emptyList()
        val ends = listOfNotNull(
            h.premiumTermYears?.let { h.startDate.plusYears(it.toLong()) },
            h.maturityDate,
        )
        val end = ends.minOrNull()
        val dates = mutableListOf<LocalDate>()
        var k = 1L
        while (true) {
            val due = h.startDate.plusMonths(k * step)
            if (due.isAfter(to) || (end != null && !due.isBefore(end))) break
            if (!due.isBefore(from)) dates += due
            k++
        }
        return dates
    }

    /** Which open reminders are worth a notification today. */
    fun alertsToday(open: List<PlannedReminder>, today: LocalDate): List<PlannedReminder> = open.filter { r ->
        val days = ChronoUnit.DAYS.between(today, r.dueDate)
        when (r.type) {
            ReminderType.MATURITY -> days in MATURITY_ALERT_DAYS || days <= 0
            else -> days <= 0
        }
    }.sortedBy { it.dueDate }

    /** Maturity alerts at 30, 15, 7 and 1 day(s) before, then daily once due. */
    val MATURITY_ALERT_DAYS = setOf(30L, 15L, 7L, 1L)
}
