package com.tailorbook.core.device

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.core.network.SupabaseClient
import com.tailorbook.core.network.SupabaseClientImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import timber.log.Timber

data class DeviceRegistration(
    val deviceId: String,
    val email: String,
    val shopName: String? = null,
    val isFreeStarterClaimed: Boolean = true
)

fun maskEmail(email: String): String {
    val atIndex = email.indexOf('@')
    if (atIndex <= 2) return email
    val name = email.substring(0, atIndex)
    val domain = email.substring(atIndex)
    val visible = name.take(3)
    return "$visible***$domain"
}

/**
 * Manages device-level hardware tracking using ANDROID_ID.
 * Implements the Device-Bound Quota Strategy:
 * - Users are NEVER blocked from registering or switching accounts on a physical device.
 * - However, the Introductory Free Starter Quota (10 free customers without ads) is tied to the physical device.
 * - If a device has already consumed its free starter pack under another email, any subsequent accounts on this
 *   device must watch rewarded video ads (or upgrade to Pro) from their first customer onward.
 */
class DeviceManager(
    private val context: Context,
    private val preferencesManager: UserPreferencesManager,
    private val supabaseClient: SupabaseClient = SupabaseClientImpl()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @SuppressLint("HardwareIds")
    fun getAndroidId(): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )?.trim() ?: "UNKNOWN_DEVICE"
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving ANDROID_ID")
            "UNKNOWN_DEVICE"
        }
    }

    /**
     * Checks if the device's introductory free starter customer quota has already been consumed
     * by a different account on this physical phone.
     */
    suspend fun isStarterQuotaConsumedByAnotherAccount(currentEmail: String?): Boolean = withContext(Dispatchers.IO) {
        val deviceId = getAndroidId()
        if (deviceId == "UNKNOWN_DEVICE" || deviceId.isBlank() || currentEmail.isNullOrBlank()) {
            return@withContext false
        }

        val cleanCurrentEmail = currentEmail.trim().lowercase()

        // 1. Check local persistent store
        val localRegisteredEmail = preferencesManager.registeredEmailFlow.firstOrNull()?.trim()?.lowercase()
        val isLocalStarterConsumed = preferencesManager.isDeviceFreeStarterConsumedFlow.firstOrNull() ?: false

        if (isLocalStarterConsumed && !localRegisteredEmail.isNullOrBlank() && localRegisteredEmail != cleanCurrentEmail) {
            Timber.i("DeviceManager: Device starter quota was consumed by $localRegisteredEmail. Disallowing free starter for $cleanCurrentEmail.")
            return@withContext true
        }

        // 2. Check Supabase remote registry
        try {
            val queryResult = supabaseClient.query("device_registrations", "device_id=eq.$deviceId")
            if (queryResult.isSuccess) {
                val jsonString = queryResult.getOrThrow()
                if (jsonString.isNotBlank() && jsonString != "[]") {
                    val elements = json.parseToJsonElement(jsonString).jsonArray
                    if (elements.isNotEmpty()) {
                        val record = elements[0].jsonObject
                        val remoteEmail = record["email"]?.jsonPrimitive?.content?.trim()?.lowercase().orEmpty()
                        val claimed = record["free_starter_claimed"]?.jsonPrimitive?.booleanOrNull ?: true

                        if (claimed && remoteEmail.isNotBlank() && remoteEmail != cleanCurrentEmail) {
                            // Sync state locally
                            preferencesManager.setDeviceFreeStarterConsumed(true)
                            preferencesManager.setRegisteredDevice(deviceId, remoteEmail)
                            Timber.i("DeviceManager: Supabase confirmed starter quota claimed by $remoteEmail. Gating $cleanCurrentEmail.")
                            return@withContext true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to query device_registrations in Supabase")
        }

        false
    }

    /**
     * Marks the free starter quota as consumed on this physical device locally and in Supabase.
     */
    suspend fun markStarterQuotaUsed(email: String?) = withContext(Dispatchers.IO) {
        val deviceId = getAndroidId()
        preferencesManager.setDeviceFreeStarterConsumed(true)

        if (deviceId == "UNKNOWN_DEVICE" || deviceId.isBlank() || email.isNullOrBlank()) return@withContext
        val cleanEmail = email.trim().lowercase()

        try {
            val payload = buildJsonObject {
                put("device_id", deviceId)
                put("email", cleanEmail)
                put("free_starter_claimed", true)
            }.toString()

            supabaseClient.upsert("device_registrations", payload, onConflict = "device_id")
        } catch (e: Exception) {
            Timber.w(e, "Error updating free_starter_claimed on Supabase")
        }
    }

    /**
     * Records or updates the device association in local preferences and Supabase.
     */
    suspend fun recordDeviceRegistration(email: String, shopName: String? = null) = withContext(Dispatchers.IO) {
        val deviceId = getAndroidId()
        if (deviceId == "UNKNOWN_DEVICE" || deviceId.isBlank()) return@withContext

        val cleanEmail = email.trim().lowercase()

        // If no primary registered email is saved yet on device, save this one
        val currentRegistered = preferencesManager.registeredEmailFlow.firstOrNull()
        if (currentRegistered.isNullOrBlank()) {
            preferencesManager.setRegisteredDevice(deviceId, cleanEmail)
        }

        // Persist to Supabase
        try {
            val payload = buildJsonObject {
                put("device_id", deviceId)
                put("email", cleanEmail)
                if (!shopName.isNullOrBlank()) {
                    put("shop_name", shopName.trim())
                }
            }.toString()

            val res = supabaseClient.upsert("device_registrations", payload, onConflict = "device_id")
            if (res.isFailure) {
                Timber.w("Failed to save device registration in Supabase: ${res.exceptionOrNull()?.message}")
            } else {
                Timber.i("Device $deviceId registered for $cleanEmail in Supabase")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error recording device registration in Supabase")
        }
    }
}
