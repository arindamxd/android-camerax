package com.arindam.camerax.data.media

import com.arindam.camerax.domain.repository.MediaRepository
import com.arindam.camerax.util.commons.Constants
import java.io.File
import java.util.Locale

class FileMediaRepository : MediaRepository {
    override fun latest(directory: File): File? =
        directory.listFiles { file ->
            Constants.FILE.EXTENSION_WHITELIST.contains(file.extension.lowercase(Locale.US))
        }?.maxOrNull()
}
