package com.tailorbook.feature.subscription.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.ui.components.TailorDangerButton
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

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onMenuClick: () -> Unit,
    customerCount: Int = 0,
    activeOrdersCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showVipCodeDialog by remember { mutableStateOf(false) }
    var enteredVipCode by remember { mutableStateOf("") }
    var vipCodeError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.subscription_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = stringResource(R.string.subscription_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation menu",
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
        },
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─────────────────────────────────────────────────────────────────
            // 1. Current Membership Status Hero Card
            // ─────────────────────────────────────────────────────────────────
            CurrentStatusHeroCard(
                isPremium = state.isPremium,
                activePlan = state.activePlan,
                expiryDate = state.expiryDate,
                customerCount = customerCount,
                activeOrdersCount = activeOrdersCount
            )

            // ─────────────────────────────────────────────────────────────────
            // 2. Billing Cycle Picker (Monthly vs Yearly)
            // ─────────────────────────────────────────────────────────────────
            if (!state.isPremium) {
                Text(
                    text = stringResource(R.string.subscription_choose_plan),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BillingCycle.values().forEach { cycle ->
                        val isSelected = state.selectedCycle == cycle
                        val displayPrice = if (cycle == BillingCycle.MONTHLY) {
                            state.monthlyDisplayPrice
                        } else {
                            state.annualDisplayPrice
                        }
                        BillingCycleCard(
                            cycle = cycle,
                            displayPrice = displayPrice,
                            isSelected = isSelected,
                            onClick = { viewModel.selectBillingCycle(cycle) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 3. Feature Comparison Matrix (Free vs Pro Boutique)
            // ─────────────────────────────────────────────────────────────────
            FeatureComparisonCard(isPremium = state.isPremium)

            // ─────────────────────────────────────────────────────────────────
            // 4. Action CTA Buttons
            // ─────────────────────────────────────────────────────────────────
            if (!state.isPremium) {
                val currentPrice = if (state.selectedCycle == BillingCycle.MONTHLY) {
                    state.monthlyDisplayPrice
                } else {
                    state.annualDisplayPrice
                }
                TailorPrimaryButton(
                    text = stringResource(
                        R.string.subscription_upgrade_cta,
                        currentPrice,
                        state.selectedCycle.period
                    ),
                    onClick = {
                        val activity = context.findActivity()
                        if (activity != null) {
                            viewModel.purchaseSubscription(activity, state.selectedCycle)
                        } else if (com.tailorbook.BuildConfig.DEBUG) {
                            viewModel.upgradeToPro(state.selectedCycle)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { viewModel.restorePurchases() }) {
                        Text(
                            text = stringResource(R.string.subscription_restore_btn),
                            style = MaterialTheme.typography.labelMedium,
                            color = CharcoalSubtle
                        )
                    }
                    TextButton(onClick = {
                        enteredVipCode = ""
                        vipCodeError = null
                        showVipCodeDialog = true
                    }) {
                        Text(
                            text = "Redeem VIP Code",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                TailorDangerButton(
                    text = stringResource(R.string.subscription_cancel_btn),
                    onClick = { showCancelConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // 5. Trust & Offline Guarantee Badge
            // ─────────────────────────────────────────────────────────────────
            TrustGuaranteeCard()

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ─────────────────────────────────────────────────────────────────────
        // Dialogs: Celebratory Upgrade Confirmation & Cancellation
        // ─────────────────────────────────────────────────────────────────────
        if (state.showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSuccessDialog() },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SoftGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.subscription_success_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = state.successMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    TailorPrimaryButton(
                        text = stringResource(R.string.subscription_start_using),
                        onClick = { viewModel.dismissSuccessDialog() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }

        if (!state.errorMessage.isNullOrBlank()) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissErrorDialog() },
                title = {
                    Text(
                        text = "Payment Notice",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                },
                text = {
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal
                    )
                },
                confirmButton = {
                    TailorPrimaryButton(
                        text = stringResource(R.string.action_close),
                        onClick = { viewModel.dismissErrorDialog() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }

        if (showVipCodeDialog) {
            AlertDialog(
                onDismissRequest = { showVipCodeDialog = false },
                title = {
                    Text(
                        text = "Redeem VIP / Partner Code",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter your secret VIP code (e.g. TAILORVIP2026) to activate lifetime free access across all features without limits.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Charcoal
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = enteredVipCode,
                            onValueChange = {
                                enteredVipCode = it
                                vipCodeError = null
                            },
                            label = { Text("VIP Access Code") },
                            isError = vipCodeError != null,
                            supportingText = if (vipCodeError != null) {
                                { Text(vipCodeError ?: "", color = StatusRed) }
                            } else null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TailorPrimaryButton(
                        text = "Activate VIP Free",
                        onClick = {
                            if (enteredVipCode.isBlank()) {
                                vipCodeError = "Please enter a valid code"
                            } else {
                                viewModel.redeemVipCode(enteredVipCode) { success ->
                                    if (success) {
                                        showVipCodeDialog = false
                                    } else {
                                        vipCodeError = "Invalid or expired VIP code"
                                    }
                                }
                            }
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showVipCodeDialog = false }) {
                        Text(text = stringResource(R.string.action_cancel), color = CharcoalSubtle)
                    }
                }
            )
        }

        if (showCancelConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showCancelConfirmDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.subscription_cancel_confirm_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = StatusRed
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.subscription_cancel_confirm_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal
                    )
                },
                confirmButton = {
                    TailorDangerButton(
                        text = stringResource(R.string.subscription_confirm_downgrade),
                        onClick = {
                            showCancelConfirmDialog = false
                            viewModel.cancelSubscription()
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showCancelConfirmDialog = false }) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            color = CharcoalSubtle
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun CurrentStatusHeroCard(
    isPremium: Boolean,
    activePlan: String,
    expiryDate: String,
    customerCount: Int = 0,
    activeOrdersCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isPremium) DeepTeal else CardWhite,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isPremium) 1.5.dp else 1.dp,
                color = if (isPremium) SoftGold else MutedSage.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isPremium) SoftGold.copy(alpha = 0.25f) else LightBeige),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPremium) Icons.Default.Star else Icons.Default.Verified,
                            contentDescription = null,
                            tint = if (isPremium) SoftGold else DeepTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isPremium) "Pro Boutique Membership" else "TailorBook Free Tier",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isPremium) CardWhite else Charcoal
                        )
                        Text(
                            text = if (isPremium) "Plan: $activePlan" else "Complete 100% Offline Core",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPremium) SoftGold else CharcoalSubtle
                        )
                    }
                }

                Surface(
                    color = if (isPremium) SoftGold else StatusGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPremium) "ACTIVE" else "FREE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isPremium) DeepTeal else StatusGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = if (isPremium) CardWhite.copy(alpha = 0.15f) else MutedSage.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            if (isPremium) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Renewal / Expiry Date:",
                        style = MaterialTheme.typography.bodySmall,
                        color = CardWhite.copy(alpha = 0.8f)
                    )
                    Text(
                        text = expiryDate,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = SoftGold
                    )
                }
            } else {
                Text(
                    text = "You are currently on the TailorBook Free Tier. Enjoy our complete offline core with the following allowance:",
                    style = MaterialTheme.typography.bodySmall,
                    color = CharcoalSubtle,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val custLimitText = if (customerCount > 0) "$customerCount/20 ($customerCount Used)" else "10 Free / 20 w/ Ads"
                    val orderLimitText = if (activeOrdersCount > 0) "$activeOrdersCount/15 Active" else "15 Max"
                    StatusLimitBadge("👥 Customers", custLimitText)
                    StatusLimitBadge("📋 Active Orders", orderLimitText)
                    StatusLimitBadge("✂️ Staff", "1 Master")
                    StatusLimitBadge("📷 Photos", "1 per Order")
                    StatusLimitBadge("🏷️ Watermark", "TailorBook Branding")
                    StatusLimitBadge("☁️ Backup", "Ad-Supported")
                }
            }
        }
    }
}

