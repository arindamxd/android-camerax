package com.arindam.camerax.ui.home.camera

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arindam.camerax.R
import com.arindam.camerax.domain.model.BoundSession
import com.arindam.camerax.domain.model.BoundSessionKind
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.domain.model.InstalledCamera
import com.arindam.camerax.domain.model.LastCameraSession
import com.arindam.camerax.domain.model.StillFormat
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import com.arindam.camerax.ui.compose.CameraGlassButton
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraFontFamily
import com.arindam.camerax.ui.theme.CameraMono
import com.arindam.camerax.ui.theme.ThemedOverlayChrome
import com.arindam.camerax.ui.theme.themedOverlayChrome

private enum class OthersRoute {
    HUB,
    EXPERIMENTAL,
    ENGINE,
    SESSION,
    INTENTS
}

@Composable
fun OthersWorkspace(
    state: CameraUiState,
    compact: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var route by rememberSaveable { mutableStateOf(OthersRoute.HUB) }
    LaunchedEffect(state.showsTools) {
        if (!state.showsTools) route = OthersRoute.HUB
    }
    if (!state.showsTools) return
    val chrome = themedOverlayChrome()
    val context = LocalContext.current
    val report = deviceReportText(state)
    val shareTitle = stringResource(R.string.app_name)
    BackHandler {
        if (route != OthersRoute.HUB) {
            route = OthersRoute.HUB
        } else {
            onExit()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(chrome.canvas)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing
                    .union(WindowInsets.systemGestures)
                    .only(WindowInsetsSides.Bottom)
            )
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = if (compact) 52.dp else 60.dp,
                bottom = othersFooterContentClearance(compact)
            )
    ) {
        when (route) {
            OthersRoute.HUB -> OthersHub(
                chrome = chrome,
                onExperimental = { route = OthersRoute.EXPERIMENTAL },
                onEngine = { route = OthersRoute.ENGINE },
                onSession = { route = OthersRoute.SESSION },
                onIntents = { route = OthersRoute.INTENTS },
                onShare = {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                            shareTitle
                        )
                    )
                }
            )
            OthersRoute.EXPERIMENTAL -> ExperimentalFeaturesScreen(
                chrome = chrome,
                compact = compact,
                features = state.deviceFeatures,
                onBack = { route = OthersRoute.HUB }
            )
            OthersRoute.ENGINE -> CameraEngineScreen(
                chrome = chrome,
                compact = compact,
                features = state.deviceFeatures,
                onBack = { route = OthersRoute.HUB }
            )
            OthersRoute.SESSION -> LastSessionScreen(
                chrome = chrome,
                compact = compact,
                session = state.lastSession,
                onBack = { route = OthersRoute.HUB }
            )
            OthersRoute.INTENTS -> CaptureIntentsScreen(
                chrome = chrome,
                compact = compact,
                onBack = { route = OthersRoute.HUB }
            )
        }
    }
}

@Composable
private fun OthersHub(
    chrome: ThemedOverlayChrome,
    onExperimental: () -> Unit,
    onEngine: () -> Unit,
    onSession: () -> Unit,
    onIntents: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.others_hub_kicker).uppercase(),
            color = CameraAccent,
            fontFamily = CameraMono,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
            style = TightHudText
        )
        Text(
            text = stringResource(R.string.others_hub_title),
            color = chrome.onGlass,
            fontFamily = CameraFontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 28.sp,
            style = TightHudText
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.others_hub_subtitle),
            color = chrome.muted,
            fontFamily = CameraFontFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(18.dp))
        ToolCard(
            chrome = chrome,
            icon = Icons.Outlined.Science,
            title = stringResource(R.string.others_experimental_title),
            subtitle = stringResource(R.string.others_experimental_subtitle),
            onClick = onExperimental
        )
        Spacer(Modifier.height(10.dp))
        ToolCard(
            chrome = chrome,
            icon = Icons.Outlined.Memory,
            title = stringResource(R.string.others_engine_title),
            subtitle = stringResource(R.string.others_engine_subtitle),
            onClick = onEngine
        )
        Spacer(Modifier.height(10.dp))
        ToolCard(
            chrome = chrome,
            icon = Icons.Outlined.History,
            title = stringResource(R.string.others_session_title),
            subtitle = stringResource(R.string.others_session_subtitle),
            onClick = onSession
        )
        Spacer(Modifier.height(10.dp))
        ToolCard(
            chrome = chrome,
            icon = Icons.Outlined.Link,
            title = stringResource(R.string.others_intents_title),
            subtitle = stringResource(R.string.others_intents_subtitle),
            onClick = onIntents
        )
        Spacer(Modifier.height(10.dp))
        ToolCard(
            chrome = chrome,
            icon = Icons.Outlined.Share,
            title = stringResource(R.string.others_share_report),
            subtitle = stringResource(R.string.others_share_report_subtitle),
            onClick = onShare
        )
    }
}

