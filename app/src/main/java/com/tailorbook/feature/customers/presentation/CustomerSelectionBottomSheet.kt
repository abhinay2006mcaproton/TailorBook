package com.tailorbook.feature.customers.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold

/**
 * Bottom Sheet allowing tailors to select which customer to measure or create an order for.
 * Ensures measurements are always bound to a specific customer profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSelectionBottomSheet(
    viewModel: CustomerViewModel,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onCustomerSelected: (customerId: String, familyMemberId: String?) -> Unit,
    onAddNewCustomerClick: () -> Unit,
    title: String = "Select Customer for Measurements"
) {
    val allCustomers by viewModel.filteredCustomers.collectAsState()
    var localSearch by remember { mutableStateOf("") }

    val displayedCustomers = remember(allCustomers, localSearch) {
        if (localSearch.isBlank()) {
            allCustomers
        } else {
            val q = localSearch.trim().lowercase()
            allCustomers.filter {
                it.customer.name.lowercase().contains(q) ||
                        it.customer.primaryPhone.contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = DeepTeal
            )
            Text(
                text = "Each customer has their own individual measurement versions",
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalSubtle
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = localSearch,
                onValueChange = { localSearch = it },
                placeholder = { Text("Search by name or phone...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = DeepTeal)
                },
                trailingIcon = {
                    if (localSearch.isNotEmpty()) {
                        IconButton(onClick = { localSearch = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = CharcoalSubtle)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Add New Customer Action Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightBeige)
                    .clickable {
                        onDismissRequest()
                        onAddNewCustomerClick()
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(DeepTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = CardWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "+ Add New Customer",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                    Text(
                        text = "Register a new client & take initial measurements",
                        style = MaterialTheme.typography.labelSmall,
                        color = CharcoalSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedCustomers, key = { it.customer.id }) { item ->
                    CustomerPickerItem(
                        item = item,
                        onClick = {
                            onDismissRequest()
                            onCustomerSelected(item.customer.id, null)
                        },
                        onFamilyMemberClick = { memberId ->
                            onDismissRequest()
                            onCustomerSelected(item.customer.id, memberId)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CustomerPickerItem(
    item: CustomerWithFamilyMembers,
    onClick: () -> Unit,
    onFamilyMemberClick: (String) -> Unit
) {
    Surface(
        color = CardWhite,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar circle
                val initials = item.customer.name.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(DeepTeal.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.customer.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Charcoal
                    )
                    Text(
                        text = item.customer.primaryPhone,
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

            // If family members exist, display quick chips to measure them directly
            if (item.familyMembers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 52.dp)
                ) {
                    Text(
                        text = "Family:",
                        style = MaterialTheme.typography.labelSmall,
                        color = CharcoalSubtle,
                        fontSize = 10.sp
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
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = DeepTeal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
