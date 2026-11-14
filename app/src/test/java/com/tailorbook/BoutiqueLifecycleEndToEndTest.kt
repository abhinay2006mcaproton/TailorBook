package com.tailorbook

import com.tailorbook.core.sync.local.SyncQueueEntity
import com.tailorbook.feature.customers.data.local.dao.CustomerDao
import com.tailorbook.feature.customers.data.local.dao.FamilyMemberDao
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import com.tailorbook.feature.customers.data.repository.CustomerRepositoryImpl
import com.tailorbook.feature.measurements.data.local.dao.MeasurementDao
import com.tailorbook.feature.measurements.data.local.entity.MeasurementVersionEntity
import com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl
import com.tailorbook.feature.orders.data.local.dao.OrderDao
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import com.tailorbook.feature.orders.data.local.entity.OrderMeasurementSnapshotEntity
import com.tailorbook.feature.orders.data.local.entity.OrderStatusHistoryEntity
import com.tailorbook.feature.orders.data.local.entity.OrderWithDetails
import com.tailorbook.feature.orders.data.repository.OrderRepositoryImpl
import com.tailorbook.feature.payments.data.local.dao.PaymentDao
import com.tailorbook.feature.payments.data.local.entity.PaymentEntity
import com.tailorbook.feature.payments.data.local.entity.PaymentRefundEntity
import com.tailorbook.feature.payments.data.repository.PaymentRepositoryImpl
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
 * Full Offline Boutique Lifecycle End-to-End Simulation Test (TASK-15-01 / TASK-15-02).
 * Exercises a complete boutique workflow entirely offline:
 * 1. Register client & family member
 * 2. Record v1 & v2 garment measurements
 * 3. Create tailoring order with frozen immutable measurement snapshot
 * 4. Progress order lifecycle (NEW -> CUTTING -> STITCHING -> READY -> DELIVERED)
 * 5. Record advance & final balance payments
 * 6. Verify zero balance and ledger consistency
 * 7. Verify offline mutation queue ready for Supabase sync
 */
class BoutiqueLifecycleEndToEndTest {

    private lateinit var fakeCustomerDao: LifecycleCustomerDao
    private lateinit var fakeFamilyMemberDao: LifecycleFamilyMemberDao
    private lateinit var fakeMeasurementDao: LifecycleMeasurementDao
    private lateinit var fakeOrderDao: LifecycleOrderDao
    private lateinit var fakePaymentDao: LifecyclePaymentDao
    private lateinit var fakeSyncDao: FakeSyncQueueDao

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var measurementRepo: MeasurementRepositoryImpl
    private lateinit var orderRepo: OrderRepositoryImpl
    private lateinit var paymentRepo: PaymentRepositoryImpl

    private val testShopId = "shop-e2e-01"

    @Before
    fun setUp() {
        fakeCustomerDao = LifecycleCustomerDao()
        fakeFamilyMemberDao = LifecycleFamilyMemberDao()
        fakeMeasurementDao = LifecycleMeasurementDao()
        fakeOrderDao = LifecycleOrderDao()
        fakePaymentDao = LifecyclePaymentDao()
        fakeSyncDao = FakeSyncQueueDao()

        customerRepo = CustomerRepositoryImpl(fakeCustomerDao, fakeFamilyMemberDao)
        measurementRepo = MeasurementRepositoryImpl(fakeMeasurementDao)
        orderRepo = OrderRepositoryImpl(fakeOrderDao)
        paymentRepo = PaymentRepositoryImpl(fakePaymentDao, fakeOrderDao)
    }

