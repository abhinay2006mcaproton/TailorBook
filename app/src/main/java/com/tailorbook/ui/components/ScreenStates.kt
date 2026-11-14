package com.tailorbook.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tailorbook.R
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.OfflineBannerBg
import com.tailorbook.ui.theme.StatusAmber
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.TailorBookTheme
import com.tailorbook.ui.theme.WarmIvory

// ─────────────────────────────────────────────────────────────────────────────
// ScreenStates — Loading, Empty, Error, Offline, Syncing
//
// All screens must handle these five states from the very first implementation.
// Showing one of these composables instead of a blank screen or crash is a
// hard requirement for every screen in TailorBook.
//
// Usage pattern in a screen composable:
//   when (uiState) {
//     is UiState.Loading  -> TailorLoadingState()
//     is UiState.Empty    -> TailorEmptyState(...)
//     is UiState.Error    -> TailorErrorState(message = uiState.message, onRetry = viewModel::load)
//     is UiState.Success  -> { /* render content */ }
//   }
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// TailorLoadingState
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen loading state with a centred Deep Teal circular progress indicator.
 *
 * Also exposes [TailorShimmerCard] for content-aware shimmer skeletons on
 * specific screens (customer list, order list, etc.).
 *
 * @param modifier Layout modifier — defaults to filling the available space.
 */
@Composable
fun TailorLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = DeepTeal,
                trackColor = LightBeige,
                strokeWidth = 3.dp
            )
            Text(
                text = stringResource(id = R.string.state_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalSubtle
            )
        }
    }
}

/**
 * Shimmer skeleton card — used to render placeholder cards while data is loading.
 *
 * The shimmer gradient travels left-to-right using an infinite animation.
 * Use multiple instances in a Column to mimic the expected list layout.
 *
 * @param modifier Layout modifier. Typically [Modifier.fillMaxWidth()].
 * @param height   Height of the shimmer block (default 80.dp for a list row).
 */
@Composable
fun TailorShimmerCard(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 80.dp
) {
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by shimmerTransition.animateFloat(
        initialValue = -300f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            LightBeige,
            CardWhite.copy(alpha = 0.9f),
            LightBeige
        ),
        start = Offset(shimmerTranslate, 0f),
        end   = Offset(shimmerTranslate + 400f, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(brush = shimmerBrush, shape = RoundedCornerShape(12.dp))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// TailorEmptyState
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen empty state.
 *
 * Shown when a list or section has no data yet (no customers, no orders, etc.).
 * Includes an icon, title, subtitle, and an optional call-to-action button.
 *
 * @param icon        Icon to display (should be a large outline icon, ~80dp).
 * @param title       Primary heading, e.g. "No Customers Yet".
 * @param subtitle    Explanatory text, e.g. "Add your first customer to get started.".
 * @param actionLabel Label for the action button. Pass null to hide the button.
 * @param onAction    Callback for the action button.
 * @param modifier    Layout modifier.
 */
@Composable
fun TailorEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon background circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(LightBeige, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MutedSage
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Charcoal,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalSubtle,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TailorPrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private val CircleShape = RoundedCornerShape(50)

// ─────────────────────────────────────────────────────────────────────────────
// TailorErrorState
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen error state.
 *
 * Shown when a data load fails. Displays a non-technical message and a Retry
 * button. Never show raw exception messages to the user — pass a friendly
 * [message] from the ViewModel.
 *
 * @param message  Human-readable error description.
 * @param onRetry  Callback for the Retry button (triggers re-load in ViewModel).
 * @param modifier Layout modifier.
 */
@Composable
fun TailorErrorState(
    message: String = stringResource(id = R.string.state_error_generic),
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                color = Charcoal,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalSubtle,
                textAlign = TextAlign.Center
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TailorSecondaryButton(
                    text = stringResource(id = R.string.action_retry),
                    onClick = onRetry,
                    leadingIcon = Icons.Default.Refresh,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TailorOfflineBanner
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Persistent slim offline status banner.
 *
 * Rendered near the top of the screen content area (just below the TopAppBar)
 * when the device has no network connectivity. It is NOT shown when online.
 *
 * The banner is non-intrusive: 36dp height, full-width, Muted Sage/Charcoal
 * tint. It does NOT block user actions — data still works offline via Room.
 *
 * @param isOffline When true the banner is visible; when false it is hidden.
 * @param modifier  Layout modifier.
 */
@Composable
fun TailorOfflineBanner(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = fadeIn(),
        exit  = fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = OfflineBannerBg,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    modifier = Modifier.size(14.dp),
                    tint = CharcoalSubtle
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(id = R.string.state_offline_banner),
                    style = MaterialTheme.typography.labelSmall,
                    color = CharcoalSubtle
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TailorSyncBanner
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sync status banner — slim indicator below the TopAppBar.
 *
 * - [isSyncing] = true  → Amber banner: "Syncing with cloud..." with a spinning icon
 * - [isSyncing] = false and [hasPendingChanges] = true → warning: "Changes pending sync"
 * - Both false → banner is hidden
 *
 * @param isSyncing          True when a Supabase sync is actively in progress.
 * @param hasPendingChanges  True when offline changes are queued but not yet synced.
 * @param modifier           Layout modifier.
 */
@Composable
fun TailorSyncBanner(
    isSyncing: Boolean,
    hasPendingChanges: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isVisible = isSyncing || hasPendingChanges

    // Spin animation for the sync icon
    val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncRotation"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit  = fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = if (isSyncing) StatusAmber.copy(alpha = 0.15f) else LightBeige
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.SyncProblem,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .then(
                            if (isSyncing) Modifier.graphicsLayer { rotationZ = rotation }
                            else Modifier
                        ),
                    tint = if (isSyncing) StatusAmber else MutedSage
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = if (isSyncing) stringResource(id = R.string.state_syncing)
                           else "Changes pending sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSyncing) StatusAmber else MutedSage
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorLoadingStatePreview() {
    TailorBookTheme {
        TailorLoadingState()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorEmptyStatePreview() {
    TailorBookTheme {
        TailorEmptyState(
            icon = Icons.Default.ContentCut,
            title = "No Orders Yet",
            subtitle = "Your first order will appear here. Tap the button below to create one.",
            actionLabel = "Create First Order",
            onAction = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorErrorStatePreview() {
    TailorBookTheme {
        TailorErrorState(
            message = "We couldn't load your orders. Please check your connection and try again.",
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorOfflineBannerPreview() {
    TailorBookTheme {
        Column {
            TailorOfflineBanner(isOffline = true)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorSyncBannerPreview() {
    TailorBookTheme {
        Column {
            TailorSyncBanner(isSyncing = true)
            Spacer(modifier = Modifier.height(8.dp))
            TailorSyncBanner(isSyncing = false, hasPendingChanges = true)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorShimmerCardPreview() {
    TailorBookTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TailorShimmerCard()
            TailorShimmerCard()
            TailorShimmerCard(height = 60.dp)
        }
    }
}
