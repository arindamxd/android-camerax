package com.arindam.camerax.data.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.arindam.camerax.util.log.Logger
import java.io.File

/**
 * Copies captures into DCIM/CameraX so they appear in the system gallery.
 */
object MediaStorePublisher {

    private const val TAG = "MediaStorePublisher"

    fun publish(context: Context, file: File): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        return runCatching {
            val video = file.extension.equals("mp4", ignoreCase = true)
            val mime = when (file.extension.lowercase()) {
                "mp4" -> "video/mp4"
                "heic" -> "image/heic"
                "dng" -> "image/x-adobe-dng"
                else -> "image/jpeg"
            }
            val collection = if (video) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            val uri = context.contentResolver.insert(collection, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            true
        }.onFailure { error ->
            Logger.warning(TAG, "MediaStore publish failed: ${error.message}")
        }.getOrDefault(false)
    }
}
