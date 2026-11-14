package com.tailorbook.feature.orders.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.core.ads.AdPlacement
import com.tailorbook.feature.customers.presentation.CustomerViewModel
import com.tailorbook.feature.orders.data.local.entity.OrderWithDetails
import com.tailorbook.ui.components.TailorAdaptiveBannerAd
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorEmptyState
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.WarmIvory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Order List Screen (TASK-08-04 / REQ-ORD-01).
 * Displays order cards filtered by status tabs (All, New, In Progress, Ready, Delivered).
 */
@Composable
fun OrderListScreen(
    viewModel: OrderViewModel,
    customerViewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    onNewOrderClick: () -> Unit,
    shopName: String = "My Boutique",
    isPremium: Boolean = false,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.filteredOrders.collectAsState()
    val selectedTab by viewModel.selectedStatusTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val customers by customerViewModel.filteredCustomers.collectAsState()

    val tabs = listOf(
        "ALL" to stringResource(id = R.string.orders_tab_all),
        "NEW" to stringResource(id = R.string.orders_tab_new),
        "IN_PROGRESS" to stringResource(id = R.string.orders_tab_in_progress),
        "READY" to stringResource(id = R.string.orders_tab_ready),
        "DELIVERED" to stringResource(id = R.string.orders_tab_delivered)
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepTeal)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.cd_navigation_menu),
                                tint = CardWhite
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.action_back),
                                tint = CardWhite
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.orders_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = "Active workflows & delivery schedules",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }
                }

                // Search field
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "Search by order #, garment, customer...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CharcoalSubtle
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = DeepTeal)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = CharcoalSubtle)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .background(CardWhite, RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewOrderClick,
                containerColor = DeepTeal,
                contentColor = CardWhite,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.order_create_title)
                )
            }
        },
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Status Filter Tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs) { (key, label) ->
                    val isSelected = (key == selectedTab)
                    Surface(
                        color = if (isSelected) DeepTeal else CardWhite,
                        contentColor = if (isSelected) CardWhite else Charcoal,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = if (isSelected) 3.dp else 1.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.selectStatusTab(key) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Orders list
            if (orders.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    TailorEmptyState(
                        icon = Icons.Default.ContentCut,
                        title = stringResource(id = R.string.order_no_orders),
                        subtitle = stringResource(id = R.string.order_no_orders_desc),
                        actionLabel = stringResource(id = R.string.order_create_title),
                        onAction = onNewOrderClick,
                        modifier = Modifier.weight(1f)
                    )
                    TailorAdaptiveBannerAd(
                        placement = AdPlacement.ORDERS,
                        isPremium = isPremium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(orders, key = { it.order.id }) { item ->
                        val customer = customers.find { it.customer.id == item.order.customerId }
                        val customerName = customer?.customer?.name ?: "Customer"
                        val customerPhone = customer?.customer?.primaryPhone ?: ""

                        OrderCard(
                            item = item,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            shopName = shopName,
                            onClick = { onOrderClick(item.order.id) },
                            onAdvanceStatus = { nextStatus ->
                                viewModel.advanceOrderStatus(item.order.id, item.order.status, nextStatus)
                            }
                        )
                    }

                    // Anchored Adaptive Banner Ad (Orders List placement)
                    item {
                        TailorAdaptiveBannerAd(
                            placement = AdPlacement.ORDERS,
                            isPremium = isPremium
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

/**
 * Responsive Order Card matching boutique design system.
 */
@Composable
fun OrderCard(
    item: OrderWithDetails,
    customerName: String,
    customerPhone: String,
    onClick: () -> Unit,
    onAdvanceStatus: (String) -> Unit,
    shopName: String = "My Boutique",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val order = item.order
    val primaryItem = item.items.firstOrNull()

    val statusBadgeColor = when (order.status) {
        "READY" -> StatusGreen
        "CUTTING", "STITCHING", "FINISHING" -> SoftGold
        "DELIVERED" -> CharcoalSubtle
        "CANCELLED" -> StatusRed
        else -> DeepTeal
    }

    TailorElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Order Number and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.orderNumber}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )

                Surface(
                    color = statusBadgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = order.status,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusBadgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Customer Name & Garment item
            Text(
                text = customerName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Charcoal
            )
            if (primaryItem != null) {
                Text(
                    text = "${primaryItem.garmentName} • Qty: ${primaryItem.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalSubtle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delivery Date & Balance Due
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val deliveryDateStr = remember(order.promisedDeliveryDate) {
                    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.promisedDeliveryDate))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Due: $deliveryDateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalSubtle
                    )
                }

                Text(
                    text = if (order.balanceAmount > 0) "Due: ₹${order.balanceAmount.toInt()}" else "Fully Paid",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (order.balanceAmount > 0) StatusRed else StatusGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Actions: Call, WhatsApp, Status Advance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Call Button
                    Surface(
                        color = LightBeige,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (customerPhone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$customerPhone"))
                                    context.startActivity(intent)
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = DeepTeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Call", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DeepTeal)
                        }
                    }

                    // WhatsApp Button with Full Bill Details
                    Surface(
                        color = LightBeige,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val billText = OrderShareHelper.buildOrderShareText(
                                    shopName = shopName,
                                    orderNumber = order.orderNumber,
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    status = order.status,
                                    deliveryDateMs = order.promisedDeliveryDate,
                                    items = item.items,
                                    totalAmount = order.totalAmount,
                                    paidAmount = order.paidAmount,
                                    balanceAmount = order.balanceAmount,
                                    includeMeasurements = false
                                )
                                OrderShareHelper.shareViaWhatsApp(context, billText, customerPhone)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", tint = StatusGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "WhatsApp", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = StatusGreen)
                        }
                    }

                    // Share Button (Any App)
                    Surface(
                        color = LightBeige,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val billText = OrderShareHelper.buildOrderShareText(
                                    shopName = shopName,
                                    orderNumber = order.orderNumber,
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    status = order.status,
                                    deliveryDateMs = order.promisedDeliveryDate,
                                    items = item.items,
                                    totalAmount = order.totalAmount,
                                    paidAmount = order.paidAmount,
                                    balanceAmount = order.balanceAmount,
                                    includeMeasurements = false
                                )
                                OrderShareHelper.shareViaSystemSheet(context, billText, "$shopName Order #${order.orderNumber}")
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = DeepTeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Share", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DeepTeal)
                        }
                    }
                }

                // Quick Status Stepper Action Button
                val nextStatus = when (order.status) {
                    "NEW" -> "CUTTING" to "Start Cutting"
                    "CUTTING" -> "STITCHING" to "Start Stitch"
                    "STITCHING" -> "FINISHING" to "Finishing"
                    "FINISHING" -> "READY" to "Mark Ready"
                    "READY" -> "DELIVERED" to "Mark Delivered"
                    else -> null
                }

                if (nextStatus != null) {
                    Surface(
                        color = DeepTeal,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAdvanceStatus(nextStatus.first) }
                    ) {
                        Text(
                            text = nextStatus.second,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
