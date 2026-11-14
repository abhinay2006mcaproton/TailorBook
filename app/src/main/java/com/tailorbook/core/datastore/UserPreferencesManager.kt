package com.tailorbook.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property on Context for DataStore singleton
val Context.tailorBookDataStore: DataStore<Preferences> by preferencesDataStore(name = "tailorbook_prefs")

/**
 * DataStore preferences manager for TailorBook.
 * Stores lightweight session, onboarding flags, shop settings, and localization choices.
 */
class UserPreferencesManager(private val context: Context) : SubscriptionPreferences {

    companion object {
        val KEY_SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_SHOP_NAME = stringPreferencesKey("shop_name")
        val KEY_OWNER_NAME = stringPreferencesKey("owner_name")
        val KEY_OWNER_PHONE = stringPreferencesKey("owner_phone")
        val KEY_BUSINESS_TYPE = stringPreferencesKey("business_type")
        val KEY_OPENING_TIME = stringPreferencesKey("opening_time")
        val KEY_CLOSING_TIME = stringPreferencesKey("closing_time")
        val KEY_WEEKLY_OFF = stringPreferencesKey("weekly_off")
        val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val KEY_MEASUREMENT_UNIT = stringPreferencesKey("measurement_unit")
        val KEY_DELIVERY_LEAD_DAYS = stringPreferencesKey("delivery_lead_days")
        val KEY_SHOP_ADDRESS = stringPreferencesKey("shop_address")
        val KEY_GSTIN = stringPreferencesKey("shop_gstin")
        val KEY_UPI_ID = stringPreferencesKey("shop_upi_id")
        val KEY_ONE_TIME_BACKUP_EXPIRY = longPreferencesKey("one_time_backup_expiry")
        val KEY_CONFIG_COMPLETED = booleanPreferencesKey("config_completed")
        val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        val KEY_SUBSCRIPTION_PLAN = stringPreferencesKey("subscription_plan")
        val KEY_SUBSCRIPTION_EXPIRY = stringPreferencesKey("subscription_expiry")
        val KEY_RELEASE_DB_CLEANED = booleanPreferencesKey("release_fresh_db_v1")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_LAST_LOGIN_EMAIL = stringPreferencesKey("last_login_email")
        val KEY_RESTORE_PENDING = booleanPreferencesKey("restore_pending")
        val KEY_REGISTERED_DEVICE_ID = stringPreferencesKey("registered_device_id")
        val KEY_REGISTERED_EMAIL = stringPreferencesKey("registered_email")
        val KEY_DEVICE_FREE_STARTER_CONSUMED = booleanPreferencesKey("device_free_starter_consumed")
    }

