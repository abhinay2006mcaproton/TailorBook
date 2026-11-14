package com.tailorbook.feature.reports.presentation

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.core.ads.AdPlacement
import com.tailorbook.ui.components.TailorAdaptiveBannerAd
import com.tailorbook.ui.components.TailorAdBanner
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

/**
 * Business Reports & Analytics Dashboard (TASK-11-01 / REQ-REP-01 / REQ-REP-05).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    shopName: String,
    onMenuClick: () -> Unit,
    isPremium: Boolean = false,
    onUpgradeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedRange by viewModel.selectedRange.collectAsState()
    val summary by viewModel.reportsSummary.collectAsState()
    var showExportGatingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.reports_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = stringResource(R.string.reports_subtitle),
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
                    IconButton(
                        onClick = {
                            if (!isPremium) {
                                showExportGatingDialog = true
                            } else {
                                val shareText = viewModel.buildShareableReportSummary(shopName)
                                shareReportText(context, shareText)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.reports_share_summary),
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
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Section 1: Date Range Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ReportDateRange.values()) { range ->
                        val isSelected = range == selectedRange
                        val label = when (range) {
                            ReportDateRange.TODAY -> stringResource(R.string.reports_range_today)
                            ReportDateRange.THIS_WEEK -> stringResource(R.string.reports_range_week)
                            ReportDateRange.THIS_MONTH -> stringResource(R.string.reports_range_month)
                            ReportDateRange.THIS_YEAR -> stringResource(R.string.reports_range_year)
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectDateRange(range) },
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
            }

            // Section 2: Cash Flow & Financial Summary Card
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Financial Overview",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Booked Value Tile
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LightBeige.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.reports_booked_value),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CharcoalSubtle
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${summary.bookedValue.toInt()}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = DeepTeal
                                )
                            }

                            // Cash Collected Tile
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StatusGreen.copy(alpha = 0.12f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.reports_collected_cash),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CharcoalSubtle
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${summary.cashCollected.toInt()}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = StatusGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pending Balance Due Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarmIvory)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pending Client Dues:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = CharcoalSubtle
                            )
                            Text(
                                text = "₹${summary.pendingBalance.toInt()}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (summary.pendingBalance > 0) StatusRed else StatusGreen
                            )
                        }
                    }
                }
            }

            // Section 3: Delivery Punctuality Performance Card
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.reports_punctuality),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DeepTeal
                                )
                            }

                            // Punctuality Badge
                            Surface(
                                color = StatusGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${summary.punctualityRate}% On-Time",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StatusGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { summary.punctualityRate / 100f },
                            color = StatusGreen,
                            trackColor = MutedSage.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Orders completed & delivered on or before the promised delivery date.",
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalSubtle
                        )
                    }
                }
            }

            // Section 4: Orders by Status Breakdown Card
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.reports_orders_breakdown),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val statuses = listOf(
                            Triple("New Orders", summary.statusBreakdown["NEW"] ?: 0, DeepTeal),
                            Triple("In Stitching", summary.statusBreakdown["IN_PROGRESS"] ?: 0, SoftGold),
                            Triple("Ready for Pickup", summary.statusBreakdown["READY"] ?: 0, Color(0xFFE67E22)),
                            Triple("Delivered", summary.statusBreakdown["DELIVERED"] ?: 0, StatusGreen)
                        )

                        statuses.forEach { (label, count, color) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Charcoal
                                    )
                                }
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DeepTeal
                                )
                            }
                        }
                    }
                }
            }

            // Section 5: Popular Garments Ranking
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.reports_popular_garments),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (summary.popularGarments.isEmpty()) {
                            Text(
                                text = "No garments recorded in this period",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalSubtle
                            )
                        } else {
                            summary.popularGarments.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            color = if (index == 0) SoftGold else LightBeige,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (index == 0) CardWhite else Charcoal
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = item.garmentName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = Charcoal
                                            )
                                            Text(
                                                text = "${item.orderCount} orders placed",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CharcoalSubtle
                                            )
                                        }
                                    }

                                    Text(
                                        text = "₹${item.totalRevenue.toInt()}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Non-intrusive Anchored Adaptive Ad Banner (Approved placement: below report summaries)
            item {
                TailorAdaptiveBannerAd(
                    placement = AdPlacement.REPORTS,
                    isPremium = isPremium
                )
            }

            // Section 6: Share Summary Button
            item {
                TailorPrimaryButton(
                    text = stringResource(R.string.reports_share_summary),
                    onClick = {
                        if (!isPremium) {
                            showExportGatingDialog = true
                        } else {
                            val shareText = viewModel.buildShareableReportSummary(shopName)
                            shareReportText(context, shareText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showExportGatingDialog) {
            AlertDialog(
                onDismissRequest = { showExportGatingDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.entitlement_reports_export_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.entitlement_reports_export_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExportGatingDialog = false
                            onUpgradeClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                    ) {
                        Text(stringResource(R.string.entitlement_btn_upgrade), color = CardWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportGatingDialog = false }) {
                        Text(stringResource(R.string.entitlement_btn_maybe_later), color = CharcoalSubtle)
                    }
                },
                containerColor = CardWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

private fun shareReportText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share TailorBook Report"))
}
