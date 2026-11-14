package com.tailorbook.feature.payments.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Payment Entity (TASK-10-01 / REQ-PAY-01 / REQ-PAY-02).
 * Master immutable ledger record tracking customer financial transactions.
 */
@Entity(
    tableName = "payments",
    indices = [
        Index("shop_id"),
        Index("order_id"),
        Index("customer_id"),
        Index("payment_date")
    ]
)
data class PaymentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "order_id")
    val orderId: String? = null,

    @ColumnInfo(name = "customer_id")
    val customerId: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "CASH", // CASH, UPI, BANK_TRANSFER, CHEQUE, OTHER

    @ColumnInfo(name = "reference_number")
    val referenceNumber: String? = null,

    @ColumnInfo(name = "payment_date")
    val paymentDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "is_reversed")
    val isReversed: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
