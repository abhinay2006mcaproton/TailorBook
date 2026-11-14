package com.tailorbook.core.billing

import com.tailorbook.feature.subscription.FakeSubscriptionPreferences
import com.tailorbook.feature.subscription.presentation.SubscriptionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
 * Unit tests verifying VIP Complimentary Free Access and Purchase Restoration.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VipAndPurchaseStateMachineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferences: FakeSubscriptionPreferences
    private lateinit var billingManager: FakePlayBillingManager
    private lateinit var viewModel: SubscriptionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferences = FakeSubscriptionPreferences()
        billingManager = FakePlayBillingManager()
        viewModel = SubscriptionViewModel(preferences, billingManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whitelistedPhoneAndEmail_identifiedAsVip() {
        // Whitelisted phone with and without prefix
        assertTrue(VipAccessManager.isWhitelisted(phone = "9876543210"))
        assertTrue(VipAccessManager.isWhitelisted(phone = "+91 9876543210"))

        // Whitelisted email
        assertTrue(VipAccessManager.isWhitelisted(email = "owner@tailorbook.com"))
        assertTrue(VipAccessManager.isWhitelisted(email = "vip@tailorbook.com"))

        // Non-whitelisted
        assertFalse(VipAccessManager.isWhitelisted(phone = "9111111111", email = "random@gmail.com"))
    }

    @Test
    fun dynamicallyAddedVip_grantsVipStatus() {
        val testPhone = "9000000001"
        assertFalse(VipAccessManager.isWhitelisted(phone = testPhone))

        VipAccessManager.addVipPhone(testPhone)
        assertTrue(VipAccessManager.isWhitelisted(phone = testPhone))
    }

    @Test
    fun validVipPromoCode_activatesLifetimeFreeAccess() = runTest {
        assertFalse(preferences.isPremiumFlow.first())

        val success = VipAccessManager.redeemVipCode("TAILORVIP2026", preferences)
        assertTrue(success)

        assertTrue(preferences.isPremiumFlow.first())
        assertEquals("VIP_COMPLIMENTARY", preferences.subscriptionPlanFlow.first())
        assertEquals("Lifetime Free", preferences.subscriptionExpiryFlow.first())
    }

    @Test
    fun invalidVipPromoCode_rejected() = runTest {
        val success = VipAccessManager.redeemVipCode("WRONG_CODE_123", preferences)
        assertFalse(success)
        assertFalse(preferences.isPremiumFlow.first())
    }

    @Test
    fun applyVipStatusIfEligible_automaticallyUpgradesWhitelistedUser() = runTest {
        assertFalse(preferences.isPremiumFlow.first())

        VipAccessManager.applyVipStatusIfEligible("9876543210", null, preferences)

        assertTrue(preferences.isPremiumFlow.first())
        assertEquals("VIP_COMPLIMENTARY", preferences.subscriptionPlanFlow.first())
    }
}
