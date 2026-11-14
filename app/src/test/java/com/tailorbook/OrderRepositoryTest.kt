package com.tailorbook

import com.tailorbook.feature.orders.data.local.dao.OrderDao
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import com.tailorbook.feature.orders.data.local.entity.OrderMeasurementSnapshotEntity
import com.tailorbook.feature.orders.data.local.entity.OrderStatusHistoryEntity
import com.tailorbook.feature.orders.data.local.entity.OrderWithDetails
import com.tailorbook.feature.orders.data.repository.OrderRepositoryImpl
import com.tailorbook.feature.orders.domain.repository.OrderRepository
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
 * Unit tests for OrderRepository & Immutable Measurement Snapshots (TASK-08-01 / 02).
 */
class OrderRepositoryTest {

    private lateinit var fakeDao: FakeOrderDao
    private lateinit var repository: OrderRepository

    @Before
    fun setUp() {
        fakeDao = FakeOrderDao()
        repository = OrderRepositoryImpl(fakeDao)
    }

    @Test
    fun createOrderWithSnapshot_freezesImmutableSnapshotAndAuditHistory() = runBlocking {
        val order = OrderEntity(
            id = "ord-001",
            shopId = "shop-1",
            orderNumber = "TB-101",
            customerId = "cust-1",
            promisedDeliveryDate = System.currentTimeMillis() + 86400000L * 5,
            status = "NEW",
            totalAmount = 1200.0,
            advanceAmount = 500.0,
            balanceAmount = 700.0
        )
        val items = listOf(
            OrderItemEntity(
                id = "item-001",
                orderId = "ord-001",
                garmentTemplateId = "BLOUSE",
                garmentName = "Silk Bridal Blouse",
                unitPrice = 1200.0
            )
        )
        val snapshot = OrderMeasurementSnapshotEntity(
            id = "snap-001",
            orderId = "ord-001",
            measurementVersionId = "ver-002",
            frozenMeasurementsJson = "\"length\": 14.5, \"chest\": 36.5",
            unit = "INCH"
        )

        repository.createOrderWithSnapshot(order, items, snapshot)

        val retrieved = repository.getOrderWithDetails("ord-001").first()
        assertNotNull(retrieved)
        assertEquals("TB-101", retrieved?.order?.orderNumber)
        assertEquals(1, retrieved?.items?.size)
        assertEquals("BLOUSE", retrieved?.items?.get(0)?.garmentTemplateId)
        assertEquals("\"length\": 14.5, \"chest\": 36.5", retrieved?.measurementSnapshot?.frozenMeasurementsJson)
        assertEquals("INCH", retrieved?.measurementSnapshot?.unit)

        // Verify status history audit
        val history = fakeDao.historyStorage.filter { it.orderId == "ord-001" }
        assertEquals(1, history.size)
        assertEquals("NEW", history[0].toStatus)
    }

    @Test
    fun updateOrderStatus_advancesStatusAndLogsHistory() = runBlocking {
        val order = OrderEntity(
            id = "ord-002",
            shopId = "shop-1",
            orderNumber = "TB-102",
            customerId = "cust-1",
            promisedDeliveryDate = System.currentTimeMillis(),
            status = "NEW",
            totalAmount = 800.0,
            balanceAmount = 800.0
        )
        fakeDao.insertOrder(order)

        repository.updateOrderStatus("ord-002", "NEW", "CUTTING", "Cloth fabric cut")

        val updated = fakeDao.orderStorage["ord-002"]
        assertEquals("CUTTING", updated?.status)

        val history = fakeDao.historyStorage.filter { it.orderId == "ord-002" }
        assertEquals(1, history.size)
        assertEquals("NEW", history[0].fromStatus)
        assertEquals("CUTTING", history[0].toStatus)
        assertEquals("Cloth fabric cut", history[0].notes)
    }

    @Test
    fun cancelOrder_recordsCancellationReason() = runBlocking {
        val order = OrderEntity(
            id = "ord-003",
            shopId = "shop-1",
            orderNumber = "TB-103",
            customerId = "cust-2",
            promisedDeliveryDate = System.currentTimeMillis(),
            status = "NEW",
            totalAmount = 900.0,
            balanceAmount = 900.0
        )
        fakeDao.insertOrder(order)

        repository.cancelOrder("ord-003", "NEW", "Customer requested cancellation")

        val updated = fakeDao.orderStorage["ord-003"]
        assertEquals("CANCELLED", updated?.status)
        assertEquals("Customer requested cancellation", updated?.cancellationReason)
    }

    @Test
    fun generateOrderNumber_incrementsSequentially() = runBlocking {
        fakeDao.orderStorage["1"] = OrderEntity(id = "1", shopId = "s1", orderNumber = "TB-101", customerId = "c1", promisedDeliveryDate = 0L, totalAmount = 0.0, balanceAmount = 0.0)
        fakeDao.orderStorage["2"] = OrderEntity(id = "2", shopId = "s1", orderNumber = "TB-102", customerId = "c1", promisedDeliveryDate = 0L, totalAmount = 0.0, balanceAmount = 0.0)

        val nextNum = repository.generateOrderNumber("s1")
        assertEquals("TB-103", nextNum)
    }

