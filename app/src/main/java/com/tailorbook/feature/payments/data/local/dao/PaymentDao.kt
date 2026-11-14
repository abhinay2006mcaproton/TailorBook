package com.tailorbook.feature.payments.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tailorbook.feature.payments.data.local.entity.PaymentEntity
import com.tailorbook.feature.payments.data.local.entity.PaymentRefundEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for Payments and Refunds (TASK-10-01 / REQ-PAY-01).
 */
@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefund(refund: PaymentRefundEntity)

    @Query("SELECT * FROM payments WHERE id = :paymentId")
    suspend fun getPaymentById(paymentId: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE order_id = :orderId AND is_reversed = 0 ORDER BY payment_date DESC")
    fun getPaymentsForOrder(orderId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customer_id = :customerId AND is_reversed = 0 ORDER BY payment_date DESC")
    fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE (:shopId = '' OR shop_id = :shopId OR :shopId = 'shop-001' OR :shopId = 'default_shop' OR shop_id = 'shop-001' OR shop_id = 'default_shop') ORDER BY payment_date DESC")
    fun getAllPayments(shopId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE (:shopId = '' OR shop_id = :shopId OR :shopId = 'shop-001' OR :shopId = 'default_shop' OR shop_id = 'shop-001' OR shop_id = 'default_shop') AND is_reversed = 0 ORDER BY payment_date DESC LIMIT :limit")
    fun getRecentPayments(shopId: String, limit: Int = 50): Flow<List<PaymentEntity>>

    @Query("SELECT SUM(amount) FROM payments WHERE (:shopId = '' OR shop_id = :shopId OR :shopId = 'shop-001' OR :shopId = 'default_shop' OR shop_id = 'shop-001' OR shop_id = 'default_shop') AND is_reversed = 0")
    fun getTotalCollected(shopId: String): Flow<Double?>

    @Query("UPDATE payments SET is_reversed = 1 WHERE id = :paymentId")
    suspend fun markPaymentReversed(paymentId: String)
}
