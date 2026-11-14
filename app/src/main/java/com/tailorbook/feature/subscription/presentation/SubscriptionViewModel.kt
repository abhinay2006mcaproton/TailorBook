package com.tailorbook.feature.subscription.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import com.tailorbook.core.billing.BillingConstants
import com.tailorbook.core.billing.PlayBillingManager
import com.tailorbook.core.billing.SubscriptionProduct
import com.tailorbook.core.billing.VipAccessManager
import com.tailorbook.core.datastore.SubscriptionPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class BillingCycle(
    val title: String,
    val defaultPrice: String,
    val priceNumber: Int,
    val period: String,
    val savingsBadge: String? = null
) {
    MONTHLY("Monthly", "₹149", 149, "/month", null),
    YEARLY("Yearly", "₹1,199", 1199, "/year", "Save 33% (2 Months Free)")
}

data class SubscriptionUiState(
    val isPremium: Boolean = false,
    val activePlan: String = "FREE",
    val expiryDate: String = "",
    val selectedCycle: BillingCycle = BillingCycle.YEARLY,
    val monthlyDisplayPrice: String = BillingConstants.FALLBACK_MONTHLY_PRICE,
    val annualDisplayPrice: String = BillingConstants.FALLBACK_ANNUAL_PRICE,
    val isBillingConnected: Boolean = false,
    val isLoading: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val successMessage: String = "",
    val errorMessage: String? = null
)

/**
 * ViewModel managing subscription status, tier entitlements, Google Play Billing prices, and purchase flows.
 * Meets REQ-SUB-01 through REQ-SUB-04.
 */
class SubscriptionViewModel(
    private val preferencesManager: SubscriptionPreferences,
    private val billingManager: PlayBillingManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        // Observe DataStore subscription status
        viewModelScope.launch {
            combine(
                preferencesManager.isPremiumFlow,
                preferencesManager.subscriptionPlanFlow,
                preferencesManager.subscriptionExpiryFlow
            ) { isPrem, plan, expiry ->
                Triple(isPrem, plan, expiry)
            }.collect { (isPrem, plan, expiry) ->
                _uiState.update { current ->
                    current.copy(
                        isPremium = isPrem,
                        activePlan = plan,
                        expiryDate = expiry
                    )
                }
            }
        }

        // Connect and observe live Google Play localized subscription pricing & purchases
        if (billingManager != null) {
            billingManager.startConnection()

            viewModelScope.launch {
                billingManager.subscriptionProducts.collect { products ->
                    val monthly = products.find { it.productId == BillingConstants.PRODUCT_PREMIUM_MONTHLY }
                    val annual = products.find { it.productId == BillingConstants.PRODUCT_PREMIUM_ANNUAL }

                    _uiState.update { current ->
                        current.copy(
                            isBillingConnected = billingManager.isBillingReady,
                            monthlyDisplayPrice = monthly?.formattedPrice ?: BillingConstants.FALLBACK_MONTHLY_PRICE,
                            annualDisplayPrice = annual?.formattedPrice ?: BillingConstants.FALLBACK_ANNUAL_PRICE
                        )
                    }
                }
            }

            viewModelScope.launch {
                billingManager.purchasesFlow.collect { purchases ->
                    handlePurchasesUpdate(purchases)
                }
            }
        }
    }

    private suspend fun handlePurchasesUpdate(purchases: List<Purchase>) {
        val activePurchase = purchases.find { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (activePurchase != null) {
            val isAnnual = activePurchase.products.contains(BillingConstants.PRODUCT_PREMIUM_ANNUAL)
            val planName = if (isAnnual) "YEARLY" else "MONTHLY"

            val cal = Calendar.getInstance()
            cal.timeInMillis = activePurchase.purchaseTime
            if (isAnnual) cal.add(Calendar.YEAR, 1) else cal.add(Calendar.MONTH, 1)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val expiryString = dateFormat.format(cal.time)

            preferencesManager.setSubscription(
                isPremium = true,
                plan = planName,
                expiry = expiryString
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    showSuccessDialog = true,
                    successMessage = "Google Play purchase successful! TailorBook Pro Boutique is active until $expiryString."
                )
            }
        }
    }

    fun selectBillingCycle(cycle: BillingCycle) {
        _uiState.update { it.copy(selectedCycle = cycle) }
    }

    /**
     * Initiates Google Play subscription purchase flow using official Play Billing library.
     */
    fun purchaseSubscription(activity: Activity, cycle: BillingCycle = _uiState.value.selectedCycle) {
        val targetProductId = if (cycle == BillingCycle.MONTHLY) {
            BillingConstants.PRODUCT_PREMIUM_MONTHLY
        } else {
            BillingConstants.PRODUCT_PREMIUM_ANNUAL
        }

        val product = billingManager?.subscriptionProducts?.value?.find { it.productId == targetProductId }

        if (billingManager != null && product != null && billingManager.isBillingReady) {
            Timber.i("Launching Google Play Billing Flow for product: ${product.productId}")
            val result = billingManager.launchBillingFlow(activity, product)
            Timber.d("Billing flow result: code=${result.responseCode}, msg=${result.debugMessage}")
        } else if (com.tailorbook.BuildConfig.DEBUG) {
            Timber.w("Billing client unavailable or product details not loaded yet. Running dev upgrade simulation (DEBUG build only).")
            upgradeToPro(cycle)
        } else {
            Timber.e("Google Play Billing unavailable in production release.")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Google Play payment service is currently unavailable. Please ensure your device is connected to the internet and signed into Google Play."
                )
            }
        }
    }

    fun upgradeToPro(cycle: BillingCycle = _uiState.value.selectedCycle) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val cal = Calendar.getInstance()
            if (cycle == BillingCycle.MONTHLY) {
                cal.add(Calendar.MONTH, 1)
            } else {
                cal.add(Calendar.YEAR, 1)
            }
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val expiryString = dateFormat.format(cal.time)

            preferencesManager.setSubscription(
                isPremium = true,
                plan = cycle.name,
                expiry = expiryString
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    showSuccessDialog = true,
                    successMessage = "Welcome to TailorBook Pro Boutique! Your subscription is active until $expiryString."
                )
            }
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            preferencesManager.cancelSubscription()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    showSuccessDialog = false,
                    isPremium = false,
                    activePlan = "FREE",
                    expiryDate = ""
                )
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (billingManager != null && billingManager.isBillingReady) {
                val purchases = billingManager.queryActivePurchases()
                val purchased = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

                if (purchased.isNotEmpty()) {
                    handlePurchasesUpdate(purchased)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showSuccessDialog = true,
                            successMessage = "Google Play purchases restored successfully!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No active Google Play subscription found for this account."
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Play Store is unavailable. Please check your internet connection."
                    )
                }
            }
        }
    }

    /**
     * Allows designated users to unlock the entire app for free using a VIP access code.
     */
    fun redeemVipCode(code: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val isSuccess = VipAccessManager.redeemVipCode(code, preferencesManager)
            if (isSuccess) {
                _uiState.update {
                    it.copy(
                        showSuccessDialog = true,
                        successMessage = "VIP Complimentary Access Activated! You have lifetime unlimited access to all TailorBook features."
                    )
                }
            }
            onResult(isSuccess)
        }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
