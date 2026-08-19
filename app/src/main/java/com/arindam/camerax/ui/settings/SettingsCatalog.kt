package com.arindam.camerax.ui.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.HdrAuto
import androidx.compose.material.icons.outlined.HdrOn
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RawOn
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.SlowMotionOptions
import com.arindam.camerax.domain.model.CaptureAspect
import com.arindam.camerax.domain.model.SlowMotionRate
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality

/**
 * Single place to add Settings rows. Append an item to a [SettingsSection] (or add a section)
 * and it appears on the screen — no XML preference layouts.
 */
fun settingsSections(
    versionLabel: String,
    videoQualities: List<VideoQuality> = VideoQuality.entries,
    videoHdrRanges: List<VideoHdrRange> = listOf(VideoHdrRange.SDR),
    videoStabilizationAvailable: Boolean = true,
    slowMotion: SlowMotionOptions = SlowMotionOptions(),
    ultraHdrAvailable: Boolean = false,
    rawCaptureAvailable: Boolean = false,
    fullSensorRawAvailable: Boolean = false,
    lowLightBoostAvailable: Boolean = false,
    videoFps60Available: Boolean = false
): List<SettingsSection> = listOf(
    SettingsSection(
        titleRes = R.string.pref_title_theme,
        items = listOf(
            SettingsRow.Choice(
                keyRes = R.string.pref_key_theme,
                titleRes = R.string.pref_title_theme_mode,
                subtitleRes = R.string.pref_subtitle_theme_mode,
                icon = Icons.Outlined.DarkMode,
                defaultValue = "on",
                appliesAppTheme = true,
                options = listOf(
                    SettingsChoice(R.string.pref_theme_light, "off"),
                    SettingsChoice(R.string.pref_theme_dark, "on"),
                    SettingsChoice(R.string.pref_theme_system, "system")
                )
            )
        )
    ),
    SettingsSection(
        titleRes = R.string.pref_title_general,
        items = listOf(
            SettingsRow.Toggle(
                keyRes = R.string.pref_key_capture_confirm,
                titleRes = R.string.pref_title_capture_confirm,
                subtitleRes = R.string.pref_subtitle_capture_confirm,
                icon = Icons.Outlined.PhotoCamera,
                defaultOn = false
            ),
            SettingsRow.Toggle(
                keyRes = R.string.pref_key_low_light_boost,
                titleRes = R.string.pref_title_low_light_boost,
                subtitleRes = if (lowLightBoostAvailable) {
                    R.string.pref_subtitle_low_light_boost
                } else {
                    R.string.pref_subtitle_low_light_boost_unsupported
                },
                icon = Icons.Outlined.NightsStay,
                defaultOn = true,
                enabled = lowLightBoostAvailable
            )
        )
    ),
    SettingsSection(
        titleRes = R.string.pref_title_photo,
        items = listOf(
            SettingsRow.Choice(
                keyRes = R.string.pref_key_capture_aspect,
                titleRes = R.string.pref_title_capture_aspect,
                subtitleRes = R.string.pref_subtitle_capture_aspect,
                icon = Icons.Outlined.Crop,
                defaultValue = CaptureAspect.FULL.prefValue,
                options = listOf(
                    SettingsChoice(R.string.pref_aspect_4_3, CaptureAspect.RATIO_4_3.prefValue),
                    SettingsChoice(R.string.pref_aspect_16_9, CaptureAspect.RATIO_16_9.prefValue),
                    SettingsChoice(R.string.pref_aspect_full, CaptureAspect.FULL.prefValue)
                )
            ),
            SettingsRow.Toggle(
                keyRes = R.string.pref_key_ultra_hdr,
                titleRes = R.string.pref_title_ultra_hdr,
                subtitleRes = if (ultraHdrAvailable) {
                    R.string.pref_subtitle_ultra_hdr
                } else {
                    R.string.pref_subtitle_ultra_hdr_unsupported
                },
                icon = Icons.Outlined.HdrOn,
                defaultOn = true,
                enabled = ultraHdrAvailable
            ),
            SettingsRow.Toggle(
                keyRes = R.string.pref_key_raw_capture,
                titleRes = R.string.pref_title_raw_capture,
                subtitleRes = if (rawCaptureAvailable) {
                    R.string.pref_subtitle_raw_capture
                } else {
                    R.string.pref_subtitle_raw_unsupported
                },
                icon = Icons.Outlined.RawOn,
                defaultOn = false,
                enabled = rawCaptureAvailable
            ),
            SettingsRow.Toggle(
                keyRes = R.string.pref_key_raw_full_sensor,
                titleRes = R.string.pref_title_raw_full_sensor,
                subtitleRes = if (fullSensorRawAvailable) {
                    R.string.pref_subtitle_raw_full_sensor
                } else {
                    R.string.pref_subtitle_raw_full_sensor_unsupported
                },
                icon = Icons.Outlined.CropFree,
                defaultOn = false,
                enabled = fullSensorRawAvailable
            )
        )
    ),
    SettingsSection(
        titleRes = R.string.pref_title_video,
        items = listOf(
            SettingsRow.Choice(
                keyRes = R.string.pref_key_video_quality,
                titleRes = R.string.pref_title_video_quality,
                subtitleRes = R.string.pref_subtitle_video_quality,
                icon = Icons.Outlined.HighQuality,
                defaultValue = VideoQuality.FHD.prefValue,
                options = videoQualities.map { quality ->
                    SettingsChoice(quality.labelRes, quality.prefValue)
                }
            ),
            SettingsRow.Choice(
                keyRes = R.string.pref_key_video_hdr,
                titleRes = R.string.pref_title_video_hdr,
                subtitleRes = if (videoHdrRanges.hasHdr) {
                    R.string.pref_subtitle_video_hdr
                } else {
                    R.string.pref_subtitle_video_hdr_unsupported
                },
                icon = Icons.Outlined.HdrAuto,
                defaultValue = VideoHdrRange.SDR.prefValue,
                options = if (videoHdrRanges.hasHdr) {
                    videoHdrRanges.map { range ->
                        SettingsChoice(range.labelRes, range.prefValue)
                    }
                } else {
                    emptyList()
                },
                enabled = videoHdrRanges.hasHdr
            ),
            SettingsRow.Toggle(
                keyRes = R.string.pref_key_video_stabilization,
                titleRes = R.string.pref_title_video_stabilization,
                subtitleRes = if (videoStabilizationAvailable) {
                    R.string.pref_subtitle_video_stabilization
                } else {
                    R.string.pref_subtitle_video_stabilization_unsupported
                },
                icon = Icons.Outlined.Videocam,
                defaultOn = true,
                enabled = videoStabilizationAvailable
            ),
            SettingsRow.Toggle(
                keyRes = R.string.pref_key_flip_while_recording,
                titleRes = R.string.pref_title_flip_while_recording,
                subtitleRes = R.string.pref_subtitle_flip_while_recording,
                icon = Icons.Outlined.Cameraswitch,
                defaultOn = false
            ),
            SettingsRow.Toggle(
                keyRes = R.string.pref_key_video_fps_60,
                titleRes = R.string.pref_title_video_fps_60,
                subtitleRes = if (videoFps60Available) {
                    R.string.pref_subtitle_video_fps_60
                } else {
                    R.string.pref_subtitle_video_fps_60_unsupported
                },
                icon = Icons.Outlined.Speed,
                defaultOn = false,
                enabled = videoFps60Available
            )
        )
    ),
    SettingsSection(
        titleRes = R.string.pref_title_slow_motion,
        items = listOf(
            SettingsRow.Choice(
                keyRes = R.string.pref_key_slow_motion_quality,
                titleRes = R.string.pref_title_slow_motion_quality,
                subtitleRes = if (slowMotion.available) {
                    R.string.pref_subtitle_slow_motion_quality
                } else {
                    R.string.slow_motion_unsupported
                },
                icon = Icons.Outlined.SlowMotionVideo,
                defaultValue = slowMotion.qualities.preferredSlowMotionDefault().prefValue,
                options = (if (slowMotion.available) {
                    slowMotion.qualities
                } else {
                    VideoQuality.entries
                }).map { quality ->
                    SettingsChoice(quality.labelRes, quality.prefValue)
                },
                enabled = slowMotion.available
            ),
            SettingsRow.Choice(
                keyRes = R.string.pref_key_slow_motion_fps,
                titleRes = R.string.pref_title_slow_motion_fps,
                subtitleRes = if (slowMotion.available) {
                    R.string.pref_subtitle_slow_motion_fps
                } else {
                    R.string.slow_motion_unsupported
                },
                icon = Icons.Outlined.Speed,
                defaultValue = SlowMotionRate.AUTO.prefValue,
                options = (if (slowMotion.available) {
                    SlowMotionRate.forFrameRates(slowMotion.frameRates)
                } else {
                    SlowMotionRate.entries
                }).map { rate ->
                    SettingsChoice(rate.labelRes, rate.prefValue)
                },
                enabled = slowMotion.available
            )
        )
    ),
    SettingsSection(
        titleRes = R.string.pref_title_links,
        items = listOf(
            SettingsRow.Link(
                titleRes = R.string.pref_title_playstore,
                subtitleRes = R.string.pref_subtitle_playstore,
                icon = Icons.Outlined.Shop,
                urlRes = R.string.link_playstore
            ),
            SettingsRow.Link(
                titleRes = R.string.pref_title_github,
                subtitleRes = R.string.pref_subtitle_github,
                icon = Icons.Outlined.Code,
                urlRes = R.string.link_repository
            ),
            SettingsRow.Link(
                titleRes = R.string.pref_title_privacy,
                subtitleRes = R.string.pref_subtitle_privacy,
                icon = Icons.Outlined.Policy,
                urlRes = R.string.link_privacy
            )
        )
    ),
    SettingsSection(
        titleRes = R.string.pref_title_about,
        items = listOf(
            SettingsRow.Link(
                titleRes = R.string.pref_title_developer,
                subtitleRes = R.string.pref_subtitle_developer,
                icon = Icons.Outlined.Person,
                urlRes = R.string.link_developer
            ),
            SettingsRow.Info(
                titleRes = R.string.pref_title_version,
                value = versionLabel,
                icon = Icons.Outlined.Info
            )
        )
    )
)

