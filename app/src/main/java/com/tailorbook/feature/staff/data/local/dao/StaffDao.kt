package com.tailorbook.feature.staff.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tailorbook.feature.staff.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Staff Management (TASK-12-01).
 */
@Dao
interface StaffDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Query("SELECT * FROM staff WHERE shop_id = :shopId ORDER BY is_active DESC, name ASC")
    fun getAllStaff(shopId: String = "shop-001"): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE shop_id = :shopId AND is_active = 1 ORDER BY name ASC")
    fun getActiveStaff(shopId: String = "shop-001"): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :staffId")
    suspend fun getStaffById(staffId: String): StaffEntity?

    @Query("UPDATE staff SET is_active = :isActive, updated_at = :updatedAt WHERE id = :staffId")
    suspend fun toggleStaffStatus(staffId: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM staff WHERE id = :staffId")
    suspend fun deleteStaff(staffId: String)
}
