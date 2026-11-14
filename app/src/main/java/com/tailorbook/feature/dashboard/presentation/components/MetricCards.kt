package com.tailorbook.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.core.util.FormattingUtils
import com.tailorbook.core.util.HapticHelper
import com.tailorbook.feature.dashboard.domain.model.DashboardMetrics
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.StatusRed

/**
 * Summary metric cards component for the Dashboard (TASK-04-02).
 * Displays Active Orders, Due Today, Ready for Delivery, and Pending Balance.
 * Enforces uniform intrinsic heights and 2-line title baselines so cards align perfectly.
 */
@Composable
fun DashboardMetricGrid(
    metrics: DashboardMetrics,
    modifier: Modifier = Modifier,
    onMetricClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Active Orders & Due Today
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = stringResource(id = R.string.dashboard_metric_active_orders),
                value = metrics.activeOrdersCount.toString(),
                icon = Icons.AutoMirrored.Filled.Assignment,
                iconTint = DeepTeal,
                modifier = Modifier.weight(1f),
                onClick = { onMetricClick("active_orders") }
            )

            MetricCard(
                title = stringResource(id = R.string.dashboard_metric_due_today),
                value = metrics.dueTodayCount.toString(),
                icon = Icons.Default.PendingActions,
                iconTint = SoftGold,
                modifier = Modifier.weight(1f),
                onClick = { onMetricClick("due_today") }
            )
        }

        // Row 2: Ready for Pickup & Pending Balance (Both cards aligned with 2-line title height)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = stringResource(id = R.string.dashboard_metric_ready),
                value = metrics.readyCount.toString(),
                icon = Icons.Default.CheckCircle,
                iconTint = StatusGreen,
                modifier = Modifier.weight(1f),
                onClick = { onMetricClick("ready_pickup") }
            )

            MetricCard(
                title = stringResource(id = R.string.dashboard_metric_pending_balance),
                value = FormattingUtils.formatCurrency(metrics.outstandingBalanceSum),
                icon = Icons.Default.CurrencyRupee,
                iconTint = if (metrics.outstandingBalanceSum > 0) StatusRed else DeepTeal,
                modifier = Modifier.weight(1f),
                onClick = { onMetricClick("pending_balance") }
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    TailorElevatedCard(
        modifier = modifier.clickable {
            HapticHelper.tick(context, haptic)
            onClick()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    ),
                    color = CharcoalSubtle,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = Charcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
