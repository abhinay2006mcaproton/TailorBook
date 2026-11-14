package com.tailorbook.feature.orders.presentation

import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import com.tailorbook.core.util.UpiPaymentQrDialog
import com.tailorbook.core.util.VoiceNoteHelper
import com.tailorbook.feature.gallery.data.local.entity.DesignReferenceEntity
import com.tailorbook.feature.gallery.presentation.GalleryViewModel
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.feature.customers.presentation.CustomerViewModel
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.WarmIvory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Order Details Screen (TASK-08-05 / TASK-08-06).
 * Displays order timeline stepper, frozen measurements, financial breakdown, and status actions.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: String,
    orderViewModel: OrderViewModel,
    customerViewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    shopName: String = "My Boutique",
    shopUpiId: String = "",
    galleryViewModel: GalleryViewModel? = null,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbOrderWithDetails by (orderViewModel.repository?.getOrderWithDetails(orderId) ?: kotlinx.coroutines.flow.flowOf(null))
        .collectAsState(initial = null)
    val inMemOrders by orderViewModel.inMemoryOrders.collectAsState()
    val orderWithDetails = dbOrderWithDetails ?: inMemOrders.find { it.order.id == orderId }

    if (orderWithDetails == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Order Details", color = CardWhite) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CardWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepTeal)
                )
            },
            containerColor = WarmIvory
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Order not found", color = Charcoal)
            }
        }
        return
    }

    val order = orderWithDetails.order
    val customers by customerViewModel.filteredCustomers.collectAsState()
    val customer = customers.find { it.customer.id == order.customerId }
    val customerName = customer?.customer?.name ?: "Customer"
    val customerPhone = customer?.customer?.primaryPhone ?: ""

    val orderDesigns by (galleryViewModel?.getDesignsForOrder(order.id) ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    var viewingDesign by remember { mutableStateOf<DesignReferenceEntity?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null && galleryViewModel != null) {
            galleryViewModel.attachPhotosToOrder(
                context = context,
                uris = listOf(tempCameraUri!!),
                orderId = order.id,
                customerId = order.customerId
            )
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty() && galleryViewModel != null) {
            galleryViewModel.attachPhotosToOrder(
                context = context,
                uris = uris,
                orderId = order.id,
                customerId = order.customerId
            )
        }
    }

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancellationReasonText by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<OrderItemEntity?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogTab by remember { mutableStateOf(0) } // 0 = Customer Bill, 1 = Karigar Job Slip
    var includeMeasurementsInShare by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }
    var showOrderReadyNotifyDialog by remember { mutableStateOf(false) }
    var playingAudioPath by remember { mutableStateOf<String?>(null) }

    val statusSteps = listOf("NEW", "CUTTING", "STITCHING", "FINISHING", "READY", "DELIVERED")
    val currentStepIndex = statusSteps.indexOf(order.status)

    val nextStatus = when (order.status) {
        "NEW" -> "CUTTING" to "Advance to Cutting"
        "CUTTING" -> "STITCHING" to "Advance to Stitching"
        "STITCHING" -> "FINISHING" to "Advance to Finishing"
        "FINISHING" -> "READY" to "Mark Ready for Delivery"
        "READY" -> "DELIVERED" to "Mark Delivered & Closed"
        else -> null
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepTeal)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back),
                            tint = CardWhite
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Order #${order.orderNumber}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = "$customerName • ${order.status}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }

                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Order",
                            tint = SoftGold
                        )
                    }

                    if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                        IconButton(onClick = { showCancelDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel Order",
                                tint = SoftGold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (nextStatus != null) {
                Surface(
                    color = CardWhite,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        TailorPrimaryButton(
                            text = nextStatus.second,
                            onClick = {
                                val targetStatus = nextStatus.first
                                orderViewModel.advanceOrderStatus(
                                    orderId = order.id,
                                    currentStatus = order.status,
                                    newStatus = targetStatus
                                )
                                if (targetStatus == "READY") {
                                    showOrderReadyNotifyDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Workflow Stepper Timeline
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Order Status Workflow",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DeepTeal
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            statusSteps.forEachIndexed { index, step ->
                                val isPassed = index <= currentStepIndex
                                val isCurrent = index == currentStepIndex

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                color = when {
                                                    isCurrent -> DeepTeal
                                                    isPassed -> StatusGreen
                                                    else -> MutedSage.copy(alpha = 0.5f)
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPassed && !isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = CardWhite,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (isCurrent) CardWhite else CharcoalSubtle
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = step.take(4),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isCurrent) DeepTeal else CharcoalSubtle
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Customer Details Card with Quick Actions
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(DeepTeal.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customerName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Charcoal
                                )
                                Text(
                                    text = customerPhone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CharcoalSubtle
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = LightBeige,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (customerPhone.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$customerPhone"))
                                            context.startActivity(intent)
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Call", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = DeepTeal)
                                }
                            }

                            Surface(
                                color = LightBeige,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val billText = OrderShareHelper.buildOrderShareText(
                                            shopName = shopName,
                                            orderNumber = order.orderNumber,
                                            customerName = customerName,
                                            customerPhone = customerPhone,
                                            status = order.status,
                                            deliveryDateMs = order.promisedDeliveryDate,
                                            items = orderWithDetails.items,
                                            totalAmount = order.totalAmount,
                                            paidAmount = order.paidAmount,
                                            balanceAmount = order.balanceAmount,
                                            includeMeasurements = false,
                                            isPremium = isPremium,
                                            upiId = shopUpiId
                                        )
                                        OrderShareHelper.shareViaWhatsApp(context, billText, customerPhone)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "WhatsApp Bill", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = StatusGreen)
                                }
                            }

                            Surface(
                                color = LightBeige,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showShareDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Share", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = DeepTeal)
                                }
                            }
                        }

                        // UPI QR Code Generator Button for collecting balance
                        if (order.balanceAmount > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = WarmIvory,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SoftGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showUpiQrDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Collect Balance via UPI QR (₹${order.balanceAmount.toInt()})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Garment Items Card (Multi-Item Support)
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val totalQty = orderWithDetails.items.sumOf { it.quantity }.coerceAtLeast(1)
                            Text(
                                text = "Order Garments ($totalQty Items)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )

                            Surface(
                                color = StatusGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Frozen Snapshot",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StatusGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        orderWithDetails.items.forEach { item ->
                            val itemPoints = if (!item.measurementsJson.isNullOrBlank()) {
                                com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl.parseMeasurements(item.measurementsJson)
                            } else {
                                val snapshotJson = orderWithDetails.measurementSnapshot?.frozenMeasurementsJson ?: "{}"
                                com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl.parseMeasurements(snapshotJson)
                            }

                            val cleanNote = VoiceNoteHelper.cleanInstructions(item.stitchingInstructions)
                            val attachedAudio = VoiceNoteHelper.extractVoiceNote(item.stitchingInstructions)
                            val assignedMaster = VoiceNoteHelper.extractMaster(item.stitchingInstructions)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LightBeige, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.quantity}x ${item.garmentName}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal
                                    )
                                    Text(
                                        text = "₹${(item.quantity * item.unitPrice).toInt()}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal
                                    )
                                }
                                Text(
                                    text = "₹${item.unitPrice.toInt()} each • Template: ${item.garmentTemplateId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CharcoalSubtle
                                )

                                if (!assignedMaster.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = WarmIvory,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, SoftGold)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Cutting Master: $assignedMaster",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                color = DeepTeal
                                            )
                                        }
                                    }
                                }

                                if (cleanNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Instructions: $cleanNote",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Charcoal
                                    )
                                }

                                if (!attachedAudio.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val isThisPlaying = playingAudioPath == attachedAudio
                                    Surface(
                                        color = DeepTeal.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                if (isThisPlaying) {
                                                    VoiceNoteHelper.stopPlayback()
                                                    playingAudioPath = null
                                                } else {
                                                    playingAudioPath = attachedAudio
                                                    VoiceNoteHelper.playVoiceNote(attachedAudio) {
                                                        playingAudioPath = null
                                                    }
                                                }
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = DeepTeal,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isThisPlaying) "Playing Voice Note..." else "▶ Play Voice Instructions",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = DeepTeal
                                            )
                                        }
                                    }
                                }

                                if (item.fabricDetails?.isNotBlank() == true) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Fabric: ${item.fabricDetails}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CharcoalSubtle
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Garment Specific Measurements Row with Edit Option & Karigar Slip button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Garment Measurements (${itemPoints.size}):",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // 1-tap Cutting Slip button
                                        Surface(
                                            color = WarmIvory,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, SoftGold),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    val slipText = OrderShareHelper.buildCuttingSlipText(
                                                        shopName = shopName,
                                                        orderNumber = order.orderNumber,
                                                        customerName = customerName,
                                                        deliveryDateMs = order.promisedDeliveryDate,
                                                        items = listOf(item),
                                                        assignedMaster = assignedMaster,
                                                        isPremium = isPremium
                                                    )
                                                    OrderShareHelper.shareViaSystemSheet(context, slipText, "Cutting Slip - ${item.garmentName}")
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(text = "Cutting Slip", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DeepTeal)
                                            }
                                        }

                                        Surface(
                                            color = DeepTeal.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { editingItem = item }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Measurements",
                                                    tint = DeepTeal,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Edit",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = DeepTeal,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemPoints.forEach { (k, v) ->
                                        Surface(
                                            color = CardWhite,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "$k: $v\"",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                ),
                                                color = DeepTeal,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Stitching Reference Designs Card
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Design References",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DeepTeal
                                )
                                Text(
                                    text = "${orderDesigns.size} Photos Attached",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CharcoalSubtle
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = LightBeige,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val tempFile = File(context.cacheDir, "camera_temp_${System.currentTimeMillis()}.jpg")
                                            val authority = "${context.packageName}.fileprovider"
                                            val uri = FileProvider.getUriForFile(context, authority, tempFile)
                                            tempCameraUri = uri
                                            cameraLauncher.launch(uri)
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Camera",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = DeepTeal,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Surface(
                                    color = LightBeige,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { galleryPickerLauncher.launch("image/*") }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Upload",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = DeepTeal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (orderDesigns.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LightBeige.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No design photos attached yet. Tap Camera or + Upload to attach pattern photos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CharcoalSubtle
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(orderDesigns, key = { it.id }) { design ->
                                    val file = remember(design.localFilePath) { File(design.localFilePath) }
                                    val bitmap = remember(design.localFilePath) {
                                        if (file.exists() && file.isFile) {
                                            try { BitmapFactory.decodeFile(file.absolutePath) } catch (e: Exception) { null }
                                        } else null
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(LightBeige)
                                            .clickable { viewingDesign = design }
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = design.notes,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                tint = DeepTeal.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }

                                        // Tag pill on bottom
                                        val tagText = design.tags.split(",").firstOrNull()?.trim() ?: "Design"
                                        Surface(
                                            color = Charcoal.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 6.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        ) {
                                            Text(
                                                text = tagText,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = CardWhite,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Financial Breakdown Card
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Payment & Balance Summary",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DeepTeal
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Stitching Charge:", style = MaterialTheme.typography.bodyMedium, color = CharcoalSubtle)
                            Text(text = "₹${order.totalAmount.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Charcoal)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Advance Received:", style = MaterialTheme.typography.bodyMedium, color = CharcoalSubtle)
                            Text(text = "₹${order.paidAmount.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = StatusGreen)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightBeige, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Balance Pending:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Charcoal)
                            Text(
                                text = if (order.balanceAmount > 0) "₹${order.balanceAmount.toInt()}" else "Settled",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (order.balanceAmount > 0) StatusRed else StatusGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showShareDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = CardWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share Complete Bill & Estimate",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = CardWhite
                            )
                        }
                    }
                }
            }
        }
    }

    // Cancellation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = "Cancel Order #${order.orderNumber}?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StatusRed
                )
            },
            text = {
                Column {
                    Text(text = "Please specify the cancellation reason:", style = MaterialTheme.typography.bodySmall, color = CharcoalSubtle)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cancellationReasonText,
                        onValueChange = { cancellationReasonText = it },
                        placeholder = { Text("e.g. Client requested refund") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        orderViewModel.cancelOrder(order.id, order.status, cancellationReasonText.ifBlank { "Client request" })
                        onNavigateBack()
                    }
                ) {
                    Text(text = "Cancel Order", color = StatusRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel), color = CharcoalSubtle)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Share Order Dialog
    if (showShareDialog) {
        val customerBillText = remember(order, orderWithDetails, includeMeasurementsInShare, isPremium, shopUpiId) {
            OrderShareHelper.buildOrderShareText(
                shopName = shopName,
                orderNumber = order.orderNumber,
                customerName = customerName,
                customerPhone = customerPhone,
                status = order.status,
                deliveryDateMs = order.promisedDeliveryDate,
                items = orderWithDetails.items,
                totalAmount = order.totalAmount,
                paidAmount = order.paidAmount,
                balanceAmount = order.balanceAmount,
                includeMeasurements = includeMeasurementsInShare,
                isPremium = isPremium,
                upiId = shopUpiId
            )
        }

        val cuttingSlipText = remember(order, orderWithDetails, isPremium) {
            OrderShareHelper.buildCuttingSlipText(
                shopName = shopName,
                orderNumber = order.orderNumber,
                customerName = customerName,
                deliveryDateMs = order.promisedDeliveryDate,
                items = orderWithDetails.items,
                assignedMaster = orderWithDetails.items.firstNotNullOfOrNull { VoiceNoteHelper.extractMaster(it.stitchingInstructions) },
                isPremium = isPremium
            )
        }

        val activeText = if (shareDialogTab == 0) customerBillText else cuttingSlipText

        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (shareDialogTab == 0) "Share Order #${order.orderNumber}" else "Cutting Slip #${order.orderNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Two-tab selector: Customer Bill vs Cutting Slip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBeige, RoundedCornerShape(8.dp))
                            .padding(3.dp)
                    ) {
                        Surface(
                            color = if (shareDialogTab == 0) DeepTeal else LightBeige,
                            contentColor = if (shareDialogTab == 0) CardWhite else Charcoal,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { shareDialogTab = 0 }
                        ) {
                            Text(
                                text = "🧾 Customer Bill",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 7.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            color = if (shareDialogTab == 1) DeepTeal else LightBeige,
                            contentColor = if (shareDialogTab == 1) CardWhite else Charcoal,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { shareDialogTab = 1 }
                        ) {
                            Text(
                                text = "✂️ Cutting Slip (No Prices)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 7.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (shareDialogTab == 0) {
                        // Toggle for Karigar / Measurement details
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { includeMeasurementsInShare = !includeMeasurementsInShare }
                                .background(LightBeige)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include Measurements",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = DeepTeal
                                )
                                Text(
                                    text = "Shows measurements in customer copy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CharcoalSubtle
                                )
                            }
                            Checkbox(
                                checked = includeMeasurementsInShare,
                                onCheckedChange = { includeMeasurementsInShare = it },
                                colors = CheckboxDefaults.colors(checkedColor = DeepTeal)
                            )
                        }
                    } else {
                        Surface(
                            color = WarmIvory,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, SoftGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔒 Pricing, advance, and financial details are hidden for workshop privacy.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = DeepTeal,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bill / Job Slip Preview Card
                    Surface(
                        color = CardWhite,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MutedSage.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = activeText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = Charcoal,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Button(
                        onClick = {
                            if (shareDialogTab == 0) {
                                OrderShareHelper.shareViaWhatsApp(context, activeText, customerPhone)
                            } else {
                                OrderShareHelper.shareViaWhatsApp(context, activeText, null)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = CardWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (shareDialogTab == 0) {
                                if (customerPhone.isNotBlank()) "Send to Customer WhatsApp" else "Share via WhatsApp"
                            } else "Share Slip to Master WhatsApp",
                            color = CardWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                OrderShareHelper.shareViaSystemSheet(context, activeText, if (shareDialogTab == 0) "$shopName Order #${order.orderNumber}" else "Cutting Slip #${order.orderNumber}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = CardWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Other Apps", color = CardWhite, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                OrderShareHelper.copyToClipboard(context, activeText)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Copy Text", color = DeepTeal, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text(text = "Close", color = CharcoalSubtle)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // "Order Ready" WhatsApp Notification Prompt Dialog (Phase A)
    if (showOrderReadyNotifyDialog) {
        val readyText = remember(order, orderWithDetails, isPremium) {
            OrderShareHelper.buildOrderReadyAlertText(
                shopName = shopName,
                orderNumber = order.orderNumber,
                customerName = customerName,
                items = orderWithDetails.items,
                balanceAmount = order.balanceAmount,
                shopPhone = "9848011223",
                isPremium = isPremium
            )
        }

        AlertDialog(
            onDismissRequest = { showOrderReadyNotifyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Order Ready for Pickup!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DeepTeal)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Would you like to notify $customerName on WhatsApp that their order is ready for trial & pickup?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (order.balanceAmount > 0) {
                        Surface(
                            color = WarmIvory,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, SoftGold)
                        ) {
                            Text(
                                text = "Pending Balance Due: ₹${order.balanceAmount.toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        OrderShareHelper.shareViaWhatsApp(context, readyText, customerPhone)
                        showOrderReadyNotifyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = CardWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Send WhatsApp Alert", color = CardWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOrderReadyNotifyDialog = false }) {
                    Text("Skip", color = CharcoalSubtle)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dynamic UPI Payment QR Code Dialog (Phase A)
    if (showUpiQrDialog) {
        UpiPaymentQrDialog(
            shopName = shopName,
            orderNumber = order.orderNumber,
            balanceAmount = order.balanceAmount,
            upiId = shopUpiId.ifBlank { "kethepalle@upi" },
            onDismiss = { showUpiQrDialog = false }
        )
    }

    // Edit Garment Measurements Dialog
    if (editingItem != null) {
        val currentPoints = if (!editingItem!!.measurementsJson.isNullOrBlank()) {
            com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl.parseMeasurements(editingItem!!.measurementsJson!!)
        } else {
            val snapshotJson = orderWithDetails.measurementSnapshot?.frozenMeasurementsJson ?: "{}"
            com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl.parseMeasurements(snapshotJson)
        }

        EditGarmentMeasurementsDialog(
            garmentName = editingItem!!.garmentName,
            templateId = editingItem!!.garmentTemplateId,
            initialMeasurements = currentPoints,
            onDismiss = { editingItem = null },
            onSave = { updatedMap ->
                orderViewModel.updateOrderItemMeasurements(order.id, editingItem!!.id, updatedMap)
                editingItem = null
            }
        )
    }

    // Design Reference Photo Full-screen Preview Dialog
    if (viewingDesign != null) {
        val targetDesign = viewingDesign!!
        val file = remember(targetDesign.localFilePath) { File(targetDesign.localFilePath) }
        val bitmap = remember(targetDesign.localFilePath) {
            if (file.exists() && file.isFile) {
                try { BitmapFactory.decodeFile(file.absolutePath) } catch (e: Exception) { null }
            } else null
        }

        AlertDialog(
            onDismissRequest = { viewingDesign = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Design Reference",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                    IconButton(
                        onClick = {
                            galleryViewModel?.deletePhoto(targetDesign.id)
                            viewingDesign = null
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Photo", tint = StatusRed)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = targetDesign.notes,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(LightBeige, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Image file preview not available", color = CharcoalSubtle)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tags: ${targetDesign.tags}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                    if (targetDesign.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = targetDesign.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Charcoal
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingDesign = null }) {
                    Text(text = "Close", color = DeepTeal, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun EditGarmentMeasurementsDialog(
    garmentName: String,
    templateId: String,
    initialMeasurements: Map<String, Double>,
    onDismiss: () -> Unit,
    onSave: (Map<String, Double>) -> Unit
) {
    val measurements = remember {
        androidx.compose.runtime.mutableStateMapOf<String, Double>().apply {
            putAll(initialMeasurements)
        }
    }
    var newPointName by remember { mutableStateOf("") }
    var newPointValue by remember { mutableStateOf("15.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Edit Garment Measurements",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )
                Text(
                    text = "$garmentName ($templateId)",
                    style = MaterialTheme.typography.labelSmall,
                    color = CharcoalSubtle
                )
            }
        },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(measurements.keys.size) { idx ->
                    val key = measurements.keys.toList()[idx]
                    val currentVal = measurements[key] ?: 0.0
                    var textVal by remember(key) { mutableStateOf(currentVal.toString()) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBeige, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = key.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DeepTeal,
                            modifier = Modifier.weight(1.3f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(2.2f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    val newVal = (currentVal - 0.5).coerceAtLeast(0.0)
                                    measurements[key] = newVal
                                    textVal = newVal.toString()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DeepTeal)
                            }

                            OutlinedTextField(
                                value = textVal,
                                onValueChange = {
                                    textVal = it
                                    it.toDoubleOrNull()?.let { num -> measurements[key] = num }
                                },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                modifier = Modifier.width(76.dp)
                            )

                            IconButton(
                                onClick = {
                                    val newVal = currentVal + 0.5
                                    measurements[key] = newVal
                                    textVal = newVal.toString()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DeepTeal)
                            }

                            IconButton(
                                onClick = { measurements.remove(key) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = CharcoalSubtle,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+ Add Custom Point",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newPointName,
                            onValueChange = { newPointName = it },
                            label = { Text("Point Name") },
                            placeholder = { Text("e.g. Cap Sleeve") },
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = newPointValue,
                            onValueChange = { newPointValue = it },
                            label = { Text("Value (\")") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        IconButton(
                            onClick = {
                                if (newPointName.isNotBlank()) {
                                    val k = newPointName.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
                                    val v = newPointValue.toDoubleOrNull() ?: 10.0
                                    measurements[k] = v
                                    newPointName = ""
                                    newPointValue = "15.0"
                                }
                            },
                            modifier = Modifier
                                .background(DeepTeal, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = CardWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(measurements.toMap()) },
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
            ) {
                Text("Save Measurements", color = CardWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Charcoal)
            }
        },
        containerColor = CardWhite,
        shape = RoundedCornerShape(16.dp)
    )
}
