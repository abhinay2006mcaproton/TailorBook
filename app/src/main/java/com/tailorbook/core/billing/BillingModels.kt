package com.tailorbook.core.billing

import com.android.billingclient.api.ProductDetails

/**
 * Standard product identifiers for TailorBook Google Play In-App Subscriptions.
 */
object BillingConstants {
    const val PRODUCT_PREMIUM_MONTHLY = "tailorbook_premium_monthly"
    const val PRODUCT_PREMIUM_ANNUAL = "tailorbook_premium_annual"

    // Default fallback pricing representations for offline / initial rendering
    const val FALLBACK_MONTHLY_PRICE = "₹149"
    const val FALLBACK_ANNUAL_PRICE = "₹1,199"

    val ALL_SUBSCRIPTION_IDS = listOf(
        PRODUCT_PREMIUM_MONTHLY,
        PRODUCT_PREMIUM_ANNUAL
    )
}

/**
 * Connection lifecycle states for Google Play Billing.
 */
enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED,
    CLOSED
}

/**
 * Clean data model representing a localized subscription product returned by Google Play.
 */
data class SubscriptionProduct(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val priceCurrencyCode: String,
    val priceAmountMicros: Long,
    val billingPeriod: String,
    val offerToken: String,
    val productDetails: ProductDetails? = null
)
