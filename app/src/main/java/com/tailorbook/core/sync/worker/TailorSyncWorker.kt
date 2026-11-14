package com.tailorbook.core.sync.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tailorbook.core.database.TailorBookDatabase
import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.core.network.SupabaseClient
import com.tailorbook.core.network.SupabaseClientImpl
import com.tailorbook.core.network.SupabaseConfig
import com.tailorbook.core.sync.local.SyncQueueEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager Sync Worker (TASK-14-03 / REQ-DATA-01).
 * Executes FIFO processing of offline mutation queue to Supabase PostgreSQL.
 * Uses CONNECTED network constraint and exponential backoff retry.
 */
class TailorSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // Can be injected / overridden in tests
    var supabaseClient: SupabaseClient = SupabaseClientImpl()

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesManager(applicationContext)
        if (!prefs.isSyncEligible()) {
            Timber.d("TailorSyncWorker: User is on Free tier (not VIP or Subscribed). Skipping cloud push.")
            return Result.success()
        }

        val database = TailorBookDatabase.getInstance(applicationContext)
        val syncDao = database.syncQueueDao()

        val pending = syncDao.getPendingEntries(limit = 50)
        if (pending.isEmpty()) {
            Timber.d("TailorSyncWorker: No pending sync items.")
            return Result.success()
        }

        // Guarantee tenant root shop exists in Supabase so foreign keys succeed
        ensureShopHeader(prefs)

        Timber.d("TailorSyncWorker: Processing ${pending.size} pending mutations...")
        var hasFailures = false

        for (item in pending) {
            syncDao.updateStatus(item.id, SyncQueueEntity.STATUS_SYNCING)
            val table = resolveTableName(item.entityType)

            val opResult: kotlin.Result<String> = if (table != null) {
                if (item.entityType == SyncQueueEntity.ENTITY_MEASUREMENT) {
                    runCatching {
                        val jsonObj = Json.parseToJsonElement(item.payloadJson).jsonObject
                        val templateId = jsonObj["template_id"]?.jsonPrimitive?.content
                        if (!templateId.isNullOrBlank()) {
                            ensureTemplate(templateId)
                        }
                    }
                }
                when (item.operation) {
                    SyncQueueEntity.OP_INSERT, SyncQueueEntity.OP_UPDATE -> {
                        supabaseClient.upsert(table, item.payloadJson)
                    }
                    SyncQueueEntity.OP_DELETE -> {
                        supabaseClient.delete(table, "id", item.entityId)
                    }
                    else -> kotlin.Result.failure(IllegalArgumentException("Unsupported op: ${item.operation}"))
                }
            } else {
                kotlin.Result.failure(IllegalArgumentException("Unknown entity type: ${item.entityType}"))
            }

            if (opResult.isSuccess) {
                syncDao.updateStatus(item.id, SyncQueueEntity.STATUS_SYNCED)
                Timber.d("Synced mutation successfully: ${item.entityType} (${item.entityId})")
            } else {
                val error = opResult.exceptionOrNull()
                Timber.e(error, "Sync failed for item ${item.id} (${item.entityType}): ${error?.message}")
                syncDao.markFailed(item.id, error?.message ?: "Unknown error")
                // Only trigger WorkManager retry for transient/network errors, not permanent 400 client syntax errors
                if (error?.message?.contains("HTTP 400") != true) {
                    hasFailures = true
                }
            }
        }

        syncDao.pruneCompletedOrExhausted()

        return if (hasFailures) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    private suspend fun ensureShopHeader(prefs: UserPreferencesManager) {
        val shopId = prefs.userIdFlow.firstOrNull() ?: SupabaseConfig.activeShopId
        if (!shopId.isNullOrBlank() && shopId != "default_shop") {
            val name = prefs.shopNameFlow.firstOrNull() ?: "My Boutique"
            val ownerName = prefs.ownerNameFlow.firstOrNull() ?: "Shop Owner"
            val ownerPhone = prefs.ownerPhoneFlow.firstOrNull() ?: "9999999999"
            val payload = buildJsonObject {
                put("id", shopId)
                put("name", name)
                put("owner_name", ownerName)
                put("owner_phone", ownerPhone)
                put("subscription_plan", "PRO_MONTHLY")
            }.toString()
            val res = supabaseClient.upsert("shops", payload)
            if (res.isFailure) {
                Timber.w("Failed to ensure shop header in Supabase: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    private suspend fun ensureTemplate(templateId: String) {
        if (templateId.isBlank()) return
        val payload = buildJsonObject {
            put("id", templateId)
            put("name_en", templateId.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
            put("name_te", templateId)
            put("category", "WOMEN")
            put("default_fields_json", kotlinx.serialization.json.JsonArray(emptyList()))
        }.toString()
        val res = supabaseClient.upsert("measurement_templates", payload)
        if (res.isFailure) {
            Timber.w("Failed to ensure template in Supabase: ${res.exceptionOrNull()?.message}")
        }
    }

    private fun resolveTableName(entityType: String): String? {
        return when (entityType) {
            SyncQueueEntity.ENTITY_SHOP -> "shops"
            SyncQueueEntity.ENTITY_CUSTOMER -> "customers"
            SyncQueueEntity.ENTITY_FAMILY_MEMBER -> "family_members"
            SyncQueueEntity.ENTITY_ORDER -> "orders"
            SyncQueueEntity.ENTITY_PAYMENT -> "payments"
            SyncQueueEntity.ENTITY_MEASUREMENT -> "measurement_versions"
            SyncQueueEntity.ENTITY_MEASUREMENT_TEMPLATE -> "measurement_templates"
            SyncQueueEntity.ENTITY_DESIGN -> "design_references"
            SyncQueueEntity.ENTITY_STAFF -> "shop_memberships"
            else -> null
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "tailorbook_periodic_sync"
        const val IMMEDIATE_WORK_NAME = "tailorbook_immediate_sync"

        /**
         * Schedules periodic sync every 15 minutes when connected to network.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<TailorSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Timber.i("Scheduled 15-minute periodic sync worker.")
        }

        /**
         * Triggers an immediate one-time sync when user saves changes or reconnects online.
         */
        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateRequest = OneTimeWorkRequestBuilder<TailorSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )
            Timber.i("Triggered immediate sync worker.")
        }
    }
}
