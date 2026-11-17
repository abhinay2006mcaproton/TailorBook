package com.tailorbook

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tailorbook.core.navigation.Screen
import com.tailorbook.core.navigation.TailorBookNavGraph
import com.tailorbook.ui.components.TailorBorderedCard
import com.tailorbook.ui.components.TailorDivider
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorNavigationDrawer
import com.tailorbook.ui.components.TailorOfflineBanner
import com.tailorbook.ui.components.TailorSyncBanner
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.TailorBookTheme
import com.tailorbook.ui.theme.WarmIvory
import kotlinx.coroutines.launch

/**
 * Main entry-point Activity for TailorBook.
 *
 * In Phase 01–02 this hosts the temporary scaffold / design system preview.
 * From Phase 03 onwards this will host the Navigation Compose graph; the
 * drawer and top app bar will be managed by the NavHost scaffold.
 */
class MainActivity : ComponentActivity() {

    private val deepLinkRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)

        // Request UMP Privacy Consent and initialize ads
        try {
            val consentManager = com.tailorbook.core.ads.GoogleUmpConsentManager(applicationContext)
            consentManager.gatherConsent(this) { canRequest ->
                timber.log.Timber.i("UMP Consent status: canRequestAds=$canRequest")
            }
        } catch (e: Exception) {
            timber.log.Timber.w(e, "Failed to initialize UMP Consent Manager")
        }

        setContent {
            TailorBookTheme {
                TailorBookApp(initialDeepLinkRoute = deepLinkRoute.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLink(it) }
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme == "tailorbook" && data.host == "auth-callback") {
            val params = mutableMapOf<String, String>()

            // Fragment check: #access_token=...&refresh_token=...&type=signup
            val fragment = data.fragment
            if (!fragment.isNullOrBlank()) {
                fragment.split("&").forEach { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) params[parts[0]] = parts[1]
                }
            }

            // Query parameter check
            try {
                data.queryParameterNames.forEach { name ->
                    data.getQueryParameter(name)?.let { params[name] = it }
                }
            } catch (_: Exception) { }

            val accessToken = params["access_token"]
            val refreshToken = params["refresh_token"]

            if (!accessToken.isNullOrBlank()) {
                com.tailorbook.core.network.SupabaseConfig.authToken = accessToken
                lifecycleScope.launch {
                    val prefs = com.tailorbook.core.datastore.UserPreferencesManager(applicationContext)
                    prefs.setLoggedIn(true)
                    Toast.makeText(
                        applicationContext,
                        "Email verified successfully! Welcome to TailorBook.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                deepLinkRoute.value = Screen.ShopProfileConfig.route
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root app composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Root composable that wires the navigation drawer to the main scaffold.
 *
 * Phase 02 placeholder:
 *   - Drawer opens/closes with hamburger icon.
 *   - Tapping a drawer item closes the drawer (navigation wiring in Phase 03+).
 *   - Offline/sync banners are shown in preview mode (hardcoded state for now).
 *
 * Navigation wiring (Phase 03+):
 *   Replace [DashboardPreviewContent] with NavHost(...) and pass the
 *   NavController to the drawer's [onDestinationSelected] callback.
 */
@Composable
fun TailorBookApp(
    initialDeepLinkRoute: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefsManager = remember { com.tailorbook.core.datastore.UserPreferencesManager(context) }
    val shopName by prefsManager.shopNameFlow.collectAsState(initial = "My Boutique")
    val accountEmail by prefsManager.userEmailFlow.collectAsState(initial = null)
    val accountPhone by prefsManager.ownerPhoneFlow.collectAsState(initial = null)

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(initialDeepLinkRoute) {
        if (!initialDeepLinkRoute.isNullOrBlank()) {
            navController.navigate(initialDeepLinkRoute) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    var selectedDestination by remember { com.tailorbook.ui.components.DrawerDestination.Dashboard.let { mutableStateOf(it) } }
    var showHelpSupportDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDrawerGestureEnabled = currentRoute == Screen.Dashboard.route ||
            currentRoute == Screen.CustomerList.route ||
            currentRoute == Screen.DesignGallery.route ||
            currentRoute == Screen.OrderList.route ||
            currentRoute == Screen.DeliveryCalendar.route ||
            currentRoute == Screen.PaymentList.route ||
            currentRoute == Screen.Reports.route ||
            currentRoute == Screen.StaffList.route ||
            currentRoute == Screen.Subscription.route ||
            currentRoute?.startsWith("measurement_templates") == true

    com.tailorbook.ui.components.TailorNavigationDrawer(
        drawerState = drawerState,
        selectedDestination = selectedDestination,
        shopName = shopName,
        accountEmail = accountEmail,
        accountPhone = accountPhone,
        notificationBadgeCount = 0,
        gesturesEnabled = isDrawerGestureEnabled,
        onDestinationSelected = { destination ->
            selectedDestination = destination
            scope.launch { drawerState.close() }
            when (destination) {
                com.tailorbook.ui.components.DrawerDestination.Dashboard -> {
                    if (currentRoute != Screen.Dashboard.route) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.Customers -> {
                    if (currentRoute != Screen.CustomerList.route) {
                        navController.navigate(Screen.CustomerList.route)
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.GarmentTemplates -> {
                    navController.navigate(Screen.GarmentTemplateSelector.createRoute(customerId = ""))
                }
                com.tailorbook.ui.components.DrawerDestination.Measurements -> {
                    navController.navigate(Screen.SelectCustomerForMeasurement.route)
                }
                com.tailorbook.ui.components.DrawerDestination.Orders -> {
                    if (currentRoute != Screen.OrderList.route) {
                        navController.navigate(Screen.OrderList.route)
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.DeliveryCalendar -> {
                    if (currentRoute != Screen.DeliveryCalendar.route) {
                        navController.navigate(Screen.DeliveryCalendar.route)
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.Payments -> {
                    if (currentRoute != Screen.PaymentList.route) {
                        navController.navigate(Screen.PaymentList.route)
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.DesignGallery -> {
                    if (currentRoute != Screen.DesignGallery.route) {
                        navController.navigate(Screen.DesignGallery.route)
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.Reports -> {
                    if (currentRoute != Screen.Reports.route) {
                        navController.navigate(Screen.Reports.route)
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.StaffManagement -> {
                    if (currentRoute != Screen.StaffList.route) {
                        navController.navigate(Screen.StaffList.route)
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.Subscription -> {
                    if (currentRoute != Screen.Subscription.route) {
                        navController.navigate(Screen.Subscription.route)
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.Settings -> {
                    if (currentRoute != Screen.ShopProfileConfig.route) {
                        navController.navigate(Screen.ShopProfileConfig.route)
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.Notifications -> {
                    if (currentRoute != Screen.Notifications.route) {
                        navController.navigate(Screen.Notifications.route)
                    }
                }
                com.tailorbook.ui.components.DrawerDestination.HelpSupport -> {
                    showHelpSupportDialog = true
                }
            }
        }
    ) {
        TailorBookNavGraph(
            navController = navController,
            onMenuClick = { scope.launch { drawerState.open() } },
            preferencesManager = prefsManager
        )

        if (showHelpSupportDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showHelpSupportDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.help_support_dialog_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.help_support_dialog_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Charcoal
                        )

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                showHelpSupportDialog = false
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("https://wa.me/919876543210?text=Hi%20TailorBook%20Support%2C%20I%20need%20help%20with%20my%20boutique%20app.")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MutedSage)
                        ) {
                            Text(
                                text = stringResource(R.string.help_support_btn_whatsapp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                showHelpSupportDialog = false
                                navController.navigate(Screen.Onboarding.route)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MutedSage)
                        ) {
                            Text(
                                text = stringResource(R.string.help_support_btn_guide),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showHelpSupportDialog = false }) {
                        Text(stringResource(R.string.help_support_btn_close), color = DeepTeal, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CardWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Scaffold with Top App Bar + content area
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TailorBookScaffold(
    onMenuClick: () -> Unit,
    isOffline: Boolean = false,
    isSyncing: Boolean = false,
    hasPendingChanges: Boolean = false
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = CardWhite
                            )
                            Text(
                                text = stringResource(id = R.string.app_tagline),
                                style = MaterialTheme.typography.labelSmall,
                                color = LightBeige
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open navigation menu",
                                tint = CardWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DeepTeal
                    )
                )
                // Offline / sync banners directly below the app bar
                TailorOfflineBanner(isOffline = isOffline)
                TailorSyncBanner(isSyncing = isSyncing, hasPendingChanges = hasPendingChanges)
            }
        },
        containerColor = WarmIvory
    ) { innerPadding ->
        DashboardPreviewContent(
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dashboard preview content
// (Temporary placeholder — will be replaced by the real Dashboard in Phase 04)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Temporary dashboard preview that showcases the Phase 02 design system
 * components in a realistic layout.
 *
 * This composable will be REPLACED entirely in Phase 04 by the real Dashboard
 * ViewModel + composable. Do not add business logic here.
 */
@Composable
fun DashboardPreviewContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Welcome card ──────────────────────────────────────────────────
        TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Welcome to TailorBook",
                    style = MaterialTheme.typography.titleLarge,
                    color = Charcoal,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Phase 02 — Design System & Components ready. " +
                           "Offline-first, Telugu bilingual, premium boutique aesthetic.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalSubtle
                )
            }
        }

        // ── Metrics row ───────────────────────────────────────────────────
        Text(
            text = "Today's Overview",
            style = MaterialTheme.typography.titleMedium,
            color = Charcoal,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label = "Active Orders",
                value = "0",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Today's Deliveries",
                value = "0",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label = "Customers",
                value = "0",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Outstanding (₹)",
                value = "0",
                modifier = Modifier.weight(1f)
            )
        }

        // ── Quick actions ─────────────────────────────────────────────────
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            color = Charcoal,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(title = "New Customer", icon = Icons.Default.People, modifier = Modifier.weight(1f))
            QuickActionCard(title = "New Order", icon = Icons.Default.ContentCut, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(title = "Measurements", icon = Icons.Default.Straighten, modifier = Modifier.weight(1f))
            QuickActionCard(title = "Record Payment", icon = Icons.Default.Add, modifier = Modifier.weight(1f))
        }

        // ── Design system showcase ─────────────────────────────────────────
        Text(
            text = "Design System — Phase 02",
            style = MaterialTheme.typography.titleMedium,
            color = Charcoal,
            fontWeight = FontWeight.SemiBold
        )
        TailorBorderedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("✅ Color tokens — complete", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                TailorDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text("✅ Typography — Roboto + Noto Sans Telugu", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                TailorDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text("✅ TailorButton, TailorTextField, TailorCard", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                TailorDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text("✅ Navigation Drawer — 13 destinations", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                TailorDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text("✅ Loading, Empty, Error, Offline, Sync states", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // FAB clearance
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared mini-components (local to MainActivity for Phase 02)
// Phase 04 will move these to the Dashboard feature module.
// ─────────────────────────────────────────────────────────────────────────────

/** KPI metric card shown in the dashboard overview row. */
@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    TailorElevatedCard(modifier = modifier, elevation = 1.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = DeepTeal,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = CharcoalSubtle
            )
        }
    }
}

/** Quick action tile with icon + label. */
@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    TailorElevatedCard(
        modifier = modifier,
        elevation = 1.dp,
        onClick = { /* TODO Phase 04: navigate to respective screen */ }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DeepTeal.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = DeepTeal
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = Charcoal
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TailorBookScaffoldPreview() {
    TailorBookTheme {
        TailorBookScaffold(onMenuClick = {}, isOffline = true)
    }
}
