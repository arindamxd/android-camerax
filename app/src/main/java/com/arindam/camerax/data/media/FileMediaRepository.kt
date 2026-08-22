package com.arindam.camerax.data.media

import android.content.Context
import android.os.Environment
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.PanoramaStitcher
import com.arindam.camerax.domain.repository.MediaRepository
import com.arindam.camerax.util.commons.Constants
import java.io.File
import java.util.Locale

/**
 * Files in the app pictures directory: latest thumbnail, gallery list, panorama stitch,
 * MediaStore publish.
 */
class FileMediaRepository(private val context: Context) : MediaRepository {

    override fun picturesDirectory(): File {
        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
            File(it, context.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }

    override fun latest(directory: File): File? {
        val files = list(directory)
        val newest = files.firstOrNull() ?: return null
        if (!newest.extension.equals("dng", ignoreCase = true)) return newest
        return files.firstOrNull { file ->
            file.nameWithoutExtension == newest.nameWithoutExtension &&
                (file.extension.equals("jpg", ignoreCase = true) ||
                    file.extension.equals("jpeg", ignoreCase = true))
        } ?: newest
    }

    override fun list(directory: File): List<File> {
        val files = directory.listFiles { file ->
            Constants.FILE.EXTENSION_WHITELIST.contains(file.extension.lowercase(Locale.US))
        } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }
    }

    override fun delete(file: File): Boolean = file.exists() && file.delete()

    override fun stitchPanorama(frames: List<File>, outputDirectory: File): Result<File> =
        runCatching { PanoramaStitcher.stitch(frames, outputDirectory) }

    override fun publish(file: File): Result<Unit> =
        if (MediaStorePublisher.publish(context, file)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Unable to save to gallery"))
        }
}
