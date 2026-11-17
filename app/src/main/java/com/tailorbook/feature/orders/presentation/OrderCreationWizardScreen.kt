package com.tailorbook.feature.orders.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tailorbook.core.util.FormattingUtils
import com.tailorbook.core.util.HapticHelper
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tailorbook.R
import com.tailorbook.core.ads.RewardedAdManager
import com.tailorbook.core.datastore.UserPreferencesManager
import com.tailorbook.core.util.UpiPaymentQrDialog
import com.tailorbook.core.util.VoiceNoteHelper
import com.tailorbook.feature.customers.presentation.CustomerViewModel
import com.tailorbook.feature.gallery.presentation.GalleryViewModel
import com.tailorbook.feature.staff.data.local.entity.StaffEntity
import com.tailorbook.feature.staff.presentation.StaffViewModel
import com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl
import com.tailorbook.feature.measurements.domain.model.GarmentCategory
import com.tailorbook.feature.measurements.domain.model.GarmentTemplates
import com.tailorbook.feature.measurements.presentation.MeasurementViewModel
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Multi-item Order Creation Wizard Screen (TASK-08-03 / REQ-ORD-01).
 * Supports customer search, multi-item cart (e.g. 2 blouses, 1 lehenga, 2 churidars),
 * garment-specific measurement editing, snapshot freezing, stitching design photos, and live financial breakdown.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OrderCreationWizardScreen(
    initialCustomerId: String = "",
    initialTemplateId: String = "BLOUSE",
    orderViewModel: OrderViewModel,
    customerViewModel: CustomerViewModel,
    measurementViewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit,
    onOrderCreated: (String) -> Unit,
    staffViewModel: StaffViewModel? = null,
    galleryViewModel: GalleryViewModel? = null,
    onRecordMeasurements: ((customerId: String, templateId: String) -> Unit)? = null,
    isPremium: Boolean = false,
    rewardedAdManager: RewardedAdManager? = null,
    onNavigateToUpgrade: () -> Unit = {},
    shopName: String = "",
    shopUpiId: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { UserPreferencesManager(context) }
    val savedShopName by prefs.shopNameFlow.collectAsState(initial = "My Boutique")
    val savedShopUpiId by prefs.shopUpiIdFlow.collectAsState(initial = "")
    val effectiveShopName = shopName.ifBlank { savedShopName }
    val effectiveShopUpiId = shopUpiId.ifBlank { savedShopUpiId }
    var showAdvanceQrDialog by remember { mutableStateOf(false) }
    val allStaffMembers by (staffViewModel?.allStaff?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList<StaffEntity>()) })
    val activeStaff = remember(allStaffMembers) { allStaffMembers.filter { it.isActive } }
    val customers by customerViewModel.filteredCustomers.collectAsState()
    var selectedCustomerId by remember { mutableStateOf(initialCustomerId) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableStateOf(initialTemplateId) }

    val activeOrdersCount by orderViewModel.activeOrderCount.collectAsState()
    var showOrderLimitDialog by remember { mutableStateOf(false) }
    var showPhotoLimitDialog by remember { mutableStateOf(false) }

    val maxAllowedPhotos = if (isPremium) 5 else com.tailorbook.feature.subscription.domain.EntitlementConstants.FREE_PHOTOS_PER_ORDER_LIMIT

    // Attached 1-5 design photos (neck, sleeves, embroidery, fabric)
    val attachedDesignUris = remember { mutableStateListOf<Uri>() }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            if (attachedDesignUris.size < maxAllowedPhotos) {
                attachedDesignUris.add(tempCameraUri!!)
            }
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val remainingSlots = maxAllowedPhotos - attachedDesignUris.size
        if (remainingSlots > 0) {
            attachedDesignUris.addAll(uris.take(remainingSlots))
        }
    }

    LaunchedEffect(customers) {
        if (customers.isNotEmpty()) {
            if (selectedCustomerId.isBlank() || selectedCustomerId == "cust-101" || customers.none { it.customer.id == selectedCustomerId }) {
                selectedCustomerId = customers.first().customer.id
            }
        }
    }

    val filteredCustomers = remember(customers, customerSearchQuery) {
        if (customerSearchQuery.isBlank()) customers
        else {
            val q = customerSearchQuery.trim()
            customers.filter {
                it.customer.name.contains(q, ignoreCase = true) ||
                it.customer.primaryPhone.contains(q)
            }
        }
    }

    val currentCustomer = customers.find { it.customer.id == selectedCustomerId }
    val customerName = currentCustomer?.customer?.name ?: "Customer"
    val customerGender = currentCustomer?.customer?.gender?.uppercase(Locale.ROOT) ?: "FEMALE"

    var filterCategory by remember { mutableStateOf("RECOMMENDED") } // "RECOMMENDED", "ALL", "WOMEN", "MEN"

    val dynamicAllTemplates by measurementViewModel.allTemplates.collectAsState()

    val templates = remember(customerGender, filterCategory, dynamicAllTemplates) {
        when (filterCategory) {
            "ALL" -> dynamicAllTemplates
            "WOMEN" -> dynamicAllTemplates.filter { it.category == GarmentCategory.WOMEN || it.category == GarmentCategory.UNISEX }
            "MEN" -> dynamicAllTemplates.filter { it.category == GarmentCategory.MEN || it.category == GarmentCategory.UNISEX }
            else -> {
                if (customerGender.contains("MALE") && !customerGender.contains("FE")) {
                    dynamicAllTemplates.filter { it.category == GarmentCategory.MEN || it.category == GarmentCategory.UNISEX }
                } else {
                    dynamicAllTemplates.filter { it.category == GarmentCategory.WOMEN || it.category == GarmentCategory.UNISEX }
                }
            }
        }
    }

    LaunchedEffect(templates) {
        if (templates.none { it.id == selectedTemplateId }) {
            templates.firstOrNull()?.let {
                selectedTemplateId = it.id
            }
        }
    }

    // Fetch latest measurement version for this customer and garment
    val allVersions by measurementViewModel.inMemoryVersions.collectAsState()
    val customerVersions = remember(allVersions, selectedCustomerId, selectedTemplateId) {
        measurementViewModel.getVersions(selectedCustomerId, null, selectedTemplateId)
    }
    val latestVersion = customerVersions.firstOrNull()

    // Form inputs for current garment item
    val selectedTemplateObj = remember(selectedTemplateId, dynamicAllTemplates) {
        dynamicAllTemplates.find { it.id.equals(selectedTemplateId, ignoreCase = true) } ?: GarmentTemplates.findById(selectedTemplateId)
    }
    val defaultGarmentName = selectedTemplateObj.getDisplayName()

    var garmentCustomName by remember(selectedTemplateId) {
        mutableStateOf(defaultGarmentName)
    }
    var currentItemQuantity by remember { mutableIntStateOf(1) }
    var currentItemUnitPriceText by remember(selectedTemplateId, selectedTemplateObj) {
        val price = if (selectedTemplateObj.defaultPrice > 0.0) {
            selectedTemplateObj.defaultPrice.toInt().toString()
        } else {
            "500"
        }
        mutableStateOf(price)
    }
    var stitchingInstructions by remember { mutableStateOf("Double stitching on side seams. Standard margin.") }
    var fabricDetails by remember { mutableStateOf("Client provided fabric") }

    // Garment-specific measurement state (loaded from profile or template defaults, editable per item)
    var currentItemMeasurements by remember(selectedCustomerId, selectedTemplateId, latestVersion) {
        val initialMap = if (latestVersion != null) {
            MeasurementRepositoryImpl.parseMeasurements(latestVersion.measurementsJson)
        } else {
            selectedTemplateObj.fields.associate { it.key to it.defaultVal }
        }
        mutableStateOf(initialMap)
    }
    var isMeasurementCustomized by remember(selectedCustomerId, selectedTemplateId) { mutableStateOf(false) }

    var showMeasurementEditorDialog by remember { mutableStateOf(false) }
    var editingCartItemIndex by remember { mutableStateOf<Int?>(null) }

    // Multi-item cart
    val orderItems = remember { mutableStateListOf<OrderViewModel.OrderItemDraft>() }

    val dayMs = 86400000L
    var promisedDeliveryDate by remember {
        val initialCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 5)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(initialCal.timeInMillis)
    }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val currentCal = Calendar.getInstance().apply { timeInMillis = promisedDeliveryDate }
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val deliveryDayCal = (currentCal.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val daysDiff = ((deliveryDayCal.timeInMillis - todayCal.timeInMillis) / dayMs).toInt()
    val deliveryRelativeText = when {
        daysDiff == 0 -> "Today"
        daysDiff == 1 -> "Tomorrow"
        daysDiff > 1 -> "in $daysDiff days"
        else -> "Past Date"
    }
    val formattedDeliveryDate = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date(promisedDeliveryDate))

    // Voice note and Master / Tailor assignment states (Phase B)
    var isRecordingAudio by remember { mutableStateOf(false) }
    var attachedVoiceNotePath by remember { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var assignedMasterName by remember { mutableStateOf("") }
    var showMasterAssignDialog by remember { mutableStateOf(false) }

    var recordingSeconds by remember { mutableIntStateOf(0) }
    var isExtendedAudioUnlocked by remember { mutableStateOf(false) }
    var showExtendedAudioDialog by remember { mutableStateOf(false) }
    val maxAudioDurationSeconds = if (isPremium || isExtendedAudioUnlocked) 120 else 30

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            while (isRecordingAudio) {
                kotlinx.coroutines.delay(1000L)
                recordingSeconds++
                if (recordingSeconds >= maxAudioDurationSeconds) {
                    val path = VoiceNoteHelper.stopRecording()
                    isRecordingAudio = false
                    if (path != null) {
                        attachedVoiceNotePath = path
                    }
                    if (!isPremium && !isExtendedAudioUnlocked) {
                        showExtendedAudioDialog = true
                    }
                    break
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val path = VoiceNoteHelper.startRecording(context)
            if (path != null) {
                recordingSeconds = 0
                isRecordingAudio = true
                Toast.makeText(context, "Recording started... Speak stitching notes", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not start audio recorder", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    // Financial calculations
    val currentItemUnitPrice = currentItemUnitPriceText.toDoubleOrNull() ?: 0.0
    val cartTotal = orderItems.sumOf { it.quantity * it.unitPrice }
    val totalAmount = if (orderItems.isNotEmpty()) cartTotal else (currentItemQuantity * currentItemUnitPrice)

    var advanceAmountText by remember { mutableStateOf("") }
    val advanceAmount = advanceAmountText.toDoubleOrNull() ?: 0.0
    val balanceAmount = (totalAmount - advanceAmount).coerceAtLeast(0.0)

    val totalGarmentCount = if (orderItems.isNotEmpty()) orderItems.sumOf { it.quantity } else currentItemQuantity

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
                            text = stringResource(id = R.string.order_create_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = "Order #${orderViewModel.getNextOrderNumber()} • $customerName (${totalGarmentCount} Garments)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = CardWhite,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    val canCreate = (orderItems.isNotEmpty() || currentItemMeasurements.isNotEmpty()) && totalAmount > 0

                    TailorPrimaryButton(
                        text = if (canCreate) {
                            "Create Order ($totalGarmentCount Garments • ${FormattingUtils.formatCurrency(totalAmount)})"
                        } else {
                            "Cannot Proceed (Enter Item Details)"
                        },
                        enabled = canCreate,
                        onClick = {
                            HapticHelper.success(context, haptic)
                            if (!isPremium && activeOrdersCount >= com.tailorbook.feature.subscription.domain.EntitlementConstants.FREE_ACTIVE_ORDERS_LIMIT) {
                                showOrderLimitDialog = true
                                return@TailorPrimaryButton
                            }

                            val itemsToSave: List<OrderViewModel.OrderItemDraft> = if (orderItems.isNotEmpty()) {
                                orderItems.toList()
                            } else {
                                listOf(
                                    OrderViewModel.OrderItemDraft(
                                        garmentTemplateId = selectedTemplateId,
                                        garmentName = garmentCustomName.ifBlank { defaultGarmentName },
                                        quantity = currentItemQuantity,
                                        unitPrice = currentItemUnitPrice,
                                        stitchingInstructions = VoiceNoteHelper.packInstructions(
                                            stitchingInstructions,
                                            attachedVoiceNotePath,
                                            assignedMasterName.ifBlank { null }
                                        ),
                                        fabricDetails = fabricDetails,
                                        frozenMeasurements = currentItemMeasurements,
                                        unit = latestVersion?.unit ?: "INCH"
                                    )
                                )
                            }

                            orderViewModel.createMultiItemOrder(
                                customerId = selectedCustomerId,
                                familyMemberId = null,
                                items = itemsToSave,
                                deliveryDate = promisedDeliveryDate,
                                totalAmount = totalAmount,
                                advanceAmount = advanceAmount,
                                onSuccess = { newOrderId ->
                                    if (attachedDesignUris.isNotEmpty() && galleryViewModel != null) {
                                        galleryViewModel.attachPhotosToOrder(
                                            context = context,
                                            uris = attachedDesignUris.toList(),
                                            orderId = newOrderId,
                                            customerId = selectedCustomerId,
                                            defaultTag = "Stitching Design"
                                        )
                                    }
                                    onOrderCreated(newOrderId)
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
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
            // Section 1: Customer Selection & Search
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. Customer Profile",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                            Surface(
                                color = LightBeige,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Gender: $customerGender",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DeepTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Customer Search Bar
                        OutlinedTextField(
                            value = customerSearchQuery,
                            onValueChange = { customerSearchQuery = it },
                            placeholder = { Text("Search customer by name or phone...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (customerSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { customerSearchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = CharcoalSubtle,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (filteredCustomers.isEmpty()) {
                            Text(
                                text = "No matching customers found",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalSubtle,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filteredCustomers) { c ->
                                    val isSelected = c.customer.id == selectedCustomerId
                                    Surface(
                                        color = if (isSelected) DeepTeal else LightBeige,
                                        contentColor = if (isSelected) CardWhite else Charcoal,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedCustomerId = c.customer.id }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = c.customer.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                if (c.customer.primaryPhone.isNotBlank()) {
                                                    Text(
                                                        text = c.customer.primaryPhone,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = if (isSelected) SoftGold else CharcoalSubtle
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Garment Item Configuration & Multi-Item Adder
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. Garment Details",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )

                            // Gender filter chips
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    "RECOMMENDED" to "Filtered",
                                    "ALL" to "All"
                                ).forEach { (catKey, catLabel) ->
                                    val isCatSelected = filterCategory == catKey
                                    Surface(
                                        color = if (isCatSelected) DeepTeal else LightBeige,
                                        contentColor = if (isCatSelected) CardWhite else Charcoal,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { filterCategory = catKey }
                                    ) {
                                        Text(
                                            text = catLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Template Selector Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(templates) { t ->
                                val isSelected = t.id == selectedTemplateId
                                val displayName = if (t.titleRes != 0) stringResource(id = t.titleRes) else t.getDisplayName()
                                Surface(
                                    color = if (isSelected) DeepTeal else LightBeige,
                                    contentColor = if (isSelected) CardWhite else Charcoal,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedTemplateId = t.id
                                            garmentCustomName = t.getDisplayName()
                                        }
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = garmentCustomName,
                            onValueChange = { garmentCustomName = it },
                            label = { Text("Garment Item Description") },
                            placeholder = { Text("e.g. Silk Bridal Blouse, Designer Lehenga, Churidar") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quantity Stepper and Unit Price Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Quantity Stepper
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = "Quantity",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Charcoal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(LightBeige, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            HapticHelper.tick(context, haptic)
                                            if (currentItemQuantity > 1) currentItemQuantity--
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrease",
                                            tint = DeepTeal
                                        )
                                    }

                                    Text(
                                        text = "$currentItemQuantity",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            HapticHelper.tick(context, haptic)
                                            currentItemQuantity++
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase",
                                            tint = DeepTeal
                                        )
                                    }
                                }
                            }

                            // Unit Price
                            OutlinedTextField(
                                value = currentItemUnitPriceText,
                                onValueChange = { currentItemUnitPriceText = it },
                                label = { Text("Unit Price (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.4f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtotal preview
                        val itemSubtotal = currentItemQuantity * currentItemUnitPrice
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item Subtotal:",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalSubtle
                            )
                            Text(
                                text = FormattingUtils.formatCurrency(itemSubtotal),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Item-level instructions
                        OutlinedTextField(
                            value = stitchingInstructions,
                            onValueChange = { stitchingInstructions = it },
                            label = { Text("Stitching Instructions") },
                            placeholder = { Text("e.g. Deep neck, double piping, tassels...") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Voice Note & Karigar Assignment Action Row (Phase B)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRecordingAudio) {
                                Surface(
                                    color = StatusRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, StatusRed),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val path = VoiceNoteHelper.stopRecording()
                                            isRecordingAudio = false
                                            if (path != null) {
                                                attachedVoiceNotePath = path
                                                Toast.makeText(context, "Voice note attached! (${recordingSeconds}s)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = StatusRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "🔴 Stop (${recordingSeconds}s / ${maxAudioDurationSeconds}s)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = StatusRed)
                                    }
                                }
                            } else if (attachedVoiceNotePath != null) {
                                Surface(
                                    color = DeepTeal.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, DeepTeal),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isAudioPlaying) {
                                                VoiceNoteHelper.stopPlayback()
                                                isAudioPlaying = false
                                            } else {
                                                isAudioPlaying = true
                                                VoiceNoteHelper.playVoiceNote(attachedVoiceNotePath!!) {
                                                    isAudioPlaying = false
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = DeepTeal,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isAudioPlaying) "Playing" else "Voice Note",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = DeepTeal
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                VoiceNoteHelper.stopPlayback()
                                                attachedVoiceNotePath = null
                                                isAudioPlaying = false
                                            },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CharcoalSubtle, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    color = LightBeige,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                            if (hasMic) {
                                                val path = VoiceNoteHelper.startRecording(context)
                                                if (path != null) {
                                                    recordingSeconds = 0
                                                    isRecordingAudio = true
                                                    Toast.makeText(context, "Recording started...", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "🎙️ Voice Note", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = DeepTeal)
                                    }
                                }
                            }

                            // Master / Tailor Assignment Chip
                            Surface(
                                color = if (assignedMasterName.isNotBlank()) WarmIvory else LightBeige,
                                shape = RoundedCornerShape(8.dp),
                                border = if (assignedMasterName.isNotBlank()) BorderStroke(1.dp, SoftGold) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showMasterAssignDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (assignedMasterName.isNotBlank()) assignedMasterName else "Assign Master",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = DeepTeal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = fabricDetails,
                            onValueChange = { fabricDetails = it },
                            label = { Text("Fabric Details") },
                            placeholder = { Text("e.g. Pure raw silk, Kundan work...") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Button to add this garment item to cart
                        Button(
                            onClick = {
                                if (currentItemUnitPrice > 0 && currentItemQuantity > 0) {
                                    orderItems.add(
                                        OrderViewModel.OrderItemDraft(
                                            garmentTemplateId = selectedTemplateId,
                                            garmentName = garmentCustomName.ifBlank { defaultGarmentName },
                                            quantity = currentItemQuantity,
                                            unitPrice = currentItemUnitPrice,
                                            stitchingInstructions = VoiceNoteHelper.packInstructions(
                                            stitchingInstructions,
                                            attachedVoiceNotePath,
                                            assignedMasterName.ifBlank { null }
                                        ),
                                            fabricDetails = fabricDetails,
                                            frozenMeasurements = currentItemMeasurements,
                                            unit = latestVersion?.unit ?: "INCH"
                                        )
                                    )

                                    // Reset quantity for next garment
                                    currentItemQuantity = 1
                                    attachedVoiceNotePath = null
                                    assignedMasterName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = CardWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ Add Item to Order Cart",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = CardWhite
                            )
                        }
                    }
                }
            }

            // Section 3: Garment Measurements with Edit / Customize Option
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "3. Garment Measurements",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DeepTeal
                                )
                                Text(
                                    text = "For: $defaultGarmentName (${currentItemMeasurements.size} Points)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CharcoalSubtle
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (isMeasurementCustomized) StatusGreen.copy(alpha = 0.15f) else LightBeige,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isMeasurementCustomized) StatusGreen else DeepTeal,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isMeasurementCustomized) "Customized" else (if (latestVersion != null) "v${latestVersion.versionNumber}" else "Default"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isMeasurementCustomized) StatusGreen else DeepTeal
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    color = DeepTeal,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showMeasurementEditorDialog = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Measurements",
                                            tint = CardWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Edit Measurements",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = CardWhite
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Measurements are specific to this garment. Tap 'Edit Measurements' or any value to adjust.",
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalSubtle
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMeasurementEditorDialog = true }
                        ) {
                            currentItemMeasurements.forEach { (pointKey, pointVal) ->
                                Surface(
                                    color = LightBeige,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "$pointKey: $pointVal\"",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = DeepTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Order Items Cart (Multiple Garments in Single Order)
            if (orderItems.isNotEmpty()) {
                item {
                    TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = DeepTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Order Cart (${orderItems.size} line items)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal
                                    )
                                }

                                Surface(
                                    color = LightBeige,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${orderItems.sumOf { it.quantity }} Garments Total",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            orderItems.forEachIndexed { index, itemDraft ->
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
                                            text = "${itemDraft.quantity}x ${itemDraft.garmentName}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = DeepTeal
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "₹${(itemDraft.quantity * itemDraft.unitPrice).toInt()}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = DeepTeal
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { orderItems.removeAt(index) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove item",
                                                    tint = StatusRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "₹${itemDraft.unitPrice.toInt()} each • Template: ${itemDraft.garmentTemplateId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CharcoalSubtle
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Item's own measurements in cart with edit button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Measurements (${itemDraft.frozenMeasurements.size}):",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = DeepTeal
                                        )
                                        Surface(
                                            color = DeepTeal.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { editingCartItemIndex = index }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Measurements",
                                                    tint = DeepTeal,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Edit",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                    color = DeepTeal
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        itemDraft.frozenMeasurements.forEach { (k, v) ->
                                            Surface(
                                                color = CardWhite,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "$k: $v\"",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                                                    color = DeepTeal,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            // Section 5: Delivery Timeline (Hybrid Model: Presets + Calendar)
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "4. Promised Delivery Timeline",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                            Surface(
                                color = WarmIvory,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SoftGold)
                            ) {
                                Text(
                                    text = deliveryRelativeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DeepTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive Delivery Date Card (Tap to open Calendar)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LightBeige.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, DeepTeal.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showDatePickerDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(DeepTeal.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Pick Delivery Date",
                                            tint = DeepTeal,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Delivery Date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CharcoalSubtle
                                        )
                                        Text(
                                            text = formattedDeliveryDate,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Charcoal
                                        )
                                    }
                                }

                                Surface(
                                    color = DeepTeal,
                                    contentColor = CardWhite,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showDatePickerDialog = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = CardWhite
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Pick Date",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Presets Row
                        Text(
                            text = "⚡ Quick Presets",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = CharcoalSubtle
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val presets = listOf(
                                "3 Days" to 3,
                                "5 Days" to 5,
                                "1 Week" to 7,
                                "10 Days" to 10,
                                "2 Weeks" to 14
                            )
                            presets.forEach { (label, days) ->
                                val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
                                val isSelected = currentCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                                        currentCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)

                                Surface(
                                    color = if (isSelected) DeepTeal else LightBeige,
                                    contentColor = if (isSelected) CardWhite else Charcoal,
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (isSelected) null else BorderStroke(0.5.dp, DeepTeal.copy(alpha = 0.15f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val c = Calendar.getInstance().apply {
                                                add(Calendar.DAY_OF_YEAR, days)
                                                set(Calendar.HOUR_OF_DAY, 18)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            promisedDeliveryDate = c.timeInMillis
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: Pricing & Financial Balance
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "5. Pricing & Advance",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )

                            Surface(
                                color = DeepTeal.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAdvanceQrDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "UPI QR Code",
                                        tint = DeepTeal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "UPI QR",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "₹${totalAmount.toInt()}",
                                onValueChange = {},
                                label = { Text("Total Amount") },
                                readOnly = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = advanceAmountText,
                                onValueChange = { advanceAmountText = it },
                                label = { Text("Advance Paid (₹)") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = {
                                    IconButton(onClick = { showAdvanceQrDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = "Show UPI QR Code",
                                            tint = DeepTeal
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightBeige, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pending Balance Due:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Charcoal
                            )
                            Text(
                                text = "₹${balanceAmount.toInt()}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (balanceAmount > 0) StatusRed else StatusGreen
                            )
                        }
                    }
                }
            }

            // Section 6: Stitching Design References & Pattern Photos (2-5 Photos)
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "6. Stitching Design Photos",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DeepTeal
                                )
                                Text(
                                    text = "Neck, sleeves, fabric & pattern references",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CharcoalSubtle
                                )
                            }

                            Surface(
                                color = if (attachedDesignUris.size >= maxAllowedPhotos) StatusGreen.copy(alpha = 0.15f) else LightBeige,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${attachedDesignUris.size} of $maxAllowedPhotos photos",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (attachedDesignUris.size >= maxAllowedPhotos) StatusGreen else DeepTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Camera & Gallery action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Camera Button
                            Surface(
                                color = if (attachedDesignUris.size < maxAllowedPhotos) LightBeige else LightBeige.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!isPremium && attachedDesignUris.size >= maxAllowedPhotos) {
                                            showPhotoLimitDialog = true
                                        } else if (attachedDesignUris.size < maxAllowedPhotos) {
                                            val tempFile = File(context.cacheDir, "camera_temp_${System.currentTimeMillis()}.jpg")
                                            val authority = "${context.packageName}.fileprovider"
                                            val uri = FileProvider.getUriForFile(context, authority, tempFile)
                                            tempCameraUri = uri
                                            cameraLauncher.launch(uri)
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = DeepTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Camera",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal
                                    )
                                }
                            }

                            // Gallery Button
                            Surface(
                                color = if (attachedDesignUris.size < maxAllowedPhotos) LightBeige else LightBeige.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!isPremium && attachedDesignUris.size >= maxAllowedPhotos) {
                                            showPhotoLimitDialog = true
                                        } else if (attachedDesignUris.size < maxAllowedPhotos) {
                                            galleryPickerLauncher.launch("image/*")
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = DeepTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Upload Gallery",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = DeepTeal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preview row or empty placeholder
                        if (attachedDesignUris.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LightBeige.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No design photos attached yet. Attach 2-5 photos via Camera or Gallery.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CharcoalSubtle
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(attachedDesignUris.size) { index ->
                                    val uri = attachedDesignUris[index]
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(LightBeige)
                                    ) {
                                        val bitmap = remember(uri) {
                                            try {
                                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                                    BitmapFactory.decodeStream(stream)
                                                }
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }

                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Design Photo ${index + 1}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        // Badge #index+1
                                        Surface(
                                            color = Charcoal.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 6.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = CardWhite,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }

                                        // Remove button
                                        IconButton(
                                            onClick = { attachedDesignUris.removeAt(index) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .background(StatusRed.copy(alpha = 0.85f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove photo",
                                                tint = CardWhite,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for editing measurements of current garment being configured
    if (showMeasurementEditorDialog) {
        OrderMeasurementEditorDialog(
            garmentName = garmentCustomName,
            templateId = selectedTemplateId,
            initialMeasurements = currentItemMeasurements,
            onDismiss = { showMeasurementEditorDialog = false },
            onSave = { updatedMap ->
                currentItemMeasurements = updatedMap
                isMeasurementCustomized = true
                showMeasurementEditorDialog = false
            }
        )
    }

    // Dialog for editing measurements of an item already in the cart
    if (editingCartItemIndex != null && editingCartItemIndex!! in orderItems.indices) {
        val targetItem = orderItems[editingCartItemIndex!!]
        OrderMeasurementEditorDialog(
            garmentName = targetItem.garmentName,
            templateId = targetItem.garmentTemplateId,
            initialMeasurements = targetItem.frozenMeasurements,
            onDismiss = { editingCartItemIndex = null },
            onSave = { updatedMap ->
                orderItems[editingCartItemIndex!!] = targetItem.copy(frozenMeasurements = updatedMap)
                editingCartItemIndex = null
            }
        )
    }

    // Material 3 Date Picker Dialog for Custom Delivery Date Selection
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = promisedDeliveryDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 18)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            promisedDeliveryDate = localCal.timeInMillis
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Select", color = DeepTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel", color = Charcoal.copy(alpha = 0.7f))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = CardWhite,
                titleContentColor = DeepTeal,
                headlineContentColor = DeepTeal,
                weekdayContentColor = DeepTeal,
                subheadContentColor = Charcoal,
                yearContentColor = Charcoal,
                currentYearContentColor = DeepTeal,
                selectedYearContentColor = CardWhite,
                selectedYearContainerColor = DeepTeal,
                dayContentColor = Charcoal,
                selectedDayContentColor = CardWhite,
                selectedDayContainerColor = DeepTeal,
                todayContentColor = DeepTeal,
                todayDateBorderColor = DeepTeal
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CardWhite,
                    titleContentColor = DeepTeal,
                    headlineContentColor = DeepTeal,
                    weekdayContentColor = DeepTeal,
                    subheadContentColor = Charcoal,
                    yearContentColor = Charcoal,
                    currentYearContentColor = DeepTeal,
                    selectedYearContentColor = CardWhite,
                    selectedYearContainerColor = DeepTeal,
                    dayContentColor = Charcoal,
                    selectedDayContentColor = CardWhite,
                    selectedDayContainerColor = DeepTeal,
                    todayContentColor = DeepTeal,
                    todayDateBorderColor = DeepTeal
                )
            )
        }
    }

    // Dialog for Assigning Master / Cutting Tailor
    if (showMasterAssignDialog) {
        var customMasterInput by remember { mutableStateOf(assignedMasterName) }

        AlertDialog(
            onDismissRequest = { showMasterAssignDialog = false },
            title = {
                Text(
                    text = "Assign Cutting Master / Tailor",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select a workshop master or enter custom name:",
                        style = MaterialTheme.typography.labelSmall,
                        color = CharcoalSubtle
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (activeStaff.isEmpty()) {
                        Surface(
                            color = LightBeige,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "No staff members added yet. Type a name below or add staff in Staff Management.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalSubtle,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        activeStaff.forEach { member ->
                            val roleFormatted = member.role.lowercase().replaceFirstChar { it.uppercase() }
                            val displayName = "${member.name} ($roleFormatted)"
                            val isSelected = assignedMasterName == member.name || assignedMasterName == displayName
                            Surface(
                                color = if (isSelected) DeepTeal else LightBeige,
                                contentColor = if (isSelected) CardWhite else Charcoal,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        assignedMasterName = member.name
                                        customMasterInput = member.name
                                        showMasterAssignDialog = false
                                    }
                            ) {
                                Text(
                                    text = "✂️ $displayName",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customMasterInput,
                        onValueChange = { customMasterInput = it },
                        label = { Text("Or Type Custom Master / Tailor") },
                        placeholder = { Text("e.g. Anand Master") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        assignedMasterName = customMasterInput.trim()
                        showMasterAssignDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                ) {
                    Text("Done", color = CardWhite)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        assignedMasterName = ""
                        showMasterAssignDialog = false
                    }
                ) {
                    Text("Clear / Unassign", color = StatusRed)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showOrderLimitDialog) {
        AlertDialog(
            onDismissRequest = { showOrderLimitDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.entitlement_active_order_limit_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.entitlement_active_order_limit_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOrderLimitDialog = false
                        onNavigateToUpgrade()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                ) {
                    Text(stringResource(R.string.entitlement_btn_upgrade), color = CardWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOrderLimitDialog = false }) {
                    Text(stringResource(R.string.entitlement_btn_maybe_later), color = CharcoalSubtle)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showPhotoLimitDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoLimitDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.entitlement_photo_limit_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.entitlement_photo_limit_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPhotoLimitDialog = false
                        onNavigateToUpgrade()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                ) {
                    Text(stringResource(R.string.entitlement_btn_upgrade), color = CardWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoLimitDialog = false }) {
                    Text(stringResource(R.string.entitlement_btn_maybe_later), color = CharcoalSubtle)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showExtendedAudioDialog) {
        AlertDialog(
            onDismissRequest = { showExtendedAudioDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.audio_unlock_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.audio_unlock_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activity = context.findActivity()
                        if (activity != null && rewardedAdManager != null) {
                            rewardedAdManager.showAd(
                                activity = activity,
                                onUserEarnedReward = {
                                    isExtendedAudioUnlocked = true
                                    showExtendedAudioDialog = false
                                    Toast.makeText(context, context.getString(R.string.audio_unlocked_success), Toast.LENGTH_SHORT).show()
                                },
                                onAdFailed = {
                                    isExtendedAudioUnlocked = true
                                    showExtendedAudioDialog = false
                                }
                            )
                        } else {
                            isExtendedAudioUnlocked = true
                            showExtendedAudioDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                ) {
                    Text(stringResource(R.string.audio_btn_watch), color = CardWhite)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showExtendedAudioDialog = false
                            onNavigateToUpgrade()
                        }
                    ) {
                        Text(stringResource(R.string.entitlement_btn_upgrade), color = DeepTeal)
                    }
                    TextButton(onClick = { showExtendedAudioDialog = false }) {
                        Text(stringResource(R.string.entitlement_btn_maybe_later), color = CharcoalSubtle)
                    }
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAdvanceQrDialog) {
        val nextOrderNum = orderViewModel.getNextOrderNumber()
        val qrAmount = if (advanceAmount > 0.0) advanceAmount else totalAmount
        UpiPaymentQrDialog(
            shopName = effectiveShopName,
            orderNumber = nextOrderNum,
            balanceAmount = qrAmount,
            upiId = effectiveShopUpiId.ifBlank { "kethepalle@upi" },
            titleAmountLabel = if (advanceAmount > 0.0) "Advance Amount" else "Order Total",
            transactionNote = "Order #$nextOrderNum Advance Payment",
            onDismiss = { showAdvanceQrDialog = false }
        )
    }
}

/**
 * Reusable dialog for editing garment measurements in Order Creation Wizard.
 */
@Composable
private fun OrderMeasurementEditorDialog(
    garmentName: String,
    templateId: String,
    initialMeasurements: Map<String, Double>,
    onDismiss: () -> Unit,
    onSave: (Map<String, Double>) -> Unit
) {
    val measurements = remember {
        mutableStateMapOf<String, Double>().apply {
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
                    text = "Customize Measurements",
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
            LazyColumn(
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
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                Text("Apply Measurements", color = CardWhite)
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
