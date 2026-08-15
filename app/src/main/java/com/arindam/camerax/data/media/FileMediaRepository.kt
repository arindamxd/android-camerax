package com.arindam.camerax.data.media

import android.content.Context
import com.arindam.camerax.data.camera.PanoramaStitcher
import com.arindam.camerax.domain.repository.MediaRepository
import com.arindam.camerax.util.commons.Constants
import java.io.File
import java.util.Locale

class FileMediaRepository(private val context: Context) : MediaRepository {

    override fun latest(directory: File): File? = directory.listFiles { file ->
        Constants.FILE.EXTENSION_WHITELIST.contains(file.extension.lowercase(Locale.US))
    }?.maxOrNull()

    override fun stitchPanorama(frames: List<File>, outputDirectory: File): File = PanoramaStitcher.stitch(frames, outputDirectory)

    override fun publish(file: File) {
        MediaStorePublisher.publish(context, file)
    }
}
