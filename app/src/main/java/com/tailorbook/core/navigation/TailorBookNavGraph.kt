package com.tailorbook.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.tailorbook.core.database.TailorBookDatabase
import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.core.network.SupabaseAuthServiceImpl
import com.tailorbook.core.network.SupabaseConfig
import com.tailorbook.feature.auth.domain.AuthRepositoryImpl
import com.tailorbook.feature.auth.presentation.AuthViewModel
import com.tailorbook.feature.auth.presentation.LanguageSelectionScreen
import com.tailorbook.feature.auth.presentation.LoginScreen
import com.tailorbook.feature.auth.presentation.OnboardingScreen
import com.tailorbook.feature.auth.presentation.ShopProfileConfigScreen
import com.tailorbook.feature.auth.presentation.ShopRegistrationScreen
import com.tailorbook.feature.auth.presentation.SplashScreen
import com.tailorbook.feature.calendar.presentation.CalendarViewModel
import com.tailorbook.feature.calendar.presentation.DeliveryCalendarScreen
import com.tailorbook.feature.customers.data.repository.CustomerRepositoryImpl
import com.tailorbook.feature.customers.presentation.AddCustomerScreen
import com.tailorbook.feature.customers.presentation.CustomerListScreen
import com.tailorbook.feature.customers.presentation.CustomerProfileScreen
import com.tailorbook.feature.customers.presentation.CustomerViewModel
import com.tailorbook.feature.customers.presentation.EditCustomerScreen
import com.tailorbook.feature.customers.presentation.SelectCustomerForMeasurementScreen
import com.tailorbook.feature.dashboard.presentation.DashboardScreen
import com.tailorbook.feature.gallery.data.repository.DesignReferenceRepositoryImpl
import com.tailorbook.feature.gallery.presentation.DesignGalleryScreen
import com.tailorbook.feature.gallery.presentation.GalleryViewModel
import com.tailorbook.feature.gallery.presentation.PhotoDetailsScreen
import com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl
import com.tailorbook.feature.orders.data.repository.OrderRepositoryImpl
import com.tailorbook.feature.orders.presentation.OrderCreationWizardScreen
import com.tailorbook.feature.orders.presentation.OrderDetailsScreen
import com.tailorbook.feature.orders.presentation.OrderListScreen
import com.tailorbook.feature.orders.presentation.OrderViewModel
import com.tailorbook.feature.payments.data.repository.PaymentRepositoryImpl
import com.tailorbook.feature.payments.presentation.PaymentListScreen
import com.tailorbook.feature.payments.presentation.PaymentViewModel
import com.tailorbook.feature.payments.presentation.RecordPaymentScreen
import com.tailorbook.feature.reports.presentation.ReportsScreen
import com.tailorbook.feature.reports.presentation.ReportsViewModel
import com.tailorbook.feature.notifications.presentation.NotificationCenterScreen
import com.tailorbook.feature.notifications.presentation.NotificationViewModel
import com.tailorbook.feature.staff.data.local.entity.StaffEntity
import com.tailorbook.feature.staff.data.repository.StaffRepositoryImpl
import com.tailorbook.feature.staff.presentation.StaffListScreen
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.core.ads.AdMobInterstitialAdManager
import com.tailorbook.core.ads.AdMobRewardedAdManager
import com.tailorbook.core.billing.PlayBillingManagerImpl
import com.tailorbook.feature.staff.presentation.StaffViewModel
import com.tailorbook.feature.subscription.domain.CustomerEntitlementEngineImpl
import com.tailorbook.feature.subscription.presentation.SubscriptionScreen
import com.tailorbook.feature.subscription.presentation.SubscriptionViewModel
import com.tailorbook.feature.measurements.presentation.GarmentTemplateSelectorScreen
import com.tailorbook.feature.measurements.presentation.MeasurementDiffScreen
import com.tailorbook.feature.measurements.presentation.MeasurementEntryScreen
import com.tailorbook.feature.measurements.presentation.MeasurementHistoryScreen
import com.tailorbook.feature.measurements.presentation.MeasurementViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Root navigation graph for TailorBook.
 * Integrates session state persistence with DataStore.
 */