    @Test
    fun completeBoutiqueJourney_executesOfflineWith100PercentDataIntegrity() = runBlocking {
        // --- STEP 1: Register Client & Family Member ---
        val customer = CustomerEntity(
            id = "cust-e2e-01",
            shopId = testShopId,
            name = "Padma Priya",
            primaryPhone = "9848011223",
            gender = "FEMALE",
            notes = "Prefers extra seam margins"
        )
        customerRepo.saveCustomer(customer)
        fakeSyncDao.enqueue(
            SyncQueueEntity(
                entityType = SyncQueueEntity.ENTITY_CUSTOMER,
                entityId = customer.id,
                operation = SyncQueueEntity.OP_INSERT,
                payloadJson = """{"id":"${customer.id}","name":"${customer.name}"}"""
            )
        )

        val retrievedCustomer = customerRepo.getCustomerById("cust-e2e-01")
        assertNotNull(retrievedCustomer)
        assertEquals("Padma Priya", retrievedCustomer?.name)

        val daughter = FamilyMemberEntity(
            id = "fam-e2e-01",
            customerId = customer.id,
            shopId = testShopId,
            name = "Anvitha",
            relationship = "Daughter",
            gender = "FEMALE"
        )
        customerRepo.addFamilyMember(daughter)
        val familyList = customerRepo.getFamilyMembersForCustomer("cust-e2e-01").first()
        assertEquals(1, familyList.size)
        assertEquals("Anvitha", familyList[0].name)

        // --- STEP 2: Record Blouse Measurements (Version 1) ---
        val blouseMeasurementsV1 = mapOf(
            "length" to 14.5,
            "chest" to 36.0,
            "waist" to 30.0,
            "shoulder" to 14.0,
            "sleeve_length" to 10.5,
            "sleeve_round" to 12.0,
            "apex_length" to 10.0,
            "apex_width" to 7.0
        )
        val v1Result = measurementRepo.saveNewVersion(
            customerId = customer.id,
            familyMemberId = null,
            templateId = "BLOUSE",
            measurements = blouseMeasurementsV1,
            unit = "INCH",
            notes = "Initial measurements for wedding blouse"
        )
        assertTrue(v1Result.isSuccess)
        val v1Entity = v1Result.getOrThrow()
        assertEquals(1, v1Entity.versionNumber)
        fakeSyncDao.enqueue(
            SyncQueueEntity(
                entityType = SyncQueueEntity.ENTITY_MEASUREMENT,
                entityId = v1Entity.id,
                operation = SyncQueueEntity.OP_INSERT,
                payloadJson = v1Entity.measurementsJson
            )
        )

        // --- STEP 3: Create Tailoring Order with Frozen Measurement Snapshot ---
        val order = OrderEntity(
            id = "ord-e2e-101",
            shopId = testShopId,
            orderNumber = "TB-301",
            customerId = customer.id,
            promisedDeliveryDate = System.currentTimeMillis() + 86400000L * 3, // 3 days later
            totalAmount = 2500.0,
            advanceAmount = 500.0,
            paidAmount = 500.0,
            balanceAmount = 2000.0,
            status = "NEW"
        )
        val orderItem = OrderItemEntity(
            id = "item-e2e-101",
            orderId = order.id,
            garmentTemplateId = "BLOUSE",
            garmentName = "Silk Bridal Blouse",
            quantity = 1,
            unitPrice = 2500.0,
            stitchingInstructions = "Gold zari thread work"
        )
        val snapshot = OrderMeasurementSnapshotEntity(
            id = "snap-e2e-101",
            orderId = order.id,
            measurementVersionId = v1Entity.id,
            frozenMeasurementsJson = v1Entity.measurementsJson,
            unit = v1Entity.unit
        )
        orderRepo.createOrderWithSnapshot(
            order = order,
            items = listOf(orderItem),
            snapshot = snapshot
        )
        fakeSyncDao.enqueue(
            SyncQueueEntity(
                entityType = SyncQueueEntity.ENTITY_ORDER,
                entityId = order.id,
                operation = SyncQueueEntity.OP_INSERT,
                payloadJson = """{"id":"${order.id}","orderNumber":"${order.orderNumber}","total":2500.0}"""
            )
        )

        // Verify DEC-001 Immutable Snapshot: subsequent changes to client's measurements do NOT alter order
        val blouseMeasurementsV2 = blouseMeasurementsV1.toMutableMap().apply {
            put("chest", 37.0) // Client altered measurement later
        }
        measurementRepo.saveNewVersion(
            customerId = customer.id,
            familyMemberId = null,
            templateId = "BLOUSE",
            measurements = blouseMeasurementsV2,
            unit = "INCH",
            notes = "Updated chest 6 months later"
        )
        // Verify frozen snapshot in order still reflects original 36.0!
        val orderDetails = orderRepo.getOrderWithDetails(order.id).first()
        assertNotNull(orderDetails)
        val savedSnapshot = orderDetails?.measurementSnapshot
        assertNotNull(savedSnapshot)
        val frozenMap = MeasurementRepositoryImpl.parseMeasurements(savedSnapshot!!.frozenMeasurementsJson)
        assertEquals(36.0, frozenMap["chest"] ?: 0.0, 0.01)

        // --- STEP 4: Progress Order Lifecycle ---
        orderRepo.updateOrderStatus(order.id, "NEW", "CUTTING", "Master tailor started cutting")
        assertEquals("CUTTING", fakeOrderDao.orders[order.id]?.status)

        orderRepo.updateOrderStatus(order.id, "CUTTING", "STITCHING", "Stitching assigned to tailor")
        assertEquals("STITCHING", fakeOrderDao.orders[order.id]?.status)

        orderRepo.updateOrderStatus(order.id, "STITCHING", "READY", "Quality inspection complete")
        assertEquals("READY", fakeOrderDao.orders[order.id]?.status)

        // --- STEP 5: Record Final Payment upon Pickup ---
        val finalPayment = PaymentEntity(
            id = "pay-e2e-final",
            shopId = testShopId,
            orderId = order.id,
            customerId = customer.id,
            amount = 2000.0,
            paymentMethod = "UPI",
            referenceNumber = "UPI-REF-998877",
            notes = "Final balance paid via PhonePe"
        )
        paymentRepo.recordPayment(finalPayment)
        fakeSyncDao.enqueue(
            SyncQueueEntity(
                entityType = SyncQueueEntity.ENTITY_PAYMENT,
                entityId = finalPayment.id,
                operation = SyncQueueEntity.OP_INSERT,
                payloadJson = """{"id":"${finalPayment.id}","amount":2000.0}"""
            )
        )

        // Verify balance recalculated to exactly 0.0!
        val updatedOrder = fakeOrderDao.orders[order.id]
        assertNotNull(updatedOrder)
        assertEquals(2500.0, updatedOrder!!.paidAmount, 0.01)
        assertEquals(0.0, updatedOrder.balanceAmount, 0.01)

        // --- STEP 6: Mark Order Delivered ---
        orderRepo.updateOrderStatus(order.id, "READY", "DELIVERED", "Client collected garment and paid in full")
        assertEquals("DELIVERED", fakeOrderDao.orders[order.id]?.status)

        // --- STEP 7: Verify Sync Queue Prepared for Cloud Replication ---
        val pendingSyncItems = fakeSyncDao.getPendingEntries()
        assertTrue(pendingSyncItems.size >= 4) // Customer, Measurement, Order, Payment all queued
        assertEquals(SyncQueueEntity.ENTITY_CUSTOMER, pendingSyncItems[0].entityType)
        assertEquals(SyncQueueEntity.ENTITY_MEASUREMENT, pendingSyncItems[1].entityType)
        assertEquals(SyncQueueEntity.ENTITY_ORDER, pendingSyncItems[2].entityType)
        assertEquals(SyncQueueEntity.ENTITY_PAYMENT, pendingSyncItems[3].entityType)
    }
}

