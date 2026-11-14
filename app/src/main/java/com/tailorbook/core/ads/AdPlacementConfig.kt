package com.tailorbook.core.ads

/**
 * Approved placements for TailorBook banner advertisements.
 */
enum class AdPlacement {
    DASHBOARD,
    CUSTOMER_LIST,
    ORDERS,
    MEASUREMENTS,
    DELIVERY_CALENDAR,
    REPORTS,
    SETTINGS
}

/**
 * Comprehensive registry of screens where ads are strictly forbidden.
 * Prohibited placements:
 * - Login, registration, OTP and verification screens.
 * - Customer creation or editing.
 * - Measurement entry form (where values are input).
 * - Order creation wizard or item configuration.
 * - Payment entry or payment confirmation.
 * - Receipts, invoices and delivery confirmation.
 * - Subscription checkout and purchase screens.
 */
object ProhibitedScreens {
    val PROHIBITED_BASE_ROUTES = setOf(
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

    fun isProhibited(route: String?): Boolean {
        if (route.isNullOrBlank()) return false
        val baseRoute = route.substringBefore("?").substringBefore("/")
        return PROHIBITED_BASE_ROUTES.contains(baseRoute)
    }
}

/**
 * Centralized placement configuration.
 * Allows independent runtime toggling of global and per-placement ad display.
 */
object AdPlacementConfig {
    /** Global master switch for banner advertisements */
    var isGlobalAdsEnabled: Boolean = true

    /** Individual approved placement switches */
    var isDashboardEnabled: Boolean = true
    var isCustomerListEnabled: Boolean = true
    var isOrdersEnabled: Boolean = true
    var isMeasurementsEnabled: Boolean = true
    var isDeliveryCalendarEnabled: Boolean = true
    var isReportsEnabled: Boolean = true

    /** Settings placement is optional and must remain disabled initially */
    var isSettingsEnabled: Boolean = false

    /**
     * Preview switch for testing: when enabled, allows banners to be rendered for manual inspection
     * even if the active local session has VIP/Premium bypass. Defaults to false in production builds.
     */
    var forceShowForTesting: Boolean = false

    fun isPlacementEnabled(placement: AdPlacement): Boolean {
        if (!isGlobalAdsEnabled) return false
        return when (placement) {
            AdPlacement.DASHBOARD -> isDashboardEnabled
            AdPlacement.CUSTOMER_LIST -> isCustomerListEnabled
            AdPlacement.ORDERS -> isOrdersEnabled
            AdPlacement.MEASUREMENTS -> isMeasurementsEnabled
            AdPlacement.DELIVERY_CALENDAR -> isDeliveryCalendarEnabled
            AdPlacement.REPORTS -> isReportsEnabled
            AdPlacement.SETTINGS -> isSettingsEnabled
        }
    }

    fun resetToDefaults() {
        isGlobalAdsEnabled = true
        isDashboardEnabled = true
        isCustomerListEnabled = true
        isOrdersEnabled = true
        isMeasurementsEnabled = true
        isDeliveryCalendarEnabled = true
        isReportsEnabled = true
        isSettingsEnabled = false
        forceShowForTesting = false
    }
}

/**
 * Contract for determining whether an ad can be requested and displayed.
 */
interface AdEligibilityProvider {
    fun isEligibleForAd(
        placement: AdPlacement,
        isPremium: Boolean,
        canRequestAds: Boolean = true
    ): Boolean

    fun isRouteEligible(
        route: String?,
        isPremium: Boolean,
        canRequestAds: Boolean = true
    ): Boolean
}

/**
 * Single centralized eligibility evaluator enforcing all commercial and regulatory rules.
 */
object DefaultAdEligibilityProvider : AdEligibilityProvider {
    override fun isEligibleForAd(
        placement: AdPlacement,
        isPremium: Boolean,
        canRequestAds: Boolean
    ): Boolean {
        // 1. Global ads switch must be enabled
        if (!AdPlacementConfig.isGlobalAdsEnabled) return false

        // 2. Individual placement must be enabled in config
        if (!AdPlacementConfig.isPlacementEnabled(placement)) return false

        // 3. Testing preview bypass: if forceShowForTesting is true, allow developer/tester to visually confirm
        if (AdPlacementConfig.forceShowForTesting) {
            return true
        }

        // 4. Premium users must never receive banner ads (100% ad-free boutique experience)
        if (isPremium) return false

        // 5. Privacy consent must be obtained (or not required) via Google UMP
        if (!canRequestAds) return false

        return true
    }

    override fun isRouteEligible(
        route: String?,
        isPremium: Boolean,
        canRequestAds: Boolean
    ): Boolean {
        if (route.isNullOrBlank()) return false
        if (ProhibitedScreens.isProhibited(route)) return false
        if (!AdPlacementConfig.isGlobalAdsEnabled) return false
        if (isPremium) return false
        if (!canRequestAds) return false
        return true
    }
}

/**
 * Pure Kotlin utility for calculating anchored adaptive banner heights.
 * Follows Google Mobile Ads anchored adaptive banner specifications.
 */
object AdaptiveBannerCalculator {
    /**
     * Determines the expected anchored adaptive banner height in dp.
     * Google AdMob anchored adaptive banner rule:
     * - width <= 0: 0 dp (graceful collapse)
     * - width <= 400 dp: 50 dp
     * - 400 < width <= 720 dp: 90 dp
     * - width > 720 dp: 90 dp
     */
    fun calculateAdaptiveBannerHeightDp(widthDp: Int): Int {
        return when {
            widthDp <= 0 -> 0
            widthDp <= 400 -> 50
            else -> 90
        }
    }

    fun isValidWidth(widthDp: Int): Boolean = widthDp > 0
}
