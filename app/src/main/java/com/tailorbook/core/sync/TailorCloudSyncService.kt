package com.tailorbook.core.sync

import com.tailorbook.core.database.TailorBookDatabase
import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.core.network.SupabaseClient
import com.tailorbook.core.network.SupabaseClientImpl
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import com.tailorbook.feature.gallery.data.local.entity.DesignReferenceEntity
import com.tailorbook.feature.measurements.data.local.entity.MeasurementVersionEntity
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.payments.data.local.entity.PaymentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import timber.log.Timber
import java.time.Instant

/**
 * Enterprise Cloud Sync Service for TailorBook.
 * Pulls existing tenant data (customers, family members, orders, payments, measurements, designs)
 * from Supabase PostgreSQL into the local Room database upon login or authenticated app startup
 * for Subscribed & VIP shops.
 */
class TailorCloudSyncService(
    private val database: TailorBookDatabase,
    private val preferencesManager: UserPreferencesManager? = null,
    private val supabaseClient: SupabaseClient = SupabaseClientImpl()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Pulls existing tenant records in relational foreign-key order:
     * 1. Shops (Tenant Root)
     * 2. Customers
     * 3. Family Members
     * 4. Orders
     * 5. Payments
     * 6. Measurement Versions
     * 7. Design References
     *
     * Only executed if the user is Subscribed or VIP. Free tier remains purely offline.
     * Offline-first: returns 0 and logs failure if network or Supabase is unreachable.
     */
    suspend fun pullShopData(shopId: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (shopId.isBlank() || shopId == "default_shop") {
                return@runCatching 0
            }

            // Sync Gating: Only Subscribed & VIP shops sync with Supabase cloud
            if (preferencesManager != null && !preferencesManager.isSyncEligible()) {
                Timber.d("TailorCloudSyncService: Shop is on Free tier (not VIP or Subscribed). Cloud restore skipped.")
                return@runCatching 0
            }

            Timber.i("TailorCloudSyncService: Pulling cloud data for shop: $shopId (VIP/Subscribed)")

            // 0. Ensure shop tenant root exists in Supabase so foreign keys hold
            ensureShopHeader(shopId)

            // 1. Fetch Customers
            var count = 0
            val custResult = supabaseClient.query("customers", "shop_id=eq.$shopId")
            if (custResult.isSuccess) {
                val jsonString = custResult.getOrThrow()
                if (jsonString.isNotBlank() && jsonString != "[]") {
                    val jsonElements = json.parseToJsonElement(jsonString).jsonArray
                    val customerEntities = jsonElements.map { elem ->
                        val obj = elem.jsonObject
                        CustomerEntity(
                            id = obj["id"]?.jsonPrimitive?.content ?: java.util.UUID.randomUUID().toString(),
                            shopId = obj["shop_id"]?.jsonPrimitive?.content ?: shopId,
                            name = obj["name"]?.jsonPrimitive?.content ?: "Customer",
                            primaryPhone = obj["phone"]?.jsonPrimitive?.content ?: "",
                            secondaryPhone = obj["secondary_phone"]?.jsonPrimitive?.content,
                            gender = obj["gender"]?.jsonPrimitive?.content ?: "FEMALE",
                            address = obj["address"]?.jsonPrimitive?.content,
                            notes = obj["notes"]?.jsonPrimitive?.content,
                            isArchived = false
                        )
                    }

                    if (customerEntities.isNotEmpty()) {
                        database.customerDao().insertCustomers(customerEntities)
                        count = customerEntities.size
                        Timber.i("Fetched and saved $count customers from Supabase for shop $shopId")
                    }
                }
            } else {
                Timber.w("Could not fetch customers from Supabase: ${custResult.exceptionOrNull()?.message}")
            }

            // 2. Fetch Family Members
            val familyResult = supabaseClient.query("family_members", "shop_id=eq.$shopId")
            if (familyResult.isSuccess) {
                val jsonString = familyResult.getOrThrow()
                if (jsonString.isNotBlank() && jsonString != "[]") {
                    val jsonElements = json.parseToJsonElement(jsonString).jsonArray
                    val familyEntities = jsonElements.map { elem ->
                        val obj = elem.jsonObject
                        FamilyMemberEntity(
                            id = obj["id"]?.jsonPrimitive?.content ?: java.util.UUID.randomUUID().toString(),
                            customerId = obj["customer_id"]?.jsonPrimitive?.content.orEmpty(),
                            shopId = obj["shop_id"]?.jsonPrimitive?.content ?: shopId,
                            name = obj["name"]?.jsonPrimitive?.content ?: "",
                            relationship = obj["relationship"]?.jsonPrimitive?.content ?: "Family Member",
                            gender = obj["gender"]?.jsonPrimitive?.content,
                            notes = obj["notes"]?.jsonPrimitive?.content
                        )
                    }
                    if (familyEntities.isNotEmpty()) {
                        database.familyMemberDao().insertFamilyMembers(familyEntities)
                        Timber.i("Fetched and saved ${familyEntities.size} family members from Supabase")
                    }
                }
            }

            // 3. Fetch Orders
            val ordersResult = supabaseClient.query("orders", "shop_id=eq.$shopId")
            if (ordersResult.isSuccess) {
                val jsonString = ordersResult.getOrThrow()
                if (jsonString.isNotBlank() && jsonString != "[]") {
                    val jsonElements = json.parseToJsonElement(jsonString).jsonArray
                    val orderEntities = jsonElements.map { elem ->
                        val obj = elem.jsonObject
                        val dueDateStr = obj["due_date"]?.jsonPrimitive?.content
                        val dueDateEpoch = parseIsoOrEpoch(dueDateStr) ?: (System.currentTimeMillis() + 86400000L * 5)
                        val createdAtStr = obj["created_at"]?.jsonPrimitive?.content
                        val createdAtEpoch = parseIsoOrEpoch(createdAtStr) ?: System.currentTimeMillis()
                        val updatedAtStr = obj["updated_at"]?.jsonPrimitive?.content
                        val updatedAtEpoch = parseIsoOrEpoch(updatedAtStr) ?: System.currentTimeMillis()

                        OrderEntity(
                            id = obj["id"]?.jsonPrimitive?.content ?: java.util.UUID.randomUUID().toString(),
                            shopId = obj["shop_id"]?.jsonPrimitive?.content ?: shopId,
                            orderNumber = obj["order_number"]?.jsonPrimitive?.content ?: "TB-001",
                            customerId = obj["customer_id"]?.jsonPrimitive?.content.orEmpty(),
                            orderDate = createdAtEpoch,
                            promisedDeliveryDate = dueDateEpoch,
                            status = obj["status"]?.jsonPrimitive?.content ?: "NEW",
                            totalAmount = obj["total_amount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                            advanceAmount = obj["advance_paid"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                            balanceAmount = obj["balance_due"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                            cancellationReason = obj["notes"]?.jsonPrimitive?.content,
                            createdAt = createdAtEpoch,
                            updatedAt = updatedAtEpoch
                        )
                    }
                    if (orderEntities.isNotEmpty()) {
                        database.orderDao().insertOrders(orderEntities)
                        Timber.i("Fetched and saved ${orderEntities.size} orders from Supabase for shop $shopId")
                    }
                }
            } else {
                Timber.w("Could not fetch orders from Supabase: ${ordersResult.exceptionOrNull()?.message}")
            }

            // 4. Fetch Payments
            val paymentsResult = supabaseClient.query("payments", "shop_id=eq.$shopId")
            if (paymentsResult.isSuccess) {
                val jsonString = paymentsResult.getOrThrow()
                if (jsonString.isNotBlank() && jsonString != "[]") {
                    val jsonElements = json.parseToJsonElement(jsonString).jsonArray
                    val paymentEntities = jsonElements.map { elem ->
                        val obj = elem.jsonObject
                        val recordedAtStr = obj["recorded_at"]?.jsonPrimitive?.content
                        val recordedAtEpoch = parseIsoOrEpoch(recordedAtStr) ?: System.currentTimeMillis()
                        val createdAtStr = obj["created_at"]?.jsonPrimitive?.content
                        val createdAtEpoch = parseIsoOrEpoch(createdAtStr) ?: recordedAtEpoch

                        PaymentEntity(
                            id = obj["id"]?.jsonPrimitive?.content ?: java.util.UUID.randomUUID().toString(),
                            shopId = obj["shop_id"]?.jsonPrimitive?.content ?: shopId,
                            orderId = obj["order_id"]?.jsonPrimitive?.content,
                            customerId = obj["customer_id"]?.jsonPrimitive?.content.orEmpty(),
                            amount = obj["amount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                            paymentMethod = obj["payment_method"]?.jsonPrimitive?.content ?: "CASH",
                            referenceNumber = obj["transaction_ref"]?.jsonPrimitive?.content
                                ?: obj["receipt_number"]?.jsonPrimitive?.content,
                            paymentDate = recordedAtEpoch,
                            notes = obj["notes"]?.jsonPrimitive?.content,
                            isReversed = false,
                            createdAt = createdAtEpoch
                        )
                    }
                    if (paymentEntities.isNotEmpty()) {
                        database.paymentDao().insertPayments(paymentEntities)
                        Timber.i("Fetched and saved ${paymentEntities.size} payments from Supabase for shop $shopId")
                    }
                }
            } else {
                Timber.w("Could not fetch payments from Supabase: ${paymentsResult.exceptionOrNull()?.message}")
            }

            // 5. Fetch Measurement Versions
            val measResult = supabaseClient.query("measurement_versions", "shop_id=eq.$shopId")
            if (measResult.isSuccess) {
                val jsonString = measResult.getOrThrow()
                if (jsonString.isNotBlank() && jsonString != "[]") {
                    val jsonElements = json.parseToJsonElement(jsonString).jsonArray
                    val measurementEntities = jsonElements.map { elem ->
                        val obj = elem.jsonObject
                        val measuredAtStr = obj["measured_at"]?.jsonPrimitive?.content
                        val measuredAtEpoch = parseIsoOrEpoch(measuredAtStr) ?: System.currentTimeMillis()
                        val createdAtStr = obj["created_at"]?.jsonPrimitive?.content
                        val createdAtEpoch = parseIsoOrEpoch(createdAtStr) ?: measuredAtEpoch

                        MeasurementVersionEntity(
                            id = obj["id"]?.jsonPrimitive?.content ?: java.util.UUID.randomUUID().toString(),
                            customerId = obj["customer_id"]?.jsonPrimitive?.content.orEmpty(),
                            familyMemberId = obj["family_member_id"]?.jsonPrimitive?.content,
                            templateId = obj["template_id"]?.jsonPrimitive?.content ?: "BLOUSE",
                            versionNumber = obj["version_number"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
                            measurementsJson = obj["measurements_json"]?.toString() ?: "{}",
                            unit = obj["unit"]?.jsonPrimitive?.content ?: "INCH",
                            notes = obj["notes"]?.jsonPrimitive?.content,
                            measuredAt = measuredAtEpoch,
                            createdAt = createdAtEpoch
                        )
                    }
                    if (measurementEntities.isNotEmpty()) {
                        database.measurementDao().insertVersions(measurementEntities)
                        Timber.i("Fetched and saved ${measurementEntities.size} measurement versions from Supabase for shop $shopId")
                    }
                }
            } else {
                Timber.w("Could not fetch measurement versions from Supabase: ${measResult.exceptionOrNull()?.message}")
            }

            // 6. Fetch Design References
            val designResult = supabaseClient.query("design_references", "shop_id=eq.$shopId")
            if (designResult.isSuccess) {
                val jsonString = designResult.getOrThrow()
                if (jsonString.isNotBlank() && jsonString != "[]") {
                    val jsonElements = json.parseToJsonElement(jsonString).jsonArray
                    val designEntities = jsonElements.map { elem ->
                        val obj = elem.jsonObject
                        val createdAtStr = obj["created_at"]?.jsonPrimitive?.content
                        val createdAtEpoch = parseIsoOrEpoch(createdAtStr) ?: System.currentTimeMillis()
                        val imageUrl = obj["image_url"]?.jsonPrimitive?.content ?: ""

                        DesignReferenceEntity(
                            id = obj["id"]?.jsonPrimitive?.content ?: java.util.UUID.randomUUID().toString(),
                            shopId = obj["shop_id"]?.jsonPrimitive?.content ?: shopId,
                            customerId = obj["customer_id"]?.jsonPrimitive?.content,
                            orderId = obj["order_id"]?.jsonPrimitive?.content,
                            localFilePath = imageUrl,
                            remoteStorageUrl = imageUrl.ifBlank { null },
                            tags = obj["tags"]?.jsonPrimitive?.content ?: "",
                            notes = obj["notes"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: "",
                            syncStatus = "UPLOADED",
                            createdAt = createdAtEpoch
                        )
                    }
                    if (designEntities.isNotEmpty()) {
                        database.designReferenceDao().insertDesigns(designEntities)
                        Timber.i("Fetched and saved ${designEntities.size} design references from Supabase for shop $shopId")
                    }
                }
            } else {
                Timber.w("Could not fetch design references from Supabase: ${designResult.exceptionOrNull()?.message}")
            }

            count
        }
    }

    private suspend fun ensureShopHeader(shopId: String) {
        if (preferencesManager == null) return
        val name = preferencesManager.shopNameFlow.firstOrNull() ?: "My Boutique"
        val owner = preferencesManager.ownerNameFlow.firstOrNull() ?: "Shop Owner"
        val phone = preferencesManager.ownerPhoneFlow.firstOrNull() ?: "9999999999"
        val payload = buildJsonObject {
            put("id", shopId)
            put("name", name)
            put("owner_name", owner)
            put("owner_phone", phone)
            put("subscription_plan", "PRO_MONTHLY")
        }.toString()
        val res = supabaseClient.upsert("shops", payload)
        if (res.isFailure) {
            Timber.w("Failed to ensure shop header in Supabase: ${res.exceptionOrNull()?.message}")
        }
    }

    private fun parseIsoOrEpoch(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            dateStr.toLongOrNull() ?: Instant.parse(dateStr).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
