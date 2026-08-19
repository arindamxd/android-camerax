package com.arindam.camerax.ui.settings

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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.arindam.camerax.data.camera.SlowMotionOptions
import com.arindam.camerax.data.camera.isFullSensorRawSupported
import com.arindam.camerax.data.camera.isLowLightBoostSupported
import com.arindam.camerax.data.camera.isRawCaptureSupported
import com.arindam.camerax.data.camera.isUltraHdrSupported
import com.arindam.camerax.data.camera.isVideoFps60Supported
import com.arindam.camerax.data.camera.isVideoStabilizationSupported
import com.arindam.camerax.data.camera.slowMotionOptions
import com.arindam.camerax.data.camera.supportedVideoHdrRanges
import com.arindam.camerax.data.camera.supportedVideoQualities
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import com.arindam.camerax.ui.compose.DarkLightPreviews
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraFontFamily
import com.arindam.camerax.ui.theme.CameraMono
import com.arindam.camerax.util.theme.NightMode
import java.util.Locale

/** Renders [settingsSections]. Camera prefs apply on [com.arindam.camerax.ui.home.camera.CameraFragment] resume. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var videoQualities by remember { mutableStateOf<List<VideoQuality>>(VideoQuality.entries) }
    var videoHdrRanges by remember { mutableStateOf(listOf(VideoHdrRange.SDR)) }
    var videoStabilizationAvailable by remember { mutableStateOf(true) }
    var slowMotion by remember { mutableStateOf(SlowMotionOptions()) }
    var ultraHdrAvailable by remember { mutableStateOf(false) }
    var rawCaptureAvailable by remember { mutableStateOf(false) }
    var fullSensorRawAvailable by remember { mutableStateOf(false) }
    var lowLightBoostAvailable by remember { mutableStateOf(false) }
    var videoFps60Available by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        videoQualities = supportedVideoQualities(context)
        videoHdrRanges = supportedVideoHdrRanges(context)
        videoStabilizationAvailable = isVideoStabilizationSupported(context)
        slowMotion = slowMotionOptions(context)
        ultraHdrAvailable = isUltraHdrSupported(context)
        rawCaptureAvailable = isRawCaptureSupported(context)
        fullSensorRawAvailable = isFullSensorRawSupported(context)
        lowLightBoostAvailable = isLowLightBoostSupported(context)
        videoFps60Available = isVideoFps60Supported(context)
    }
    val sections = remember(
        videoQualities,
        videoHdrRanges,
        videoStabilizationAvailable,
        slowMotion,
        ultraHdrAvailable,
        rawCaptureAvailable,
        fullSensorRawAvailable,
        lowLightBoostAvailable,
        videoFps60Available
    ) {
        settingsSections(
            versionLabel = context.getString(R.string.app_version),
            videoQualities = videoQualities,
            videoHdrRanges = videoHdrRanges,
            videoStabilizationAvailable = videoStabilizationAvailable,
            slowMotion = slowMotion,
            ultraHdrAvailable = ultraHdrAvailable,
            rawCaptureAvailable = rawCaptureAvailable,
            fullSensorRawAvailable = fullSensorRawAvailable,
            lowLightBoostAvailable = lowLightBoostAvailable,
            videoFps60Available = videoFps60Available
        )
    }
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        fontFamily = CameraFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_alt)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.background,
                    titleContentColor = scheme.onBackground,
                    navigationIconContentColor = scheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            sections.forEach { section ->
                Text(
                    text = stringResource(section.titleRes).uppercase(Locale.US),
                    color = CameraAccent,
                    fontFamily = CameraMono,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp)
                )
                SettingsGroupCard {
                    section.items.forEachIndexed { index, row ->
                        SettingsRowView(
                            row = row,
                            prefs = prefs,
                            showDivider = index < section.items.lastIndex
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
    showDivider: Boolean
) {
    val context = LocalContext.current
    when (row) {
        is SettingsRow.Toggle -> {
            val key = stringResource(row.keyRes)
            var on by remember(key) {
                mutableStateOf(prefs.getBoolean(key, row.defaultOn))
            }
            SettingsBaseRow(
                icon = row.icon,
                title = stringResource(row.titleRes),
                subtitle = stringResource(row.subtitleRes),
                enabled = row.enabled,
                onClick = {
                    val next = !on
                    on = next
                    prefs.edit().putBoolean(key, next).apply()
                },
                trailing = {
                    Switch(
                        checked = on && row.enabled,
                        enabled = row.enabled,
                        onCheckedChange = { checked ->
                            on = checked
                            prefs.edit().putBoolean(key, checked).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = CameraAccent,
                            checkedThumbColor = Color.Black,
                            checkedBorderColor = CameraAccent
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
                                    selectedContainerColor = CameraAccent,
                                    selectedLabelColor = Color.Black,
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
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
                .background(CameraAccent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CameraAccent,
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
