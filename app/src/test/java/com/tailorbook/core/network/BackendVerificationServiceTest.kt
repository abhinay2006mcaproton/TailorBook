package com.tailorbook.core.network

import com.android.billingclient.api.Purchase
import com.tailorbook.core.billing.BillingConstants
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying secure server-side purchase verification and anti-replay guards.
 */
class BackendVerificationServiceTest {

    private lateinit var service: SupabaseBackendVerificationService

    @Before
    fun setUp() {
        service = SupabaseBackendVerificationService(isLiveBackendConfigured = false)
    }

    @Test
    fun validPurchase_verifiesSuccessfully() = runTest {
        val result = service.verifyPlayPurchaseToken(
            shopId = "shop-001",
            userId = "user-001",
            purchaseToken = "token_valid_123",
            productId = BillingConstants.PRODUCT_PREMIUM_MONTHLY,
            purchaseState = Purchase.PurchaseState.PURCHASED
        )

        assertTrue(result is VerificationResult.Success)
        val success = result as VerificationResult.Success
        assertEquals("PRO_MONTHLY", success.tier)
        assertTrue(success.isUnlimited)
    }

    @Test
    fun antiReplayGuard_blocksTokenReusedForDifferentShop() = runTest {
        val token = "token_shared_456"

        // First verification by shop-001 succeeds
        val result1 = service.verifyPlayPurchaseToken(
            shopId = "shop-001",
            userId = "user-001",
            purchaseToken = token,
            productId = BillingConstants.PRODUCT_PREMIUM_ANNUAL,
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        assertTrue(result1 is VerificationResult.Success)

        // Attempting to register the same token for shop-002 is rejected by anti-replay
        val result2 = service.verifyPlayPurchaseToken(
            shopId = "shop-002",
            userId = "user-002",
            purchaseToken = token,
            productId = BillingConstants.PRODUCT_PREMIUM_ANNUAL,
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        assertTrue(result2 is VerificationResult.Failed)
        val failed = result2 as VerificationResult.Failed
        assertTrue(failed.reason.contains("Replay attack detected"))
    }

    @Test
    fun unknownProductId_rejected() = runTest {
        val result = service.verifyPlayPurchaseToken(
            shopId = "shop-001",
            userId = "user-001",
            purchaseToken = "token_789",
            productId = "unknown_hacked_product",
            purchaseState = Purchase.PurchaseState.PURCHASED
        )

        assertTrue(result is VerificationResult.Failed)
        val failed = result as VerificationResult.Failed
        assertTrue(failed.reason.contains("Invalid product identifier"))
    }

    @Test
    fun rewardTokenVerification_preventsDuplicateConsumption() = runTest {
        val token = "tb_rwd_unique_token_001"

        // First verification succeeds
        val res1 = service.verifyRewardedAdToken("shop-001", token)
        assertTrue(res1 is VerificationResult.Success)

        // Duplicate verification fails
        val res2 = service.verifyRewardedAdToken("shop-001", token)
        assertTrue(res2 is VerificationResult.Failed)
        val failed = res2 as VerificationResult.Failed
        assertTrue(failed.reason.contains("already been consumed"))
    }

    @Test
    fun checkVipEligibility_identifiesWhitelistedTailors() = runTest {
        val isVip = service.checkVipEligibility("shop-001", "9876543210", null)
        assertTrue(isVip)

        val isNotVip = service.checkVipEligibility("shop-001", "9000000000", null)
        assertFalse(isNotVip)
    }
}
