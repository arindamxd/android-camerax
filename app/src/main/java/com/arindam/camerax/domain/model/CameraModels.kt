package com.arindam.camerax.domain.model

enum class CameraMode {
    PHOTO,
    VIDEO,
    EFFECTS
}

enum class CameraLens {
    BACK,
    FRONT;

    fun toggle(): CameraLens = if (this == BACK) FRONT else BACK
}

enum class FlashMode {
    OFF,
    ON,
    AUTO,
    TORCH;

    fun next(): FlashMode = when (this) {
        OFF -> ON
        ON -> AUTO
        AUTO -> TORCH
        TORCH -> OFF
    }
}

enum class TimerMode(val seconds: Int) {
    OFF(0),
    THREE(3),
    TEN(10);

    fun next(): TimerMode = when (this) {
        OFF -> THREE
        THREE -> TEN
        TEN -> OFF
    }
}

enum class CameraExtension {
    NONE,
    HDR,
    NIGHT,
    PORTRAIT,
    BEAUTY
}

enum class ColorFilterType {
    NONE,
    MONO,
    VINTAGE,
    COOL,
    WARM,
    VIVID
}

data class FocusPoint(val x: Float, val y: Float)

data class CameraBindConfig(
    val lens: CameraLens,
    val flash: FlashMode,
    val extension: CameraExtension,
    val colorFilter: ColorFilterType,
    val faceDetection: Boolean
)

data class CameraBindResult(
    val hasFlash: Boolean,
    val minZoom: Float,
    val maxZoom: Float,
    val zoomRatio: Float,
    val videoAvailable: Boolean,
    val supportedExtensions: Set<CameraExtension>
)

data class ZoomInfo(
    val ratio: Float,
    val min: Float,
    val max: Float
)

sealed interface RecordingEvent {
    data class Status(val durationNanos: Long) : RecordingEvent
    data object Paused : RecordingEvent
    data object Resumed : RecordingEvent
    data class Finalized(val success: Boolean) : RecordingEvent
}

/** Opaque preview/lifecycle handle. Data layer provides the CameraX implementation. */
interface CameraHost
