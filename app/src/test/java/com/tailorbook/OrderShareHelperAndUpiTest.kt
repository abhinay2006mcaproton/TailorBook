package com.tailorbook

import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import com.tailorbook.feature.orders.presentation.OrderShareHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderShareHelperAndUpiTest {

    @Test
    fun `buildOrderShareText includes UPI payment line when pending balance exists and UPI ID provided`() {
        val items = listOf(
            OrderItemEntity(
                id = "item-1",
                orderId = "ord-1",
                garmentTemplateId = "BLOUSE",
                garmentName = "Designer Saree Blouse",
                quantity = 1,
                unitPrice = 1200.0,
                stitchingInstructions = "Padded blouse",
                fabricDetails = "Silk"
            )
        )

        val billText = OrderShareHelper.buildOrderShareText(
            shopName = "Royal Boutique",
            orderNumber = "TB-501",
            customerName = "Priyanka Sharma",
            customerPhone = "9876543210",
            status = "STITCHING",
            deliveryDateMs = System.currentTimeMillis() + 86400000L * 3,
            items = items,
            totalAmount = 1200.0,
            paidAmount = 500.0,
            balanceAmount = 700.0,
            includeMeasurements = false,
            isPremium = true,
            upiId = "royalboutique@upi"
        )

        assertTrue(billText.contains("⚠️ *Balance Pending:* ₹700"))
        assertTrue(billText.contains("💳 *Pay Balance via UPI:* royalboutique@upi"))
        assertFalse(billText.contains("Generated via TailorBook App")) // White-label check for premium
    }

    @Test
    fun `buildOrderShareText omits UPI payment line when balance is zero`() {
        val items = listOf(
            OrderItemEntity(
                id = "item-2",
                orderId = "ord-2",
                garmentTemplateId = "KURTI",
                garmentName = "A-Line Kurti",
                quantity = 1,
                unitPrice = 800.0,
                stitchingInstructions = null,
                fabricDetails = "Cotton"
            )
        )

        val billText = OrderShareHelper.buildOrderShareText(
            shopName = "Royal Boutique",
            orderNumber = "TB-502",
            customerName = "Priyanka Sharma",
            customerPhone = "9876543210",
            status = "READY",
            deliveryDateMs = System.currentTimeMillis(),
            items = items,
            totalAmount = 800.0,
            paidAmount = 800.0,
            balanceAmount = 0.0,
            includeMeasurements = false,
            isPremium = false,
            upiId = "royalboutique@upi"
        )

        assertTrue(billText.contains("🎉 *Payment Status:* Fully Settled"))
        assertFalse(billText.contains("💳 *Pay Balance via UPI:*"))
        assertTrue(billText.contains("📱 _Generated via TailorBook App_")) // Watermark for free tier
    }

    @Test
    fun `buildCuttingSlipText avoids the word Karigar and uses Master or Cutting Slip`() {
        val items = listOf(
            OrderItemEntity(
                id = "item-3",
                orderId = "ord-3",
                garmentTemplateId = "LEHENGA",
                garmentName = "Bridal Lehenga",
                quantity = 1,
                unitPrice = 4500.0,
                stitchingInstructions = "Can-can net with heavy flair",
                fabricDetails = "Velvet"
            )
        )

        val slipText = OrderShareHelper.buildCuttingSlipText(
            shopName = "Royal Boutique",
            orderNumber = "TB-503",
            customerName = "Meera Bai",
            deliveryDateMs = System.currentTimeMillis() + 86400000L * 5,
            items = items,
            assignedMaster = "Ramesh Master",
            isPremium = true
        )

        assertTrue(slipText.contains("WORKSHOP CUTTING SLIP / JOB CARD"))
        assertTrue(slipText.contains("Assigned Cutting Master"))
        assertTrue(slipText.contains("Ramesh Master"))
        assertFalse("Output slip should not contain the word Karigar", slipText.contains("Karigar", ignoreCase = true))
    }
}
