package app.agentsetu.ui.nav

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.agentsetu.R
import androidx.hilt.navigation.compose.hiltViewModel
import app.agentsetu.ui.AgentSetuRootLock
import app.agentsetu.ui.backup.BackupScreen
import app.agentsetu.ui.business.AddBusinessScreen
import app.agentsetu.ui.commission.CommissionScreen
import app.agentsetu.ui.customers.CustomerDetailScreen
import app.agentsetu.ui.customers.CustomerEditScreen
import app.agentsetu.ui.customers.CustomersScreen
import app.agentsetu.ui.home.HomeScreen
import app.agentsetu.ui.profile.ProfileScreen
import app.agentsetu.ui.rates.RatesScreen
import app.agentsetu.ui.rates.RuleAddScreen
import app.agentsetu.ui.rates.RuleEditScreen
import app.agentsetu.ui.lock.PinSetupScreen
import app.agentsetu.ui.settings.ErrorReportScreen
import app.agentsetu.ui.settings.SettingsScreen
import app.agentsetu.ui.tools.CalcKind
import app.agentsetu.ui.tools.CalculatorScreen
import app.agentsetu.ui.tools.InterestRateEditScreen
import app.agentsetu.ui.tools.InterestRatesScreen
import app.agentsetu.ui.tools.SchemeDetailScreen
import app.agentsetu.ui.tools.SchemeListScreen
import app.agentsetu.ui.tools.ToolsScreen

private object Routes {
    const val TODAY = "today"
    const val CUSTOMERS = "customers"
    const val COMMISSION = "commission"
    const val SETTINGS = "settings"
    const val CUSTOMER_NEW = "customer-new"
    const val CUSTOMER = "customer/{id}"
    const val CUSTOMER_EDIT = "customer-edit/{id}"
    const val BUSINESS = "business/{customerId}"
    const val HOLDING = "holding/{holdingId}"
    const val PROFILE = "profile"
    const val RATES = "rates"
    const val RULE = "rule/{id}"
    const val RULE_NEW = "rule-new"
    const val TOOLS = "tools"
    const val CALC = "calc/{kind}"
    const val SCHEMES = "schemes"
    const val SCHEME = "scheme/{code}"
    const val INTEREST_RATES = "interest-rates"
    const val INTEREST_RATE = "interest-rate/{id}"
    const val APP_LOCK = "app-lock"
    const val BACKUP = "backup"
    const val ERROR_REPORT = "error-report"

    fun customer(id: String) = "customer/$id"
    fun customerEdit(id: String) = "customer-edit/$id"
    fun business(customerId: String) = "business/$customerId"
    fun holding(holdingId: String) = "holding/$holdingId"
    fun rule(id: String) = "rule/$id"
    fun calc(kind: CalcKind) = "calc/${kind.name}"
    fun scheme(code: String) = "scheme/$code"
    fun interestRate(id: String) = "interest-rate/$id"
}

private data class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TODAY, R.string.nav_today, Icons.Filled.Home),
    Tab(Routes.CUSTOMERS, R.string.nav_customers, Icons.Filled.Person),
    Tab(Routes.COMMISSION, R.string.nav_commission, Icons.Filled.DateRange),
    Tab(Routes.TOOLS, R.string.nav_tools, Icons.Filled.Build),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
fun MainNavigation() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showTabs = tabs.any { it.route == route }

    // Screens draw their own top bars and insets; this Scaffold only adds the bottom tabs.
    Scaffold(
        // The whole app (tabs included) moves up above the keyboard; inner screens see no IME inset.
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = {
                                Text(
                                    stringResource(tab.label),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.TODAY, modifier = Modifier.padding(padding)) {
            composable(Routes.TODAY) {
                HomeScreen(
                    onOpenCustomer = { nav.navigate(Routes.customer(it)) },
                    onAddBusiness = { nav.navigate(Routes.business(it)) },
                    onAddCustomer = { nav.navigate(Routes.CUSTOMER_NEW) },
                    onBackup = { nav.navigate(Routes.BACKUP) },
                )
            }
            composable(Routes.CUSTOMERS) {
                CustomersScreen(
                    onAdd = { nav.navigate(Routes.CUSTOMER_NEW) },
                    onOpen = { nav.navigate(Routes.customer(it)) },
                )
            }
            composable(Routes.COMMISSION) { CommissionScreen() }
            composable(Routes.TOOLS) {
                ToolsScreen(
                    onCalculator = { nav.navigate(Routes.calc(it)) },
                    onSchemes = { nav.navigate(Routes.SCHEMES) },
                    onRates = { nav.navigate(Routes.INTEREST_RATES) },
                )
            }
            composable(Routes.CALC, arguments = listOf(navArgument("kind") { type = NavType.StringType })) {
                CalculatorScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.SCHEMES) {
                SchemeListScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate(Routes.scheme(it)) })
            }
            composable(Routes.SCHEME, arguments = listOf(navArgument("code") { type = NavType.StringType })) {
                SchemeDetailScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.INTEREST_RATES) {
                InterestRatesScreen(onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.interestRate(it)) })
            }
            composable(Routes.INTEREST_RATE, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                InterestRateEditScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onProfile = { nav.navigate(Routes.PROFILE) },
                    onRates = { nav.navigate(Routes.RATES) },
                    onAppLock = { nav.navigate(Routes.APP_LOCK) },
                    onBackup = { nav.navigate(Routes.BACKUP) },
                    onErrorReport = { nav.navigate(Routes.ERROR_REPORT) },
                )
            }
            composable(Routes.CUSTOMER_NEW) {
                CustomerEditScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { id ->
                        // Replace the form with the new customer's page, whichever tab opened the form.
                        nav.navigate(Routes.customer(id)) { popUpTo(Routes.CUSTOMER_NEW) { inclusive = true } }
                    },
                    onDeleted = { nav.popBackStack() },
                )
            }
            composable(Routes.CUSTOMER, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                CustomerDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.customerEdit(it)) },
                    onAddBusiness = { nav.navigate(Routes.business(it)) },
                    onOpenHolding = { nav.navigate(Routes.holding(it)) },
                )
            }
            composable(Routes.CUSTOMER_EDIT, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                CustomerEditScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() },
                    onDeleted = { nav.popBackStack(Routes.CUSTOMERS, inclusive = false) },
                )
            }
            composable(Routes.BUSINESS, arguments = listOf(navArgument("customerId") { type = NavType.StringType })) {
                AddBusinessScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(Routes.HOLDING, arguments = listOf(navArgument("holdingId") { type = NavType.StringType })) {
                AddBusinessScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(Routes.RATES) {
                RatesScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.rule(it)) },
                    onAdd = { nav.navigate(Routes.RULE_NEW) },
                )
            }
            composable(Routes.RULE_NEW) {
                RuleAddScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(Routes.APP_LOCK) {
                val lock = hiltViewModel<AgentSetuRootLock>().appLock
                PinSetupScreen(appLock = lock, onDone = { nav.popBackStack() }, onSkip = null, onBack = { nav.popBackStack() })
            }
            composable(Routes.BACKUP) { BackupScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.ERROR_REPORT) { ErrorReportScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.RULE, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                RuleEditScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
