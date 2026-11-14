package com.tailorbook.core.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Supabase monetization schema models.
 */
class MonetizationModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun customerEntitlementDto_serializesAndDeserializesCorrectly() {
        val dto = CustomerEntitlementDto(
            shopId = "shop-001",
            tier = "AD_SUPPORTED",
            maxCustomers = 10,
            unlockedAdSlots = 5,
            isUnlimited = false,
            isVipActive = false
        )

        val raw = json.encodeToString(dto)
        val parsed = json.decodeFromString<CustomerEntitlementDto>(raw)

        assertEquals("shop-001", parsed.shopId)
        assertEquals("AD_SUPPORTED", parsed.tier)
        assertEquals(5, parsed.unlockedAdSlots)
    }

    @Test
    fun rewardTransactionDto_preservesSingleUseToken() {
        val token = "tb_rwd_12345_67890"
        val dto = RewardTransactionDto(
            shopId = "shop-001",
            rewardToken = token,
            adUnitId = "ca-app-pub-3940256099942544/5224354917",
            isConsumed = false
        )

        val raw = json.encodeToString(dto)
        val parsed = json.decodeFromString<RewardTransactionDto>(raw)

        assertEquals(token, parsed.rewardToken)
        assertEquals(1, parsed.rewardAmount)
        assertEquals(false, parsed.isConsumed)
    }

    @Test
    fun shopSubscriptionDto_handlesVipComplimentaryField() {
        val dto = ShopSubscriptionDto(
            shopId = "shop-001",
            productId = "tailorbook_premium_annual",
            purchaseToken = "test_purchase_token_vip",
            isVipComplimentary = true,
            vipReason = "Designated beta partner"
        )

        val raw = json.encodeToString(dto)
        val parsed = json.decodeFromString<ShopSubscriptionDto>(raw)

        assertTrue(parsed.isVipComplimentary)
        assertEquals("Designated beta partner", parsed.vipReason)
    }
}
