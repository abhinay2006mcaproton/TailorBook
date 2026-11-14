package com.tailorbook

import com.tailorbook.core.datastore.SubscriptionPreferences
import com.tailorbook.feature.subscription.presentation.BillingCycle
import com.tailorbook.feature.subscription.presentation.SubscriptionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSubscriptionPreferences : SubscriptionPreferences {
        private val _isPremium = MutableStateFlow(false)
        override val isPremiumFlow: Flow<Boolean> = _isPremium.asStateFlow()

        private val _plan = MutableStateFlow("FREE")
        override val subscriptionPlanFlow: Flow<String> = _plan.asStateFlow()

        private val _expiry = MutableStateFlow("")
        override val subscriptionExpiryFlow: Flow<String> = _expiry.asStateFlow()

        override suspend fun setSubscription(isPremium: Boolean, plan: String, expiry: String) {
            _isPremium.value = isPremium
            _plan.value = plan
            _expiry.value = expiry
        }

        override suspend fun cancelSubscription() {
            _isPremium.value = false
            _plan.value = "FREE"
            _expiry.value = ""
        }
    }

    private lateinit var fakePreferences: FakeSubscriptionPreferences
    private lateinit var viewModel: SubscriptionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePreferences = FakeSubscriptionPreferences()
        viewModel = SubscriptionViewModel(fakePreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state defaults to free tier with yearly selected`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isPremium)
        assertEquals("FREE", state.activePlan)
        assertEquals("", state.expiryDate)
        assertEquals(BillingCycle.YEARLY, state.selectedCycle)
    }

    @Test
    fun `selecting billing cycle updates state`() = runTest {
        advanceUntilIdle()
        viewModel.selectBillingCycle(BillingCycle.MONTHLY)
        advanceUntilIdle()
        assertEquals(BillingCycle.MONTHLY, viewModel.uiState.value.selectedCycle)

        viewModel.selectBillingCycle(BillingCycle.YEARLY)
        advanceUntilIdle()
        assertEquals(BillingCycle.YEARLY, viewModel.uiState.value.selectedCycle)
    }

    @Test
    fun `upgradeToPro updates preferences and triggers success dialog`() = runTest {
        advanceUntilIdle()
        viewModel.selectBillingCycle(BillingCycle.YEARLY)
        viewModel.upgradeToPro(BillingCycle.YEARLY)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isPremium)
        assertEquals("YEARLY", state.activePlan)
        assertTrue(state.expiryDate.isNotBlank())
        assertTrue(state.showSuccessDialog)
        assertTrue(state.successMessage.contains("Welcome to TailorBook Pro Boutique"))
    }

    @Test
    fun `cancelSubscription downgrades back to free tier`() = runTest {
        advanceUntilIdle()
        viewModel.upgradeToPro(BillingCycle.MONTHLY)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPremium)

        viewModel.cancelSubscription()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isPremium)
        assertEquals("FREE", state.activePlan)
        assertEquals("", state.expiryDate)
    }

    @Test
    fun `billing manager provides localized prices to state`() = runTest {
        val fakeBilling = com.tailorbook.core.billing.FakePlayBillingManager()
        val vmWithBilling = SubscriptionViewModel(fakePreferences, fakeBilling)
        advanceUntilIdle()

        val state = vmWithBilling.uiState.value
        assertEquals("₹149.00", state.monthlyDisplayPrice)
        assertEquals("₹1,199.00", state.annualDisplayPrice)
        assertTrue(state.isBillingConnected)
    }
}
