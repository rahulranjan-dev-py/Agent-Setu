package app.agentsetu.ui.rates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.agentsetu.R
import app.agentsetu.core.format.IndianFormat
import app.agentsetu.core.input.AmountInput
import app.agentsetu.core.model.CommissionBasis
import app.agentsetu.core.model.PolicyCategory
import app.agentsetu.core.model.ProductGroup
import app.agentsetu.core.model.StaffType
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.ProductEntity
import app.agentsetu.data.repo.CommissionRepository
import app.agentsetu.ui.common.AppScaffold
import app.agentsetu.ui.common.CheckRow
import app.agentsetu.ui.common.DateField
import app.agentsetu.ui.common.FormField
import app.agentsetu.ui.common.RadioGroup
import app.agentsetu.ui.common.SectionTitle
import app.agentsetu.ui.common.labelRes
import app.agentsetu.ui.common.localized
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 1 = first-year premium, 2 = renewal premium, null = every year / not year-based. */
enum class YearChoice(val yearOfPolicy: Int?) { ANY(null), FIRST(1), RENEWAL(2) }

@HiltViewModel
class RuleAddViewModel @Inject constructor(
    db: AgentSetuDatabase,
    private val commission: CommissionRepository,
) : ViewModel() {
    val products = db.productDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var product by mutableStateOf<ProductEntity?>(null)
    var category by mutableStateOf(PolicyCategory.ANY)
    var staffTypes by mutableStateOf(setOf<StaffType>())
    var basis by mutableStateOf(CommissionBasis.PERCENT_OF_PREMIUM)
    var rate by mutableStateOf("")
    var year by mutableStateOf(YearChoice.ANY)
    var minTerm by mutableStateOf("")
    var maxTerm by mutableStateOf("")
    var from by mutableStateOf(IndianFormat.date(LocalDate.now()))
    var orderRef by mutableStateOf("")
    var notes by mutableStateOf("")
    var verified by mutableStateOf(false)
    var showErrors by mutableStateOf(false)
        private set

    val isPli get() = product?.productGroup == ProductGroup.PLI
    val isInsurance get() = product?.productGroup == ProductGroup.PLI || product?.productGroup == ProductGroup.RPLI
    val isFlat get() = basis == CommissionBasis.FLAT_PER_CASE

    fun selectProduct(p: ProductEntity) {
        product = p
        basis = when (p.productGroup) {
            ProductGroup.PLI, ProductGroup.RPLI -> CommissionBasis.PERCENT_OF_PREMIUM
            ProductGroup.SAVINGS -> CommissionBasis.PERCENT_OF_DEPOSIT
            ProductGroup.IPPB, ProductGroup.OTHER -> CommissionBasis.FLAT_PER_CASE
        }
        if (p.productGroup != ProductGroup.PLI) category = PolicyCategory.ANY
        if (p.productGroup != ProductGroup.PLI && p.productGroup != ProductGroup.RPLI) year = YearChoice.ANY
    }

    fun toggleStaff(type: StaffType, on: Boolean) {
        staffTypes = if (on) staffTypes + type else staffTypes - type
    }

    private fun parsedRate(): BigDecimal? = if (isFlat) AmountInput.parse(rate) else AmountInput.parsePercent(rate)
    private fun parsedTerm(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..99 }
    val productMissing get() = product == null
    val staffMissing get() = staffTypes.isEmpty()
    val rateInvalid get() = parsedRate() == null
    val minTermInvalid get() = minTerm.isNotBlank() && parsedTerm(minTerm) == null
    val maxTermInvalid get() = maxTerm.isNotBlank() && parsedTerm(maxTerm) == null
    val fromInvalid get() = IndianFormat.parseDate(from) == null
    val orderMissing get() = orderRef.isBlank()

    /** Adds a product the catalogue does not have and selects it for this rule. */
    fun addCustomProduct(nameEn: String, nameHi: String, group: ProductGroup, onDone: () -> Unit) {
        if (nameEn.isBlank()) return
        viewModelScope.launch {
            selectProduct(commission.addCustomProduct(nameEn, nameHi, group))
            onDone()
        }
    }

    fun save(onSaved: () -> Unit) {
        showErrors = true
        val p = product ?: return
        val r = parsedRate() ?: return
        val start = IndianFormat.parseDate(from) ?: return
        if (staffMissing || orderMissing || minTermInvalid || maxTermInvalid) return
        viewModelScope.launch {
            commission.addRule(
                product = p,
                policyCategory = category,
                staffTypes = staffTypes,
                basis = basis,
                rate = r,
                yearOfPolicy = if (isInsurance) year.yearOfPolicy else null,
                minTermYears = if (isInsurance) parsedTerm(minTerm) else null,
                maxTermYears = if (isInsurance) parsedTerm(maxTerm) else null,
                from = start,
                orderRef = orderRef,
                verified = verified,
                notes = notes,
            )
            onSaved()
        }
    }
}

