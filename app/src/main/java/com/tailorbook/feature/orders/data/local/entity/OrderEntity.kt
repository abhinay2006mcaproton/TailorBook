package com.tailorbook.feature.orders.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Main Order entity (TASK-08-01 / REQ-ORD-01).
 * Master record tracking tailoring orders, status workflow, and balances.
 */
@Entity(
    tableName = "orders",
    indices = [
        Index("shop_id"),
        Index("customer_id"),
        Index("family_member_id"),
        Index("status"),
        Index("promised_delivery_date")
    ]
)
data class OrderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "order_number")
    val orderNumber: String, // e.g. TB-101

    @ColumnInfo(name = "customer_id")
    val customerId: String,

    @ColumnInfo(name = "family_member_id")
    val familyMemberId: String? = null,

    @ColumnInfo(name = "order_date")
    val orderDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "promised_delivery_date")
    val promisedDeliveryDate: Long,

    @ColumnInfo(name = "actual_delivery_date")
    val actualDeliveryDate: Long? = null,

    @ColumnInfo(name = "status")
    val status: String = "NEW", // NEW, CUTTING, STITCHING, FINISHING, READY, DELIVERED, CANCELLED

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double,

    @ColumnInfo(name = "advance_amount")
    val advanceAmount: Double = 0.0,

    @ColumnInfo(name = "discount_amount")
    val discountAmount: Double = 0.0,

    @ColumnInfo(name = "paid_amount")
    val paidAmount: Double = 0.0,

    @ColumnInfo(name = "balance_amount")
    val balanceAmount: Double,

    @ColumnInfo(name = "cancellation_reason")
    val cancellationReason: String? = null,

    @ColumnInfo(name = "assigned_staff_id")
    val assignedStaffId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
