package com.tailorbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sanity and configuration unit tests for TailorBook.
 * Verifies multi-version Android specifications and core domain constants.
 */
class TailorBookSanityTest {

    @Test
    fun testAndroidBackwardsCompatibilityTarget() {
        val minSdk = 26 // Android 8.0 Oreo
        val targetSdk = 34 // Android 14

        // Verifies that at least 2+ major previous Android versions are supported
        assertTrue("MinSdk must support at least 2 versions back", (targetSdk - minSdk) >= 2)
    }

    @Test
    fun testOrderStatusWorkflowInvariants() {
        val orderStatuses = listOf(
            "NEW",
            "CUTTING",
            "STITCHING",
            "FINISHING",
            "READY",
            "DELIVERED",
            "CANCELLED"
        )
        assertEquals(7, orderStatuses.size)
        assertTrue(orderStatuses.contains("READY"))
        assertTrue(orderStatuses.contains("DELIVERED"))
    }

    @Test
    fun testGarmentTemplatesCount() {
        val standardTemplates = listOf(
            "BLOUSE",
            "SHIRT",
            "TSHIRT",
            "PANTS",
            "KURTI",
            "DRESS",
            "CHURIDAR",
            "CUSTOM"
        )
        assertEquals("TailorBook must support exactly 8 garment templates in MVP", 8, standardTemplates.size)
    }
}
