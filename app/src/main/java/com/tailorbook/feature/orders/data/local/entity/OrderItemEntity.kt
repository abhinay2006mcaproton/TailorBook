package com.tailorbook.feature.orders.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Line item within an order (TASK-08-01).
 * Represents a stitched garment with price and instructions.
 */
@Entity(
    tableName = "order_items",
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
        Index("garment_template_id")
    ]
)
data class OrderItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "order_id")
    val orderId: String,

    @ColumnInfo(name = "garment_template_id")
    val garmentTemplateId: String, // e.g. BLOUSE, KURTI

    @ColumnInfo(name = "garment_name")
    val garmentName: String,

    @ColumnInfo(name = "quantity")
    val quantity: Int = 1,

    @ColumnInfo(name = "unit_price")
    val unitPrice: Double,

    @ColumnInfo(name = "stitching_instructions")
    val stitchingInstructions: String? = null,

    @ColumnInfo(name = "fabric_details")
    val fabricDetails: String? = null,

    @ColumnInfo(name = "measurements_json")
    val measurementsJson: String? = null,

    @ColumnInfo(name = "unit")
    val unit: String = "INCH",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
