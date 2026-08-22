package com.arindam.camerax.util.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.MICROPHONE_PERMISSION

object MicrophonePermission {

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, MICROPHONE_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    fun openAppSettings(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    }
}
