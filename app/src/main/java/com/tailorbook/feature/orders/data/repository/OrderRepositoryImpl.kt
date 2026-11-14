package com.tailorbook.feature.orders.data.repository

import android.content.Context
import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.core.sync.local.SyncQueueDao
import com.tailorbook.core.sync.local.SyncQueueEntity
import com.tailorbook.core.sync.worker.TailorSyncWorker
import com.tailorbook.feature.orders.data.local.dao.OrderDao
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import com.tailorbook.feature.orders.data.local.entity.OrderMeasurementSnapshotEntity
import com.tailorbook.feature.orders.data.local.entity.OrderStatusHistoryEntity
import com.tailorbook.feature.orders.data.local.entity.OrderWithDetails
import com.tailorbook.feature.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

/**
 * Implementation of OrderRepository (TASK-08-01 / 02).
 * Backed by Room DAOs and Supabase Background Sync Queue for Subscribed & VIP shops.
 */
class OrderRepositoryImpl(
    private val orderDao: OrderDao,
    private val syncQueueDao: SyncQueueDao? = null,
    private val context: Context? = null,
    private val preferencesManager: UserPreferencesManager? = null
) : OrderRepository {

    override fun getAllOrders(shopId: String): Flow<List<OrderEntity>> {
        return orderDao.getAllOrders(shopId)
    }

    override fun getAllOrdersWithDetails(shopId: String): Flow<List<OrderWithDetails>> {
        return orderDao.getAllOrdersWithDetails(shopId)
    }

    override fun getOrdersByStatus(shopId: String, status: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByStatus(shopId, status)
    }

    override fun getOrdersForCustomer(customerId: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersForCustomer(customerId)
    }

    override fun getOrderWithDetails(orderId: String): Flow<OrderWithDetails?> {
        return orderDao.getOrderWithDetails(orderId)
    }

    override suspend fun createOrderWithSnapshot(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        snapshot: OrderMeasurementSnapshotEntity
    ) {
        orderDao.insertOrder(order)
        orderDao.insertOrderItems(items)
        orderDao.insertMeasurementSnapshot(snapshot)
        orderDao.insertStatusHistory(
            OrderStatusHistoryEntity(
                orderId = order.id,
                fromStatus = null,
                toStatus = order.status,
                notes = "Order created"
            )
        )
        enqueueOrderSync(order, SyncQueueEntity.OP_INSERT)
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        fromStatus: String,
        toStatus: String,
        notes: String?
    ) {
        val now = System.currentTimeMillis()
        orderDao.updateOrderStatus(orderId, toStatus, now)
        orderDao.insertStatusHistory(
            OrderStatusHistoryEntity(
                orderId = orderId,
                fromStatus = fromStatus,
                toStatus = toStatus,
                notes = notes,
                timestamp = now
            )
        )
        val updated = orderDao.getOrderById(orderId)
        if (updated != null) {
            enqueueOrderSync(updated, SyncQueueEntity.OP_UPDATE)
        }
    }

    override suspend fun cancelOrder(orderId: String, fromStatus: String, reason: String) {
        val now = System.currentTimeMillis()
        orderDao.cancelOrder(orderId, reason, now)
        orderDao.insertStatusHistory(
            OrderStatusHistoryEntity(
                orderId = orderId,
                fromStatus = fromStatus,
                toStatus = "CANCELLED",
                notes = "Cancelled: $reason",
                timestamp = now
            )
        )
        val updated = orderDao.getOrderById(orderId)
        if (updated != null) {
            enqueueOrderSync(updated, SyncQueueEntity.OP_UPDATE)
        }
    }

    override suspend fun rescheduleOrderDelivery(orderId: String, newDeliveryDate: Long, reason: String?) {
        val now = System.currentTimeMillis()
        orderDao.rescheduleOrder(orderId, newDeliveryDate, now)
        val order = orderDao.getOrderById(orderId)
        val currentStatus = order?.status ?: "NEW"
        orderDao.insertStatusHistory(
            OrderStatusHistoryEntity(
                orderId = orderId,
                fromStatus = currentStatus,
                toStatus = currentStatus,
                notes = if (reason != null) "Rescheduled delivery: $reason" else "Rescheduled delivery date",
                timestamp = now
            )
        )
        if (order != null) {
            enqueueOrderSync(order.copy(promisedDeliveryDate = newDeliveryDate, updatedAt = now), SyncQueueEntity.OP_UPDATE)
        }
    }

    override suspend fun generateOrderNumber(shopId: String): String {
        val count = orderDao.getOrderCount(shopId)
        return "TB-%03d".format(count + 101)
    }

    override suspend fun updateOrderItemMeasurements(
        orderId: String,
        itemId: String,
        measurements: Map<String, Double>
    ) {
        val jsonStr = measurements.entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) ->
            "\"$k\":$v"
        }
        orderDao.updateOrderItemMeasurements(itemId, jsonStr)
        orderDao.updateOrderSnapshotMeasurements(orderId, jsonStr)
    }

    private suspend fun enqueueOrderSync(order: OrderEntity, operation: String) {
        if (syncQueueDao == null) return
        val isEligible = preferencesManager?.isSyncEligible() ?: true
        if (!isEligible) return

        val mappedStatus = when (order.status.uppercase()) {
            "FINISHING" -> "STITCHING"
            "NEW", "CUTTING", "STITCHING", "READY", "DELIVERED", "CANCELLED" -> order.status.uppercase()
            else -> "NEW"
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

        val payload = buildJsonObject {
            put("id", order.id)
            put("shop_id", order.shopId)
            put("customer_id", order.customerId)
            put("order_number", order.orderNumber)
            put("status", mappedStatus)
            put("due_date", dueDateStr)
            put("total_amount", order.totalAmount)
            put("advance_paid", order.advanceAmount)
            put("balance_due", order.balanceAmount)
            order.cancellationReason?.let { put("notes", it) }
            put("created_at", createdAtStr)
            put("updated_at", updatedAtStr)
        }.toString()

        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_ORDER,
            entityId = order.id,
            operation = operation,
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
