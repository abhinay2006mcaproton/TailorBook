package com.tailorbook.feature.measurements.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import java.util.UUID

/**
 * Immutable measurement version entity (TASK-06-02 / REQ-MEAS-04).
 * Historical versions are NEVER overwritten. Each edit generates a new version number (v1, v2, v3...).
 */
@Entity(
    tableName = "measurement_versions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["family_member_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["family_member_id"]),
        Index(value = ["customer_id", "family_member_id", "template_id", "version_number"], unique = true)
    ]
)
data class MeasurementVersionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "customer_id")
    val customerId: String,

    @ColumnInfo(name = "family_member_id")
    val familyMemberId: String? = null,

    @ColumnInfo(name = "template_id")
    val templateId: String, // 'BLOUSE', 'KURTI', etc.

    @ColumnInfo(name = "version_number")
    val versionNumber: Int,

    @ColumnInfo(name = "measurements_json")
    val measurementsJson: String, // JSON key-value map, e.g. {"length":14.5,"chest":36.0}

    @ColumnInfo(name = "unit")
    val unit: String = "INCH", // 'INCH' or 'CM'

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "measured_at")
    val measuredAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
