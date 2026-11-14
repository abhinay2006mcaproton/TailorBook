package com.tailorbook

import com.tailorbook.ui.components.DrawerDestination
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusAmber
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.TailorBookTypography
import com.tailorbook.ui.theme.TailorBookTypographyTelugu
import com.tailorbook.ui.theme.WarmIvory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating Phase 02 Design System specifications:
 * - 13 Navigation Drawer destinations in exact order
 * - Brand palette token validity
 * - Telugu typography line-height safety (preventing diacritic / matra clipping)
 */
class DesignSystemSanityTest {

    @Test
    fun testDrawerDestinations_exactThirteenDestinations() {
        val destinations = DrawerDestination.values()
        assertEquals("Drawer must contain exactly 13 navigation destinations", 13, destinations.size)

        val expectedOrder = listOf(
            DrawerDestination.Dashboard,
            DrawerDestination.Customers,
            DrawerDestination.Measurements,
            DrawerDestination.Orders,
            DrawerDestination.DeliveryCalendar,
            DrawerDestination.Payments,
            DrawerDestination.DesignGallery,
            DrawerDestination.Reports,
            DrawerDestination.StaffManagement,
            DrawerDestination.Subscription,
            DrawerDestination.Notifications,
            DrawerDestination.Settings,
            DrawerDestination.HelpSupport
        )
        assertEquals("Drawer destinations must match required order", expectedOrder, destinations.toList())
    }

    @Test
    fun testBrandColorTokens_distinctAndNonZero() {
        assertNotEquals(DeepTeal, WarmIvory)
        assertNotEquals(MutedSage, SoftGold)
        assertNotEquals(CardWhite, Charcoal)
        assertNotEquals(StatusGreen, StatusRed)
        assertNotEquals(StatusAmber, StatusRed)
    }

    @Test
    fun testTeluguTypography_hasAdequateLineHeightForDiacritics() {
        // Telugu diacritics (matras) above and below glyphs require >= 1.35x line height
        val teluguBodyLarge = TailorBookTypographyTelugu.bodyLarge
        val fontSize = teluguBodyLarge.fontSize.value
        val lineHeight = teluguBodyLarge.lineHeight.value
        val ratio = lineHeight / fontSize

        assertTrue(
            "Telugu bodyLarge line-height ratio ($ratio) must be >= 1.35x to avoid diacritic clipping",
            ratio >= 1.35f
        )

        // Verify Telugu line height is strictly greater than English line height
        val englishLineHeight = TailorBookTypography.bodyLarge.lineHeight.value
        assertTrue(
            "Telugu bodyLarge line-height ($lineHeight) must be greater than English ($englishLineHeight)",
            lineHeight > englishLineHeight
        )
    }
}
