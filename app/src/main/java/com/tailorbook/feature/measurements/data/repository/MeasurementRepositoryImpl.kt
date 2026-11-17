package com.tailorbook.feature.measurements.data.repository

import android.content.Context
import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.core.sync.local.SyncQueueDao
import com.tailorbook.core.sync.local.SyncQueueEntity
import com.tailorbook.core.sync.worker.TailorSyncWorker
import com.tailorbook.feature.measurements.data.local.dao.MeasurementDao
import com.tailorbook.feature.measurements.data.local.dao.MeasurementTemplateDao
import com.tailorbook.feature.measurements.data.local.entity.MeasurementTemplateEntity
import com.tailorbook.feature.measurements.data.local.entity.MeasurementVersionEntity
import com.tailorbook.feature.measurements.domain.model.GarmentCategory
import com.tailorbook.feature.measurements.domain.model.GarmentTemplate
import com.tailorbook.feature.measurements.domain.model.GarmentTemplates
import com.tailorbook.feature.measurements.domain.model.MeasurementField
import com.tailorbook.feature.measurements.domain.repository.MeasurementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

/**
 * Offline-first implementation of [MeasurementRepository].
 * Manages version incrementation, JSON persistence for dimension maps,
 * custom garment templates, and Supabase synchronization for Subscribed & VIP shops.
 */
