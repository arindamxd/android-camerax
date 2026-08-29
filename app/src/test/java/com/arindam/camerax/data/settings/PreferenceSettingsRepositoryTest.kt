package com.arindam.camerax.data.settings

import androidx.preference.PreferenceManager
import com.arindam.camerax.R
import com.arindam.camerax.domain.model.CaptureAspect
import com.arindam.camerax.domain.model.CaptureSettings
import com.arindam.camerax.domain.model.SlowMotionRate
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PreferenceSettingsRepositoryTest {

    @Before
    fun clearPrefs() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication())
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun loadCaptureSettings_usesDefaults() {
        val settings = repository().loadCaptureSettings()
        assertEquals(
            CaptureSettings(),
            settings
        )
        assertFalse(settings.confirmEnabled)
        assertEquals(CaptureAspect.FULL, settings.aspect)
        assertEquals(VideoQuality.FHD, settings.videoQuality)
        assertEquals(VideoHdrRange.SDR, settings.videoHdrRange)
        assertTrue(settings.videoStabilization)
        assertTrue(settings.lowLightBoost)
        assertTrue(settings.frontMirror)
        assertFalse(settings.galleryVideoAutoplay)
    }

    @Test
    fun loadCaptureSettings_readsPersistedValues() {
        val context = RuntimeEnvironment.getApplication()
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(context.getString(R.string.pref_key_capture_confirm), true)
            .putString(context.getString(R.string.pref_key_capture_aspect), CaptureAspect.RATIO_4_3.prefValue)
            .putString(context.getString(R.string.pref_key_video_quality), VideoQuality.UHD.prefValue)
            .putString(context.getString(R.string.pref_key_video_hdr), VideoHdrRange.HLG10.prefValue)
            .putBoolean(context.getString(R.string.pref_key_gallery_video_autoplay), true)
            .putString(context.getString(R.string.pref_key_slow_motion_fps), SlowMotionRate.FPS_240.prefValue)
            .apply()

        val settings = repository().loadCaptureSettings()

        assertTrue(settings.confirmEnabled)
        assertEquals(CaptureAspect.RATIO_4_3, settings.aspect)
        assertEquals(VideoQuality.UHD, settings.videoQuality)
        assertEquals(VideoHdrRange.HLG10, settings.videoHdrRange)
        assertTrue(settings.galleryVideoAutoplay)
        assertEquals(SlowMotionRate.FPS_240, settings.slowMotionRate)
    }

    private fun repository() = PreferenceSettingsRepository(RuntimeEnvironment.getApplication())
}
