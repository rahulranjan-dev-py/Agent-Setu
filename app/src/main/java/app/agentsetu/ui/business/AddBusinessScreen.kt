package app.agentsetu.ui.business

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import app.agentsetu.R
import app.agentsetu.core.catalogue.DefaultProducts
import app.agentsetu.core.commission.CommissionQuery
import app.agentsetu.core.commission.CommissionResult
import app.agentsetu.core.format.IndianFormat
import app.agentsetu.core.input.AmountInput
import app.agentsetu.core.ledger.PolicyDates
import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.Money
import app.agentsetu.core.model.PaymentFrequency
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.ProductGroup
import app.agentsetu.core.model.RefNumber
import app.agentsetu.core.model.StaffType
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.HoldingEntity
import app.agentsetu.data.db.ProductEntity
import app.agentsetu.data.repo.CommissionRepository
import app.agentsetu.data.repo.ReminderRepository
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.DateField
import app.agentsetu.ui.common.editableAmount
import app.agentsetu.ui.common.FormField
import app.agentsetu.ui.common.RadioGroup
import app.agentsetu.ui.common.SectionTitle
import app.agentsetu.ui.common.labelRes
import app.agentsetu.ui.common.localized
import app.agentsetu.ui.common.rupees
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Preview {
    data object NeedInput : Preview
    data class Expected(val amountPaise: Long, val rate: BigDecimal, val flat: Boolean, val sample: Boolean) : Preview
    data object NoRule : Preview
    data object Ambiguous : Preview
}

