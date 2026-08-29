package com.arindam.camerax.data.media

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.arindam.camerax.util.log.Logger
import java.io.File

/**
 * Data: copies captures into DCIM/CameraX so they appear in the system gallery.
 * Call via [com.arindam.camerax.domain.usecase.PublishMedia] — MediaStore I/O stays off the main thread.
 */
object MediaStorePublisher {

    private const val TAG = "MediaStorePublisher"

    fun publish(context: Context, file: File): Result<Unit> {
        if (!file.exists() || file.length() <= 0L) {
            return Result.failure(MediaPublishException.MissingFile())
        }
        val sdk = Build.VERSION.SDK_INT
        if (shouldSkipLegacyStoragePublish(sdk, hasLegacyWritePermission(context))) {
            return Result.failure(MediaPublishException.NoStoragePermission())
        }
        val video = file.extension.equals("mp4", ignoreCase = true)
        val mime = mimeFor(file)
        val values = contentValues(file, mime, sdk)
        var lastError: Throwable? = null
        for (collection in mediaStoreCollections(video, sdk)) {
            var pending: Uri? = null
            try {
                val uri = context.contentResolver.insert(collection, ContentValues(values))
                    ?: continue
                pending = uri
                val copied = context.contentResolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                    true
                } ?: false
                if (!copied) {
                    abandonPending(pending) { row ->
                        context.contentResolver.delete(row, null, null)
                    }
                    pending = null
                    continue
                }
                if (sdk >= Build.VERSION_CODES.Q) {
                    val done = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, done, null, null)
                }
                return Result.success(Unit)
            } catch (error: Exception) {
                lastError = error
                Logger.warning(TAG, "MediaStore publish failed: ${error.message}", error)
                abandonPending(pending) { row ->
                    context.contentResolver.delete(row, null, null)
                }
            }
        }
        return if (lastError != null) {
            Result.failure(MediaPublishException.Unexpected(lastError))
        } else {
            Result.failure(MediaPublishException.InsertFailed())
        }
    }
}

internal fun shouldSkipLegacyStoragePublish(sdkInt: Int, hasWritePermission: Boolean): Boolean =
    sdkInt <= Build.VERSION_CODES.P && !hasWritePermission

internal fun mediaStoreCollections(video: Boolean, sdkInt: Int): List<Uri> {
    val legacy = if (video) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    if (sdkInt < Build.VERSION_CODES.Q) return listOf(legacy)
    val primary = if (video) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }
    val external = if (video) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    }
    return listOf(primary, external, legacy).distinct()
}

internal fun abandonPending(uri: Uri?, delete: (Uri) -> Unit) {
    if (uri == null) return
    runCatching { delete(uri) }
}

private fun hasLegacyWritePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

private fun mimeFor(file: File): String = when (file.extension.lowercase()) {
    "mp4" -> "video/mp4"
    "heic" -> "image/heic"
    "dng" -> "image/x-adobe-dng"
    else -> "image/jpeg"
}

private fun contentValues(file: File, mime: String, sdkInt: Int): ContentValues =
    ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
        put(MediaStore.MediaColumns.MIME_TYPE, mime)
        if (sdkInt >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + "/CameraX"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        } else {
            put(
                MediaStore.MediaColumns.DATA,
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    "CameraX/${file.name}"
                ).apply { parentFile?.mkdirs() }.absolutePath
            )
        }
    }
