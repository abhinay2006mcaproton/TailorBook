package com.tailorbook.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.WarmIvory
import java.util.Locale

/**
 * Utility for generating and displaying standard Indian UPI payment QR codes (PhonePe, GPay, Paytm, BHIM).
 */
object UpiQrHelper {

    /**
     * Builds standard NPCI UPI payment deep link string.
     * Scheme: upi://pay?pa=<vpa>&pn=<name>&am=<amount>&cu=INR&tn=<note>
     */
    fun buildUpiString(
        upiId: String,
        payeeName: String,
        amount: Double,
        transactionNote: String = "TailorBook Payment"
    ): String {
        val safeUpiId = upiId.trim()
        val safePayeeName = Uri.encode(payeeName.trim())
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val safeNote = Uri.encode(transactionNote.trim())
        return "upi://pay?pa=$safeUpiId&pn=$safePayeeName&am=$formattedAmount&cu=INR&tn=$safeNote"
    }

    /**
     * Generates an Android Bitmap containing the QR code.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // TailorBook DeepTeal for dark modules, white for light
            val darkColor = Color.rgb(20, 52, 43)
            val lightColor = Color.WHITE

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) darkColor else lightColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Interactive Dialog rendering the Dynamic UPI QR Code for instant balance collection.
 */
@Composable
fun UpiPaymentQrDialog(
    shopName: String,
    orderNumber: String,
    balanceAmount: Double,
    upiId: String = "kethepalle@upi",
    titleAmountLabel: String = "Collect Amount",
    transactionNote: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val upiString = remember(upiId, shopName, balanceAmount, orderNumber, transactionNote) {
        UpiQrHelper.buildUpiString(
            upiId = upiId,
            payeeName = shopName,
            amount = balanceAmount,
            transactionNote = transactionNote ?: "Order $orderNumber Payment"
        )
    }

    val qrBitmap = remember(upiString) {
        UpiQrHelper.generateQrBitmap(upiString, sizePx = 480)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = DeepTeal, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UPI Payment QR",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CharcoalSubtle)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Customer can scan using any UPI App",
                    style = MaterialTheme.typography.labelSmall,
                    color = CharcoalSubtle
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pending balance pill
                Surface(
                    color = WarmIvory,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftGold)
                ) {
                    Text(
                        text = "$titleAmountLabel: " + FormattingUtils.formatCurrency(balanceAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // QR Code Display Card
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(CardWhite, RoundedCornerShape(12.dp))
                        .border(1.5.dp, DeepTeal.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI Payment QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                    } else {
                        Text(
                            text = "Could not generate QR",
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalSubtle
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Supported apps badge
                Text(
                    text = "GPay • PhonePe • Paytm • BHIM • Cred",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = CharcoalSubtle,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Copy UPI ID Row
                Surface(
                    color = LightBeige.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId))
                            Toast.makeText(context, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "UPI ID / VPA", style = MaterialTheme.typography.labelSmall, color = CharcoalSubtle, fontSize = 10.sp)
                            Text(text = upiId, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Charcoal)
                        }
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy UPI ID", tint = DeepTeal, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        containerColor = CardWhite,
        shape = RoundedCornerShape(16.dp)
    )
}
