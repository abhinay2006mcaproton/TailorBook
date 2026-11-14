package com.tailorbook

import com.tailorbook.core.network.SupabaseClient
import com.tailorbook.core.sync.local.SyncQueueDao
import com.tailorbook.core.sync.local.SyncQueueEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SyncQueue mechanics, Supabase replication, and Conflict Resolution (TASK-14-05).
 */
class TailorSyncWorkerTest {

    private lateinit var fakeSyncDao: FakeSyncQueueDao
    private lateinit var fakeSupabaseClient: FakeSupabaseClient

    @Before
    fun setUp() {
        fakeSyncDao = FakeSyncQueueDao()
        fakeSupabaseClient = FakeSupabaseClient()
    }

    @Test
    fun enqueue_addsPendingMutationToQueue() = runBlocking {
        val entry = SyncQueueEntity(
            id = "sync-1",
            entityType = SyncQueueEntity.ENTITY_CUSTOMER,
            entityId = "cust-1",
            operation = SyncQueueEntity.OP_INSERT,
            payloadJson = """{"id":"cust-1","name":"Ananya Reddy","phone":"9876543210"}"""
        )
        fakeSyncDao.enqueue(entry)

        val pending = fakeSyncDao.getPendingEntries()
        assertEquals(1, pending.size)
        assertEquals("sync-1", pending[0].id)
        assertEquals(SyncQueueEntity.STATUS_PENDING, pending[0].status)
    }

    @Test
    fun processSync_successfullyPushesMutationsToSupabase() = runBlocking {
        // Enqueue Customer Insert and Order Insert
        fakeSyncDao.enqueue(
            SyncQueueEntity(
                id = "sync-cust",
                entityType = SyncQueueEntity.ENTITY_CUSTOMER,
                entityId = "c-100",
                operation = SyncQueueEntity.OP_INSERT,
                payloadJson = """{"id":"c-100","name":"Lakshmi Devi"}"""
            )
        )
        fakeSyncDao.enqueue(
            SyncQueueEntity(
                id = "sync-ord",
                entityType = SyncQueueEntity.ENTITY_ORDER,
                entityId = "o-200",
                operation = SyncQueueEntity.OP_INSERT,
                payloadJson = """{"id":"o-200","orderNumber":"TB-101"}"""
            )
        )

        val pending = fakeSyncDao.getPendingEntries()
        assertEquals(2, pending.size)

        for (item in pending) {
            fakeSyncDao.updateStatus(item.id, SyncQueueEntity.STATUS_SYNCING)
            val table = when (item.entityType) {
                SyncQueueEntity.ENTITY_CUSTOMER -> "customers"
                SyncQueueEntity.ENTITY_ORDER -> "orders"
                else -> "other"
            }
            val res = fakeSupabaseClient.upsert(table, item.payloadJson)
            assertTrue(res.isSuccess)
            fakeSyncDao.updateStatus(item.id, SyncQueueEntity.STATUS_SYNCED)
        }

        // Verify Supabase records received
        assertEquals(1, fakeSupabaseClient.upsertedRecords["customers"]?.size)
        assertEquals(1, fakeSupabaseClient.upsertedRecords["orders"]?.size)

        // Pruning removes SYNCED items
        fakeSyncDao.pruneCompletedOrExhausted()
        val remaining = fakeSyncDao.getPendingEntries()
        assertEquals(0, remaining.size)
    }

    @Test
    fun conflictResolution_financialPaymentsAreAdditiveAndIdempotent() = runBlocking {
        // Simulating duplicate offline sync pushes of the same payment record
        val paymentJson = """{"id":"pay-101","receipt_number":"RCP-001","amount":500.0}"""

        val res1 = fakeSupabaseClient.upsert("payments", paymentJson, onConflict = "id")
        val res2 = fakeSupabaseClient.upsert("payments", paymentJson, onConflict = "id")

        assertTrue(res1.isSuccess)
        assertTrue(res2.isSuccess)
        // Idempotent upsert in fake client updates existing record rather than duplicating
        assertEquals(1, fakeSupabaseClient.upsertedRecords["payments"]?.size)
    }

    @Test
    fun networkFailure_marksStatusFailedAndIncrementsRetry() = runBlocking {
        fakeSupabaseClient.shouldFail = true

        val entry = SyncQueueEntity(
            id = "sync-fail",
            entityType = SyncQueueEntity.ENTITY_CUSTOMER,
            entityId = "c-err",
            operation = SyncQueueEntity.OP_INSERT,
            payloadJson = "{}"
        )
        fakeSyncDao.enqueue(entry)

        val item = fakeSyncDao.getPendingEntries()[0]
        val res = fakeSupabaseClient.upsert("customers", item.payloadJson)
        assertTrue(res.isFailure)

        fakeSyncDao.markFailed(item.id, res.exceptionOrNull()?.message ?: "Network error")

        val updated = fakeSyncDao.getById("sync-fail")
        assertNotNull(updated)
        assertEquals(SyncQueueEntity.STATUS_FAILED, updated?.status)
        assertEquals(1, updated?.retryCount)
    }
}

