package com.tailorbook.core.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fake implementation of PlayBillingManager for robust unit and ViewModel testing.
 */
class FakePlayBillingManager : PlayBillingManager {

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _subscriptionProducts = MutableStateFlow<List<SubscriptionProduct>>(emptyList())
    override val subscriptionProducts: StateFlow<List<SubscriptionProduct>> = _subscriptionProducts.asStateFlow()

    private val _purchasesFlow = MutableSharedFlow<List<Purchase>>(extraBufferCapacity = 10)
    override val purchasesFlow: SharedFlow<List<Purchase>> = _purchasesFlow.asSharedFlow()

    override val isBillingReady: Boolean
        get() = _connectionState.value == BillingConnectionState.CONNECTED

    override fun startConnection() {
        _connectionState.value = BillingConnectionState.CONNECTED
        querySubscriptionProducts()
    }

    override fun endConnection() {
        _connectionState.value = BillingConnectionState.CLOSED
    }

    override fun querySubscriptionProducts() {
        _subscriptionProducts.value = listOf(
            SubscriptionProduct(
                productId = BillingConstants.PRODUCT_PREMIUM_MONTHLY,
                title = "TailorBook Pro Monthly",
                description = "Unlimited customers and ad-free experience",
                formattedPrice = "₹149.00",
                priceCurrencyCode = "INR",
                priceAmountMicros = 149000000L,
                billingPeriod = "P1M",
                offerToken = "token_monthly_mock"
            ),
            SubscriptionProduct(
                productId = BillingConstants.PRODUCT_PREMIUM_ANNUAL,
                title = "TailorBook Pro Annual",
                description = "Unlimited customers and ad-free experience (Save ₹589)",
                formattedPrice = "₹1,199.00",
                priceCurrencyCode = "INR",
                priceAmountMicros = 1199000000L,
                billingPeriod = "P1Y",
                offerToken = "token_annual_mock"
            )
        )
    }

    override fun launchBillingFlow(
        activity: Activity,
        product: SubscriptionProduct,
        obfuscatedAccountId: String
    ): BillingResult {
        return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build()
    }

    override suspend fun queryActivePurchases(): List<Purchase> = emptyList()

    override suspend fun acknowledgePurchase(purchaseToken: String): Boolean = true

    fun simulateConnectionFailure() {
        _connectionState.value = BillingConnectionState.FAILED
    }

    fun emitPurchases(purchases: List<Purchase>) {
        _purchasesFlow.tryEmit(purchases)
    }
}

/**
 * Unit tests verifying Google Play Billing connection lifecycle and product state machine.
 */
class PlayBillingManagerTest {

    private lateinit var billingManager: FakePlayBillingManager

    @Before
    fun setUp() {
        billingManager = FakePlayBillingManager()
    }

    @Test
    fun initialState_isDisconnected() {
        assertEquals(BillingConnectionState.DISCONNECTED, billingManager.connectionState.value)
        assertFalse(billingManager.isBillingReady)
        assertTrue(billingManager.subscriptionProducts.value.isEmpty())
    }

    @Test
    fun startConnection_transitionsToConnected_andLoadsProducts() = runTest {
        billingManager.startConnection()

        assertEquals(BillingConnectionState.CONNECTED, billingManager.connectionState.value)
        assertTrue(billingManager.isBillingReady)

        val products = billingManager.subscriptionProducts.value
        assertEquals(2, products.size)

        val monthly = products.find { it.productId == BillingConstants.PRODUCT_PREMIUM_MONTHLY }
        val annual = products.find { it.productId == BillingConstants.PRODUCT_PREMIUM_ANNUAL }

        assertTrue(monthly != null)
        assertEquals("₹149.00", monthly?.formattedPrice)
        assertEquals("P1M", monthly?.billingPeriod)

        assertTrue(annual != null)
        assertEquals("₹1,199.00", annual?.formattedPrice)
        assertEquals("P1Y", annual?.billingPeriod)
    }

    @Test
    fun endConnection_transitionsToClosed() {
        billingManager.startConnection()
        assertTrue(billingManager.isBillingReady)

        billingManager.endConnection()
        assertEquals(BillingConnectionState.CLOSED, billingManager.connectionState.value)
        assertFalse(billingManager.isBillingReady)
    }

    @Test
    fun constants_andProductIds_matchGooglePlayConsoleRequirements() {
        assertEquals("tailorbook_premium_monthly", BillingConstants.PRODUCT_PREMIUM_MONTHLY)
        assertEquals("tailorbook_premium_annual", BillingConstants.PRODUCT_PREMIUM_ANNUAL)
        assertEquals(2, BillingConstants.ALL_SUBSCRIPTION_IDS.size)
        assertEquals("₹149", BillingConstants.FALLBACK_MONTHLY_PRICE)
        assertEquals("₹1,199", BillingConstants.FALLBACK_ANNUAL_PRICE)
    }
}
