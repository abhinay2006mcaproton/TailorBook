package com.tailorbook.feature.gallery.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailorbook.feature.gallery.data.local.entity.DesignReferenceEntity
import com.tailorbook.feature.gallery.data.repository.ImageCompressor
import com.tailorbook.feature.gallery.domain.repository.DesignReferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Design Gallery & Photo Management (TASK-07-04 / TASK-07-05).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GalleryViewModel(
    private val repository: DesignReferenceRepository? = null,
    val initialShopId: String = "shop-001"
) : ViewModel() {

    private val _activeShopId = MutableStateFlow(initialShopId)
    val activeShopId: StateFlow<String> = _activeShopId

    val shopId: String get() = _activeShopId.value

    fun setActiveShopId(newShopId: String) {
        val trimmed = newShopId.trim()
        if (trimmed.isNotBlank() && _activeShopId.value != trimmed) {
            _activeShopId.value = trimmed
        }
    }

    private val _inMemoryDesigns = MutableStateFlow<List<DesignReferenceEntity>>(emptyList())
    val inMemoryDesigns: StateFlow<List<DesignReferenceEntity>> = _inMemoryDesigns

    private val _selectedTag = MutableStateFlow("All")
    val selectedTag: StateFlow<String> = _selectedTag

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        if (com.tailorbook.BuildConfig.DEBUG && repository == null) {
            seedSampleDesignsIfEmpty()
        } else if (repository != null) {
            viewModelScope.launch {
                _activeShopId.flatMapLatest { id ->
                    repository.getAllDesigns(id)
                }.collect { list ->
                    _inMemoryDesigns.value = list
                }
            }
        }
    }

    private fun seedSampleDesignsIfEmpty() {
        if (_inMemoryDesigns.value.isEmpty()) {
            val samples = listOf(
                DesignReferenceEntity(
                    id = "des-001",
                    shopId = shopId,
                    localFilePath = "sample_pot_neck",
                    tags = "Neck Design, Blouse, Bridal",
                    notes = "Deep pot neck with gold zari piping and handcrafted tassels. Suitable for silk sarees.",
                    createdAt = System.currentTimeMillis() - 86400000L * 3
                ),
                DesignReferenceEntity(
                    id = "des-002",
                    shopId = shopId,
                    localFilePath = "sample_maggam_peacock",
                    tags = "Maggam Work, Embroidery, Blouse",
                    notes = "Heavy peacock motif maggam work with Kundan stones and cut-work border on sleeves.",
                    createdAt = System.currentTimeMillis() - 86400000L * 2
                ),
                DesignReferenceEntity(
                    id = "des-003",
                    shopId = shopId,
                    localFilePath = "sample_puff_sleeve",
                    tags = "Sleeves, Blouse",
                    notes = "Vintage South-Indian puff sleeve with broad gold zari border cuff.",
                    createdAt = System.currentTimeMillis() - 86400000L * 1
                ),
                DesignReferenceEntity(
                    id = "des-004",
                    shopId = shopId,
                    localFilePath = "sample_boat_neck_kurti",
                    tags = "Neck Design, Kurti, Border / Lace",
                    notes = "Contemporary boat neck kurti with lace insets and keyhole back opening.",
                    createdAt = System.currentTimeMillis() - 3600000L * 5
                ),
                DesignReferenceEntity(
                    id = "des-005",
                    shopId = shopId,
                    localFilePath = "sample_bridal_lehenga",
                    tags = "Lehenga, Embroidery, Bridal",
                    notes = "Multi-panel bridal lehenga with heavy zardozi flare and scalloped waistband.",
                    createdAt = System.currentTimeMillis() - 3600000L * 2
                )
            )
            _inMemoryDesigns.value = samples
        }
    }

    val filteredDesigns: StateFlow<List<DesignReferenceEntity>> = combine(
        _inMemoryDesigns,
        _selectedTag,
        _searchQuery
    ) { designs, tag, query ->
        designs.filter { item ->
            val matchesTag = if (tag == "All") true else item.tags.contains(tag, ignoreCase = true)
            val matchesQuery = if (query.isBlank()) true else {
                item.tags.contains(query, ignoreCase = true) || item.notes.contains(query, ignoreCase = true)
            }
            matchesTag && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTag(tag: String) {
        _selectedTag.value = tag
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun getDesignById(id: String): DesignReferenceEntity? {
        return _inMemoryDesigns.value.find { it.id == id }
    }

    fun getDesignsForOrder(orderId: String): kotlinx.coroutines.flow.Flow<List<DesignReferenceEntity>> {
        return repository?.getDesignsForOrder(orderId)
            ?: kotlinx.coroutines.flow.flowOf(_inMemoryDesigns.value.filter { it.orderId == orderId })
    }

    fun attachPhotosToOrder(
        context: Context,
        uris: List<Uri>,
        orderId: String,
        customerId: String?,
        defaultTag: String = "Stitching Design",
        onComplete: (List<DesignReferenceEntity>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val created = mutableListOf<DesignReferenceEntity>()
            for (uri in uris) {
                val savedPath = try {
                    ImageCompressor.compressAndSave(context, uri)
                } catch (e: Exception) {
                    uri.toString()
                }

                val entity = DesignReferenceEntity(
                    id = UUID.randomUUID().toString(),
                    shopId = shopId,
                    customerId = customerId,
                    orderId = orderId,
                    localFilePath = savedPath,
                    tags = defaultTag,
                    notes = "Order reference photo",
                    createdAt = System.currentTimeMillis()
                )
                repository?.saveDesign(entity)
                created.add(entity)
            }
            _inMemoryDesigns.value = created + _inMemoryDesigns.value
            onComplete(created)
        }
    }

    fun savePhoto(
        context: Context,
        sourceUri: Uri,
        tags: List<String>,
        notes: String,
        customerId: String? = null,
        orderId: String? = null,
        onSuccess: (DesignReferenceEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val savedPath = try {
                ImageCompressor.compressAndSave(context, sourceUri)
            } catch (e: Exception) {
                sourceUri.toString()
            }

            val entity = DesignReferenceEntity(
                id = UUID.randomUUID().toString(),
                shopId = shopId,
                customerId = customerId,
                orderId = orderId,
                localFilePath = savedPath,
                tags = tags.joinToString(", "),
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            _inMemoryDesigns.value = listOf(entity) + _inMemoryDesigns.value
            repository?.saveDesign(entity)
            onSuccess(entity)
        }
    }

    fun updatePhoto(
        designId: String,
        tags: List<String>,
        notes: String,
        customerId: String? = null
    ) {
        viewModelScope.launch {
            val current = _inMemoryDesigns.value.find { it.id == designId } ?: return@launch
            val updated = current.copy(
                tags = tags.joinToString(", "),
                notes = notes,
                customerId = customerId
            )
            _inMemoryDesigns.value = _inMemoryDesigns.value.map { if (it.id == designId) updated else it }
            repository?.updateDesign(updated)
        }
    }

    fun deletePhoto(id: String) {
        viewModelScope.launch {
            _inMemoryDesigns.value = _inMemoryDesigns.value.filterNot { it.id == id }
            repository?.deleteDesign(id)
        }
    }
}
