package com.tailorbook.core.sync.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Local offline sync queue table (TASK-14-03 / REQ-DATA-01).
 * Records every local entity mutation for background replication to Supabase PostgreSQL.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "entity_type")
    val entityType: String, // CUSTOMER, ORDER, PAYMENT, MEASUREMENT, DESIGN

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "operation")
    val operation: String, // INSERT, UPDATE, DELETE

    @ColumnInfo(name = "payload_json")
    val payloadJson: String,

    @ColumnInfo(name = "status")
    val status: String = STATUS_PENDING, // PENDING, SYNCING, FAILED, SYNCED

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SYNCING = "SYNCING"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_SYNCED = "SYNCED"

        const val ENTITY_CUSTOMER = "CUSTOMER"
        const val ENTITY_FAMILY_MEMBER = "FAMILY_MEMBER"
        const val ENTITY_ORDER = "ORDER"
        const val ENTITY_PAYMENT = "PAYMENT"
        const val ENTITY_MEASUREMENT = "MEASUREMENT"
        const val ENTITY_MEASUREMENT_TEMPLATE = "MEASUREMENT_TEMPLATE"
        const val ENTITY_DESIGN = "DESIGN"
        const val ENTITY_STAFF = "STAFF"
        const val ENTITY_SHOP = "SHOP"

        const val OP_INSERT = "INSERT"
        const val OP_UPDATE = "UPDATE"
        const val OP_DELETE = "DELETE"
    }
}
