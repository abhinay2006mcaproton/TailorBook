package com.tailorbook.feature.orders.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility helper for generating customer bills, estimates, and karigar work orders
 * with complete garment breakdowns (e.g. 2x Blouse * 600 = 1200, 1x Churidar * 800 = 800, Total 3 Garments: 2000).
 */
object OrderShareHelper {

    /**
     * Builds a formatted plain-text receipt / bill suitable for WhatsApp, SMS, or Print.
     */
    fun buildOrderShareText(
        shopName: String,
        orderNumber: String,
        customerName: String,
        customerPhone: String?,
        status: String,
        deliveryDateMs: Long?,
        items: List<OrderItemEntity>,
        totalAmount: Double,
        paidAmount: Double,
        balanceAmount: Double,
        includeMeasurements: Boolean = false,
        isPremium: Boolean = false,
        upiId: String? = null
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val deliveryStr = deliveryDateMs?.let { dateFormat.format(Date(it)) } ?: "Standard"
        val totalGarments = items.sumOf { it.quantity }.coerceAtLeast(1)

        return buildString {
            appendLine("👗 *$shopName*")
            appendLine("🧾 *Order Details & Bill*")
            appendLine("────────────────────────")
            appendLine("📋 *Order #:* $orderNumber")
            appendLine("👤 *Customer:* $customerName")
            if (!customerPhone.isNullOrBlank()) {
                appendLine("📱 *Phone:* $customerPhone")
            }
            appendLine("📅 *Delivery Date:* $deliveryStr")
            appendLine("📊 *Status:* $status")
            appendLine("────────────────────────")
            appendLine("✂️ *GARMENTS / ITEMS:*")

            if (items.isEmpty()) {
                appendLine("• 1x Custom Stitching = ₹${totalAmount.toInt()}")
            } else {
                items.forEach { item ->
                    val lineTotal = (item.quantity * item.unitPrice).toInt()
                    val unitPriceInt = item.unitPrice.toInt()
                    appendLine("• *${item.quantity}x ${item.garmentName}* @ ₹$unitPriceInt = *₹$lineTotal*")

                    val cleanCustNote = com.tailorbook.core.util.VoiceNoteHelper.cleanInstructions(item.stitchingInstructions)
                    if (cleanCustNote.isNotBlank()) {
                        appendLine("   ↳ _Note: ${cleanCustNote}_")
                    }
                    if (!item.fabricDetails.isNullOrBlank()) {
                        appendLine("   ↳ _Fabric: ${item.fabricDetails}_")
                    }

                    if (includeMeasurements && !item.measurementsJson.isNullOrBlank()) {
                        val points = MeasurementRepositoryImpl.parseMeasurements(item.measurementsJson)
                        if (points.isNotEmpty()) {
                            val measStr = points.entries.joinToString(", ") { "${it.key}: ${it.value}\"" }
                            appendLine("   ↳ 📏 _Measurements: $measStr (${item.unit})_")
                        }
                    }
                }
            }

            appendLine("────────────────────────")
            appendLine("📦 *Total Garments:* $totalGarments ${if (totalGarments == 1) "Garment" else "Garments"}")
            appendLine("💰 *Total Amount:* ₹${totalAmount.toInt()}")
            appendLine("✅ *Advance Received:* ₹${paidAmount.toInt()}")
            val balanceInt = balanceAmount.toInt()
            if (balanceInt > 0) {
                appendLine("⚠️ *Balance Pending:* ₹$balanceInt")
                if (!upiId.isNullOrBlank()) {
                    appendLine("💳 *Pay Balance via UPI:* $upiId")
                }
            } else {
                appendLine("🎉 *Payment Status:* Fully Settled")
            }
            appendLine("────────────────────────")
            appendLine("✨ _Thank you for choosing $shopName!_")
            if (!isPremium) {
                appendLine("────────────────────────")
                appendLine("📱 _Generated via TailorBook App_")
            }
        }
    }

