package com.tailorbook.feature.staff.data.repository

import com.tailorbook.feature.staff.data.local.dao.StaffDao
import com.tailorbook.feature.staff.data.local.entity.StaffEntity
import com.tailorbook.feature.staff.domain.repository.StaffRepository
import kotlinx.coroutines.flow.Flow

/**
 * Concrete implementation of [StaffRepository] backed by Room [StaffDao] (TASK-12-02).
 */
class StaffRepositoryImpl(
    private val staffDao: StaffDao
) : StaffRepository {

    override fun getAllStaff(shopId: String): Flow<List<StaffEntity>> {
        return staffDao.getAllStaff(shopId)
    }

    override fun getActiveStaff(shopId: String): Flow<List<StaffEntity>> {
        return staffDao.getActiveStaff(shopId)
    }

    override suspend fun getStaffById(staffId: String): StaffEntity? {
        return staffDao.getStaffById(staffId)
    }

    override suspend fun addStaff(staff: StaffEntity): Long {
        return staffDao.insertStaff(staff)
    }

    override suspend fun updateStaff(staff: StaffEntity) {
        staffDao.updateStaff(staff.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun toggleStaffStatus(staffId: String, isActive: Boolean) {
        staffDao.toggleStaffStatus(staffId, isActive)
    }

    override suspend fun deleteStaff(staffId: String) {
        staffDao.deleteStaff(staffId)
    }
}
