package com.arindam.camerax.data.media

import com.arindam.camerax.util.commons.Constants
import java.io.File
import java.util.Locale

/**
 * Data: capture-file helpers used by [FileMediaRepository] and the in-app gallery.
 * Keep extension checks here so the UI does not reimplement the media whitelist.
 */
internal fun listedCaptures(directory: File): List<File> {
    val files = directory.listFiles { file -> file.isCaptureFile() } ?: return emptyList()
    return files.sortedByDescending { it.lastModified() }
}

internal fun File.isCaptureFile(): Boolean =
    Constants.FILE.EXTENSION_WHITELIST.contains(extension.lowercase(Locale.US))

internal fun File.isVideoCapture(): Boolean = extension.equals("mp4", ignoreCase = true)

internal fun File.isRawCapture(): Boolean = extension.equals("dng", ignoreCase = true)

internal fun File.isStillCapture(): Boolean =
    extension.equals("jpg", ignoreCase = true) ||
        extension.equals("jpeg", ignoreCase = true) ||
        extension.equals("heic", ignoreCase = true)
