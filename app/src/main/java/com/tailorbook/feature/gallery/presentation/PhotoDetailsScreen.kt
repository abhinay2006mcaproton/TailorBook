package com.tailorbook.feature.gallery.presentation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tailorbook.R
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.WarmIvory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Photo Details & Tagging Screen (TASK-07-05 / REQ-GAL-05).
 * Full image view, multi-tag management, notes editor, customer/order linking and deletion.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoDetailsScreen(
    photoId: String,
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    onPhotoDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val design = remember(photoId) { viewModel.getDesignById(photoId) }

    if (design == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmIvory),
            contentAlignment = Alignment.Center
        ) {
            Text("Photo not found", color = Charcoal)
        }
        return
    }

    var notesText by remember { mutableStateOf(design.notes) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val allAvailableTags = listOf(
        "Neck Design",
        "Sleeves",
        "Embroidery",
        "Maggam Work",
        "Border / Lace",
        "Blouse",
        "Kurti",
        "Lehenga",
        "Bridal",
        "Piping"
    )

    val currentTags = remember {
        mutableStateListOf<String>().apply {
            addAll(design.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() })
        }
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
                            text = stringResource(id = R.string.photo_details_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        val dateFormatted = remember(design.createdAt) {
                            SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(design.createdAt))
                        }
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }

                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.photo_delete),
                            tint = SoftGold
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
                    TailorPrimaryButton(
                        text = stringResource(id = R.string.photo_save),
                        onClick = {
                            viewModel.updatePhoto(
                                designId = design.id,
                                tags = currentTags.toList(),
                                notes = notesText,
                                customerId = design.customerId
                            )
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Main Hero Image Display
            TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    DeepTeal.copy(alpha = 0.9f),
                                    SoftGold.copy(alpha = 0.75f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val file = remember(design.localFilePath) { File(design.localFilePath) }
                    if (file.exists() && file.isFile) {
                        val bitmap = remember(design.localFilePath) {
                            try {
                                BitmapFactory.decodeFile(file.absolutePath)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = design.notes,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            val icon = when {
                                currentTags.any { it.contains("Neck", ignoreCase = true) } -> Icons.Default.ContentCut
                                currentTags.any { it.contains("Maggam", ignoreCase = true) || it.contains("Embroidery", ignoreCase = true) } -> Icons.Default.AutoAwesome
                                currentTags.any { it.contains("Sleeve", ignoreCase = true) } -> Icons.Default.Style
                                else -> Icons.Default.PhotoLibrary
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = CardWhite,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentTags.firstOrNull() ?: "Boutique Design",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = CardWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Multi-tag Selection Section
            Text(
                text = stringResource(id = R.string.photo_tags_label),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepTeal
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allAvailableTags.forEach { tag ->
                    val isChecked = currentTags.contains(tag)
                    Surface(
                        color = if (isChecked) DeepTeal else CardWhite,
                        contentColor = if (isChecked) CardWhite else Charcoal,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = if (isChecked) 2.dp else 1.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (isChecked) {
                                    currentTags.remove(tag)
                                } else {
                                    currentTags.add(tag)
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CardWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notes / Tailoring Guidance Field
            Text(
                text = "Pattern Instructions & Notes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepTeal
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.photo_notes_hint),
                        color = CharcoalSubtle
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(CardWhite, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Customer Association Card
            TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(LightBeige, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = DeepTeal
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.photo_link_customer),
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalSubtle
                        )
                        Text(
                            text = if (design.customerId != null) "Linked to Customer Profile" else "General Boutique Reference Catalog",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Charcoal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.photo_delete),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StatusRed
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.photo_delete_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePhoto(design.id)
                        onPhotoDeleted()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.photo_delete),
                        color = StatusRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = stringResource(id = R.string.action_cancel),
                        color = CharcoalSubtle
                    )
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