@Composable
private fun ToolCard(
    chrome: ThemedOverlayChrome,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(chrome.glass)
            .border(1.dp, chrome.stroke, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CameraAccent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CameraAccent,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = title,
                color = chrome.onGlass,
                fontFamily = CameraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = chrome.muted,
                fontFamily = CameraFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = chrome.muted
        )
    }
}

@Composable
private fun ExperimentalFeaturesScreen(
    chrome: ThemedOverlayChrome,
    compact: Boolean,
    features: DeviceCaptureFeatures,
    onBack: () -> Unit
) {
    val api36 = Build.VERSION.SDK_INT >= 36
    val items = listOf(
        LabItem(
            title = stringResource(R.string.lab_extensions_title),
            body = stringResource(R.string.lab_extensions_body),
            ready = features.extensions.isNotEmpty()
        ),
        LabItem(
            title = stringResource(R.string.lab_night_auto_title),
            body = stringResource(R.string.lab_night_auto_body),
            ready = api36 && CameraExtension.NIGHT in features.extensions
        ),
        LabItem(
            title = stringResource(R.string.lab_hybrid_ae_title),
            body = stringResource(R.string.lab_hybrid_ae_body),
            ready = api36
        ),
        LabItem(
            title = stringResource(R.string.lab_concurrent_title),
            body = stringResource(R.string.lab_concurrent_body),
            ready = features.concurrent
        ),
        LabItem(
            title = stringResource(R.string.lab_high_speed_title),
            body = stringResource(R.string.lab_high_speed_body),
            ready = features.slowMotion.available
        ),
        LabItem(
            title = stringResource(R.string.lab_fps60_title),
            body = stringResource(R.string.lab_fps60_body),
            ready = features.videoFps60
        ),
        LabItem(
            title = stringResource(R.string.lab_jpeg_uhdr_title),
            body = stringResource(R.string.lab_jpeg_uhdr_body),
            ready = features.jpegUltraHdr
        ),
        LabItem(
            title = stringResource(R.string.lab_heic_uhdr_title),
            body = stringResource(R.string.lab_heic_uhdr_body),
            ready = features.heicUltraHdr
        ),
        LabItem(
            title = stringResource(R.string.lab_raw_title),
            body = stringResource(R.string.lab_raw_body),
            ready = features.rawCapture
        ),
        LabItem(
            title = stringResource(R.string.lab_full_raw_title),
            body = stringResource(R.string.lab_full_raw_body),
            ready = features.fullSensorRaw
        ),
        LabItem(
            title = stringResource(R.string.lab_motion_title),
            body = stringResource(R.string.lab_motion_body),
            ready = features.videoQualities.isNotEmpty()
        ),
        LabItem(
            title = stringResource(R.string.lab_persistent_title),
            body = stringResource(R.string.lab_persistent_body),
            ready = features.videoQualities.isNotEmpty()
        ),
        LabItem(
            title = stringResource(R.string.lab_video_hdr_title),
            body = stringResource(R.string.lab_video_hdr_body),
            ready = features.videoHdrRanges.any { range -> range != VideoHdrRange.SDR }
        ),
        LabItem(
            title = stringResource(R.string.lab_llb_title),
            body = stringResource(R.string.lab_llb_body),
            ready = features.lowLightBoost
        )
    )
    ToolScreen(
        chrome = chrome,
        compact = compact,
        kicker = stringResource(R.string.others_experimental_kicker),
        title = stringResource(R.string.others_experimental_title),
        onBack = onBack
    ) {
        items.forEach { item ->
            FeatureTile(chrome = chrome, item = item)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CameraEngineScreen(
    chrome: ThemedOverlayChrome,
    compact: Boolean,
    features: DeviceCaptureFeatures,
    onBack: () -> Unit
) {
    val none = stringResource(R.string.engine_value_none)
    val ready = stringResource(R.string.others_ready)
    val unavailable = stringResource(R.string.others_unavailable)
    val hdrLabel = joinedHdrLabel(features.videoHdrRanges)
    val qualities = joinedQualityLabel(features.videoQualities, none)
    val extensions = joinedExtensionLabel(features.extensions, none)
    ToolScreen(
        chrome = chrome,
        compact = compact,
        kicker = stringResource(R.string.others_engine_kicker),
        title = stringResource(R.string.others_engine_title),
        onBack = onBack
    ) {
        EngineHero(chrome = chrome)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.engine_cameras_heading).uppercase(),
            color = CameraAccent,
            fontFamily = CameraMono,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(8.dp))
        if (features.cameras.isEmpty()) {
            StatusRow(
                chrome = chrome,
                label = stringResource(R.string.engine_cameras_empty),
                value = none,
                ready = false
            )
        } else {
            features.cameras.forEach { camera ->
                StatusRow(
                    chrome = chrome,
                    label = installedCameraLabel(camera),
                    value = camera.id,
                    ready = true
                )
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.engine_capabilities_heading).uppercase(),
            color = CameraAccent,
            fontFamily = CameraMono,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.engine_row_concurrent),
            value = if (features.concurrent) ready else unavailable,
            ready = features.concurrent
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.engine_row_slow_motion),
            value = if (features.slowMotion.available) {
                features.slowMotion.frameRates.joinToString { fps -> "$fps fps" }
            } else {
                unavailable
            },
            ready = features.slowMotion.available
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.engine_row_fps60),
            value = if (features.videoFps60) ready else unavailable,
            ready = features.videoFps60
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.engine_row_ultra_hdr),
            value = if (features.ultraHdr) ready else unavailable,
            ready = features.ultraHdr
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.engine_row_raw),
            value = if (features.rawCapture) ready else unavailable,
            ready = features.rawCapture
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.engine_row_llb),
            value = if (features.lowLightBoost) ready else unavailable,
            ready = features.lowLightBoost
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.engine_row_stab),
            value = if (features.videoStabilization) ready else unavailable,
            ready = features.videoStabilization
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.engine_row_video_hdr),
            value = hdrLabel,
            ready = features.videoHdrRanges.any { range -> range != VideoHdrRange.SDR }
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.engine_row_qualities),
            value = qualities,
            ready = features.videoQualities.isNotEmpty()
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.engine_row_extensions),
            value = extensions,
            ready = features.extensions.isNotEmpty()
        )
    }
}

