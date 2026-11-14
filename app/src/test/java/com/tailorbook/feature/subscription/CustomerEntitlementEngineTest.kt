package com.tailorbook.feature.subscription

import com.tailorbook.core.datastore.SubscriptionPreferences
import com.tailorbook.feature.customers.data.local.dao.CustomerDao
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.subscription.domain.CreationDecision
import com.tailorbook.feature.subscription.domain.CustomerEntitlementEngine
import com.tailorbook.feature.subscription.domain.CustomerEntitlementEngineImpl
import com.tailorbook.feature.subscription.domain.EntitlementConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSubscriptionPreferences : SubscriptionPreferences {
    val isPremiumState = MutableStateFlow(false)
    val planState = MutableStateFlow("FREE")
    val expiryState = MutableStateFlow("")

    override val isPremiumFlow: Flow<Boolean> = isPremiumState.asStateFlow()
    override val subscriptionPlanFlow: Flow<String> = planState.asStateFlow()
    override val subscriptionExpiryFlow: Flow<String> = expiryState.asStateFlow()

    override suspend fun setSubscription(isPremium: Boolean, plan: String, expiry: String) {
        isPremiumState.value = isPremium
        planState.value = plan
        expiryState.value = expiry
    }

    override suspend fun cancelSubscription() {
        isPremiumState.value = false
        planState.value = "FREE"
        expiryState.value = ""
    }
}

class FakeCustomerDao : CustomerDao {
    val countState = MutableStateFlow(0)

    override fun getActiveCustomerCount(shopId: String): Flow<Int> = countState.asStateFlow()

    override suspend fun insertCustomer(customer: CustomerEntity): Long = 1L
    override suspend fun insertCustomers(customers: List<CustomerEntity>) {}
    override suspend fun updateCustomer(customer: CustomerEntity) {}
    override suspend fun softDeleteCustomer(customerId: String, updatedAt: Long) {}
    override suspend fun getCustomerById(customerId: String): CustomerEntity? = null
    override fun getActiveCustomers(shopId: String): Flow<List<CustomerEntity>> = MutableStateFlow(emptyList())
    override fun searchCustomers(query: String, shopId: String): Flow<List<CustomerEntity>> = MutableStateFlow(emptyList())
    override fun getCustomerWithFamilyMembers(customerId: String): Flow<com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers?> = MutableStateFlow(null)
    override fun getAllCustomersWithFamilyMembers(shopId: String): Flow<List<com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers>> = MutableStateFlow(emptyList())
}

class CustomerEntitlementEngineTest {

    private lateinit var customerDao: FakeCustomerDao
    private lateinit var subscriptionPrefs: FakeSubscriptionPreferences
    private lateinit var engine: CustomerEntitlementEngine

    @Before
    fun setUp() {
        customerDao = FakeCustomerDao()
        subscriptionPrefs = FakeSubscriptionPreferences()
        engine = CustomerEntitlementEngineImpl(
            customerDao = customerDao,
            subscriptionPreferences = subscriptionPrefs,
            shopIdProvider = { "test-shop" }
        )
    }

    @Test
    fun `customers 1 to 10 are allowed normally without ads`() = runBlocking {
        assertEquals(10, EntitlementConstants.FREE_TIER_LIMIT)
        assertEquals(20, EntitlementConstants.REWARDED_AD_TIER_LIMIT)

        // Count = 0
        customerDao.countState.value = 0
        val d0 = engine.evaluateCreationDecision()
        assertTrue(d0 is CreationDecision.AllowedNormal)
        assertEquals(10, (d0 as CreationDecision.AllowedNormal).remainingFreeSlots)

        // Count = 9 (Creating 10th customer)
        customerDao.countState.value = 9
        val d9 = engine.evaluateCreationDecision()
        assertTrue(d9 is CreationDecision.AllowedNormal)
        assertEquals(1, (d9 as CreationDecision.AllowedNormal).remainingFreeSlots)
    }

    @Test
    fun `customer 11 requires a verified rewarded ad`() = runBlocking {
        // Count = 10 (Creating 11th customer)
        customerDao.countState.value = 10
        val decision = engine.evaluateCreationDecision()
        assertTrue("Decision must require rewarded video", decision is CreationDecision.RequiresRewardedAd)
        assertEquals(11, (decision as CreationDecision.RequiresRewardedAd).nextCustomerNumber)
        assertFalse(engine.hasValidRewardToken())
    }

    @Test
    fun `granting reward token allows single customer creation`() = runBlocking {
        customerDao.countState.value = 10
        val token = engine.grantRewardToken()
        assertTrue("Token must not be empty", token.isNotBlank())
        assertTrue(engine.hasValidRewardToken())

        // Re-evaluate decision
        val decision = engine.evaluateCreationDecision()
        assertTrue("Decision must now be AllowedWithReward", decision is CreationDecision.AllowedWithReward)
        assertEquals(token, (decision as CreationDecision.AllowedWithReward).rewardToken)

        // Consume the token upon saving
        val consumedFirst = engine.consumeRewardToken(token)
        assertTrue("First consumption must succeed", consumedFirst)
        assertFalse("Token must no longer be valid after consumption", engine.hasValidRewardToken())

        // Attempting to reuse the same token must fail
        val consumedSecond = engine.consumeRewardToken(token)
        assertFalse("Reusing the same reward token must be blocked", consumedSecond)
    }

    @Test
    fun `customers 11 to 20 all require rewarded video until 20 limit is reached`() = runBlocking {
        // Count = 19 (Creating 20th customer)
        customerDao.countState.value = 19
        val decision = engine.evaluateCreationDecision()
        assertTrue(decision is CreationDecision.RequiresRewardedAd)
        assertEquals(20, (decision as CreationDecision.RequiresRewardedAd).nextCustomerNumber)
    }

    @Test
    fun `customer 21 is strictly blocked and requires Premium subscription`() = runBlocking {
        // Count = 20 (Attempting to create 21st customer)
        customerDao.countState.value = 20
        val decision = engine.evaluateCreationDecision()
        assertTrue("Customer 21+ must require Premium", decision is CreationDecision.RequiresPremium)
        assertEquals(20, (decision as CreationDecision.RequiresPremium).currentCount)

        // Even with a reward token, 21+ must be blocked unless Premium
        engine.grantRewardToken()
        val decisionWithToken = engine.evaluateCreationDecision()
        assertTrue("21+ cannot bypass with ad token", decisionWithToken is CreationDecision.RequiresPremium)
    }

    @Test
    fun `active Premium allows unlimited customer creation and disables ads`() = runBlocking {
        customerDao.countState.value = 75
        subscriptionPrefs.isPremiumState.value = true

        val decision = engine.evaluateCreationDecision()
        assertTrue("Premium user must be AllowedUnlimited", decision is CreationDecision.AllowedUnlimited)
        assertEquals(75, (decision as CreationDecision.AllowedUnlimited).currentCount)
    }
}
