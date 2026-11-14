package com.tailorbook.core.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object FormattingUtils {

    /**
     * Formats an amount into standard Indian Rupee notation (e.g. ₹1,500, ₹25,000, ₹1,50,000).
     * Pure implementation of the Indian numbering system (3 digits for hundreds, groups of 2 for lakhs & crores).
     */
    fun formatCurrency(amount: Double, symbol: String = "₹"): String {
        val longVal = amount.roundToLong()
        val isNegative = longVal < 0
        val absVal = abs(longVal).toString()

        val formattedNumber = if (absVal.length <= 3) {
            absVal
        } else {
            val lastThree = absVal.takeLast(3)
            val rest = absVal.dropLast(3)
            val groupedRest = rest.reversed().chunked(2).joinToString(",").reversed()
            "$groupedRest,$lastThree"
        }

        val prefix = if (isNegative) "-$symbol" else symbol
        return "$prefix$formattedNumber"
    }

    /** Overload for Int amounts */
    fun formatCurrency(amount: Int, symbol: String = "₹"): String {
        return formatCurrency(amount.toDouble(), symbol)
    }

    /** Overload for Long amounts */
    fun formatCurrency(amount: Long, symbol: String = "₹"): String {
        return formatCurrency(amount.toDouble(), symbol)
    }

    /**
     * Capitalizes each word in a person's name (e.g. "ananya reddy" -> "Ananya Reddy").
     */
    fun formatPersonName(name: String): String {
        if (name.isBlank()) return ""
        return name.trim().split("\\s+".toRegex()).joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
    }
}
