package com.arindam.camerax.ui.home.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.arindam.camerax.R
import com.arindam.camerax.ui.compose.ChromeActionPill
import com.arindam.camerax.ui.theme.CameraFontFamily
import com.arindam.camerax.ui.theme.CameraMono
import com.arindam.camerax.ui.theme.themedOverlayChrome

/** Shown when camera permission is missing or was denied. */
@Composable
fun PermissionsScreen(
    showSettingsHint: Boolean,
    microphoneDenied: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = themedOverlayChrome()
    val bodyRes = if (showSettingsHint) {
        R.string.permission_screen_settings_body
    } else {
        R.string.permission_screen_body
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(chrome.canvas)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(chrome.scrim.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, chrome.scrim.copy(alpha = 0.62f))
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = chrome.accent,
                    fontFamily = CameraMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chrome.glass)
                        .border(1.dp, chrome.stroke, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(chrome.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = chrome.accent,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = stringResource(R.string.permission_screen_title),
                    color = chrome.onGlass,
                    fontFamily = CameraFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(bodyRes),
                    color = chrome.muted,
                    fontFamily = CameraFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing
                            .union(WindowInsets.systemGestures)
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (microphoneDenied && !showSettingsHint) {
                    Text(
                        text = stringResource(R.string.permission_screen_mic_denied),
                        color = chrome.muted,
                        fontFamily = CameraMono,
                        fontSize = 10.sp,
                        letterSpacing = 0.06.em,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showSettingsHint) {
                        ChromeActionPill(
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Filled.Settings,
                            label = stringResource(R.string.permission_open_settings),
                            filled = true,
                            onClick = onOpenSettings
                        )
                        ChromeActionPill(
                            modifier = Modifier.fillMaxWidth(),
                            icon = null,
                            label = stringResource(R.string.permission_try_again),
                            filled = false,
                            onClick = onRequestPermissions
                        )
                    } else {
                        ChromeActionPill(
                            modifier = Modifier.fillMaxWidth(),
                            icon = null,
                            label = stringResource(R.string.permission_allow),
                            filled = true,
                            onClick = onRequestPermissions
                        )
                    }
                }
            }
        }
    }
}
