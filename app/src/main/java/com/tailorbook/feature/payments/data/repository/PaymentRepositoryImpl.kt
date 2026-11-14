package com.tailorbook.feature.payments.data.repository

import android.content.Context
import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.core.sync.local.SyncQueueDao
import com.tailorbook.core.sync.local.SyncQueueEntity
import com.tailorbook.core.sync.worker.TailorSyncWorker
import com.tailorbook.feature.orders.data.local.dao.OrderDao
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.data.local.entity.OrderStatusHistoryEntity
import com.tailorbook.feature.payments.data.local.dao.PaymentDao
import com.tailorbook.feature.payments.data.local.entity.PaymentEntity
import com.tailorbook.feature.payments.data.local.entity.PaymentRefundEntity
import com.tailorbook.feature.payments.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

/**
 * Concrete implementation of PaymentRepository with atomic order balance recalculation
 * and Supabase synchronization for Subscribed & VIP shops (TASK-10-02 / REQ-PAY-01 / REQ-PAY-06).
 */
class PaymentRepositoryImpl(
    private val paymentDao: PaymentDao,
    private val orderDao: OrderDao? = null,
    private val syncQueueDao: SyncQueueDao? = null,
    private val context: Context? = null,
    private val preferencesManager: UserPreferencesManager? = null
) : PaymentRepository {

    override suspend fun recordPayment(payment: PaymentEntity): Result<PaymentEntity> {
        return try {
            paymentDao.insertPayment(payment)

            // If attached to an order, atomically update order balance
            val orderId = payment.orderId
            if (orderId != null && orderDao != null) {
                val order = orderDao.getOrderById(orderId)
                if (order != null) {
                    val newPaid = order.paidAmount + payment.amount
                    val newBalance = (order.totalAmount - newPaid).coerceAtLeast(0.0)
                    val now = System.currentTimeMillis()

                    val updatedOrder = order.copy(
                        paidAmount = newPaid,
                        balanceAmount = newBalance,
                        updatedAt = now
                    )
                    orderDao.updateOrder(updatedOrder)

                    // Audit log in status history
                    orderDao.insertStatusHistory(
                        OrderStatusHistoryEntity(
                            orderId = orderId,
                            fromStatus = order.status,
                            toStatus = order.status,
                            notes = "Payment of ₹${payment.amount.toInt()} received via ${payment.paymentMethod}",
                            timestamp = now
                        )
                    )

                    // Enqueue updated order balance for sync
                    enqueueOrderSync(updatedOrder)
                }
            }

            // Enqueue payment sync to Supabase
            enqueuePaymentSync(payment)

            Result.success(payment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refundPayment(refund: PaymentRefundEntity): Result<Unit> {
        return try {
            paymentDao.insertRefund(refund)
            paymentDao.markPaymentReversed(refund.paymentId)

            val orderId = refund.orderId
            if (orderId != null && orderDao != null) {
                val order = orderDao.getOrderById(orderId)
                if (order != null) {
                    val newPaid = (order.paidAmount - refund.amount).coerceAtLeast(0.0)
                    val newBalance = (order.totalAmount - newPaid).coerceAtLeast(0.0)
                    val now = System.currentTimeMillis()

                    val updatedOrder = order.copy(
                        paidAmount = newPaid,
                        balanceAmount = newBalance,
                        updatedAt = now
                    )
                    orderDao.updateOrder(updatedOrder)

                    orderDao.insertStatusHistory(
                        OrderStatusHistoryEntity(
                            orderId = orderId,
                            fromStatus = order.status,
                            toStatus = order.status,
                            notes = "Refund of ₹${refund.amount.toInt()} processed (${refund.reason})",
                            timestamp = now
                        )
                    )

                    // Enqueue updated order balance for sync
                    enqueueOrderSync(updatedOrder)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPaymentsForOrder(orderId: String): Flow<List<PaymentEntity>> {
        return paymentDao.getPaymentsForOrder(orderId)
    }

    override fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentEntity>> {
        return paymentDao.getPaymentsForCustomer(customerId)
    }

    override fun getAllPayments(shopId: String): Flow<List<PaymentEntity>> {
        return paymentDao.getAllPayments(shopId)
    }

    override fun getRecentPayments(shopId: String, limit: Int): Flow<List<PaymentEntity>> {
        return paymentDao.getRecentPayments(shopId, limit)
    }

    override fun getTotalCollected(shopId: String): Flow<Double> {
        return paymentDao.getTotalCollected(shopId).map { it ?: 0.0 }
    }

    private suspend fun enqueuePaymentSync(payment: PaymentEntity) {
        if (preferencesManager != null && !preferencesManager.isSyncEligible()) {
            return
        }

        val recordedAtStr = try {
            Instant.ofEpochMilli(payment.paymentDate).toString()
        } catch (_: Exception) {
            Instant.now().toString()
        }

        val createdAtStr = try {
            Instant.ofEpochMilli(payment.createdAt).toString()
        } catch (_: Exception) {
            Instant.now().toString()
        }

        val mappedMethod = when (payment.paymentMethod.uppercase()) {
            "CASH", "UPI", "CARD", "BANK_TRANSFER", "CHEQUE" -> payment.paymentMethod.uppercase()
            else -> "CASH"
        }

        val receiptNumber = payment.referenceNumber?.trim()?.ifBlank { null }
            ?: ("REC-" + payment.id.take(8).uppercase())

        val payload = buildJsonObject {
            put("id", payment.id)
            put("shop_id", payment.shopId)
            payment.orderId?.let { put("order_id", it) }
            put("customer_id", payment.customerId)
            put("receipt_number", receiptNumber)
            put("amount", payment.amount)
            put("payment_method", mappedMethod)
            payment.referenceNumber?.let { put("transaction_ref", it) }
            payment.notes?.let { put("notes", it) }
            put("recorded_at", recordedAtStr)
            put("created_at", createdAtStr)
        }.toString()

        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_PAYMENT,
            entityId = payment.id,
            operation = SyncQueueEntity.OP_INSERT,
            payloadJson = payload
        )
    }

    private suspend fun enqueueOrderSync(order: OrderEntity) {
        if (preferencesManager != null && !preferencesManager.isSyncEligible()) {
            return
        }

        val dueDateStr = try {
            Instant.ofEpochMilli(order.promisedDeliveryDate).toString()
        } catch (_: Exception) {
            Instant.now().toString()
        }

        val createdAtStr = try {
            Instant.ofEpochMilli(order.createdAt).toString()
        } catch (_: Exception) {
            Instant.now().toString()
        }

        val updatedAtStr = try {
            Instant.ofEpochMilli(order.updatedAt).toString()
        } catch (_: Exception) {
            Instant.now().toString()
        }

        val mappedStatus = when (order.status.uppercase()) {
            "NEW", "CUTTING", "STITCHING", "READY", "DELIVERED", "CANCELLED" -> order.status.uppercase()
            else -> "NEW"
        }

        val payload = buildJsonObject {
            put("id", order.id)
            put("shop_id", order.shopId)
            put("customer_id", order.customerId)
            put("order_number", order.orderNumber)
            put("status", mappedStatus)
            put("due_date", dueDateStr)
            put("total_amount", order.totalAmount)
            put("advance_paid", order.paidAmount)
            put("balance_due", order.balanceAmount)
            order.cancellationReason?.let { put("notes", it) }
            put("created_at", createdAtStr)
            put("updated_at", updatedAtStr)
        }.toString()

        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_ORDER,
            entityId = order.id,
            operation = SyncQueueEntity.OP_UPDATE,
            payloadJson = payload
        )
    }

    private suspend fun enqueueSync(entityType: String, entityId: String, operation: String, payloadJson: String) {
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
