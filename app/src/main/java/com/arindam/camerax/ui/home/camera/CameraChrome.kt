package com.arindam.camerax.ui.home.camera

import android.media.MediaMetadataRetriever
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arindam.camerax.R
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraMode
import com.arindam.camerax.domain.model.CaptureAction
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.ExposurePriority
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.NightScene
import com.arindam.camerax.domain.model.StillFormat
import com.arindam.camerax.domain.model.TimerMode
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraDanger
import com.arindam.camerax.ui.theme.CameraFontFamily
import com.arindam.camerax.ui.theme.CameraGlass
import com.arindam.camerax.ui.theme.CameraGlassStrong
import com.arindam.camerax.ui.theme.CameraMono
import com.arindam.camerax.ui.theme.CameraOnGlass
import com.arindam.camerax.ui.theme.CameraOnGlassMuted
import java.io.File

@Composable
fun CameraHeader(
    state: CameraUiState,
    compact: Boolean = false,
    onFlashClicked: () -> Unit,
    onTimerClicked: () -> Unit,
    onGridClicked: () -> Unit,
    onMotionClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onExposurePrioritySelected: (ExposurePriority) -> Unit = {},
    onIsoChanged: (Int) -> Unit = {},
    onShutterChanged: (Long) -> Unit = {},
    onCompensationChanged: (Int) -> Unit = {}
) {
    var exposureOpen by remember { mutableStateOf(false) }
    LaunchedEffect(state.showsExposureControls) {
        if (!state.showsExposureControls) exposureOpen = false
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent)
                )
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val hasQuickActions = state.showsFlash ||
                    state.showsTimer ||
                    state.showsGrid ||
                    state.showsMotion ||
                    state.showsExposureControls
                if (hasQuickActions) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(28.dp))
                            .background(CameraGlassStrong)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                if (state.showsFlash) {
                    GlassIconButton(
                        icon = flashIcon(state.flash),
                        contentDescription = stringResource(state.flash.labelRes),
                        selected = state.flash != FlashMode.OFF,
                        compact = compact,
                        embedded = true,
                        tooltip = true,
                        onClick = onFlashClicked
                    )
                }
                if (state.showsTimer) {
                    GlassIconButton(
                        icon = timerIcon(state.timer),
                        contentDescription = stringResource(state.timer.labelRes),
                        selected = state.timer != TimerMode.OFF,
                        compact = compact,
                        embedded = true,
                        tooltip = true,
                        onClick = onTimerClicked
                    )
                }
                if (state.showsGrid) {
                    GlassIconButton(
                        icon = if (state.gridEnabled) Icons.Filled.GridOn else Icons.Filled.GridOff,
                        contentDescription = stringResource(
                            if (state.gridEnabled) R.string.grid_on else R.string.grid_off
                        ),
                        selected = state.gridEnabled,
                        compact = compact,
                        embedded = true,
                        tooltip = true,
                        onClick = onGridClicked
                    )
                }
                if (state.showsMotion) {
                    GlassIconButton(
                        icon = if (state.motionPhotoEnabled) {
                            Icons.Filled.MotionPhotosOn
                        } else {
                            Icons.Filled.MotionPhotosOff
                        },
                        contentDescription = stringResource(
                            if (state.motionPhotoEnabled) R.string.motion_photo_on else R.string.motion_photo_off
                        ),
                        selected = state.motionPhotoEnabled,
                        compact = compact,
                        embedded = true,
                        tooltip = true,
                        onClick = onMotionClicked
                    )
                }
                if (state.showsExposureControls) {
                    val exposureActive = exposureOpen ||
                        state.exposurePriority != ExposurePriority.AUTO ||
                        state.exposureCompensation != 0
                    GlassIconButton(
                        icon = Icons.Filled.Exposure,
                        contentDescription = stringResource(R.string.exposure_button),
                        selected = exposureActive,
                        compact = compact,
                        embedded = true,
                        tooltip = true,
                        onClick = { exposureOpen = !exposureOpen }
                    )
                }
                    }
                } else {
                    Spacer(Modifier.size(1.dp))
                }
                GlassIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings),
                    compact = compact,
                    tooltip = true,
                    onClick = onSettingsClicked
                )
            }
        if (exposureOpen && state.showsExposureControls) {
            ExposureControls(
                state = state,
                compact = compact,
                onPrioritySelected = onExposurePrioritySelected,
                onIsoChanged = onIsoChanged,
                onShutterChanged = onShutterChanged,
                onCompensationChanged = onCompensationChanged
            )
        }
    }
}

