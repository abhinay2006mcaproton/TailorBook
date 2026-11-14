package com.tailorbook.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 light color scheme for TailorBook Premium Boutique palette.
 *
 * TailorBook is intentionally light-theme only for the MVP. The boutique
 * aesthetic relies on Warm Ivory backgrounds and Deep Teal accents, which
 * do not adapt well to an automatic dark mode without a dedicated dark
 * color system. Dark mode is planned as a post-MVP enhancement.
 */
private val LightColorScheme = lightColorScheme(
    primary             = DeepTeal,
    onPrimary           = CardWhite,
    primaryContainer    = MutedSage,
    onPrimaryContainer  = DeepTealDark,
    secondary           = SoftGold,
    onSecondary         = CardWhite,
    secondaryContainer  = LightBeige,
    onSecondaryContainer= Charcoal,
    tertiary            = MutedSage,
    onTertiary          = CardWhite,
    tertiaryContainer   = LightBeige,
    onTertiaryContainer = Charcoal,
    background          = WarmIvory,
    onBackground        = Charcoal,
    surface             = CardWhite,
    onSurface           = Charcoal,
    surfaceVariant      = LightBeige,
    onSurfaceVariant    = CharcoalSubtle,
    outline             = MutedSage,
    outlineVariant      = LightBeige,
    error               = StatusRed,
    onError             = CardWhite,
    errorContainer      = StatusRedLight,
    onErrorContainer    = StatusRed,
    scrim               = Scrim
)

/**
 * Root theme composable for TailorBook.
 *
 * Features:
 * - Always uses the light colour scheme (dark mode is a future enhancement).
 * - Switches typography to [TailorBookTypographyTelugu] when the device locale
 *   is Telugu ("te"), so diacritic-heavy glyphs get the wider lineHeight they
 *   need to prevent clipping.
 * - Sets the status bar colour to Deep Teal with light (white) icons.
 * - Uses [WindowCompat.setDecorFitsSystemWindows] = false so Compose content
 *   draws edge-to-edge on API 30+ while Scaffold handles the inset padding.
 *
 * @param content The composable content tree to be themed.
 */
@Composable
fun TailorBookTheme(
    content: @Composable () -> Unit
) {
    // Detect active locale to choose the appropriate typography
    val locale = LocalConfiguration.current.locales[0]
    val typography = if (locale.language == "te") {
        TailorBookTypographyTelugu
    } else {
        TailorBookTypography
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set the status bar to Deep Teal (#203B36) with white icons.
            // WindowCompat handles the edge-to-edge and inset compatibility
            // across API 26–36 without deprecated statusBarColor on API 35+.
            @Suppress("DEPRECATION")
            window.statusBarColor = DeepTeal.value.toInt()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false  // white icons on dark bg
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = typography,
        content     = content
    )
}
