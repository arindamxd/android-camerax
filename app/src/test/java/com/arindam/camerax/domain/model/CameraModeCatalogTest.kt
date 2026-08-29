package com.arindam.camerax.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraModeCatalogTest {

    @Test
    fun visibleModes_hidesSlowMotionAndDualWhenUnsupported() {
        val modes = CameraModeCatalog.visibleModes(
            slowMotionSupported = false,
            concurrentSupported = false
        )
        assertFalse(modes.contains(CameraMode.SLOW_MOTION))
        assertFalse(modes.contains(CameraMode.DUAL))
        assertTrue(modes.contains(CameraMode.PHOTO))
        assertTrue(modes.contains(CameraMode.VIDEO))
        assertTrue(modes.contains(CameraMode.OTHERS))
    }

    @Test
    fun visibleModes_includesSlowMotionAndDualWhenSupported() {
        val modes = CameraModeCatalog.visibleModes(
            slowMotionSupported = true,
            concurrentSupported = true
        )
        assertTrue(modes.contains(CameraMode.SLOW_MOTION))
        assertTrue(modes.contains(CameraMode.DUAL))
    }

    @Test
    fun resolve_fallsBackFromSlowMotionToVideo() {
        val resolved = CameraModeCatalog.resolve(
            mode = CameraMode.SLOW_MOTION,
            slowMotionSupported = false,
            concurrentSupported = false
        )
        assertEquals(CameraMode.VIDEO, resolved)
    }

    @Test
    fun dualProfile_recordsCompositionVideo() {
        val profile = CameraModeCatalog.profile(CameraMode.DUAL)
        assertEquals(CaptureAction.VIDEO, profile.captureAction)
        assertTrue(profile.showsVideoStatus)
        assertTrue(profile.bindConcurrent)
        assertTrue(profile.showsPip)
    }

    @Test
    fun resolve_fallsBackFromDualToPhoto() {
        val resolved = CameraModeCatalog.resolve(
            mode = CameraMode.DUAL,
            slowMotionSupported = false,
            concurrentSupported = false
        )
        assertEquals(CameraMode.PHOTO, resolved)
    }

    @Test
    fun profile_existsForEveryMode() {
        CameraMode.entries.forEach { mode ->
            assertEquals(mode, CameraModeCatalog.profile(mode).mode)
        }
    }

    @Test
    fun captureAspect_fromPrefDefaultsToFull() {
        assertEquals(CaptureAspect.FULL, CaptureAspect.fromPref(null))
        assertEquals(CaptureAspect.RATIO_4_3, CaptureAspect.fromPref("4_3"))
    }

    @Test
    fun videoQuality_fromPrefDefaultsToFhd() {
        assertEquals(VideoQuality.FHD, VideoQuality.fromPref("unknown"))
        assertEquals(VideoQuality.UHD, VideoQuality.fromPref("uhd"))
    }

    @Test
    fun slowMotionRate_forFrameRatesIncludesAuto() {
        val rates = SlowMotionRate.forFrameRates(listOf(120, 240))
        assertEquals(SlowMotionRate.AUTO, rates.first())
        assertTrue(rates.contains(SlowMotionRate.FPS_120))
        assertTrue(rates.contains(SlowMotionRate.FPS_240))
        assertFalse(rates.contains(SlowMotionRate.FPS_960))
    }

    @Test
    fun cameraLens_toggleFlipsFrontAndBack() {
        assertEquals(CameraLens.FRONT, CameraLens.BACK.toggle())
        assertEquals(CameraLens.BACK, CameraLens.FRONT.toggle())
    }

    @Test
    fun flashMode_nextCycles() {
        assertEquals(FlashMode.ON, FlashMode.OFF.next())
        assertEquals(FlashMode.OFF, FlashMode.TORCH.next())
    }

    @Test
    fun effectsProfile_enablesEffectChipsAndStillCapture() {
        val profile = CameraModeCatalog.profile(CameraMode.EFFECTS)
        assertEquals(CaptureAction.STILL, profile.captureAction)
        assertTrue(profile.showsEffects)
        assertFalse(profile.allowsExtensions)
        assertTrue(profile.rebindOnEnter)
    }

    @Test
    fun panoramaProfile_disablesZoomAndFlash() {
        val profile = CameraModeCatalog.profile(CameraMode.PANORAMA)
        assertEquals(CaptureAction.PANORAMA, profile.captureAction)
        assertFalse(profile.showsFlash)
        assertFalse(profile.showsZoom)
        assertFalse(profile.allowsMotionPhoto)
    }

    @Test
    fun videoHdrRange_fromPrefDefaultsToSdr() {
        assertEquals(VideoHdrRange.SDR, VideoHdrRange.fromPref(null))
        assertEquals(VideoHdrRange.HLG10, VideoHdrRange.fromPref("hlg10"))
    }

    @Test
    fun slowMotionRate_fromPrefDefaultsToAuto() {
        assertEquals(SlowMotionRate.AUTO, SlowMotionRate.fromPref(null))
        assertEquals(SlowMotionRate.FPS_120, SlowMotionRate.fromPref("120"))
    }
}