@Composable
private fun joinedHdrLabel(ranges: List<VideoHdrRange>): String {
    val labels = ArrayList<String>()
    for (range in ranges) {
        if (range != VideoHdrRange.SDR) {
            labels.add(stringResource(hdrRangeRes(range)))
        }
    }
    return if (labels.isEmpty()) {
        stringResource(R.string.engine_value_sdr)
    } else {
        labels.joinToString()
    }
}

@Composable
private fun joinedQualityLabel(qualities: List<VideoQuality>, empty: String): String {
    if (qualities.isEmpty()) return empty
    val labels = ArrayList<String>(qualities.size)
    for (quality in qualities) {
        labels.add(stringResource(qualityRes(quality)))
    }
    return labels.joinToString()
}

@Composable
private fun joinedExtensionLabel(extensions: Set<CameraExtension>, empty: String): String {
    if (extensions.isEmpty()) return empty
    val labels = ArrayList<String>(extensions.size)
    for (extension in extensions) {
        labels.add(stringResource(extension.labelRes))
    }
    return labels.joinToString()
}

private fun hdrRangeRes(range: VideoHdrRange): Int = when (range) {
    VideoHdrRange.HLG10 -> R.string.pref_video_hdr_hlg10
    VideoHdrRange.HDR10 -> R.string.pref_video_hdr_hdr10
    VideoHdrRange.HDR10_PLUS -> R.string.pref_video_hdr_hdr10_plus
    VideoHdrRange.DOLBY_VISION -> R.string.pref_video_hdr_dolby
    VideoHdrRange.SDR -> R.string.pref_video_hdr_sdr
}