@Composable
private fun StatusLimitBadge(title: String, limit: String) {
    Surface(
        color = LightBeige,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MutedSage.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title: ",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = DeepTeal
            )
            Text(
                text = limit,
                style = MaterialTheme.typography.labelSmall,
                color = Charcoal
            )
        }
    }
}

@Composable
private fun BillingCycleCard(
    cycle: BillingCycle,
    displayPrice: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) DeepTeal.copy(alpha = 0.08f) else CardWhite,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isSelected) 2.dp else 1.dp,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) DeepTeal else MutedSage.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            if (cycle.savingsBadge != null) {
                Surface(
                    color = SoftGold,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = cycle.savingsBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CardWhite,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = cycle.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) DeepTeal else Charcoal
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = displayPrice,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = if (isSelected) DeepTeal else Charcoal
                )
                Text(
                    text = cycle.period,
                    style = MaterialTheme.typography.labelSmall,
                    color = CharcoalSubtle,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureComparisonCard(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    TailorElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.subscription_comparison_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepTeal
            )
            Spacer(modifier = Modifier.height(12.dp))

            val features = listOf(
                FeatureRowItem("Customer Capacity", "10 Free / 20 with Ads", "Unlimited Customers", false, true),
                FeatureRowItem("Active Orders", "15 Concurrent", "Unlimited", false, true),
                FeatureRowItem("Team & Staff", "1 Master", "Unlimited & Piece-Rate", false, true),
                FeatureRowItem("Design Photos", "1 Photo / Order", "Up to 5 Photos", false, true),
                FeatureRowItem("Receipts & Cutting Slips", "Watermarked", "100% White-Label", false, true),
                FeatureRowItem("Cloud Backup", "Ad-Supported Manual", "Real-Time Supabase", false, true),
                FeatureRowItem("Custom Templates", "Ad-Supported", "Unlimited", false, true),
                FeatureRowItem("Audio Voice Notes", "30s (2m with Ad)", "Full 2 Mins Unlimited", false, true),
                FeatureRowItem("Ad-Free Experience", "Video & Banner Ads", "Zero Ads", false, true),
                FeatureRowItem("100% Offline Database", "Unlimited", "Unlimited", true, true),
                FeatureRowItem("VIP Priority WhatsApp Support", "Community", "24/7 Dedicated", false, true)
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightBeige)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Feature",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Charcoal,
                    modifier = Modifier.weight(1.8f)
                )
                Text(
                    text = "Free",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CharcoalSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Pro Boutique",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.2f)
                )
            }

            features.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Charcoal,
                        modifier = Modifier.weight(1.8f)
                    )
                    Text(
                        text = item.freeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isFreeAvailable) Charcoal else CharcoalSubtle.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier.weight(1.2f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (item.isProAvailable) StatusGreen else CharcoalSubtle,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = item.proText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DeepTeal,
                            fontSize = 11.sp
                        )
                    }
                }
                if (index < features.lastIndex) {
                    HorizontalDivider(color = MutedSage.copy(alpha = 0.2f))
                }
            }
        }
    }
}

private data class FeatureRowItem(
    val name: String,
    val freeText: String,
    val proText: String,
    val isFreeAvailable: Boolean,
    val isProAvailable: Boolean
)

@Composable
private fun TrustGuaranteeCard(modifier: Modifier = Modifier) {
    Surface(
        color = LightBeige.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = DeepTeal,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "Boutique Owner's Data Guarantee",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )
                Text(
                    text = "Your customer profiles, measurements, and order history are 100% saved locally on your device and will NEVER be held hostage behind a paywall.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = CharcoalSubtle,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
