package app.agentsetu.data.db

import app.agentsetu.core.model.CommissionStatus
import java.time.LocalDate

/** Read-only view for the Commission screen (see CommissionEntryDao.observeLedger). */
data class LedgerRow(
    val id: String,
    val period: String,
    val policyYear: Int?,
    val status: CommissionStatus,
    val baseAmountPaise: Long,
    val expectedPaise: Long?,
    val receivedPaise: Long?,
    val receivedDate: LocalDate?,
    val rateApplied: String?,
    val customerName: String,
    val productCode: String,
    val productNameEn: String,
    val productNameHi: String,
)