@Composable
fun RuleAddScreen(onBack: () -> Unit, onSaved: () -> Unit, viewModel: RuleAddViewModel = hiltViewModel()) {
    val vm = viewModel
    val products by vm.products.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    var addingProduct by remember { mutableStateOf(false) }

    AppScaffold(title = stringResource(R.string.rule_add_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.rule_add_help), style = MaterialTheme.typography.bodyMedium)

            SectionTitle(stringResource(R.string.business_product))
            OutlinedButton(
                onClick = { picking = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(vm.product?.let { localized(it.nameEn, it.nameHi) } ?: stringResource(R.string.business_choose_product))
            }
            if (vm.showErrors && vm.productMissing) {
                Text(stringResource(R.string.error_required), color = MaterialTheme.colorScheme.error)
            }
            if (vm.isPli) {
                Text(stringResource(R.string.business_policy_category), style = MaterialTheme.typography.bodyMedium)
                RadioGroup(
                    listOf(PolicyCategory.ANY, PolicyCategory.AEA, PolicyCategory.NON_AEA),
                    vm.category,
                    { it.labelRes()?.let { res -> stringResource(res) } ?: stringResource(R.string.rule_category_any) },
                ) { vm.category = it }
            }

            SectionTitle(stringResource(R.string.rule_staff_types))
            StaffType.entries.forEach { type ->
                CheckRow(type in vm.staffTypes, { vm.toggleStaff(type, it) }, stringResource(type.labelRes()))
            }
            if (vm.showErrors && vm.staffMissing) {
                Text(stringResource(R.string.rule_staff_missing), color = MaterialTheme.colorScheme.error)
            }

            SectionTitle(stringResource(R.string.rule_basis))
            RadioGroup(CommissionBasis.entries, vm.basis, { stringResource(it.labelRes()) }) { vm.basis = it }
            FormField(
                vm.rate, { vm.rate = it },
                stringResource(if (vm.isFlat) R.string.rule_new_amount else R.string.rule_new_rate),
                error = if (vm.showErrors && vm.rateInvalid) {
                    stringResource(if (vm.isFlat) R.string.error_amount else R.string.error_percent)
                } else {
                    null
                },
                keyboardType = KeyboardType.Decimal,
            )

            if (vm.isInsurance) {
                SectionTitle(stringResource(R.string.rule_year))
                RadioGroup(
                    YearChoice.entries,
                    vm.year,
                    {
                        when (it) {
                            YearChoice.ANY -> stringResource(R.string.rule_year_any)
                            YearChoice.FIRST -> stringResource(R.string.rates_year_1)
                            YearChoice.RENEWAL -> stringResource(R.string.rates_year_2)
                        }
                    },
                ) { vm.year = it }
                Text(stringResource(R.string.rule_term_help), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField(
                        vm.minTerm, { vm.minTerm = it }, stringResource(R.string.rule_term_min),
                        modifier = Modifier.weight(1f),
                        error = if (vm.showErrors && vm.minTermInvalid) stringResource(R.string.error_whole_years) else null,
                        keyboardType = KeyboardType.Number,
                    )
                    FormField(
                        vm.maxTerm, { vm.maxTerm = it }, stringResource(R.string.rule_term_max),
                        modifier = Modifier.weight(1f),
                        error = if (vm.showErrors && vm.maxTermInvalid) stringResource(R.string.error_whole_years) else null,
                        keyboardType = KeyboardType.Number,
                    )
                }
            }

            SectionTitle(stringResource(R.string.rule_source))
            DateField(
                vm.from, { vm.from = it }, stringResource(R.string.rule_from),
                error = if (vm.showErrors && vm.fromInvalid) stringResource(R.string.error_date) else null,
            )
            FormField(
                vm.orderRef, { vm.orderRef = it }, stringResource(R.string.rule_order),
                error = if (vm.showErrors && vm.orderMissing) stringResource(R.string.error_required) else null,
            )
            FormField(vm.notes, { vm.notes = it }, stringResource(R.string.rule_notes), singleLine = false)
            CheckRow(vm.verified, { vm.verified = it }, stringResource(R.string.rule_verified))

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
                            supportingContent = if (p.isCustom) {
                                { Text(stringResource(R.string.rates_badge_custom)) }
                            } else {
                                null
                            },
                            modifier = Modifier.clickable {
                                vm.selectProduct(p)
                                picking = false
                            },
                        )
                    }
                    item {
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.product_add), color = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable {
                                picking = false
                                addingProduct = true
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (addingProduct) {
        AddProductDialog(
            onDismiss = { addingProduct = false },
            onSave = { en, hi, group -> vm.addCustomProduct(en, hi, group) { addingProduct = false } },
        )
    }
}

@Composable
private fun AddProductDialog(onDismiss: () -> Unit, onSave: (String, String, ProductGroup) -> Unit) {
    var nameEn by remember { mutableStateOf("") }
    var nameHi by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(ProductGroup.OTHER) }
    var tried by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.product_add)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.product_add_help), style = MaterialTheme.typography.bodyMedium)
                FormField(
                    nameEn, { nameEn = it }, stringResource(R.string.product_name_en),
                    error = if (tried && nameEn.isBlank()) stringResource(R.string.error_required) else null,
                )
                FormField(nameHi, { nameHi = it }, stringResource(R.string.product_name_hi))
                Text(stringResource(R.string.product_group), style = MaterialTheme.typography.bodyMedium)
                RadioGroup(ProductGroup.entries, group, { stringResource(it.labelRes()) }) { group = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (nameEn.isNotBlank()) onSave(nameEn, nameHi, group)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
