package com.tailorbook.feature.payments.domain.repository

import com.tailorbook.feature.payments.data.local.entity.PaymentEntity
import com.tailorbook.feature.payments.data.local.entity.PaymentRefundEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for Payments, Receipts, and Reversals (TASK-10-02 / REQ-PAY-01).
 */
interface PaymentRepository {
    suspend fun recordPayment(payment: PaymentEntity): Result<PaymentEntity>
    suspend fun refundPayment(refund: PaymentRefundEntity): Result<Unit>
    fun getPaymentsForOrder(orderId: String): Flow<List<PaymentEntity>>
    fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentEntity>>
    fun getAllPayments(shopId: String): Flow<List<PaymentEntity>>
    fun getRecentPayments(shopId: String, limit: Int = 50): Flow<List<PaymentEntity>>
    fun getTotalCollected(shopId: String): Flow<Double>
}
