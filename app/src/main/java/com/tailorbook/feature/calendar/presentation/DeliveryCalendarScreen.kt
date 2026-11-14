package com.tailorbook.feature.calendar.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.core.ads.AdPlacement
import com.tailorbook.feature.calendar.domain.model.CalendarDay
import com.tailorbook.feature.calendar.domain.model.ScheduledOrder
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.ui.components.TailorAdaptiveBannerAd
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Monthly Delivery Calendar & Daily Schedule Screen (TASK-09-02 / TASK-09-03).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryCalendarScreen(
    viewModel: CalendarViewModel,
    onMenuClick: () -> Unit,
    onNavigateToOrderDetails: (String) -> Unit,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayedCalendar by viewModel.displayedCalendar.collectAsState()
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val monthDays by viewModel.monthDays.collectAsState()
    val scheduledOrders by viewModel.scheduledOrdersForSelectedDate.collectAsState()

    val orderToReschedule by viewModel.orderToReschedule.collectAsState()
    val rescheduleNewDate by viewModel.rescheduleNewDate.collectAsState()
    val rescheduleReason by viewModel.rescheduleReason.collectAsState()

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val selectedDateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    val todayStartMillis = CalendarViewModel.getStartOfDay(System.currentTimeMillis())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.calendar_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = stringResource(R.string.calendar_subtitle),
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
                    Surface(
                        color = SoftGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { viewModel.goToToday() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.calendar_today),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = CardWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepTeal)
            )
        },
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Month Header with navigation chevrons
            item {
                Surface(
                    color = CardWhite,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            IconButton(onClick = { viewModel.previousMonth() }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = DeepTeal)
                            }
                            Text(
                                text = monthFormat.format(displayedCalendar.time),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                            IconButton(onClick = { viewModel.nextMonth() }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = DeepTeal)
                            }
                        }

                        // Day of week labels (Sun - Sat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                            daysOfWeek.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CharcoalSubtle
                                )
                            }
                        }

                        // Calendar Grid (35 or 42 cells)
                        val rows = monthDays.chunked(7)
                        rows.forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                week.forEach { day ->
                                    CalendarDayCell(
                                        day = day,
                                        isSelected = day.timestamp == selectedDateMillis,
                                        startOfTodayMillis = todayStartMillis,
                                        onClick = { viewModel.selectDate(day.timestamp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Status Filter Chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val filters = listOf(
                        "ALL" to stringResource(R.string.calendar_filter_all),
                        "DUE" to stringResource(R.string.calendar_filter_due),
                        "OVERDUE" to stringResource(R.string.calendar_filter_overdue),
                        "DELIVERED" to stringResource(R.string.calendar_filter_delivered)
                    )

                    filters.forEach { (key, label) ->
                        val isSelected = statusFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepTeal,
                                selectedLabelColor = CardWhite,
                                containerColor = CardWhite,
                                labelColor = Charcoal
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) DeepTeal else LightBeige
                            )
                        )
                    }
                }
            }

            // Daily Deliveries Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = selectedDateFormat.format(selectedDateMillis),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                    Surface(
                        color = if (scheduledOrders.isNotEmpty()) DeepTeal.copy(alpha = 0.1f) else LightBeige,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${scheduledOrders.size} Orders",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (scheduledOrders.isNotEmpty()) DeepTeal else CharcoalSubtle,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Scheduled Orders List
            if (scheduledOrders.isEmpty()) {
                item {
                    TailorElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.calendar_no_deliveries),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Charcoal,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.calendar_no_deliveries_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalSubtle,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(scheduledOrders, key = { it.order.id }) { scheduledOrder ->
                    CalendarOrderCard(
                        scheduledOrder = scheduledOrder,
                        onCardClick = { onNavigateToOrderDetails(scheduledOrder.order.id) },
                        onCallClick = {
                            val phone = scheduledOrder.customer?.primaryPhone
                            if (!phone.isNullOrBlank()) {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }
                        },
                        onWhatsAppClick = {
                            val cust = scheduledOrder.customer
                            val phone = cust?.primaryPhone?.replace("+", "")?.trim()
                            if (!phone.isNullOrBlank() && cust != null) {
                                val url = "https://api.whatsapp.com/send?phone=91$phone&text=Hello%20${cust.name},%20your%20order%20${scheduledOrder.order.orderNumber}%20is%20scheduled%20for%20delivery."
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        onRescheduleClick = {
                            viewModel.openRescheduleDialog(scheduledOrder.order)
                        },
                        onAdvanceStatus = { nextStatus ->
                            viewModel.advanceOrderStatus(scheduledOrder.order.id, scheduledOrder.order.status, nextStatus)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Anchored Adaptive Banner Ad (Delivery Calendar placement - always visible)
            item {
                TailorAdaptiveBannerAd(
                    placement = AdPlacement.DELIVERY_CALENDAR,
                    isPremium = isPremium
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Reschedule Delivery Dialog
    if (orderToReschedule != null) {
        val targetOrder = orderToReschedule!!
        val newDateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

        AlertDialog(
            onDismissRequest = { viewModel.closeRescheduleDialog() },
            title = {
                Text(
                    text = stringResource(R.string.calendar_reschedule_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )
            },
            text = {
                Column {
                    Text(
                        text = "Order #${targetOrder.orderNumber}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Charcoal
                    )
                    Text(
                        text = stringResource(R.string.calendar_reschedule_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalSubtle
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "New Delivery Date:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = LightBeige,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = newDateFormat.format(rescheduleNewDate),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Charcoal,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick shortcut chips in 2 rows
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssistChip(
                            onClick = { viewModel.addRescheduleDays(1) },
                            label = { Text(stringResource(R.string.calendar_quick_days_1), fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CardWhite),
                            modifier = Modifier.weight(1f)
                        )
                        AssistChip(
                            onClick = { viewModel.addRescheduleDays(2) },
                            label = { Text(stringResource(R.string.calendar_quick_days_2), fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CardWhite),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssistChip(
                            onClick = { viewModel.addRescheduleDays(3) },
                            label = { Text(stringResource(R.string.calendar_quick_days_3), fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CardWhite),
                            modifier = Modifier.weight(1f)
                        )
                        AssistChip(
                            onClick = { viewModel.addRescheduleDays(7) },
                            label = { Text(stringResource(R.string.calendar_quick_days_7), fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CardWhite),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = rescheduleReason,
                        onValueChange = { viewModel.setRescheduleReason(it) },
                        label = { Text(stringResource(R.string.calendar_reschedule_reason)) },
                        placeholder = { Text("e.g. Client requested postponement") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = LightBeige
                        )
                    )
                }
            },
            confirmButton = {
                TailorPrimaryButton(
                    text = stringResource(R.string.calendar_confirm_reschedule),
                    onClick = { viewModel.confirmReschedule() }
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeRescheduleDialog() }) {
                    Text("Cancel", color = CharcoalSubtle)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Individual cell in the 7-column calendar month grid.
 */
@Composable
fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    startOfTodayMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasOverdue = day.hasOverdue(startOfTodayMillis)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> DeepTeal
                        day.isToday -> SoftGold.copy(alpha = 0.25f)
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = if (day.isToday && !isSelected) 1.5.dp else 0.dp,
                    color = if (day.isToday && !isSelected) SoftGold else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal
                ),
                color = when {
                    isSelected -> CardWhite
                    day.isToday -> DeepTeal
                    !day.isCurrentMonth -> CharcoalSubtle.copy(alpha = 0.4f)
                    else -> Charcoal
                }
            )
        }

        // Dot indicator row
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(8.dp)
        ) {
            if (day.hasDeliveries) {
                val dotColor = when {
                    hasOverdue -> StatusRed
                    day.activeCount > 0 -> SoftGold
                    else -> StatusGreen
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                if (day.totalCount > 1) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${day.totalCount}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) DeepTeal else CharcoalSubtle
                    )
                }
            }
        }
    }
}

/**
 * Detailed order card displayed in the daily schedule.
 */
@Composable
fun CalendarOrderCard(
    scheduledOrder: ScheduledOrder,
    onCardClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onRescheduleClick: () -> Unit,
    onAdvanceStatus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val order = scheduledOrder.order
    val customer = scheduledOrder.customer

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(order.promisedDeliveryDate)

    val (statusLabel, statusColor, nextAction) = when (order.status) {
        "NEW" -> Triple("NEW", CharcoalSubtle, "Start Cutting" to "CUTTING")
        "CUTTING" -> Triple("CUTTING", SoftGold, "Start Stitch" to "STITCHING")
        "STITCHING" -> Triple("STITCHING", SoftGold, "Finishing" to "FINISHING")
        "FINISHING" -> Triple("FINISHING", SoftGold, "Mark Ready" to "READY")
        "READY" -> Triple("READY", StatusGreen, "Mark Delivered" to "DELIVERED")
        "DELIVERED" -> Triple("DELIVERED", MutedSage, null)
        "CANCELLED" -> Triple("CANCELLED", StatusRed, null)
        else -> Triple(order.status, CharcoalSubtle, null)
    }

    TailorElevatedCard(
        modifier = modifier.clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Order #, Status pill, Due amount
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "#${order.orderNumber}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Customer Name & Phone
            Text(
                text = customer?.name ?: "Customer",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Charcoal
            )

            // Promised delivery time and balance due
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = CharcoalSubtle,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Delivery by $formattedTime",
                        style = MaterialTheme.typography.labelMedium,
                        color = CharcoalSubtle
                    )
                }

                if (order.balanceAmount > 0) {
                    Text(
                        text = "Due: " + com.tailorbook.core.util.FormattingUtils.formatCurrency(order.balanceAmount),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = StatusRed
                    )
                } else {
                    Text(
                        text = "Fully Paid",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = StatusGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row: Call, WhatsApp, Reschedule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCallClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", style = MaterialTheme.typography.labelSmall, color = DeepTeal)
                }

                OutlinedButton(
                    onClick = onWhatsAppClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("WhatsApp", style = MaterialTheme.typography.labelSmall, color = StatusGreen)
                }

                OutlinedButton(
                    onClick = onRescheduleClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null, tint = CharcoalSubtle, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.calendar_reschedule), style = MaterialTheme.typography.labelSmall, color = Charcoal)
                }
            }

            if (nextAction != null) {
                val (actionTitle, nextStatus) = nextAction
                Spacer(modifier = Modifier.height(8.dp))
                TailorPrimaryButton(
                    text = actionTitle,
                    onClick = { onAdvanceStatus(nextStatus) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
