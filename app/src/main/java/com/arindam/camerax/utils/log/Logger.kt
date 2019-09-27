package com.arindam.camerax.utils.log

import android.util.Log

/**
 * Created by Arindam Karmakar on 26/9/19.
 */

interface Logger {
    fun log(message: String)
}

private const val TAG = "CameraX: Dynamic Code"
object MainLogger : Logger {
    override fun log(message: String) {
        Log.d(TAG, message)
    }
}