private fun qualityRes(quality: VideoQuality): Int = when (quality) {
    VideoQuality.SD -> R.string.pref_video_sd
    VideoQuality.HD -> R.string.pref_video_hd
    VideoQuality.FHD -> R.string.pref_video_fhd
    VideoQuality.UHD -> R.string.pref_video_uhd
}

@Composable
private fun EngineHero(chrome: ThemedOverlayChrome) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        CameraAccent.copy(alpha = 0.22f),
                        chrome.chipIdle
                    )
                )
            )
            .border(1.dp, CameraAccent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CameraAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                tint = CameraAccent,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = stringResource(R.string.engine_library),
                color = chrome.muted,
                fontFamily = CameraMono,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.engine_camerax_version),
                color = chrome.onGlass,
                fontFamily = CameraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.engine_preview_impl),
                color = chrome.muted,
                fontFamily = CameraFontFamily,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ToolScreen(
    chrome: ThemedOverlayChrome,
    compact: Boolean,
    kicker: String,
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CameraGlassButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back_button_alt),
                onClick = onBack,
                compact = compact,
                onGlass = chrome.onGlass,
                glass = chrome.glass,
                stroke = chrome.stroke
            )
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = kicker.uppercase(),
                    color = CameraAccent,
                    fontFamily = CameraMono,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 1.2.sp,
                    style = TightHudText
                )
                Text(
                    text = title,
                    color = chrome.onGlass,
                    fontFamily = CameraFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    style = TightHudText
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun LastSessionScreen(
    chrome: ThemedOverlayChrome,
    compact: Boolean,
    session: LastCameraSession?,
    onBack: () -> Unit
) {
    val none = stringResource(R.string.engine_value_none)
    val ready = stringResource(R.string.others_ready)
    val unavailable = stringResource(R.string.others_unavailable)
    ToolScreen(
        chrome = chrome,
        compact = compact,
        kicker = stringResource(R.string.others_session_kicker),
        title = stringResource(R.string.others_session_title),
        onBack = onBack
    ) {
        if (session == null) {
            Text(
                text = stringResource(R.string.others_session_empty),
                color = chrome.muted,
                fontFamily = CameraFontFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            return@ToolScreen
        }
        val bound = session.bound
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_mode),
            value = stringResource(session.mode.labelRes),
            ready = true
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.others_session_kicker),
            value = stringResource(boundSessionKindRes(bound.kind)),
            ready = true
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_usecases),
            value = boundUseCaseLabel(bound),
            ready = bound.preview
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_fallback),
            value = if (bound.stillsOnlyFallback) ready else unavailable,
            ready = bound.stillsOnlyFallback
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_camera),
            value = bound.cameraId ?: none,
            ready = bound.cameraId != null
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_lens),
            value = stringResource(
                if (bound.lens == CameraLens.FRONT) {
                    R.string.engine_lens_front
                } else {
                    R.string.engine_lens_back
                }
            ),
            ready = true
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_extension),
            value = stringResource(bound.extension.labelRes),
            ready = bound.extension != CameraExtension.NONE
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_still),
            value = stringResource(stillFormatRes(bound.stillFormat)),
            ready = bound.stills
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_hdr),
            value = if (bound.videoHdr == VideoHdrRange.SDR) {
                stringResource(R.string.engine_value_sdr)
            } else {
                stringResource(hdrRangeRes(bound.videoHdr))
            },
            ready = bound.videoHdr != VideoHdrRange.SDR
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_fps60),
            value = if (bound.videoFps60) ready else unavailable,
            ready = bound.videoFps60
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_stab),
            value = if (bound.videoStabilization) ready else unavailable,
            ready = bound.videoStabilization
        )
        Spacer(Modifier.height(6.dp))
        StatusRow(
            chrome = chrome,
            label = stringResource(R.string.session_row_full_raw),
            value = if (bound.rawFullSensor) ready else unavailable,
            ready = bound.rawFullSensor
        )
    }
}