@HiltViewModel
class AddBusinessViewModel @Inject constructor(
    private val db: AgentSetuDatabase,
    private val commission: CommissionRepository,
    private val reminders: ReminderRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /** Editing an existing policy/account when set; otherwise adding one for [customerId]. */
    private val holdingId: String? = savedStateHandle["holdingId"]
    private var customerId: String? = savedStateHandle["customerId"]
    val isEditing get() = holdingId != null

    val products = db.productDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var product by mutableStateOf<ProductEntity?>(null)
        private set
    var category by mutableStateOf(PolicyCategory.NON_AEA)
        private set
    var termYears by mutableStateOf("")
        private set
    var sumAssured by mutableStateOf("")
        private set
    var amount by mutableStateOf("")
        private set
    var frequency by mutableStateOf(PaymentFrequency.MONTHLY)
        private set
    var startDate by mutableStateOf(IndianFormat.date(LocalDate.now()))
        private set
    var maturityDate by mutableStateOf("")
        private set
    /** Full policy/account number as typed (optional); the stored refLast4 is derived from it. */
    var refNumber by mutableStateOf("")
        private set
    var preview by mutableStateOf<Preview>(Preview.NeedInput)
        private set
    var showErrors by mutableStateOf(false)
        private set

    private var maturityEdited = false
    private var staffType: StaffType? = null
    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            staffType = db.userProfileDao().observe().first()?.staffType
            holdingId?.let { loadForEdit(it) }
            refreshPreview()
        }
    }

    /** Last-4 of a policy recorded before 1.1.1, kept as is unless a full number is typed. */
    var legacyLast4: String? = null
        private set

    private suspend fun loadForEdit(id: String) {
        val h = db.holdingDao().get(id) ?: return
        customerId = h.customerId
        val p = db.productDao().get(h.productId) ?: return
        product = p
        category = h.policyCategory.takeIf { it != PolicyCategory.ANY } ?: PolicyCategory.NON_AEA
        termYears = h.premiumTermYears?.toString().orEmpty()
        frequency = h.frequency
        startDate = IndianFormat.date(h.startDate)
        maturityDate = h.maturityDate?.let(IndianFormat::date).orEmpty()
        maturityEdited = true
        refNumber = h.refNumber.orEmpty()
        legacyLast4 = if (h.refNumber == null) h.refLast4 else null
        val insurance = p.productGroup == ProductGroup.PLI || p.productGroup == ProductGroup.RPLI
        sumAssured = if (insurance) editableAmount(h.amountPaise) else ""
        amount = editableAmount(h.instalmentPaise ?: h.amountPaise)
    }

    fun delete(onDeleted: () -> Unit) {
        val id = holdingId ?: return
        viewModelScope.launch {
            reminders.deleteHolding(id)
            onDeleted()
        }
    }

    val isInsurance get() = product?.let { it.productGroup == ProductGroup.PLI || it.productGroup == ProductGroup.RPLI } == true
    val isPli get() = product?.productGroup == ProductGroup.PLI
    val isRecurring get() = product?.code?.startsWith("RD") == true

    // Field validity (shown after the first Save attempt).
    val productMissing get() = product == null
    val amountInvalid get() = AmountInput.parse(amount)?.takeIf { it.signum() > 0 } == null
    val sumAssuredInvalid get() = isInsurance && AmountInput.parse(sumAssured)?.takeIf { it.signum() > 0 } == null
    val termInvalid get() = isInsurance && termYears.toIntOrNull()?.takeIf { it in 1..60 } == null
    val startInvalid get() = IndianFormat.parseDate(startDate) == null
    val maturityInvalid get() = maturityDate.isNotBlank() && (
        IndianFormat.parseDate(maturityDate) == null ||
            IndianFormat.parseDate(startDate)?.let { start -> IndianFormat.parseDate(maturityDate)!!.isBefore(start) } == true
        )
    val refInvalid get() = !RefNumber.isValid(refNumber.trim().ifBlank { null })

    fun selectProduct(p: ProductEntity) {
        product = p
        frequency = when {
            p.productGroup == ProductGroup.PLI || p.productGroup == ProductGroup.RPLI -> PaymentFrequency.MONTHLY
            p.code.startsWith("RD") -> PaymentFrequency.MONTHLY
            else -> PaymentFrequency.ONE_TIME
        }
        suggestMaturity()
        refreshPreview()
    }

    fun updateCategory(value: PolicyCategory) { category = value; refreshPreview() }
    fun updateTerm(value: String) { termYears = value.filter { it.isDigit() }.take(2); refreshPreview() }
    fun updateSumAssured(value: String) { sumAssured = value }
    fun updateAmount(value: String) { amount = value; refreshPreview() }
    fun updateFrequency(value: PaymentFrequency) { frequency = value }
    fun updateStart(value: String) { startDate = value; suggestMaturity(); refreshPreview() }
    fun updateMaturity(value: String) { maturityDate = value; maturityEdited = true }
    fun updateRef(value: String) { refNumber = value.take(RefNumber.MAX_LENGTH) }

    private fun suggestMaturity() {
        if (maturityEdited) return
        val code = product?.code ?: return
        val start = IndianFormat.parseDate(startDate) ?: return
        // A custom product has no usual term: clear any suggestion left by the previous product.
        val catalogue = DefaultProducts.byCode(code) ?: run { maturityDate = ""; return }
        maturityDate = PolicyDates.suggestedMaturity(catalogue, start)?.let(IndianFormat::date).orEmpty()
    }

    private fun query(): CommissionQuery? {
        val p = product ?: return null
        val staff = staffType ?: return null
        val start = IndianFormat.parseDate(startDate) ?: return null
        return CommissionQuery(
            productCode = p.code,
            policyCategory = if (p.productGroup == ProductGroup.PLI) category else PolicyCategory.ANY,
            staffType = staff,
            premiumTermYears = if (isInsurance) termYears.toIntOrNull() else null,
            policyYear = if (isInsurance) 1 else null,
            date = start,
        )
    }

    private fun refreshPreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val q = query()
            val base = AmountInput.parse(amount)
            if (q == null || base == null) {
                preview = Preview.NeedInput
                return@launch
            }
            preview = when (val result = commission.preview(q, base)) {
                is CommissionResult.Expected -> Preview.Expected(
                    amountPaise = Money.toPaise(result.amount),
                    rate = result.rule.rate,
                    flat = result.rule.basis == CommissionBasis.FLAT_PER_CASE,
                    sample = db.commissionRuleDao().get(result.rule.id)?.let { it.isSample || !it.verified } ?: true,
                )
                CommissionResult.NoRule -> Preview.NoRule
                is CommissionResult.Ambiguous -> Preview.Ambiguous
            }
        }
    }

    fun save(onSaved: () -> Unit) {
        showErrors = true
        if (productMissing || amountInvalid || sumAssuredInvalid || termInvalid || startInvalid || maturityInvalid || refInvalid) return
        val p = product ?: return
        val base = AmountInput.parse(amount) ?: return
        val start = IndianFormat.parseDate(startDate) ?: return
        val owner = customerId ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val basePaise = Money.toPaise(base)
            val existing = holdingId?.let { db.holdingDao().get(it) }
            if (existing != null) {
                // Editing: change the policy/account itself; ledger entries already recorded stay as they are.
                db.holdingDao().upsert(
                    existing.copy(
                        productId = p.id,
                        refNumber = refNumber.trim().ifBlank { null },
                        refLast4 = RefNumber.last4(refNumber) ?: legacyLast4.takeIf { refNumber.isBlank() },
                        policyCategory = if (p.productGroup == ProductGroup.PLI) category else PolicyCategory.ANY,
                        premiumTermYears = if (isInsurance) termYears.toIntOrNull() else null,
                        amountPaise = if (isInsurance) Money.toPaise(AmountInput.parse(sumAssured) ?: BigDecimal.ZERO) else basePaise,
                        instalmentPaise = if (isInsurance || isRecurring) basePaise else null,
                        frequency = frequency,
                        startDate = start,
                        maturityDate = IndianFormat.parseDate(maturityDate),
                        updatedAt = now,
                    ),
                )
                reminders.holdingEdited(existing.id)
                onSaved()
                return@launch
            }
            val holding = HoldingEntity(
                customerId = owner,
                productId = p.id,
                refNumber = refNumber.trim().ifBlank { null },
                refLast4 = RefNumber.last4(refNumber),
                policyCategory = if (p.productGroup == ProductGroup.PLI) category else PolicyCategory.ANY,
                premiumTermYears = if (isInsurance) termYears.toIntOrNull() else null,
                amountPaise = if (isInsurance) Money.toPaise(AmountInput.parse(sumAssured) ?: BigDecimal.ZERO) else basePaise,
                instalmentPaise = if (isInsurance || isRecurring) basePaise else null,
                frequency = frequency,
                startDate = start,
                maturityDate = IndianFormat.parseDate(maturityDate),
                createdAt = now,
                updatedAt = now,
            )
            db.withTransaction {
                db.holdingDao().upsert(holding)
                // First premium / deposit goes straight into the ledger; later ones come with reminders.
                query()?.let { commission.recordExpected(holding.id, YearMonth.from(start), it, base) }
            }
            reminders.regenerate()
            onSaved()
        }
    }
}

