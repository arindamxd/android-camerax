package com.arindam.camerax.data.media

import android.media.MediaMetadataRetriever
import com.arindam.camerax.data.camera.StillImageExif
import java.io.File

/**
 * Data: width / height / duration for a capture on disk.
 * [mediaFileInfo] touches the filesystem / [MediaMetadataRetriever] — call off the main thread.
 */
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
    val (width, height) = StillImageExif.displaySize(file)
    return MediaFileInfo(width, height, null)
}
