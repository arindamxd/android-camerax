package com.arindam.camerax.ui.home.camera

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Science
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arindam.camerax.R
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraFontFamily
import com.arindam.camerax.ui.theme.CameraGlassStrong
import com.arindam.camerax.ui.theme.CameraMono
import com.arindam.camerax.ui.theme.CameraOnGlass
import com.arindam.camerax.ui.theme.CameraOnGlassMuted

private enum class OthersRoute {
    HUB,
    EXPERIMENTAL,
    ENGINE
}

@Composable
fun OthersWorkspace(
    state: CameraUiState,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    var route by rememberSaveable { mutableStateOf(OthersRoute.HUB) }
    LaunchedEffect(state.showsTools) {
        if (!state.showsTools) route = OthersRoute.HUB
    }
    if (!state.showsTools) return
    BackHandler(enabled = route != OthersRoute.HUB) {
        route = OthersRoute.HUB
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.72f),
                        Color.Black.copy(alpha = 0.88f)
                    )
                )
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = if (compact) 52.dp else 60.dp,
                bottom = if (compact) 64.dp else 78.dp
            )
    ) {
        when (route) {
            OthersRoute.HUB -> OthersHub(
                onExperimental = { route = OthersRoute.EXPERIMENTAL },
                onEngine = { route = OthersRoute.ENGINE }
            )
            OthersRoute.EXPERIMENTAL -> ExperimentalFeaturesScreen(
                features = state.deviceFeatures,
                onBack = { route = OthersRoute.HUB }
            )
            OthersRoute.ENGINE -> CameraEngineScreen(
                features = state.deviceFeatures,
                onBack = { route = OthersRoute.HUB }
            )
        }
    }
}

@Composable
private fun OthersHub(
    onExperimental: () -> Unit,
    onEngine: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.others_hub_kicker),
            color = CameraAccent,
            fontFamily = CameraMono,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.others_hub_title),
            color = CameraOnGlass,
            fontFamily = CameraFontFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 36.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.others_hub_subtitle),
            color = CameraOnGlassMuted,
            fontFamily = CameraFontFamily,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(28.dp))
        ToolCard(
            icon = Icons.Outlined.Science,
            title = stringResource(R.string.others_experimental_title),
            subtitle = stringResource(R.string.others_experimental_subtitle),
            onClick = onExperimental
        )
        Spacer(Modifier.height(14.dp))
        ToolCard(
            icon = Icons.Outlined.Memory,
            title = stringResource(R.string.others_engine_title),
            subtitle = stringResource(R.string.others_engine_subtitle),
            onClick = onEngine
        )
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CameraGlassStrong)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CameraAccent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CameraAccent,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                color = CameraOnGlass,
                fontFamily = CameraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = CameraOnGlassMuted,
                fontFamily = CameraFontFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = CameraOnGlassMuted
        )
    }
}

@Composable
private fun ExperimentalFeaturesScreen(
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
            title = stringResource(R.string.lab_ultra_hdr_title),
            body = stringResource(R.string.lab_ultra_hdr_body),
            ready = features.ultraHdr
        ),
        LabItem(
            title = stringResource(R.string.lab_raw_title),
            body = stringResource(R.string.lab_raw_body),
            ready = features.rawCapture
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
        kicker = stringResource(R.string.others_experimental_kicker),
        title = stringResource(R.string.others_experimental_title),
        onBack = onBack
    ) {
        items.forEach { item ->
            FeatureTile(item)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CameraEngineScreen(
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
        kicker = stringResource(R.string.others_engine_kicker),
        title = stringResource(R.string.others_engine_title),
        onBack = onBack
    ) {
        EngineHero()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.engine_cameras_heading),
            color = CameraAccent,
            fontFamily = CameraMono,
            fontSize = 11.sp,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(10.dp))
        if (features.cameras.isEmpty()) {
            StatusRow(
                label = stringResource(R.string.engine_cameras_empty),
                value = none,
                ready = false
            )
        } else {
            features.cameras.forEach { camera ->
                StatusRow(
                    label = stringResource(
                        if (camera.lens == CameraLens.FRONT) {
                            R.string.engine_lens_front
                        } else {
                            R.string.engine_lens_back
                        }
                    ),
                    value = camera.id,
                    ready = true
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.engine_capabilities_heading),
            color = CameraAccent,
            fontFamily = CameraMono,
            fontSize = 11.sp,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(10.dp))
        StatusRow(
            label = stringResource(R.string.engine_row_concurrent),
            value = if (features.concurrent) ready else unavailable,
            ready = features.concurrent
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            label = stringResource(R.string.engine_row_slow_motion),
            value = if (features.slowMotion.available) {
                features.slowMotion.frameRates.joinToString { fps -> "$fps fps" }
            } else {
                unavailable
            },
            ready = features.slowMotion.available
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            label = stringResource(R.string.engine_row_fps60),
            value = if (features.videoFps60) ready else unavailable,
            ready = features.videoFps60
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            label = stringResource(R.string.engine_row_ultra_hdr),
            value = if (features.ultraHdr) ready else unavailable,
            ready = features.ultraHdr
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            label = stringResource(R.string.engine_row_raw),
            value = if (features.rawCapture) ready else unavailable,
            ready = features.rawCapture
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            label = stringResource(R.string.engine_row_llb),
            value = if (features.lowLightBoost) ready else unavailable,
            ready = features.lowLightBoost
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            label = stringResource(R.string.engine_row_stab),
            value = if (features.videoStabilization) ready else unavailable,
            ready = features.videoStabilization
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            label = stringResource(R.string.engine_row_video_hdr),
            value = hdrLabel,
            ready = features.videoHdrRanges.any { range -> range != VideoHdrRange.SDR }
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
            label = stringResource(R.string.engine_row_qualities),
            value = qualities,
            ready = features.videoQualities.isNotEmpty()
        )
        Spacer(Modifier.height(8.dp))
        StatusRow(
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
private fun EngineHero() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        CameraAccent.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.06f)
                    )
                )
            )
            .border(1.dp, CameraAccent.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CameraAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                tint = CameraAccent
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = stringResource(R.string.engine_library),
                color = CameraOnGlassMuted,
                fontFamily = CameraMono,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.engine_camerax_version),
                color = CameraOnGlass,
                fontFamily = CameraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.engine_preview_impl),
                color = CameraOnGlassMuted,
                fontFamily = CameraFontFamily,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ToolScreen(
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_button_alt),
                    tint = CameraOnGlass
                )
            }
            Column {
                Text(
                    text = kicker,
                    color = CameraAccent,
                    fontFamily = CameraMono,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp
                )
                Text(
                    text = title,
                    color = CameraOnGlass,
                    fontFamily = CameraFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        content()
    }
}

@Composable
private fun FeatureTile(item: LabItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.title,
                color = CameraOnGlass,
                fontFamily = CameraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            ReadyPill(ready = item.ready)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.body,
            color = CameraOnGlassMuted,
            fontFamily = CameraFontFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, ready: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = CameraOnGlassMuted,
                fontFamily = CameraFontFamily,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = CameraOnGlass,
                fontFamily = CameraMono,
                fontSize = 13.sp
            )
        }
        ReadyPill(ready = ready)
    }
}

@Composable
private fun ReadyPill(ready: Boolean) {
    val fill = if (ready) CameraAccent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)
    val tint = if (ready) CameraAccent else CameraOnGlassMuted
    Text(
        text = stringResource(if (ready) R.string.others_ready else R.string.others_unavailable),
        color = tint,
        fontFamily = CameraMono,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fill)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private data class LabItem(
    val title: String,
    val body: String,
    val ready: Boolean
)
