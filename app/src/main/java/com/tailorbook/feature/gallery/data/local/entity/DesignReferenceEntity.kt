package com.tailorbook.feature.gallery.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Design Reference Entity (TASK-07-01 / REQ-GAL-01).
 * Represents design photos, embroidery patterns, and sketches linked to shops, customers, and orders.
 */
@Entity(
    tableName = "design_references",
    indices = [
        Index("shop_id"),
        Index("customer_id"),
        Index("order_id")
    ]
)
data class DesignReferenceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "customer_id")
    val customerId: String? = null,

    @ColumnInfo(name = "order_id")
    val orderId: String? = null,

    @ColumnInfo(name = "local_file_path")
    val localFilePath: String,

    @ColumnInfo(name = "remote_storage_url")
    val remoteStorageUrl: String? = null,

    @ColumnInfo(name = "tags")
    val tags: String = "", // Comma-delimited list: "Neck Design, Embroidery, Blouse"

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING", // PENDING, UPLOADED, FAILED

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
