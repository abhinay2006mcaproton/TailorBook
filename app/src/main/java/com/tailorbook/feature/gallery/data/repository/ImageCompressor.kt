package com.tailorbook.feature.gallery.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Utility for local image compression and storage (TASK-07-03 / REQ-GAL-03).
 * Compresses incoming camera photos or gallery images to <= 500KB into app-private storage.
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1600
    private const val TARGET_MAX_BYTES = 500 * 1024 // 500 KB

    suspend fun compressAndSave(
        context: Context,
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val galleryDir = File(context.filesDir, "design_photos").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(galleryDir, "design_${UUID.randomUUID()}.jpg")

        // 1. Decode bounds to compute inSampleSize
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        var sampleSize = 1
        val width = options.outWidth
        val height = options.outHeight
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / sampleSize) >= MAX_DIMENSION || (halfHeight / sampleSize) >= MAX_DIMENSION) {
                sampleSize *= 2
            }
        }

        // 2. Decode sampled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565 // Conserves memory
        }

        val decodedBitmap: Bitmap? = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }

        if (decodedBitmap == null) {
            throw IllegalArgumentException("Unable to decode image from uri: $sourceUri")
        }

        // 3. Compress iteratively to keep under TARGET_MAX_BYTES
        var quality = 85
        var compressedBytes: ByteArray
        do {
            val stream = java.io.ByteArrayOutputStream()
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            compressedBytes = stream.toByteArray()
            quality -= 10
        } while (compressedBytes.size > TARGET_MAX_BYTES && quality > 40)

        // 4. Write to app-private file
        FileOutputStream(targetFile).use { out ->
            out.write(compressedBytes)
            out.flush()
        }

        decodedBitmap.recycle()
        targetFile.absolutePath
    }
}
