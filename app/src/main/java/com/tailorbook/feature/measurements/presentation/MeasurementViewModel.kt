package com.tailorbook.feature.measurements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailorbook.feature.measurements.data.local.entity.MeasurementVersionEntity
import com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl
import com.tailorbook.feature.measurements.domain.model.GarmentCategory
import com.tailorbook.feature.measurements.domain.model.GarmentTemplate
import com.tailorbook.feature.measurements.domain.model.GarmentTemplates
import com.tailorbook.feature.measurements.domain.repository.MeasurementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing Garment Templates, Version History, Measurement Entry, and Version Diffs (Phase 06).
 */
class MeasurementViewModel(
    private val repository: MeasurementRepository
) : ViewModel() {

    // ── Template Selection ─────────────────────────────────────────────────────
    private val _selectedCategory = MutableStateFlow<GarmentCategory?>(null)
    val selectedCategory: StateFlow<GarmentCategory?> = _selectedCategory.asStateFlow()

    val allTemplates: StateFlow<List<GarmentTemplate>> = repository.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GarmentTemplates.ALL_TEMPLATES
        )

    val filteredTemplates: StateFlow<List<GarmentTemplate>> = combine(
        allTemplates,
        _selectedCategory
    ) { templates, cat ->
        if (cat == null) {
            templates
        } else {
            templates.filter { it.category == cat }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GarmentTemplates.ALL_TEMPLATES
    )

    fun onCategoryFilterSelected(category: GarmentCategory?) {
        _selectedCategory.value = category
    }

    fun saveCustomTemplate(
        id: String? = null,
        nameEn: String,
        nameTe: String? = null,
        category: GarmentCategory,
        fields: List<com.tailorbook.feature.measurements.domain.model.MeasurementField>,
        defaultPrice: Double = 0.0,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val templateId = id?.ifBlank { null } ?: ("CUSTOM_" + nameEn.trim().uppercase().replace(Regex("[^A-Z0-9_]"), "_"))
            val template = GarmentTemplate(
                id = templateId,
                category = category,
                customNameEn = nameEn.trim(),
                customNameTe = nameTe?.trim()?.ifBlank { null },
                fields = fields,
                isCustom = true,
                defaultPrice = defaultPrice
            )
            repository.saveCustomTemplate(template)
            onSaved()
        }
    }

    fun deleteCustomTemplate(
        templateId: String,
        onDeleted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.deleteCustomTemplate(templateId)
            onDeleted()
        }
    }

    // ── Pre-seeded Measurement Versions for instant rich experience in debug only ────────────
    private val _inMemoryVersions = MutableStateFlow<List<MeasurementVersionEntity>>(
        if (com.tailorbook.BuildConfig.DEBUG) seedInitialVersions() else emptyList()
    )
    val inMemoryVersions: StateFlow<List<MeasurementVersionEntity>> = _inMemoryVersions.asStateFlow()

    fun getVersions(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): List<MeasurementVersionEntity> {
        return _inMemoryVersions.value.filter {
            it.customerId == customerId &&
            it.familyMemberId == familyMemberId &&
            it.templateId.equals(templateId, ignoreCase = true)
        }.sortedByDescending { it.versionNumber }
    }

    fun getLatestVersion(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): MeasurementVersionEntity? {
        return getVersions(customerId, familyMemberId, templateId).firstOrNull()
    }

    fun getNextVersionNumber(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Int {
        val existing = getVersions(customerId, familyMemberId, templateId)
        return (existing.maxOfOrNull { it.versionNumber } ?: 0) + 1
    }

    fun saveMeasurementVersion(
        customerId: String,
        familyMemberId: String?,
        templateId: String,
        values: Map<String, Double>,
        unit: String,
        notes: String?,
        onSaved: (MeasurementVersionEntity) -> Unit
    ) {
        viewModelScope.launch {
            val nextVersion = getNextVersionNumber(customerId, familyMemberId, templateId)
            val jsonStr = values.entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) ->
                "\"$k\":$v"
            }

            val entity = MeasurementVersionEntity(
                customerId = customerId,
                familyMemberId = familyMemberId,
                templateId = templateId,
                versionNumber = nextVersion,
                measurementsJson = jsonStr,
                unit = unit,
                notes = notes?.trim()?.ifBlank { null },
                measuredAt = System.currentTimeMillis()
            )

            // Persist to room repository
            repository.saveNewVersion(customerId, familyMemberId, templateId, values, unit, notes)

            // Update in-memory reactive flow
            val updated = _inMemoryVersions.value.toMutableList()
            updated.add(0, entity)
            _inMemoryVersions.value = updated

            onSaved(entity)
        }
    }

    companion object {
        fun seedInitialVersions(): List<MeasurementVersionEntity> = listOf(
            MeasurementVersionEntity(
                id = "meas-v1-blouse",
                customerId = "cust-101",
                familyMemberId = null,
                templateId = "BLOUSE",
                versionNumber = 1,
                measurementsJson = """{"length":14.0,"chest":36.0,"upper_chest":34.0,"waist":30.0,"shoulder":14.0,"front_neck":6.5,"back_neck":8.0,"armhole":16.0,"sleeve_length":10.0,"sleeve_round":12.0,"cross_front":13.0,"cross_back":14.0,"apex_length":10.0,"apex_width":7.0}""",
                unit = "INCH",
                notes = "Initial bridal measurement with standard seam allowance",
                measuredAt = System.currentTimeMillis() - 86400000L * 30
            ),
            MeasurementVersionEntity(
                id = "meas-v2-blouse",
                customerId = "cust-101",
                familyMemberId = null,
                templateId = "BLOUSE",
                versionNumber = 2,
                measurementsJson = """{"length":14.5,"chest":36.5,"upper_chest":34.0,"waist":30.5,"shoulder":14.0,"front_neck":6.5,"back_neck":8.5,"armhole":16.5,"sleeve_length":10.5,"sleeve_round":12.0,"cross_front":13.0,"cross_back":14.0,"apex_length":10.0,"apex_width":7.0}""",
                unit = "INCH",
                notes = "Updated back neck to 8.5 inches deep and lengthened sleeves",
                measuredAt = System.currentTimeMillis() - 86400000L * 5
            ),
            MeasurementVersionEntity(
                id = "meas-v1-kurti",
                customerId = "cust-102",
                familyMemberId = null,
                templateId = "KURTI",
                versionNumber = 1,
                measurementsJson = """{"length":42.0,"bust":38.0,"waist":34.0,"hip":40.0,"shoulder":15.0,"armhole":17.0,"sleeve_length":16.0,"sleeve_round":11.0,"front_neck":7.0,"back_neck":5.0,"slit_length":14.0}""",
                unit = "INCH",
                notes = "Daily wear comfort cut",
                measuredAt = System.currentTimeMillis() - 86400000L * 15
            )
        )
    }
}
