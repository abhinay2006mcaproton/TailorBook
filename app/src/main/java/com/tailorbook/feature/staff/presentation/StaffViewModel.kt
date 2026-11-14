package com.tailorbook.feature.staff.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailorbook.feature.staff.data.local.entity.StaffEntity
import com.tailorbook.feature.staff.domain.repository.StaffRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing Boutique Staff Members, Roles, and Status (TASK-12-02 / 03).
 */
class StaffViewModel(
    val staffRepository: StaffRepository,
    val shopId: String = "shop-001",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _selectedRoleFilter = MutableStateFlow("ALL")
    val selectedRoleFilter: StateFlow<String> = _selectedRoleFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allStaff: StateFlow<List<StaffEntity>> = staffRepository.getAllStaff(shopId)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val filteredStaff: StateFlow<List<StaffEntity>> = combine(
        allStaff,
        _selectedRoleFilter,
        _searchQuery
    ) { staffList, roleFilter, query ->
        staffList.filter { member ->
            val matchesRole = if (roleFilter == "ALL") true else member.role == roleFilter
            val matchesQuery = if (query.isBlank()) true else {
                member.name.contains(query, ignoreCase = true) || member.phone.contains(query)
            }
            matchesRole && matchesQuery
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun selectRoleFilter(role: String) {
        _selectedRoleFilter.value = role
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addStaff(
        name: String,
        phone: String,
        role: String,
        notes: String? = null,
        onSuccess: () -> Unit
    ) {
        scope.launch {
            val staff = StaffEntity(
                id = UUID.randomUUID().toString(),
                shopId = shopId,
                name = name.trim(),
                phone = phone.trim(),
                role = role,
                isActive = true,
                joinedAt = System.currentTimeMillis(),
                notes = notes?.trim()?.ifBlank { null }
            )
            staffRepository.addStaff(staff)
            onSuccess()
        }
    }

    fun toggleStaffStatus(staffId: String, currentStatus: Boolean) {
        scope.launch {
            staffRepository.toggleStaffStatus(staffId, !currentStatus)
        }
    }

    fun deleteStaff(staffId: String) {
        scope.launch {
            staffRepository.deleteStaff(staffId)
        }
    }
}
