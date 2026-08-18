package com.arindam.camerax.data.media

import android.content.Context
import com.arindam.camerax.data.camera.PanoramaStitcher
import com.arindam.camerax.domain.repository.MediaRepository
import com.arindam.camerax.util.commons.Constants
import java.io.File
import java.util.Locale

class FileMediaRepository(private val context: Context) : MediaRepository {

    override fun latest(directory: File): File? {
        val files = directory.listFiles { file ->
            Constants.FILE.EXTENSION_WHITELIST.contains(file.extension.lowercase(Locale.US))
        } ?: return null
        val newest = files.maxByOrNull { it.lastModified() } ?: return null
        if (!newest.extension.equals("dng", ignoreCase = true)) return newest
        return files.firstOrNull { file ->
            file.nameWithoutExtension == newest.nameWithoutExtension &&
                (file.extension.equals("jpg", ignoreCase = true) ||
                file.extension.equals("jpeg", ignoreCase = true))
        } ?: newest
    }

    override fun stitchPanorama(frames: List<File>, outputDirectory: File): File = PanoramaStitcher.stitch(frames, outputDirectory)

    override fun publish(file: File) {
        MediaStorePublisher.publish(context, file)
    }
}
