package com.tailorbook.feature.customers.presentation

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.core.ads.AdPlacement
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import com.tailorbook.ui.components.TailorAdaptiveBannerAd
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.WarmIvory

/**
 * Screen enabling tailors to choose which customer (or family member) to measure.
 * Guarantees measurements are always scoped strictly to individual customer profiles.
 */
@Composable
fun SelectCustomerForMeasurementScreen(
    viewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    onCustomerSelected: (customerId: String, familyMemberId: String?) -> Unit,
    onAddNewCustomerClick: () -> Unit,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    val allCustomers by viewModel.filteredCustomers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val displayedCustomers = remember(allCustomers, searchQuery) {
        if (searchQuery.isBlank()) {
            allCustomers
        } else {
            val q = searchQuery.trim().lowercase()
            allCustomers.filter {
                it.customer.name.lowercase().contains(q) ||
                        it.customer.primaryPhone.contains(q) ||
                        it.familyMembers.any { member -> member.name.lowercase().contains(q) }
            }
        }
    }

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back),
                            tint = CardWhite
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Customer",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = "Choose client to take or view measurements",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search customer by name or phone...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CharcoalSubtle
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = DeepTeal)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
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
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // New Customer Quick Action Card
            item {
                Surface(
                    color = CardWhite,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onAddNewCustomerClick)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(LightBeige, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "+ Add New Customer",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                            Text(
                                text = "Register new client & record initial measurements",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalSubtle
                            )
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = SoftGold)
                    }
                }
            }

            // Customer List
            items(displayedCustomers, key = { it.customer.id }) { item ->
                CustomerMeasurementCard(
                    item = item,
                    onCustomerClick = { onCustomerSelected(item.customer.id, null) },
                    onFamilyMemberClick = { memberId -> onCustomerSelected(item.customer.id, memberId) }
                )
            }

            // Anchored Adaptive Banner Ad (Measurements placement)
            item {
                TailorAdaptiveBannerAd(
                    placement = AdPlacement.MEASUREMENTS,
                    isPremium = isPremium
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CustomerMeasurementCard(
    item: CustomerWithFamilyMembers,
    onCustomerClick: () -> Unit,
    onFamilyMemberClick: (String) -> Unit
) {
    TailorElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCustomerClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Initials circle
                val initials = item.customer.name.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(DeepTeal.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Charcoal
                    )
                    Text(
                        text = "Phone: ${item.customer.primaryPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalSubtle
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SoftGold
                )
            }

            // Family Members quick chips
            if (item.familyMembers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 58.dp)
                ) {
                    Text(
                        text = "Family:",
                        style = MaterialTheme.typography.labelSmall,
                        color = CharcoalSubtle,
                        fontSize = 11.sp
                    )
                    item.familyMembers.forEach { member ->
                        Surface(
                            color = LightBeige,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onFamilyMemberClick(member.id) }
                        ) {
                            Text(
                                text = "${member.name} (${member.relationship})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = DeepTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
