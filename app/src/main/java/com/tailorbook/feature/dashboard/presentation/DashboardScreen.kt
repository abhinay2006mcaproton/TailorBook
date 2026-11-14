package com.tailorbook.feature.dashboard.presentation

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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tailorbook.R
import com.tailorbook.feature.dashboard.domain.model.DashboardUiState
import com.tailorbook.feature.dashboard.presentation.components.DashboardMetricGrid
import com.tailorbook.feature.dashboard.presentation.components.QuickActionGrid
import com.tailorbook.feature.dashboard.presentation.components.TodayDeliveriesSection
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorLoadingState
import com.tailorbook.core.ads.AdPlacement
import com.tailorbook.ui.components.TailorAdaptiveBannerAd
import com.tailorbook.ui.components.TailorOfflineBanner
import com.tailorbook.ui.components.TailorSyncBanner
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.WarmIvory

/**
 * Complete Dashboard Screen (TASK-04-05).
 * Combines Metric Cards, Quick Actions, Today's Deliveries, and offline/sync banners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onMenuClick: () -> Unit,
    shopName: String = "My Boutique",
    onNewCustomerClick: () -> Unit = {},
    onNewOrderClick: () -> Unit = {},
    onNewMeasurementClick: () -> Unit = {},
    onRecordPaymentClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMetricClick: (String) -> Unit = {},
    isPremium: Boolean = false,
    dashboardViewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by dashboardViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = shopName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = stringResource(id = R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = CardWhite.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(id = R.string.cd_navigation_menu),
                            tint = CardWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(id = R.string.nav_notifications),
                            tint = CardWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepTeal,
                    titleContentColor = CardWhite,
                    navigationIconContentColor = CardWhite,
                    actionIconContentColor = CardWhite
                )
            )
        },
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                TailorLoadingState(modifier = Modifier.padding(innerPadding))
            }
            is DashboardUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = DeepTeal)
                }
            }
            is DashboardUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Sync and Offline Banners (Phase 02 / REQ-DASH-04)
                    TailorOfflineBanner(isOffline = state.isOffline)
                    TailorSyncBanner(isSyncing = state.isSyncing)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Quick Actions Grid (REQ-DASH-02)
                        QuickActionGrid(
                            onNewCustomerClick = onNewCustomerClick,
                            onNewOrderClick = onNewOrderClick,
                            onNewMeasurementClick = onNewMeasurementClick,
                            onRecordPaymentClick = onRecordPaymentClick
                        )

                        // Metric Cards (REQ-DASH-01)
                        DashboardMetricGrid(
                            metrics = state.metrics,
                            onMetricClick = onMetricClick
                        )

                        // Anchored Adaptive Banner Ad (Approved placement: below summary cards & before secondary content)
                        TailorAdaptiveBannerAd(
                            placement = AdPlacement.DASHBOARD,
                            isPremium = isPremium
                        )

                        // Today's Deliveries (REQ-DASH-03)
                        TodayDeliveriesSection(
                            deliveries = state.todayDeliveries,
                            onOrderStatusChange = { orderId, newStatus ->
                                dashboardViewModel.updateOrderStatus(orderId, newStatus)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