@Composable
fun RecordingHud(
    state: CameraUiState,
    onPauseClicked: () -> Unit,
    onMuteClicked: () -> Unit
) {
    AnimatedVisibility(
        visible = state.isRecording,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "recPulse"
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(CameraGlassStrong)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Canvas(Modifier.size(8.dp)) {
                drawCircle(CameraDanger.copy(alpha = if (state.isPaused) 0.4f else pulse))
            }
            Text(
                text = if (state.isPaused) {
                    stringResource(R.string.recording_paused)
                } else {
                    formatRecordingTime(state.recordingNanos)
                },
                color = CameraOnGlass,
                fontFamily = CameraMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            GlassIconButton(
                icon = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = stringResource(
                    if (state.isPaused) R.string.resume_recording else R.string.pause_recording
                ),
                compact = true,
                embedded = true,
                onClick = onPauseClicked
            )
            if (state.allowsAudioMute) {
                GlassIconButton(
                    icon = if (state.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = stringResource(
                        if (state.isMuted) R.string.unmute_audio else R.string.mute_audio
                    ),
                    compact = true,
                    embedded = true,
                    selected = state.isMuted,
                    onClick = onMuteClicked
                )
            }
        }
    }
}

@Composable
fun LiveStatusStrip(state: CameraUiState) {
    val chips = buildList {
        if (state.showsStillBadge) {
            add(
                when {
                    state.stillFormat == StillFormat.RAW_JPEG -> stringResource(R.string.raw_dng)
                    state.stillFormat == StillFormat.HEIC_ULTRA_HDR ->
                        stringResource(R.string.ultrahdr_heic)
                    else -> stringResource(R.string.ultrahdr)
                }
            )
        }
        if (state.showsNightHint && state.autoNightActive) add(stringResource(R.string.night_mode_active))
        else if (state.showsNightHint && state.nightScene == NightScene.RECOMMENDED) {
            add(stringResource(R.string.night_mode_recommended))
        }
        if (state.showsPip) add(stringResource(R.string.dual_live_hint))
        if (state.captureAction == CaptureAction.PANORAMA) {
            add(
                if (state.panoramaActive) {
                    stringResource(R.string.panorama_panning, state.panoramaFrames)
                } else {
                    stringResource(R.string.panorama_hint)
                }
            )
        }
        if (state.showsSlowMotionFps &&
            state.slowMotionSupported &&
            state.slowMotionFps > 0
        ) {
            add(stringResource(R.string.slow_motion_fps, state.slowMotionFps))
        }
        if (state.showsVideoStatus && state.videoStabilizationActive) {
            add(stringResource(R.string.video_stabilization_on))
        }
        when (state.videoHdrBound) {
            VideoHdrRange.HLG10 ->
                if (state.showsVideoStatus) add(stringResource(R.string.video_hdr_hlg))
            VideoHdrRange.HDR10 ->
                if (state.showsVideoStatus) add(stringResource(R.string.video_hdr_hdr10))
            VideoHdrRange.HDR10_PLUS ->
                if (state.showsVideoStatus) add(stringResource(R.string.video_hdr_hdr10_plus))
            VideoHdrRange.DOLBY_VISION ->
                if (state.showsVideoStatus) add(stringResource(R.string.video_hdr_dolby))
            VideoHdrRange.SDR -> Unit
        }
        if (state.videoFps60Active && state.showsVideoStatus) {
            add(stringResource(R.string.video_fps_60_on))
        }
        if (state.lowLightBoostActive && state.showsLowLightBoost) {
            add(stringResource(R.string.low_light_boost_on))
        }
    }
    AnimatedVisibility(
        visible = chips.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState())
                .clip(RoundedCornerShape(20.dp))
                .background(CameraGlassStrong)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            chips.forEach { label ->
                Text(
                    text = label,
                    color = CameraAccent,
                    fontFamily = CameraMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ExposureControls(
    state: CameraUiState,
    compact: Boolean = false,
    onPrioritySelected: (ExposurePriority) -> Unit,
    onIsoChanged: (Int) -> Unit,
    onShutterChanged: (Long) -> Unit,
    onCompensationChanged: (Int) -> Unit
) {
    val limits = state.exposureLimits
    val showHybrid = limits.supportedPriorities.size >= 2
    val showEv = limits.evSupported && limits.evMax > limits.evMin
    if (!showHybrid && !showEv) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        if (showHybrid) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(CameraGlass)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                limits.supportedPriorities.forEach { priority ->
                    val selected = state.exposurePriority == priority
                    Text(
                        text = stringResource(priority.labelRes),
                        color = if (selected) Color.Black else CameraOnGlass,
                        fontFamily = CameraMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (compact) 11.sp else 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) CameraAccent else Color.Transparent)
                            .clickable { onPrioritySelected(priority) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            if (state.exposurePriority == ExposurePriority.ISO) {
                Slider(
                    value = state.iso.toFloat(),
                    onValueChange = { onIsoChanged(it.toInt()) },
                    valueRange = limits.isoMin.toFloat()..limits.isoMax.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .padding(top = 4.dp)
                )
                Text(
                    text = stringResource(R.string.iso_value, state.iso),
                    color = CameraOnGlass,
                    fontFamily = CameraMono,
                    fontSize = 11.sp
                )
            }
            if (state.exposurePriority == ExposurePriority.SHUTTER) {
                Slider(
                    value = state.shutterNanos.toFloat(),
                    onValueChange = { onShutterChanged(it.toLong()) },
                    valueRange = limits.shutterMinNanos.toFloat()..limits.shutterMaxNanos.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .padding(top = 4.dp)
                )
                Text(
                    text = stringResource(R.string.shutter_value, shutterLabel(state.shutterNanos)),
                    color = CameraOnGlass,
                    fontFamily = CameraMono,
                    fontSize = 11.sp
                )
            }
        }
        if (showEv) {
            val ev = state.exposureCompensation * limits.evStep
            val evLabel = when {
                ev > 0.05f -> "+%.1f".format(ev)
                ev < -0.05f -> "%.1f".format(ev)
                else -> "0.0"
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .padding(top = if (showHybrid) 8.dp else 0.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CameraGlass)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.exposure_ev, evLabel),
                    color = CameraAccent,
                    fontFamily = CameraMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Slider(
                    value = state.exposureCompensation.toFloat(),
                    onValueChange = { onCompensationChanged(it.toInt()) },
                    valueRange = limits.evMin.toFloat()..limits.evMax.toFloat(),
                    steps = (limits.evMax - limits.evMin - 1).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun shutterLabel(nanos: Long): String {
    if (nanos <= 0L) return "—"
    val seconds = nanos / 1_000_000_000.0
    return if (seconds >= 1.0) {
        String.format("%.1fs", seconds)
    } else {
        "1/%d".format((1.0 / seconds).toInt().coerceAtLeast(1))
    }
}

@Composable
fun ZoomChips(
    state: CameraUiState,
    onZoomSelected: (Float) -> Unit
) {
    if (!state.showsZoomChips || state.zoomChips.size < 2) return
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(CameraGlassStrong)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        state.zoomChips.forEach { ratio ->
            val selected = kotlin.math.abs(state.activeZoomChip - ratio) < 0.12f
            val label = when {
                ratio < 1f && kotlin.math.abs(ratio - 0.5f) < 0.08f -> ".5×"
                ratio < 1f -> String.format("%.1f×", ratio)
                else -> "${ratio.toInt()}×"
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) CameraAccent else Color.White.copy(alpha = 0.08f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = { onZoomSelected(ratio) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.Black else CameraOnGlass,
                    fontFamily = CameraMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (ratio < 1f) 11.sp else 12.sp
                )
            }
        }
    }
}

@Composable
fun EffectsFilmstrip(
    state: CameraUiState,
    onFilterSelected: (ColorFilterType) -> Unit,
    onExtensionSelected: (CameraExtension) -> Unit
) {
    AnimatedVisibility(visible = state.showsFilters && !state.isRecording) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ColorFilterType.entries.forEach { filter ->
                    FilterThumb(
                        filter = filter,
                        selected = state.colorFilter == filter,
                        onClick = { onFilterSelected(filter) }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExtensionChip(
                    label = stringResource(R.string.extension_none),
                    selected = state.extension == CameraExtension.NONE,
                    onClick = { onExtensionSelected(CameraExtension.NONE) }
                )
                state.supportedExtensions.forEach { extension ->
                    ExtensionChip(
                        label = stringResource(extension.labelRes),
                        selected = state.extension == extension,
                        onClick = { onExtensionSelected(extension) }
                    )
                }
            }
        }
    }
}

@Composable
fun CameraFooter(
    state: CameraUiState,
    compact: Boolean = false,
    onModeSelected: (CameraMode) -> Unit,
    onFlipClicked: () -> Unit,
    onShutterClicked: () -> Unit,
    onGalleryClicked: () -> Unit,
    onFilterSelected: (ColorFilterType) -> Unit,
    onExtensionSelected: (CameraExtension) -> Unit,
    onZoomSelected: (Float) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f))
                )
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing
                    .union(WindowInsets.systemGestures)
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(bottom = if (compact) 8.dp else 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ZoomChips(state = state, onZoomSelected = onZoomSelected)
        if (state.showsZoomChips && state.zoomChips.size >= 2) {
            Spacer(Modifier.height(10.dp))
        }
        if (!state.lockCaptureMode) {
            EffectsFilmstrip(
                state = state,
                onFilterSelected = onFilterSelected,
                onExtensionSelected = onExtensionSelected
            )
            val modes = state.visibleModes
            key(state.slowMotionSupported, state.concurrentSupported) {
                DiscretePager(
                    items = modes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 32.dp else 40.dp),
                    itemFraction = 0.24f,
                    overshootFraction = 0.75f,
                    initialIndex = modes.indexOf(state.mode).coerceAtLeast(0),
                    itemSpacing = 4.dp,
                    onItemSelected = onModeSelected
                ) { item ->
                    val selected = item == state.mode
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(item.labelRes).uppercase(),
                            color = if (selected) CameraAccent else CameraOnGlassMuted,
                            fontFamily = if (selected) CameraFontFamily else CameraMono,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = if (compact) 12.sp else 13.sp,
                            letterSpacing = if (selected) 0.8.sp else 1.4.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(if (selected) 18.dp else 0.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (selected) CameraAccent else Color.Transparent)
                        )
                    }
                }
            }
            Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sideSize = if (compact) 44.dp else 48.dp
            Spacer(Modifier.weight(1f))
            if (state.lockCaptureMode) {
                Spacer(Modifier.size(sideSize))
            } else {
                GalleryThumb(
                    file = state.thumbnail,
                    size = sideSize,
                    onClick = onGalleryClicked
                )
            }
            Spacer(Modifier.weight(1f))
            ShutterButton(
                recordsVideo = state.recordsVideo,
                isRecording = state.isRecording,
                panoramaActive = state.panoramaActive,
                compact = compact,
                onClick = onShutterClicked
            )
            Spacer(Modifier.weight(1f))
            if (state.showsFlipControl) {
                GlassIconButton(
                    icon = Icons.Filled.Cameraswitch,
                    contentDescription = stringResource(R.string.switch_camera_button_alt),
                    diameter = sideSize,
                    onClick = onFlipClicked
                )
            } else {
                Spacer(Modifier.size(sideSize))
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun ShutterButton(
    recordsVideo: Boolean,
    isRecording: Boolean,
    panoramaActive: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val innerScale by animateFloatAsState(
        if (isRecording || panoramaActive) 0.42f else 0.78f,
        label = "shutterScale"
    )
    val corner by animateFloatAsState(
        if (isRecording || panoramaActive) 0.22f else 0.5f,
        label = "shutterCorner"
    )
    Box(
        modifier = Modifier
            .size(if (compact) 64.dp else 84.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                style = Stroke(width = 3.dp.toPx()),
                radius = size.minDimension / 2f - 2.dp.toPx()
            )
            val inner = size.minDimension * innerScale
            val origin = Offset((size.width - inner) / 2f, (size.height - inner) / 2f)
            val color = when {
                recordsVideo || isRecording -> CameraDanger
                panoramaActive -> CameraAccent
                else -> Color.White
            }
            drawRoundRect(
                color = color,
                topLeft = origin,
                size = Size(inner, inner),
                cornerRadius = CornerRadius(inner * corner, inner * corner)
            )
        }
    }
}

@Composable
fun RuleOfThirdsGrid() {
    Canvas(Modifier.fillMaxSize()) {
        val color = Color.White.copy(alpha = 0.28f)
        val stroke = 1.dp.toPx()
        drawLine(color, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), stroke)
        drawLine(
            color,
            Offset(size.width * 2f / 3f, 0f),
            Offset(size.width * 2f / 3f, size.height),
            stroke
        )
        drawLine(color, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), stroke)
        drawLine(
            color,
            Offset(0f, size.height * 2f / 3f),
            Offset(size.width, size.height * 2f / 3f),
            stroke
        )
    }
}

