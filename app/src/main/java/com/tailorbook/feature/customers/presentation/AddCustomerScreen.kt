package com.tailorbook.feature.customers.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tailorbook.core.util.FormattingUtils
import com.tailorbook.core.util.HapticHelper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.core.ads.RewardedAdManager
import com.tailorbook.feature.subscription.domain.CreationDecision
import com.tailorbook.feature.subscription.domain.CustomerEntitlementEngine
import com.tailorbook.ui.components.TailorPrimaryButton
import kotlinx.coroutines.launch
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.WarmIvory

enum class GenderOption(val code: String, val labelRes: Int) {
    FEMALE("FEMALE", R.string.customers_gender_female),
    MALE("MALE", R.string.customers_gender_male),
    OTHER("OTHER", R.string.customers_gender_other)
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Add Customer Screen (TASK-05-04 / REQ-CUST-01).
 * Validates 10-digit Indian mobile regex, name length, and saves to repository.
 */
@Composable
fun AddCustomerScreen(
    onNavigateBack: () -> Unit,
    onCustomerCreated: (customerId: String) -> Unit,
    viewModel: CustomerViewModel,
    entitlementEngine: CustomerEntitlementEngine? = null,
    rewardedAdManager: RewardedAdManager? = null,
    onNavigateToUpgrade: () -> Unit = {},
    onWatchRewardedAd: (onSuccess: () -> Unit) -> Unit = { onSuccess -> onSuccess() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isAdLoading by rewardedAdManager?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        rewardedAdManager?.preloadAd(context)
    }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var altPhone by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(GenderOption.FEMALE) }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var showPremiumRequiredDialog by remember { mutableStateOf(false) }
    var pendingNextCustomerNumber by remember { mutableIntStateOf(11) }

    val errorNameMsg = stringResource(id = R.string.customers_error_name)
    val errorPhoneMsg = stringResource(id = R.string.customers_error_phone)
    val haptic = LocalHapticFeedback.current

    fun executeSave() {
        focusManager.clearFocus()
        HapticHelper.click(context, haptic)
        val cleanName = FormattingUtils.formatPersonName(name)
        viewModel.addCustomer(
            name = cleanName,
            phone = phone.trim(),
            altPhone = altPhone.trim().ifBlank { null },
            gender = selectedGender.code,
            address = address.trim().ifBlank { null },
            notes = notes.trim().ifBlank { null },
            onSuccess = { customerId ->
                onCustomerCreated(customerId)
            }
        )
    }

    fun handleSave() {
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
            if (entitlementEngine != null) {
                scope.launch {
                    val decision = entitlementEngine.evaluateCreationDecision()
                    when (decision) {
                        is CreationDecision.AllowedNormal -> {
                            executeSave()
                        }
                        is CreationDecision.AllowedUnlimited -> {
                            executeSave()
                        }
                        is CreationDecision.AllowedWithReward -> {
                            entitlementEngine.consumeRewardToken(decision.rewardToken)
                            executeSave()
                        }
                        is CreationDecision.RequiresRewardedAd -> {
                            pendingNextCustomerNumber = decision.nextCustomerNumber
                            showRewardedAdDialog = true
                        }
                        is CreationDecision.RequiresPremium -> {
                            showPremiumRequiredDialog = true
                        }
                    }
                }
            } else {
                executeSave()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    text = stringResource(id = R.string.customers_add_title),
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
                    text = stringResource(id = R.string.customers_btn_save),
                    onClick = { handleSave() },
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
                placeholder = { Text("10-digit mobile number") },
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

    // Rewarded Ad Dialog (11–50 customers)
    if (showRewardedAdDialog) {
        AlertDialog(
            onDismissRequest = { showRewardedAdDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.entitlement_unlock_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.entitlement_unlock_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Charcoal,
                        lineHeight = 20.sp
                    )
                )
            },
            confirmButton = {
                TailorPrimaryButton(
                    text = if (isAdLoading) stringResource(id = R.string.ad_loading) else stringResource(id = R.string.entitlement_btn_watch_ad),
                    onClick = {
                        showRewardedAdDialog = false
                        if (rewardedAdManager != null) {
                            val activity = context.findActivity()
                            if (activity != null) {
                                rewardedAdManager.showAd(
                                    activity = activity,
                                    onUserEarnedReward = {
                                        scope.launch {
                                            val token = entitlementEngine?.grantRewardToken()
                                            if (token != null) {
                                                val consumed = entitlementEngine.consumeRewardToken(token)
                                                if (consumed) {
                                                    executeSave()
                                                }
                                            } else {
                                                executeSave()
                                            }
                                        }
                                    },
                                    onAdFailed = { errorMsg ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = errorMsg,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                )
                            } else {
                                onWatchRewardedAd {
                                    scope.launch {
                                        val token = entitlementEngine?.grantRewardToken()
                                        if (token != null) {
                                            entitlementEngine.consumeRewardToken(token)
                                        }
                                        executeSave()
                                    }
                                }
                            }
                        } else {
                            onWatchRewardedAd {
                                scope.launch {
                                    val token = entitlementEngine?.grantRewardToken()
                                    if (token != null) {
                                        entitlementEngine.consumeRewardToken(token)
                                    }
                                    executeSave()
                                }
                            }
                        }
                    }
                )
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showRewardedAdDialog = false
                        onNavigateToUpgrade()
                    }) {
                        Text(
                            text = stringResource(id = R.string.entitlement_btn_upgrade),
                            color = DeepTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = { showRewardedAdDialog = false }) {
                        Text(
                            text = stringResource(id = R.string.entitlement_btn_not_now),
                            color = CharcoalSubtle
                        )
                    }
                }
            },
            containerColor = WarmIvory,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Limit Reached Dialog (51+ customers)
    if (showPremiumRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumRequiredDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.entitlement_limit_reached_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.entitlement_limit_reached_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Charcoal,
                        lineHeight = 20.sp
                    )
                )
            },
            confirmButton = {
                TailorPrimaryButton(
                    text = stringResource(id = R.string.entitlement_btn_upgrade),
                    onClick = {
                        showPremiumRequiredDialog = false
                        onNavigateToUpgrade()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showPremiumRequiredDialog = false }) {
                    Text(
                        text = stringResource(id = R.string.entitlement_btn_maybe_later),
                        color = CharcoalSubtle
                    )
                }
            },
            containerColor = WarmIvory,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
