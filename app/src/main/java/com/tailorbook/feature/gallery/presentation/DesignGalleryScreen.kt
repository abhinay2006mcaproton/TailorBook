package com.tailorbook.feature.gallery.presentation

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.tailorbook.R
import com.tailorbook.feature.gallery.data.local.entity.DesignReferenceEntity
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorEmptyState
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.DeepTealDark
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.WarmIvory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Design Gallery Grid Screen (TASK-07-04 / REQ-GAL-01).
 * Displays photos tagged by pattern types with camera & file picker integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignGalleryScreen(
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val designs by viewModel.filteredDesigns.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showPickerSheet by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.savePhoto(
                context = context,
                sourceUri = tempCameraUri!!,
                tags = listOf(if (selectedTag != "All") selectedTag else "Neck Design"),
                notes = "Captured on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}"
            )
        }
    }

    // Photo picker launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.savePhoto(
                context = context,
                sourceUri = uri,
                tags = listOf(if (selectedTag != "All") selectedTag else "Embroidery"),
                notes = "Imported from phone gallery"
            )
        }
    }

    val tagsList = listOf(
        "All",
        "Neck Design",
        "Sleeves",
        "Embroidery",
        "Maggam Work",
        "Border / Lace",
        "Blouse",
        "Kurti",
        "Lehenga"
    )

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
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.cd_navigation_menu),
                                tint = CardWhite
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.action_back),
                                tint = CardWhite
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.gallery_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = stringResource(id = R.string.gallery_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }

                    // Direct Add Camera icon button in top bar
                    IconButton(
                        onClick = {
                            val tempFile = File(context.cacheDir, "camera_temp_${System.currentTimeMillis()}.jpg")
                            val authority = "${context.packageName}.fileprovider"
                            try {
                                val uri = FileProvider.getUriForFile(context, authority, tempFile)
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                showPickerSheet = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = stringResource(id = R.string.gallery_camera),
                            tint = SoftGold
                        )
                    }
                }

                // Search field
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "Search designs, tags, notes...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CharcoalSubtle
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = DeepTeal
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = CharcoalSubtle
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .background(CardWhite, RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPickerSheet = true },
                containerColor = DeepTeal,
                contentColor = CardWhite,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.gallery_add_photo)
                )
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
            // Horizontal Tag Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tagsList) { tag ->
                    val isSelected = (tag == selectedTag)
                    Surface(
                        color = if (isSelected) DeepTeal else CardWhite,
                        contentColor = if (isSelected) CardWhite else Charcoal,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = if (isSelected) 3.dp else 1.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.selectTag(tag) }
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Grid of designs
            if (designs.isEmpty()) {
                TailorEmptyState(
                    icon = Icons.Default.Image,
                    title = stringResource(id = R.string.gallery_no_photos),
                    subtitle = stringResource(id = R.string.gallery_no_photos_desc),
                    actionLabel = stringResource(id = R.string.gallery_add_photo),
                    onAction = { showPickerSheet = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(designs, key = { it.id }) { design ->
                        DesignPhotoCard(
                            design = design,
                            onClick = { onPhotoClick(design.id) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Photo Source Selection (Camera vs Gallery)
    if (showPickerSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showPickerSheet = false },
            sheetState = sheetState,
            containerColor = CardWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.gallery_add_photo),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepTeal
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Camera option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showPickerSheet = false
                            val tempFile = File(context.cacheDir, "camera_temp_${System.currentTimeMillis()}.jpg")
                            val authority = "${context.packageName}.fileprovider"
                            val uri = FileProvider.getUriForFile(context, authority, tempFile)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(LightBeige, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = DeepTeal
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.gallery_camera),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Charcoal
                        )
                        Text(
                            text = "Capture fresh client design or fabric",
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalSubtle
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Gallery picker option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showPickerSheet = false
                            galleryPickerLauncher.launch("image/*")
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(LightBeige, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = DeepTeal
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.gallery_pick),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Charcoal
                        )
                        Text(
                            text = "Import reference screenshot or photo",
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalSubtle
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Card displaying a single design reference photo with boutique styling.
 */
@Composable
fun DesignPhotoCard(
    design: DesignReferenceEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TailorElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Photo or sample preview container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                DeepTeal.copy(alpha = 0.85f),
                                SoftGold.copy(alpha = 0.70f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // If local file exists, decode and display
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
                    // Stylized artistic boutique pattern illustration
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val icon = when {
                            design.tags.contains("Neck", ignoreCase = true) -> Icons.Default.ContentCut
                            design.tags.contains("Maggam", ignoreCase = true) || design.tags.contains("Embroidery", ignoreCase = true) -> Icons.Default.AutoAwesome
                            design.tags.contains("Sleeve", ignoreCase = true) -> Icons.Default.Style
                            else -> Icons.Default.PhotoLibrary
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = CardWhite,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = design.tags.split(",").firstOrNull()?.trim() ?: "Design",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                    }
                }
            }

            // Info & Tags Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // First tag pill
                val primaryTag = design.tags.split(",").firstOrNull()?.trim() ?: "Design"
                Surface(
                    color = LightBeige,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = primaryTag,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                if (design.notes.isNotBlank()) {
                    Text(
                        text = design.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Charcoal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                val dateStr = remember(design.createdAt) {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(design.createdAt))
                }
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = CharcoalSubtle
                )
            }
        }
    }
}
