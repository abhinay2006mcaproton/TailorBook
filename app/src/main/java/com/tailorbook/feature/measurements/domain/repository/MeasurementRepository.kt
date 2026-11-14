package com.tailorbook.feature.measurements.domain.repository

import com.tailorbook.feature.measurements.data.local.entity.MeasurementVersionEntity
import com.tailorbook.feature.measurements.domain.model.GarmentTemplate
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for versioned measurements (TASK-06-02).
 */
interface MeasurementRepository {

    fun getVersions(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Flow<List<MeasurementVersionEntity>>

    fun getLatestVersion(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Flow<MeasurementVersionEntity?>

    suspend fun getNextVersionNumber(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Int

    suspend fun saveNewVersion(
        customerId: String,
        familyMemberId: String?,
        templateId: String,
        measurements: Map<String, Double>,
        unit: String,
        notes: String?
    ): Result<MeasurementVersionEntity>

    fun getAllMeasurementsForCustomer(customerId: String): Flow<List<MeasurementVersionEntity>>

    fun getAllTemplates(): Flow<List<GarmentTemplate>>

    suspend fun saveCustomTemplate(template: GarmentTemplate): Result<Unit>

    suspend fun deleteCustomTemplate(templateId: String): Result<Unit>
}