@Composable
fun AddBusinessScreen(onBack: () -> Unit, onSaved: () -> Unit, viewModel: AddBusinessViewModel = hiltViewModel()) {
    val vm = viewModel
    val products by vm.products.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val err = vm.showErrors

    AppScaffold(
        title = stringResource(if (vm.isEditing) R.string.holding_edit_title else R.string.business_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(stringResource(R.string.business_product))
            OutlinedButton(
                onClick = { picking = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(vm.product?.let { localized(it.nameEn, it.nameHi) } ?: stringResource(R.string.business_choose_product))
            }
            if (err && vm.productMissing) Text(stringResource(R.string.error_required), color = MaterialTheme.colorScheme.error)

            if (vm.isPli) {
                SectionTitle(stringResource(R.string.business_policy_category))
                RadioGroup(
                    options = listOf(PolicyCategory.NON_AEA, PolicyCategory.AEA),
                    selected = vm.category,
                    label = { stringResource(it.labelRes()!!) },
                    onSelect = vm::updateCategory,
                )
            }
            if (vm.isInsurance) {
                FormField(
                    vm.termYears, vm::updateTerm, stringResource(R.string.business_premium_term),
                    error = if (err && vm.termInvalid) stringResource(R.string.error_whole_years) else null,
                    keyboardType = KeyboardType.Number,
                )
                FormField(
                    vm.sumAssured, vm::updateSumAssured, stringResource(R.string.business_sum_assured),
                    error = if (err && vm.sumAssuredInvalid) stringResource(R.string.error_amount) else null,
                    keyboardType = KeyboardType.Decimal,
                )
            }
            val amountLabel = when {
                vm.isInsurance -> R.string.business_premium
                vm.isRecurring -> R.string.business_instalment
                else -> R.string.business_deposit
            }
            FormField(
                vm.amount, vm::updateAmount, stringResource(amountLabel),
                error = if (err && vm.amountInvalid) stringResource(R.string.error_amount) else null,
                keyboardType = KeyboardType.Decimal,
            )
            if (vm.isInsurance) {
                SectionTitle(stringResource(R.string.business_frequency))
                RadioGroup(
                    options = listOf(PaymentFrequency.MONTHLY, PaymentFrequency.QUARTERLY, PaymentFrequency.HALF_YEARLY, PaymentFrequency.YEARLY),
                    selected = vm.frequency,
                    label = { stringResource(it.labelRes()) },
                    onSelect = vm::updateFrequency,
                )
            }
            DateField(
                vm.startDate, vm::updateStart, stringResource(R.string.business_start_date),
                error = if (err && vm.startInvalid) stringResource(R.string.error_date) else null,
            )
            DateField(
                vm.maturityDate, vm::updateMaturity, stringResource(R.string.business_maturity_date),
                error = if (err && vm.maturityInvalid) stringResource(R.string.error_date) else null,
            )
            FormField(
                vm.refNumber, vm::updateRef, stringResource(R.string.business_ref_number),
                error = if (err && vm.refInvalid) stringResource(R.string.business_ref_error) else null,
                supporting = vm.legacyLast4?.takeIf { vm.refNumber.isBlank() }
                    ?.let { stringResource(R.string.business_ref_legacy, it) }
                    ?: stringResource(R.string.business_ref_help),
            )

            if (!vm.isEditing) PreviewCard(vm.preview)

            Button(
                onClick = { vm.save(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
            if (vm.isEditing) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.holding_delete)) }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.holding_delete)) },
            text = { Text(stringResource(R.string.holding_delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        vm.delete(onSaved)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.holding_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.business_choose_product)) },
            text = {
                LazyColumn {
                    items(products, key = { it.id }) { p ->
                        ListItem(
                            headlineContent = { Text(localized(p.nameEn, p.nameHi)) },
                            modifier = Modifier.clickable {
                                vm.selectProduct(p)
                                picking = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun PreviewCard(preview: Preview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.business_expected), style = MaterialTheme.typography.titleMedium)
            when (preview) {
                Preview.NeedInput -> Text(stringResource(R.string.business_fill_in), style = MaterialTheme.typography.bodyMedium)
                is Preview.Expected -> {
                    val text = if (preview.flat) {
                        rupees(preview.amountPaise)
                    } else {
                        stringResource(R.string.business_expected_value, rupees(preview.amountPaise), preview.rate.stripTrailingZeros().toPlainString())
                    }
                    Text(text, style = MaterialTheme.typography.headlineSmall)
                    if (preview.sample) {
                        Text(stringResource(R.string.business_expected_sample), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Preview.NoRule -> Text(stringResource(R.string.business_no_rule), color = MaterialTheme.colorScheme.error)
                Preview.Ambiguous -> Text(stringResource(R.string.business_ambiguous), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
