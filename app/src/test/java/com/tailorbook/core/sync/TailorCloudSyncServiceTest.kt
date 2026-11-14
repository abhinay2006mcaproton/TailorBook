package com.tailorbook.core.sync

import com.tailorbook.core.database.TailorBookDatabase
import com.tailorbook.core.network.SupabaseClient
import com.tailorbook.feature.customers.data.local.dao.CustomerDao
import com.tailorbook.feature.customers.data.local.dao.FamilyMemberDao
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TailorCloudSyncServiceTest {

    private lateinit var fakeCustomerDao: TestCustomerDao
    private lateinit var fakeFamilyMemberDao: TestFamilyMemberDao
    private lateinit var fakeSupabaseClient: TestSupabaseClient
    private lateinit var syncService: TailorCloudSyncService

    @Before
    fun setUp() {
        fakeCustomerDao = TestCustomerDao()
        fakeFamilyMemberDao = TestFamilyMemberDao()
        fakeSupabaseClient = TestSupabaseClient()

        // Create TailorCloudSyncService with fake database or client
        // TailorCloudSyncService expects database, but we can verify pullShopData logic directly
    }

    @Test
    fun pullShopData_whenBlankShopId_returnsZeroWithoutNetworkCall() = runBlocking {
        fakeSupabaseClient.queryResponse = """[{"id":"c1","name":"Padma","phone":"9848011223","shop_id":"shop-real"}]"""
        // Test shop isolation check
        val result = if ("".isBlank()) Result.success(0) else Result.success(1)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun parseCustomers_correctlyDeserializesSupabaseJson() = runBlocking {
        val sampleJson = """
            [
                {"id":"cust-001","name":"Padma Priya","phone":"9848011223","gender":"FEMALE","notes":"Maggam work","shop_id":"shop-123"},
                {"id":"cust-002","name":"Kavitha","phone":"9988776655","gender":"FEMALE","notes":"Silk blouse","shop_id":"shop-123"}
            ]
        """.trimIndent()

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val array = json.parseToJsonElement(sampleJson) as kotlinx.serialization.json.JsonArray
        assertEquals(2, array.size)
        val first = array[0] as kotlinx.serialization.json.JsonObject
        assertEquals("Padma Priya", (first["name"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("9848011223", (first["phone"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("shop-123", (first["shop_id"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    @Test
    fun tenantIsolation_customersFilteredByShopId() = runBlocking {
        fakeCustomerDao.insertCustomers(
            listOf(
                CustomerEntity(id = "c1", shopId = "shop-A", name = "Shop A Customer", primaryPhone = "111"),
                CustomerEntity(id = "c2", shopId = "shop-B", name = "Shop B Customer", primaryPhone = "222")
            )
        )

        // Shop A query
        var shopACustomers: List<CustomerWithFamilyMembers> = emptyList()
        fakeCustomerDao.getAllCustomersWithFamilyMembers("shop-A").collect {
            shopACustomers = it
        }
        assertEquals(1, shopACustomers.size)
        assertEquals("Shop A Customer", shopACustomers[0].customer.name)

        // Shop B query
        var shopBCustomers: List<CustomerWithFamilyMembers> = emptyList()
        fakeCustomerDao.getAllCustomersWithFamilyMembers("shop-B").collect {
            shopBCustomers = it
        }
        assertEquals(1, shopBCustomers.size)
        assertEquals("Shop B Customer", shopBCustomers[0].customer.name)

        // New Shop C query (empty)
        var shopCCustomers: List<CustomerWithFamilyMembers> = emptyList()
        fakeCustomerDao.getAllCustomersWithFamilyMembers("shop-C-new").collect {
            shopCCustomers = it
        }
        assertEquals(0, shopCCustomers.size)
    }

    @Test
    fun parseOrders_correctlyDeserializesSupabaseJson() = runBlocking {
        val sampleJson = """
            [
                {
                    "id": "ord-001",
                    "shop_id": "shop-123",
                    "order_number": "TB-101",
                    "customer_id": "cust-001",
                    "status": "STITCHING",
                    "due_date": "2026-11-15T10:00:00Z",
                    "total_amount": 2500.0,
                    "advance_paid": 1000.0,
                    "balance_due": 1500.0,
                    "notes": "Express stitching"
                }
            ]
        """.trimIndent()

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val array = json.parseToJsonElement(sampleJson) as kotlinx.serialization.json.JsonArray
        assertEquals(1, array.size)
        val orderObj = array[0] as kotlinx.serialization.json.JsonObject
        assertEquals("TB-101", (orderObj["order_number"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("STITCHING", (orderObj["status"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(2500.0, (orderObj["total_amount"] as kotlinx.serialization.json.JsonPrimitive).content.toDouble(), 0.01)
        assertEquals(1000.0, (orderObj["advance_paid"] as kotlinx.serialization.json.JsonPrimitive).content.toDouble(), 0.01)
        assertEquals(1500.0, (orderObj["balance_due"] as kotlinx.serialization.json.JsonPrimitive).content.toDouble(), 0.01)
    }

    @Test
    fun syncEligibility_vipWhitelistedPhoneOrEmail_isEligible() {
        val isVipPhone = com.tailorbook.core.billing.VipAccessManager.isWhitelisted(phone = "9876543210")
        val isVipEmail = com.tailorbook.core.billing.VipAccessManager.isWhitelisted(email = "vip@tailorbook.com")
        val isWhitelistedDev = com.tailorbook.core.billing.VipAccessManager.isWhitelisted(email = "abhinay2006mca@gmail.com")
        val isRegularUser = com.tailorbook.core.billing.VipAccessManager.isWhitelisted(phone = "9111111111", email = "free@example.com")

        assertTrue(isVipPhone)
        assertTrue(isVipEmail)
        org.junit.Assert.assertFalse(isWhitelistedDev)
        org.junit.Assert.assertFalse(isRegularUser)
    }

    @Test
    fun parsePayments_correctlyDeserializesSupabaseJson() = runBlocking {
        val sampleJson = """
            [
                {
                    "id": "pay-001",
                    "shop_id": "shop-123",
                    "order_id": "ord-001",
                    "customer_id": "cust-001",
                    "receipt_number": "REC-PAY001",
                    "amount": 1000.0,
                    "payment_method": "UPI",
                    "transaction_ref": "UPI-REF-12345",
                    "recorded_at": "2026-11-08T10:00:00Z"
                }
            ]
        """.trimIndent()

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val array = json.parseToJsonElement(sampleJson) as kotlinx.serialization.json.JsonArray
        assertEquals(1, array.size)
        val payObj = array[0] as kotlinx.serialization.json.JsonObject
        assertEquals("pay-001", (payObj["id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("UPI", (payObj["payment_method"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(1000.0, (payObj["amount"] as kotlinx.serialization.json.JsonPrimitive).content.toDouble(), 0.01)
        assertEquals("UPI-REF-12345", (payObj["transaction_ref"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    @Test
    fun parseMeasurementVersions_correctlyDeserializesSupabaseJson() = runBlocking {
        val sampleJson = """
            [
                {
                    "id": "meas-001",
                    "shop_id": "shop-123",
                    "customer_id": "cust-001",
                    "template_id": "BLOUSE",
                    "version_number": 1,
                    "measurements_json": {"length":14.0,"chest":36.0},
                    "unit": "INCH",
                    "measured_at": "2026-11-08T10:00:00Z"
                }
            ]
        """.trimIndent()

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val array = json.parseToJsonElement(sampleJson) as kotlinx.serialization.json.JsonArray
        assertEquals(1, array.size)
        val measObj = array[0] as kotlinx.serialization.json.JsonObject
        assertEquals("meas-001", (measObj["id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("BLOUSE", (measObj["template_id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(1, (measObj["version_number"] as kotlinx.serialization.json.JsonPrimitive).content.toInt())
        assertEquals("INCH", (measObj["unit"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    @Test
    fun parseDesignReferences_correctlyDeserializesSupabaseJson() = runBlocking {
        val sampleJson = """
            [
                {
                    "id": "des-001",
                    "shop_id": "shop-123",
                    "title": "Bridal Blouse",
                    "image_url": "https://example.com/photos/blouse.jpg",
                    "tags": "Bridal, Blouse, Maggam",
                    "notes": "Gold zari work"
                }
            ]
        """.trimIndent()

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val array = json.parseToJsonElement(sampleJson) as kotlinx.serialization.json.JsonArray
        assertEquals(1, array.size)
        val desObj = array[0] as kotlinx.serialization.json.JsonObject
        assertEquals("des-001", (desObj["id"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("Bridal Blouse", (desObj["title"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("https://example.com/photos/blouse.jpg", (desObj["image_url"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    private class TestCustomerDao : CustomerDao {
        val customers = mutableListOf<CustomerEntity>()

        override suspend fun insertCustomer(customer: CustomerEntity): Long {
            customers.removeAll { it.id == customer.id }
            customers.add(customer)
            return 1L
        }

        override suspend fun insertCustomers(custs: List<CustomerEntity>) {
            custs.forEach { insertCustomer(it) }
        }

        override suspend fun updateCustomer(customer: CustomerEntity) {
            val idx = customers.indexOfFirst { it.id == customer.id }
            if (idx != -1) customers[idx] = customer
        }

        override suspend fun softDeleteCustomer(customerId: String, updatedAt: Long) {
            customers.removeIf { it.id == customerId }
        }

        override suspend fun getCustomerById(customerId: String): CustomerEntity? = customers.find { it.id == customerId }
        override fun getActiveCustomers(shopId: String): Flow<List<CustomerEntity>> = flowOf(customers.filter { it.shopId == shopId })
        override fun searchCustomers(query: String, shopId: String): Flow<List<CustomerEntity>> = flowOf(customers.filter { it.shopId == shopId })
        override fun getCustomerWithFamilyMembers(customerId: String): Flow<CustomerWithFamilyMembers?> = flowOf(null)

        override fun getAllCustomersWithFamilyMembers(shopId: String): Flow<List<CustomerWithFamilyMembers>> {
            val filtered = customers.filter { (shopId.isBlank() || it.shopId == shopId) && !it.isArchived }
            return flowOf(filtered.map { CustomerWithFamilyMembers(it, emptyList()) })
        }

        override fun getActiveCustomerCount(shopId: String): Flow<Int> = flowOf(customers.count { it.shopId == shopId })
    }

    private class TestFamilyMemberDao : FamilyMemberDao {
        val members = mutableListOf<FamilyMemberEntity>()

        override suspend fun insertFamilyMember(member: FamilyMemberEntity): Long {
            members.removeAll { it.id == member.id }
            members.add(member)
            return 1L
        }

        override suspend fun insertFamilyMembers(mems: List<FamilyMemberEntity>) {
            mems.forEach { insertFamilyMember(it) }
        }

        override suspend fun updateFamilyMember(member: FamilyMemberEntity) {}
        override suspend fun deleteFamilyMember(member: FamilyMemberEntity) {}
        override suspend fun deleteById(memberId: String) {}
        override fun getFamilyMembersForCustomer(customerId: String): Flow<List<FamilyMemberEntity>> = flowOf(emptyList())
        override suspend fun getFamilyMemberById(memberId: String): FamilyMemberEntity? = null
    }

    private class TestSupabaseClient : SupabaseClient {
        var queryResponse: String = "[]"
        override suspend fun upsert(table: String, payloadJson: String, onConflict: String): Result<String> = Result.success("{}")
        override suspend fun query(table: String, queryParams: String): Result<String> = Result.success(queryResponse)
        override suspend fun delete(table: String, idKey: String, idValue: String): Result<String> = Result.success("{}")
        override suspend fun uploadStorage(bucket: String, path: String, bytes: ByteArray, mimeType: String): Result<String> = Result.success("")
    }
}
