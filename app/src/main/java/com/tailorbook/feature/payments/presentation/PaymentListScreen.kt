package com.tailorbook.feature.payments.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorPrimaryButton
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
 * Payments & Outstanding Balances ledger screen (TASK-10-04 / REQ-PAY-01).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentListScreen(
    viewModel: PaymentViewModel,
    shopName: String,
    onMenuClick: () -> Unit,
    onRecordPaymentClick: (orderId: String?, customerId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val totalOutstanding by viewModel.totalOutstanding.collectAsState()
    val totalCollected by viewModel.totalCollected.collectAsState()
    val customerBalances by viewModel.customerBalances.collectAsState()
    val paymentsWithDetails by viewModel.paymentsWithDetails.collectAsState()
    val activeReceipt by viewModel.activeReceipt.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.payments_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = stringResource(R.string.payments_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.cd_navigation_menu),
                            tint = CardWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepTeal)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onRecordPaymentClick(null, null) },
                containerColor = DeepTeal,
                contentColor = CardWhite,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record Payment")
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
            // Metrics Summary Cards
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Outstanding Balance Card
                TailorElevatedCard(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.payments_total_outstanding),
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalSubtle
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${totalOutstanding.toInt()}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (totalOutstanding > 0) StatusRed else StatusGreen
                        )
                    }
                }

                // Total Collected Card
                TailorElevatedCard(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.payments_total_collected),
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalSubtle
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${totalCollected.toInt()}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusGreen
                        )
                    }
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = if (selectedTab == "OUTSTANDING") 0 else 1,
                containerColor = CardWhite,
                contentColor = DeepTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (selectedTab == "OUTSTANDING") 0 else 1]),
                        color = DeepTeal,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == "OUTSTANDING",
                    onClick = { viewModel.setSelectedTab("OUTSTANDING") },
                    text = {
                        Text(
                            text = stringResource(R.string.payments_outstanding_list),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == "OUTSTANDING") FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                )
                Tab(
                    selected = selectedTab == "HISTORY",
                    onClick = { viewModel.setSelectedTab("HISTORY") },
                    text = {
                        Text(
                            text = stringResource(R.string.payments_recent_transactions),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == "HISTORY") FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                )
            }

            // Tab Content
            if (selectedTab == "OUTSTANDING") {
                if (customerBalances.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.payments_no_dues),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Charcoal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(customerBalances, key = { it.customer.id }) { balanceItem ->
                            CustomerBalanceCard(
                                balanceItem = balanceItem,
                                onRecordPayment = { orderId ->
                                    onRecordPaymentClick(orderId, balanceItem.customer.id)
                                },
                                onSendReminder = {
                                    viewModel.sendWhatsAppReminder(context, balanceItem, shopName)
                                }
                            )
                        }
                    }
                }
            } else {
                // Payment History Tab
                if (paymentsWithDetails.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.payments_no_transactions),
                                style = MaterialTheme.typography.bodyLarge,
                                color = CharcoalSubtle,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(paymentsWithDetails, key = { it.payment.id }) { paymentItem ->
                            PaymentTransactionCard(
                                item = paymentItem,
                                onShareReceipt = {
                                    val receipt = PaymentReceiptModel(
                                        receiptId = "RCP-${paymentItem.payment.id.take(6).uppercase()}",
                                        paymentDate = paymentItem.payment.paymentDate,
                                        amountPaid = paymentItem.payment.amount,
                                        paymentMethod = paymentItem.payment.paymentMethod,
                                        referenceNumber = paymentItem.payment.referenceNumber,
                                        customerName = paymentItem.customer?.name ?: "Customer",
                                        customerPhone = paymentItem.customer?.primaryPhone ?: "",
                                        orderNumber = paymentItem.order?.orderNumber,
                                        garmentDescription = null,
                                        totalOrderAmount = paymentItem.order?.totalAmount ?: paymentItem.payment.amount,
                                        remainingBalance = paymentItem.order?.balanceAmount ?: 0.0,
                                        notes = paymentItem.payment.notes
                                    )
                                    viewModel.shareReceipt(context, receipt, shopName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Active Receipt Dialog (after recording payment)
    if (activeReceipt != null) {
        val receipt = activeReceipt!!
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        AlertDialog(
            onDismissRequest = { viewModel.closeReceiptDialog() },
            title = {
                Text(
                    text = stringResource(R.string.payments_receipt_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )
            },
            text = {
                Surface(
                    color = LightBeige,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Receipt #${receipt.receiptId}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                            Text(
                                text = dateFormat.format(Date(receipt.paymentDate)),
                                style = MaterialTheme.typography.labelSmall,
                                color = CharcoalSubtle
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Client: ${receipt.customerName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Charcoal
                        )

                        if (receipt.orderNumber != null) {
                            Text(
                                text = "Order #${receipt.orderNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalSubtle
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Amount paid highlight
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Amount Paid:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Charcoal
                            )
                            Text(
                                text = "₹${receipt.amountPaid.toInt()}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = StatusGreen
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Payment Mode:",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalSubtle
                            )
                            Text(
                                text = receipt.paymentMethod,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Charcoal
                            )
                        }

                        if (!receipt.referenceNumber.isNullOrBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Ref / Txn ID:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CharcoalSubtle
                                )
                                Text(
                                    text = receipt.referenceNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Charcoal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Remaining Balance:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Charcoal
                            )
                            Text(
                                text = "₹${receipt.remainingBalance.toInt()}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (receipt.remainingBalance > 0) StatusRed else StatusGreen
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TailorPrimaryButton(
                    text = stringResource(R.string.payments_share_receipt),
                    onClick = { viewModel.shareReceipt(context, receipt, shopName) }
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeReceiptDialog() }) {
                    Text("Done", color = CharcoalSubtle)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Card displaying customer pending balances.
 */
@Composable
fun CustomerBalanceCard(
    balanceItem: CustomerOutstandingBalance,
    onRecordPayment: (orderId: String?) -> Unit,
    onSendReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstOrder = balanceItem.ordersWithBalance.firstOrNull()

    TailorElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = balanceItem.customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Charcoal
                    )
                    Text(
                        text = balanceItem.customer.primaryPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalSubtle
                    )
                }

                Text(
                    text = "Due: ₹${balanceItem.totalBalance.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StatusRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Order chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                balanceItem.ordersWithBalance.take(3).forEach { order ->
                    Surface(
                        color = LightBeige,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "#${order.orderNumber} (₹${order.balanceAmount.toInt()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Charcoal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSendReminder,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reminder", style = MaterialTheme.typography.labelSmall, color = StatusGreen)
                }

                TailorPrimaryButton(
                    text = "Record Payment",
                    onClick = { onRecordPayment(firstOrder?.id) },
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}

/**
 * Card displaying individual payment ledger entry.
 */
@Composable
fun PaymentTransactionCard(
    item: PaymentWithDetails,
    onShareReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    TailorElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = StatusGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.payment.paymentMethod,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (item.order != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Order #${item.order.orderNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DeepTeal
                        )
                    }
                }

                Text(
                    text = "+₹${item.payment.amount.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StatusGreen
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.customer?.name ?: "Customer",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Charcoal
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = dateFormat.format(Date(item.payment.paymentDate)),
                    style = MaterialTheme.typography.labelSmall,
                    color = CharcoalSubtle
                )

                IconButton(
                    onClick = onShareReceipt,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Receipt",
                        tint = DeepTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
