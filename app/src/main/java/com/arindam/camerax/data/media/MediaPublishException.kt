package com.arindam.camerax.data.media

/**
 * Data: MediaStore publish failure. [expected] failures stay out of Crashlytics; unexpected ones
 * keep their cause so [com.arindam.camerax.util.log.Logger.error] can record them.
 */
sealed class MediaPublishException(
    message: String,
    cause: Throwable? = null,
    val expected: Boolean
) : IllegalStateException(message, cause) {

    class MissingFile : MediaPublishException(MESSAGE, expected = true)

    class NoStoragePermission : MediaPublishException(MESSAGE, expected = true)

    class InsertFailed(cause: Throwable? = null) : MediaPublishException(MESSAGE, cause, expected = true)

    class Unexpected(cause: Throwable) : MediaPublishException(MESSAGE, cause, expected = false)

    companion object {
        const val MESSAGE = "Unable to save to gallery"
    }
}
