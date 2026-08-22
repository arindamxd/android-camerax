package com.arindam.camerax.data.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.arindam.camerax.domain.model.EffectMode
import com.arindam.camerax.util.log.Logger

/**
 * Live color effects from the CameraX effects sample: each [ImageAnalysis] frame is decoded,
 * oriented, and recolored with a [android.graphics.ColorMatrix]. Switching chips is a field
 * write — no rebind.
 */
internal class ColorEffectAnalyzer(
    private val onFrame: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile
    var effect: EffectMode = EffectMode.NONE

    @Volatile
    private var processing = false

    override fun analyze(imageProxy: ImageProxy) {
        if (processing) {
            imageProxy.close()
            return
        }
        processing = true
        try {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val source = imageProxy.toBitmap()
            val upright = if (rotation == 0) {
                source
            } else {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            }
            val output = ColorEffects.render(upright, effect)
            if (upright !== source) upright.recycle()
            source.recycle()
            onFrame(output)
        } catch (error: Exception) {
            Logger.error(TAG, "Effect processing failed: ${error.message}")
        } finally {
            imageProxy.close()
            processing = false
        }
    }

    private companion object {
        const val TAG = "ColorEffectAnalyzer"
    }
}
