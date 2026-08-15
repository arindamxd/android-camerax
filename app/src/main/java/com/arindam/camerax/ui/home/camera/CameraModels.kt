package com.arindam.camerax.ui.home.camera

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import com.arindam.camerax.R
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.CameraMode
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.TimerMode
import java.io.File

val CameraMode.labelRes: Int
    @StringRes get() = when (this) {
        CameraMode.PHOTO -> R.string.mode_photo
        CameraMode.VIDEO -> R.string.mode_video
        CameraMode.EFFECTS -> R.string.mode_effects
    }

val FlashMode.labelRes: Int
    @StringRes get() = when (this) {
        FlashMode.OFF -> R.string.flash_off
        FlashMode.ON -> R.string.flash_on
        FlashMode.AUTO -> R.string.flash_auto
        FlashMode.TORCH -> R.string.flash_torch
    }

val TimerMode.labelRes: Int
    @StringRes get() = when (this) {
        TimerMode.OFF -> R.string.timer_off
        TimerMode.THREE -> R.string.timer_3
        TimerMode.TEN -> R.string.timer_10
    }

val CameraExtension.labelRes: Int
    @StringRes get() = when (this) {
        CameraExtension.NONE -> R.string.extension_none
        CameraExtension.HDR -> R.string.extension_hdr
        CameraExtension.NIGHT -> R.string.extension_night
        CameraExtension.PORTRAIT -> R.string.extension_portrait
        CameraExtension.BEAUTY -> R.string.extension_beauty
    }

val ColorFilterType.labelRes: Int
    @StringRes get() = when (this) {
        ColorFilterType.NONE -> R.string.filter_none
        ColorFilterType.MONO -> R.string.filter_mono
        ColorFilterType.VINTAGE -> R.string.filter_vintage
        ColorFilterType.COOL -> R.string.filter_cool
        ColorFilterType.WARM -> R.string.filter_warm
        ColorFilterType.VIVID -> R.string.filter_vivid
    }

data class CameraUiState(
    val mode: CameraMode = CameraMode.PHOTO,
    val lens: CameraLens = CameraLens.BACK,
    val flash: FlashMode = FlashMode.OFF,
    val timer: TimerMode = TimerMode.OFF,
    val gridEnabled: Boolean = false,
    val countdownRemaining: Int? = null,
    val zoomRatio: Float = 1f,
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val hasFlash: Boolean = false,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val isMuted: Boolean = false,
    val recordingNanos: Long = 0L,
    val thumbnail: File? = null,
    val extension: CameraExtension = CameraExtension.NONE,
    val supportedExtensions: Set<CameraExtension> = emptySet(),
    val colorFilter: ColorFilterType = ColorFilterType.NONE,
    val faceDetectionEnabled: Boolean = false,
    val focusPoint: Offset? = null,
    val captureFlashToken: Int = 0,
    val bindRevision: Int = 0,
    val message: String? = null
) {
    val zoomChips: List<Float>
        get() = buildList {
            if (minZoom <= 0.7f) add(minZoom.coerceAtLeast(0.5f))
            add(1f)
            if (maxZoom >= 1.9f) add(2f)
            if (maxZoom >= 4.5f) add(5f)
        }.distinct()
}

fun formatRecordingTime(nanos: Long): String {
    val totalSeconds = nanos / 1_000_000_000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
