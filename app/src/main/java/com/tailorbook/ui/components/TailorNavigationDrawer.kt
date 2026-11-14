package com.tailorbook.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tailorbook.R
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.DeepTealDark
import com.tailorbook.ui.theme.DrawerHeaderBg
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.TailorBookTheme

// ─────────────────────────────────────────────────────────────────────────────
// TailorNavigationDrawer
//
// Design spec:
//   - ModalNavigationDrawer (left-side drawer, swipe or hamburger to open)
//   - Header: Deep Teal bg, shop logo placeholder (circle), shop name, tagline
//   - Items: 13 navigation destinations grouped by section
//   - Selected item: Deep Teal tint with subtle highlight
//   - Footer area: version / subscription tier label
//
// Navigation destinations (in order):
//   1. Dashboard
//   2. Customers
//   3. Measurements
//   4. Orders
//   5. Delivery Calendar
//   6. Payments
//   7. Design Gallery
//   8. Reports
//   9. Staff Management
//  10. Subscription & Premium
//  11. Notifications
//  12. Settings
//  13. Help & Support
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Enum of all navigation destinations in the drawer.
 * Each entry carries the string resource ID, selected/unselected icons,
 * and an optional badge count for unread notifications.
 */
enum class DrawerDestination(
    val labelResId: Int,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
) {
    Dashboard(
        R.string.nav_dashboard,
        Icons.Filled.Dashboard,
        Icons.Outlined.Dashboard
    ),
    Customers(
        R.string.nav_customers,
        Icons.Filled.People,
        Icons.Outlined.People
    ),
    Measurements(
        R.string.nav_measurements,
        Icons.Filled.Straighten,
        Icons.Outlined.Straighten
    ),
    Orders(
        R.string.nav_orders,
        Icons.Filled.ContentCut,
        Icons.Outlined.ContentCut
    ),
    DeliveryCalendar(
        R.string.nav_delivery_calendar,
        Icons.Filled.CalendarMonth,
        Icons.Outlined.CalendarMonth
    ),
    Payments(
        R.string.nav_payments,
        Icons.Filled.Payment,
        Icons.Outlined.Payment
    ),
    DesignGallery(
        R.string.nav_design_gallery,
        Icons.Filled.Photo,
        Icons.Outlined.Photo
    ),
    Reports(
        R.string.nav_reports,
        Icons.Filled.Analytics,
        Icons.Outlined.Analytics
    ),
    StaffManagement(
        R.string.nav_staff_management,
        Icons.Filled.Groups,
        Icons.Outlined.Groups
    ),
    Subscription(
        R.string.nav_subscription,
        Icons.Filled.Star,
        Icons.Outlined.Star
    ),
    Notifications(
        R.string.nav_notifications,
        Icons.Filled.Notifications,
        Icons.Outlined.Notifications
    ),
    Settings(
        R.string.nav_settings,
        Icons.Filled.Settings,
        Icons.Outlined.Settings
    ),
    HelpSupport(
        R.string.nav_help_support,
        Icons.AutoMirrored.Filled.Help,
        Icons.AutoMirrored.Outlined.Help
    )
}

/**
 * Root navigation drawer composable.
 *
 * Wraps [content] in a [ModalNavigationDrawer]. The drawer opens from the left
 * and can be toggled programmatically via [drawerState]. In full navigation
 * integration (Phase 05+), pass the NavController destination to [selectedDestination].
 *
 * @param drawerState         Controls open/closed state. Use [rememberDrawerState].
 * @param selectedDestination Currently selected nav destination (highlighted in drawer).
 * @param shopName            Shop name shown in the drawer header.
 * @param onDestinationSelected Callback when a nav item is tapped. Close drawer and navigate.
 * @param notificationBadgeCount Unread notifications count, shown on the Notifications item.
 * @param content             The main screen content behind the drawer.
 */
@Composable
fun TailorNavigationDrawer(
    drawerState: DrawerState,
    selectedDestination: DrawerDestination = DrawerDestination.Dashboard,
    shopName: String = "My Boutique",
    accountEmail: String? = null,
    accountPhone: String? = null,
    onDestinationSelected: (DrawerDestination) -> Unit = {},
    notificationBadgeCount: Int = 0,
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            TailorDrawerSheet(
                selectedDestination = selectedDestination,
                shopName = shopName,
                accountEmail = accountEmail,
                accountPhone = accountPhone,
                onDestinationSelected = onDestinationSelected,
                notificationBadgeCount = notificationBadgeCount
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.5f),
        content = content
    )
}

/**
 * The visual content of the drawer sheet.
 * Separated from [TailorNavigationDrawer] so it can be previewed independently.
 */
