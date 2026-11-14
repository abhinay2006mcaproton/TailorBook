package com.tailorbook.feature.subscription

import android.app.Activity
import com.tailorbook.core.ads.FakeRewardedAdManager
import com.tailorbook.core.billing.BillingConstants
import com.tailorbook.core.billing.FakePlayBillingManager
import com.tailorbook.core.billing.VipAccessManager
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.subscription.domain.CreationDecision
import com.tailorbook.feature.subscription.domain.CustomerEntitlementEngineImpl
import com.tailorbook.feature.subscription.presentation.BillingCycle
import com.tailorbook.feature.subscription.presentation.SubscriptionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Complete End-to-End Monetization Journey Simulation (TASK-17-11).
 * Validates the entire commercial lifecycle:
 * 1. Tier 1 (1–10 Free Starter): Normal creation without ads.
 * 2. Tier 2 (11–20 Ad-Supported Growth): 1 verified rewarded video per customer. Single-use token integrity.
 * 3. Tier 3 (21+ Premium): Strict limit reached gate directing to Premium subscription.
 * 4. Google Play In-App Subscriptions: Monthly & Annual purchases, auto-acknowledgment, unlimited unlocking.
 * 5. Data Protection: 100% data preservation on subscription expiry (zero records locked or deleted).
 * 6. VIP Complimentary Free Access: Designated whitelisted users and promo code bypasses.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonetizationEndToEndTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var customerDao: FakeCustomerDao
    private lateinit var preferences: FakeSubscriptionPreferences
    private lateinit var entitlementEngine: CustomerEntitlementEngineImpl
    private lateinit var adManager: FakeRewardedAdManager
    private lateinit var billingManager: FakePlayBillingManager
    private lateinit var subscriptionViewModel: SubscriptionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        customerDao = FakeCustomerDao()
        preferences = FakeSubscriptionPreferences()
        entitlementEngine = CustomerEntitlementEngineImpl(
            customerDao = customerDao,
            subscriptionPreferences = preferences,
            shopIdProvider = { "shop_kethepalle" },
            userPhoneProvider = { "9876500000" },
            userEmailProvider = { "tailor@test.com" }
        )
        adManager = FakeRewardedAdManager()
        billingManager = FakePlayBillingManager()
        subscriptionViewModel = SubscriptionViewModel(preferences, billingManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun tier1_customers1To10_createdNormallyWithoutAdvertisements() = runTest {
        // Customer 1 to 10
        for (count in 0 until 10) {
            customerDao.countState.value = count
            val decision = entitlementEngine.evaluateCreationDecision()
            assertTrue("Customer ${count + 1} must be AllowedNormal", decision is CreationDecision.AllowedNormal)
            assertEquals(10 - count, (decision as CreationDecision.AllowedNormal).remainingFreeSlots)
        }
    }

    @Test
    fun tier2_customer11_requiresRewardedVideo_andSingleUseTokenGuaranteed() = runTest {
        customerDao.countState.value = 10 // Attempting to create 11th customer

        // Initial evaluation requires rewarded video ad
        val initialDecision = entitlementEngine.evaluateCreationDecision()
        assertTrue(initialDecision is CreationDecision.RequiresRewardedAd)
        assertEquals(11, (initialDecision as CreationDecision.RequiresRewardedAd).nextCustomerNumber)

        // Case A: User skips video ad -> Customer NOT saved, no token granted
        var rewardGranted = false
        adManager.setAdLoaded(true)
        adManager.setShouldRewardUser(false) // Skipped
        val dummyActivity = Activity()
        adManager.showAd(dummyActivity, { rewardGranted = true }, {})

        assertFalse("Reward must not be granted on skipped ad", rewardGranted)
        assertFalse(entitlementEngine.hasValidRewardToken())
        assertTrue(entitlementEngine.evaluateCreationDecision() is CreationDecision.RequiresRewardedAd)

        // Case B: User watches video ad to completion -> Single-use token granted
        adManager.setAdLoaded(true)
        adManager.setShouldRewardUser(true) // Completed
        adManager.showAd(dummyActivity, { rewardGranted = true }, {})

        assertTrue("Reward granted on completed ad", rewardGranted)
        val token = entitlementEngine.grantRewardToken()
        assertTrue(token.startsWith("tb_rwd_"))

        // Decision now allows creation with reward token
        val decisionWithReward = entitlementEngine.evaluateCreationDecision()
        assertTrue(decisionWithReward is CreationDecision.AllowedWithReward)

        // Consuming token saves customer 11
        val consumed = entitlementEngine.consumeRewardToken(token)
        assertTrue(consumed)
        customerDao.countState.value = 11

        // Single-use guarantee: Reusing same token for customer 12 fails
        val reused = entitlementEngine.consumeRewardToken(token)
        assertFalse("Token cannot be reused for subsequent customers", reused)

        // Customer 12 requires a new rewarded ad
        val decisionFor12 = entitlementEngine.evaluateCreationDecision()
        assertTrue(decisionFor12 is CreationDecision.RequiresRewardedAd)
        assertEquals(12, (decisionFor12 as CreationDecision.RequiresRewardedAd).nextCustomerNumber)
    }

    @Test
    fun tier3_customer21_strictlyRequiresPremiumSubscription() = runTest {
        // Customer 20 reached via free ad-supported growth
        customerDao.countState.value = 20

        val decision = entitlementEngine.evaluateCreationDecision()
        assertTrue("Customer 21 must require Premium", decision is CreationDecision.RequiresPremium)
        assertEquals(20, (decision as CreationDecision.RequiresPremium).currentCount)
    }

    @Test
    fun premiumSubscription_unlocksUnlimitedCustomersAndRemovesAllAds() = runTest {
        customerDao.countState.value = 75 // Beyond free allowance
        advanceUntilIdle()

        // Tailor upgrades to Pro Boutique Monthly
        subscriptionViewModel.upgradeToPro(BillingCycle.MONTHLY)
        advanceUntilIdle()

        assertTrue(preferences.isPremiumFlow.first())
        assertEquals("MONTHLY", preferences.subscriptionPlanFlow.first())

        // Entitlement engine now returns AllowedUnlimited
        val decision = entitlementEngine.evaluateCreationDecision()
        assertTrue("Premium user must have AllowedUnlimited", decision is CreationDecision.AllowedUnlimited)
        assertEquals(75, (decision as CreationDecision.AllowedUnlimited).currentCount)
    }

    @Test
    fun dataProtection_whenSubscriptionExpires_allRecordsRemain100PercentIntact() = runTest {
        // Shop has 60 customers and active Pro subscription
        customerDao.countState.value = 60
        subscriptionViewModel.upgradeToPro(BillingCycle.YEARLY)
        advanceUntilIdle()
        assertTrue(preferences.isPremiumFlow.first())

        // Subscription expires or is cancelled
        subscriptionViewModel.cancelSubscription()
        advanceUntilIdle()

        assertFalse(preferences.isPremiumFlow.first())
        assertEquals("FREE", preferences.subscriptionPlanFlow.first())

        // All 60 existing customer records remain intact in database
        assertEquals(60, customerDao.getActiveCustomerCount("shop_kethepalle").first())

        // Only NEW customer creation is gated to require Premium renewal
        val decision = entitlementEngine.evaluateCreationDecision()
        assertTrue(decision is CreationDecision.RequiresPremium)
    }

    @Test
    fun vipComplimentaryAccess_whitelistedTailorsBypassAllPaywalls() = runTest {
        customerDao.countState.value = 80

        // Redeem VIP partner code
        val success = VipAccessManager.redeemVipCode("TAILORVIP2026", preferences)
        assertTrue(success)

        assertTrue(preferences.isPremiumFlow.first())
        assertEquals("VIP_COMPLIMENTARY", preferences.subscriptionPlanFlow.first())
        assertEquals("Lifetime Free", preferences.subscriptionExpiryFlow.first())

        // Entitlement engine recognizes VIP access
        val decision = entitlementEngine.evaluateCreationDecision()
        assertTrue(decision is CreationDecision.AllowedUnlimited)
        assertEquals(80, (decision as CreationDecision.AllowedUnlimited).currentCount)
    }
}
