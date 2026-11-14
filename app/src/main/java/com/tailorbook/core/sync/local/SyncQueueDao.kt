package com.tailorbook.core.sync.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for local SyncQueue (TASK-14-03).
 */
@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entry: SyncQueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueAll(entries: List<SyncQueueEntity>)

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED') AND retry_count < :maxRetries ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingEntries(limit: Int = 50, maxRetries: Int = 5): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'FAILED', last_error = :error, retry_count = retry_count + 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markFailed(id: String, error: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED' OR (status = 'FAILED' AND retry_count >= :maxRetries)")
    suspend fun pruneCompletedOrExhausted(maxRetries: Int = 5)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'FAILED')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'FAILED')")
    suspend fun getPendingCount(): Int
}
