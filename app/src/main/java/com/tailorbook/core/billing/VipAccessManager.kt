package com.tailorbook.core.billing

import com.tailorbook.core.datastore.SubscriptionPreferences
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Manages VIP Complimentary Access for selected users.
 * Allows designated tailors, test accounts, and owners to access the entire application
 * completely free with unlimited customers and zero advertisements.
 */
object VipAccessManager {

    // Configurable list of selected VIP phone numbers (Indian 10-digit mobile numbers)
    private val VIP_WHITELISTED_PHONES = mutableSetOf(
        "9876543210",
        "9848012345",
        "9988776655"
    )

    // Configurable list of selected VIP email addresses
    private val VIP_WHITELISTED_EMAILS = mutableSetOf(
        "owner@tailorbook.com",
        "vip@tailorbook.com",
        "tester@tailorbook.com"
    )

    // Secret VIP activation promo codes
    private val VALID_VIP_PROMO_CODES = setOf(
        "TAILORVIP2026",
        "LIFETIMEFREE",
        "SPECIALTAILOR"
    )

    /**
     * Checks if a phone number or email belongs to the selected VIP free access whitelist.
     */
    fun isWhitelisted(phone: String? = null, email: String? = null): Boolean {
        val cleanPhone = phone?.trim()?.replace("+91", "")?.replace(" ", "") ?: ""
        val cleanEmail = email?.trim()?.lowercase() ?: ""

        val isPhoneVip = cleanPhone.isNotEmpty() && VIP_WHITELISTED_PHONES.contains(cleanPhone)
        val isEmailVip = cleanEmail.isNotEmpty() && VIP_WHITELISTED_EMAILS.contains(cleanEmail)

        return isPhoneVip || isEmailVip
    }

    /**
     * Dynamically adds a phone number to the VIP whitelist.
     */
    fun addVipPhone(phone: String) {
        val cleanPhone = phone.trim().replace("+91", "").replace(" ", "")
        if (cleanPhone.isNotEmpty()) {
            VIP_WHITELISTED_PHONES.add(cleanPhone)
            Timber.i("Added VIP whitelisted phone: $cleanPhone")
        }
    }

    /**
     * Dynamically adds an email to the VIP whitelist.
     */
    fun addVipEmail(email: String) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isNotEmpty()) {
            VIP_WHITELISTED_EMAILS.add(cleanEmail)
            Timber.i("Added VIP whitelisted email: $cleanEmail")
        }
    }

    /**
     * Validates a VIP promo code and activates lifetime free access if valid.
     */
    suspend fun redeemVipCode(code: String, preferences: SubscriptionPreferences): Boolean {
        val normalizedCode = code.trim().uppercase()
        if (VALID_VIP_PROMO_CODES.contains(normalizedCode)) {
            preferences.setSubscription(
                isPremium = true,
                plan = "VIP_COMPLIMENTARY",
                expiry = "Lifetime Free"
            )
            Timber.i("VIP promo code successfully redeemed: $normalizedCode")
            return true
        }
        return false
    }

    /**
     * Automatically applies VIP complimentary access to subscription preferences if the user is in the whitelist.
     */
    suspend fun applyVipStatusIfEligible(
        phone: String?,
        email: String?,
        preferences: SubscriptionPreferences
    ) {
        if (isWhitelisted(phone = phone, email = email)) {
            val isAlreadyPremium = preferences.isPremiumFlow.first()
            if (!isAlreadyPremium) {
                preferences.setSubscription(
                    isPremium = true,
                    plan = "VIP_COMPLIMENTARY",
                    expiry = "Lifetime Free"
                )
                Timber.i("Applied VIP Complimentary Access for: phone=$phone, email=$email")
            }
        }
    }
}
