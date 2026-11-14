package com.tailorbook.feature.gallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tailorbook.feature.gallery.data.local.entity.DesignReferenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for Design Reference photos (TASK-07-01).
 */
@Dao
interface DesignReferenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(design: DesignReferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesigns(designs: List<DesignReferenceEntity>)

    @Update
    suspend fun update(design: DesignReferenceEntity)

    @Query("DELETE FROM design_references WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM design_references WHERE id = :id")
    suspend fun getById(id: String): DesignReferenceEntity?

    @Query("SELECT * FROM design_references WHERE shop_id = :shopId ORDER BY created_at DESC")
    fun getAllDesigns(shopId: String): Flow<List<DesignReferenceEntity>>

    @Query("SELECT * FROM design_references WHERE customer_id = :customerId ORDER BY created_at DESC")
    fun getDesignsForCustomer(customerId: String): Flow<List<DesignReferenceEntity>>

    @Query("SELECT * FROM design_references WHERE order_id = :orderId ORDER BY created_at DESC")
    fun getDesignsForOrder(orderId: String): Flow<List<DesignReferenceEntity>>

    @Query("SELECT * FROM design_references WHERE shop_id = :shopId AND tags LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchByTag(shopId: String, query: String): Flow<List<DesignReferenceEntity>>

    @Query("SELECT * FROM design_references WHERE sync_status = 'PENDING'")
    suspend fun getPendingUploads(): List<DesignReferenceEntity>
}
