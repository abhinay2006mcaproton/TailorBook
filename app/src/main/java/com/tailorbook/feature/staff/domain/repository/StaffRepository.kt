package com.tailorbook.feature.staff.domain.repository

import com.tailorbook.feature.staff.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for Staff Management (TASK-12-02 / REQ-STAF-01).
 */
interface StaffRepository {
    fun getAllStaff(shopId: String): Flow<List<StaffEntity>>
    fun getActiveStaff(shopId: String): Flow<List<StaffEntity>>
    suspend fun getStaffById(staffId: String): StaffEntity?
    suspend fun addStaff(staff: StaffEntity): Long
    suspend fun updateStaff(staff: StaffEntity)
    suspend fun toggleStaffStatus(staffId: String, isActive: Boolean)
    suspend fun deleteStaff(staffId: String)
}
