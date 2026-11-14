package com.tailorbook.feature.orders.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Immutable measurement snapshot captured at the moment an order is booked (TASK-08-02 / DEC-001).
 * Freezes exact measurements into the order record so future customer edits never alter placed orders.
 */
@Entity(
    tableName = "order_measurement_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["order_id"], unique = true),
        Index("measurement_version_id")
    ]
)
data class OrderMeasurementSnapshotEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "order_id")
    val orderId: String,

    @ColumnInfo(name = "measurement_version_id")
    val measurementVersionId: String,

    @ColumnInfo(name = "frozen_measurements_json")
    val frozenMeasurementsJson: String, // Exact serialized map: "length": 14.5, "chest": 36.5

    @ColumnInfo(name = "unit")
    val unit: String = "INCH",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
