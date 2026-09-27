package app.agentsetu.core.reminders

import app.agentsetu.core.model.PaymentFrequency
import app.agentsetu.core.model.ReminderType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {

    private fun d(s: String) = LocalDate.parse(s)

    private fun holding(
        id: String = "h",
        insurance: Boolean = true,
        rd: Boolean = false,
        frequency: PaymentFrequency = PaymentFrequency.MONTHLY,
        start: String = "2026-01-31",
        maturity: String? = null,
        term: Int? = null,
        active: Boolean = true,
    ) = PlannerHolding(id, insurance, rd, frequency, d(start), maturity?.let(::d), term, active)

    @Test
    fun monthlyDatesDoNotDriftAtMonthEnd() {
        val dates = ReminderPlanner.dueDates(holding(), d("2026-01-01"), d("2026-05-31"))
        assertEquals(listOf("2026-02-28", "2026-03-31", "2026-04-30", "2026-05-31").map(::d), dates)
    }

    @Test
    fun firstPaymentIsNotRemindedAgain() {
        val dates = ReminderPlanner.dueDates(holding(start = "2026-09-01"), d("2026-08-01"), d("2026-09-30"))
        assertEquals(emptyList<LocalDate>(), dates)
    }

    @Test
    fun stopsAtEndOfPremiumTermAndBeforeMaturity() {
        val yearly = holding(frequency = PaymentFrequency.YEARLY, start = "2020-06-01", term = 3)
        assertEquals(listOf("2021-06-01", "2022-06-01").map(::d), ReminderPlanner.dueDates(yearly, d("2020-01-01"), d("2030-01-01")))

        val rd = holding(insurance = false, rd = true, start = "2026-01-10", maturity = "2026-04-10")
        assertEquals(listOf("2026-02-10", "2026-03-10").map(::d), ReminderPlanner.dueDates(rd, d("2026-01-01"), d("2026-12-31")))
    }

    @Test
    fun oneTimeDepositsHaveNoDues() {
        val td = holding(insurance = false, frequency = PaymentFrequency.ONE_TIME, start = "2026-01-01")
        assertEquals(emptyList<LocalDate>(), ReminderPlanner.dueDates(td, d("2026-01-01"), d("2027-01-01")))
    }

    @Test
    fun planWindows() {
        val today = d("2026-09-28")
        val plan = ReminderPlanner.plan(
            holdings = listOf(
                holding(id = "pli", frequency = PaymentFrequency.QUARTERLY, start = "2026-07-02"), // due 02-10
                holding(id = "far", frequency = PaymentFrequency.QUARTERLY, start = "2026-07-20"), // due 20-10: too far
                holding(id = "td", insurance = false, frequency = PaymentFrequency.ONE_TIME, start = "2021-10-20", maturity = "2026-10-20"),
                holding(id = "closed", active = false, start = "2026-08-28"),
            ),
            leads = listOf(
                PlannerLead("soon", d("2026-10-01"), open = true),
                PlannerLead("later", d("2026-11-01"), open = true),
                PlannerLead("lost", d("2026-09-29"), open = false),
            ),
            today = today,
        )
        assertEquals(
            setOf(
                PlannedReminder(ReminderType.PREMIUM_DUE, "pli", d("2026-10-02")),
                PlannedReminder(ReminderType.MATURITY, "td", d("2026-10-20")),
                PlannedReminder(ReminderType.FOLLOW_UP, "soon", d("2026-10-01")),
            ),
            plan.toSet(),
        )
    }

    @Test
    fun missedDuesWithinLookbackAreKept() {
        val plan = ReminderPlanner.plan(listOf(holding(start = "2026-07-15")), emptyList(), d("2026-09-28"))
        assertEquals(listOf(d("2026-09-15")), plan.map { it.dueDate })
    }

    @Test
    fun alertsOnlyWhenDueOrOnMaturityMilestones() {
        val today = d("2026-09-28")
        val open = listOf(
            PlannedReminder(ReminderType.PREMIUM_DUE, "a", d("2026-09-28")),
            PlannedReminder(ReminderType.PREMIUM_DUE, "b", d("2026-09-30")),
            PlannedReminder(ReminderType.FOLLOW_UP, "c", d("2026-09-20")),
            PlannedReminder(ReminderType.MATURITY, "d", d("2026-10-28")), // 30 days
            PlannedReminder(ReminderType.MATURITY, "e", d("2026-10-10")), // 12 days
            PlannedReminder(ReminderType.MATURITY, "f", d("2026-10-05")), // 7 days
        )
        val alerts = ReminderPlanner.alertsToday(open, today).map { it.subjectId }
        assertEquals(listOf("c", "a", "f", "d"), alerts)
        assertTrue("b" !in alerts && "e" !in alerts)
    }
}
