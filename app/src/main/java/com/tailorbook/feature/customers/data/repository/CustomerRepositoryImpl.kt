package com.tailorbook.feature.customers.data.repository

import android.content.Context
import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.core.sync.local.SyncQueueDao
import com.tailorbook.core.sync.local.SyncQueueEntity
import com.tailorbook.core.sync.worker.TailorSyncWorker
import com.tailorbook.feature.customers.data.local.dao.CustomerDao
import com.tailorbook.feature.customers.data.local.dao.FamilyMemberDao
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import com.tailorbook.feature.customers.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * Concrete implementation of [CustomerRepository] backed by Room DAOs and Supabase Sync Queue (TASK-05-02).
 */
class CustomerRepositoryImpl(
    private val customerDao: CustomerDao,
    private val familyMemberDao: FamilyMemberDao,
    private val syncQueueDao: SyncQueueDao? = null,
    private val context: Context? = null,
    private val preferencesManager: UserPreferencesManager? = null
) : CustomerRepository {

    override fun getActiveCustomers(shopId: String): Flow<List<CustomerEntity>> {
        return customerDao.getActiveCustomers(shopId)
    }

    override fun searchCustomers(query: String, shopId: String): Flow<List<CustomerEntity>> {
        return customerDao.searchCustomers(query.trim(), shopId)
    }

    override suspend fun getCustomerById(customerId: String): CustomerEntity? {
        return customerDao.getCustomerById(customerId)
    }

    override fun getCustomerWithFamilyMembers(customerId: String): Flow<CustomerWithFamilyMembers?> {
        return customerDao.getCustomerWithFamilyMembers(customerId)
    }

    override fun getAllCustomersWithFamilyMembers(): Flow<List<CustomerWithFamilyMembers>> {
        return customerDao.getAllCustomersWithFamilyMembers("")
    }

    override fun getAllCustomersWithFamilyMembers(shopId: String): Flow<List<CustomerWithFamilyMembers>> {
        return customerDao.getAllCustomersWithFamilyMembers(shopId)
    }

    override suspend fun saveCustomer(customer: CustomerEntity): Long {
        val rowId = customerDao.insertCustomer(customer)
        enqueueCustomerSync(customer, SyncQueueEntity.OP_INSERT)
        return rowId
    }

    override suspend fun updateCustomer(customer: CustomerEntity) {
        val updated = customer.copy(updatedAt = System.currentTimeMillis())
        customerDao.updateCustomer(updated)
        enqueueCustomerSync(updated, SyncQueueEntity.OP_UPDATE)
    }

    override suspend fun archiveCustomer(customerId: String) {
        customerDao.softDeleteCustomer(customerId, System.currentTimeMillis())
        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_CUSTOMER,
            entityId = customerId,
            operation = SyncQueueEntity.OP_DELETE,
            payloadJson = """{"id":"$customerId"}"""
        )
    }

    override suspend fun addFamilyMember(member: FamilyMemberEntity): Long {
        val rowId = familyMemberDao.insertFamilyMember(member)
        enqueueFamilySync(member, SyncQueueEntity.OP_INSERT)
        return rowId
    }

    override suspend fun updateFamilyMember(member: FamilyMemberEntity) {
        val updated = member.copy(updatedAt = System.currentTimeMillis())
        familyMemberDao.updateFamilyMember(updated)
        enqueueFamilySync(updated, SyncQueueEntity.OP_UPDATE)
    }

    override suspend fun deleteFamilyMember(memberId: String) {
        familyMemberDao.deleteById(memberId)
        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_CUSTOMER,
            entityId = memberId,
            operation = SyncQueueEntity.OP_DELETE,
            payloadJson = """{"id":"$memberId"}"""
        )
    }

    override fun getFamilyMembersForCustomer(customerId: String): Flow<List<FamilyMemberEntity>> {
        return familyMemberDao.getFamilyMembersForCustomer(customerId)
    }

    private suspend fun enqueueCustomerSync(customer: CustomerEntity, operation: String) {
        val payload = buildJsonObject {
            put("id", customer.id)
            put("shop_id", customer.shopId)
            put("name", customer.name)
            put("phone", customer.primaryPhone)
            customer.secondaryPhone?.let { put("secondary_phone", it) }
            put("gender", customer.gender)
            customer.address?.let { put("address", it) }
            customer.notes?.let { put("notes", it) }
        }.toString()

        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_CUSTOMER,
            entityId = customer.id,
            operation = operation,
            payloadJson = payload
        )
    }

    private suspend fun enqueueFamilySync(member: FamilyMemberEntity, operation: String) {
        val payload = buildJsonObject {
            put("id", member.id)
            put("customer_id", member.customerId)
            put("shop_id", member.shopId)
            put("name", member.name)
            put("relationship", member.relationship)
            member.gender?.let { put("gender", it) }
            member.notes?.let { put("notes", it) }
        }.toString()

        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_FAMILY_MEMBER,
            entityId = member.id,
            operation = operation,
            payloadJson = payload
        )
    }

    private suspend fun enqueueSync(entityType: String, entityId: String, operation: String, payloadJson: String) {
        val isEligible = preferencesManager?.isSyncEligible() ?: true
        if (!isEligible) return

        syncQueueDao?.let { dao ->
            runCatching {
                dao.enqueue(
                    SyncQueueEntity(
                        id = UUID.randomUUID().toString(),
                        entityType = entityType,
                        entityId = entityId,
                        operation = operation,
                        payloadJson = payloadJson
                    )
                )
                context?.let { ctx ->
                    TailorSyncWorker.triggerImmediateSync(ctx)
                }
            }
        }
    }
}
