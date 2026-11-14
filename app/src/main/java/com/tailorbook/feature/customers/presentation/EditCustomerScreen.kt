package com.tailorbook.feature.customers.presentation

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tailorbook.R
import com.tailorbook.core.util.FormattingUtils
import com.tailorbook.core.util.HapticHelper
import com.tailorbook.ui.components.TailorLoadingState
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.WarmIvory

/**
 * Edit Customer Screen (TASK-05-05).
 * Modifies an existing customer profile with validation.
 */
@Composable
fun EditCustomerScreen(
    customerId: String,
    viewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    onCustomerUpdated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.filteredCustomers.collectAsState()
    val item = customers.find { it.customer.id == customerId }
    val focusManager = LocalFocusManager.current

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

    var name by remember(customer.id) { mutableStateOf(customer.name) }
    var phone by remember(customer.id) { mutableStateOf(customer.primaryPhone) }
    var altPhone by remember(customer.id) { mutableStateOf(customer.secondaryPhone ?: "") }
    var selectedGender by remember(customer.id) {
        mutableStateOf(GenderOption.values().find { it.code == customer.gender } ?: GenderOption.FEMALE)
    }
    var address by remember(customer.id) { mutableStateOf(customer.address ?: "") }
    var notes by remember(customer.id) { mutableStateOf(customer.notes ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val errorNameMsg = stringResource(id = R.string.customers_error_name)
    val errorPhoneMsg = stringResource(id = R.string.customers_error_phone)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    fun handleUpdate() {
        val phoneRegex = Regex("^[6-9]\\d{9}$")
        var isValid = true

        if (name.trim().length < 2) {
            nameError = errorNameMsg
            isValid = false
        } else {
            nameError = null
        }

        if (!phone.trim().matches(phoneRegex)) {
            phoneError = errorPhoneMsg
            isValid = false
        } else {
            phoneError = null
        }

        if (isValid) {
            focusManager.clearFocus()
            HapticHelper.click(context, haptic)
            val cleanName = FormattingUtils.formatPersonName(name)
            viewModel.updateCustomer(
                customerId = customer.id,
                name = cleanName,
                phone = phone.trim(),
                altPhone = altPhone.trim().ifBlank { null },
                gender = selectedGender.code,
                address = address.trim().ifBlank { null },
                notes = notes.trim().ifBlank { null },
                onSuccess = onCustomerUpdated
            )
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepTeal)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.action_back),
                        tint = CardWhite
                    )
                }

                Text(
                    text = stringResource(id = R.string.customer_edit_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = CardWhite
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                TailorPrimaryButton(
                    text = stringResource(id = R.string.customer_edit_btn_save),
                    onClick = { handleUpdate() },
                    modifier = Modifier.fillMaxWidth()
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Full Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError != null && it.trim().length >= 2) nameError = null
                },
                label = { Text(stringResource(id = R.string.customers_field_name)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = DeepTeal)
                },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = StatusRed) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = MutedSage,
                    focusedLabelColor = DeepTeal,
                    errorBorderColor = StatusRed
                )
            )

            // Primary Phone
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    if (it.length <= 10 && it.all { ch -> ch.isDigit() }) {
                        phone = it
                        if (phoneError != null && it.length == 10) phoneError = null
                    }
                },
                label = { Text(stringResource(id = R.string.customers_field_phone)) },
                prefix = { Text("+91 ") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = DeepTeal)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it, color = StatusRed) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = MutedSage,
                    focusedLabelColor = DeepTeal,
                    errorBorderColor = StatusRed
                )
            )

            // Secondary Phone
            OutlinedTextField(
                value = altPhone,
                onValueChange = {
                    if (it.length <= 10 && it.all { ch -> ch.isDigit() }) altPhone = it
                },
                label = { Text(stringResource(id = R.string.customers_field_alt_phone)) },
                prefix = { Text("+91 ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = MutedSage,
                    focusedLabelColor = DeepTeal
                )
            )

            // Gender Selector Chips
            Column {
                Text(
                    text = stringResource(id = R.string.customers_field_gender),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Charcoal
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GenderOption.values().forEach { gender ->
                        val isSelected = selectedGender == gender
                        val bg = if (isSelected) DeepTeal else WarmIvory
                        val border = if (isSelected) DeepTeal else MutedSage
                        val textCol = if (isSelected) WarmIvory else Charcoal

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable { selectedGender = gender }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = gender.labelRes),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = textCol
                            )
                        }
                    }
                }
            }

            // Address
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(id = R.string.customers_field_address)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = DeepTeal)
                },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = MutedSage,
                    focusedLabelColor = DeepTeal
                )
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(id = R.string.customers_field_notes)) },
                leadingIcon = {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = DeepTeal)
                },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = MutedSage,
                    focusedLabelColor = DeepTeal
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
