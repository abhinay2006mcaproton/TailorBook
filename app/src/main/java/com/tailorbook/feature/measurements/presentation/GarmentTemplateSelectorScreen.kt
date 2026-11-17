package com.tailorbook.feature.measurements.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import com.tailorbook.core.ads.RewardedAdManager
import com.tailorbook.feature.measurements.domain.model.GarmentCategory
import com.tailorbook.feature.measurements.domain.model.GarmentTemplate
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.WarmIvory

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Garment Template Selector Screen (TASK-06-03 / REQ-MEAS-01).
 * Displays 8 standard garment types with categories and point counts.
 */
@Composable
fun GarmentTemplateSelectorScreen(
    customerId: String,
    familyMemberId: String? = null,
    customerName: String = "Customer",
    viewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit,
    onTemplateSelected: (templateId: String) -> Unit,
    isPremium: Boolean = false,
    rewardedAdManager: RewardedAdManager? = null,
    onNavigateToUpgrade: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val templates by viewModel.filteredTemplates.collectAsState()
    val allVersions by viewModel.inMemoryVersions.collectAsState()

    var showTemplateDialog by remember { mutableStateOf(false) }
    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<GarmentTemplate?>(null) }

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
                            text = if (customerId.isBlank()) "Garment Templates & Pricing" else stringResource(id = R.string.measurement_templates_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = if (customerId.isBlank()) "Configure measurements & standard prices" else "For: $customerName",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }

                    IconButton(
                        onClick = {
                            editingTemplate = null
                            if (!isPremium) {
                                showRewardedAdDialog = true
                            } else {
                                showTemplateDialog = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Template",
                            tint = CardWhite
                        )
                    }
                }
            }
        },
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.onCategoryFilterSelected(null) },
                    label = { Text("All (${templates.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepTeal,
                        selectedLabelColor = CardWhite
                    )
                )

                FilterChip(
                    selected = selectedCategory == GarmentCategory.WOMEN,
                    onClick = { viewModel.onCategoryFilterSelected(GarmentCategory.WOMEN) },
                    label = { Text("Women") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepTeal,
                        selectedLabelColor = CardWhite
                    )
                )

                FilterChip(
                    selected = selectedCategory == GarmentCategory.MEN,
                    onClick = { viewModel.onCategoryFilterSelected(GarmentCategory.MEN) },
                    label = { Text("Men") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepTeal,
                        selectedLabelColor = CardWhite
                    )
                )

                FilterChip(
                    selected = selectedCategory == GarmentCategory.UNISEX,
                    onClick = { viewModel.onCategoryFilterSelected(GarmentCategory.UNISEX) },
                    label = { Text("Unisex") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepTeal,
                        selectedLabelColor = CardWhite
                    )
                )
            }

            // Grid of Garments
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(templates, key = { it.id }) { template ->
                    val existingVersions = allVersions.filter {
                        it.customerId == customerId &&
                        it.familyMemberId == familyMemberId &&
                        it.templateId.equals(template.id, ignoreCase = true)
                    }
                    val latestVersion = existingVersions.maxByOrNull { it.versionNumber }

                    GarmentCard(
                        template = template,
                        latestVersionNumber = if (customerId.isNotBlank()) latestVersion?.versionNumber else null,
                        onClick = {
                            if (customerId.isNotBlank()) {
                                onTemplateSelected(template.id)
                            } else {
                                editingTemplate = template
                                showTemplateDialog = true
                            }
                        },
                        onEdit = {
                            editingTemplate = template
                            showTemplateDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showTemplateDialog) {
        GarmentTemplateEditorDialog(
            template = editingTemplate,
            onDismiss = {
                showTemplateDialog = false
                editingTemplate = null
            },
            onSave = { id, nameEn, nameTe, category, fields, price ->
                viewModel.saveCustomTemplate(
                    id = id,
                    nameEn = nameEn,
                    nameTe = nameTe,
                    category = category,
                    fields = fields,
                    defaultPrice = price
                )
                showTemplateDialog = false
                editingTemplate = null
            },
            onDelete = { id ->
                viewModel.deleteCustomTemplate(id)
                showTemplateDialog = false
                editingTemplate = null
            }
        )
    }

    if (showRewardedAdDialog) {
        AlertDialog(
            onDismissRequest = { showRewardedAdDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.template_unlock_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.template_unlock_desc),
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
                                    showRewardedAdDialog = false
                                    showTemplateDialog = true
                                },
                                onAdFailed = {
                                    showRewardedAdDialog = false
                                    showTemplateDialog = true
                                }
                            )
                        } else {
                            showRewardedAdDialog = false
                            showTemplateDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                ) {
                    Text(stringResource(R.string.template_btn_watch), color = CardWhite)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRewardedAdDialog = false
                        onNavigateToUpgrade()
                    }
                ) {
                    Text(stringResource(R.string.entitlement_btn_upgrade), color = DeepTeal)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun GarmentCard(
    template: GarmentTemplate,
    latestVersionNumber: Int?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = if (template.titleRes != 0) stringResource(id = template.titleRes) else template.getDisplayName()

    TailorElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DeepTeal.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = template.icon,
                        contentDescription = displayName,
                        tint = DeepTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (latestVersionNumber != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftGold.copy(alpha = 0.20f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v$latestVersionNumber",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Template",
                            tint = CharcoalSubtle,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepTeal,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${template.fields.size} Measurement Points",
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalSubtle
            )

            if (template.defaultPrice > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = SoftGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Price: ₹${template.defaultPrice.toInt()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (latestVersionNumber != null) "View / Edit" else "Record",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SoftGold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun GarmentTemplateEditorDialog(
    template: GarmentTemplate?,
    onDismiss: () -> Unit,
    onSave: (id: String?, nameEn: String, nameTe: String?, category: GarmentCategory, fields: List<com.tailorbook.feature.measurements.domain.model.MeasurementField>, defaultPrice: Double) -> Unit,
    onDelete: (id: String) -> Unit
) {
    val isEditing = template != null
    val templateId = template?.id
    var nameEn by remember { mutableStateOf(template?.getDisplayName() ?: "") }
    var nameTe by remember { mutableStateOf(template?.customNameTe ?: "") }
    var selectedCategory by remember { mutableStateOf(template?.category ?: GarmentCategory.WOMEN) }
    var priceText by remember {
        mutableStateOf(template?.defaultPrice?.takeIf { it > 0 }?.toInt()?.toString() ?: "350")
    }

    val fields = remember {
        mutableStateListOf<com.tailorbook.feature.measurements.domain.model.MeasurementField>().apply {
            if (template != null) {
                addAll(template.fields)
            } else {
                // Pre-populate with typical measurement points
                addAll(
                    listOf(
                        com.tailorbook.feature.measurements.domain.model.MeasurementField("shoulder", "Shoulder", "భుజం", defaultVal = 14.5),
                        com.tailorbook.feature.measurements.domain.model.MeasurementField("length", "Length", "పొడవు", defaultVal = 36.0),
                        com.tailorbook.feature.measurements.domain.model.MeasurementField("chest_bust", "Chest / Bust", "ఛాతీ", defaultVal = 36.0),
                        com.tailorbook.feature.measurements.domain.model.MeasurementField("waist", "Waist", "నడుము", defaultVal = 32.0),
                        com.tailorbook.feature.measurements.domain.model.MeasurementField("hip", "Hip", "సీటు", defaultVal = 38.0)
                    )
                )
            }
        }
    }

    var newPointName by remember { mutableStateOf("") }
    var newPointDefaultVal by remember { mutableStateOf("15.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Garment Template" else "Add New Garment Template",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepTeal
            )
        },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text("Garment Name (English)*") },
                        placeholder = { Text("e.g. Lehenga, Anarkali, Sherwani") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = nameTe,
                        onValueChange = { nameTe = it },
                        label = { Text("Garment Name (Telugu - Optional)") },
                        placeholder = { Text("e.g. లెహంగా") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Charcoal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GarmentCategory.values().forEach { cat ->
                            val isSel = selectedCategory == cat
                            Surface(
                                color = if (isSel) DeepTeal else LightBeige,
                                contentColor = if (isSel) CardWhite else Charcoal,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Standard Stitching Price (₹)") },
                        placeholder = { Text("e.g. 350") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Measurement Points (${fields.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                }

                items(fields.size) { index ->
                    val field = fields[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBeige, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = field.nameEn,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = DeepTeal
                            )
                            Text(
                                text = "Default: ${field.defaultVal}\" • Key: ${field.key}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CharcoalSubtle
                            )
                        }
                        IconButton(
                            onClick = { fields.removeAt(index) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove field",
                                tint = CharcoalSubtle,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                item {
                    // Add new point inline
                    Text(
                        text = "+ Add Measurement Point",
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
                            placeholder = { Text("e.g. Ghera Flare") },
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = newPointDefaultVal,
                            onValueChange = { newPointDefaultVal = it },
                            label = { Text("Default (\")") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        IconButton(
                            onClick = {
                                if (newPointName.isNotBlank()) {
                                    val key = newPointName.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
                                    val defVal = newPointDefaultVal.toDoubleOrNull() ?: 10.0
                                    fields.add(
                                        com.tailorbook.feature.measurements.domain.model.MeasurementField(
                                            key = key,
                                            nameEn = newPointName.trim(),
                                            nameTe = newPointName.trim(),
                                            defaultVal = defVal
                                        )
                                    )
                                    newPointName = ""
                                    newPointDefaultVal = "15.0"
                                }
                            },
                            modifier = Modifier
                                .background(DeepTeal, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add point",
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
                onClick = {
                    if (nameEn.isNotBlank() && fields.isNotEmpty()) {
                        val parsedPrice = priceText.toDoubleOrNull() ?: 0.0
                        onSave(templateId, nameEn, nameTe.ifBlank { null }, selectedCategory, fields.toList(), parsedPrice)
                    }
                },
                enabled = nameEn.isNotBlank() && fields.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
            ) {
                Text("Save Template", color = CardWhite)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing && template?.isCustom == true && templateId != null) {
                    TextButton(
                        onClick = { onDelete(templateId) },
                        colors = ButtonDefaults.textButtonColors(contentColor = com.tailorbook.ui.theme.StatusRed)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Charcoal)
                }
            }
        }
    )
}
