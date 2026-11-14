package com.tailorbook

import com.tailorbook.feature.staff.data.local.entity.StaffEntity
import com.tailorbook.feature.staff.domain.repository.StaffRepository
import com.tailorbook.feature.staff.presentation.StaffViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StaffViewModelTest {

    private lateinit var viewModel: StaffViewModel
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val sampleStaff = listOf(
        StaffEntity(
            id = "s1",
            shopId = "shop-001",
            name = "Ramesh Kumar",
            phone = "9876500001",
            role = "OWNER",
            isActive = true
        ),
        StaffEntity(
            id = "s2",
            shopId = "shop-001",
            name = "Suresh Master",
            phone = "9876500002",
            role = "TAILOR",
            isActive = true
        ),
        StaffEntity(
            id = "s3",
            shopId = "shop-001",
            name = "Venkatesh",
            phone = "9876500003",
            role = "WORKER",
            isActive = false
        )
    )

    private val staffList = mutableListOf<StaffEntity>().apply { addAll(sampleStaff) }

    private val fakeStaffRepository = object : StaffRepository {
        override fun getAllStaff(shopId: String): Flow<List<StaffEntity>> = flowOf(staffList)
        override fun getActiveStaff(shopId: String): Flow<List<StaffEntity>> = flowOf(staffList.filter { it.isActive })
        override suspend fun getStaffById(staffId: String): StaffEntity? = staffList.find { it.id == staffId }
        override suspend fun addStaff(staff: StaffEntity): Long {
            staffList.add(staff)
            return 1L
        }
        override suspend fun updateStaff(staff: StaffEntity) {}
        override suspend fun toggleStaffStatus(staffId: String, isActive: Boolean) {
            val idx = staffList.indexOfFirst { it.id == staffId }
            if (idx != -1) {
                staffList[idx] = staffList[idx].copy(isActive = isActive)
            }
        }
        override suspend fun deleteStaff(staffId: String) {
            staffList.removeAll { it.id == staffId }
        }
    }

    @Before
    fun setUp() {
        viewModel = StaffViewModel(
            staffRepository = fakeStaffRepository,
            externalScope = testScope
        )
    }

    @Test
    fun allStaff_initializesWithCorrectCount() {
        assertEquals(3, viewModel.allStaff.value.size)
    }

    @Test
    fun addStaff_persistsMember() = runBlocking {
        var callbackCalled = false
        viewModel.addStaff(
            name = "Kavitha",
            phone = "9876500004",
            role = "MANAGER",
            notes = "Handles customer reception"
        ) {
            callbackCalled = true
        }

        assertTrue(callbackCalled)
        assertTrue(staffList.any { it.name == "Kavitha" && it.role == "MANAGER" })
    }

    @Test
    fun toggleStaffStatus_updatesActiveFlag() = runBlocking {
        viewModel.toggleStaffStatus("s3", false)
        val updated = fakeStaffRepository.getStaffById("s3")
        assertTrue(updated?.isActive == true)
    }
}
