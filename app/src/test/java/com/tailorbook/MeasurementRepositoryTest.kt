package com.tailorbook

import com.tailorbook.feature.measurements.data.local.dao.MeasurementDao
import com.tailorbook.feature.measurements.data.local.entity.MeasurementVersionEntity
import com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl
import com.tailorbook.feature.measurements.domain.model.GarmentTemplates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeMeasurementDao : MeasurementDao {
    val storage = mutableListOf<MeasurementVersionEntity>()

    override fun getVersions(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Flow<List<MeasurementVersionEntity>> {
        return flowOf(
            storage.filter {
                it.customerId == customerId &&
                it.familyMemberId == familyMemberId &&
                it.templateId == templateId
            }.sortedByDescending { it.versionNumber }
        )
    }

    override fun getLatestVersion(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Flow<MeasurementVersionEntity?> {
        val latest = storage.filter {
            it.customerId == customerId &&
            it.familyMemberId == familyMemberId &&
            it.templateId == templateId
        }.maxByOrNull { it.versionNumber }
        return flowOf(latest)
    }

    override suspend fun getLatestVersionNumber(
        customerId: String,
        familyMemberId: String?,
        templateId: String
    ): Int? {
        return storage.filter {
            it.customerId == customerId &&
            it.familyMemberId == familyMemberId &&
            it.templateId == templateId
        }.maxOfOrNull { it.versionNumber }
    }

    override fun getAllForCustomer(customerId: String): Flow<List<MeasurementVersionEntity>> {
        return flowOf(storage.filter { it.customerId == customerId })
    }

    override suspend fun insertVersion(version: MeasurementVersionEntity) {
        storage.add(version)
    }

    override suspend fun insertVersions(versions: List<MeasurementVersionEntity>) {
        storage.addAll(versions)
    }

    override suspend fun deleteVersion(version: MeasurementVersionEntity) {
        storage.remove(version)
    }
}

class MeasurementRepositoryTest {

    private lateinit var fakeDao: FakeMeasurementDao
    private lateinit var repository: MeasurementRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeMeasurementDao()
        repository = MeasurementRepositoryImpl(fakeDao)
    }

    @Test
    fun garmentTemplates_has8PreconfiguredTemplates() {
        assertEquals(8, GarmentTemplates.ALL_TEMPLATES.size)
        assertNotNull(GarmentTemplates.findById("BLOUSE"))
        assertNotNull(GarmentTemplates.findById("KURTI"))
        assertNotNull(GarmentTemplates.findById("SALWAR"))
        assertNotNull(GarmentTemplates.findById("DRESS"))
        assertNotNull(GarmentTemplates.findById("SHIRT"))
        assertNotNull(GarmentTemplates.findById("TROUSER"))
        assertNotNull(GarmentTemplates.findById("TSHIRT"))
        assertNotNull(GarmentTemplates.findById("CUSTOM"))
    }

    @Test
    fun blouseTemplate_contains14MeasurementPoints() {
        val blouse = GarmentTemplates.BLOUSE
        assertEquals("BLOUSE", blouse.id)
        assertEquals(14, blouse.fields.size)
        assertTrue(blouse.fields.any { it.key == "armhole" })
        assertTrue(blouse.fields.any { it.key == "apex_length" })
        assertTrue(blouse.fields.any { it.key == "front_neck" })
    }

    @Test
    fun parseMeasurements_correctlyExtractsMapFromJson() {
        val json = """{"length":14.5,"chest":36.0,"waist":30.25}"""
        val map = MeasurementRepositoryImpl.parseMeasurements(json)

        assertEquals(3, map.size)
        assertEquals(14.5, map["length"] ?: 0.0, 0.001)
        assertEquals(36.0, map["chest"] ?: 0.0, 0.001)
        assertEquals(30.25, map["waist"] ?: 0.0, 0.001)
    }

    @Test
    fun getNextVersionNumber_incrementsFromZeroWhenEmpty() = runBlocking {
        val next = repository.getNextVersionNumber("c1", null, "BLOUSE")
        assertEquals(1, next)
    }

    @Test
    fun getNextVersionNumber_incrementsExistingVersion() = runBlocking {
        fakeDao.storage.add(
            MeasurementVersionEntity(
                customerId = "c1",
                familyMemberId = null,
                templateId = "BLOUSE",
                versionNumber = 1,
                measurementsJson = "{}"
            )
        )
        fakeDao.storage.add(
            MeasurementVersionEntity(
                customerId = "c1",
                familyMemberId = null,
                templateId = "BLOUSE",
                versionNumber = 2,
                measurementsJson = "{}"
            )
        )

        val next = repository.getNextVersionNumber("c1", null, "BLOUSE")
        assertEquals(3, next)
    }

    @Test
    fun saveNewVersion_generatesValidEntityWithIncrementedVersion() = runBlocking {
        fakeDao.storage.add(
            MeasurementVersionEntity(
                customerId = "c1",
                familyMemberId = null,
                templateId = "BLOUSE",
                versionNumber = 1,
                measurementsJson = "{}"
            )
        )

        val measurements = mapOf("length" to 15.0, "chest" to 38.0)
        val result = repository.saveNewVersion(
            customerId = "c1",
            familyMemberId = null,
            templateId = "BLOUSE",
            measurements = measurements,
            unit = "INCH",
            notes = "Comfort fit"
        )

        assertTrue(result.isSuccess)
        val saved = result.getOrNull()
        assertNotNull(saved)
        assertEquals(2, saved?.versionNumber)
        assertEquals("BLOUSE", saved?.templateId)
        assertEquals("INCH", saved?.unit)
        assertEquals("Comfort fit", saved?.notes)
    }
}
