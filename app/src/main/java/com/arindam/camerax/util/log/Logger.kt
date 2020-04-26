package com.arindam.camerax.util.log

import android.util.Log
import com.arindam.camerax.BuildConfig

/**
 * Created by Arindam Karmakar on 26/9/19.
 */

private const val TAG = "CameraX: Dynamic Code"

object Logger {

    fun info(tag: String, text: String?) {
        if (BuildConfig.DEBUG) Log.i(tag, text ?: "")
    }

    fun debug(tag: String, text: String?) {
        if (BuildConfig.DEBUG) Log.d(tag, text ?: "")
    }

    fun warning(tag: String, text: String?) {
        if (BuildConfig.DEBUG) Log.w(tag, text ?: "")
    }

    fun error(tag: String, text: String?) {
        if (BuildConfig.DEBUG) Log.e(tag, text ?: "")
    }
}
