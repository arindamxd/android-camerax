package com.arindam.camerax.util.commons

import android.Manifest
import android.os.Build

/** Util: shared permission, filename, and intent extra constants. */
object Constants {

    /** Camera / microphone / legacy storage permission names. */
    object PERMISSIONS {
        /** Must be granted before opening the camera. */
        internal val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        /** Requested together on first launch; microphone may stay denied (muted video). */
        internal val RUNTIME_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        internal val MICROPHONE_PERMISSION = Manifest.permission.RECORD_AUDIO
    }

    /** Capture filename keys, whitelist, and extensions. */
    object FILE {
        internal const val FILE_NAME_KEY = "file_name"
        internal val EXTENSION_WHITELIST = arrayOf("jpg", "jpeg", "heic", "dng", "mp4")
        internal const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        internal const val PHOTO_EXTENSION = ".jpg"
        internal const val HEIC_EXTENSION = ".heic"
        internal const val DNG_EXTENSION = ".dng"
        internal const val VIDEO_EXTENSION = ".mp4"
    }

    /** Volume-key and other activity intent extras. */
    object EXTRAS {
        const val KEY_EVENT_ACTION = "key_event_action"
        const val KEY_EVENT_EXTRA = "key_event_extra"
    }

    /** Shared animation durations for chrome transitions. */
    object UI {
        const val ANIMATION_FAST_MILLIS = 50L
        const val ANIMATION_SLOW_MILLIS = 100L
    }
}
