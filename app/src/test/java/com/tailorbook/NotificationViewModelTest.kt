package com.tailorbook

import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.domain.repository.CustomerRepository
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import com.tailorbook.feature.orders.data.local.entity.OrderMeasurementSnapshotEntity
import com.tailorbook.feature.orders.data.local.entity.OrderWithDetails
import com.tailorbook.feature.orders.domain.repository.OrderRepository
import com.tailorbook.feature.notifications.presentation.NotificationType
import com.tailorbook.feature.notifications.presentation.NotificationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class NotificationViewModelTest {

    private lateinit var viewModel: NotificationViewModel
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val todayNoon = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val dayMs = 86400000L

    private val sampleCustomers = listOf(
        CustomerEntity(
            id = "c1",
            shopId = "shop-001",
            name = "Ananya Reddy",
            primaryPhone = "9876543210"
        ),
        CustomerEntity(
            id = "c2",
            shopId = "shop-001",
            name = "Lakshmi Devi",
            primaryPhone = "9848012345"
        )
    )

    private val sampleOrders = listOf(
        // Overdue order
        OrderEntity(
            id = "o1",
            shopId = "shop-001",
            orderNumber = "TB-100",
            customerId = "c1",
            orderDate = todayNoon - dayMs * 10,
            promisedDeliveryDate = todayNoon - dayMs * 2,
            status = "STITCHING",
            totalAmount = 1000.0,
            advanceAmount = 500.0,
            paidAmount = 500.0,
            balanceAmount = 500.0
        ),
        // Due today order
        OrderEntity(
            id = "o2",
            shopId = "shop-001",
            orderNumber = "TB-101",
            customerId = "c1",
            orderDate = todayNoon - dayMs * 3,
            promisedDeliveryDate = todayNoon,
            status = "READY",
            totalAmount = 1450.0,
            advanceAmount = 1000.0,
            paidAmount = 1000.0,
            balanceAmount = 450.0
        ),
        // Due tomorrow order
        OrderEntity(
            id = "o3",
            shopId = "shop-001",
            orderNumber = "TB-102",
            customerId = "c2",
            orderDate = todayNoon - dayMs * 2,
            promisedDeliveryDate = todayNoon + dayMs,
            status = "CUTTING",
            totalAmount = 800.0,
            advanceAmount = 400.0,
            paidAmount = 400.0,
            balanceAmount = 400.0
        )
    )

    private val fakeOrderRepository = object : OrderRepository {
        override fun getAllOrders(shopId: String): Flow<List<OrderEntity>> = flowOf(sampleOrders)
        override fun getOrdersByStatus(shopId: String, status: String): Flow<List<OrderEntity>> = flowOf(emptyList())
        override fun getOrdersForCustomer(customerId: String): Flow<List<OrderEntity>> = flowOf(emptyList())
        override fun getOrderWithDetails(orderId: String): Flow<OrderWithDetails?> = flowOf(null)
        override suspend fun createOrderWithSnapshot(order: OrderEntity, items: List<OrderItemEntity>, snapshot: OrderMeasurementSnapshotEntity) {}
        override suspend fun updateOrderStatus(orderId: String, fromStatus: String, toStatus: String, notes: String?) {}
        override suspend fun cancelOrder(orderId: String, fromStatus: String, reason: String) {}
        override suspend fun rescheduleOrderDelivery(orderId: String, newDeliveryDate: Long, reason: String?) {}
        override suspend fun generateOrderNumber(shopId: String): String = "TB-104"
    }

    private val fakeCustomerRepository = object : CustomerRepository {
        override fun getActiveCustomers(shopId: String): Flow<List<CustomerEntity>> = flowOf(sampleCustomers)
        override fun searchCustomers(query: String, shopId: String): Flow<List<CustomerEntity>> = flowOf(emptyList())
        override suspend fun getCustomerById(customerId: String): CustomerEntity? = sampleCustomers.find { it.id == customerId }
        override fun getCustomerWithFamilyMembers(customerId: String) = flowOf(null)
        override suspend fun saveCustomer(customer: CustomerEntity): Long = 1L
        override suspend fun updateCustomer(customer: CustomerEntity) {}
        override suspend fun archiveCustomer(customerId: String) {}
        override suspend fun addFamilyMember(member: com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity): Long = 1L
        override suspend fun updateFamilyMember(member: com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity) {}
        override suspend fun deleteFamilyMember(memberId: String) {}
        override fun getFamilyMembersForCustomer(customerId: String) = flowOf(emptyList<com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity>())
        override fun getAllCustomersWithFamilyMembers(shopId: String) = flowOf(sampleCustomers.map { com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers(it, emptyList()) })
    }

    @Before
    fun setUp() {
        viewModel = NotificationViewModel(
            orderRepository = fakeOrderRepository,
            customerRepository = fakeCustomerRepository,
            externalScope = testScope
        )
    }

    @Test
    fun generateNotifications_createsExpectedAlerts() {
        val notifs = viewModel.generateNotifications(
            orders = sampleOrders,
            customers = sampleCustomers,
            readIds = emptySet()
        )

        // Should have: 1 Overdue, 1 Due Today, 1 Due Tomorrow, 1 Payment Due (for o2 which is READY with balance 450)
        assertTrue(notifs.any { it.type == NotificationType.OVERDUE })
        assertTrue(notifs.any { it.type == NotificationType.DUE_TODAY })
        assertTrue(notifs.any { it.type == NotificationType.DUE_TOMORROW })
        assertTrue(notifs.any { it.type == NotificationType.PAYMENT_DUE })
    }

    @Test
    fun markAsRead_updatesNotificationStatus() {
        val notifId = "today-o2"
        viewModel.markAsRead(notifId)

        assertTrue(viewModel.readNotificationIds.value.contains(notifId))
    }
}
