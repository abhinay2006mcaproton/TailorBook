package com.tailorbook.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * Contract for subscription preferences and state storage.
 * Enables clean inversion of control and unit testability.
 */
interface SubscriptionPreferences {
    val isPremiumFlow: Flow<Boolean>
    val subscriptionPlanFlow: Flow<String>
    val subscriptionExpiryFlow: Flow<String>
    suspend fun setSubscription(isPremium: Boolean, plan: String, expiry: String)
    suspend fun cancelSubscription()
}
