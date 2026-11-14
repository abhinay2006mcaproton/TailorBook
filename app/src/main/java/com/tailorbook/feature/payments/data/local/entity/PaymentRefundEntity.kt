package com.tailorbook.feature.payments.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Payment Refund / Reversal Entity (TASK-10-01 / REQ-PAY-06).
 * Auditable reversal record ensuring financial integrity without silent deletion.
 */
@Entity(
    tableName = "payment_refunds",
    indices = [
        Index("payment_id"),
        Index("order_id")
    ]
)
data class PaymentRefundEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "payment_id")
    val paymentId: String,

    @ColumnInfo(name = "order_id")
    val orderId: String? = null,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "refund_method")
    val refundMethod: String = "CASH",

    @ColumnInfo(name = "refund_date")
    val refundDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