    /** Stream of whether this device has already consumed its initial free starter customer quota */
    val isDeviceFreeStarterConsumedFlow: Flow<Boolean> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_DEVICE_FREE_STARTER_CONSUMED] ?: false
        }

    suspend fun setDeviceFreeStarterConsumed(consumed: Boolean) {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_DEVICE_FREE_STARTER_CONSUMED] = consumed
        }
    }

    /** Stream of the device-registered identifier */
    val registeredDeviceFlow: Flow<String?> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_REGISTERED_DEVICE_ID]
        }

    /** Stream of the email registered on this device */
    val registeredEmailFlow: Flow<String?> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_REGISTERED_EMAIL]
        }

    suspend fun setRegisteredDevice(deviceId: String, email: String) {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_REGISTERED_DEVICE_ID] = deviceId
            prefs[KEY_REGISTERED_EMAIL] = email
        }
    }

    /** Stream of the active user email */
    val userEmailFlow: Flow<String?> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_USER_EMAIL]
        }

    /** Stream of the owner phone */
    val ownerPhoneFlow: Flow<String?> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_OWNER_PHONE]
        }

    /** Stream of the owner name */
    val ownerNameFlow: Flow<String?> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_OWNER_NAME]
        }

    /** Stream indicating whether user is eligible for Supabase cloud sync (Subscribed, VIP, or rewarded one-time token) */
    val isSyncEligibleFlow: Flow<Boolean> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val isPremium = preferences[KEY_IS_PREMIUM] == true
            val plan = preferences[KEY_SUBSCRIPTION_PLAN] ?: "FREE"
            val phone = preferences[KEY_OWNER_PHONE]
            val email = preferences[KEY_USER_EMAIL]
            val oneTimeExpiry = preferences[KEY_ONE_TIME_BACKUP_EXPIRY] ?: 0L
            val hasOneTimeToken = System.currentTimeMillis() < oneTimeExpiry
            isPremium || plan == "VIP_COMPLIMENTARY" || hasOneTimeToken || com.tailorbook.core.billing.VipAccessManager.isWhitelisted(phone = phone, email = email)
        }

    /** Checks whether the user is eligible for Supabase cloud sync (Subscribed, VIP, or rewarded one-time token) */
    suspend fun isSyncEligible(): Boolean {
        val prefs = context.tailorBookDataStore.data.first()
        val isPremium = prefs[KEY_IS_PREMIUM] == true
        val plan = prefs[KEY_SUBSCRIPTION_PLAN] ?: "FREE"
        val phone = prefs[KEY_OWNER_PHONE]
        val email = prefs[KEY_USER_EMAIL]
        val oneTimeExpiry = prefs[KEY_ONE_TIME_BACKUP_EXPIRY] ?: 0L
        val hasOneTimeToken = System.currentTimeMillis() < oneTimeExpiry
        return isPremium || plan == "VIP_COMPLIMENTARY" || hasOneTimeToken || com.tailorbook.core.billing.VipAccessManager.isWhitelisted(phone = phone, email = email)
    }

    /** Grants a 15-minute temporary cloud sync window after a verified rewarded ad view */
    suspend fun grantOneTimeCloudBackup() {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_ONE_TIME_BACKUP_EXPIRY] = System.currentTimeMillis() + 15 * 60 * 1000L
        }
    }

    /** Stream of the last entered login email/phone hint */
    val lastLoginEmailFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_LAST_LOGIN_EMAIL] ?: ""
        }

    /** Stream indicating whether an existing user must restore their cloud database before using the app */
    val isRestorePendingFlow: Flow<Boolean> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_RESTORE_PENDING] ?: false
        }

    suspend fun setRestorePending(pending: Boolean) {
        context.tailorBookDataStore.edit { preferences ->
            preferences[KEY_RESTORE_PENDING] = pending
        }
    }

    /** Stream indicating whether the release database has been freshly initialized */
    val isReleaseDbCleanedFlow: Flow<Boolean> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_RELEASE_DB_CLEANED] ?: false
        }

    suspend fun setReleaseDbCleaned(cleaned: Boolean) {
        context.tailorBookDataStore.edit { preferences ->
            preferences[KEY_RELEASE_DB_CLEANED] = cleaned
        }
    }

    /** Stream of the currently selected app language ('en' or 'te', defaults to 'en') */
    val selectedLanguageFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_SELECTED_LANGUAGE] ?: "en"
        }

    /** Stream indicating whether onboarding has been completed */
    val isOnboardingCompletedFlow: Flow<Boolean> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: false
        }

    /** Stream indicating whether the user/shop is logged in */
    val isLoggedInFlow: Flow<Boolean> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_IS_LOGGED_IN] ?: false
        }

    /** Stream of the active user/shop ID */
    val userIdFlow: Flow<String?> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_USER_ID]
        }

    /** Stream of the shop name */
    val shopNameFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_SHOP_NAME] ?: "My Boutique"
        }

    /** Stream of whether the user has an active Pro Boutique subscription */
    override val isPremiumFlow: Flow<Boolean> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_IS_PREMIUM] ?: false
        }

    /** Stream of the active subscription plan ("FREE", "MONTHLY", "YEARLY") */
    override val subscriptionPlanFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_SUBSCRIPTION_PLAN] ?: "FREE"
        }

    /** Stream of the subscription expiry date string */
    override val subscriptionExpiryFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_SUBSCRIPTION_EXPIRY] ?: ""
        }

    /** Stream of the configured weekly off day (e.g. "Sunday", "Monday", ..., "None") */
    val weeklyOffFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_WEEKLY_OFF] ?: "Sunday"
        }

    /** Stream of the default measurement unit ("INCH" or "CM") */
    val measurementUnitFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_MEASUREMENT_UNIT] ?: "INCH"
        }

    val openingTimeFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_OPENING_TIME] ?: "09:30 AM"
        }

    val closingTimeFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_CLOSING_TIME] ?: "08:30 PM"
        }

    val deliveryLeadDaysFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_DELIVERY_LEAD_DAYS] ?: "5"
        }

    val shopAddressFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_SHOP_ADDRESS] ?: ""
        }

    val gstinFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_GSTIN] ?: ""
        }

    /** Stream of the boutique's UPI ID / VPA */
    val shopUpiIdFlow: Flow<String> = context.tailorBookDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_UPI_ID] ?: ""
        }

    suspend fun setShopUpiId(upiId: String) {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_UPI_ID] = upiId.trim()
        }
    }

    suspend fun setSelectedLanguage(languageCode: String) {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_SELECTED_LANGUAGE] = languageCode
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setLoggedIn(isLoggedIn: Boolean, userId: String? = null, email: String? = null) {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = isLoggedIn
            if (userId != null) {
                prefs[KEY_USER_ID] = userId
            }
            if (!email.isNullOrBlank()) {
                prefs[KEY_USER_EMAIL] = email
                prefs[KEY_LAST_LOGIN_EMAIL] = email
            }
        }
    }

    suspend fun setUserEmail(email: String) {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_LAST_LOGIN_EMAIL] = email
        }
    }

    suspend fun saveShopProfile(
        shopName: String,
        ownerName: String,
        ownerPhone: String,
        businessType: String,
        upiId: String = ""
    ) {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_SHOP_NAME] = shopName
            prefs[KEY_OWNER_NAME] = ownerName
            prefs[KEY_OWNER_PHONE] = ownerPhone
            prefs[KEY_BUSINESS_TYPE] = businessType
            if (upiId.isNotBlank()) {
                prefs[KEY_UPI_ID] = upiId.trim()
            }
            prefs[KEY_IS_LOGGED_IN] = true
            if (prefs[KEY_USER_ID].isNullOrBlank()) {
                prefs[KEY_USER_ID] = java.util.UUID.randomUUID().toString()
            }
        }
    }

    suspend fun saveShopWorkingConfig(
        openingTime: String,
        closingTime: String,
        weeklyOff: String,
        currencySymbol: String = "₹",
        measurementUnit: String = "INCH",
        leadDays: String = "5",
        address: String = "",
        gstin: String = "",
        upiId: String = ""
    ) {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_OPENING_TIME] = openingTime
            prefs[KEY_CLOSING_TIME] = closingTime
            prefs[KEY_WEEKLY_OFF] = weeklyOff
            prefs[KEY_CURRENCY_SYMBOL] = currencySymbol
            prefs[KEY_MEASUREMENT_UNIT] = measurementUnit
            prefs[KEY_DELIVERY_LEAD_DAYS] = leadDays
            prefs[KEY_SHOP_ADDRESS] = address
            prefs[KEY_GSTIN] = gstin
            if (upiId.isNotBlank()) {
                prefs[KEY_UPI_ID] = upiId.trim()
            }
            prefs[KEY_CONFIG_COMPLETED] = true
        }
    }

    suspend fun clearSession() {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
            prefs[KEY_RESTORE_PENDING] = false
            prefs.remove(KEY_USER_ID)
        }
    }

    override suspend fun setSubscription(isPremium: Boolean, plan: String, expiry: String) {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = isPremium
            prefs[KEY_SUBSCRIPTION_PLAN] = plan
            prefs[KEY_SUBSCRIPTION_EXPIRY] = expiry
        }
    }

    override suspend fun cancelSubscription() {
        context.tailorBookDataStore.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = false
            prefs[KEY_SUBSCRIPTION_PLAN] = "FREE"
            prefs[KEY_SUBSCRIPTION_EXPIRY] = ""
        }
    }
}
