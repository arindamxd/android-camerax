package com.arindam.camerax.ui.settings

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.arindam.camerax.ui.theme.AppTheme

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForNightMode()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

private fun AppCompatActivity.enableEdgeToEdgeForNightMode() {
    val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    val transparent = Color.TRANSPARENT
    val barStyle = if (dark) {
        SystemBarStyle.dark(transparent)
    } else {
        SystemBarStyle.light(transparent, transparent)
    }
    enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
}