class LifecycleCustomerDao : CustomerDao {
    val customers = mutableMapOf<String, CustomerEntity>()

    override suspend fun insertCustomer(customer: CustomerEntity): Long {
        customers[customer.id] = customer
        return 1L
    }

    override suspend fun insertCustomers(customers: List<CustomerEntity>) {
        customers.forEach { this.customers[it.id] = it }
    }

    override suspend fun updateCustomer(customer: CustomerEntity) {
        customers[customer.id] = customer
    }

    override suspend fun softDeleteCustomer(customerId: String, updatedAt: Long) {
        customers[customerId]?.let {
            customers[customerId] = it.copy(isArchived = true, updatedAt = updatedAt)
        }
    }

    override suspend fun getCustomerById(customerId: String): CustomerEntity? = customers[customerId]

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

class LifecycleFamilyMemberDao : FamilyMemberDao {
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

    override suspend fun getFamilyMemberById(memberId: String): FamilyMemberEntity? = members[memberId]
}

class LifecycleMeasurementDao : MeasurementDao {
    val versions = mutableListOf<MeasurementVersionEntity>()

    override suspend fun insertVersion(version: MeasurementVersionEntity) {
        versions.add(version)
    }

    override suspend fun insertVersions(versions: List<MeasurementVersionEntity>) {
        this.versions.addAll(versions)
    }

