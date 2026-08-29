package com.arindam.camerax.ui.settings

import com.arindam.camerax.R
import com.arindam.camerax.domain.model.SlowMotionOptions
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCatalogTest {

    @Test
    fun settingsSections_includesThemeAndCaptureConfirm() {
        val sections = settingsSections(versionLabel = "1.7.0")
        assertTrue(sections.any { it.titleRes == R.string.pref_title_theme })
        val toggles = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Toggle>()
        assertTrue(toggles.any { it.keyRes == R.string.pref_key_capture_confirm })
    }

    @Test
    fun settingsSections_disablesSlowMotionWhenUnavailable() {
        val sections = settingsSections(
            versionLabel = "1.7.0",
            slowMotion = SlowMotionOptions()
        )
        val choices = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Choice>()
        val slowMotion = choices.filter {
            it.keyRes == R.string.pref_key_slow_motion_quality ||
                it.keyRes == R.string.pref_key_slow_motion_fps
        }
        assertTrue(slowMotion.isNotEmpty())
        assertTrue(slowMotion.all { !it.enabled })
    }

    @Test
    fun settingsSections_promptsForMicrophoneOnRecordMutedWhenUnavailable() {
        val sections = settingsSections(
            versionLabel = "1.7.0",
            microphonePermissionGranted = false
        )
        val recordMuted = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Toggle>()
            .first { it.keyRes == R.string.pref_key_record_muted }
        assertTrue(recordMuted.enabled)
        assertTrue(recordMuted.requestsMicrophoneWhenUnavailable)
    }

    @Test
    fun settingsSections_usesUnavailableSubtitleWhenMicrophoneDenied() {
        val sections = settingsSections(
            versionLabel = "1.7.0",
            microphonePermissionGranted = false
        )
        val recordMuted = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Toggle>()
            .first { it.keyRes == R.string.pref_key_record_muted }
        assertEquals(R.string.pref_subtitle_record_muted_unavailable, recordMuted.subtitleRes)
    }

    @Test
    fun settingsSections_usesDefaultSubtitleWhenMicrophoneGranted() {
        val sections = settingsSections(
            versionLabel = "1.7.0",
            microphonePermissionGranted = true
        )
        val recordMuted = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Toggle>()
            .first { it.keyRes == R.string.pref_key_record_muted }
        assertEquals(R.string.pref_subtitle_record_muted, recordMuted.subtitleRes)
        assertTrue(recordMuted.enabled)
        assertTrue(recordMuted.requestsMicrophoneWhenUnavailable)
    }

    @Test
    fun settingsSections_recordMutedStaysEnabledWhenMicrophoneGranted() {
        val sections = settingsSections(
            versionLabel = "1.7.0",
            microphonePermissionGranted = true
        )
        val recordMuted = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Toggle>()
            .first { it.keyRes == R.string.pref_key_record_muted }
        assertTrue(recordMuted.enabled)
    }

    @Test
    fun settingsSections_keepsHdrRowWhenDeviceListsIt() {
        val sections = settingsSections(
            versionLabel = "1.7.0",
            videoQualities = listOf(VideoQuality.HD, VideoQuality.FHD),
            videoHdrRanges = listOf(VideoHdrRange.SDR, VideoHdrRange.HLG10)
        )
        val hdr = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Choice>()
            .first { it.keyRes == R.string.pref_key_video_hdr }
        assertTrue(hdr.enabled)
        assertTrue(hdr.options.any { it.value == VideoHdrRange.HLG10.prefValue })
    }

    @Test
    fun settingsSections_disablesUltraHdrWhenUnavailable() {
        val sections = settingsSections(versionLabel = "1.7.0", ultraHdrAvailable = false)
        val ultraHdr = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Toggle>()
            .first { it.keyRes == R.string.pref_key_ultra_hdr }
        assertFalse(ultraHdr.enabled)
        assertEquals(R.string.pref_subtitle_ultra_hdr_unsupported, ultraHdr.subtitleRes)
    }

    @Test
    fun settingsSections_disablesRawRowsWhenUnavailable() {
        val sections = settingsSections(
            versionLabel = "1.7.0",
            rawCaptureAvailable = false,
            fullSensorRawAvailable = false
        )
        val toggles = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Toggle>()
        val raw = toggles.first { it.keyRes == R.string.pref_key_raw_capture }
        val fullSensor = toggles.first { it.keyRes == R.string.pref_key_raw_full_sensor }
        assertFalse(raw.enabled)
        assertFalse(fullSensor.enabled)
        assertEquals(R.string.pref_subtitle_raw_unsupported, raw.subtitleRes)
        assertEquals(R.string.pref_subtitle_raw_full_sensor_unsupported, fullSensor.subtitleRes)
    }

    @Test
    fun settingsSections_disablesFps60WhenUnavailable() {
        val sections = settingsSections(versionLabel = "1.7.0", videoFps60Available = false)
        val fps60 = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Toggle>()
            .first { it.keyRes == R.string.pref_key_video_fps_60 }
        assertFalse(fps60.enabled)
        assertEquals(R.string.pref_subtitle_video_fps_60_unsupported, fps60.subtitleRes)
    }

    @Test
    fun settingsSections_disablesStabilizationWhenUnavailable() {
        val sections = settingsSections(versionLabel = "1.7.0", videoStabilizationAvailable = false)
        val stabilization = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Toggle>()
            .first { it.keyRes == R.string.pref_key_video_stabilization }
        assertFalse(stabilization.enabled)
        assertEquals(R.string.pref_subtitle_video_stabilization_unsupported, stabilization.subtitleRes)
    }

    @Test
    fun settingsSections_includesVersionInfo() {
        val sections = settingsSections(versionLabel = "2.0.0")
        val version = sections.flatMap { it.items }.filterIsInstance<SettingsRow.Info>()
            .first { it.titleRes == R.string.pref_title_version }
        assertEquals("2.0.0", version.value)
    }
}
