package com.tailorbook.feature.subscription.domain

import com.tailorbook.core.billing.VipAccessManager
import com.tailorbook.core.datastore.SubscriptionPreferences
import com.tailorbook.feature.customers.data.local.dao.CustomerDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Commercial tier limits approved by the product owner (Option A):
 * - Free Starter: 1–10 customers normal creation without ads.
 * - Free Ad-Supported Growth: 11–20 customers, 1 verified rewarded video per customer.
 * - TailorBook Premium: 21+ customers requires active subscription; unlimited customers, ad-free.
 */
object EntitlementConstants {
    const val FREE_TIER_LIMIT = 10
    const val REWARDED_AD_TIER_LIMIT = 20
    const val FREE_ACTIVE_ORDERS_LIMIT = 15
    const val FREE_STAFF_LIMIT = 1
    const val FREE_PHOTOS_PER_ORDER_LIMIT = 1
}

/**
 * Decision returned when checking if a customer can be created.
 */
sealed interface CreationDecision {
    /** Under free limit (1–10). Normal creation allowed. */
    data class AllowedNormal(
        val currentCount: Int,
        val remainingFreeSlots: Int
    ) : CreationDecision

    /** Between 11–20 and no verified reward token has been granted yet. Rewarded video required. */
    data class RequiresRewardedAd(
        val currentCount: Int,
        val nextCustomerNumber: Int
    ) : CreationDecision

    /** Between 11–20 and user has watched a rewarded video; single-use token ready to be consumed on save. */
    data class AllowedWithReward(
        val currentCount: Int,
        val rewardToken: String
    ) : CreationDecision

    /** Reached or exceeded 20 customers. Requires an active TailorBook Premium subscription. */
    data class RequiresPremium(
        val currentCount: Int
    ) : CreationDecision

    /** User has an active TailorBook Premium subscription. Unlimited customers allowed. */
    data class AllowedUnlimited(
        val currentCount: Int
    ) : CreationDecision
}

/**
 * Comprehensive entitlement state summary.
 */
data class EntitlementSummary(
    val activeCustomerCount: Int,
    val isPremium: Boolean,
    val tierDisplayName: String,
    val hasPendingRewardToken: Boolean,
    val decision: CreationDecision
)

/**
 * Centralized Entitlement Engine contract (Section 3).
 * Single source of truth for customer allowance, reward verification, and tier enforcement.
 */
interface CustomerEntitlementEngine {
    val entitlementSummaryFlow: Flow<EntitlementSummary>

    /** Evaluates current permission to create a customer. */
    suspend fun evaluateCreationDecision(): CreationDecision

    /** Grants a cryptographically secure single-use token upon rewarded ad completion. */
    suspend fun grantRewardToken(): String

    /** Consumes the token upon saving customer. Returns true if valid and consumed, false if invalid/already consumed. */
    suspend fun consumeRewardToken(token: String): Boolean

    /** Checks whether an unconsumed reward token is available. */
    fun hasValidRewardToken(): Boolean

    /** Clears any pending reward token (e.g. if creation was cancelled). */
    fun clearRewardToken()
}

class CustomerEntitlementEngineImpl(
    private val customerDao: CustomerDao,
    private val subscriptionPreferences: SubscriptionPreferences,
    private val shopIdProvider: () -> String = { "shop-001" },
    private val userPhoneProvider: () -> String? = { null },
    private val userEmailProvider: () -> String? = { null },
    private val deviceManager: com.tailorbook.core.device.DeviceManager? = null
) : CustomerEntitlementEngine {

    private val mutex = Mutex()
    private var pendingRewardToken: String? = null
    private val consumedTokens = mutableSetOf<String>()

    override val entitlementSummaryFlow: Flow<EntitlementSummary> = combine(
        customerDao.getActiveCustomerCount(shopIdProvider()),
        subscriptionPreferences.isPremiumFlow
    ) { count, isPremium ->
        val email = userEmailProvider()
        val isDeviceStarterConsumed = deviceManager?.isStarterQuotaConsumedByAnotherAccount(email) ?: false
        val decision = computeDecision(count, isPremium, pendingRewardToken, isDeviceStarterConsumed)
        val effectiveFreeLimit = if (isDeviceStarterConsumed) 0 else EntitlementConstants.FREE_TIER_LIMIT
        val tierName = when {
            isPremium -> "TailorBook Premium"
            count < effectiveFreeLimit -> "Free Starter"
            count < EntitlementConstants.REWARDED_AD_TIER_LIMIT -> "Ad-Supported Growth"
            else -> "Limit Reached"
        }
        EntitlementSummary(
            activeCustomerCount = count,
            isPremium = isPremium,
            tierDisplayName = tierName,
            hasPendingRewardToken = !pendingRewardToken.isNullOrBlank(),
            decision = decision
        )
    }.distinctUntilChanged()

    override suspend fun evaluateCreationDecision(): CreationDecision {
        val phone = userPhoneProvider()
        val email = userEmailProvider()
        VipAccessManager.applyVipStatusIfEligible(phone, email, subscriptionPreferences)

        val count = customerDao.getActiveCustomerCount(shopIdProvider()).first()
        val isPremium = subscriptionPreferences.isPremiumFlow.first()
        val isDeviceStarterConsumed = deviceManager?.isStarterQuotaConsumedByAnotherAccount(email) ?: false

        if (count > 0) {
            deviceManager?.markStarterQuotaUsed(email)
        }

        return mutex.withLock {
            computeDecision(count, isPremium, pendingRewardToken, isDeviceStarterConsumed)
        }
    }

    override suspend fun grantRewardToken(): String = mutex.withLock {
        val token = "tb_rwd_${UUID.randomUUID()}_${System.currentTimeMillis()}"
        pendingRewardToken = token
        token
    }

    override suspend fun consumeRewardToken(token: String): Boolean = mutex.withLock {
        if (token.isBlank()) return false
        if (consumedTokens.contains(token)) return false
        if (pendingRewardToken == token) {
            consumedTokens.add(token)
            pendingRewardToken = null
            deviceManager?.markStarterQuotaUsed(userEmailProvider())
            return true
        }
        return false
    }

    override fun hasValidRewardToken(): Boolean {
        return !pendingRewardToken.isNullOrBlank()
    }

    override fun clearRewardToken() {
        pendingRewardToken = null
    }

    private fun computeDecision(
        count: Int,
        isPremium: Boolean,
        rewardToken: String?,
        isDeviceStarterConsumed: Boolean = false
    ): CreationDecision {
        if (isPremium) {
            return CreationDecision.AllowedUnlimited(currentCount = count)
        }

        // If this physical device already consumed its free starter quota under a different account,
        // subsequent accounts start at 0 free slots and must watch rewarded ads from customer #1.
        val effectiveFreeLimit = if (isDeviceStarterConsumed) 0 else EntitlementConstants.FREE_TIER_LIMIT

        if (count < effectiveFreeLimit) {
            return CreationDecision.AllowedNormal(
                currentCount = count,
                remainingFreeSlots = effectiveFreeLimit - count
            )
        }

        if (count < EntitlementConstants.REWARDED_AD_TIER_LIMIT) {
            return if (!rewardToken.isNullOrBlank()) {
                CreationDecision.AllowedWithReward(
                    currentCount = count,
                    rewardToken = rewardToken
                )
            } else {
                CreationDecision.RequiresRewardedAd(
                    currentCount = count,
                    nextCustomerNumber = count + 1
                )
            }
        }

        return CreationDecision.RequiresPremium(currentCount = count)
    }
}
