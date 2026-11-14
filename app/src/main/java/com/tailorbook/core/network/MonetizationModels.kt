package com.tailorbook.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase customer entitlement entity.
 */
@Serializable
data class CustomerEntitlementDto(
    @SerialName("shop_id") val shopId: String,
    @SerialName("tier") val tier: String = "FREE_STARTER",
    @SerialName("max_customers") val maxCustomers: Int = 10,
    @SerialName("unlocked_ad_slots") val unlockedAdSlots: Int = 0,
    @SerialName("is_unlimited") val isUnlimited: Boolean = false,
    @SerialName("is_vip_active") val isVipActive: Boolean = false,
    @SerialName("vip_granted_by") val vipGrantedBy: String? = null,
    @SerialName("vip_granted_at") val vipGrantedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Supabase reward transaction entity.
 */
@Serializable
data class RewardTransactionDto(
    @SerialName("id") val id: String? = null,
    @SerialName("shop_id") val shopId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("reward_token") val rewardToken: String,
    @SerialName("ad_unit_id") val adUnitId: String,
    @SerialName("reward_type") val rewardType: String = "CUSTOMER_SLOT",
    @SerialName("reward_amount") val rewardAmount: Int = 1,
    @SerialName("is_consumed") val isConsumed: Boolean = false,
    @SerialName("consumed_at") val consumedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * Supabase shop subscription entity.
 */
@Serializable
data class ShopSubscriptionDto(
    @SerialName("id") val id: String? = null,
    @SerialName("shop_id") val shopId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("product_id") val productId: String,
    @SerialName("purchase_token") val purchaseToken: String,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("purchase_state") val purchaseState: String = "PURCHASED",
    @SerialName("is_acknowledged") val isAcknowledged: Boolean = true,
    @SerialName("status") val status: String = "ACTIVE",
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_vip_complimentary") val isVipComplimentary: Boolean = false,
    @SerialName("vip_reason") val vipReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Supabase VIP whitelisted account entity.
 */
@Serializable
data class VipWhitelistedAccountDto(
    @SerialName("id") val id: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("shop_name") val shopName: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)
