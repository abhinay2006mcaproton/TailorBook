package com.tailorbook

import com.tailorbook.feature.customers.data.local.dao.CustomerDao
import com.tailorbook.feature.customers.data.local.dao.FamilyMemberDao
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import com.tailorbook.feature.customers.data.repository.CustomerRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TASK-05-02: CustomerRepository with offline caching behaviors.
 */
class CustomerRepositoryTest {

    private lateinit var fakeCustomerDao: FakeCustomerDao
    private lateinit var fakeFamilyMemberDao: FakeFamilyMemberDao
    private lateinit var repository: CustomerRepositoryImpl

    @Before
    fun setup() {
        fakeCustomerDao = FakeCustomerDao()
        fakeFamilyMemberDao = FakeFamilyMemberDao()
        repository = CustomerRepositoryImpl(fakeCustomerDao, fakeFamilyMemberDao)
    }

    @Test
    fun testSaveAndGetCustomer() = runBlocking {
        val customer = CustomerEntity(name = "Kavitha Rao", primaryPhone = "9876543210")
        repository.saveCustomer(customer)

        val retrieved = repository.getCustomerById(customer.id)
        assertNotNull(retrieved)
        assertEquals("Kavitha Rao", retrieved?.name)
        assertEquals("9876543210", retrieved?.primaryPhone)
    }

    @Test
    fun testArchiveCustomer_softDeletes() = runBlocking {
        val customer = CustomerEntity(name = "Sunitha", primaryPhone = "9848011223")
        repository.saveCustomer(customer)

        repository.archiveCustomer(customer.id)
        val active = repository.getActiveCustomers().first()
        assertTrue("Archived customer must not appear in active list", active.none { it.id == customer.id })
    }

    @Test
    fun testSearchCustomers_matchesNameOrPhone() = runBlocking {
        val c1 = CustomerEntity(name = "Bhavani S", primaryPhone = "9988776655")
        val c2 = CustomerEntity(name = "Suresh K", primaryPhone = "9123456789")
        repository.saveCustomer(c1)
        repository.saveCustomer(c2)

        val resultsByName = repository.searchCustomers("Bhavani").first()
        assertEquals(1, resultsByName.size)
        assertEquals("Bhavani S", resultsByName[0].name)

        val resultsByPhone = repository.searchCustomers("9123").first()
        assertEquals(1, resultsByPhone.size)
        assertEquals("Suresh K", resultsByPhone[0].name)
    }

    @Test
    fun testFamilyMembers_addAndRetrieve() = runBlocking {
        val customerId = "cust-1"
        val member = FamilyMemberEntity(
            customerId = customerId,
            name = "Ananya",
            relationship = "Daughter"
        )
        repository.addFamilyMember(member)

        val members = repository.getFamilyMembersForCustomer(customerId).first()
        assertEquals(1, members.size)
        assertEquals("Ananya", members[0].name)

        repository.deleteFamilyMember(member.id)
        val remaining = repository.getFamilyMembersForCustomer(customerId).first()
        assertEquals(0, remaining.size)
    }
}

private class FakeCustomerDao : CustomerDao {
    val customers = mutableMapOf<String, CustomerEntity>()

    override suspend fun insertCustomer(customer: CustomerEntity): Long {
        customers[customer.id] = customer
        return 1L
    }

    override suspend fun insertCustomers(customersList: List<CustomerEntity>) {
        customersList.forEach { customers[it.id] = it }
    }

    override suspend fun updateCustomer(customer: CustomerEntity) {
        customers[customer.id] = customer
    }

    override suspend fun softDeleteCustomer(customerId: String, updatedAt: Long) {
        customers[customerId]?.let {
            customers[customerId] = it.copy(isArchived = true, updatedAt = updatedAt)
        }
    }

    override suspend fun getCustomerById(customerId: String): CustomerEntity? {
        return customers[customerId]
    }

    override fun getActiveCustomers(shopId: String): Flow<List<CustomerEntity>> {
        return flowOf(customers.values.filter { it.shopId == shopId && !it.isArchived })
    }

    override fun searchCustomers(query: String, shopId: String): Flow<List<CustomerEntity>> {
        return flowOf(
            customers.values.filter {
                it.shopId == shopId && !it.isArchived &&
                        (it.name.contains(query, ignoreCase = true) || it.primaryPhone.contains(query))
            }
        )
    }

    override fun getCustomerWithFamilyMembers(customerId: String): Flow<CustomerWithFamilyMembers?> {
        val cust = customers[customerId] ?: return flowOf(null)
        return flowOf(CustomerWithFamilyMembers(cust, emptyList()))
    }

    override fun getAllCustomersWithFamilyMembers(shopId: String): Flow<List<CustomerWithFamilyMembers>> {
        return flowOf(customers.values.filter { (shopId.isBlank() || it.shopId == shopId) && !it.isArchived }.map { CustomerWithFamilyMembers(it, emptyList()) })
    }

    override fun getActiveCustomerCount(shopId: String): Flow<Int> {
        return flowOf(customers.values.count { it.shopId == shopId && !it.isArchived })
    }
}

private class FakeFamilyMemberDao : FamilyMemberDao {
    val members = mutableMapOf<String, FamilyMemberEntity>()

    override suspend fun insertFamilyMember(member: FamilyMemberEntity): Long {
        members[member.id] = member
        return 1L
    }

    override suspend fun insertFamilyMembers(membersList: List<FamilyMemberEntity>) {
        membersList.forEach { members[it.id] = it }
    }

    override suspend fun updateFamilyMember(member: FamilyMemberEntity) {
        members[member.id] = member
    }

    override suspend fun deleteFamilyMember(member: FamilyMemberEntity) {
        members.remove(member.id)
    }

    override suspend fun deleteById(memberId: String) {
        members.remove(memberId)
    }

    override fun getFamilyMembersForCustomer(customerId: String): Flow<List<FamilyMemberEntity>> {
        return flowOf(members.values.filter { it.customerId == customerId })
    }

    override suspend fun getFamilyMemberById(memberId: String): FamilyMemberEntity? {
        return members[memberId]
    }
}
