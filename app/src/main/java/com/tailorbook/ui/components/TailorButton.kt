package com.tailorbook.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.DeepTealDark
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.TailorBookTheme
import com.tailorbook.ui.theme.WarmIvory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut

// ─────────────────────────────────────────────────────────────────────────────
// TailorButton variants
//
// Design spec:
//   Primary   — Deep Teal bg (#203B36), White text, 52dp height, 12dp corners
//   Secondary — Outlined Deep Teal border, Warm Ivory bg, Deep Teal text
//   Danger    — Status Red bg, White text (used for destructive actions)
//   Text      — No background, Deep Teal text
//
// All buttons:
//   - Have minimum touch target of 48dp (height 52dp satisfies this)
//   - Show a [CircularProgressIndicator] in place of text when [isLoading]=true
//   - Are disabled and show no ripple when [enabled]=false
//   - Support full-width layout via [fillMaxWidth] modifier
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Primary action button.
 *
 * Deep Teal background with White text. Used for the main call-to-action on
 * any screen (e.g., "Save", "Create Order", "Confirm Delivery").
 *
 * @param text       Button label. Should be short — max 2–3 words.
 * @param onClick    Callback invoked when the button is tapped.
 * @param modifier   Optional layout modifier; use [Modifier.fillMaxWidth] for full-width.
 * @param isLoading  When true, the label is replaced by a small spinner and the
 *                   button is disabled to prevent double-taps.
 * @param enabled    Controls whether the button responds to taps.
 * @param leadingIcon Optional icon displayed before the label text.
 */
@Composable
fun TailorPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor  = DeepTeal,
            contentColor    = CardWhite,
            disabledContainerColor = MutedSage.copy(alpha = 0.4f),
            disabledContentColor   = CardWhite.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    ) {
        ButtonContent(
            text = text,
            isLoading = isLoading,
            leadingIcon = leadingIcon,
            contentColor = CardWhite
        )
    }
}

/**
 * Secondary / outlined button.
 *
 * Warm Ivory background with a Deep Teal border and Deep Teal text.
 * Used for secondary actions alongside a [TailorPrimaryButton]
 * (e.g., "Cancel", "View Details").
 */
@Composable
fun TailorSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = WarmIvory,
            contentColor   = DeepTeal,
            disabledContentColor = MutedSage
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (enabled && !isLoading) 1.5.dp else 1.dp,
            color = if (enabled && !isLoading) DeepTeal else MutedSage.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        ButtonContent(
            text = text,
            isLoading = isLoading,
            leadingIcon = leadingIcon,
            contentColor = DeepTeal
        )
    }
}

/**
 * Danger / destructive button.
 *
 * Uses Material 3 error color (Status Red) background for actions like
 * "Delete Customer", "Cancel Order". Always requires a confirmation dialog
 * before any destructive side effect — the button itself just triggers that flow.
 */
@Composable
fun TailorDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor   = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
            disabledContentColor   = CardWhite.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        ButtonContent(
            text = text,
            isLoading = isLoading,
            leadingIcon = null,
            contentColor = CardWhite
        )
    }
}

/**
 * Text-only button — no background, no border.
 *
 * Deep Teal text. Used for low-priority actions in dialogs or inline within
 * cards ("View all", "Learn more", "Forgot password").
 */
@Composable
fun TailorTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = DeepTeal
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAB variants
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Standard floating action button.
 *
 * Deep Teal background with a Soft Gold / White icon. Used for the primary
 * creation action on list screens (e.g., "Add Customer", "Add Order").
 *
 * @param icon             The icon to display inside the FAB.
 * @param contentDescription Accessibility label.
 * @param onClick          Callback when tapped.
 * @param iconTint         Icon colour — defaults to Soft Gold for premium feel.
 */
@Composable
fun TailorFloatingActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = SoftGold
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        containerColor = DeepTeal,
        contentColor   = iconTint
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint
        )
    }
}

/**
 * Extended FAB with an icon and a text label.
 *
 * Used on empty-state screens or the Dashboard to make the primary action
 * more discoverable ("+ New Order", "+ Add Customer").
 */
@Composable
fun TailorExtendedFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    iconTint: Color = SoftGold
) {
    ExtendedFloatingActionButton(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = CardWhite
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = iconTint
            )
        },
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        containerColor = DeepTeal,
        expanded = expanded
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal shared composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders either a spinner (when [isLoading]) or the icon+label row.
 * Kept internal — composable consumers use the typed button variants above.
 */
@Composable
private fun RowScope.ButtonContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: ImageVector?,
    contentColor: Color
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = contentColor,
            strokeWidth = 2.dp
        )
    } else {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.size(ButtonDefaults.IconSpacing)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorPrimaryButtonPreview() {
    TailorBookTheme {
        TailorPrimaryButton(
            text = "Create Order",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Default.ContentCut
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorSecondaryButtonPreview() {
    TailorBookTheme {
        TailorSecondaryButton(
            text = "Cancel",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorLoadingButtonPreview() {
    TailorBookTheme {
        TailorPrimaryButton(
            text = "Saving...",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            isLoading = true
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorFabPreview() {
    TailorBookTheme {
        TailorFloatingActionButton(
            icon = Icons.Default.Add,
            contentDescription = "Add",
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorExtendedFabPreview() {
    TailorBookTheme {
        TailorExtendedFab(
            text = "New Customer",
            icon = Icons.Default.Add,
            onClick = {}
        )
    }
}