@Composable
private fun CaptureIntentsScreen(
    chrome: ThemedOverlayChrome,
    compact: Boolean,
    onBack: () -> Unit
) {
    ToolScreen(
        chrome = chrome,
        compact = compact,
        kicker = stringResource(R.string.others_intents_kicker),
        title = stringResource(R.string.others_intents_title),
        onBack = onBack
    ) {
        FeatureTile(
            chrome = chrome,
            item = LabItem(
                title = stringResource(R.string.intent_image_title),
                body = stringResource(R.string.intent_image_body),
                ready = true
            )
        )
        Spacer(Modifier.height(8.dp))
        FeatureTile(
            chrome = chrome,
            item = LabItem(
                title = stringResource(R.string.intent_motion_title),
                body = stringResource(R.string.intent_motion_body),
                ready = true
            )
        )
    }
}

@Composable
private fun installedCameraLabel(camera: InstalledCamera): String {
    val facing = stringResource(
        if (camera.lens == CameraLens.FRONT) {
            R.string.engine_lens_front
        } else {
            R.string.engine_lens_back
        }
    )
    val parts = ArrayList<String>(3)
    parts.add(facing)
    camera.zoomLabel?.let { zoom ->
        val label = if (zoom % 1f == 0f) zoom.toInt().toString() else zoom.toString()
        parts.add(stringResource(R.string.engine_zoom_format, label))
    }
    camera.focalMm?.let { mm ->
        parts.add(stringResource(R.string.engine_focal_format, mm))
    }
    return parts.joinToString(" · ")
}

@Composable
private fun boundUseCaseLabel(bound: BoundSession): String {
    val parts = ArrayList<String>(4)
    if (bound.preview) parts.add(stringResource(R.string.session_usecase_preview))
    if (bound.stills) parts.add(stringResource(R.string.session_usecase_stills))
    if (bound.video) parts.add(stringResource(R.string.session_usecase_video))
    if (bound.analysis) parts.add(stringResource(R.string.session_usecase_analysis))
    return if (parts.isEmpty()) stringResource(R.string.engine_value_none) else parts.joinToString()
}

private fun boundSessionKindRes(kind: BoundSessionKind): Int = when (kind) {
    BoundSessionKind.STANDARD -> R.string.session_kind_standard
    BoundSessionKind.HIGH_SPEED -> R.string.session_kind_high_speed
    BoundSessionKind.CONCURRENT -> R.string.session_kind_concurrent
}

private fun stillFormatRes(format: StillFormat): Int = when (format) {
    StillFormat.JPEG -> R.string.session_format_jpeg
    StillFormat.JPEG_ULTRA_HDR -> R.string.session_format_jpeg_uhdr
    StillFormat.HEIC_ULTRA_HDR -> R.string.session_format_heic_uhdr
    StillFormat.RAW_JPEG -> R.string.session_format_raw
}