@Composable
fun TailorDrawerSheet(
    selectedDestination: DrawerDestination,
    shopName: String,
    accountEmail: String? = null,
    accountPhone: String? = null,
    onDestinationSelected: (DrawerDestination) -> Unit,
    notificationBadgeCount: Int = 0
) {
    ModalDrawerSheet(
        drawerContainerColor = CardWhite,
        drawerTonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // ── Drawer Header ──────────────────────────────────────────────
            DrawerHeader(
                shopName = shopName,
                accountEmail = accountEmail,
                accountPhone = accountPhone
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Primary navigation items ───────────────────────────────────
            // Group 1: Core business
            DrawerSectionLabel("Business")
            DrawerNavItem(DrawerDestination.Dashboard, selectedDestination, onDestinationSelected)
            DrawerNavItem(DrawerDestination.Customers, selectedDestination, onDestinationSelected)
            DrawerNavItem(DrawerDestination.Measurements, selectedDestination, onDestinationSelected)
            DrawerNavItem(DrawerDestination.Orders, selectedDestination, onDestinationSelected)
            DrawerNavItem(DrawerDestination.DeliveryCalendar, selectedDestination, onDestinationSelected)

            TailorDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            // Group 2: Finance & media
            DrawerSectionLabel("Finance & Media")
            DrawerNavItem(DrawerDestination.Payments, selectedDestination, onDestinationSelected)
            DrawerNavItem(DrawerDestination.DesignGallery, selectedDestination, onDestinationSelected)
            DrawerNavItem(DrawerDestination.Reports, selectedDestination, onDestinationSelected)

            TailorDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            // Group 3: Settings & account
            DrawerSectionLabel("Account")
            DrawerNavItem(DrawerDestination.StaffManagement, selectedDestination, onDestinationSelected)
            DrawerNavItem(DrawerDestination.Subscription, selectedDestination, onDestinationSelected)
            DrawerNavItem(
                destination = DrawerDestination.Notifications,
                selected = selectedDestination,
                onClick = onDestinationSelected,
                badgeCount = notificationBadgeCount
            )
            DrawerNavItem(DrawerDestination.Settings, selectedDestination, onDestinationSelected)
            DrawerNavItem(DrawerDestination.HelpSupport, selectedDestination, onDestinationSelected)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Footer: app version / tier hint ───────────────────────────
            DrawerFooter()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal sub-composables
// ─────────────────────────────────────────────────────────────────────────────

/** Deep Teal header with shop logo placeholder, shop name and logged-in account identity. */
@Composable
private fun DrawerHeader(
    shopName: String,
    accountEmail: String? = null,
    accountPhone: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DrawerHeaderBg)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Column {
            // Boutique app logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tailorbook_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = shopName,
                        style = MaterialTheme.typography.titleMedium,
                        color = CardWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    val loginIdentity = accountEmail?.ifBlank { null } ?: accountPhone?.ifBlank { null }
                    if (!loginIdentity.isNullOrBlank()) {
                        Text(
                            text = loginIdentity,
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftGold,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = "Powered by TailorBook",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSage
                        )
                    }
                }
            }
        }
    }
}

/** Section label — small all-caps heading between item groups. */
@Composable
private fun DrawerSectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = CharcoalSubtle,
        modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 4.dp)
    )
}

/** Single navigation drawer item row. */
@Composable
private fun DrawerNavItem(
    destination: DrawerDestination,
    selected: DrawerDestination,
    onClick: (DrawerDestination) -> Unit,
    badgeCount: Int = 0
) {
    val isSelected = destination == selected
    NavigationDrawerItem(
        label = {
            Text(
                text = stringResource(id = destination.labelResId),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) DeepTeal else Charcoal
            )
        },
        icon = {
            Icon(
                imageVector = if (isSelected) destination.iconSelected else destination.iconUnselected,
                contentDescription = stringResource(id = destination.labelResId),
                tint = if (isSelected) DeepTeal else CharcoalSubtle
            )
        },
        badge = if (badgeCount > 0) {
            {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DeepTeal)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = CardWhite
                    )
                }
            }
        } else null,
        selected = isSelected,
        onClick = { onClick(destination) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor   = DeepTeal.copy(alpha = 0.10f),
            unselectedContainerColor = Color.Transparent,
            selectedIconColor        = DeepTeal,
            unselectedIconColor      = CharcoalSubtle,
            selectedTextColor        = DeepTeal,
            unselectedTextColor      = Charcoal
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    )
}

/** Footer row showing app version and subscription tier. */
@Composable
private fun DrawerFooter() {
    TailorDivider(modifier = Modifier.padding(horizontal = 16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TailorBook v1.0",
            style = MaterialTheme.typography.labelSmall,
            color = CharcoalSubtle
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(LightBeige)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Free",
                style = MaterialTheme.typography.labelSmall,
                color = MutedSage,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TailorDrawerSheetPreview() {
    TailorBookTheme {
        TailorDrawerSheet(
            selectedDestination = DrawerDestination.Dashboard,
            shopName = "Lakshmi Boutique",
            onDestinationSelected = {},
            notificationBadgeCount = 3
        )
    }
}
