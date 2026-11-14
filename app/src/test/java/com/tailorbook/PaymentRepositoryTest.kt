package com.tailorbook

import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.payments.data.local.dao.PaymentDao
import com.tailorbook.feature.payments.data.local.entity.PaymentEntity
import com.tailorbook.feature.payments.data.local.entity.PaymentRefundEntity
import com.tailorbook.feature.payments.data.repository.PaymentRepositoryImpl
import com.tailorbook.feature.payments.domain.repository.PaymentRepository
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
 * Unit tests for PaymentRepository & atomic balance calculation (TASK-10-02 / REQ-PAY-01).
 */
class PaymentRepositoryTest {

    private lateinit var fakePaymentDao: FakePaymentDao
    private lateinit var fakeOrderDao: FakeOrderDao
    private lateinit var repository: PaymentRepository

    @Before
    fun setUp() {
        fakePaymentDao = FakePaymentDao()
        fakeOrderDao = FakeOrderDao()
        repository = PaymentRepositoryImpl(fakePaymentDao, fakeOrderDao)
    }

    @Test
    fun recordPayment_updatesOrderBalanceAndPaidAmountAtomically() = runBlocking {
        val order = OrderEntity(
            id = "ord-p1",
            shopId = "shop-1",
            orderNumber = "TB-201",
            customerId = "cust-1",
            promisedDeliveryDate = System.currentTimeMillis() + 86400000L,
            totalAmount = 2000.0,
            advanceAmount = 500.0,
            paidAmount = 500.0,
            balanceAmount = 1500.0
        )
        fakeOrderDao.insertOrder(order)

        val payment = PaymentEntity(
            id = "pay-1",
            shopId = "shop-1",
            orderId = "ord-p1",
            customerId = "cust-1",
            amount = 1000.0,
            paymentMethod = "UPI",
            referenceNumber = "UPI9876543210"
        )

        val result = repository.recordPayment(payment)
        assertTrue(result.isSuccess)

        val updatedOrder = fakeOrderDao.getOrderById("ord-p1")
        assertNotNull(updatedOrder)
        assertEquals(1500.0, updatedOrder?.paidAmount ?: 0.0, 0.01)
        assertEquals(500.0, updatedOrder?.balanceAmount ?: 0.0, 0.01)

        // Verify audit log
        val auditHistory = fakeOrderDao.historyStorage.filter { it.orderId == "ord-p1" }
        assertEquals(1, auditHistory.size)
        assertTrue(auditHistory[0].notes?.contains("Payment of ₹1000 received via UPI") == true)
    }

    @Test
    fun recordPayment_fullSettlementReducesBalanceToZero() = runBlocking {
        val order = OrderEntity(
            id = "ord-p2",
            shopId = "shop-1",
            orderNumber = "TB-202",
            customerId = "cust-1",
            promisedDeliveryDate = System.currentTimeMillis(),
            totalAmount = 800.0,
            advanceAmount = 300.0,
            paidAmount = 300.0,
            balanceAmount = 500.0
        )
        fakeOrderDao.insertOrder(order)

        val payment = PaymentEntity(
            id = "pay-2",
            shopId = "shop-1",
            orderId = "ord-p2",
            customerId = "cust-1",
            amount = 500.0,
            paymentMethod = "CASH"
        )

        repository.recordPayment(payment)

        val updatedOrder = fakeOrderDao.getOrderById("ord-p2")
        assertEquals(800.0, updatedOrder?.paidAmount ?: 0.0, 0.01)
        assertEquals(0.0, updatedOrder?.balanceAmount ?: 0.0, 0.01)
    }

    @Test
    fun refundPayment_reversesPaymentAndRestoresOrderBalance() = runBlocking {
        val order = OrderEntity(
            id = "ord-p3",
            shopId = "shop-1",
            orderNumber = "TB-203",
            customerId = "cust-1",
            promisedDeliveryDate = System.currentTimeMillis(),
            totalAmount = 1500.0,
            advanceAmount = 500.0,
            paidAmount = 1500.0,
            balanceAmount = 0.0
        )
        fakeOrderDao.insertOrder(order)

        val refund = PaymentRefundEntity(
            id = "ref-1",
            paymentId = "pay-orig",
            orderId = "ord-p3",
            amount = 500.0,
            reason = "Customer cancelled matching saree dupatta",
            refundMethod = "CASH"
        )

        repository.refundPayment(refund)

        val updatedOrder = fakeOrderDao.getOrderById("ord-p3")
        assertEquals(1000.0, updatedOrder?.paidAmount ?: 0.0, 0.01)
        assertEquals(500.0, updatedOrder?.balanceAmount ?: 0.0, 0.01)

        val auditHistory = fakeOrderDao.historyStorage.filter { it.orderId == "ord-p3" }
        assertEquals(1, auditHistory.size)
        assertTrue(auditHistory[0].notes?.contains("Refund of ₹500 processed") == true)
    }
}

/**
 * In-memory test double for PaymentDao.
 */
class FakePaymentDao : PaymentDao {
    val payments = mutableMapOf<String, PaymentEntity>()
    val refunds = mutableMapOf<String, PaymentRefundEntity>()

    override suspend fun insertPayment(payment: PaymentEntity) {
        payments[payment.id] = payment
    }

    override suspend fun insertPayments(payments: List<PaymentEntity>) {
        payments.forEach { this.payments[it.id] = it }
    }

    override suspend fun updatePayment(payment: PaymentEntity) {
        payments[payment.id] = payment
    }

    override suspend fun insertRefund(refund: PaymentRefundEntity) {
        refunds[refund.id] = refund
    }

    override suspend fun getPaymentById(paymentId: String): PaymentEntity? {
        return payments[paymentId]
    }

    override fun getPaymentsForOrder(orderId: String): Flow<List<PaymentEntity>> {
        return flowOf(payments.values.filter { it.orderId == orderId && !it.isReversed }.sortedByDescending { it.paymentDate })
    }

    override fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentEntity>> {
        return flowOf(payments.values.filter { it.customerId == customerId && !it.isReversed }.sortedByDescending { it.paymentDate })
    }

    override fun getAllPayments(shopId: String): Flow<List<PaymentEntity>> {
        return flowOf(payments.values.filter { it.shopId == shopId }.sortedByDescending { it.paymentDate })
    }

    override fun getRecentPayments(shopId: String, limit: Int): Flow<List<PaymentEntity>> {
        return flowOf(payments.values.filter { it.shopId == shopId && !it.isReversed }.sortedByDescending { it.paymentDate }.take(limit))
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
