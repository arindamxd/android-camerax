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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arindam.camerax.ui.compose.DarkLightPreviews
import com.arindam.camerax.ui.theme.AppTheme
import java.io.File

@Composable
fun CameraScreen(
    outputDirectory: File?,
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

    LaunchedEffect(outputDirectory) {
        outputDirectory?.let { viewModel.setOutputDirectory(it) }
    }
    LaunchedEffect(
        state.bindRevision,
        state.lens,
        state.extension,
        state.faceDetectionEnabled,
        previewView
    ) {
        if (!inspection) {
            viewModel.bind(lifecycleOwner, previewView)
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
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
                    .pointerInput(state.isRecording) {
                        detectTapGestures { offset ->
                            viewModel.tapToFocus(previewView, offset)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            viewModel.pinchZoom(zoom)
                        }
                    }
            )
            if (state.gridEnabled) {
                RuleOfThirdsGrid()
            }
            FocusRing(state.focusPoint)
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
                    onSettingsClicked = onSettingsClicked
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NightSceneBanner(state)
                        PanoramaBanner(state)
                        RecordingHud(
                            state = state,
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
                HybridAeControls(
                    state = state,
                    onPrioritySelected = viewModel::setExposurePriority,
                    onIsoChanged = viewModel::setIso,
                    onShutterChanged = viewModel::setShutterNanos
                )
                Spacer(Modifier.height(8.dp))
                ZoomChips(state = state, onZoomSelected = viewModel::setZoom)
                Spacer(Modifier.height(8.dp))
                CameraFooter(
                    state = state,
                    compact = compact,
                    onModeSelected = viewModel::setMode,
                    onFlipClicked = viewModel::toggleLens,
                    onShutterClicked = { viewModel.onShutter(previewView) },
                    onGalleryClicked = onGalleryClicked,
                    onFilterSelected = viewModel::setColorFilter,
                    onExtensionSelected = viewModel::setExtension,
                    onFaceDetectionClicked = viewModel::toggleFaceDetection
                )
            }
            CountdownOverlay(state.countdownRemaining)
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
                    onFilterSelected = {},
                    onExtensionSelected = {},
                    onFaceDetectionClicked = {}
                )
            }
        }
    }
}
