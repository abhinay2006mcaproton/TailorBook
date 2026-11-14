package com.tailorbook.feature.orders.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import com.tailorbook.feature.orders.data.local.entity.OrderMeasurementSnapshotEntity
import com.tailorbook.feature.orders.data.local.entity.OrderStatusHistoryEntity
import com.tailorbook.feature.orders.data.local.entity.OrderWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for Orders, Order Items, and Measurement Snapshots (TASK-08-01).
 */
@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurementSnapshot(snapshot: OrderMeasurementSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusHistory(history: OrderStatusHistoryEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :newStatus, updated_at = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, newStatus: String, updatedAt: Long)

    @Query("UPDATE orders SET status = 'CANCELLED', cancellation_reason = :reason, updated_at = :updatedAt WHERE id = :orderId")
    suspend fun cancelOrder(orderId: String, reason: String, updatedAt: Long)

    @Query("UPDATE orders SET promised_delivery_date = :newDeliveryDate, updated_at = :updatedAt WHERE id = :orderId")
    suspend fun rescheduleOrder(orderId: String, newDeliveryDate: Long, updatedAt: Long)

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE (:shopId = '' OR shop_id = :shopId OR shop_id = '' OR :shopId = 'shop-001' OR :shopId = 'default_shop' OR shop_id = 'shop-001' OR shop_id = 'default_shop' OR (SELECT COUNT(*) FROM orders WHERE shop_id = :shopId) = 0) ORDER BY order_date DESC")
    fun getAllOrders(shopId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE (:shopId = '' OR shop_id = :shopId OR shop_id = '' OR :shopId = 'shop-001' OR :shopId = 'default_shop' OR shop_id = 'shop-001' OR shop_id = 'default_shop' OR (SELECT COUNT(*) FROM orders WHERE shop_id = :shopId) = 0) AND status = :status ORDER BY promised_delivery_date ASC")
    fun getOrdersByStatus(shopId: String, status: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE customer_id = :customerId ORDER BY order_date DESC")
    fun getOrdersForCustomer(customerId: String): Flow<List<OrderEntity>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderWithDetails(orderId: String): Flow<OrderWithDetails?>

    @Transaction
    @Query("SELECT * FROM orders WHERE (:shopId = '' OR shop_id = :shopId OR shop_id = '' OR :shopId = 'shop-001' OR :shopId = 'default_shop' OR shop_id = 'shop-001' OR shop_id = 'default_shop' OR (SELECT COUNT(*) FROM orders WHERE shop_id = :shopId) = 0) ORDER BY order_date DESC")
    fun getAllOrdersWithDetails(shopId: String): Flow<List<OrderWithDetails>>

    @Query("SELECT COUNT(*) FROM orders WHERE (:shopId = '' OR shop_id = :shopId OR shop_id = '' OR :shopId = 'shop-001' OR :shopId = 'default_shop' OR shop_id = 'shop-001' OR shop_id = 'default_shop' OR (SELECT COUNT(*) FROM orders WHERE shop_id = :shopId) = 0)")
    suspend fun getOrderCount(shopId: String): Int

    @Query("UPDATE order_items SET measurements_json = :measurementsJson WHERE id = :itemId")
    suspend fun updateOrderItemMeasurements(itemId: String, measurementsJson: String)

    @Query("UPDATE order_measurement_snapshots SET frozen_measurements_json = :json WHERE order_id = :orderId")
    suspend fun updateOrderSnapshotMeasurements(orderId: String, json: String)
}
