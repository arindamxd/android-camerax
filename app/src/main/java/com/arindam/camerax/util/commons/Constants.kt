package com.arindam.camerax.util.commons

import android.Manifest
import android.os.Build

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

object Constants {

    object INTERNAL {
        internal const val PREF_NAME = "pref_camerax"
    }

    object EXTRAS {
        internal const val KEY_EVENT_ACTION = "key_event_action"
        internal const val KEY_EVENT_EXTRA = "key_event_extra"
    }

    object PERMISSIONS {
        internal val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    object FILE {
        internal const val FILE_NAME_KEY = "file_name"
        internal val EXTENSION_WHITELIST = arrayOf("JPG")
    }

    object FIRESTORE {
        internal const val DOCUMENT_PATH = "active/quote"
        internal const val KEY_MESSAGE = "message"
        internal const val KEY_AUTHOR = "author"
    }
}
