package com.arindam.camerax.ui.home.camera

import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arindam.camerax.domain.model.CameraMode
import com.arindam.camerax.ui.compose.DarkLightPreviews
import com.arindam.camerax.ui.theme.AppTheme
import java.io.File

@Composable
fun CameraScreen(
    outputDirectory: File?,
    onGalleryClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    viewModel: CameraViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val inspection = LocalInspectionMode.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
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
        }
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
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
        CameraHeader(
            state = state,
            onFlashClicked = viewModel::cycleFlash,
            onTimerClicked = viewModel::cycleTimer,
            onGridClicked = viewModel::toggleGrid,
            onSettingsClicked = onSettingsClicked
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
        ) {
            RecordingHud(
                state = state,
                onPauseClicked = viewModel::pauseOrResume,
                onMuteClicked = viewModel::toggleMute
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (state.mode == CameraMode.EFFECTS) 300.dp else 210.dp)
        ) {
            ZoomChips(state = state, onZoomSelected = viewModel::setZoom)
        }
        Box(Modifier.align(Alignment.BottomCenter)) {
            CameraFooter(
                state = state,
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

@DarkLightPreviews
@Composable
private fun CameraChromePreview() {
    val previewState = CameraUiState(
        hasFlash = true,
        gridEnabled = true,
        mode = CameraMode.PHOTO,
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
