package com.tailorbook.feature.measurements.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tailorbook.feature.measurements.data.local.entity.MeasurementTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for custom garment templates.
 */
@Dao
interface MeasurementTemplateDao {

    @Query("SELECT * FROM measurement_templates ORDER BY created_at ASC")
    fun getAllTemplates(): Flow<List<MeasurementTemplateEntity>>

    @Query("SELECT * FROM measurement_templates ORDER BY created_at ASC")
    suspend fun getAllTemplatesSync(): List<MeasurementTemplateEntity>

    @Query("SELECT * FROM measurement_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: String): MeasurementTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MeasurementTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<MeasurementTemplateEntity>)

    @Query("DELETE FROM measurement_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)
}