@Composable
private fun deviceReportText(state: CameraUiState): String {
    val yes = stringResource(R.string.report_yes)
    val no = stringResource(R.string.report_no)
    val none = stringResource(R.string.engine_value_none)
    val features = state.deviceFeatures
    val lines = ArrayList<String>()
    fun kv(label: String, value: String) {
        lines.add("$label: $value")
    }
    fun flag(label: String, on: Boolean) {
        kv(label, if (on) yes else no)
    }

    lines.add(stringResource(R.string.app_name))
    kv(stringResource(R.string.report_version), stringResource(R.string.app_version))
    kv(stringResource(R.string.report_play_store), stringResource(R.string.link_playstore))
    kv(stringResource(R.string.report_library), stringResource(R.string.engine_camerax_version))
    kv(stringResource(R.string.report_android_api), Build.VERSION.SDK_INT.toString())
    kv(stringResource(R.string.report_viewfinder), stringResource(R.string.engine_preview_impl))

    lines.add("")
    lines.add(stringResource(R.string.report_section_cameras))
    if (features.cameras.isEmpty()) {
        lines.add(stringResource(R.string.engine_cameras_empty))
    } else {
        features.cameras.forEach { camera ->
            val details = installedCameraLabel(camera).replace(" · ", ", ")
            lines.add(stringResource(R.string.report_camera_row, camera.id, details))
        }
    }

    lines.add("")
    lines.add(stringResource(R.string.report_section_capabilities))
    kv(
        stringResource(R.string.engine_row_extensions),
        joinedExtensionLabel(features.extensions, none)
    )
    flag(stringResource(R.string.lab_concurrent_title), features.concurrent)
    flag(stringResource(R.string.lab_high_speed_title), features.slowMotion.available)
    flag(stringResource(R.string.lab_fps60_title), features.videoFps60)
    flag(stringResource(R.string.lab_jpeg_uhdr_title), features.jpegUltraHdr)
    flag(stringResource(R.string.lab_heic_uhdr_title), features.heicUltraHdr)
    flag(stringResource(R.string.lab_raw_title), features.rawCapture)
    flag(stringResource(R.string.lab_full_raw_title), features.fullSensorRaw)
    flag(stringResource(R.string.lab_llb_title), features.lowLightBoost)
    flag(stringResource(R.string.engine_row_stab), features.videoStabilization)
    kv(
        stringResource(R.string.engine_row_qualities),
        joinedQualityLabel(features.videoQualities, none)
    )
    kv(
        stringResource(R.string.engine_row_video_hdr),
        joinedHdrLabel(features.videoHdrRanges)
    )

    lines.add("")
    lines.add(stringResource(R.string.report_section_last_bind))
    val session = state.lastSession
    if (session == null) {
        lines.add(stringResource(R.string.others_session_empty))
    } else {
        val bound = session.bound
        kv(stringResource(R.string.session_row_mode), stringResource(session.mode.labelRes))
        kv(
            stringResource(R.string.others_session_kicker),
            stringResource(boundSessionKindRes(bound.kind))
        )
        kv(stringResource(R.string.session_row_usecases), boundUseCaseLabel(bound))
        val lens = stringResource(
            if (bound.lens == CameraLens.FRONT) {
                R.string.engine_lens_front
            } else {
                R.string.engine_lens_back
            }
        )
        kv(
            stringResource(R.string.session_row_camera),
            bound.cameraId?.let { id -> "$id ($lens)" } ?: none
        )
        kv(stringResource(R.string.session_row_still), stringResource(stillFormatRes(bound.stillFormat)))
        kv(stringResource(R.string.session_row_extension), stringResource(bound.extension.labelRes))
        flag(stringResource(R.string.session_row_fallback), bound.stillsOnlyFallback)
        flag(stringResource(R.string.session_row_fps60), bound.videoFps60)
        flag(stringResource(R.string.session_row_stab), bound.videoStabilization)
        kv(
            stringResource(R.string.session_row_hdr),
            if (bound.videoHdr == VideoHdrRange.SDR) {
                stringResource(R.string.engine_value_sdr)
            } else {
                stringResource(hdrRangeRes(bound.videoHdr))
            }
        )
    }
    return lines.joinToString("\n")
}

@Composable
private fun FeatureTile(chrome: ThemedOverlayChrome, item: LabItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chrome.chipIdle)
            .border(1.dp, chrome.stroke, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.title,
                color = chrome.onGlass,
                fontFamily = CameraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            ReadyPill(chrome = chrome, ready = item.ready)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.body,
            color = chrome.muted,
            fontFamily = CameraFontFamily,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun StatusRow(
    chrome: ThemedOverlayChrome,
    label: String,
    value: String,
    ready: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(chrome.chipIdle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = chrome.muted,
                fontFamily = CameraFontFamily,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = chrome.onGlass,
                fontFamily = CameraMono,
                fontSize = 12.sp
            )
        }
        ReadyPill(chrome = chrome, ready = ready)
    }
}

@Composable
private fun ReadyPill(chrome: ThemedOverlayChrome, ready: Boolean) {
    val fill = if (ready) CameraAccent.copy(alpha = 0.18f) else chrome.chipIdle
    val tint = if (ready) CameraAccent else chrome.muted
    Text(
        text = stringResource(
            if (ready) R.string.others_ready else R.string.others_unavailable
        ).uppercase(),
        color = tint,
        fontFamily = CameraMono,
        fontSize = 9.sp,
        letterSpacing = 0.7.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fill)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private val TightHudText = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

private data class LabItem(
    val title: String,
    val body: String,
    val ready: Boolean
)
