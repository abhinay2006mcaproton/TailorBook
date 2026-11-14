package com.tailorbook.feature.customers.domain.repository

import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Customer and Family Member operations (TASK-05-02 / REQ-CUST-01).
 * Abstracts Room database queries with offline caching.
 */
interface CustomerRepository {

    fun getActiveCustomers(shopId: String = "default_shop"): Flow<List<CustomerEntity>>

    fun searchCustomers(query: String, shopId: String = "default_shop"): Flow<List<CustomerEntity>>

    suspend fun getCustomerById(customerId: String): CustomerEntity?

    fun getCustomerWithFamilyMembers(customerId: String): Flow<CustomerWithFamilyMembers?>

    fun getAllCustomersWithFamilyMembers(): Flow<List<CustomerWithFamilyMembers>> = getAllCustomersWithFamilyMembers("")

    fun getAllCustomersWithFamilyMembers(shopId: String): Flow<List<CustomerWithFamilyMembers>>

    suspend fun saveCustomer(customer: CustomerEntity): Long

    suspend fun updateCustomer(customer: CustomerEntity)

    suspend fun archiveCustomer(customerId: String)

    suspend fun addFamilyMember(member: FamilyMemberEntity): Long

    suspend fun updateFamilyMember(member: FamilyMemberEntity)

    suspend fun deleteFamilyMember(memberId: String)

    fun getFamilyMembersForCustomer(customerId: String): Flow<List<FamilyMemberEntity>>
}
