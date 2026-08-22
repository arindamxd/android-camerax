package com.arindam.camerax.ui.home.camera

import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arindam.camerax.R
import com.arindam.camerax.domain.model.CameraLens
import kotlin.math.abs
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arindam.camerax.ui.compose.DarkLightPreviews
import com.arindam.camerax.ui.theme.AppTheme
import androidx.compose.material3.MaterialTheme
import java.io.File
import kotlinx.coroutines.delay

/**
 * Compose viewfinder: [PreviewView] plus overlay chrome from [CameraChrome].
 * Gestures (focus / pinch / drag zoom) stay here; capture goes through [CameraViewModel].
 */
@Composable
fun CameraScreen(
    onGalleryClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onExternalCaptureReady: (File) -> Unit,
    viewModel: CameraViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val inspection = LocalInspectionMode.current
    val configuration = LocalConfiguration.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            isClickable = false
            isFocusable = false
        }
    }

    val pipPreviewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            isClickable = false
            isFocusable = false
        }
    }

    var keepPreview by remember { mutableStateOf(true) }
    LaunchedEffect(
        state.bindRevision,
        state.lens,
        state.extension,
        state.mode,
        state.showsPip,
        previewView
    ) {
        if (inspection) return@LaunchedEffect
        if (state.showsTools) {
            delay(OTHERS_ENTER_MILLIS)
            keepPreview = false
            viewModel.unbindPreview()
        } else {
            keepPreview = true
            viewModel.bind(
                lifecycleOwner,
                previewView,
                pipPreviewView.takeIf { state.showsPip }
            )
            previewView.display?.rotation?.let(viewModel::updateTargetRotation)
        }
    }
    LaunchedEffect(configuration.orientation, configuration.screenWidthDp) {
        if (!inspection) {
            previewView.display?.rotation?.let(viewModel::updateTargetRotation)
        }
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }
    LaunchedEffect(viewModel) {
        viewModel.externalCaptureReady.collect(onExternalCaptureReady)
    }

    DisposableEffect(previewView) {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                if (previewView.display?.displayId == displayId) {
                    previewView.display?.rotation?.let(viewModel::updateTargetRotation)
                }
            }
        }
        displayManager?.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { displayManager?.unregisterDisplayListener(listener) }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onHostStopped()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(state.panoramaActive) {
        if (!state.panoramaActive) return@DisposableEffect onDispose { }
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotation = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                viewModel.onPanoramaYaw(Math.toDegrees(orientation[0].toDouble()).toFloat())
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 480.dp ||
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (state.showsTools) {
                        MaterialTheme.colorScheme.background
                    } else {
                        Color.Black
                    }
                )
        ) {
            if (keepPreview) {
                if (state.showsEffects) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.size(1.dp)
                    )
                    val effectFrame = state.effectFrame
                    if (effectFrame != null) {
                        Image(
                            bitmap = effectFrame,
                            contentDescription = stringResource(R.string.effect_frame_description),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = if (state.lens == CameraLens.FRONT && state.frontMirror) {
                                        -1f
                                    } else {
                                        1f
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (state.showsPip) {
                    AndroidView(
                        factory = { pipPreviewView },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .zIndex(0.5f)
                            .padding(end = 16.dp, bottom = 228.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                            .width(108.dp)
                            .height(144.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
                    .then(
                        if (state.showsTools) {
                            Modifier
                        } else {
                            Modifier.pointerInput(previewView, state.showsZoomChips) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val start = down.position
                                    val slop = viewConfiguration.touchSlop
                                    var dragged = false
                                    var pinch = false
                                    var cumulativeZoom = 1f
                                    if (state.showsZoomChips) {
                                        viewModel.beginZoomGesture()
                                    }
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressed = event.changes.filter { it.pressed }
                                        if (pressed.isEmpty()) break
                                        if (state.showsZoomChips && pressed.size >= 2) {
                                            pinch = true
                                            dragged = true
                                            cumulativeZoom *= event.calculateZoom()
                                            viewModel.zoomByPinch(cumulativeZoom)
                                            pressed.forEach { change ->
                                                if (change.positionChanged()) change.consume()
                                            }
                                        } else if (state.showsZoomChips && !pinch) {
                                            val pointer = pressed.first()
                                            val dx = pointer.position.x - start.x
                                            val dy = pointer.position.y - start.y
                                            if (abs(dx) > slop || abs(dy) > slop) {
                                                if (abs(dy) >= abs(dx)) {
                                                    dragged = true
                                                    viewModel.zoomByDrag(dy, size.height.toFloat())
                                                    if (pointer.positionChanged()) pointer.consume()
                                                } else {
                                                    break
                                                }
                                            }
                                        } else {
                                            val pointer = pressed.first()
                                            val dx = pointer.position.x - start.x
                                            val dy = pointer.position.y - start.y
                                            if (abs(dx) > slop || abs(dy) > slop) dragged = true
                                        }
                                    }
                                    if (!dragged) {
                                        viewModel.tapToFocus(previewView, start)
                                    }
                                }
                            }
                        }
                    )
            )
            if (state.gridEnabled) {
                RuleOfThirdsGrid()
            }
            FocusRing(state.focusPoint)
            AnimatedVisibility(
                visible = state.showsTools,
                modifier = Modifier.zIndex(0.7f),
                enter = fadeIn(tween(OTHERS_ENTER_MILLIS.toInt())) +
                    slideInVertically(tween(OTHERS_ENTER_MILLIS.toInt())) { distance ->
                        distance / 12
                    },
                exit = fadeOut(tween(260)) +
                    slideOutVertically(tween(260)) { distance ->
                        distance / 14
                    }
            ) {
                OthersWorkspace(
                    state = state,
                    compact = compact,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
            ) {
                CameraHeader(
                    state = state,
                    compact = compact,
                    onFlashClicked = viewModel::cycleFlash,
                    onTimerClicked = viewModel::cycleTimer,
                    onGridClicked = viewModel::toggleGrid,
                    onMotionClicked = viewModel::toggleMotionPhoto,
                    onSettingsClicked = onSettingsClicked,
                    onExposurePrioritySelected = viewModel::setExposurePriority,
                    onIsoChanged = viewModel::setIso,
                    onShutterChanged = viewModel::setShutterNanos,
                    onCompensationChanged = viewModel::setExposureCompensation
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LiveStatusStrip(state)
                        RecordingHud(
                            state = state,
                            compact = compact,
                            onPauseClicked = viewModel::pauseOrResume,
                            onMuteClicked = viewModel::toggleMute
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CameraFooter(
                    state = state,
                    compact = compact,
                    onModeSelected = viewModel::setMode,
                    onFlipClicked = viewModel::toggleLens,
                    onShutterClicked = { viewModel.onShutter(previewView) },
                    onGalleryClicked = onGalleryClicked,
                    onEffectSelected = viewModel::setEffect,
                    onZoomSelected = viewModel::setZoom
                )
            }
            CountdownOverlay(state.countdownRemaining)
            state.review?.let { review ->
                AppTheme {
                    CaptureConfirmOverlay(
                        review = review,
                        onRetake = viewModel::retakeCapture,
                        onKeep = viewModel::keepCapture
                    )
                }
            }
        }
    }
}

@DarkLightPreviews
@Composable
private fun CameraChromePreview() {
    val previewState = CameraUiState(
        hasFlash = true,
        gridEnabled = true,
        mode = com.arindam.camerax.domain.model.CameraMode.PHOTO,
        zoomRatio = 1f,
        minZoom = 0.5f,
        maxZoom = 5f
    )
    AppTheme {
        Box(Modifier.fillMaxSize()) {
            CameraHeader(
                state = previewState,
                onFlashClicked = {},
                onTimerClicked = {},
                onGridClicked = {},
                onMotionClicked = {},
                onSettingsClicked = {}
            )
            Box(Modifier.align(Alignment.BottomCenter)) {
                CameraFooter(
                    state = previewState,
                    onModeSelected = {},
                    onFlipClicked = {},
                    onShutterClicked = {},
                    onGalleryClicked = {},
                    onEffectSelected = {}
                )
            }
        }
    }
}

private const val OTHERS_ENTER_MILLIS = 360L