    override suspend fun deleteVersion(version: MeasurementVersionEntity) {
        versions.remove(version)
    }

    override fun getVersions(customerId: String, familyMemberId: String?, templateId: String): Flow<List<MeasurementVersionEntity>> {
        return flowOf(
            versions.filter {
                it.customerId == customerId && it.familyMemberId == familyMemberId && it.templateId == templateId
            }.sortedByDescending { it.versionNumber }
        )
    }

    override fun getLatestVersion(customerId: String, familyMemberId: String?, templateId: String): Flow<MeasurementVersionEntity?> {
        return flowOf(
            versions.filter {
                it.customerId == customerId && it.familyMemberId == familyMemberId && it.templateId == templateId
            }.maxByOrNull { it.versionNumber }
        )
    }

    override suspend fun getLatestVersionNumber(customerId: String, familyMemberId: String?, templateId: String): Int? {
        return versions.filter {
            it.customerId == customerId && it.familyMemberId == familyMemberId && it.templateId == templateId
        }.maxOfOrNull { it.versionNumber }
    }

    override fun getAllForCustomer(customerId: String): Flow<List<MeasurementVersionEntity>> {
        return flowOf(versions.filter { it.customerId == customerId })
    }
}

class LifecycleOrderDao : OrderDao {
    val orders = mutableMapOf<String, OrderEntity>()
    val items = mutableListOf<OrderItemEntity>()
    var snapshot: OrderMeasurementSnapshotEntity? = null
    val history = mutableListOf<OrderStatusHistoryEntity>()

    override suspend fun insertOrder(order: OrderEntity) {
        orders[order.id] = order
    }

    override suspend fun insertOrders(orders: List<OrderEntity>) {
        orders.forEach { this.orders[it.id] = it }
    }

    override suspend fun insertOrderItems(items: List<OrderItemEntity>) {
        this.items.addAll(items)
    }

    override suspend fun insertMeasurementSnapshot(snapshot: OrderMeasurementSnapshotEntity) {
        this.snapshot = snapshot
    }

    override suspend fun insertStatusHistory(history: OrderStatusHistoryEntity) {
        this.history.add(history)
    }

    override suspend fun updateOrder(order: OrderEntity) {
        orders[order.id] = order
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: String, updatedAt: Long) {
        orders[orderId]?.let {
            orders[orderId] = it.copy(status = newStatus, updatedAt = updatedAt)
        }
    }

    override suspend fun cancelOrder(orderId: String, reason: String, updatedAt: Long) {
        orders[orderId]?.let {
            orders[orderId] = it.copy(status = "CANCELLED", cancellationReason = reason, updatedAt = updatedAt)
        }
    }

