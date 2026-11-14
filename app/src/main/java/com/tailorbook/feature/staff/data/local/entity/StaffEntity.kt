package com.tailorbook.feature.staff.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Staff entity representing a boutique team member (TASK-12-01 / REQ-STAF-01).
 * Supported roles: OWNER, MANAGER, TAILOR, WORKER.
 */
@Entity(
    tableName = "staff",
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["shop_id", "phone"]),
        Index(value = ["role"])
    ]
)
data class StaffEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String = "shop-001",

    val name: String,

    val phone: String,

    val role: String, // OWNER, MANAGER, TAILOR, WORKER

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "joined_at")
    val joinedAt: Long = System.currentTimeMillis(),

    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
