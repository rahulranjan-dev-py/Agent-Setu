package app.agentsetu.ui.nav

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import app.agentsetu.ui.rates.RuleEditScreen
import app.agentsetu.ui.lock.PinSetupScreen
import app.agentsetu.ui.settings.ErrorReportScreen
import app.agentsetu.ui.settings.SettingsScreen

private object Routes {
    const val TODAY = "today"
    const val CUSTOMERS = "customers"
    const val COMMISSION = "commission"
    const val SETTINGS = "settings"
    const val CUSTOMER_NEW = "customer-new"
    const val CUSTOMER = "customer/{id}"
    const val CUSTOMER_EDIT = "customer-edit/{id}"
    const val BUSINESS = "business/{customerId}"
    const val PROFILE = "profile"
    const val RATES = "rates"
    const val RULE = "rule/{id}"
    const val APP_LOCK = "app-lock"
    const val BACKUP = "backup"
    const val ERROR_REPORT = "error-report"

    fun customer(id: String) = "customer/$id"
    fun customerEdit(id: String) = "customer-edit/$id"
    fun business(customerId: String) = "business/$customerId"
    fun rule(id: String) = "rule/$id"
}

private data class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TODAY, R.string.nav_today, Icons.Filled.Home),
    Tab(Routes.CUSTOMERS, R.string.nav_customers, Icons.Filled.Person),
    Tab(Routes.COMMISSION, R.string.nav_commission, Icons.Filled.DateRange),
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
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
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
                        nav.navigate(Routes.customer(id)) { popUpTo(Routes.CUSTOMERS) }
                    },
                    onDeleted = { nav.popBackStack() },
                )
            }
            composable(Routes.CUSTOMER, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                CustomerDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.customerEdit(it)) },
                    onAddBusiness = { nav.navigate(Routes.business(it)) },
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
            composable(Routes.PROFILE) {
                ProfileScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(Routes.RATES) {
                RatesScreen(onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.rule(it)) })
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
