package com.tailorbook.core.ads

import android.app.Activity
import android.content.Context
import com.tailorbook.feature.subscription.FakeCustomerDao
import com.tailorbook.feature.subscription.FakeSubscriptionPreferences
import com.tailorbook.feature.subscription.domain.CreationDecision
import com.tailorbook.feature.subscription.domain.CustomerEntitlementEngineImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fake implementation of RewardedAdManager for unit testing.
 */
class FakeRewardedAdManager(
    private var shouldRewardUser: Boolean = true,
    private var shouldFailToShow: Boolean = false
) : RewardedAdManager {

    private val _isAdLoaded = MutableStateFlow(false)
    override val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    override fun preloadAd(context: Context) {
        _isAdLoaded.value = true
        _isLoading.value = false
    }

    override fun showAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdFailed: (errorMessage: String) -> Unit
    ) {
        if (!_isAdLoaded.value) {
            onAdFailed("Ad not loaded")
            return
        }

        if (shouldFailToShow) {
            _isAdLoaded.value = false
            onAdFailed("Ad failed to show")
            return
        }

        _isAdLoaded.value = false
        if (shouldRewardUser) {
            onUserEarnedReward()
        } else {
            onAdFailed("User skipped or dismissed the video before completion.")
        }
    }

    fun setShouldRewardUser(reward: Boolean) {
        shouldRewardUser = reward
    }

    fun setShouldFailToShow(fail: Boolean) {
        shouldFailToShow = fail
    }

    fun setAdLoaded(loaded: Boolean) {
        _isAdLoaded.value = loaded
    }
}

/**
 * Tests the complete lifecycle of rewarded ads and single-use token consumption.
 */
class RewardedAdManagerTest {

    private lateinit var fakeDao: FakeCustomerDao
    private lateinit var prefs: FakeSubscriptionPreferences
    private lateinit var engine: CustomerEntitlementEngineImpl
    private lateinit var fakeAdManager: FakeRewardedAdManager

    @Before
    fun setUp() {
        fakeDao = FakeCustomerDao()
        fakeDao.countState.value = 15 // In Tier 2 (11-50): Requires rewarded ad
        prefs = FakeSubscriptionPreferences()
        engine = CustomerEntitlementEngineImpl(
            customerDao = fakeDao,
            subscriptionPreferences = prefs,
            shopIdProvider = { "test-shop" }
        )
        fakeAdManager = FakeRewardedAdManager()
    }

    @Test
    fun customerCreation_atCount15_requiresRewardedAdInitially() = runTest {
        val decision = engine.evaluateCreationDecision()
        assertTrue(decision is CreationDecision.RequiresRewardedAd)
        assertEquals(15, (decision as CreationDecision.RequiresRewardedAd).currentCount)
        assertEquals(16, decision.nextCustomerNumber)
        assertFalse(engine.hasValidRewardToken())
    }

    @Test
    fun rewardedAdCompleted_grantsValidSingleUseToken_andAllowsCreation() = runTest {
        var rewardEarned = false
        var failureMessage: String? = null

        fakeAdManager.setAdLoaded(true)
        fakeAdManager.setShouldRewardUser(true)

        val dummyActivity = Activity()

        fakeAdManager.showAd(
            activity = dummyActivity,
            onUserEarnedReward = {
                rewardEarned = true
            },
            onAdFailed = { err ->
                failureMessage = err
            }
        )

        assertTrue(rewardEarned)
        assertFalse(fakeAdManager.isAdLoaded.value)

        // Grant token upon verified reward
        val token = engine.grantRewardToken()
        assertTrue(token.startsWith("tb_rwd_"))
        assertTrue(engine.hasValidRewardToken())

        // Entitlement re-evaluated: now allowed with reward
        val nextDecision = engine.evaluateCreationDecision()
        assertTrue(nextDecision is CreationDecision.AllowedWithReward)

        // Consuming the token authorizes customer save
        val consumed = engine.consumeRewardToken(token)
        assertTrue(consumed)

        // Attempting to reuse the same token fails (single customer authorization guarantee)
        val reused = engine.consumeRewardToken(token)
        assertFalse(reused)
        assertFalse(engine.hasValidRewardToken())
    }

    @Test
    fun rewardedAdSkippedOrFailed_doesNotGrantRewardToken_andPreservesRequirement() = runTest {
        var rewardEarned = false
        var failureMessage: String? = null

        fakeAdManager.setAdLoaded(true)
        fakeAdManager.setShouldRewardUser(false) // User skipped the video

        val dummyActivity = Activity()

        fakeAdManager.showAd(
            activity = dummyActivity,
            onUserEarnedReward = {
                rewardEarned = true
            },
            onAdFailed = { err ->
                failureMessage = err
            }
        )

        assertFalse(rewardEarned)
        assertEquals("User skipped or dismissed the video before completion.", failureMessage)

        // Engine state is unchanged: no token granted
        assertFalse(engine.hasValidRewardToken())
        val decision = engine.evaluateCreationDecision()
        assertTrue(decision is CreationDecision.RequiresRewardedAd)
    }
}
