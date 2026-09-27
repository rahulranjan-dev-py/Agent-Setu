package app.agentsetu.data.db

import app.agentsetu.core.model.ReminderType
import java.time.LocalDate

/** Read-only view for the Today screen and the daily notification (see ReminderDao). */
data class ReminderRow(
    val id: String,
    val type: ReminderType,
    val subjectId: String,
    val dueDate: LocalDate,
    val customerId: String,
    val customerName: String,
    val mobile: String?,
    val productNameEn: String?,
    val productNameHi: String?,
    val instalmentPaise: Long?,
    val amountPaise: Long?,
    val note: String?,
)