    /**
     * Builds a workshop cutting slip / job card for cutting masters & tailors containing ONLY
     * garments, stitching instructions, and exact body measurements.
     * EXPLICITLY OMITS ALL PRICING, ADVANCE, AND FINANCIAL BALANCES.
     */
    fun buildCuttingSlipText(
        shopName: String,
        orderNumber: String,
        customerName: String,
        deliveryDateMs: Long?,
        items: List<OrderItemEntity>,
        assignedMaster: String? = null,
        isPremium: Boolean = false
    ): String {
        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
        val deliveryStr = deliveryDateMs?.let { dateFormat.format(Date(it)) } ?: "Standard"
        val totalGarments = items.sumOf { it.quantity }.coerceAtLeast(1)

        return buildString {
            appendLine("✂️ *$shopName*")
            appendLine("🪡 *WORKSHOP CUTTING SLIP / JOB CARD*")
            appendLine("────────────────────────")
            appendLine("📋 *Order #:* $orderNumber")
            appendLine("👤 *Customer Ref:* $customerName")
            appendLine("📅 *Target Delivery:* $deliveryStr")
            if (!assignedMaster.isNullOrBlank()) {
                appendLine("👷 *Assigned Cutting Master:* $assignedMaster")
            }
            appendLine("📦 *Total Garments:* $totalGarments")
            appendLine("────────────────────────")
            appendLine("📐 *GARMENTS & STITCHING SPECS:*")

            if (items.isEmpty()) {
                appendLine("• 1x Custom Stitching (Refer to pattern book)")
            } else {
                items.forEachIndexed { idx, item ->
                    appendLine("")
                    appendLine("*[Item #${idx + 1}] ${item.quantity}x ${item.garmentName}*")
                    appendLine("Template: ${item.garmentTemplateId}")

                    val cleanNote = com.tailorbook.core.util.VoiceNoteHelper.cleanInstructions(item.stitchingInstructions)
                    if (cleanNote.isNotBlank()) {
                        appendLine("↳ *Instructions:* $cleanNote")
                    }
                    if (!item.fabricDetails.isNullOrBlank()) {
                        appendLine("↳ *Fabric:* ${item.fabricDetails}")
                    }

                    val hasAudio = com.tailorbook.core.util.VoiceNoteHelper.extractVoiceNote(item.stitchingInstructions) != null
                    if (hasAudio) {
                        appendLine("↳ 🎙️ *Audio Voice Note:* Attached in TailorBook App")
                    }

                    if (!item.measurementsJson.isNullOrBlank()) {
                        val points = MeasurementRepositoryImpl.parseMeasurements(item.measurementsJson)
                        if (points.isNotEmpty()) {
                            appendLine("↳ 📏 *Measurements (${item.unit}):*")
                            points.forEach { (key, value) ->
                                appendLine("     • $key: $value\"")
                            }
                        }
                    }
                }
            }

            appendLine("────────────────────────")
            appendLine("⚠️ *Cutting Master Note:* Double-check margins & seam allowances before final trial.")
            if (!isPremium) {
                appendLine("────────────────────────")
                appendLine("🪡 _Printed via TailorBook App_")
            }
        }
    }

    /** Backward-compatible alias for buildCuttingSlipText */
    fun buildKarigarJobCardText(
        shopName: String,
        orderNumber: String,
        customerName: String,
        deliveryDateMs: Long?,
        items: List<OrderItemEntity>,
        assignedKarigar: String? = null
    ): String = buildCuttingSlipText(
        shopName = shopName,
        orderNumber = orderNumber,
        customerName = customerName,
        deliveryDateMs = deliveryDateMs,
        items = items,
        assignedMaster = assignedKarigar
    )

    /**
     * Builds a WhatsApp alert message when an order is marked READY for pickup/trial.
     */
    fun buildOrderReadyAlertText(
        shopName: String,
        orderNumber: String,
        customerName: String,
        items: List<OrderItemEntity>,
        balanceAmount: Double,
        shopPhone: String? = null,
        isPremium: Boolean = false
    ): String {
        val garmentSummary = if (items.isNotEmpty()) {
            items.joinToString(", ") { "${it.quantity}x ${it.garmentName}" }
        } else "Garment(s)"

        val balanceInt = balanceAmount.toInt()

        return buildString {
            appendLine("👗 *$shopName*")
            appendLine("✨ *Good News, $customerName!*")
            appendLine("────────────────────────")
            appendLine("🪡 Your order (*#$orderNumber*) is stitched and *READY for trial & pickup*!")
            appendLine("📦 *Items:* $garmentSummary")
            appendLine("────────────────────────")
            if (balanceInt > 0) {
                appendLine("💰 *Pending Balance:* ₹$balanceInt (Cash, GPay, PhonePe, Paytm accepted)")
            } else {
                appendLine("🎉 *Payment Status:* Fully Settled")
            }
            if (!shopPhone.isNullOrBlank()) {
                appendLine("📞 *Contact:* $shopPhone")
            }
            appendLine("────────────────────────")
            appendLine("✨ _Please visit us soon for your trial! Thank you!_")
            if (!isPremium) {
                appendLine("────────────────────────")
                appendLine("📱 _Sent via TailorBook App_")
            }
        }
    }

    /**
     * Share text via Android System Chooser (WhatsApp, Telegram, SMS, Gmail, etc.)
     */
    fun shareViaSystemSheet(context: Context, text: String, subject: String = "TailorBook Order Details") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Order Details via"))
    }

    /**
     * Share text directly to WhatsApp contact or WhatsApp conversation picker.
     */
    fun shareViaWhatsApp(context: Context, text: String, phoneNumber: String?) {
        val cleaned = phoneNumber?.replace(Regex("[^0-9]"), "") ?: ""
        val fullPhone = if (cleaned.length == 10) "91$cleaned" else cleaned
        val url = if (fullPhone.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$fullPhone&text=${Uri.encode(text)}"
        } else {
            "https://api.whatsapp.com/send?text=${Uri.encode(text)}"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to system share chooser if WhatsApp direct URL cannot be resolved
            shareViaSystemSheet(context, text, "TailorBook Order Details")
        }
    }

    /**
     * Copies the formatted bill text to the system clipboard.
     */
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Order Details", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Order bill copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
