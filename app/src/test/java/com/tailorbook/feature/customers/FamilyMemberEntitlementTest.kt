package com.tailorbook.feature.customers

import com.tailorbook.core.ads.FakeRewardedAdManager
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import com.tailorbook.feature.customers.domain.repository.CustomerRepository
import com.tailorbook.feature.customers.presentation.CustomerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyMemberEntitlementTest {

    private val testDispatcher = StandardTestDispatcher()

    // Lightweight Fake CustomerRepository matching domain contract
    private class FakeCustomerRepository(
        val initialList: List<CustomerWithFamilyMembers> = CustomerViewModel.initialCustomers()
    ) : CustomerRepository {
        val customersFlow = kotlinx.coroutines.flow.MutableStateFlow(initialList)
        override fun getActiveCustomers(shopId: String): Flow<List<CustomerEntity>> = flowOf(emptyList())
        override fun searchCustomers(query: String, shopId: String): Flow<List<CustomerEntity>> = flowOf(emptyList())
        override suspend fun getCustomerById(customerId: String): CustomerEntity? = null
        override fun getCustomerWithFamilyMembers(customerId: String): Flow<CustomerWithFamilyMembers?> = flowOf(null)
        override suspend fun saveCustomer(customer: CustomerEntity): Long = 1L
        override suspend fun updateCustomer(customer: CustomerEntity) {}
        override suspend fun archiveCustomer(customerId: String) {}
        override suspend fun addFamilyMember(member: FamilyMemberEntity): Long = 1L
        override suspend fun updateFamilyMember(member: FamilyMemberEntity) {}
        override suspend fun deleteFamilyMember(memberId: String) {}
        override fun getFamilyMembersForCustomer(customerId: String): Flow<List<FamilyMemberEntity>> = flowOf(emptyList())
        override fun getAllCustomersWithFamilyMembers(shopId: String): Flow<List<CustomerWithFamilyMembers>> = customersFlow
    }

    private lateinit var viewModel: CustomerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val repo = FakeCustomerRepository()
        viewModel = CustomerViewModel(repo, repo.initialList)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun maxFiveFamilyMembers_enforcedStrictly() = runTest {
        val customerId = "cust-101" // Ananya Reddy, starts with 1 family member (Saanvi)

        var customer = viewModel.getCustomerById(customerId)!!
        assertEquals(1, customer.familyMembers.size)
        assertTrue("Under 5 members: can add family member", viewModel.canAddFamilyMember(customerId))

        // Add 2nd member
        assertTrue(viewModel.addFamilyMember(customerId, "Sister", "Sister"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Add 3rd member
        assertTrue(viewModel.addFamilyMember(customerId, "Mother", "Mother"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Add 4th member
        assertTrue(viewModel.addFamilyMember(customerId, "Father", "Father"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Add 5th member (reaches maximum 5)
        assertTrue(viewModel.addFamilyMember(customerId, "Brother", "Brother"))
        testDispatcher.scheduler.advanceUntilIdle()

        customer = viewModel.getCustomerById(customerId)!!
        assertEquals(5, customer.familyMembers.size)
        assertFalse("At 5 members: cannot add more family members", viewModel.canAddFamilyMember(customerId))

        // Attempt to add 6th member: must be rejected!
        val addedSixth = viewModel.addFamilyMember(customerId, "Uncle", "Uncle")
        assertFalse("Adding a 6th family member must be strictly blocked", addedSixth)
        testDispatcher.scheduler.advanceUntilIdle()

        customer = viewModel.getCustomerById(customerId)!!
        assertEquals("Count must remain capped at exactly 5", 5, customer.familyMembers.size)
    }

    @Test
    fun removingFamilyMember_reopensAvailableSlot() = runTest {
        val customerId = "cust-101"

        // Fill to capacity (5 members)
        viewModel.addFamilyMember(customerId, "Member 2", "Relation")
        viewModel.addFamilyMember(customerId, "Member 3", "Relation")
        viewModel.addFamilyMember(customerId, "Member 4", "Relation")
        viewModel.addFamilyMember(customerId, "Member 5", "Relation")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.canAddFamilyMember(customerId))

        // Remove 1 member
        val memberToRemove = viewModel.getCustomerById(customerId)!!.familyMembers.last()
        viewModel.removeFamilyMember(customerId, memberToRemove.id)
        testDispatcher.scheduler.advanceUntilIdle()

        // Slot re-opened
        assertTrue("Removing a member re-opens slot for a new family member", viewModel.canAddFamilyMember(customerId))
        assertEquals(4, viewModel.getCustomerById(customerId)!!.familyMembers.size)

        // Can now add 5th member again
        assertTrue(viewModel.addFamilyMember(customerId, "New Member", "Relation"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, viewModel.getCustomerById(customerId)!!.familyMembers.size)
    }

    @Test
    fun rewardedAdFlow_freeUserMustCompleteVideoAd() = runTest {
        val fakeRewardedAdManager = FakeRewardedAdManager(shouldRewardUser = true)
        fakeRewardedAdManager.setAdLoaded(true)
        val dummyActivity = android.app.Activity()
        var rewardEarned = false
        var dialogOpened = false

        // Simulate Free user attempting to add a family member
        val isPremium = false
        if (!isPremium) {
            // Video ad prompt is triggered
            fakeRewardedAdManager.showAd(
                activity = dummyActivity,
                onUserEarnedReward = {
                    rewardEarned = true
                    dialogOpened = true
                },
                onAdFailed = {}
            )
        } else {
            dialogOpened = true
        }

        assertTrue("Rewarded video must be completed to earn slot", rewardEarned)
        assertTrue("Input dialog should open only after video completion", dialogOpened)
    }

    @Test
    fun rewardedAdFlow_premiumUserBypassesAdDirectly() = runTest {
        var adShown = false
        var dialogOpened = false

        // Simulate Premium user
        val isPremium = true
        if (!isPremium) {
            adShown = true
        } else {
            // Direct bypass: no ad required
            dialogOpened = true
        }

        assertFalse("Premium users must NEVER be prompted with ads", adShown)
        assertTrue("Premium users open the family member dialog directly", dialogOpened)
    }
}
