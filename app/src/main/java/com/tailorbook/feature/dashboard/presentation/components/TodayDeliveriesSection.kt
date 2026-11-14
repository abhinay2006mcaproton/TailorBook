package com.tailorbook.feature.dashboard.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.core.util.FormattingUtils
import com.tailorbook.core.util.HapticHelper
import com.tailorbook.feature.dashboard.domain.model.DeliverySummaryItem
import com.tailorbook.feature.dashboard.domain.model.OrderStatus
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.StatusRed

/**
 * Today's Deliveries section (TASK-04-03 / REQ-DASH-03).
 * Displays today's scheduled deliveries with direct Call, WhatsApp, and status actions.
 */
@Composable
fun TodayDeliveriesSection(
    deliveries: List<DeliverySummaryItem>,
    onOrderStatusChange: (orderId: String, newStatus: OrderStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_deliveries_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepTeal
            )

            Text(
                text = "${deliveries.size} Orders",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = CharcoalSubtle
            )
        }

        if (deliveries.isEmpty()) {
            TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.dashboard_deliveries_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharcoalSubtle
                    )
                }
            }
        } else {
            deliveries.forEach { item ->
                DeliveryCard(
                    item = item,
                    onCallClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${item.customerPhone}")
                        }
                        context.startActivity(intent)
                    },
                    onWhatsAppClick = {
                        val message = "Hello ${item.customerName}, your tailoring order ${item.orderNumber} is scheduled for delivery today."
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=+91${item.customerPhone}&text=${Uri.encode(message)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                    onStatusToggle = {
                        val nextStatus = when (item.status) {
                            OrderStatus.READY -> OrderStatus.DELIVERED
                            OrderStatus.STITCHING -> OrderStatus.READY
                            else -> OrderStatus.READY
                        }
                        onOrderStatusChange(item.orderId, nextStatus)
                    }
                )
            }
        }
    }
}

@Composable
fun DeliveryCard(
    item: DeliverySummaryItem,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onStatusToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusBg = when (item.status) {
        OrderStatus.READY -> StatusGreen.copy(alpha = 0.12f)
        OrderStatus.DELIVERED -> MutedSage.copy(alpha = 0.2f)
        OrderStatus.CANCELLED -> StatusRed.copy(alpha = 0.12f)
        else -> SoftGold.copy(alpha = 0.15f)
    }

    val statusTextCol = when (item.status) {
        OrderStatus.READY -> StatusGreen
        OrderStatus.DELIVERED -> CharcoalSubtle
        OrderStatus.CANCELLED -> StatusRed
        else -> DeepTeal
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    TailorElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Customer Name + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = FormattingUtils.formatPersonName(item.customerName),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Charcoal
                    )
                    Text(
                        text = "${item.orderNumber} • ${item.garmentType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalSubtle
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(id = item.status.labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusTextCol
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: Delivery time & Balance amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = CharcoalSubtle,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.deliveryDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalSubtle
                    )
                }

                if (item.balanceAmount > 0) {
                    Text(
                        text = "Due: " + FormattingUtils.formatCurrency(item.balanceAmount),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = StatusRed
                    )
                } else {
                    Text(
                        text = "Fully Paid",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = StatusGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row: Call, WhatsApp, and Status toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Action
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MutedSage, RoundedCornerShape(8.dp))
                        .clickable {
                            HapticHelper.tick(context, haptic)
                            onCallClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = stringResource(id = R.string.dashboard_call_action),
                        tint = DeepTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.dashboard_call_action),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = DeepTeal
                    )
                }

                // WhatsApp Action
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, StatusGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable {
                            HapticHelper.tick(context, haptic)
                            onWhatsAppClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = stringResource(id = R.string.dashboard_whatsapp_action),
                        tint = StatusGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.dashboard_whatsapp_action),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = StatusGreen
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Status advancement button (e.g. Mark Ready or Mark Delivered)
                if (item.status != OrderStatus.DELIVERED) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DeepTeal)
                            .clickable {
                                HapticHelper.success(context, haptic)
                                onStatusToggle()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (item.status == OrderStatus.READY) "Mark Delivered" else "Mark Ready",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                    }
                }
            }
        }
    }
}
