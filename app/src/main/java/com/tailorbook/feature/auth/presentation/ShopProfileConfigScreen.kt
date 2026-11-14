package com.tailorbook.feature.auth.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tailorbook.R
import com.tailorbook.core.ads.AdPlacement
import com.tailorbook.core.ads.ConsentManager
import com.tailorbook.core.ads.RewardedAdManager
import com.tailorbook.ui.components.TailorAdaptiveBannerAd
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.components.TailorSectionCard
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

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

enum class MeasurementUnit(val code: String, val labelRes: Int) {
    INCHES("INCH", R.string.shop_profile_unit_inch),
    CENTIMETERS("CM", R.string.shop_profile_unit_cm);

    companion object {
        fun fromCode(code: String): MeasurementUnit {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INCHES
        }
    }
}

enum class WeeklyOffDay(val code: String, val labelRes: Int) {
    SUNDAY("Sunday", R.string.shop_profile_day_sunday),
    MONDAY("Monday", R.string.shop_profile_day_monday),
    TUESDAY("Tuesday", R.string.shop_profile_day_tuesday),
    WEDNESDAY("Wednesday", R.string.shop_profile_day_wednesday),
    THURSDAY("Thursday", R.string.shop_profile_day_thursday),
    FRIDAY("Friday", R.string.shop_profile_day_friday),
    SATURDAY("Saturday", R.string.shop_profile_day_saturday),
    NONE("None", R.string.shop_profile_day_none);

    companion object {
        fun fromCode(code: String): WeeklyOffDay {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SUNDAY
        }
    }
}

