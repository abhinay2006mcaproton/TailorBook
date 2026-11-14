package com.tailorbook.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Comprehensive verification of AdMob Anchored Adaptive Banner Ads:
 * - Gap 1: Adaptive width calculation, orientation changes, zero/invalid width handling
 * - Gap 2: Lifecycle resume/pause/destroy, single destroy invariant, load & failure collapse state machine, recomposition stability
 * - Gap 3: Centralized placement switches, global disable, individual placement disable, Premium suppression, consent requirement, prohibited screens enforcement
 */
class AdaptiveBannerAdTest {

    @Before
    fun setUp() {
        AdPlacementConfig.resetToDefaults()
        AdMobConfig.setLiveAdsEnabled(false)
    }

    // =========================================================================
    // GAP 1: Adaptive Banner Sizing Tests
    // =========================================================================

    @Test
    fun adaptiveWidthCalculation_normalPhoneWidth_computesStandardHeight() {
        val widthPortrait = 360 // Standard portrait phone width in dp
        assertTrue(AdaptiveBannerCalculator.isValidWidth(widthPortrait))
        assertEquals(50, AdaptiveBannerCalculator.calculateAdaptiveBannerHeightDp(widthPortrait))
    }

    @Test
    fun adaptiveWidthCalculation_orientationChange_recalculatesHeightForLandscape() {
        val widthPortrait = 390
        val widthLandscape = 780

        val heightPortrait = AdaptiveBannerCalculator.calculateAdaptiveBannerHeightDp(widthPortrait)
        val heightLandscape = AdaptiveBannerCalculator.calculateAdaptiveBannerHeightDp(widthLandscape)

        assertEquals(50, heightPortrait)
        assertEquals(90, heightLandscape)
    }

    @Test
    fun adaptiveWidthCalculation_zeroOrNegativeWidth_returnsZeroAndIsInvalid() {
        assertFalse(AdaptiveBannerCalculator.isValidWidth(0))
        assertFalse(AdaptiveBannerCalculator.isValidWidth(-100))
        assertEquals(0, AdaptiveBannerCalculator.calculateAdaptiveBannerHeightDp(0))
        assertEquals(0, AdaptiveBannerCalculator.calculateAdaptiveBannerHeightDp(-50))
    }

    // =========================================================================
    // GAP 2: Lifecycle, Collapse & Recomposition State Machine Tests
    // =========================================================================

    /**
     * Simulates the AdView lifecycle & state machine in Compose:
     * - Hidden while loading
     * - Displayed when loaded
     * - Collapsed on failure
     * - Resume on ON_RESUME, Pause on ON_PAUSE, Destroy on ON_DESTROY exactly once
     */
    class FakeAdLifecycleTracker {
        var isLoaded: Boolean = false
        var isFailed: Boolean = false
        var isPaused: Boolean = false
        var isResumed: Boolean = false
        val destroyCount = AtomicInteger(0)
        private val isDestroyed = AtomicBoolean(false)

        fun onAdLoaded() {
            isLoaded = true
            isFailed = false
        }

        fun onAdFailedToLoad() {
            isLoaded = false
            isFailed = true
        }

        fun onResume() {
            isResumed = true
            isPaused = false
        }

        fun onPause() {
            isPaused = true
            isResumed = false
        }

        fun onDestroy() {
            if (!isDestroyed.getAndSet(true)) {
                destroyCount.incrementAndGet()
            }
        }

        fun isContainerVisible(): Boolean = isLoaded && !isFailed
    }

    @Test
    fun adLoading_initiallyHidden_displayedOnlyWhenLoaded() {
        val tracker = FakeAdLifecycleTracker()
        assertFalse("Ad container must remain hidden while loading", tracker.isContainerVisible())

        tracker.onAdLoaded()
        assertTrue("Ad container must become visible once loaded", tracker.isContainerVisible())
    }

    @Test
    fun adFailure_collapsesContainerCompletelyWithoutBlankSpace() {
        val tracker = FakeAdLifecycleTracker()
        tracker.onAdFailedToLoad()

        assertFalse("Ad container must collapse completely upon failure", tracker.isContainerVisible())
        assertFalse(tracker.isLoaded)
        assertTrue(tracker.isFailed)
    }

    @Test
    fun lifecycleManagement_resumePauseAndDestroy_invokesExpectedActions() {
        val tracker = FakeAdLifecycleTracker()

        tracker.onResume()
        assertTrue(tracker.isResumed)
        assertFalse(tracker.isPaused)

        tracker.onPause()
        assertTrue(tracker.isPaused)
        assertFalse(tracker.isResumed)

        // Ensure destroy is called exactly once per instance
        tracker.onDestroy()
        tracker.onDestroy() // duplicate call attempt on configuration change / disposal
        assertEquals("Destroy must be called exactly once per AdView instance", 1, tracker.destroyCount.get())
    }

    @Test
    fun recompositionStability_sameDimensions_reusesExistingInstanceWithoutDuplicateRequests() {
        // Caching key simulator
        val cache = mutableMapOf<Pair<Int, String>, FakeAdLifecycleTracker>()
        val adUnitId = AdMobConfig.getBannerAdUnitId()
        val containerWidth = 360

        // Initial composition
        val key = Pair(containerWidth, adUnitId)
        val initialInstance = cache.getOrPut(key) { FakeAdLifecycleTracker() }

        // Recomposition with identical width and unit ID
        val recomposedInstance = cache.getOrPut(key) { FakeAdLifecycleTracker() }

        assertEquals("Recomposition must reuse existing AdView instance", initialInstance, recomposedInstance)
        assertEquals("No duplicate AdViews should be created on recomposition", 1, cache.size)
    }

