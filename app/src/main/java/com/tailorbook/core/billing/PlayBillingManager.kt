package com.tailorbook.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.math.min

/**
 * Interface contract for managing Google Play Billing connection, queries, and purchases.
 */
interface PlayBillingManager {
    val connectionState: StateFlow<BillingConnectionState>
    val subscriptionProducts: StateFlow<List<SubscriptionProduct>>
    val purchasesFlow: SharedFlow<List<Purchase>>
    val isBillingReady: Boolean

    fun startConnection()
    fun endConnection()
    fun querySubscriptionProducts()
    suspend fun queryActivePurchases(): List<Purchase>
    suspend fun acknowledgePurchase(purchaseToken: String): Boolean
    fun launchBillingFlow(
        activity: Activity,
        product: SubscriptionProduct,
        obfuscatedAccountId: String = ""
    ): BillingResult
}

/**
 * Production implementation of [PlayBillingManager] using official Google Play Billing Library 6.x.
 * Implements exponential backoff on disconnect, automatic product details querying,
 * and purchase updates routing.
 */
class PlayBillingManagerImpl(
    private val context: Context,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : PlayBillingManager, PurchasesUpdatedListener, BillingClientStateListener {

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _subscriptionProducts = MutableStateFlow<List<SubscriptionProduct>>(emptyList())
    override val subscriptionProducts: StateFlow<List<SubscriptionProduct>> = _subscriptionProducts.asStateFlow()

    private val _purchasesFlow = MutableSharedFlow<List<Purchase>>(extraBufferCapacity = 10)
    override val purchasesFlow: SharedFlow<List<Purchase>> = _purchasesFlow.asSharedFlow()

    override val isBillingReady: Boolean
        get() = billingClient?.isReady == true

    private var billingClient: BillingClient? = null
    private var reconnectDelayMs = 1000L
    private val maxReconnectDelayMs = 32000L

    init {
        buildBillingClient()
    }

    private fun buildBillingClient() {
        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases()
            .build()
    }

    override fun startConnection() {
        if (billingClient?.isReady == true) {
            _connectionState.value = BillingConnectionState.CONNECTED
            return
        }

        _connectionState.value = BillingConnectionState.CONNECTING
        try {
            billingClient?.startConnection(this)
        } catch (e: Exception) {
            Timber.e(e, "Error starting billing client connection")
            _connectionState.value = BillingConnectionState.FAILED
            scheduleReconnect()
        }
    }

    override fun endConnection() {
        _connectionState.value = BillingConnectionState.CLOSED
        billingClient?.endConnection()
        billingClient = null
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Timber.i("Google Play Billing client connected successfully.")
            _connectionState.value = BillingConnectionState.CONNECTED
            reconnectDelayMs = 1000L // Reset backoff
            querySubscriptionProducts()
        } else {
            Timber.w("Billing setup failed: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
            _connectionState.value = BillingConnectionState.FAILED
            scheduleReconnect()
        }
    }

    override fun onBillingServiceDisconnected() {
        Timber.w("Billing service disconnected. Scheduling reconnect...")
        _connectionState.value = BillingConnectionState.DISCONNECTED
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        externalScope.launch {
            delay(reconnectDelayMs)
            reconnectDelayMs = min(reconnectDelayMs * 2, maxReconnectDelayMs)
            Timber.d("Retrying billing connection with backoff delay: ${reconnectDelayMs}ms")
            if (billingClient == null) {
                buildBillingClient()
            }
            startConnection()
        }
    }

    override fun querySubscriptionProducts() {
        val client = billingClient
        if (client == null || !client.isReady) {
            Timber.w("Cannot query products: BillingClient is not ready.")
            return
        }

        val productList = BillingConstants.ALL_SUBSCRIPTION_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val mappedProducts = productDetailsList.mapNotNull { details ->
                    mapProductDetails(details)
                }
                Timber.i("Fetched ${mappedProducts.size} subscription products from Google Play.")
                _subscriptionProducts.value = mappedProducts
            } else {
                Timber.e("Failed to query product details: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
            }
        }
    }

    private fun mapProductDetails(details: ProductDetails): SubscriptionProduct? {
        val offerDetails = details.subscriptionOfferDetails?.firstOrNull() ?: return null
        val pricingPhase = offerDetails.pricingPhases.pricingPhaseList.firstOrNull() ?: return null

        return SubscriptionProduct(
            productId = details.productId,
            title = details.title,
            description = details.description,
            formattedPrice = pricingPhase.formattedPrice,
            priceCurrencyCode = pricingPhase.priceCurrencyCode,
            priceAmountMicros = pricingPhase.priceAmountMicros,
            billingPeriod = pricingPhase.billingPeriod,
            offerToken = offerDetails.offerToken,
            productDetails = details
        )
    }

    override fun launchBillingFlow(
        activity: Activity,
        product: SubscriptionProduct,
        obfuscatedAccountId: String
    ): BillingResult {
        val client = billingClient
        val details = product.productDetails
        if (client == null || !client.isReady || details == null) {
            Timber.e("Cannot launch billing flow: Client not ready or product details missing.")
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                .setDebugMessage("BillingClient is not ready.")
                .build()
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(product.offerToken)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .apply {
                if (obfuscatedAccountId.isNotBlank()) {
                    setObfuscatedAccountId(obfuscatedAccountId)
                }
            }
            .build()

        return client.launchBillingFlow(activity, flowParams)
    }

    override suspend fun queryActivePurchases(): List<Purchase> = suspendCancellableCoroutine { continuation ->
        val client = billingClient
        if (client == null || !client.isReady) {
            Timber.w("Cannot query active purchases: Client is not ready.")
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.i("Query active purchases returned ${purchasesList.size} subscriptions.")
                continuation.resume(purchasesList)
            } else {
                Timber.e("Query active purchases failed: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                continuation.resume(emptyList())
            }
        }
    }

    override suspend fun acknowledgePurchase(purchaseToken: String): Boolean = suspendCancellableCoroutine { continuation ->
        val client = billingClient
        if (client == null || !client.isReady) {
            Timber.w("Cannot acknowledge purchase: Client is not ready.")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        client.acknowledgePurchase(params) { billingResult ->
            val isSuccess = billingResult.responseCode == BillingClient.BillingResponseCode.OK
            if (isSuccess) {
                Timber.i("Google Play purchase acknowledged successfully: $purchaseToken")
            } else {
                Timber.e("Failed to acknowledge purchase: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
            }
            continuation.resume(isSuccess)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Timber.i("Purchases updated received with ${purchases.size} purchases.")
            externalScope.launch {
                // Auto-acknowledge completed purchases to protect against 3-day revocation rule
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase.purchaseToken)
                    }
                }
                _purchasesFlow.emit(purchases)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Timber.d("User cancelled Google Play subscription flow.")
        } else {
            Timber.w("Purchases updated with error: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
        }
    }
}
