package com.tailorbook.feature.customers.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import com.tailorbook.feature.customers.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Customer Directory, Search, and Family Member operations (TASK-05-03 / 04).
 */
class CustomerViewModel(
    private val repository: CustomerRepository,
    initialList: List<CustomerWithFamilyMembers> = emptyList()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeShopId = MutableStateFlow("")
    val activeShopId: StateFlow<String> = _activeShopId.asStateFlow()

    private val _rawCustomers = MutableStateFlow<List<CustomerWithFamilyMembers>>(initialList)

    val filteredCustomers: StateFlow<List<CustomerWithFamilyMembers>> = combine(
        _rawCustomers,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val q = query.trim().lowercase()
            list.filter { item ->
                item.customer.name.lowercase().contains(q) ||
                        item.customer.primaryPhone.contains(q) ||
                        item.familyMembers.any { member -> member.name.lowercase().contains(q) }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initialList
    )

    init {
        loadCustomers()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setActiveShopId(shopId: String) {
        val trimmed = shopId.trim()
        if (_activeShopId.value != trimmed) {
            _activeShopId.value = trimmed
            loadCustomers(trimmed)
        }
    }

    fun loadCustomers(shopId: String = _activeShopId.value) {
        viewModelScope.launch {
            repository.getAllCustomersWithFamilyMembers(shopId).collect { customers ->
                _rawCustomers.value = customers
            }
        }
    }

    companion object {
        const val MAX_FAMILY_MEMBERS = 5

        fun initialCustomers() = listOf(
            CustomerWithFamilyMembers(
                customer = CustomerEntity(
                    id = "cust-101",
                    name = "Ananya Reddy",
                    primaryPhone = "9876543210",
                    address = "Banjara Hills, Road #12",
                    notes = "Prefers deep back neck designs and gold zari lining"
                ),
                familyMembers = listOf(
                    FamilyMemberEntity(
                        id = "fam-101",
                        customerId = "cust-101",
                        name = "Saanvi (Daughter)",
                        relationship = "Daughter"
                    )
                )
            ),
            CustomerWithFamilyMembers(
                customer = CustomerEntity(
                    id = "cust-102",
                    name = "Lakshmi Devi",
                    primaryPhone = "9848012345",
                    address = "Kukatpally Housing Board",
                    notes = "Cotton kurtis, comfort fit sleeve"
                ),
                familyMembers = listOf(
                    FamilyMemberEntity(
                        id = "fam-102",
                        customerId = "cust-102",
                        name = "Divya",
                        relationship = "Sister"
                    ),
                    FamilyMemberEntity(
                        id = "fam-103",
                        customerId = "cust-102",
                        name = "Nani",
                        relationship = "Son"
                    )
                )
            ),
            CustomerWithFamilyMembers(
                customer = CustomerEntity(
                    id = "cust-103",
                    name = "Sunitha Rao",
                    primaryPhone = "9123456789",
                    address = "Madhapur, Near Metro Station",
                    notes = "Bridal lehengas & heavy Maggam work blouses"
                ),
                familyMembers = emptyList()
            )
        )
    }

    fun addCustomer(
        name: String,
        phone: String,
        altPhone: String?,
        gender: String,
        address: String?,
        notes: String?,
        shopId: String? = null,
        onSuccess: (customerId: String) -> Unit
    ) {
        viewModelScope.launch {
            val targetShopId = (shopId ?: _activeShopId.value).ifBlank { "default_shop" }
            val newCustomer = CustomerEntity(
                shopId = targetShopId,
                name = name.trim(),
                primaryPhone = phone.trim(),
                secondaryPhone = altPhone?.trim()?.ifBlank { null },
                gender = gender,
                address = address?.trim()?.ifBlank { null },
                notes = notes?.trim()?.ifBlank { null }
            )

            repository.saveCustomer(newCustomer)
            val currentList = _rawCustomers.value.toMutableList()
            currentList.removeAll { it.customer.id == newCustomer.id }
            currentList.add(0, CustomerWithFamilyMembers(customer = newCustomer, familyMembers = emptyList()))
            _rawCustomers.value = currentList
            onSuccess(newCustomer.id)
        }
    }

    fun canAddFamilyMember(customerId: String): Boolean {
        val currentCount = _rawCustomers.value.find { it.customer.id == customerId }?.familyMembers?.size ?: 0
        return currentCount < MAX_FAMILY_MEMBERS
    }

    fun addFamilyMember(
        customerId: String,
        name: String,
        relationship: String,
        gender: String? = null,
        shopId: String? = null
    ): Boolean {
        val currentCount = _rawCustomers.value.find { it.customer.id == customerId }?.familyMembers?.size ?: 0
        if (currentCount >= MAX_FAMILY_MEMBERS) {
            return false
        }

        viewModelScope.launch {
            val targetShopId = (shopId ?: _activeShopId.value).ifBlank { "default_shop" }
            val newMember = FamilyMemberEntity(
                customerId = customerId,
                shopId = targetShopId,
                name = name.trim(),
                relationship = relationship,
                gender = gender
            )
            repository.addFamilyMember(newMember)

            val updated = _rawCustomers.value.map { item ->
                if (item.customer.id == customerId) {
                    item.copy(familyMembers = item.familyMembers + newMember)
                } else item
            }
            _rawCustomers.value = updated
        }
        return true
    }

    fun getCustomerById(customerId: String): CustomerWithFamilyMembers? {
        return _rawCustomers.value.find { it.customer.id == customerId }
    }

    fun updateCustomer(
        customerId: String,
        name: String,
        phone: String,
        altPhone: String?,
        gender: String,
        address: String?,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = _rawCustomers.value.find { it.customer.id == customerId }
            if (existing != null) {
                val updatedCust = existing.customer.copy(
                    name = name.trim(),
                    primaryPhone = phone.trim(),
                    secondaryPhone = altPhone?.trim()?.ifBlank { null },
                    gender = gender,
                    address = address?.trim()?.ifBlank { null },
                    notes = notes?.trim()?.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateCustomer(updatedCust)
                val updated = _rawCustomers.value.map { item ->
                    if (item.customer.id == customerId) {
                        item.copy(customer = updatedCust)
                    } else item
                }
                _rawCustomers.value = updated
            }
            onSuccess()
        }
    }

    fun archiveCustomer(customerId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.archiveCustomer(customerId)
            _rawCustomers.value = _rawCustomers.value.filter { it.customer.id != customerId }
            onSuccess()
        }
    }

    fun removeFamilyMember(customerId: String, memberId: String) {
        viewModelScope.launch {
            repository.deleteFamilyMember(memberId)
            val updated = _rawCustomers.value.map { item ->
                if (item.customer.id == customerId) {
                    item.copy(familyMembers = item.familyMembers.filter { it.id != memberId })
                } else item
            }
            _rawCustomers.value = updated
        }
    }
}
