package com.tailorbook.feature.orders.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Audit log tracking each status transition for an order (TASK-08-01 / REQ-ORD-02).
 */
@Entity(
    tableName = "order_status_history",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("order_id"),
        Index("timestamp")
    ]
)
data class OrderStatusHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "order_id")
    val orderId: String,

    @ColumnInfo(name = "from_status")
    val fromStatus: String? = null,

    @ColumnInfo(name = "to_status")
    val toStatus: String, // NEW, CUTTING, STITCHING, FINISHING, READY, DELIVERED, CANCELLED

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "changed_by")
    val changedBy: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
