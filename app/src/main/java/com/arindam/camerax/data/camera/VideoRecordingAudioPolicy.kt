package com.arindam.camerax.data.camera

/**
 * Whether [androidx.camera.video.PendingRecording.withAudioEnabled] should be used.
 * Slow-motion high-speed sessions never record audio; without [android.Manifest.permission.RECORD_AUDIO]
 * the clip is video-only (silently muted).
 */
internal fun shouldEnableRecordingAudio(
    highSpeedSession: Boolean,
    recordAudioGranted: Boolean
): Boolean = !highSpeedSession && recordAudioGranted
