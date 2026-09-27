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
import app.agentsetu.core.model.RefLast4
import app.agentsetu.core.model.StaffType
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.HoldingEntity
import app.agentsetu.data.db.ProductEntity
import app.agentsetu.data.repo.CommissionRepository
import app.agentsetu.ui.common.AppScaffold
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val customerId: String = checkNotNull(savedStateHandle["customerId"])

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
    var refLast4 by mutableStateOf("")
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
            refreshPreview()
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
    val maturityInvalid get() = maturityDate.isNotBlank() && IndianFormat.parseDate(maturityDate) == null
    val refInvalid get() = refLast4.isNotBlank() && !RefLast4.isValid(refLast4)

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
    fun updateRef(value: String) { refLast4 = value.filter { it.isDigit() }.take(4) }

    private fun suggestMaturity() {
        if (maturityEdited) return
        val code = product?.code ?: return
        val start = IndianFormat.parseDate(startDate) ?: return
        val catalogue = DefaultProducts.byCode(code) ?: return
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
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val basePaise = Money.toPaise(base)
            val holding = HoldingEntity(
                customerId = customerId,
                productId = p.id,
                refLast4 = refLast4.ifBlank { null },
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
            onSaved()
        }
    }
}

@Composable
fun AddBusinessScreen(onBack: () -> Unit, onSaved: () -> Unit, viewModel: AddBusinessViewModel = hiltViewModel()) {
    val vm = viewModel
    val products by vm.products.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    val err = vm.showErrors

    AppScaffold(title = stringResource(R.string.business_title), onBack = onBack) { padding ->
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
            FormField(
                vm.startDate, vm::updateStart, stringResource(R.string.business_start_date),
                error = if (err && vm.startInvalid) stringResource(R.string.error_date) else null,
                supporting = stringResource(R.string.date_hint),
                keyboardType = KeyboardType.Number,
            )
            FormField(
                vm.maturityDate, vm::updateMaturity, stringResource(R.string.business_maturity_date),
                error = if (err && vm.maturityInvalid) stringResource(R.string.error_date) else null,
                supporting = stringResource(R.string.date_hint),
                keyboardType = KeyboardType.Number,
            )
            FormField(
                vm.refLast4, vm::updateRef, stringResource(R.string.business_ref_last4),
                error = if (err && vm.refInvalid) stringResource(R.string.business_ref_error) else null,
                supporting = stringResource(R.string.business_ref_error),
                keyboardType = KeyboardType.Number,
            )

            PreviewCard(vm.preview)

            Button(
                onClick = { vm.save(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
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