@Composable
fun FocusRing(point: Offset?) {
    AnimatedVisibility(
        visible = point != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (point == null) return@AnimatedVisibility
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = CameraAccent,
                topLeft = Offset(point.x - 36.dp.toPx(), point.y - 36.dp.toPx()),
                size = Size(72.dp.toPx(), 72.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun CountdownOverlay(value: Int?) {
    if (value == null) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            color = Color.White,
            fontFamily = CameraFontFamily,
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GalleryThumb(
    file: File?,
    size: Dp,
    onClick: () -> Unit
) {
    val model = remember(file) { file?.let { mediaThumbnail(it) } }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(CameraGlassStrong)
            .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = stringResource(R.string.gallery_button_alt),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Photo,
                contentDescription = stringResource(R.string.gallery_button_alt),
                tint = CameraOnGlass,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FilterThumb(
    filter: ColorFilterType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fill = when (filter) {
        ColorFilterType.NONE -> Color.White
        ColorFilterType.MONO -> Color(0xFFBDBDBD)
        ColorFilterType.INVERT -> Color(0xFF212121)
        ColorFilterType.VINTAGE -> Color(0xFFD7A86E)
        ColorFilterType.COOL -> Color(0xFF7EC8E3)
        ColorFilterType.WARM -> Color(0xFFFFB74D)
        ColorFilterType.VIVID -> Color(0xFFFF5C8A)
        ColorFilterType.BRIGHT -> Color(0xFFFFF59D)
        ColorFilterType.CONTRAST -> Color(0xFF90A4AE)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = false),
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(fill)
                .then(
                    if (selected) Modifier.border(2.dp, CameraAccent, CircleShape)
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(filter.labelRes),
            color = if (selected) CameraAccent else CameraOnGlass,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ExtensionChip(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CameraAccent else CameraGlass)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.Black else CameraOnGlass,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            color = if (selected) Color.Black else CameraOnGlass,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    compact: Boolean = false,
    embedded: Boolean = false,
    tooltip: Boolean = false,
    diameter: Dp? = null
) {
    val background = when {
        selected -> CameraAccent
        embedded -> Color.White.copy(alpha = 0.08f)
        else -> CameraGlassStrong
    }
    val size = diameter ?: if (compact || embedded) 40.dp else 44.dp
    val interactionSource = remember { MutableInteractionSource() }
    var showTooltip by remember { mutableStateOf(false) }
    var skipClick by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    if (tooltip) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        skipClick = false
                        delay(android.view.ViewConfiguration.getLongPressTimeout().toLong())
                        skipClick = true
                        showTooltip = true
                    }
                    else -> showTooltip = false
                }
            }
        }
    }
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .then(if (embedded) Modifier else Modifier.minimumInteractiveComponentSize())
                .size(size)
                .clip(CircleShape)
                .background(background)
                .then(
                    if (embedded) Modifier
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    onClick = {
                        if (!skipClick) onClick()
                        skipClick = false
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) Color.Black else CameraOnGlass,
                modifier = Modifier.size(
                if (diameter != null) 22.dp else if (compact || embedded) 18.dp else 20.dp
            )
            )
        }
        if (tooltip && showTooltip) {
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val gap = with(density) { 8.dp.roundToPx() }
                        val margin = with(density) { 8.dp.roundToPx() }
                        val x = (anchorBounds.left +
                            (anchorBounds.width - popupContentSize.width) / 2)
                            .coerceIn(
                                margin,
                                (windowSize.width - popupContentSize.width - margin)
                                    .coerceAtLeast(margin)
                            )
                        return IntOffset(x, anchorBounds.bottom + gap)
                    }
                },
                properties = PopupProperties(focusable = false, clippingEnabled = false)
            ) {
                Text(
                    text = contentDescription,
                    color = CameraOnGlass,
                    fontFamily = CameraFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.88f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun flashIcon(mode: FlashMode) = when (mode) {
    FlashMode.OFF -> Icons.Filled.FlashOff
    FlashMode.ON -> Icons.Filled.FlashOn
    FlashMode.AUTO -> Icons.Filled.FlashAuto
    FlashMode.TORCH -> Icons.Filled.FlashlightOn
}

private fun timerIcon(mode: TimerMode) = when (mode) {
    TimerMode.OFF -> Icons.Filled.TimerOff
    TimerMode.THREE -> Icons.Filled.Timer3
    TimerMode.TEN -> Icons.Filled.Timer
}

internal fun mediaThumbnail(file: File?): Any {
    if (file == null) return R.drawable.ic_photo
    if (file.extension.lowercase() != "mp4") return file
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        retriever.getFrameAtTime(0) ?: R.drawable.ic_camera_video
    } catch (_: Exception) {
        R.drawable.ic_camera_video
    } finally {
        retriever.release()
    }
}
