package com.arindam.camerax.data.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Build
import com.arindam.camerax.domain.model.EffectMode

/**
 * Data: ColorMatrix set for Effects mode (grayscale / invert / sepia / cool / warm / vivid).
 * Used by Effects-mode [ImageAnalysis] and still capture inside [CameraSession].
 *
 * [ImageProxy.toBitmap] often returns a HARDWARE bitmap. [ColorMatrixColorFilter] is skipped on
 * those buffers, which is why Sepia / Invert can look like the unfiltered camera. Copy to
 * software ARGB first, then draw so the matrices actually apply.
 */
object ColorEffects {

    fun androidMatrix(type: EffectMode): ColorMatrix? = when (type) {
        EffectMode.NONE -> null
        EffectMode.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }
        EffectMode.INVERT -> ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        EffectMode.SEPIA -> ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        EffectMode.COOL -> ColorMatrix().apply { setScale(0.85f, 1f, 1.2f, 1f) }
        EffectMode.WARM -> ColorMatrix().apply { setScale(1.2f, 1f, 0.85f, 1f) }
        EffectMode.VIVID -> ColorMatrix().apply { setSaturation(1.7f) }
    }

    fun render(source: Bitmap, type: EffectMode): Bitmap {
        val software = source.toSoftwareArgb()
        val output = Bitmap.createBitmap(software.width, software.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            androidMatrix(type)?.let { colorFilter = ColorMatrixColorFilter(it) }
        }
        Canvas(output).drawBitmap(software, 0f, 0f, paint)
        if (software !== source) software.recycle()
        return output
    }

    fun applyToBitmap(source: Bitmap, type: EffectMode): Bitmap {
        if (type == EffectMode.NONE) return source
        return render(source, type)
    }
}

internal fun Bitmap.toSoftwareArgb(): Bitmap {
    if (config == Bitmap.Config.ARGB_8888) return this
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
        return copy(Bitmap.Config.ARGB_8888, false) ?: this
    }
    return copy(Bitmap.Config.ARGB_8888, false) ?: this
}
