package com.arindam.camerax.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.arindam.camerax.R
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.ui.compose.CameraGlassButton
import com.arindam.camerax.ui.compose.DarkLightPreviews
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.ui.theme.CameraFontFamily
import com.arindam.camerax.ui.theme.CameraMono
import com.arindam.camerax.ui.theme.themedOverlayChrome
import com.arindam.camerax.util.theme.NightMode
import java.util.Locale

/** Renders [settingsSections]. Camera prefs apply through [LoadCaptureSettings] on camera resume. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    features: DeviceCaptureFeatures = DeviceCaptureFeatures(),
    versionLabel: String = "",
    microphonePermissionGranted: Boolean = true,
    onRequestMicrophonePermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val sections = remember(features, versionLabel, microphonePermissionGranted) {
        settingsSections(
            versionLabel = versionLabel.ifEmpty { context.getString(R.string.app_version) },
            videoQualities = features.videoQualities,
            videoHdrRanges = features.videoHdrRanges,
            videoStabilizationAvailable = features.videoStabilization,
            slowMotion = features.slowMotion,
            ultraHdrAvailable = features.ultraHdr,
            rawCaptureAvailable = features.rawCapture,
            fullSensorRawAvailable = features.fullSensorRaw,
            lowLightBoostAvailable = features.lowLightBoost,
            videoFps60Available = features.videoFps60,
            microphonePermissionGranted = microphonePermissionGranted
        )
    }
    val scheme = MaterialTheme.colorScheme

    val chrome = themedOverlayChrome()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CameraGlassButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_button_alt),
                    onClick = onBack,
                    onGlass = chrome.onGlass,
                    glass = chrome.glass,
                    stroke = chrome.stroke
                )
                Text(
                    text = stringResource(R.string.settings),
                    color = scheme.onBackground,
                    fontFamily = CameraFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }
            sections.forEachIndexed { sectionIndex, section ->
                Text(
                    text = stringResource(section.titleRes).uppercase(Locale.US),
                    color = chrome.accent,
                    fontFamily = CameraMono,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(
                        top = if (sectionIndex == 0) 0.dp else 16.dp,
                        bottom = 8.dp
                    )
                )
                SettingsGroupCard {
                    section.items.forEachIndexed { index, row ->
                        SettingsRowView(
                            row = row,
                            prefs = prefs,
                            showDivider = index < section.items.lastIndex,
                            microphonePermissionGranted = microphonePermissionGranted,
                            onRequestMicrophonePermission = onRequestMicrophonePermission
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.disabledAlpha(enabled: Boolean): Modifier =
    if (enabled) this else alpha(0.38f)

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsRowView(
    row: SettingsRow,
    prefs: android.content.SharedPreferences,
    showDivider: Boolean,
    microphonePermissionGranted: Boolean,
    onRequestMicrophonePermission: () -> Unit
) {
    val context = LocalContext.current
    val chrome = themedOverlayChrome()
    when (row) {
        is SettingsRow.Toggle -> {
            val key = stringResource(row.keyRes)
            var on by remember(key) {
                mutableStateOf(prefs.getBoolean(key, row.defaultOn))
            }
            val needsMicrophone = row.requestsMicrophoneWhenUnavailable && !microphonePermissionGranted
            SettingsBaseRow(
                icon = row.icon,
                title = stringResource(row.titleRes),
                subtitle = stringResource(row.subtitleRes),
                enabled = row.enabled,
                onClick = {
                    if (needsMicrophone) {
                        onRequestMicrophonePermission()
                        return@SettingsBaseRow
                    }
                    if (!row.enabled) return@SettingsBaseRow
                    val next = !on
                    on = next
                    prefs.edit().putBoolean(key, next).apply()
                },
                trailing = {
                    Switch(
                        checked = on && row.enabled && !needsMicrophone,
                        enabled = row.enabled && !needsMicrophone,
                        onCheckedChange = { checked ->
                            if (needsMicrophone) {
                                onRequestMicrophonePermission()
                                return@Switch
                            }
                            if (!row.enabled) return@Switch
                            on = checked
                            prefs.edit().putBoolean(key, checked).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = chrome.accent,
                            checkedThumbColor = chrome.onAccent,
                            checkedBorderColor = chrome.accent
                        )
                    )
                }
            )
        }

        is SettingsRow.Choice -> {
            val key = stringResource(row.keyRes)
            var selected by remember(key) {
                mutableStateOf(prefs.getString(key, row.defaultValue) ?: row.defaultValue)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .disabledAlpha(row.enabled)
            ) {
                SettingsBaseRow(
                    icon = row.icon,
                    title = stringResource(row.titleRes),
                    subtitle = stringResource(row.subtitleRes)
                )
                if (row.options.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(start = 64.dp, end = 16.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.options.forEach { option ->
                            val isSelected = row.enabled && selected == option.value
                            FilterChip(
                                selected = isSelected,
                                enabled = row.enabled,
                                onClick = {
                                    if (!row.enabled || selected == option.value) return@FilterChip
                                    selected = option.value
                                    prefs.edit().putString(key, option.value).apply()
                                    if (row.appliesAppTheme) {
                                        NightMode.applyPref(option.value)
                                    }
                                },
                                label = {
                                    Text(
                                        text = stringResource(option.labelRes),
                                        fontFamily = CameraFontFamily,
                                        fontSize = 13.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chrome.accent,
                                    selectedLabelColor = chrome.onAccent,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledSelectedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        is SettingsRow.Link -> {
            val url = stringResource(row.urlRes)
            SettingsBaseRow(
                icon = row.icon,
                title = stringResource(row.titleRes),
                subtitle = stringResource(row.subtitleRes),
                onClick = {
                    openUrlInBrowser(context, url)
                },
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        is SettingsRow.Info -> {
            SettingsBaseRow(
                icon = row.icon,
                title = stringResource(row.titleRes),
                subtitle = row.value
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 64.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsBaseRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    val chrome = themedOverlayChrome()
    val rowModifier = Modifier
        .fillMaxWidth()
        .disabledAlpha(enabled)
        .then(
            if (onClick != null) {
                Modifier.clickable(enabled = enabled, onClick = onClick)
            } else {
                Modifier
            }
        )
        .padding(horizontal = 16.dp, vertical = 14.dp)
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(chrome.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = chrome.accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = CameraFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = CameraFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@DarkLightPreviews
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(onBack = {})
    }
}

/**
 * Presentation: Open http(s) links in a browser so verified App Links for our
 * host do not reopen this app (e.g. Privacy Policy on github.io).
 */
private fun openUrlInBrowser(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        val browserIntent = Intent.makeMainSelectorActivity(
            Intent.ACTION_MAIN,
            Intent.CATEGORY_APP_BROWSER
        ).apply {
            data = uri
        }
        context.startActivity(browserIntent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
