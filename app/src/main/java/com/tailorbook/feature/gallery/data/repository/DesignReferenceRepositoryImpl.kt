package com.tailorbook.feature.gallery.data.repository

import android.content.Context
import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.core.sync.local.SyncQueueDao
import com.tailorbook.core.sync.local.SyncQueueEntity
import com.tailorbook.core.sync.worker.TailorSyncWorker
import com.tailorbook.feature.gallery.data.local.dao.DesignReferenceDao
import com.tailorbook.feature.gallery.data.local.entity.DesignReferenceEntity
import com.tailorbook.feature.gallery.domain.repository.DesignReferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

/**
 * Implementation of DesignReferenceRepository (TASK-07-01).
 * Backed by Room DAO and Supabase Cloud Sync for Subscribed & VIP shops.
 */
class DesignReferenceRepositoryImpl(
    private val dao: DesignReferenceDao,
    private val syncQueueDao: SyncQueueDao? = null,
    private val context: Context? = null,
    private val preferencesManager: UserPreferencesManager? = null
) : DesignReferenceRepository {

    override fun getAllDesigns(shopId: String): Flow<List<DesignReferenceEntity>> {
        return dao.getAllDesigns(shopId)
    }

    override fun getDesignsForCustomer(customerId: String): Flow<List<DesignReferenceEntity>> {
        return dao.getDesignsForCustomer(customerId)
    }

    override fun getDesignsForOrder(orderId: String): Flow<List<DesignReferenceEntity>> {
        return dao.getDesignsForOrder(orderId)
    }

    override fun searchByTag(shopId: String, query: String): Flow<List<DesignReferenceEntity>> {
        return dao.searchByTag(shopId, query)
    }

    override suspend fun getDesignById(id: String): DesignReferenceEntity? {
        return dao.getById(id)
    }

    override suspend fun saveDesign(design: DesignReferenceEntity) {
        dao.insert(design)
        enqueueDesignSync(design, SyncQueueEntity.OP_INSERT)
    }

    override suspend fun updateDesign(design: DesignReferenceEntity) {
        dao.update(design)
        enqueueDesignSync(design, SyncQueueEntity.OP_UPDATE)
    }

    override suspend fun deleteDesign(id: String) {
        dao.deleteById(id)
        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_DESIGN,
            entityId = id,
            operation = SyncQueueEntity.OP_DELETE,
            payloadJson = "{}"
        )
    }

    private suspend fun enqueueDesignSync(design: DesignReferenceEntity, operation: String) {
        if (preferencesManager != null && !preferencesManager.isSyncEligible()) {
            return
        }

        val createdAtStr = try {
            Instant.ofEpochMilli(design.createdAt).toString()
        } catch (_: Exception) {
            Instant.now().toString()
        }

        val title = design.notes.take(30).trim().ifBlank {
            design.tags.take(30).trim().ifBlank { "Design Reference" }
        }

        val imageUrl = design.remoteStorageUrl?.ifBlank { null }
            ?: design.localFilePath.ifBlank { "local_reference" }

        val payload = buildJsonObject {
            put("id", design.id)
            put("shop_id", design.shopId)
            design.customerId?.let { put("customer_id", it) }
            design.orderId?.let { put("order_id", it) }
            put("title", title)
            put("image_url", imageUrl)
            if (design.tags.isNotBlank()) put("tags", design.tags)
            if (design.notes.isNotBlank()) put("notes", design.notes)
            put("created_at", createdAtStr)
        }.toString()

        enqueueSync(
            entityType = SyncQueueEntity.ENTITY_DESIGN,
            entityId = design.id,
            operation = operation,
            payloadJson = payload
        )
    }

    private suspend fun enqueueSync(entityType: String, entityId: String, operation: String, payloadJson: String) {
        syncQueueDao?.let { queueDao ->
            runCatching {
                queueDao.enqueue(
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
}
