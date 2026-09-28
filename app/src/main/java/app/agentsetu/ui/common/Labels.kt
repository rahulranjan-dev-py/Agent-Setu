package app.agentsetu.ui.common

import androidx.annotation.StringRes
import app.agentsetu.R
import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.CommissionStatus
import app.agentsetu.core.model.ProductGroup
import app.agentsetu.core.model.ReceiptMode
import app.agentsetu.core.model.PaymentFrequency
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.StaffType

@StringRes
fun StaffType.labelRes(): Int = when (this) {
    StaffType.DEPARTMENTAL -> R.string.staff_DEPARTMENTAL
    StaffType.GDS -> R.string.staff_GDS
    StaffType.GDS_BPM -> R.string.staff_GDS_BPM
    StaffType.DIRECT_AGENT -> R.string.staff_DIRECT_AGENT
    StaffType.FIELD_OFFICER -> R.string.staff_FIELD_OFFICER
    StaffType.ODE -> R.string.staff_ODE
    StaffType.SAS_AGENT -> R.string.staff_SAS_AGENT
    StaffType.MPKBY_AGENT -> R.string.staff_MPKBY_AGENT
}

@StringRes
fun CommissionStatus.labelRes(): Int = when (this) {
    CommissionStatus.EXPECTED -> R.string.status_EXPECTED
    CommissionStatus.PARTLY_RECEIVED -> R.string.status_PARTLY_RECEIVED
    CommissionStatus.RECEIVED -> R.string.status_RECEIVED
    CommissionStatus.NO_RULE -> R.string.status_NO_RULE
}

@StringRes
fun PaymentFrequency.labelRes(): Int = when (this) {
    PaymentFrequency.MONTHLY -> R.string.freq_MONTHLY
    PaymentFrequency.QUARTERLY -> R.string.freq_QUARTERLY
    PaymentFrequency.HALF_YEARLY -> R.string.freq_HALF_YEARLY
    PaymentFrequency.YEARLY -> R.string.freq_YEARLY
    PaymentFrequency.ONE_TIME -> R.string.freq_ONE_TIME
}

@StringRes
fun PolicyCategory.labelRes(): Int? = when (this) {
    PolicyCategory.AEA -> R.string.category_AEA
    PolicyCategory.NON_AEA -> R.string.category_NON_AEA
    PolicyCategory.ANY -> null
}

@StringRes
fun ReceiptMode.labelRes(): Int = when (this) {
    ReceiptMode.CASH -> R.string.mode_CASH
    ReceiptMode.POSB -> R.string.mode_POSB
    ReceiptMode.BANK -> R.string.mode_BANK
    ReceiptMode.OTHER -> R.string.mode_OTHER
}

@StringRes
fun ProductGroup.labelRes(): Int = when (this) {
    ProductGroup.PLI -> R.string.group_PLI
    ProductGroup.RPLI -> R.string.group_RPLI
    ProductGroup.SAVINGS -> R.string.group_SAVINGS
    ProductGroup.IPPB -> R.string.group_IPPB
    ProductGroup.OTHER -> R.string.group_OTHER
}

@StringRes
fun CommissionBasis.labelRes(): Int = when (this) {
    CommissionBasis.PERCENT_OF_PREMIUM -> R.string.basis_PERCENT_OF_PREMIUM
    CommissionBasis.PERCENT_OF_DEPOSIT -> R.string.basis_PERCENT_OF_DEPOSIT
    CommissionBasis.PERCENT_OF_NET_ACCRETION -> R.string.basis_PERCENT_OF_NET_ACCRETION
    CommissionBasis.FLAT_PER_CASE -> R.string.basis_FLAT_PER_CASE
}
