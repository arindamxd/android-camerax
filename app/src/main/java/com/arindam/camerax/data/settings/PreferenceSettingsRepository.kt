package com.arindam.camerax.data.settings

import android.content.Context
import androidx.preference.PreferenceManager
import com.arindam.camerax.R
import com.arindam.camerax.domain.model.CaptureAspect
import com.arindam.camerax.domain.model.CaptureSettings
import com.arindam.camerax.domain.model.SlowMotionRate
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import com.arindam.camerax.domain.repository.SettingsRepository

/** SharedPreferences implementation of [SettingsRepository]. */
class PreferenceSettingsRepository(context: Context) : SettingsRepository {

    private val appContext = context.applicationContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)

    override fun loadCaptureSettings(): CaptureSettings = CaptureSettings(
        confirmEnabled = prefs.getBoolean(key(R.string.pref_key_capture_confirm), false),
        aspect = CaptureAspect.fromPref(prefs.getString(key(R.string.pref_key_capture_aspect), null)),
        videoQuality = VideoQuality.fromPref(prefs.getString(key(R.string.pref_key_video_quality), null)),
        videoHdrRange = VideoHdrRange.fromPref(prefs.getString(key(R.string.pref_key_video_hdr), null)),
        videoStabilization = prefs.getBoolean(key(R.string.pref_key_video_stabilization), true),
        slowMotionQuality = VideoQuality.fromPref(
            prefs.getString(key(R.string.pref_key_slow_motion_quality), null)
        ),
        slowMotionRate = SlowMotionRate.fromPref(
            prefs.getString(key(R.string.pref_key_slow_motion_fps), null)
        ),
        ultraHdr = prefs.getBoolean(key(R.string.pref_key_ultra_hdr), true),
        rawCapture = prefs.getBoolean(key(R.string.pref_key_raw_capture), false),
        rawFullSensor = prefs.getBoolean(key(R.string.pref_key_raw_full_sensor), false),
        flipWhileRecording = prefs.getBoolean(key(R.string.pref_key_flip_while_recording), false),
        recordMuted = prefs.getBoolean(key(R.string.pref_key_record_muted), false),
        lowLightBoost = prefs.getBoolean(key(R.string.pref_key_low_light_boost), true),
        videoFps60 = prefs.getBoolean(key(R.string.pref_key_video_fps_60), false)
    )

    private fun key(id: Int): String = appContext.getString(id)
}