class MeasurementRepositoryImpl(
    private val measurementDao: MeasurementDao,
    private val syncQueueDao: SyncQueueDao? = null,
    private val context: Context? = null,
    private val preferencesManager: UserPreferencesManager? = null,
    private val measurementTemplateDao: MeasurementTemplateDao? = null
) : MeasurementRepository {

    override fun getVersions(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Flow<List<MeasurementVersionEntity>> {
        return measurementDao.getVersions(customerId, familyMemberId, templateId)
    }

    override fun getLatestVersion(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Flow<MeasurementVersionEntity?> {
        return measurementDao.getLatestVersion(customerId, familyMemberId, templateId)
    }

    override suspend fun getNextVersionNumber(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Int {
        val currentMax = measurementDao.getLatestVersionNumber(customerId, familyMemberId, templateId) ?: 0
        return currentMax + 1
    }

    override suspend fun saveNewVersion(
        customerId: String,
        familyMemberId: String?,
        templateId: String,
        measurements: Map<String, Double>,
        unit: String,
        notes: String?
    ): Result<MeasurementVersionEntity> {
        return try {
            val nextVersion = getNextVersionNumber(customerId, familyMemberId, templateId)
            val jsonString = measurements.entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) ->
                "\"$k\":$v"
            }

            val entity = MeasurementVersionEntity(
                customerId = customerId,
                familyMemberId = familyMemberId,
                templateId = templateId,
                versionNumber = nextVersion,
                measurementsJson = jsonString,
                unit = unit,
                notes = notes?.trim()?.ifBlank { null }
            )

            measurementDao.insertVersion(entity)
            enqueueMeasurementSync(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllMeasurementsForCustomer(customerId: String): Flow<List<MeasurementVersionEntity>> {
        return measurementDao.getAllForCustomer(customerId)
    }

    private suspend fun enqueueMeasurementSync(entity: MeasurementVersionEntity) {
        if (preferencesManager != null && !preferencesManager.isSyncEligible()) {
            return
        }

        val shopId = preferencesManager?.userIdFlow?.firstOrNull()?.trim()
        if (shopId.isNullOrBlank() || shopId == "default_shop") return

        val measuredAtStr = try {
            Instant.ofEpochMilli(entity.measuredAt).toString()
        } catch (_: Exception) {
            Instant.now().toString()
        }

        val createdAtStr = try {
            Instant.ofEpochMilli(entity.createdAt).toString()
        } catch (_: Exception) {
            Instant.now().toString()
        }

        val measurementsJsonElement = try {
            Json.parseToJsonElement(entity.measurementsJson)
        } catch (_: Exception) {
            buildJsonObject {}
        }

        val payload = buildJsonObject {
            put("id", entity.id)
            put("shop_id", shopId)
            put("customer_id", entity.customerId)
            entity.familyMemberId?.let { put("family_member_id", it) }
            put("template_id", entity.templateId)
            put("version_number", entity.versionNumber)
            put("measurements_json", measurementsJsonElement)
            put("unit", if (entity.unit.uppercase() == "CM") "CM" else "INCH")
            entity.notes?.let { put("notes", it) }
            put("measured_at", measuredAtStr)
            put("created_at", createdAtStr)
        }.toString()

        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_MEASUREMENT,
            entityId = entity.id,
            operation = SyncQueueEntity.OP_INSERT,
            payloadJson = payload
        )
    }

    override fun getAllTemplates(): Flow<List<GarmentTemplate>> {
        val dao = measurementTemplateDao ?: return flowOf(GarmentTemplates.ALL_TEMPLATES)
        return dao.getAllTemplates().map { entities ->
            val customTemplates = entities.map { entity ->
                val category = try {
                    GarmentCategory.valueOf(entity.category.uppercase())
                } catch (_: Exception) {
                    GarmentCategory.WOMEN
                }
                val fields = deserializeFields(entity.defaultFieldsJson)
                GarmentTemplate(
                    id = entity.id,
                    category = category,
                    customNameEn = entity.nameEn,
                    customNameTe = entity.nameTe,
                    fields = fields,
                    isCustom = true,
                    defaultPrice = entity.defaultPrice
                )
            }
            // Register dynamic templates so GarmentTemplates.ALL_TEMPLATES reflects them globally
            GarmentTemplates.registerTemplates(customTemplates)
            GarmentTemplates.ALL_TEMPLATES
        }
    }

    override suspend fun saveCustomTemplate(template: GarmentTemplate): Result<Unit> {
        return runCatching {
            val fieldsJson = serializeFields(template.fields)
            val entity = MeasurementTemplateEntity(
                id = template.id,
                nameEn = template.getDisplayName(),
                nameTe = template.customNameTe,
                category = template.category.name,
                defaultFieldsJson = fieldsJson,
                isCustom = true,
                defaultPrice = template.defaultPrice
            )
            measurementTemplateDao?.insertTemplate(entity)
            enqueueTemplateSync(entity, template.fields)
        }
    }

    override suspend fun deleteCustomTemplate(templateId: String): Result<Unit> {
        return runCatching {
            measurementTemplateDao?.deleteTemplate(templateId)
            enqueueSync(
                entityType = SyncQueueEntity.ENTITY_MEASUREMENT_TEMPLATE,
                entityId = templateId,
                operation = SyncQueueEntity.OP_DELETE,
                payloadJson = "{}"
            )
        }
    }

    private suspend fun enqueueTemplateSync(entity: MeasurementTemplateEntity, fields: List<MeasurementField>) {
        if (preferencesManager != null && !preferencesManager.isSyncEligible()) {
            return
        }

        val fieldsArray = buildJsonArray {
            fields.forEach { f ->
                add(buildJsonObject {
                    put("key", f.key)
                    put("name_en", f.nameEn)
                    put("name_te", f.nameTe)
                    put("hint", f.hint)
                    put("default_val", f.defaultVal)
                    put("min_val", f.minVal)
                    put("max_val", f.maxVal)
                })
            }
        }

        val payload = buildJsonObject {
            put("id", entity.id)
            put("name_en", entity.nameEn)
            put("name_te", entity.nameTe ?: entity.nameEn)
            put("category", entity.category)
            put("default_fields_json", fieldsArray)
        }.toString()

        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_MEASUREMENT_TEMPLATE,
            entityId = entity.id,
            operation = SyncQueueEntity.OP_INSERT,
            payloadJson = payload
        )
    }

    private suspend fun enqueueSync(entityType: String, entityId: String, operation: String, payloadJson: String) {
        syncQueueDao?.let { dao ->
            runCatching {
                dao.enqueue(
                    SyncQueueEntity(
                        id = UUID.randomUUID().toString(),
                        entityType = entityType,
                        entityId = entityId,
                        operation = operation,
                        payloadJson = payloadJson
                    )
                )
                context?.let { ctx ->
                    TailorSyncWorker.triggerImmediateSync(ctx)
                }
            }
        }
    }

    companion object {
        fun parseMeasurements(jsonStr: String): Map<String, Double> {
            val clean = jsonStr.trim().removePrefix("{").removeSuffix("}").trim()
            if (clean.isBlank()) return emptyMap()
            val map = mutableMapOf<String, Double>()
            clean.split(",").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"").removeSurrounding("'")
                    val value = parts[1].trim().toDoubleOrNull() ?: 0.0
                    map[key] = value
                }
            }
            return map
        }

        fun serializeFields(fields: List<MeasurementField>): String {
            val array = buildJsonArray {
                fields.forEach { f ->
                    add(buildJsonObject {
                        put("key", f.key)
                        put("name_en", f.nameEn)
                        put("name_te", f.nameTe)
                        put("hint", f.hint)
                        put("default_val", f.defaultVal)
                        put("min_val", f.minVal)
                        put("max_val", f.maxVal)
                    })
                }
            }
            return array.toString()
        }

        fun deserializeFields(jsonStr: String): List<MeasurementField> {
            return runCatching {
                val element = Json.parseToJsonElement(jsonStr)
                if (element is kotlinx.serialization.json.JsonArray) {
                    element.mapNotNull { item ->
                        if (item is JsonObject) {
                            val key = item["key"]?.jsonPrimitive?.content ?: return@mapNotNull null
                            val nameEn = item["name_en"]?.jsonPrimitive?.content ?: key
                            val nameTe = item["name_te"]?.jsonPrimitive?.content ?: nameEn
                            val hint = item["hint"]?.jsonPrimitive?.content ?: ""
                            val defVal = item["default_val"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                            val minVal = item["min_val"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 2.0
                            val maxVal = item["max_val"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 70.0
                            MeasurementField(key, nameEn, nameTe, hint, defVal, minVal, maxVal)
                        } else null
                    }
                } else emptyList()
            }.getOrElse { emptyList() }
        }
    }
}
