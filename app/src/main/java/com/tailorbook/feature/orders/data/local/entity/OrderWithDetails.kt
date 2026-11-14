package com.tailorbook.feature.orders.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Composite model combining an order with its items, frozen measurements, and audit timeline.
 */
data class OrderWithDetails(
    @Embedded
    val order: OrderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "order_id"
    )
    val items: List<OrderItemEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "order_id"
    )
    val measurementSnapshot: OrderMeasurementSnapshotEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "order_id"
    )
    val statusHistory: List<OrderStatusHistoryEntity> = emptyList()
)
