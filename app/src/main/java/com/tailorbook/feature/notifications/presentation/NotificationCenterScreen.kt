package com.tailorbook.feature.notifications.presentation

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorEmptyState
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusAmber
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.WarmIvory

/**
 * In-App Notification Center (TASK-11-02 / REQ-NOTIF-01 / REQ-NOTIF-02).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    viewModel: NotificationViewModel,
    onNavigateBack: () -> Unit,
    onOrderClick: (orderId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifications by viewModel.activeNotifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.notifications_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = if (unreadCount > 0) "$unreadCount active alerts" else stringResource(R.string.notifications_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = CardWhite
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text(
                                text = stringResource(R.string.notifications_mark_all_read),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CardWhite
                            )
                        }
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
        if (notifications.isEmpty()) {
            TailorEmptyState(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.notifications_empty),
                subtitle = stringResource(R.string.notifications_empty_desc),
                actionLabel = null,
                onAction = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(notifications, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onCardClick = {
                            viewModel.markAsRead(item.id)
                            item.orderId?.let { onOrderClick(it) }
                        },
                        onCallClick = { phone ->
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        },
                        onWhatsAppClick = { phone, title ->
                            val text = "Hello! Update regarding your tailoring order: $title."
                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=91$phone&text=${Uri.encode(text)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: BusinessNotification,
    onCardClick: () -> Unit,
    onCallClick: (String) -> Unit,
    onWhatsAppClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeFg, icon) = when (item.type) {
        NotificationType.OVERDUE -> Triple(StatusRed.copy(alpha = 0.15f), StatusRed, Icons.Default.Warning)
        NotificationType.DUE_TODAY -> Triple(SoftGold.copy(alpha = 0.2f), DeepTeal, Icons.Default.Alarm)
        NotificationType.DUE_TOMORROW -> Triple(LightBeige, DeepTeal, Icons.Default.CalendarToday)
        NotificationType.PAYMENT_DUE -> Triple(StatusGreen.copy(alpha = 0.15f), StatusGreen, Icons.Default.AttachMoney)
    }

    TailorElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Icon Badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold
                            ),
                            color = Charcoal,
                            modifier = Modifier.weight(1f)
                        )

                        if (!item.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(badgeFg)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalSubtle
                    )
                }
            }

            // Quick Client Communication Shortcuts
            if (!item.customerPhone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onCallClick(item.customerPhone) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Call", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { onWhatsAppClick(item.customerPhone, item.title) },
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
            }
        }
    }
}
