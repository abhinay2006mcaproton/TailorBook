package com.tailorbook.feature.customers.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Customer Room Entity (TASK-05-01 / REQ-CUST-01).
 * Represents a primary client of the tailoring boutique.
 */
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["shopId", "primaryPhone"]),
        Index(value = ["shopId", "name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val shopId: String = "default_shop",
    val name: String,
    val primaryPhone: String,
    val secondaryPhone: String? = null,
    val gender: String = "FEMALE",
    val address: String? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