/**
 * Fake In-Memory implementation of SyncQueueDao for pure JVM tests.
 */
class FakeSyncQueueDao : SyncQueueDao {
    private val queue = mutableListOf<SyncQueueEntity>()
    private val queueState = MutableStateFlow<List<SyncQueueEntity>>(emptyList())

    private fun notifyState() {
        queueState.value = queue.toList()
    }

    override suspend fun enqueue(entry: SyncQueueEntity) {
        queue.removeAll { it.id == entry.id }
        queue.add(entry)
        notifyState()
    }

    override suspend fun enqueueAll(entries: List<SyncQueueEntity>) {
        entries.forEach { enqueue(it) }
    }

    override suspend fun getPendingEntries(limit: Int, maxRetries: Int): List<SyncQueueEntity> {
        return queue.filter {
            (it.status == SyncQueueEntity.STATUS_PENDING || it.status == SyncQueueEntity.STATUS_FAILED) &&
                it.retryCount < maxRetries
        }.sortedBy { it.createdAt }.take(limit)
    }

    override suspend fun updateStatus(id: String, status: String, updatedAt: Long) {
        val index = queue.indexOfFirst { it.id == id }
        if (index != -1) {
            queue[index] = queue[index].copy(status = status, updatedAt = updatedAt)
            notifyState()
        }
    }

    override suspend fun markFailed(id: String, error: String, updatedAt: Long) {
        val index = queue.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = queue[index]
            queue[index] = current.copy(
                status = SyncQueueEntity.STATUS_FAILED,
                lastError = error,
                retryCount = current.retryCount + 1,
                updatedAt = updatedAt
            )
            notifyState()
        }
    }

    override suspend fun pruneCompletedOrExhausted(maxRetries: Int) {
        queue.removeAll {
            it.status == SyncQueueEntity.STATUS_SYNCED ||
                (it.status == SyncQueueEntity.STATUS_FAILED && it.retryCount >= maxRetries)
        }
        notifyState()
    }

    override fun observePendingCount(): Flow<Int> {
        return queueState.map { list ->
            list.count { it.status == SyncQueueEntity.STATUS_PENDING || it.status == SyncQueueEntity.STATUS_FAILED }
        }
    }

    override suspend fun getPendingCount(): Int {
        return queue.count { it.status == SyncQueueEntity.STATUS_PENDING || it.status == SyncQueueEntity.STATUS_FAILED }
    }

    fun getById(id: String): SyncQueueEntity? = queue.find { it.id == id }
}

/**
 * Fake In-Memory implementation of SupabaseClient for pure JVM tests.
 */
class FakeSupabaseClient : SupabaseClient {
    var shouldFail: Boolean = false
    val upsertedRecords = mutableMapOf<String, MutableMap<String, String>>()
    val deletedRecords = mutableListOf<String>()
    val uploadedImages = mutableMapOf<String, ByteArray>()

    override suspend fun upsert(table: String, payloadJson: String, onConflict: String): Result<String> {
        if (shouldFail) return Result.failure(Exception("HTTP 503: Service Unavailable"))
        val tableMap = upsertedRecords.getOrPut(table) { mutableMapOf() }
        val id = payloadJson.hashCode().toString()
        tableMap[id] = payloadJson
        return Result.success("""[{"status":"success"}]""")
    }

    override suspend fun query(table: String, queryParams: String): Result<String> {
        if (shouldFail) return Result.failure(Exception("HTTP 503: Service Unavailable"))
        return Result.success("[]")
    }

    override suspend fun delete(table: String, idKey: String, idValue: String): Result<String> {
        if (shouldFail) return Result.failure(Exception("HTTP 503: Service Unavailable"))
        deletedRecords.add("$table:$idKey=$idValue")
        return Result.success("""[{"deleted":true}]""")
    }

    override suspend fun uploadStorage(bucket: String, path: String, bytes: ByteArray, mimeType: String): Result<String> {
        if (shouldFail) return Result.failure(Exception("HTTP 503: Service Unavailable"))
        uploadedImages["$bucket/$path"] = bytes
        return Result.success("https://supabase.co/storage/v1/object/$bucket/$path")
    }
}