/**
 * Shop Profile and Working Hours Configuration Screen (TASK-03-06).
 * Allows the shop owner to configure daily operating schedule, weekly off day,
 * default measurement unit (Inches vs cm), standard lead time, and tax/address info.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopProfileConfigScreen(
    initialOpeningTime: String = "09:30 AM",
    initialClosingTime: String = "08:30 PM",
    initialWeeklyOff: String = "Sunday",
    initialUnit: String = "INCH",
    initialLeadDays: String = "5",
    initialAddress: String = "",
    initialGstin: String = "",
    initialUpiId: String = "",
    shopName: String = "My Boutique",
    accountEmail: String = "",
    accountPhone: String = "",
    isPremium: Boolean = false,
    rewardedAdManager: RewardedAdManager? = null,
    consentManager: ConsentManager? = null,
    onTriggerCloudBackup: () -> Unit = {},
    onTriggerCloudRestore: () -> Unit = {},
    onExportOfflineBackup: () -> Unit = {},
    onNavigateToUpgrade: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    onConfigSaved: (
        openingTime: String,
        closingTime: String,
        weeklyOff: String,
        unit: String,
        leadDays: String,
        address: String,
        gstin: String,
        upiId: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var openingTime by remember(initialOpeningTime) { mutableStateOf(initialOpeningTime) }
    var closingTime by remember(initialClosingTime) { mutableStateOf(initialClosingTime) }
    var selectedWeeklyOff by remember(initialWeeklyOff) { mutableStateOf(WeeklyOffDay.fromCode(initialWeeklyOff)) }
    var selectedUnit by remember(initialUnit) { mutableStateOf(MeasurementUnit.fromCode(initialUnit)) }
    var leadDays by remember(initialLeadDays) { mutableStateOf(initialLeadDays) }
    var shopAddress by remember(initialAddress) { mutableStateOf(initialAddress) }
    var gstin by remember(initialGstin) { mutableStateOf(initialGstin) }
    var upiId by remember(initialUpiId) { mutableStateOf(initialUpiId) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val leadDaysOptions = listOf("3", "5", "7", "10")

    Scaffold(
        topBar = {
            if (onNavigateBack != null) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.nav_settings),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.action_back),
                                tint = CardWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DeepTeal,
                        titleContentColor = CardWhite,
                        navigationIconContentColor = CardWhite
                    )
                )
            }
        },
        containerColor = WarmIvory,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                TailorPrimaryButton(
                    text = stringResource(id = R.string.shop_profile_btn_save),
                    onClick = {
                        onConfigSaved(
                            openingTime,
                            closingTime,
                            selectedWeeklyOff.code,
                            selectedUnit.code,
                            leadDays,
                            shopAddress,
                            gstin,
                            upiId
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Text(
                text = stringResource(id = R.string.shop_profile_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepTeal
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.shop_profile_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalSubtle
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 0: Registered Account & Boutique Details
            TailorSectionCard(title = "Account & Login Identity") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Boutique / Shop", style = MaterialTheme.typography.bodySmall, color = CharcoalSubtle)
                        Text(shopName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                    }
                    if (accountEmail.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Registered Email", style = MaterialTheme.typography.bodySmall, color = CharcoalSubtle)
                            Text(accountEmail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = DeepTeal)
                        }
                    }
                    if (accountPhone.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mobile Number", style = MaterialTheme.typography.bodySmall, color = CharcoalSubtle)
                            Text(accountPhone, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = DeepTeal)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: Working Hours & Schedule
            TailorSectionCard(title = stringResource(id = R.string.shop_profile_section_hours)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Opening Time
                    OutlinedTextField(
                        value = openingTime,
                        onValueChange = { openingTime = it },
                        label = { Text(stringResource(id = R.string.shop_profile_field_opening)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = DeepTeal
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = MutedSage,
                            focusedLabelColor = DeepTeal
                        )
                    )

                    // Closing Time
                    OutlinedTextField(
                        value = closingTime,
                        onValueChange = { closingTime = it },
                        label = { Text(stringResource(id = R.string.shop_profile_field_closing)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = DeepTeal
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = MutedSage,
                            focusedLabelColor = DeepTeal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weekly Off Day
                Text(
                    text = stringResource(id = R.string.shop_profile_field_weekly_off),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Charcoal
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2 Rows of 4 days for all 7 days + None
                val daysList = WeeklyOffDay.entries
                val row1 = daysList.take(4)
                val row2 = daysList.drop(4)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row1.forEach { day ->
                            val isSelected = selectedWeeklyOff == day
                            val bg = if (isSelected) DeepTeal else WarmIvory
                            val border = if (isSelected) DeepTeal else MutedSage
                            val textCol = if (isSelected) WarmIvory else Charcoal

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .border(1.dp, border, RoundedCornerShape(8.dp))
                                    .clickable { selectedWeeklyOff = day }
                                    .padding(vertical = 10.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = day.labelRes),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = textCol,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row2.forEach { day ->
                            val isSelected = selectedWeeklyOff == day
                            val bg = if (isSelected) DeepTeal else WarmIvory
                            val border = if (isSelected) DeepTeal else MutedSage
                            val textCol = if (isSelected) WarmIvory else Charcoal

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .border(1.dp, border, RoundedCornerShape(8.dp))
                                    .clickable { selectedWeeklyOff = day }
                                    .padding(vertical = 10.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = day.labelRes),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = textCol,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 2: Measurement Units & Defaults
            TailorSectionCard(title = stringResource(id = R.string.shop_profile_section_preferences)) {
                // Measurement Unit Toggle
                Text(
                    text = stringResource(id = R.string.shop_profile_field_unit),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Charcoal
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MeasurementUnit.values().forEach { unit ->
                        val isSelected = selectedUnit == unit
                        val bg = if (isSelected) DeepTeal else WarmIvory
                        val border = if (isSelected) DeepTeal else MutedSage
                        val textCol = if (isSelected) WarmIvory else Charcoal

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable { selectedUnit = unit }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = if (isSelected) SoftGold else DeepTeal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = unit.labelRes),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = textCol
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Standard Delivery Lead Days
                Text(
                    text = stringResource(id = R.string.shop_profile_field_lead_days),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Charcoal
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    leadDaysOptions.forEach { days ->
                        val isSelected = leadDays == days
                        val bg = if (isSelected) DeepTeal else WarmIvory
                        val border = if (isSelected) DeepTeal else MutedSage
                        val textCol = if (isSelected) WarmIvory else Charcoal

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable { leadDays = days }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$days Days",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = textCol
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: Optional Address & Tax Information
            TailorSectionCard(title = stringResource(id = R.string.shop_profile_section_additional)) {
                OutlinedTextField(
                    value = shopAddress,
                    onValueChange = { shopAddress = it },
                    label = { Text(stringResource(id = R.string.shop_profile_field_address)) },
                    placeholder = { Text(stringResource(id = R.string.shop_profile_field_address_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = DeepTeal
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepTeal,
                        unfocusedBorderColor = MutedSage,
                        focusedLabelColor = DeepTeal
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it.uppercase() },
                    label = { Text(stringResource(id = R.string.shop_profile_field_gstin)) },
                    placeholder = { Text(stringResource(id = R.string.shop_profile_field_gstin_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = DeepTeal
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepTeal,
                        unfocusedBorderColor = MutedSage,
                        focusedLabelColor = DeepTeal
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 4: UPI & Instant QR Payments
            TailorSectionCard(title = "UPI & Instant QR Payments") {
                Text(
                    text = "Customer payments will be routed directly to this UPI ID when generating instant on-screen QR codes in orders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CharcoalSubtle
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it.trim() },
                    label = { Text("Boutique UPI ID (VPA)") },
                    placeholder = { Text("e.g. 9848011223@ybl, boutique@okaxis") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = DeepTeal
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepTeal,
                        unfocusedBorderColor = MutedSage,
                        focusedLabelColor = DeepTeal
                    )
                )

                if (upiId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBeige, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = DeepTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active QR Payee: $upiId",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = DeepTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 5: Cloud Backup & Data Security
            TailorSectionCard(title = stringResource(id = R.string.backup_cloud_title)) {
                val statusText = if (isPremium) stringResource(id = R.string.backup_pro_status)
                                 else stringResource(id = R.string.backup_free_status)
                val statusColor = if (isPremium) StatusGreen else SoftGold

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightBeige, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isPremium) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Charcoal
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (isPremium) {
                            Toast.makeText(context, context.getString(R.string.backup_success), Toast.LENGTH_SHORT).show()
                            onTriggerCloudBackup()
                        } else {
                            showBackupDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = CardWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.backup_btn_now),
                        color = CardWhite,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        if (isPremium) {
                            Toast.makeText(context, context.getString(R.string.restore_success), Toast.LENGTH_SHORT).show()
                            onTriggerCloudRestore()
                        } else {
                            showRestoreDialog = true
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal),
                    border = BorderStroke(1.dp, DeepTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.restore_btn_now),
                        color = DeepTeal,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onExportOfflineBackup,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal),
                    border = BorderStroke(1.dp, MutedSage),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.backup_btn_export_offline),
                        color = DeepTeal,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 6: Legal, Privacy & Compliance
            TailorSectionCard(title = stringResource(id = R.string.legal_privacy_title)) {
                // Privacy Policy Button
                OutlinedButton(
                    onClick = { showPrivacyPolicyDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal),
                    border = BorderStroke(1.dp, MutedSage)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.legal_privacy_policy_btn),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Terms of Service Button
                OutlinedButton(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal),
                    border = BorderStroke(1.dp, MutedSage)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.legal_terms_btn),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ad Privacy & Consent Choices
                OutlinedButton(
                    onClick = {
                        val activity = context.findActivity()
                        if (activity != null && consentManager != null) {
                            consentManager.showPrivacyOptionsForm(activity) {
                                Toast.makeText(context, "Privacy choices updated", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Ad privacy choices are managed in compliance with Google Play.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal),
                    border = BorderStroke(1.dp, MutedSage)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.legal_ad_privacy_btn),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.legal_app_version),
                        style = MaterialTheme.typography.labelSmall,
                        color = CharcoalSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Anchored Adaptive Banner Ad (Approved placement: Settings / Profile)
            TailorAdaptiveBannerAd(
                placement = AdPlacement.SETTINGS,
                isPremium = isPremium
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showBackupDialog) {
            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.backup_dialog_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.backup_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val activity = context.findActivity()
                            if (activity != null && rewardedAdManager != null) {
                                rewardedAdManager.showAd(
                                    activity = activity,
                                    onUserEarnedReward = {
                                        showBackupDialog = false
                                        Toast.makeText(context, context.getString(R.string.backup_success), Toast.LENGTH_LONG).show()
                                        onTriggerCloudBackup()
                                    },
                                    onAdFailed = {
                                        showBackupDialog = false
                                        Toast.makeText(context, "Video ad not ready. Backing up now...", Toast.LENGTH_SHORT).show()
                                        onTriggerCloudBackup()
                                    }
                                )
                            } else {
                                showBackupDialog = false
                                Toast.makeText(context, context.getString(R.string.backup_success), Toast.LENGTH_SHORT).show()
                                onTriggerCloudBackup()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                    ) {
                        Text(stringResource(R.string.backup_dialog_watch), color = CardWhite)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showBackupDialog = false
                            onNavigateToUpgrade()
                        }
                    ) {
                        Text(stringResource(R.string.entitlement_btn_upgrade), color = DeepTeal)
                    }
                },
                containerColor = CardWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.restore_dialog_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.restore_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val activity = context.findActivity()
                            if (activity != null && rewardedAdManager != null) {
                                rewardedAdManager.showAd(
                                    activity = activity,
                                    onUserEarnedReward = {
                                        showRestoreDialog = false
                                        Toast.makeText(context, context.getString(R.string.restore_success), Toast.LENGTH_LONG).show()
                                        onTriggerCloudRestore()
                                    },
                                    onAdFailed = {
                                        showRestoreDialog = false
                                        Toast.makeText(context, "Video ad not ready. Restoring data now...", Toast.LENGTH_SHORT).show()
                                        onTriggerCloudRestore()
                                    }
                                )
                            } else {
                                showRestoreDialog = false
                                Toast.makeText(context, context.getString(R.string.restore_success), Toast.LENGTH_SHORT).show()
                                onTriggerCloudRestore()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                    ) {
                        Text(stringResource(R.string.restore_dialog_watch), color = CardWhite)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRestoreDialog = false
                            onNavigateToUpgrade()
                        }
                    ) {
                        Text(stringResource(R.string.entitlement_btn_upgrade), color = DeepTeal)
                    }
                },
                containerColor = CardWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showPrivacyPolicyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyPolicyDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.legal_privacy_title_dialog),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.legal_privacy_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                        Text("OK", color = DeepTeal, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    val onlineUrl = stringResource(R.string.legal_privacy_online_url)
                    TextButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(onlineUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, onlineUrl, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text(stringResource(R.string.legal_privacy_online_btn), color = DeepTeal)
                    }
                },
                containerColor = CardWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.legal_terms_title_dialog),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.legal_terms_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showTermsDialog = false }) {
                        Text("OK", color = DeepTeal, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CardWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopProfileConfigScreenPreview() {
    TailorBookTheme {
        ShopProfileConfigScreen(
            onConfigSaved = { _, _, _, _, _, _, _, _ -> }
        )
    }
}
