package com.tailorbook

import com.tailorbook.feature.gallery.data.local.dao.DesignReferenceDao
import com.tailorbook.feature.gallery.data.local.entity.DesignReferenceEntity
import com.tailorbook.feature.gallery.data.repository.DesignReferenceRepositoryImpl
import com.tailorbook.feature.gallery.domain.repository.DesignReferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DesignReferenceRepository (TASK-07-01).
 */
class DesignReferenceRepositoryTest {

    private lateinit var fakeDao: FakeDesignReferenceDao
    private lateinit var repository: DesignReferenceRepository

    @Before
    fun setUp() {
        fakeDao = FakeDesignReferenceDao()
        repository = DesignReferenceRepositoryImpl(fakeDao)
    }

    @Test
    fun saveDesign_savesAndRetrievesById() = runBlocking {
        val design = DesignReferenceEntity(
            id = "des-101",
            shopId = "shop-1",
            localFilePath = "/data/photos/blouse_neck.jpg",
            tags = "Neck Design, Blouse, Bridal",
            notes = "Deep pot neck with zari work"
        )

        repository.saveDesign(design)
        val retrieved = repository.getDesignById("des-101")

        assertNotNull(retrieved)
        assertEquals("des-101", retrieved?.id)
        assertEquals("Deep pot neck with zari work", retrieved?.notes)
        assertTrue(retrieved?.tags?.contains("Neck Design") == true)
    }

    @Test
    fun getAllDesigns_filtersByShopId() = runBlocking {
        val d1 = DesignReferenceEntity(id = "1", shopId = "shop-1", localFilePath = "/p1.jpg")
        val d2 = DesignReferenceEntity(id = "2", shopId = "shop-1", localFilePath = "/p2.jpg")
        val d3 = DesignReferenceEntity(id = "3", shopId = "shop-2", localFilePath = "/p3.jpg")

        repository.saveDesign(d1)
        repository.saveDesign(d2)
        repository.saveDesign(d3)

        val designs = repository.getAllDesigns("shop-1").first()
        assertEquals(2, designs.size)
        assertTrue(designs.any { it.id == "1" })
        assertTrue(designs.any { it.id == "2" })
    }

    @Test
    fun getDesignsForCustomer_filtersCorrectly() = runBlocking {
        val d1 = DesignReferenceEntity(id = "1", shopId = "s1", customerId = "cust-1", localFilePath = "/p1.jpg")
        val d2 = DesignReferenceEntity(id = "2", shopId = "s1", customerId = "cust-2", localFilePath = "/p2.jpg")

        repository.saveDesign(d1)
        repository.saveDesign(d2)

        val cust1Designs = repository.getDesignsForCustomer("cust-1").first()
        assertEquals(1, cust1Designs.size)
        assertEquals("1", cust1Designs[0].id)
    }

    @Test
    fun searchByTag_matchesTagQuery() = runBlocking {
        val d1 = DesignReferenceEntity(id = "1", shopId = "s1", tags = "Maggam Work, Bridal", localFilePath = "/p1.jpg")
        val d2 = DesignReferenceEntity(id = "2", shopId = "s1", tags = "Kurti, Boat Neck", localFilePath = "/p2.jpg")

        repository.saveDesign(d1)
        repository.saveDesign(d2)

        val maggamDesigns = repository.searchByTag("s1", "Maggam").first()
        assertEquals(1, maggamDesigns.size)
        assertEquals("1", maggamDesigns[0].id)
    }

    @Test
    fun updateDesign_modifiesExistingRecord() = runBlocking {
        val d = DesignReferenceEntity(id = "1", shopId = "s1", notes = "Initial note", localFilePath = "/p1.jpg")
        repository.saveDesign(d)

        val updated = d.copy(notes = "Updated note with custom lace instructions")
        repository.updateDesign(updated)

        val retrieved = repository.getDesignById("1")
        assertEquals("Updated note with custom lace instructions", retrieved?.notes)
    }

    @Test
    fun deleteDesign_removesFromStorage() = runBlocking {
        val d = DesignReferenceEntity(id = "1", shopId = "s1", localFilePath = "/p1.jpg")
        repository.saveDesign(d)

        repository.deleteDesign("1")
        val retrieved = repository.getDesignById("1")
        assertNull(retrieved)
    }
}

/**
 * In-memory test double for DesignReferenceDao.
 */
class FakeDesignReferenceDao : DesignReferenceDao {
    private val storage = mutableMapOf<String, DesignReferenceEntity>()

    override suspend fun insert(design: DesignReferenceEntity) {
        storage[design.id] = design
    }

    override suspend fun insertDesigns(designs: List<DesignReferenceEntity>) {
        designs.forEach { storage[it.id] = it }
    }

    override suspend fun update(design: DesignReferenceEntity) {
        storage[design.id] = design
    }

    override suspend fun deleteById(id: String) {
        storage.remove(id)
    }

    override suspend fun getById(id: String): DesignReferenceEntity? {
        return storage[id]
    }

    override fun getAllDesigns(shopId: String): Flow<List<DesignReferenceEntity>> {
        return flowOf(storage.values.filter { it.shopId == shopId }.sortedByDescending { it.createdAt })
    }

    override fun getDesignsForCustomer(customerId: String): Flow<List<DesignReferenceEntity>> {
        return flowOf(storage.values.filter { it.customerId == customerId }.sortedByDescending { it.createdAt })
    }

    override fun getDesignsForOrder(orderId: String): Flow<List<DesignReferenceEntity>> {
        return flowOf(storage.values.filter { it.orderId == orderId }.sortedByDescending { it.createdAt })
    }

    override fun searchByTag(shopId: String, query: String): Flow<List<DesignReferenceEntity>> {
        return flowOf(storage.values.filter { it.shopId == shopId && it.tags.contains(query, ignoreCase = true) }.sortedByDescending { it.createdAt })
    }

    override suspend fun getPendingUploads(): List<DesignReferenceEntity> {
        return storage.values.filter { it.syncStatus == "PENDING" }
    }
}