data class SettingsSection(
    @StringRes val titleRes: Int,
    val items: List<SettingsRow>
)

data class SettingsChoice(
    @StringRes val labelRes: Int,
    val value: String
)

sealed interface SettingsRow {
    val titleRes: Int
    val icon: ImageVector

    data class Toggle(
        @StringRes val keyRes: Int,
        @StringRes override val titleRes: Int,
        @StringRes val subtitleRes: Int,
        override val icon: ImageVector,
        val defaultOn: Boolean,
        val enabled: Boolean = true
    ) : SettingsRow

    data class Choice(
        @StringRes val keyRes: Int,
        @StringRes override val titleRes: Int,
        @StringRes val subtitleRes: Int,
        override val icon: ImageVector,
        val defaultValue: String,
        val options: List<SettingsChoice>,
        val appliesAppTheme: Boolean = false,
        val enabled: Boolean = true
    ) : SettingsRow

    data class Link(
        @StringRes override val titleRes: Int,
        @StringRes val subtitleRes: Int,
        override val icon: ImageVector,
        @StringRes val urlRes: Int
    ) : SettingsRow

    data class Info(
        @StringRes override val titleRes: Int,
        val value: String,
        override val icon: ImageVector
    ) : SettingsRow
}

private val VideoQuality.labelRes: Int
    get() = when (this) {
        VideoQuality.SD -> R.string.pref_video_sd
        VideoQuality.HD -> R.string.pref_video_hd
        VideoQuality.FHD -> R.string.pref_video_fhd
        VideoQuality.UHD -> R.string.pref_video_uhd
    }

