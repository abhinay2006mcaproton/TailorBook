package com.tailorbook.feature.measurements.presentation

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.core.ads.AdPlacement
import com.tailorbook.feature.measurements.data.local.entity.MeasurementVersionEntity
import com.tailorbook.feature.measurements.data.repository.MeasurementRepositoryImpl
import com.tailorbook.feature.measurements.domain.model.GarmentTemplates
import com.tailorbook.ui.components.TailorAdaptiveBannerAd
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorEmptyState
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.components.TailorSecondaryButton
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.WarmIvory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Measurement History & Version List (TASK-06-05 / REQ-MEAS-04).
 * Lists all immutable historical versions for a garment with diff comparison trigger.
 */
@Composable
fun MeasurementHistoryScreen(
    customerId: String,
    familyMemberId: String? = null,
    templateId: String,
    customerName: String = "Customer",
    viewModel: MeasurementViewModel,
    onNavigateBack: () -> Unit,
    onRecordNewVersionClick: () -> Unit,
    onCompareVersionsClick: (version1: Int, version2: Int) -> Unit,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    val allVersions by viewModel.inMemoryVersions.collectAsState()
    val versions = remember(allVersions, customerId, familyMemberId, templateId) {
        viewModel.getVersions(customerId, familyMemberId, templateId)
    }
    val template = remember(templateId) { GarmentTemplates.findById(templateId) }

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
                            text = "${stringResource(id = template.titleRes)} History",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = "$customerName • ${versions.size} Versions Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }

                    if (versions.size >= 2) {
                        IconButton(
                            onClick = {
                                val v1 = versions[1].versionNumber
                                val v2 = versions[0].versionNumber
                                onCompareVersionsClick(v1, v2)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = "Compare Versions",
                                tint = SoftGold
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRecordNewVersionClick,
                containerColor = DeepTeal,
                contentColor = CardWhite,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Version"
                )
            }
        },
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        if (versions.isEmpty()) {
            TailorEmptyState(
                icon = Icons.Default.Straighten,
                title = stringResource(id = R.string.measurement_no_records),
                subtitle = "Record the initial measurements for this garment template.",
                actionLabel = stringResource(id = R.string.measurement_take_new),
                onAction = onRecordNewVersionClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Compare action banner if multiple versions exist
                if (versions.size >= 2) {
                    item {
                        TailorElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val v1 = versions[1].versionNumber
                                    val v2 = versions[0].versionNumber
                                    onCompareVersionsClick(v1, v2)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CompareArrows,
                                        contentDescription = null,
                                        tint = DeepTeal,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Compare Latest Changes",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = DeepTeal
                                        )
                                        Text(
                                            text = "See difference between v${versions[1].versionNumber} and v${versions[0].versionNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CharcoalSubtle
                                        )
                                    }
                                }

                                Text(
                                    text = "Diff →",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SoftGold
                                )
                            }
                        }
                    }
                }

                items(versions, key = { it.id }) { version ->
                    VersionCard(version = version)
                }

                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}

@Composable
private fun VersionCard(
    version: MeasurementVersionEntity,
    modifier: Modifier = Modifier
) {
    val measurements = remember(version.measurementsJson) {
        MeasurementRepositoryImpl.parseMeasurements(version.measurementsJson)
    }
    val dateStr = remember(version.measuredAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(version.measuredAt))
    }

    TailorElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DeepTeal)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Version ${version.versionNumber}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Unit: ${version.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CharcoalSubtle
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = CharcoalSubtle
                )
            }

            if (!version.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: \"${version.notes}\"",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = DeepTeal
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dimension Grid Pills
            val entries = measurements.entries.toList()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entries.chunked(3).forEach { rowEntries ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowEntries.forEach { (k, v) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LightBeige)
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$k: $v${if (version.unit == "INCH") "\"" else "cm"}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = Charcoal,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        // Fill empty cells if last row has fewer items
                        repeat(3 - rowEntries.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
