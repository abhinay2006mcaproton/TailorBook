package com.tailorbook.feature.measurements.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl
import com.tailorbook.feature.measurements.domain.model.GarmentTemplates
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusAmber
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.WarmIvory

/**
 * Measurement Comparison (Diff) Screen (TASK-06-06 / REQ-MEAS-04).
 * Compares two measurement versions side-by-side with delta highlighting.
 */
@Composable
fun MeasurementDiffScreen(
    customerId: String,
    familyMemberId: String? = null,
    templateId: String,
    v1Number: Int,
    v2Number: Int,
    customerName: String = "Customer",
    viewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val template = remember(templateId) { GarmentTemplates.findById(templateId) }
    val versions = remember(customerId, familyMemberId, templateId) {
        viewModel.getVersions(customerId, familyMemberId, templateId)
    }

    val ver1 = remember(versions, v1Number) { versions.find { it.versionNumber == v1Number } }
    val ver2 = remember(versions, v2Number) { versions.find { it.versionNumber == v2Number } }

    val map1 = remember(ver1) { ver1?.let { MeasurementRepositoryImpl.parseMeasurements(it.measurementsJson) } ?: emptyMap() }
    val map2 = remember(ver2) { ver2?.let { MeasurementRepositoryImpl.parseMeasurements(it.measurementsJson) } ?: emptyMap() }

    val diffItems = remember(template, map1, map2) {
        template.fields.map { field ->
            val val1 = map1[field.key] ?: 0.0
            val val2 = map2[field.key] ?: 0.0
            val delta = val2 - val1
            DiffRowData(
                field = field,
                val1 = val1,
                val2 = val2,
                delta = delta
            )
        }
    }

    val changedCount = diffItems.count { kotlin.math.abs(it.delta) > 0.001 }

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
                            text = "${stringResource(id = template.titleRes)} Diff",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = "$customerName • Comparing v$v1Number vs v$v2Number",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary Card
            item {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Comparison Summary",
                                style = MaterialTheme.typography.labelSmall,
                                color = CharcoalSubtle
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$changedCount dimension(s) modified",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (changedCount > 0) StatusGreen else DeepTeal
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeepTeal.copy(alpha = 0.10f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "v$v1Number → v$v2Number",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = DeepTeal
                            )
                        }
                    }
                }
            }

            // Table Header Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MEASUREMENT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CharcoalSubtle,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = "v$v1Number",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CharcoalSubtle,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = "v$v2Number",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CharcoalSubtle,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = "DIFF",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CharcoalSubtle,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Diff Rows
            items(diffItems) { row ->
                val hasChanged = kotlin.math.abs(row.delta) > 0.001
                val isIncrease = row.delta > 0.0

                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = row.field.nameEn,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (hasChanged) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (hasChanged) DeepTeal else Charcoal
                            )
                            Text(
                                text = row.field.nameTe,
                                style = MaterialTheme.typography.labelSmall,
                                color = CharcoalSubtle,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "${row.val1}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CharcoalSubtle,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )

                        Text(
                            text = "${row.val2}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (hasChanged) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (hasChanged) DeepTeal else Charcoal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (hasChanged) {
                                val deltaStr = (if (isIncrease) "+" else "") + String.format("%.2f", row.delta).trimEnd('0').trimEnd('.') + "\""
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isIncrease) StatusGreen.copy(alpha = 0.15f) else StatusAmber.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = deltaStr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isIncrease) StatusGreen else StatusAmber
                                    )
                                }
                            } else {
                                Text(
                                    text = "—",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CharcoalSubtle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DiffRowData(
    val field: com.tailorbook.feature.measurements.domain.model.MeasurementField,
    val val1: Double,
    val val2: Double,
    val delta: Double
)
