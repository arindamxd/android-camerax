package com.arindam.camerax.data.media

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import java.io.File

/** Width / height / duration for a capture on disk. */
data class MediaFileInfo(
    val width: Int,
    val height: Int,
    val durationNanos: Long? = null
)

fun mediaFileInfo(file: File, video: Boolean = file.extension.equals("mp4", ignoreCase = true)): MediaFileInfo {
    if (video) {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val width = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.toIntOrNull() ?: 0
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            MediaFileInfo(width, height, durationMs * 1_000_000L)
        } catch (_: Exception) {
            MediaFileInfo(0, 0, null)
        } finally {
            retriever.release()
        }
    }
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    return MediaFileInfo(options.outWidth, options.outHeight, null)
}
