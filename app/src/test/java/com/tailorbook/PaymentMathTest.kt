package com.tailorbook

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Validates financial arithmetic invariants:
 * - Order total calculation
 * - Advance deduction
 * - Multiple partial payments
 * - Exact balance matching with zero rounding errors
 */
class PaymentMathTest {

    @Test
    fun testExactBalanceCalculation() {
        val totalStitchingCost = BigDecimal("1500.00")
        val advancePaid = BigDecimal("500.00")
        val secondPayment = BigDecimal("600.00")

        val totalPaid = advancePaid.add(secondPayment)
        val balanceDue = totalStitchingCost.subtract(totalPaid)

        assertEquals(BigDecimal("1100.00"), totalPaid)
        assertEquals(BigDecimal("400.00"), balanceDue)
    }

    @Test
    fun testFullPaymentClearsBalance() {
        val totalAmount = BigDecimal("850.50")
        val advance = BigDecimal("850.50")

        val remaining = totalAmount.subtract(advance)
        assertEquals(BigDecimal("0.00"), remaining.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testRefundReversalDeduction() {
        val originalPayment = BigDecimal("1000.00")
        val reversedAmount = BigDecimal("250.00")

        val netCollected = originalPayment.subtract(reversedAmount)
        assertEquals(BigDecimal("750.00"), netCollected)
    }
}
