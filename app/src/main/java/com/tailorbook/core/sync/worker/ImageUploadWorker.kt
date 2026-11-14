package com.tailorbook.core.sync.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tailorbook.core.database.TailorBookDatabase
import com.tailorbook.core.network.SupabaseClient
import com.tailorbook.core.network.SupabaseClientImpl
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for uploading offline design photos to Supabase Storage (TASK-14-04 / REQ-GAL-01).
 * Reads local compressed JPEG files and streams them to the 'shop-designs' Supabase bucket.
 */
class ImageUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    var supabaseClient: SupabaseClient = SupabaseClientImpl()

    override suspend fun doWork(): Result {
        val database = TailorBookDatabase.getInstance(applicationContext)
        val designDao = database.designReferenceDao()

        val pendingUploads = designDao.getPendingUploads()
        if (pendingUploads.isEmpty()) {
            Timber.d("ImageUploadWorker: No pending photo uploads.")
            return Result.success()
        }

        Timber.d("ImageUploadWorker: Uploading ${pendingUploads.size} design photos...")
        var hasFailures = false

        for (item in pendingUploads) {
            val localFile = File(item.localFilePath)
            if (!localFile.exists() || localFile.length() == 0L) {
                Timber.w("Local file missing or empty for design ${item.id}: ${item.localFilePath}")
                designDao.update(item.copy(syncStatus = "FAILED"))
                continue
            }

            val storagePath = "${item.shopId}/${item.id}.jpg"
            val fileBytes = localFile.readBytes()

            val uploadResult = supabaseClient.uploadStorage(
                bucket = BUCKET_SHOP_DESIGNS,
                path = storagePath,
                bytes = fileBytes,
                mimeType = "image/jpeg"
            )

            if (uploadResult.isSuccess) {
                val remoteUrl = uploadResult.getOrNull()
                designDao.update(
                    item.copy(
                        remoteStorageUrl = remoteUrl,
                        syncStatus = "UPLOADED"
                    )
                )
                Timber.d("Successfully uploaded design photo ${item.id} -> $remoteUrl")
            } else {
                val error = uploadResult.exceptionOrNull()
                Timber.e(error, "Failed to upload design photo ${item.id}: ${error?.message}")
                designDao.update(item.copy(syncStatus = "FAILED"))
                hasFailures = true
            }
        }

        return if (hasFailures) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        const val BUCKET_SHOP_DESIGNS = "shop-designs"
        const val WORK_NAME = "tailorbook_image_upload"

        fun triggerImmediateUpload(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ImageUploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Timber.i("Triggered immediate ImageUploadWorker.")
        }
    }
}
