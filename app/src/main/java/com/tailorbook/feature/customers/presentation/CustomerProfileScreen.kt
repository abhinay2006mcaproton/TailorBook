package com.tailorbook.feature.customers.presentation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.tailorbook.core.ads.RewardedAdManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorLoadingState
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.components.TailorSecondaryButton
import com.tailorbook.ui.components.TailorSectionCard
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.WarmIvory

/**
 * Customer Profile Screen (TASK-05-06 / REQ-CUST-03 / REQ-FAM-01).
 * Displays full contact card, metrics, linked family members, and measurement shortcuts.
 */
@Composable
fun CustomerProfileScreen(
    customerId: String,
    viewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    onEditCustomerClick: (customerId: String) -> Unit,
    onNewOrderClick: (customerId: String) -> Unit = {},
    onNewMeasurementClick: (customerId: String) -> Unit = {},
    isPremium: Boolean = false,
    rewardedAdManager: RewardedAdManager? = null,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.filteredCustomers.collectAsState()
    val item = customers.find { it.customer.id == customerId }
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        rewardedAdManager?.preloadAd(context)
    }

    var showArchiveDialog by remember { mutableStateOf(false) }

    if (customers.isEmpty()) {
        TailorLoadingState()
        return
    }

    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmIvory),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Customer record not found", color = Charcoal)
        }
        return
    }

    val customer = item.customer
    val initial = customer.name.take(1).uppercase()

    // Archive Customer Confirmation Dialog
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.customer_profile_delete_dialog_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StatusRed
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.customer_profile_delete_dialog_msg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveDialog = false
                        viewModel.archiveCustomer(customer.id) {
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(text = "Archive", color = StatusRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text(text = "Cancel", color = CharcoalSubtle)
                }
            },
            containerColor = CardWhite
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepTeal)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back),
                            tint = CardWhite
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.customer_profile_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = CardWhite
                    )
                }

                IconButton(onClick = { onEditCustomerClick(customer.id) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(id = R.string.customer_profile_edit),
                        tint = SoftGold
                    )
                }
            }
        },
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Contact Card
            TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(DeepTeal.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = DeepTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Charcoal
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "+91 ${customer.primaryPhone}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharcoalSubtle
                    )

                    if (!customer.address.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = customer.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalSubtle
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Direct Action Buttons: Call & WhatsApp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Call
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeepTeal.copy(alpha = 0.08f))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${customer.primaryPhone}")
                                    }
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = DeepTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Call",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = DeepTeal
                            )
                        }

                        // WhatsApp
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusGreen.copy(alpha = 0.12f))
                                .clickable {
                                    val url = "https://api.whatsapp.com/send?phone=+91${customer.primaryPhone}&text=${Uri.encode("Hello ${customer.name}, greetings from TailorBook boutique!")}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "WhatsApp",
                                tint = StatusGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WhatsApp",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = StatusGreen
                            )
                        }
                    }
                }
            }

            // Summary Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Orders
                TailorElevatedCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Orders",
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalSubtle
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "3",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DeepTeal
                        )
                    }
                }

                // Lifetime Value
                TailorElevatedCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Lifetime",
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalSubtle
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹4,200",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Charcoal
                        )
                    }
                }

                // Outstanding Balance
                TailorElevatedCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Due Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalSubtle
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹450",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = StatusRed
                        )
                    }
                }
            }

            // Family Members Section (if any existing linked members)
            if (item.familyMembers.isNotEmpty()) {
                TailorSectionCard(
                    title = stringResource(id = R.string.customer_profile_family_title),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item.familyMembers.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(SoftGold.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = SoftGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Charcoal
                                    )
                                    Text(
                                        text = member.relationship,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CharcoalSubtle
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.removeFamilyMember(customer.id, member.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = CharcoalSubtle,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions: New Order & Measurements
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TailorPrimaryButton(
                    text = stringResource(id = R.string.customer_profile_action_new_order),
                    onClick = { onNewOrderClick(customer.id) },
                    modifier = Modifier.weight(1f)
                )

                TailorSecondaryButton(
                    text = stringResource(id = R.string.customer_profile_action_new_measure),
                    onClick = { onNewMeasurementClick(customer.id) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Archive Customer Option (REQ-CUST-05)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showArchiveDialog = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.customer_profile_delete_btn),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = StatusRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