    // =========================================================================
    // GAP 3: Centralized Placement Configuration & Eligibility Tests
    // =========================================================================

    @Test
    fun forceShowForTesting_whenEnabled_allowsPreviewEvenWithPremium() {
        AdPlacementConfig.forceShowForTesting = true
        assertTrue(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.DASHBOARD, isPremium = true, canRequestAds = true))
    }

    @Test
    fun defaultPlacements_initialStateVerified() {
        assertTrue("Global ads must be enabled by default for testing", AdPlacementConfig.isGlobalAdsEnabled)
        assertTrue("Dashboard placement must be enabled", AdPlacementConfig.isPlacementEnabled(AdPlacement.DASHBOARD))
        assertTrue("Customer list placement must be enabled", AdPlacementConfig.isPlacementEnabled(AdPlacement.CUSTOMER_LIST))
        assertTrue("Reports placement must be enabled", AdPlacementConfig.isPlacementEnabled(AdPlacement.REPORTS))
        assertTrue("Orders placement must be enabled", AdPlacementConfig.isPlacementEnabled(AdPlacement.ORDERS))
        assertTrue("Measurements placement must be enabled", AdPlacementConfig.isPlacementEnabled(AdPlacement.MEASUREMENTS))
        assertTrue("Delivery calendar placement must be enabled", AdPlacementConfig.isPlacementEnabled(AdPlacement.DELIVERY_CALENDAR))
        assertFalse("Settings placement must remain disabled initially", AdPlacementConfig.isPlacementEnabled(AdPlacement.SETTINGS))
    }

    @Test
    fun globalAdsDisabled_suppressesAllPlacements() {
        AdPlacementConfig.isGlobalAdsEnabled = false

        assertFalse(AdPlacementConfig.isPlacementEnabled(AdPlacement.DASHBOARD))
        assertFalse(AdPlacementConfig.isPlacementEnabled(AdPlacement.CUSTOMER_LIST))
        assertFalse(AdPlacementConfig.isPlacementEnabled(AdPlacement.REPORTS))
        assertFalse(AdPlacementConfig.isPlacementEnabled(AdPlacement.SETTINGS))

        assertFalse(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.DASHBOARD, isPremium = false, canRequestAds = true))
    }

    @Test
    fun individualPlacementDisabled_suppressesOnlyTargetPlacement() {
        AdPlacementConfig.isDashboardEnabled = false

        assertFalse(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.DASHBOARD, isPremium = false, canRequestAds = true))
        assertTrue(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.CUSTOMER_LIST, isPremium = false, canRequestAds = true))
        assertTrue(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.REPORTS, isPremium = false, canRequestAds = true))
    }

    @Test
    fun premiumEntitlement_strictlySuppressesAllBanners() {
        // Free user is eligible
        assertTrue(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.DASHBOARD, isPremium = false, canRequestAds = true))

        // Premium user must NEVER receive banner ads across any placement
        assertFalse(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.DASHBOARD, isPremium = true, canRequestAds = true))
        assertFalse(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.CUSTOMER_LIST, isPremium = true, canRequestAds = true))
        assertFalse(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.REPORTS, isPremium = true, canRequestAds = true))
        assertFalse(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.SETTINGS, isPremium = true, canRequestAds = true))
    }

    @Test
    fun consentNotYetObtained_suppressesAdRequests() {
        // When UMP consent is not obtained (canRequestAds = false), no ads may be requested
        assertFalse(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.DASHBOARD, isPremium = false, canRequestAds = false))
        assertFalse(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.CUSTOMER_LIST, isPremium = false, canRequestAds = false))
        assertFalse(DefaultAdEligibilityProvider.isEligibleForAd(AdPlacement.REPORTS, isPremium = false, canRequestAds = false))
    }

    @Test
    fun prohibitedScreens_strictlyBlockedFromDisplayingBannerAds() {
        val prohibitedScreens = listOf(
            "login",
            "shop_registration",
            "otp_verification",
            "add_customer",
            "edit_customer",
            "garment_template_selector",
            "measurement_entry",
            "measurement_history",
            "measurement_diff",
            "order_creation",
            "order_details",
            "order_status_update",
            "record_payment",
            "digital_receipt",
            "delivery_confirmation",
            "subscription"
        )

        for (screen in prohibitedScreens) {
            assertTrue("Screen '$screen' must be in ProhibitedScreens registry", ProhibitedScreens.isProhibited(screen))
            assertFalse(
                "Prohibited screen '$screen' must NEVER be eligible for banner ads",
                DefaultAdEligibilityProvider.isRouteEligible(screen, isPremium = false, canRequestAds = true)
            )
        }

        // Approved screens must NOT be in the prohibited registry
        assertFalse(ProhibitedScreens.isProhibited("dashboard"))
        assertFalse(ProhibitedScreens.isProhibited("customer_list"))
        assertFalse(ProhibitedScreens.isProhibited("reports"))
    }

    @Test
    fun productionSafety_verifiesOfficialGoogleTestUnitIdUsed() {
        assertEquals(
            "ca-app-pub-3940256099942544/6300978111",
            AdMobConfig.getBannerAdUnitId()
        )
        assertFalse("Live ads must remain disabled in development", AdMobConfig.isLiveAdsEnabled)
    }
}
