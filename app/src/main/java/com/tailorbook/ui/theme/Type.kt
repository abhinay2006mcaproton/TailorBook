package com.tailorbook.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tailorbook.R

/**
 * Noto Sans Telugu FontFamily.
 *
 * Uses the downloadable font descriptor at res/font/noto_sans_telugu.xml.
 * The Google Fonts provider caches it on first use. If Play Services are
 * unavailable the system falls back to the nearest available Telugu system
 * font (Android 8+ ships Noto Sans Telugu as part of AOSP).
 *
 * LINE HEIGHT NOTE:
 * Telugu script uses stacked diacritics (matras) above and below the base
 * glyph. To prevent clipping of characters like కొ, కు, కా, గి we use a
 * lineHeight multiplier of ~1.4× for body text and ~1.35× for display text.
 * This is applied in the Telugu-aware typography variant below.
 */
val NotoSansTelugu = FontFamily(
    Font(R.font.noto_sans_telugu, weight = FontWeight.Normal, style = FontStyle.Normal)
)

/**
 * Default English typography using system Roboto.
 * Used when the app locale is English (default).
 */
val TailorBookTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Telugu-aware typography variant.
 *
 * Identical to [TailorBookTypography] but uses [NotoSansTelugu] as the
 * fontFamily and increases lineHeight by ~1.35× to prevent matra clipping.
 *
 * Usage: pass this to MaterialTheme when the active locale is Telugu.
 * The locale-switching logic lives in TailorBookTheme.
 */
val TailorBookTypographyTelugu = Typography(
    displayLarge = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 48.sp,        // 1.5× for top-level display glyphs
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 38.sp,        // 1.58× for headline
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 34.sp,        // 1.7× to protect tall akshara stacks
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 28.sp,        // 1.75×
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 26.sp,        // ~1.73× — primary reading text
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp,        // ~1.71×
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NotoSansTelugu,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )
)
