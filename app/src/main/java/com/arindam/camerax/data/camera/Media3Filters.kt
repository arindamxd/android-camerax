package com.arindam.camerax.data.camera

import androidx.media3.common.Effect
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import com.arindam.camerax.domain.model.ColorFilterType

/** Media3 GPU effects for Bright / Contrast. Do not combine with [ColorFilterProcessor] on the same session. */
internal fun ColorFilterType.media3Effects(): List<Effect> = when (this) {
    ColorFilterType.BRIGHT -> listOf(Brightness(0.35f))
    ColorFilterType.CONTRAST -> listOf(Contrast(0.6f))
    else -> emptyList()
}