    @Test
    fun rescheduleOrderDelivery_updatesPromisedDateAndLogsAudit() = runBlocking {
        val originalDate = System.currentTimeMillis() + 86400000L * 2
        val newDate = System.currentTimeMillis() + 86400000L * 5
        val order = OrderEntity(
            id = "ord-reschedule",
            shopId = "s1",
            orderNumber = "TB-105",
            customerId = "c1",
            promisedDeliveryDate = originalDate,
            status = "STITCHING",
            totalAmount = 1500.0,
            balanceAmount = 500.0
        )
        fakeDao.insertOrder(order)

        repository.rescheduleOrderDelivery("ord-reschedule", newDate, "Client wedding postponed")

        val updated = fakeDao.orderStorage["ord-reschedule"]
        assertEquals(newDate, updated?.promisedDeliveryDate)

        val history = fakeDao.historyStorage.filter { it.orderId == "ord-reschedule" }
        assertEquals(1, history.size)
        assertTrue(history[0].notes?.contains("Client wedding postponed") == true)
    }
}

/**
 * In-memory test double for OrderDao.
 */
class FakeOrderDao : OrderDao {
    val orderStorage = mutableMapOf<String, OrderEntity>()
    val itemStorage = mutableMapOf<String, OrderItemEntity>()
    val snapshotStorage = mutableMapOf<String, OrderMeasurementSnapshotEntity>()
    val historyStorage = mutableListOf<OrderStatusHistoryEntity>()

    override suspend fun insertOrder(order: OrderEntity) {
        orderStorage[order.id] = order
    }

    override suspend fun insertOrders(orders: List<OrderEntity>) {
        orders.forEach { orderStorage[it.id] = it }
    }

    override suspend fun insertOrderItems(items: List<OrderItemEntity>) {
        items.forEach { itemStorage[it.id] = it }
    }

    override suspend fun insertMeasurementSnapshot(snapshot: OrderMeasurementSnapshotEntity) {
        snapshotStorage[snapshot.orderId] = snapshot
    }

    override suspend fun insertStatusHistory(history: OrderStatusHistoryEntity) {
        historyStorage.add(history)
    }

    override suspend fun updateOrder(order: OrderEntity) {
        orderStorage[order.id] = order
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: String, updatedAt: Long) {
        orderStorage[orderId]?.let {
            orderStorage[orderId] = it.copy(status = newStatus, updatedAt = updatedAt)
        }
    }

    override suspend fun cancelOrder(orderId: String, reason: String, updatedAt: Long) {
        orderStorage[orderId]?.let {
            orderStorage[orderId] = it.copy(status = "CANCELLED", cancellationReason = reason, updatedAt = updatedAt)
        }
    }

    override suspend fun rescheduleOrder(orderId: String, newDeliveryDate: Long, updatedAt: Long) {
        orderStorage[orderId]?.let {
            orderStorage[orderId] = it.copy(promisedDeliveryDate = newDeliveryDate, updatedAt = updatedAt)
        }
    }

    override suspend fun getOrderById(orderId: String): OrderEntity? {
        return orderStorage[orderId]
    }

    override fun getAllOrders(shopId: String): Flow<List<OrderEntity>> {
        return flowOf(orderStorage.values.filter { it.shopId == shopId }.sortedByDescending { it.orderDate })
    }

    override fun getOrdersByStatus(shopId: String, status: String): Flow<List<OrderEntity>> {
        return flowOf(orderStorage.values.filter { it.shopId == shopId && it.status == status }.sortedBy { it.promisedDeliveryDate })
    }

    override fun getOrdersForCustomer(customerId: String): Flow<List<OrderEntity>> {
        return flowOf(orderStorage.values.filter { it.customerId == customerId }.sortedByDescending { it.orderDate })
    }

    override fun getOrderWithDetails(orderId: String): Flow<OrderWithDetails?> {
        val o = orderStorage[orderId] ?: return flowOf(null)
        val items = itemStorage.values.filter { it.orderId == orderId }
        val snap = snapshotStorage[orderId]
        val hist = historyStorage.filter { it.orderId == orderId }
        return flowOf(OrderWithDetails(order = o, items = items, measurementSnapshot = snap, statusHistory = hist))
    }

    override fun getAllOrdersWithDetails(shopId: String): Flow<List<OrderWithDetails>> {
        val list = orderStorage.values
            .filter { it.shopId == shopId || shopId.isBlank() }
            .sortedByDescending { it.orderDate }
            .map { o ->
                val items = itemStorage.values.filter { it.orderId == o.id }
                val snap = snapshotStorage[o.id]
                val hist = historyStorage.filter { it.orderId == o.id }
                OrderWithDetails(order = o, items = items, measurementSnapshot = snap, statusHistory = hist)
            }
        return flowOf(list)
    }

    override suspend fun getOrderCount(shopId: String): Int {
        return orderStorage.values.count { it.shopId == shopId }
    }

    override suspend fun updateOrderItemMeasurements(itemId: String, measurementsJson: String) {
        itemStorage[itemId]?.let {
            itemStorage[itemId] = it.copy(measurementsJson = measurementsJson)
        }
    }

    override suspend fun updateOrderSnapshotMeasurements(orderId: String, json: String) {
        snapshotStorage[orderId]?.let {
            snapshotStorage[orderId] = it.copy(frozenMeasurementsJson = json)
        }
    }
}