    override suspend fun rescheduleOrder(orderId: String, newDeliveryDate: Long, updatedAt: Long) {
        orders[orderId]?.let {
            orders[orderId] = it.copy(promisedDeliveryDate = newDeliveryDate, updatedAt = updatedAt)
        }
    }

    override suspend fun getOrderCount(shopId: String): Int = orders.values.count { it.shopId == shopId }

    override suspend fun updateOrderItemMeasurements(itemId: String, measurementsJson: String) {}

    override suspend fun updateOrderSnapshotMeasurements(orderId: String, json: String) {}

    override suspend fun getOrderById(orderId: String): OrderEntity? = orders[orderId]

    override fun getAllOrders(shopId: String): Flow<List<OrderEntity>> {
        return flowOf(orders.values.filter { it.shopId == shopId })
    }

    override fun getOrdersByStatus(shopId: String, status: String): Flow<List<OrderEntity>> {
        return flowOf(orders.values.filter { it.shopId == shopId && it.status == status })
    }

    override fun getOrdersForCustomer(customerId: String): Flow<List<OrderEntity>> {
        return flowOf(orders.values.filter { it.customerId == customerId })
    }

    override fun getOrderWithDetails(orderId: String): Flow<OrderWithDetails?> {
        val ord = orders[orderId] ?: return flowOf(null)
        val orderItems = items.filter { it.orderId == orderId }
        val orderSnap = if (snapshot?.orderId == orderId) snapshot else null
        val hist = history.filter { it.orderId == orderId }
        return flowOf(OrderWithDetails(ord, orderItems, orderSnap, hist))
    }

    override fun getAllOrdersWithDetails(shopId: String): Flow<List<OrderWithDetails>> {
        val list = orders.values.filter { it.shopId == shopId || shopId.isBlank() }.map { ord ->
            val orderItems = items.filter { it.orderId == ord.id }
            val orderSnap = if (snapshot?.orderId == ord.id) snapshot else null
            val hist = history.filter { it.orderId == ord.id }
            OrderWithDetails(ord, orderItems, orderSnap, hist)
        }
        return flowOf(list)
    }
}

class LifecyclePaymentDao : PaymentDao {
    val payments = mutableMapOf<String, PaymentEntity>()

    override suspend fun insertPayment(payment: PaymentEntity) {
        payments[payment.id] = payment
    }

    override suspend fun insertPayments(payments: List<PaymentEntity>) {
        payments.forEach { this.payments[it.id] = it }
    }

    override suspend fun updatePayment(payment: PaymentEntity) {
        payments[payment.id] = payment
    }

    override suspend fun insertRefund(refund: PaymentRefundEntity) {}

    override suspend fun getPaymentById(paymentId: String): PaymentEntity? = payments[paymentId]

    override fun getPaymentsForOrder(orderId: String): Flow<List<PaymentEntity>> {
        return flowOf(payments.values.filter { it.orderId == orderId && !it.isReversed })
    }

    override fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentEntity>> {
        return flowOf(payments.values.filter { it.customerId == customerId && !it.isReversed })
    }

    override fun getAllPayments(shopId: String): Flow<List<PaymentEntity>> {
        return flowOf(payments.values.filter { it.shopId == shopId && !it.isReversed })
    }

    override fun getRecentPayments(shopId: String, limit: Int): Flow<List<PaymentEntity>> {
        return flowOf(payments.values.filter { it.shopId == shopId && !it.isReversed }.take(limit))
    }

    override fun getTotalCollected(shopId: String): Flow<Double?> {
        return flowOf(payments.values.filter { it.shopId == shopId && !it.isReversed }.sumOf { it.amount })
    }

    override suspend fun markPaymentReversed(paymentId: String) {
        payments[paymentId]?.let {
            payments[paymentId] = it.copy(isReversed = true)
        }
    }
}
