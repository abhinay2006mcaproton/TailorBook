package com.tailorbook.feature.orders.domain.repository

import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import com.tailorbook.feature.orders.data.local.entity.OrderMeasurementSnapshotEntity
import com.tailorbook.feature.orders.data.local.entity.OrderWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Order Management (TASK-08-01 / TASK-08-02).
 */
interface OrderRepository {
    fun getAllOrders(shopId: String): Flow<List<OrderEntity>>
    fun getAllOrdersWithDetails(shopId: String): Flow<List<OrderWithDetails>> = kotlinx.coroutines.flow.emptyFlow()
    fun getOrdersByStatus(shopId: String, status: String): Flow<List<OrderEntity>>
    fun getOrdersForCustomer(customerId: String): Flow<List<OrderEntity>>
    fun getOrderWithDetails(orderId: String): Flow<OrderWithDetails?>
    suspend fun createOrderWithSnapshot(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        snapshot: OrderMeasurementSnapshotEntity
    )
    suspend fun updateOrderStatus(
        orderId: String,
        fromStatus: String,
        toStatus: String,
        notes: String? = null
    )
    suspend fun cancelOrder(orderId: String, fromStatus: String, reason: String)
    suspend fun rescheduleOrderDelivery(orderId: String, newDeliveryDate: Long, reason: String? = null)
    suspend fun generateOrderNumber(shopId: String): String
    suspend fun updateOrderItemMeasurements(orderId: String, itemId: String, measurements: Map<String, Double>) {}
}
