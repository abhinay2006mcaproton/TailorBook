package com.tailorbook.feature.staff.presentation

import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.feature.staff.data.local.entity.StaffEntity
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorEmptyState
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

/**
 * Staff Team Management Screen (TASK-12-03 / REQ-STAF-01 / REQ-STAF-02).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffListScreen(
    viewModel: StaffViewModel,
    onMenuClick: () -> Unit,
    isPremium: Boolean = false,
    onNavigateToUpgrade: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val staffList by viewModel.filteredStaff.collectAsState()
    val selectedFilter by viewModel.selectedRoleFilter.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showStaffLimitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.staff_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = stringResource(R.string.staff_subtitle),
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
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.staff_add),
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isPremium && staffList.size >= com.tailorbook.feature.subscription.domain.EntitlementConstants.FREE_STAFF_LIMIT) {
                        showStaffLimitDialog = true
                    } else {
                        showAddDialog = true
                    }
                },
                containerColor = DeepTeal,
                contentColor = CardWhite,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.staff_add)
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
            val roleFilters = listOf(
                "ALL" to "All",
                "OWNER" to stringResource(R.string.staff_role_owner),
                "MANAGER" to stringResource(R.string.staff_role_manager),
                "TAILOR" to stringResource(R.string.staff_role_tailor),
                "WORKER" to stringResource(R.string.staff_role_worker)
            )

            // Role Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(roleFilters) { (roleKey, label) ->
                    val isSelected = selectedFilter == roleKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectRoleFilter(roleKey) },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepTeal,
                            selectedLabelColor = CardWhite,
                            containerColor = CardWhite,
                            labelColor = Charcoal
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) DeepTeal else MutedSage
                        )
                    )
                }
            }

            if (staffList.isEmpty()) {
                TailorEmptyState(
                    icon = Icons.Default.Groups,
                    title = stringResource(R.string.staff_empty),
                    subtitle = stringResource(R.string.staff_empty_desc),
                    actionLabel = stringResource(R.string.staff_add),
                    onAction = { showAddDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(staffList, key = { it.id }) { member ->
                        StaffMemberCard(
                            member = member,
                            onToggleStatus = {
                                viewModel.toggleStaffStatus(member.id, member.isActive)
                            },
                            onDelete = {
                                viewModel.deleteStaff(member.id)
                            },
                            onCallClick = { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            },
                            onWhatsAppClick = { phone, name ->
                                val text = "Hello $name! Message from boutique management."
                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=91$phone&text=${Uri.encode(text)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddStaffDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, phone, role, notes ->
                    viewModel.addStaff(name, phone, role, notes) {
                        showAddDialog = false
                    }
                }
            )
        }

        if (showStaffLimitDialog) {
            AlertDialog(
                onDismissRequest = { showStaffLimitDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.entitlement_staff_limit_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.entitlement_staff_limit_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showStaffLimitDialog = false
                            onNavigateToUpgrade()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                    ) {
                        Text(stringResource(R.string.entitlement_btn_upgrade), color = CardWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStaffLimitDialog = false }) {
                        Text(stringResource(R.string.entitlement_btn_maybe_later), color = CharcoalSubtle)
                    }
                },
                containerColor = CardWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun StaffMemberCard(
    member: StaffEntity,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onCallClick: (String) -> Unit,
    onWhatsAppClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (roleBg, roleFg, roleLabel) = when (member.role) {
        "OWNER" -> Triple(DeepTeal.copy(alpha = 0.12f), DeepTeal, stringResource(R.string.staff_role_owner))
        "MANAGER" -> Triple(SoftGold.copy(alpha = 0.2f), DeepTeal, stringResource(R.string.staff_role_manager))
        "TAILOR" -> Triple(StatusGreen.copy(alpha = 0.15f), StatusGreen, stringResource(R.string.staff_role_tailor))
        else -> Triple(LightBeige, Charcoal, stringResource(R.string.staff_role_worker))
    }

    TailorElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (member.isActive) LightBeige else MutedSage.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (member.isActive) DeepTeal else CharcoalSubtle,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (member.isActive) Charcoal else CharcoalSubtle
                        )
                        Text(
                            text = member.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalSubtle
                        )
                    }
                }

                // Role Badge & Status
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = roleBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = roleLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = roleFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (member.isActive) stringResource(R.string.staff_active) else stringResource(R.string.staff_inactive),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (member.isActive) StatusGreen else StatusRed
                    )
                }
            }

            if (!member.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = member.notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = CharcoalSubtle
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Communication & Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onCallClick(member.phone) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Call", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { onWhatsAppClick(member.phone, member.name) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusGreen)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = member.isActive,
                        onCheckedChange = { onToggleStatus() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CardWhite,
                            checkedTrackColor = DeepTeal,
                            uncheckedThumbColor = CharcoalSubtle,
                            uncheckedTrackColor = MutedSage.copy(alpha = 0.4f)
                        )
                    )

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Staff",
                            tint = StatusRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddStaffDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, role: String, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("TAILOR") }
    var notes by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.staff_add),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = DeepTeal
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.staff_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    isError = hasError && name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.staff_phone_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    isError = hasError && phone.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Assign Role & Permission Level:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )

                val roles = listOf(
                    "TAILOR" to stringResource(R.string.staff_role_tailor),
                    "MANAGER" to stringResource(R.string.staff_role_manager),
                    "WORKER" to stringResource(R.string.staff_role_worker),
                    "OWNER" to stringResource(R.string.staff_role_owner)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    roles.take(2).forEach { (roleKey, label) ->
                        val isSelected = role == roleKey
                        Surface(
                            color = if (isSelected) DeepTeal else LightBeige,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { role = roleKey }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) CardWhite else Charcoal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    roles.drop(2).forEach { (roleKey, label) ->
                        val isSelected = role == roleKey
                        Surface(
                            color = if (isSelected) DeepTeal else LightBeige,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { role = roleKey }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) CardWhite else Charcoal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Specialty & Notes (Optional)") },
                    placeholder = { Text("e.g. Master cutter, specialist in bridal blouses") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TailorPrimaryButton(
                text = stringResource(R.string.staff_save_btn),
                onClick = {
                    if (name.isBlank() || phone.isBlank()) {
                        hasError = true
                    } else {
                        onSave(name, phone, role, notes)
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = CharcoalSubtle
                )
            }
        },
        containerColor = CardWhite,
        shape = RoundedCornerShape(16.dp)
    )
}
