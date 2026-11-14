package com.tailorbook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.TailorBookTheme
import com.tailorbook.ui.theme.WarmIvory

// ─────────────────────────────────────────────────────────────────────────────
// TailorCard — Elevated and bordered card variants
//
// Design spec:
//   Elevated card  — 16dp corners, 2dp elevation, White (#FFF) surface
//   Bordered card  — 12dp corners, 0dp elevation, 1dp Light Beige border
//   Clickable card — Adds ripple and pointer cursor on API 24+
//
// Usage guideline:
//   Use TailorElevatedCard for primary content blocks (customer profiles,
//   order summaries, dashboard metrics).
//   Use TailorBorderedCard for secondary info panels, inline detail rows,
//   and nested sections inside an elevated card.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Elevated card — the primary card surface in TailorBook.
 *
 * White background with 2dp shadow and 16dp rounded corners.
 * Optionally clickable (adds ripple) when [onClick] is provided.
 *
 * @param modifier   Layout modifier. Usually [Modifier.fillMaxWidth].
 * @param onClick    When non-null, the card is tappable and shows a ripple.
 * @param elevation  Card elevation in dp (default 2.dp — subtle shadow).
 * @param content    Composable content inside the card.
 */
@Composable
fun TailorElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 2.dp,
    containerColor: Color = CardWhite,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(content = content)
    }
}

/**
 * Bordered card — secondary / nested content surface.
 *
 * Warm Ivory background with a 1dp Light Beige border and 12dp rounded corners.
 * Zero elevation (flat). Good for measurement rows, payment line items,
 * order detail sub-sections.
 *
 * @param modifier   Layout modifier.
 * @param onClick    When non-null, the card is tappable.
 * @param borderColor Border colour (default [LightBeige]).
 * @param content    Composable content inside the card.
 */
@Composable
fun TailorBorderedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = LightBeige,
    containerColor: Color = WarmIvory,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    } else {
        modifier
    }

    OutlinedCard(
        modifier = cardModifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(width = 1.dp, color = borderColor),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

/**
 * Section card — elevated card with a labelled header area.
 *
 * Used for grouping related fields on detail screens (e.g., "Contact Details",
 * "Measurement Details", "Payment Summary"). The [header] is rendered inside a
 * padded row above the [content] body, separated by a [TailorDivider].
 *
 * @param title   Section heading text.
 * @param modifier Layout modifier.
 * @param content  Section body content.
 */
@Composable
fun TailorSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    TailorElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Charcoal
            )
        }
        TailorDivider()
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            content = content
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TailorDivider
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Horizontal divider styled to TailorBook's Light Beige (#E7E2D9).
 *
 * Replaces [HorizontalDivider] or [Divider] across the app to keep the
 * divider colour consistent with the brand palette.
 *
 * @param modifier  Layout modifier (usually just fill width, which is the default).
 * @param thickness Divider thickness (default 1.dp).
 * @param color     Divider colour (default [LightBeige]).
 */
@Composable
fun TailorDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = LightBeige
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}

/**
 * A divider with equal vertical spacing above and below, for use inside lists
 * and between card sections.
 *
 * @param verticalPadding Padding above and below the divider line (default 8.dp).
 */
@Composable
fun TailorSpacedDivider(
    verticalPadding: Dp = 8.dp,
    color: Color = LightBeige
) {
    Spacer(modifier = Modifier.height(verticalPadding))
    TailorDivider(color = color)
    Spacer(modifier = Modifier.height(verticalPadding))
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorElevatedCardPreview() {
    TailorBookTheme {
        TailorElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Customer: Priya Reddy", style = MaterialTheme.typography.titleMedium, color = Charcoal)
                Spacer(modifier = Modifier.height(4.dp))
                Text("📞 98765 43210", style = MaterialTheme.typography.bodyMedium, color = CharcoalSubtle)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorBorderedCardPreview() {
    TailorBookTheme {
        TailorBorderedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bust / Chest: 36 in", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                Text("Waist: 30 in", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                Text("Hip: 38 in", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorSectionCardPreview() {
    TailorBookTheme {
        TailorSectionCard(
            title = "Measurement Details",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Bust: 36 in", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Waist: 30 in", style = MaterialTheme.typography.bodyMedium, color = Charcoal)
        }
    }
}
