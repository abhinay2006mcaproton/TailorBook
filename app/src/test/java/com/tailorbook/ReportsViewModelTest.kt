package com.tailorbook

import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import com.tailorbook.feature.orders.data.local.entity.OrderMeasurementSnapshotEntity
import com.tailorbook.feature.orders.data.local.entity.OrderWithDetails
import com.tailorbook.feature.orders.domain.repository.OrderRepository
import com.tailorbook.feature.payments.data.local.entity.PaymentEntity
import com.tailorbook.feature.payments.data.local.entity.PaymentRefundEntity
import com.tailorbook.feature.payments.domain.repository.PaymentRepository
import com.tailorbook.feature.reports.presentation.ReportDateRange
import com.tailorbook.feature.reports.presentation.ReportsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReportsViewModelTest {

    private lateinit var viewModel: ReportsViewModel
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val now = System.currentTimeMillis()
    private val hourMs = 3600000L

    private val sampleOrders = listOf(
        OrderEntity(
            id = "o1",
            shopId = "shop-001",
            orderNumber = "TB-101",
            customerId = "c1",
            orderDate = now - hourMs * 1,
            promisedDeliveryDate = now + hourMs * 10,
            actualDeliveryDate = now,
            status = "DELIVERED",
            totalAmount = 1450.0,
            advanceAmount = 1000.0,
            paidAmount = 1450.0,
            balanceAmount = 0.0
        ),
        OrderEntity(
            id = "o2",
            shopId = "shop-001",
            orderNumber = "TB-102",
            customerId = "c2",
            orderDate = now - hourMs * 2,
            promisedDeliveryDate = now + hourMs * 12,
            status = "STITCHING",
            totalAmount = 800.0,
            advanceAmount = 500.0,
            paidAmount = 500.0,
            balanceAmount = 300.0
        ),
        OrderEntity(
            id = "o3",
            shopId = "shop-001",
            orderNumber = "TB-103",
            customerId = "c3",
            orderDate = now - hourMs * 3,
            promisedDeliveryDate = now + hourMs * 14,
            status = "READY",
            totalAmount = 2400.0,
            advanceAmount = 1200.0,
            paidAmount = 1200.0,
            balanceAmount = 1200.0
        )
    )

    private val samplePayments = listOf(
        PaymentEntity(
            id = "p1",
            shopId = "shop-001",
            orderId = "o1",
            customerId = "c1",
            amount = 1000.0,
            paymentMethod = "CASH",
            paymentDate = now - hourMs * 3
        ),
        PaymentEntity(
            id = "p2",
            shopId = "shop-001",
            orderId = "o1",
            customerId = "c1",
            amount = 450.0,
            paymentMethod = "UPI",
            paymentDate = now - hourMs * 1
        ),
        PaymentEntity(
            id = "p3",
            shopId = "shop-001",
            orderId = "o2",
            customerId = "c2",
            amount = 500.0,
            paymentMethod = "UPI",
            paymentDate = now - hourMs * 2
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

    private val fakePaymentRepository = object : PaymentRepository {
        override suspend fun recordPayment(payment: PaymentEntity): Result<PaymentEntity> = Result.success(payment)
        override suspend fun refundPayment(refund: PaymentRefundEntity): Result<Unit> = Result.success(Unit)
        override fun getPaymentsForOrder(orderId: String): Flow<List<PaymentEntity>> = flowOf(emptyList())
        override fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentEntity>> = flowOf(emptyList())
        override fun getAllPayments(shopId: String): Flow<List<PaymentEntity>> = flowOf(samplePayments)
        override fun getRecentPayments(shopId: String, limit: Int): Flow<List<PaymentEntity>> = flowOf(samplePayments)
        override fun getTotalCollected(shopId: String): Flow<Double> = flowOf(1950.0)
    }

    @Before
    fun setUp() {
        viewModel = ReportsViewModel(
            orderRepository = fakeOrderRepository,
            paymentRepository = fakePaymentRepository,
            externalScope = testScope
        )
    }

    @Test
    fun computeSummary_calculatesCorrectTotals() {
        val summary = viewModel.computeSummary(
            range = ReportDateRange.THIS_MONTH,
            orders = sampleOrders,
            payments = samplePayments
        )

        // Booked total: 1450 + 800 + 2400 = 4650
        assertEquals(4650.0, summary.bookedValue, 0.01)

        // Collected total: 1000 + 450 + 500 = 1950
        assertEquals(1950.0, summary.cashCollected, 0.01)

        // Pending balance: 0 + 300 + 1200 = 1500
        assertEquals(1500.0, summary.pendingBalance, 0.01)

        // 3 orders
        assertEquals(3, summary.totalOrdersCount)
    }

    @Test
    fun computeSummary_calculatesPunctualityRate() {
        val summary = viewModel.computeSummary(
            range = ReportDateRange.THIS_MONTH,
            orders = sampleOrders,
            payments = samplePayments
        )

        // o1 was delivered on time (actualDeliveryDate <= promisedDeliveryDate)
        assertEquals(100, summary.punctualityRate)
    }

    @Test
    fun computeSummary_calculatesStatusBreakdown() {
        val summary = viewModel.computeSummary(
            range = ReportDateRange.THIS_MONTH,
            orders = sampleOrders,
            payments = samplePayments
        )

        assertEquals(1, summary.statusBreakdown["DELIVERED"])
        assertEquals(1, summary.statusBreakdown["IN_PROGRESS"])
        assertEquals(1, summary.statusBreakdown["READY"])
    }

    @Test
    fun buildShareableReportSummary_containsFormattedInformation() {
        val shareText = viewModel.buildShareableReportSummary("Sita Boutique")
        assertTrue(shareText.contains("TailorBook Business Summary"))
        assertTrue(shareText.contains("Sita Boutique"))
        assertTrue(shareText.contains("Booked Order Value"))
    }
}