@Composable
fun TailorBookNavGraph(
    navController: NavHostController,
    onMenuClick: () -> Unit,
    startDestination: String = Screen.Splash.route,
    preferencesManager: UserPreferencesManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(preferencesManager) { preferencesManager ?: UserPreferencesManager(context) }
    val deviceManager = remember { com.tailorbook.core.device.DeviceManager(context, prefs) }
    val authService = remember { SupabaseAuthServiceImpl() }
    val authRepository = remember { AuthRepositoryImpl(authService, prefs, deviceManager) }
    val authViewModel = remember { AuthViewModel(authRepository) }
    val shopName by prefs.shopNameFlow.collectAsState(initial = "My Boutique")
    val shopUpiId by prefs.shopUpiIdFlow.collectAsState(initial = "")
    val userEmail by prefs.userEmailFlow.collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    val database = remember { TailorBookDatabase.getInstance(context) }
    val customerRepository = remember { CustomerRepositoryImpl(database.customerDao(), database.familyMemberDao(), database.syncQueueDao(), context, prefs) }
    val cloudSyncService = remember { com.tailorbook.core.sync.TailorCloudSyncService(database, prefs) }
    val customerViewModel = remember { CustomerViewModel(customerRepository) }
    val measurementRepository = remember { MeasurementRepositoryImpl(database.measurementDao(), database.syncQueueDao(), context, prefs, database.measurementTemplateDao()) }
    val measurementViewModel = remember { MeasurementViewModel(measurementRepository) }
    val galleryRepository = remember { DesignReferenceRepositoryImpl(database.designReferenceDao(), database.syncQueueDao(), context, prefs) }
    val galleryViewModel = remember { GalleryViewModel(galleryRepository) }
    val orderRepository = remember { OrderRepositoryImpl(database.orderDao(), database.syncQueueDao(), context, prefs) }
    val orderViewModel = remember { OrderViewModel(orderRepository) }
    val calendarViewModel = remember { CalendarViewModel(orderRepository, customerRepository) }
    val paymentRepository = remember { PaymentRepositoryImpl(database.paymentDao(), database.orderDao(), database.syncQueueDao(), context, prefs) }
    val paymentViewModel = remember { PaymentViewModel(paymentRepository, orderRepository, customerRepository) }
    val reportsViewModel = remember { ReportsViewModel(orderRepository, paymentRepository) }
    val notificationViewModel = remember { NotificationViewModel(orderRepository, customerRepository) }
    val staffRepository = remember { StaffRepositoryImpl(database.staffDao()) }
    val staffViewModel = remember { StaffViewModel(staffRepository) }
    val playBillingManager = remember { PlayBillingManagerImpl(context) }
    val subscriptionViewModel = remember { SubscriptionViewModel(prefs, playBillingManager) }
    val entitlementEngine = remember(userEmail) {
        CustomerEntitlementEngineImpl(
            customerDao = database.customerDao(),
            subscriptionPreferences = prefs,
            shopIdProvider = { SupabaseConfig.activeShopId ?: "shop-001" },
            userEmailProvider = { userEmail },
            deviceManager = deviceManager
        )
    }
    val rewardedAdManager = remember { AdMobRewardedAdManager() }
    val interstitialAdManager = remember { AdMobInterstitialAdManager() }
    val consentManager = remember { com.tailorbook.core.ads.GoogleUmpConsentManager(context) }
    val dashboardViewModel = remember {
        com.tailorbook.feature.dashboard.presentation.DashboardViewModel(
            orderDao = database.orderDao(),
            customerDao = database.customerDao()
        )
    }
    val isPremium by prefs.isPremiumFlow.collectAsState(initial = false)
    val activeUserId by prefs.userIdFlow.collectAsState(initial = null)

    LaunchedEffect(activeUserId) {
        val shopId = activeUserId.orEmpty()
        customerViewModel.setActiveShopId(shopId)
        orderViewModel.setActiveShopId(shopId)
        paymentViewModel.setActiveShopId(shopId)
        galleryViewModel.setActiveShopId(shopId)
        dashboardViewModel.setActiveShopId(shopId)
        val email = prefs.userEmailFlow.first()
        val phone = prefs.ownerPhoneFlow.first()
        com.tailorbook.core.billing.VipAccessManager.applyVipStatusIfEligible(phone, email, prefs)
        if (shopId.isNotBlank() && shopId != "default_shop") {
            withContext(Dispatchers.IO) {
                cloudSyncService.pullShopData(shopId)
            }
        }
    }

    LaunchedEffect(Unit) {
        rewardedAdManager.preloadAd(context)
        interstitialAdManager.preloadAd(context)
        val isCleaned = prefs.isReleaseDbCleanedFlow.first()
        if (!isCleaned) {
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
            prefs.setReleaseDbCleaned(true)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    scope.launch {
                        val isLoggedIn = prefs.isLoggedInFlow.first()
                        val isRestorePending = prefs.isRestorePendingFlow.first()
                        val isOnboardingCompleted = prefs.isOnboardingCompletedFlow.first()
                        val targetRoute = when {
                            isLoggedIn && isRestorePending -> Screen.Login.route
                            isLoggedIn -> Screen.Dashboard.route
                            isOnboardingCompleted -> Screen.Login.route
                            else -> Screen.Onboarding.route
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    scope.launch {
                        prefs.setOnboardingCompleted(true)
                        navController.navigate(Screen.LanguageSelection.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.LanguageSelection.route) {
            LanguageSelectionScreen(
                onLanguageConfirmed = { langCode ->
                    scope.launch {
                        prefs.setSelectedLanguage(langCode)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.LanguageSelection.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            val lastLoginEmail by prefs.lastLoginEmailFlow.collectAsState(initial = "")
            val isRestorePending by prefs.isRestorePendingFlow.collectAsState(initial = false)
            val activeUserId by prefs.userIdFlow.collectAsState(initial = null)
            var showLoginRestorePrompt by remember { mutableStateOf(false) }
            var isRestoringData by remember { mutableStateOf(false) }
            var pendingLoginUserId by remember { mutableStateOf("") }

            LaunchedEffect(isRestorePending, activeUserId) {
                if (isRestorePending && !activeUserId.isNullOrBlank()) {
                    pendingLoginUserId = activeUserId.orEmpty()
                    showLoginRestorePrompt = true
                }
            }

            LoginScreen(
                authViewModel = authViewModel,
                initialEmail = lastLoginEmail,
                onLoginSuccess = {
                    scope.launch {
                        val userId = prefs.userIdFlow.first().orEmpty()
                        if (userId.isNotBlank()) {
                            customerViewModel.setActiveShopId(userId)
                            dashboardViewModel.setActiveShopId(userId)
                            val isEligible = prefs.isSyncEligible()
                            if (isEligible) {
                                withContext(Dispatchers.IO) {
                                    cloudSyncService.pullShopData(userId)
                                }
                                prefs.setRestorePending(false)
                                withContext(Dispatchers.Main) {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            } else {
                                prefs.setRestorePending(true)
                                pendingLoginUserId = userId
                                showLoginRestorePrompt = true
                            }
                        } else {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.ShopRegistration.route)
                }
            )

            if (showLoginRestorePrompt) {
                AlertDialog(
                    onDismissRequest = {
                        // Strictly non-dismissible: user must restore cloud database before using app
                    },
                    title = {
                        Text(
                            text = stringResource(id = com.tailorbook.R.string.restore_required_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DeepTeal
                        )
                    },
                    text = {
                        if (isRestoringData) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = DeepTeal,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(id = com.tailorbook.R.string.restore_in_progress),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Charcoal
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(id = com.tailorbook.R.string.restore_required_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Charcoal
                            )
                        }
                    },
                    confirmButton = {
                        if (!isRestoringData) {
                            Button(
                                onClick = {
                                    val activity = context.findActivity()
                                    if (activity != null) {
                                        rewardedAdManager.showAd(
                                            activity = activity,
                                            onUserEarnedReward = {
                                                isRestoringData = true
                                                scope.launch {
                                                    prefs.grantOneTimeCloudBackup()
                                                    withContext(Dispatchers.IO) {
                                                        cloudSyncService.pullShopData(pendingLoginUserId)
                                                    }
                                                    prefs.setRestorePending(false)
                                                    isRestoringData = false
                                                    showLoginRestorePrompt = false
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, context.getString(com.tailorbook.R.string.restore_success), Toast.LENGTH_LONG).show()
                                                        navController.navigate(Screen.Dashboard.route) {
                                                            popUpTo(Screen.Login.route) { inclusive = true }
                                                        }
                                                    }
                                                }
                                            },
                                            onAdFailed = {
                                                isRestoringData = false
                                                Toast.makeText(context, context.getString(com.tailorbook.R.string.restore_ad_not_completed), Toast.LENGTH_LONG).show()
                                                rewardedAdManager.preloadAd(activity)
                                            }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                            ) {
                                Text(stringResource(id = com.tailorbook.R.string.restore_dialog_watch), color = CardWhite)
                            }
                        }
                    },
                    dismissButton = {
                        if (!isRestoringData) {
                            TextButton(
                                onClick = {
                                    showLoginRestorePrompt = false
                                    scope.launch {
                                        authViewModel.signOut()
                                        prefs.clearSession()
                                    }
                                }
                            ) {
                                Text(stringResource(id = com.tailorbook.R.string.restore_btn_signout), color = CharcoalSubtle)
                            }
                        }
                    },
                    containerColor = CardWhite,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        composable(Screen.ShopRegistration.route) {
            ShopRegistrationScreen(
                authViewModel = authViewModel,
                onRegistrationComplete = { registeredShop, registeredOwner, registeredPhone, registeredType, registeredEmail, registeredUpiId ->
                    scope.launch {
                        prefs.saveShopProfile(registeredShop, registeredOwner, registeredPhone, registeredType, registeredUpiId)
                        if (registeredEmail.isNotBlank()) {
                            prefs.setUserEmail(registeredEmail)
                        }
                        val newUserId = prefs.userIdFlow.first().orEmpty()
                        customerViewModel.setActiveShopId(newUserId)
                        dashboardViewModel.setActiveShopId(newUserId)
                        navController.navigate(Screen.ShopProfileConfig.route) {
                            popUpTo(Screen.ShopRegistration.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ShopRegistration.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ShopProfileConfig.route) {
            val openingTime by prefs.openingTimeFlow.collectAsState(initial = "09:30 AM")
            val closingTime by prefs.closingTimeFlow.collectAsState(initial = "08:30 PM")
            val weeklyOff by prefs.weeklyOffFlow.collectAsState(initial = "Sunday")
            val unit by prefs.measurementUnitFlow.collectAsState(initial = "INCH")
            val leadDays by prefs.deliveryLeadDaysFlow.collectAsState(initial = "5")
            val address by prefs.shopAddressFlow.collectAsState(initial = "")
            val gstin by prefs.gstinFlow.collectAsState(initial = "")
            val currentShopName by prefs.shopNameFlow.collectAsState(initial = "My Boutique")
            val currentEmail by prefs.userEmailFlow.collectAsState(initial = "")
            val currentPhone by prefs.ownerPhoneFlow.collectAsState(initial = "")

            ShopProfileConfigScreen(
                initialOpeningTime = openingTime,
                initialClosingTime = closingTime,
                initialWeeklyOff = weeklyOff,
                initialUnit = unit,
                initialLeadDays = leadDays,
                initialAddress = address,
                initialGstin = gstin,
                initialUpiId = shopUpiId,
                shopName = currentShopName,
                accountEmail = currentEmail.orEmpty(),
                accountPhone = currentPhone.orEmpty(),
                isPremium = isPremium,
                rewardedAdManager = rewardedAdManager,
                consentManager = consentManager,
                onTriggerCloudBackup = {
                    scope.launch {
                        prefs.grantOneTimeCloudBackup()
                        val sId = prefs.userIdFlow.first().orEmpty()
                        if (sId.isNotBlank()) {
                            withContext(Dispatchers.IO) {
                                cloudSyncService.pullShopData(sId)
                                com.tailorbook.core.sync.worker.TailorSyncWorker.triggerImmediateSync(context)
                            }
                        }
                    }
                },
                onTriggerCloudRestore = {
                    scope.launch {
                        prefs.grantOneTimeCloudBackup()
                        val sId = prefs.userIdFlow.first().orEmpty()
                        if (sId.isNotBlank()) {
                            withContext(Dispatchers.IO) {
                                cloudSyncService.pullShopData(sId)
                            }
                            prefs.setRestorePending(false)
                        }
                    }
                },
                onExportOfflineBackup = {
                    scope.launch {
                        val sId = prefs.userIdFlow.first().orEmpty().ifBlank { "shop-001" }
                        val result = com.tailorbook.core.backup.OfflineBackupHelper.exportShopData(
                            context = context,
                            database = database,
                            shopName = currentShopName,
                            shopId = sId
                        )
                        result.onSuccess { backupFile ->
                            com.tailorbook.core.backup.OfflineBackupHelper.shareBackupFile(
                                context = context,
                                file = backupFile,
                                shopName = currentShopName
                            )
                        }.onFailure { e ->
                            Toast.makeText(context, "Failed to export backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onNavigateToUpgrade = {
                    navController.navigate(Screen.Subscription.route)
                },
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Dashboard.route)
                    }
                },
                onConfigSaved = { opening, closing, newWeeklyOff, newUnit, newLeadDays, newAddress, newGstin, newUpiId ->
                    scope.launch {
                        prefs.saveShopWorkingConfig(
                            openingTime = opening,
                            closingTime = closing,
                            weeklyOff = newWeeklyOff,
                            measurementUnit = newUnit,
                            leadDays = newLeadDays,
                            address = newAddress,
                            gstin = newGstin,
                            upiId = newUpiId
                        )
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.ShopProfileConfig.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val isRestorePending by prefs.isRestorePendingFlow.collectAsState(initial = false)
            LaunchedEffect(isRestorePending) {
                if (isRestorePending) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            }

            DashboardScreen(
                onMenuClick = onMenuClick,
                shopName = shopName,
                isPremium = isPremium,
                dashboardViewModel = dashboardViewModel,
                onNewCustomerClick = {
                    navController.navigate(Screen.AddCustomer.route)
                },
                onNewMeasurementClick = {
                    navController.navigate(Screen.SelectCustomerForMeasurement.route)
                },
                onNewOrderClick = {
                    navController.navigate(Screen.OrderCreation.createRoute())
                },
                onRecordPaymentClick = {
                    navController.navigate(Screen.PaymentList.route)
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Notifications.route)
                },
                onMetricClick = { metricType ->
                    when (metricType) {
                        "pending_balance" -> navController.navigate(Screen.PaymentList.route)
                        "active_orders" -> navController.navigate(Screen.OrderList.route)
                        "due_today" -> navController.navigate(Screen.DeliveryCalendar.route)
                        "ready_pickup" -> navController.navigate(Screen.OrderList.route)
                    }
                }
            )
        }

        composable(Screen.CustomerList.route) {
            CustomerListScreen(
                viewModel = customerViewModel,
                isPremium = isPremium,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                },
                onMenuClick = onMenuClick,
                onCustomerClick = { customerId ->
                    navController.navigate(Screen.CustomerProfile.createRoute(customerId))
                },
                onAddCustomerClick = {
                    navController.navigate(Screen.AddCustomer.route)
                }
            )
        }

        composable(Screen.AddCustomer.route) {
            AddCustomerScreen(
                viewModel = customerViewModel,
                entitlementEngine = entitlementEngine,
                rewardedAdManager = rewardedAdManager,
                onNavigateToUpgrade = {
                    navController.navigate(Screen.Subscription.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCustomerCreated = { customerId ->
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.CustomerProfile.route,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            CustomerProfileScreen(
                customerId = customerId,
                viewModel = customerViewModel,
                isPremium = isPremium,
                rewardedAdManager = rewardedAdManager,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.CustomerList.route)
                    }
                },
                onEditCustomerClick = { id ->
                    navController.navigate(Screen.EditCustomer.createRoute(id))
                },
                onNewMeasurementClick = { id ->
                    navController.navigate(Screen.GarmentTemplateSelector.createRoute(id))
                },
                onNewOrderClick = { id ->
                    navController.navigate(Screen.OrderCreation.createRoute(customerId = id))
                }
            )
        }

        composable(
            route = Screen.EditCustomer.route,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            EditCustomerScreen(
                customerId = customerId,
                viewModel = customerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCustomerUpdated = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GarmentTemplateSelector.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType },
                navArgument("familyMemberId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            val familyMemberId = backStackEntry.arguments?.getString("familyMemberId")
            val customer = customerViewModel.filteredCustomers.collectAsState().value
                .find { it.customer.id == customerId }
            val customerName = customer?.customer?.name ?: "Customer"

            GarmentTemplateSelectorScreen(
                customerId = customerId,
                familyMemberId = familyMemberId,
                customerName = customerName,
                viewModel = measurementViewModel,
                isPremium = isPremium,
                rewardedAdManager = rewardedAdManager,
                onNavigateToUpgrade = {
                    navController.navigate(Screen.Subscription.route)
                },
                onNavigateBack = { navController.popBackStack() },
                onTemplateSelected = { templateId ->
                    val existing = measurementViewModel.getVersions(customerId, familyMemberId, templateId)
                    if (existing.isNotEmpty()) {
                        navController.navigate(Screen.MeasurementHistory.createRoute(customerId, templateId, familyMemberId))
                    } else {
                        navController.navigate(Screen.MeasurementEntry.createRoute(customerId, templateId, familyMemberId))
                    }
                }
            )
        }

        composable(
            route = Screen.MeasurementEntry.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType },
                navArgument("templateId") { type = NavType.StringType },
                navArgument("familyMemberId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            val templateId = backStackEntry.arguments?.getString("templateId") ?: "BLOUSE"
            val familyMemberId = backStackEntry.arguments?.getString("familyMemberId")
            val customer = customerViewModel.filteredCustomers.collectAsState().value
                .find { it.customer.id == customerId }
            val customerName = customer?.customer?.name ?: "Customer"

            MeasurementEntryScreen(
                customerId = customerId,
                familyMemberId = familyMemberId,
                templateId = templateId,
                customerName = customerName,
                viewModel = measurementViewModel,
                onNavigateBack = { navController.popBackStack() },
                onVersionSaved = { _ ->
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.MeasurementHistory.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType },
                navArgument("templateId") { type = NavType.StringType },
                navArgument("familyMemberId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            val templateId = backStackEntry.arguments?.getString("templateId") ?: "BLOUSE"
            val familyMemberId = backStackEntry.arguments?.getString("familyMemberId")
            val customer = customerViewModel.filteredCustomers.collectAsState().value
                .find { it.customer.id == customerId }
            val customerName = customer?.customer?.name ?: "Customer"

            MeasurementHistoryScreen(
                customerId = customerId,
                familyMemberId = familyMemberId,
                templateId = templateId,
                customerName = customerName,
                viewModel = measurementViewModel,
                isPremium = isPremium,
                onNavigateBack = { navController.popBackStack() },
                onRecordNewVersionClick = {
                    navController.navigate(Screen.MeasurementEntry.createRoute(customerId, templateId, familyMemberId))
                },
                onCompareVersionsClick = { v1, v2 ->
                    navController.navigate(Screen.MeasurementDiff.createRoute(customerId, templateId, v1, v2, familyMemberId))
                }
            )
        }

        composable(
            route = Screen.MeasurementDiff.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType },
                navArgument("templateId") { type = NavType.StringType },
                navArgument("v1") { type = NavType.IntType },
                navArgument("v2") { type = NavType.IntType },
                navArgument("familyMemberId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            val templateId = backStackEntry.arguments?.getString("templateId") ?: "BLOUSE"
            val v1 = backStackEntry.arguments?.getInt("v1") ?: 1
            val v2 = backStackEntry.arguments?.getInt("v2") ?: 2
            val familyMemberId = backStackEntry.arguments?.getString("familyMemberId")
            val customer = customerViewModel.filteredCustomers.collectAsState().value
                .find { it.customer.id == customerId }
            val customerName = customer?.customer?.name ?: "Customer"

            MeasurementDiffScreen(
                customerId = customerId,
                familyMemberId = familyMemberId,
                templateId = templateId,
                v1Number = v1,
                v2Number = v2,
                customerName = customerName,
                viewModel = measurementViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DesignGallery.route) {
            DesignGalleryScreen(
                viewModel = galleryViewModel,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                },
                onMenuClick = onMenuClick,
                onPhotoClick = { photoId ->
                    navController.navigate(Screen.PhotoDetails.createRoute(photoId))
                }
            )
        }

        composable(
            route = Screen.PhotoDetails.route,
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            PhotoDetailsScreen(
                photoId = photoId,
                viewModel = galleryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPhotoDeleted = { navController.popBackStack() }
            )
        }

        composable(Screen.SelectCustomerForMeasurement.route) {
            SelectCustomerForMeasurementScreen(
                viewModel = customerViewModel,
                isPremium = isPremium,
                onNavigateBack = { navController.popBackStack() },
                onCustomerSelected = { customerId, familyMemberId ->
                    navController.navigate(Screen.GarmentTemplateSelector.createRoute(customerId, familyMemberId)) {
                        popUpTo(Screen.SelectCustomerForMeasurement.route) { inclusive = true }
                    }
                },
                onAddNewCustomerClick = {
                    navController.navigate(Screen.AddCustomer.route)
                }
            )
        }

        composable(Screen.OrderList.route) {
            OrderListScreen(
                viewModel = orderViewModel,
                customerViewModel = customerViewModel,
                shopName = shopName,
                isPremium = isPremium,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                },
                onMenuClick = onMenuClick,
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                },
                onNewOrderClick = {
                    navController.navigate(Screen.OrderCreation.createRoute())
                }
            )
        }

        composable(
            route = Screen.OrderCreation.route,
            arguments = listOf(
                navArgument("customerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("templateId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId").orEmpty()
            val templateId = backStackEntry.arguments?.getString("templateId") ?: "BLOUSE"
            OrderCreationWizardScreen(
                initialCustomerId = customerId,
                initialTemplateId = templateId,
                orderViewModel = orderViewModel,
                customerViewModel = customerViewModel,
                measurementViewModel = measurementViewModel,
                staffViewModel = staffViewModel,
                galleryViewModel = galleryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRecordMeasurements = { custId, templId ->
                    navController.navigate(Screen.MeasurementEntry.createRoute(customerId = custId, templateId = templId))
                },
                isPremium = isPremium,
                rewardedAdManager = rewardedAdManager,
                onNavigateToUpgrade = {
                    navController.navigate(Screen.Subscription.route)
                },
                onOrderCreated = { newOrderId ->
                    val activity = context.findActivity()
                    if (activity != null) {
                        interstitialAdManager.showAdIfEligible(activity, isPremium) {
                            navController.navigate(Screen.OrderDetails.createRoute(newOrderId)) {
                                popUpTo(Screen.OrderCreation.route) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(Screen.OrderDetails.createRoute(newOrderId)) {
                            popUpTo(Screen.OrderCreation.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.OrderDetails.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailsScreen(
                orderId = orderId,
                orderViewModel = orderViewModel,
                customerViewModel = customerViewModel,
                shopName = shopName,
                shopUpiId = shopUpiId,
                galleryViewModel = galleryViewModel,
                isPremium = isPremium,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DeliveryCalendar.route) {
            DeliveryCalendarScreen(
                viewModel = calendarViewModel,
                onMenuClick = onMenuClick,
                isPremium = isPremium,
                onNavigateToOrderDetails = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                }
            )
        }

        composable(Screen.PaymentList.route) {
            PaymentListScreen(
                viewModel = paymentViewModel,
                shopName = shopName,
                onMenuClick = onMenuClick,
                onRecordPaymentClick = { orderId, customerId ->
                    navController.navigate(Screen.RecordPayment.createRoute(orderId, customerId))
                }
            )
        }

        composable(
            route = Screen.RecordPayment.route,
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("customerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            val customerId = backStackEntry.arguments?.getString("customerId")
            RecordPaymentScreen(
                viewModel = paymentViewModel,
                initialOrderId = orderId,
                initialCustomerId = customerId,
                onNavigateBack = { navController.popBackStack() },
                onPaymentRecorded = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                viewModel = reportsViewModel,
                shopName = shopName,
                onMenuClick = onMenuClick,
                isPremium = isPremium,
                onUpgradeClick = {
                    navController.navigate(Screen.Subscription.route)
                }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationCenterScreen(
                viewModel = notificationViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                }
            )
        }

        composable(Screen.StaffList.route) {
            StaffListScreen(
                viewModel = staffViewModel,
                onMenuClick = onMenuClick,
                isPremium = isPremium,
                onNavigateToUpgrade = {
                    navController.navigate(Screen.Subscription.route)
                }
            )
        }

        composable(Screen.Subscription.route) {
            val customers by customerViewModel.filteredCustomers.collectAsState()
            val activeOrdersCount by orderViewModel.activeOrderCount.collectAsState()
            SubscriptionScreen(
                viewModel = subscriptionViewModel,
                onMenuClick = onMenuClick,
                customerCount = customers.size,
                activeOrdersCount = activeOrdersCount
            )
        }
    }
}
