package com.arindam.camerax.data.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.arindam.camerax.domain.model.ColorFilterType
import androidx.core.graphics.createBitmap

/**
 * Shared color matrices for live CameraX [androidx.camera.core.CameraEffect] shaders
 * and still-image post-processing. GL values are column-major 4x4.
 */
object ColorFilters {

    private val identityGl = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    fun glMatrix(type: ColorFilterType): FloatArray = when (type) {
        ColorFilterType.NONE -> identityGl.copyOf()
        ColorFilterType.MONO -> floatArrayOf(
            0.299f, 0.299f, 0.299f, 0f,
            0.587f, 0.587f, 0.587f, 0f,
            0.114f, 0.114f, 0.114f, 0f,
            0f, 0f, 0f, 1f
        )
        ColorFilterType.INVERT -> floatArrayOf(
            -1f, 0f, 0f, 0f,
            0f, -1f, 0f, 0f,
            0f, 0f, -1f, 0f,
            1f, 1f, 1f, 1f
        )
        ColorFilterType.VINTAGE -> floatArrayOf(
            0.393f, 0.349f, 0.272f, 0f,
            0.769f, 0.686f, 0.534f, 0f,
            0.189f, 0.168f, 0.131f, 0f,
            0f, 0f, 0f, 1f
        )
        ColorFilterType.COOL -> floatArrayOf(
            0.85f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1.2f, 0f,
            0f, 0f, 0f, 1f
        )
        ColorFilterType.WARM -> floatArrayOf(
            1.2f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 0.85f, 0f,
            0f, 0f, 0f, 1f
        )
        ColorFilterType.VIVID -> floatArrayOf(
            1.5115f, -0.4284f, -0.0831f, 0f,
            -0.1542f, 1.2376f, -0.0831f, 0f,
            -0.1542f, -0.4284f, 1.5826f, 0f,
            0f, 0f, 0f, 1f
        )
    }

    fun androidMatrix(type: ColorFilterType): ColorMatrix = when (type) {
        ColorFilterType.NONE -> ColorMatrix()
        ColorFilterType.MONO -> ColorMatrix().apply { setSaturation(0f) }
        ColorFilterType.INVERT -> ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        ColorFilterType.VINTAGE -> ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        ColorFilterType.COOL -> ColorMatrix().apply { setScale(0.85f, 1f, 1.2f, 1f) }
        ColorFilterType.WARM -> ColorMatrix().apply { setScale(1.2f, 1f, 0.85f, 1f) }
        ColorFilterType.VIVID -> ColorMatrix().apply { setSaturation(1.7f) }
    }

    fun applyToBitmap(source: Bitmap, type: ColorFilterType): Bitmap {
        if (type == ColorFilterType.NONE) return source
        val output = createBitmap(source.width, source.height)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(androidMatrix(type))
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }
}
