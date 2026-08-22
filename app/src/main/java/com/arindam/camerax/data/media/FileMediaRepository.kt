package com.arindam.camerax.data.media

import android.content.Context
import android.os.Environment
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.PanoramaStitcher
import com.arindam.camerax.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Data: files in the app pictures directory — latest thumbnail, gallery list, panorama stitch,
 * MediaStore publish. Disk work runs on [io].
 */
class FileMediaRepository(
    private val context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : MediaRepository {

    @Volatile
    private var cachedPictures: File? = null

    override fun picturesDirectory(): File {
        cachedPictures?.let { return it }
        synchronized(this) {
            cachedPictures?.let { return it }
            val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { root ->
                File(root, context.getString(R.string.app_name)).apply { mkdirs() }
            }
            val directory = if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
            cachedPictures = directory
            return directory
        }
    }

    override suspend fun latest(directory: File): File? = withContext(io) {
        val files = listedCaptures(directory)
        val newest = files.firstOrNull() ?: return@withContext null
        if (!newest.extension.equals("dng", ignoreCase = true)) return@withContext newest
        files.firstOrNull { file ->
            file.nameWithoutExtension == newest.nameWithoutExtension &&
                (file.extension.equals("jpg", ignoreCase = true) ||
                    file.extension.equals("jpeg", ignoreCase = true))
        } ?: newest
    }

    override suspend fun list(directory: File): List<File> = withContext(io) {
        listedCaptures(directory)
    }

    override suspend fun delete(file: File): Boolean = withContext(io) {
        file.exists() && file.delete()
    }

    override suspend fun stitchPanorama(frames: List<File>, outputDirectory: File): Result<File> =
        withContext(io) {
            runCatching { PanoramaStitcher.stitch(frames, outputDirectory) }
        }

    override suspend fun publish(file: File): Result<Unit> = withContext(io) {
        if (MediaStorePublisher.publish(context, file)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Unable to save to gallery"))
        }
    }
}
