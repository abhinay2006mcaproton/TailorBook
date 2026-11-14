package com.tailorbook.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AdMob configuration and safety gates.
 */
class AdMobConfigTest {

    @Before
    fun setUp() {
        // Reset to test mode before each test
        AdMobConfig.setLiveAdsEnabled(false)
    }

    @Test
    fun defaultConfiguration_usesOfficialTestUnitIds() {
        assertFalse(AdMobConfig.isLiveAdsEnabled)
        assertEquals(AdMobConfig.TEST_REWARDED_AD_UNIT_ID, AdMobConfig.getRewardedAdUnitId())
        assertEquals(AdMobConfig.TEST_BANNER_AD_UNIT_ID, AdMobConfig.getBannerAdUnitId())
        assertEquals("ca-app-pub-3940256099942544/5224354917", AdMobConfig.TEST_REWARDED_AD_UNIT_ID)
        assertEquals("ca-app-pub-3940256099942544/6300978111", AdMobConfig.TEST_BANNER_AD_UNIT_ID)
        assertEquals("ca-app-pub-3940256099942544~3347511713", AdMobConfig.TEST_APP_ID)
    }

    @Test
    fun liveAdsToggle_whenToggled_updatesFlagCorrectly() {
        assertFalse(AdMobConfig.isLiveAdsEnabled)
        AdMobConfig.setLiveAdsEnabled(true)
        assertTrue(AdMobConfig.isLiveAdsEnabled)
        AdMobConfig.setLiveAdsEnabled(false)
        assertFalse(AdMobConfig.isLiveAdsEnabled)
    }
}
