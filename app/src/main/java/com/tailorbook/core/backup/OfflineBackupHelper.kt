package com.tailorbook.core.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.tailorbook.core.database.TailorBookDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OfflineBackupHelper {

    /**
     * Exports local customer profiles and orders into a structured JSON backup file.
     * Saved in internal cache and prepared for Storage Access Framework / Sharesheet.
     */
    suspend fun exportShopData(
        context: Context,
        database: TailorBookDatabase,
        shopName: String,
        shopId: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val customers = database.customerDao().getActiveCustomers(shopId).first()
            val orders = database.orderDao().getAllOrdersWithDetails(shopId).first()

            val backupPayload = buildJsonObject {
                put("app", "TailorBook")
                put("version", "1.0.0")
                put("shop_name", shopName)
                put("exported_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("total_customers", customers.size)
                put("total_orders", orders.size)

                put("customers", buildJsonArray {
                    customers.forEach { c ->
                        add(buildJsonObject {
                            put("id", c.id)
                            put("name", c.name)
                            put("phone", c.primaryPhone)
                            put("alt_phone", c.secondaryPhone ?: "")
                            put("gender", c.gender)
                            put("address", c.address ?: "")
                            put("notes", c.notes ?: "")
                        })
                    }
                })

                put("orders", buildJsonArray {
                    orders.forEach { o ->
                        add(buildJsonObject {
                            put("order_number", o.order.orderNumber)
                            put("customer_id", o.order.customerId)
                            put("status", o.order.status)
                            put("total_amount", o.order.totalAmount)
                            put("advance_amount", o.order.advanceAmount)
                            put("balance_amount", o.order.balanceAmount)
                            put("delivery_date", o.order.promisedDeliveryDate)
                            put("items_count", o.items.size)
                        })
                    }
                })
            }

            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            val timeTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val safeShopName = shopName.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val backupFile = File(dir, "TailorBook_${safeShopName}_$timeTag.json")
            backupFile.writeText(backupPayload.toString())
            backupFile
        }
    }

    /**
     * Launches the system share chooser so the tailor can save the backup file
     * directly to their phone's Downloads folder, Google Drive, or WhatsApp.
     */
    fun shareBackupFile(context: Context, file: File, shopName: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "TailorBook Offline Backup - $shopName")
            putExtra(
                Intent.EXTRA_TEXT,
                "Here is your offline database backup for $shopName.\n\nYou can save this file to your phone's Downloads folder, Google Drive, or send it to yourself on WhatsApp."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Save / Export Backup")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
