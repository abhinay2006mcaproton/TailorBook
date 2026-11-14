package com.tailorbook.feature.measurements.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for custom or edited garment templates (TASK-06-01 / REQ-MEAS-01).
 * Replicated to Supabase measurement_templates table for subscribed shops.
 */
@Entity(tableName = "measurement_templates")
data class MeasurementTemplateEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name_en")
    val nameEn: String,

    @ColumnInfo(name = "name_te")
    val nameTe: String? = null,

    @ColumnInfo(name = "category")
    val category: String = "WOMEN", // "WOMEN", "MEN", "UNISEX"

    @ColumnInfo(name = "default_fields_json")
    val defaultFieldsJson: String = "[]",

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
