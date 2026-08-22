package com.arindam.camerax.util.log

import android.util.Log
import com.arindam.camerax.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * App-wide log helper. Debug/info stay in debug builds; warnings and errors always go to logcat
 * so Play Console / Crashlytics breadcrumbs survive a release build.
 */
object Logger {

    fun info(tag: String, text: String?) {
        if (BuildConfig.DEBUG) Log.i(tag, text ?: "")
    }

    fun debug(tag: String, text: String?) {
        if (BuildConfig.DEBUG) Log.d(tag, text ?: "")
    }

    fun warning(tag: String, text: String?, error: Throwable? = null) {
        if (error != null) {
            Log.w(tag, text ?: "", error)
        } else {
            Log.w(tag, text ?: "")
        }
    }

    fun error(tag: String, text: String?, error: Throwable? = null) {
        if (error != null) {
            Log.e(tag, text ?: "", error)
        } else {
            Log.e(tag, text ?: "")
        }
        if (BuildConfig.DEBUG) return
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            if (!text.isNullOrBlank()) crashlytics.log("$tag: $text")
            if (error != null) crashlytics.recordException(error)
        }
    }
}
