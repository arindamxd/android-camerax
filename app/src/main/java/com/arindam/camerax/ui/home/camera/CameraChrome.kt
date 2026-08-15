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
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Face
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.ExposurePriority
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.NightScene
import com.arindam.camerax.domain.model.StillFormat
import com.arindam.camerax.domain.model.TimerMode
import com.arindam.camerax.ui.theme.CameraAccent
import com.arindam.camerax.ui.theme.CameraDanger
import com.arindam.camerax.ui.theme.CameraGlass
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
    onSettingsClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
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
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.hasFlash) {
                    GlassIconButton(
                        icon = flashIcon(state.flash),
                        contentDescription = stringResource(state.flash.labelRes),
                        selected = state.flash != FlashMode.OFF,
                        compact = compact,
                        onClick = onFlashClicked
                    )
                }
                GlassIconButton(
                    icon = timerIcon(state.timer),
                    contentDescription = stringResource(state.timer.labelRes),
                    selected = state.timer != TimerMode.OFF,
                    compact = compact,
                    onClick = onTimerClicked
                )
                GlassIconButton(
                    icon = if (state.gridEnabled) Icons.Filled.GridOn else Icons.Filled.GridOff,
                    contentDescription = stringResource(
                        if (state.gridEnabled) R.string.grid_on else R.string.grid_off
                    ),
                    selected = state.gridEnabled,
                    compact = compact,
                    onClick = onGridClicked
                )
                if (state.mode == CameraMode.PHOTO) {
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
                        onClick = onMotionClicked
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                GlassIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings),
                    compact = compact,
                    onClick = onSettingsClicked
                )
                if (state.ultraHdrEnabled) {
                    Text(
                        text = if (state.stillFormat == StillFormat.HEIC_ULTRA_HDR) {
                            stringResource(R.string.ultrahdr_heic)
                        } else {
                            stringResource(R.string.ultrahdr)
                        },
                        color = CameraAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
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
                .clip(RoundedCornerShape(24.dp))
                .background(CameraGlass)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Canvas(Modifier.size(10.dp)) {
                drawCircle(CameraDanger.copy(alpha = if (state.isPaused) 0.4f else pulse))
            }
            Text(
                text = if (state.isPaused) {
                    stringResource(R.string.recording_paused)
                } else {
                    formatRecordingTime(state.recordingNanos)
                },
                color = CameraOnGlass,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            GlassIconButton(
                icon = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = stringResource(
                    if (state.isPaused) R.string.resume_recording else R.string.pause_recording
                ),
                compact = true,
                onClick = onPauseClicked
            )
            GlassIconButton(
                icon = if (state.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = stringResource(
                    if (state.isMuted) R.string.unmute_audio else R.string.mute_audio
                ),
                compact = true,
                selected = state.isMuted,
                onClick = onMuteClicked
            )
        }
    }
}

@Composable
fun PanoramaBanner(state: CameraUiState) {
    val visible = state.mode == CameraMode.PANORAMA
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Text(
            text = if (state.panoramaActive) {
                stringResource(R.string.panorama_panning, state.panoramaFrames)
            } else {
                stringResource(R.string.panorama_hint)
            },
            color = CameraAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CameraGlass)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun NightSceneBanner(state: CameraUiState) {
    val visible = state.nightScene == NightScene.RECOMMENDED || state.autoNightActive
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Text(
            text = stringResource(
                if (state.autoNightActive) R.string.night_mode_active else R.string.night_mode_recommended
            ),
            color = CameraAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CameraGlass)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun HybridAeControls(
    state: CameraUiState,
    onPrioritySelected: (ExposurePriority) -> Unit,
    onIsoChanged: (Int) -> Unit,
    onShutterChanged: (Long) -> Unit
) {
    if (state.exposureLimits.supportedPriorities.size < 2 ||
        state.mode == CameraMode.VIDEO ||
        state.mode == CameraMode.PANORAMA
    ) return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(CameraGlass)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            state.exposureLimits.supportedPriorities.forEach { priority ->
                val selected = state.exposurePriority == priority
                Text(
                    text = stringResource(priority.labelRes),
                    color = if (selected) Color.Black else CameraOnGlass,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) CameraAccent else Color.Transparent)
                        .clickable { onPrioritySelected(priority) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        if (state.exposurePriority == ExposurePriority.ISO) {
            val limits = state.exposureLimits
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
                fontSize = 11.sp
            )
        }
        if (state.exposurePriority == ExposurePriority.SHUTTER) {
            val limits = state.exposureLimits
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
                fontSize = 11.sp
            )
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
    if (state.zoomChips.size < 2) return
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CameraGlass)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        state.zoomChips.forEach { ratio ->
            val selected = kotlin.math.abs(state.zoomRatio - ratio) < 0.15f ||
                    (ratio == 1f && state.zoomRatio in 0.85f..1.2f)
            val label = if (ratio < 1f) String.format("%.1f", ratio) else ratio.toInt().toString()
            Text(
                text = "${label}x",
                color = if (selected) Color.Black else CameraOnGlass,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) CameraAccent else Color.Transparent)
                    .clickable { onZoomSelected(ratio) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun EffectsFilmstrip(
    state: CameraUiState,
    onFilterSelected: (ColorFilterType) -> Unit,
    onExtensionSelected: (CameraExtension) -> Unit,
    onFaceDetectionClicked: () -> Unit
) {
    AnimatedVisibility(visible = state.mode == CameraMode.EFFECTS && !state.isRecording) {
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
                ExtensionChip(
                    label = stringResource(R.string.face_detection),
                    selected = state.faceDetectionEnabled,
                    icon = Icons.Filled.Face,
                    onClick = onFaceDetectionClicked
                )
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
    onFaceDetectionClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
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
            .padding(bottom = if (compact) 8.dp else 16.dp)
    ) {
        if (!state.lockCaptureMode) {
            EffectsFilmstrip(
                state = state,
                onFilterSelected = onFilterSelected,
                onExtensionSelected = onExtensionSelected,
                onFaceDetectionClicked = onFaceDetectionClicked
            )
            DiscretePager(
                items = CameraMode.entries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 28.dp else 36.dp),
                itemFraction = 0.28f,
                overshootFraction = 0.75f,
                initialIndex = state.mode.ordinal,
                itemSpacing = 8.dp,
                onItemSelected = onModeSelected
            ) { item ->
                val selected = item == state.mode
                Text(
                    text = stringResource(item.labelRes).uppercase(),
                    color = if (selected) CameraAccent else CameraOnGlassMuted,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (compact) 11.sp else 13.sp,
                    letterSpacing = 1.2.sp
                )
            }
            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                GlassIconButton(
                    icon = Icons.Filled.Cameraswitch,
                    contentDescription = stringResource(R.string.switch_camera_button_alt),
                    compact = compact,
                    onClick = onFlipClicked
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ShutterButton(
                    mode = state.mode,
                    isRecording = state.isRecording,
                    panoramaActive = state.panoramaActive,
                    compact = compact,
                    onClick = onShutterClicked
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (state.lockCaptureMode) {
                    Spacer(Modifier.size(48.dp))
                } else {
                    GalleryThumb(file = state.thumbnail, onClick = onGalleryClicked)
                }
            }
        }
    }
}

@Composable
fun ShutterButton(
    mode: CameraMode,
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
                color = Color.White,
                style = Stroke(width = 4.dp.toPx()),
                radius = size.minDimension / 2f - 3.dp.toPx()
            )
            val inner = size.minDimension * innerScale
            val origin = Offset((size.width - inner) / 2f, (size.height - inner) / 2f)
            val color = when {
                mode == CameraMode.VIDEO || isRecording -> CameraDanger
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
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GalleryThumb(file: File?, onClick: () -> Unit) {
    val model = remember(file) { mediaThumbnail(file) }
    AsyncImage(
        model = model,
        contentDescription = stringResource(R.string.gallery_button_alt),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick
            )
    )
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
        ColorFilterType.VINTAGE -> Color(0xFFD7A86E)
        ColorFilterType.COOL -> Color(0xFF7EC8E3)
        ColorFilterType.WARM -> Color(0xFFFFB74D)
        ColorFilterType.VIVID -> Color(0xFFFF5C8A)
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
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(fill)
                .then(
                    if (selected) Modifier.border(2.dp, CameraAccent, RoundedCornerShape(14.dp))
                    else Modifier
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
    compact: Boolean = false
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(if (compact) 44.dp else 48.dp)
            .clip(CircleShape)
            .background(if (selected) CameraAccent.copy(alpha = 0.9f) else CameraGlass)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Color.Black else CameraOnGlass,
            modifier = Modifier.size(if (compact) 20.dp else 22.dp)
        )
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
