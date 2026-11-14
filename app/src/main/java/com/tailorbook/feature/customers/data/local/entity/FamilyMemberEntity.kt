package com.tailorbook.feature.customers.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Family Member Room Entity (TASK-05-01 / REQ-FAM-01).
 * Linked to a parent Customer; allows individual measurements and garment orders.
 */
@Entity(
    tableName = "family_members",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["shopId"])
    ]
)
data class FamilyMemberEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val shopId: String = "default_shop",
    val name: String,
    val relationship: String,
    val gender: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
