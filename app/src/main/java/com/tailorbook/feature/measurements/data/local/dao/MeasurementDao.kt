package com.tailorbook.feature.measurements.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tailorbook.feature.measurements.data.local.entity.MeasurementVersionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for versioned measurements (TASK-06-02).
 */
@Dao
interface MeasurementDao {

    @Query("""
        SELECT * FROM measurement_versions 
        WHERE customer_id = :customerId 
          AND (:familyMemberId IS NULL AND family_member_id IS NULL OR family_member_id = :familyMemberId)
          AND template_id = :templateId 
        ORDER BY version_number DESC
    """)
    fun getVersions(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Flow<List<MeasurementVersionEntity>>

    @Query("""
        SELECT * FROM measurement_versions 
        WHERE customer_id = :customerId 
          AND (:familyMemberId IS NULL AND family_member_id IS NULL OR family_member_id = :familyMemberId)
          AND template_id = :templateId 
        ORDER BY version_number DESC 
        LIMIT 1
    """)
    fun getLatestVersion(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Flow<MeasurementVersionEntity?>

    @Query("""
        SELECT MAX(version_number) FROM measurement_versions 
        WHERE customer_id = :customerId 
          AND (:familyMemberId IS NULL AND family_member_id IS NULL OR family_member_id = :familyMemberId)
          AND template_id = :templateId
    """)
    suspend fun getLatestVersionNumber(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Int?

    @Query("""
        SELECT * FROM measurement_versions 
        WHERE customer_id = :customerId 
        ORDER BY measured_at DESC
    """)
    fun getAllForCustomer(customerId: String): Flow<List<MeasurementVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVersion(version: MeasurementVersionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<MeasurementVersionEntity>)

    @Delete
    suspend fun deleteVersion(version: MeasurementVersionEntity)
}
