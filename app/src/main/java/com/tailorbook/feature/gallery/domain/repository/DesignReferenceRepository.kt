package com.tailorbook.feature.gallery.domain.repository

import com.tailorbook.feature.gallery.data.local.entity.DesignReferenceEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Domain repository contract for Design References (TASK-07-01).
 */
interface DesignReferenceRepository {
    fun getAllDesigns(shopId: String): Flow<List<DesignReferenceEntity>>
    fun getDesignsForCustomer(customerId: String): Flow<List<DesignReferenceEntity>>
    fun getDesignsForOrder(orderId: String): Flow<List<DesignReferenceEntity>>
    fun searchByTag(shopId: String, query: String): Flow<List<DesignReferenceEntity>>
    suspend fun getDesignById(id: String): DesignReferenceEntity?
    suspend fun saveDesign(design: DesignReferenceEntity)
    suspend fun updateDesign(design: DesignReferenceEntity)
    suspend fun deleteDesign(id: String)
}
