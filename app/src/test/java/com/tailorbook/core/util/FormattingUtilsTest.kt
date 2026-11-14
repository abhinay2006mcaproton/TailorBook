package com.tailorbook.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingUtilsTest {

    @Test
    fun formatCurrency_formatsIndianRupeeCorrectly() {
        assertEquals("₹0", FormattingUtils.formatCurrency(0.0))
        assertEquals("₹500", FormattingUtils.formatCurrency(500.0))
        assertEquals("₹1,500", FormattingUtils.formatCurrency(1500.0))
        assertEquals("₹25,000", FormattingUtils.formatCurrency(25000.0))
        assertEquals("₹1,50,000", FormattingUtils.formatCurrency(150000.0))
    }

    @Test
    fun formatCurrency_integerOverload_worksCorrectly() {
        assertEquals("₹1,200", FormattingUtils.formatCurrency(1200))
        assertEquals("₹900", FormattingUtils.formatCurrency(900))
    }

    @Test
    fun formatPersonName_capitalizesEachWord() {
        assertEquals("Ananya Reddy", FormattingUtils.formatPersonName("ananya reddy"))
        assertEquals("Ananya Reddy", FormattingUtils.formatPersonName("ANANYA REDDY"))
        assertEquals("P. Srinivas Rao", FormattingUtils.formatPersonName("p. srinivas rao"))
        assertEquals("Sita", FormattingUtils.formatPersonName("sita"))
        assertEquals("", FormattingUtils.formatPersonName("   "))
    }
}