private val VideoHdrRange.labelRes: Int
    get() = when (this) {
        VideoHdrRange.SDR -> R.string.pref_video_hdr_sdr
        VideoHdrRange.HLG10 -> R.string.pref_video_hdr_hlg10
        VideoHdrRange.HDR10 -> R.string.pref_video_hdr_hdr10
        VideoHdrRange.HDR10_PLUS -> R.string.pref_video_hdr_hdr10_plus
        VideoHdrRange.DOLBY_VISION -> R.string.pref_video_hdr_dolby
    }

private val List<VideoHdrRange>.hasHdr: Boolean
    get() = any { it != VideoHdrRange.SDR }

private fun List<VideoQuality>.preferredSlowMotionDefault(): VideoQuality =
    firstOrNull { it == VideoQuality.FHD }
        ?: firstOrNull { it == VideoQuality.HD }
        ?: firstOrNull()
        ?: VideoQuality.HD

private val SlowMotionRate.labelRes: Int
    get() = when (this) {
        SlowMotionRate.AUTO -> R.string.pref_slow_motion_fps_auto
        SlowMotionRate.FPS_120 -> R.string.pref_slow_motion_fps_120
        SlowMotionRate.FPS_240 -> R.string.pref_slow_motion_fps_240
        SlowMotionRate.FPS_480 -> R.string.pref_slow_motion_fps_480
        SlowMotionRate.FPS_960 -> R.string.pref_slow_motion_fps_960
    }
