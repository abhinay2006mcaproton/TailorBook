package com.tailorbook.feature.measurements.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl
import com.tailorbook.feature.measurements.domain.model.GarmentTemplate
import com.tailorbook.feature.measurements.domain.model.GarmentTemplates
import com.tailorbook.feature.measurements.domain.model.MeasurementField
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.WarmIvory

/**
 * Measurement Entry Form (TASK-06-04 / REQ-MEAS-02 / REQ-MEAS-04).
 * Supports tape fraction steps (1/4, 1/2, 3/4"), unit toggle (Inches / CM),
 * tooltips, and saves an immutable next version.
 */
@Composable
fun MeasurementEntryScreen(
    customerId: String,
    familyMemberId: String? = null,
    templateId: String,
    customerName: String = "Customer",
    viewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit,
    onVersionSaved: (versionNumber: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val template = remember(templateId) { GarmentTemplates.findById(templateId) }
    val latestVersion = remember(customerId, familyMemberId, templateId) {
        viewModel.getLatestVersion(customerId, familyMemberId, templateId)
    }
    val nextVersionNumber = remember(customerId, familyMemberId, templateId) {
        viewModel.getNextVersionNumber(customerId, familyMemberId, templateId)
    }

    var unit by remember { mutableStateOf(latestVersion?.unit ?: "INCH") }
    var notes by remember { mutableStateOf("") }

    // Pre-populate with previous version if available, or default values
    val fieldValues = remember {
        val initialMap = mutableStateMapOf<String, String>()
        val parsedPrev = latestVersion?.let { MeasurementRepositoryImpl.parseMeasurements(it.measurementsJson) } ?: emptyMap()

        template.fields.forEach { field ->
            val prevVal = parsedPrev[field.key]
            val defaultVal = if (prevVal != null) {
                prevVal.toString()
            } else if (field.defaultVal > 0.0) {
                field.defaultVal.toString()
            } else {
                ""
            }
            initialMap[field.key] = defaultVal
        }
        initialMap
    }

    var activeFieldKey by remember { mutableStateOf(template.fields.firstOrNull()?.key ?: "") }

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
                            text = "${stringResource(id = template.titleRes)} Measurements",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = "$customerName • New Version (v$nextVersionNumber)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }

                    // Unit Toggle (INCH / CM)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardWhite.copy(alpha = 0.15f))
                            .clickable {
                                unit = if (unit == "INCH") "CM" else "INCH"
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = CardWhite,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    TailorPrimaryButton(
                        text = "Save Version (v$nextVersionNumber)",
                        onClick = {
                            val mapToSave = mutableMapOf<String, Double>()
                            fieldValues.forEach { (k, v) ->
                                v.toDoubleOrNull()?.let { num ->
                                    mapToSave[k] = num
                                }
                            }

                            viewModel.saveMeasurementVersion(
                                customerId = customerId,
                                familyMemberId = familyMemberId,
                                templateId = template.id,
                                values = mapToSave,
                                unit = unit,
                                notes = notes,
                                onSaved = { savedEntity ->
                                    onVersionSaved(savedEntity.versionNumber)
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
            // Tape Fraction Quick Picker (1/4, 1/2, 3/4)
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Tailor's Tape Helper ($unit)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = DeepTeal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ".00 (Flat)" to 0.0,
                                "¼ (.25)" to 0.25,
                                "½ (.50)" to 0.50,
                                "¾ (.75)" to 0.75
                            ).forEach { (label, fraction) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DeepTeal.copy(alpha = 0.08f))
                                        .clickable {
                                            if (activeFieldKey.isNotBlank()) {
                                                val currentVal = fieldValues[activeFieldKey]?.toDoubleOrNull() ?: 0.0
                                                val integerPart = currentVal.toInt()
                                                val newVal = integerPart + fraction
                                                fieldValues[activeFieldKey] = newVal.toString()
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = DeepTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Measurement Fields
            items(template.fields, key = { it.key }) { field ->
                MeasurementFieldRow(
                    field = field,
                    unit = unit,
                    value = fieldValues[field.key] ?: "",
                    isActive = activeFieldKey == field.key,
                    onFocus = { activeFieldKey = field.key },
                    onValueChange = { newVal ->
                        fieldValues[field.key] = newVal
                    },
                    onIncrement = { delta ->
                        val current = fieldValues[field.key]?.toDoubleOrNull() ?: 0.0
                        val updated = (current + delta).coerceAtLeast(0.0)
                        fieldValues[field.key] = String.format("%.2f", updated).trimEnd('0').trimEnd('.')
                    }
                )
            }

            // Fitting Notes Field
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Fitting & Styling Notes (Optional)") },
                    placeholder = { Text("e.g. Needs 1 inch extra margin inside seams") },
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = DeepTeal)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepTeal,
                        unfocusedBorderColor = MutedSage,
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MeasurementFieldRow(
    field: MeasurementField,
    unit: String,
    value: String,
    isActive: Boolean,
    onFocus: () -> Unit,
    onValueChange: (String) -> Unit,
    onIncrement: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    TailorElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Text(
                        text = field.nameEn,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isActive) DeepTeal else Charcoal
                    )
                    if (field.nameTe.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "(${field.nameTe})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = DeepTeal.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        )
                    }

                    if (field.hint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = field.hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalSubtle.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Increment / Decrement Steppers & Value Display Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(LightBeige)
                            .clickable { onIncrement(-0.25) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = DeepTeal,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(94.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardWhite)
                            .border(
                                width = if (isActive) 2.dp else 1.2.dp,
                                color = if (isActive) DeepTeal else CharcoalSubtle.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onFocus() }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = value,
                                onValueChange = onValueChange,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    color = Charcoal,
                                    fontSize = 17.sp
                                ),
                                cursorBrush = SolidColor(DeepTeal),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp, top = 2.dp, bottom = 2.dp)
                            )
                            Text(
                                text = if (unit == "INCH") "\"" else "cm",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DeepTeal.copy(alpha = 0.75f),
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(LightBeige)
                            .clickable { onIncrement(0.25) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = DeepTeal,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
