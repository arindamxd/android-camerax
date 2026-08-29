package com.arindam.camerax.data.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import com.arindam.camerax.util.commons.Constants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Data: horizontal sweep stitcher for CameraX stills. Aligns neighboring frames by
 * minimizing overlap error, then cross-fades the seam. Call via
 * [com.arindam.camerax.domain.usecase.StitchPanorama] / [com.arindam.camerax.domain.repository.MediaRepository]
 * — keep off the main thread.
 */
object PanoramaStitcher {

    fun stitch(frames: List<File>, outputDirectory: File): File {
        require(frames.isNotEmpty()) { "Panorama needs at least one frame" }
        val bitmaps = frames.mapNotNull { decode(it) }
        require(bitmaps.isNotEmpty()) { "Unable to decode panorama frames" }
        val result = if (bitmaps.size == 1) bitmaps[0] else merge(bitmaps)
        val output = File(
            outputDirectory,
            SimpleDateFormat(Constants.FILE.FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + Constants.FILE.PHOTO_EXTENSION
        )
        StillImageExif.writeJpeg(result, output, quality = 92)
        if (result !== bitmaps[0]) result.recycle()
        bitmaps.forEach { it.recycle() }
        return output
    }

    private fun decode(file: File): Bitmap? = StillImageExif.decodeSampled(file, MAX_EDGE)

    private fun merge(frames: List<Bitmap>): Bitmap {
        val height = frames.minOf { it.height }
        val scaled = frames.map { scaleToHeight(it, height) }
        val offsets = IntArray(scaled.size)
        for (index in 1 until scaled.size) {
            val overlap = estimateOverlap(scaled[index - 1], scaled[index])
            offsets[index] = offsets[index - 1] + scaled[index - 1].width - overlap
        }
        val width = offsets.last() + scaled.last().width
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        scaled.forEachIndexed { index, bitmap ->
            if (index == 0) {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            } else {
                val overlap = scaled[index - 1].width - (offsets[index] - offsets[index - 1])
                drawBlended(canvas, bitmap, offsets[index], overlap.coerceAtLeast(1), height)
            }
        }
        scaled.filter { it !in frames }.forEach { it.recycle() }
        return output
    }

    private fun scaleToHeight(source: Bitmap, height: Int): Bitmap {
        if (source.height == height) return source
        val width =
            (source.width * (height / source.height.toFloat())).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun estimateOverlap(left: Bitmap, right: Bitmap): Int {
        val minOverlap = (left.width * 0.18f).roundToInt().coerceAtLeast(24)
        val maxOverlap = (min(left.width, right.width) * 0.62f).roundToInt()
        val step = max(4, (maxOverlap - minOverlap) / 48)
        val sampleY = max(1, left.height / 80)
        var bestOverlap = (left.width * 0.3f).roundToInt()
        var bestError = Long.MAX_VALUE
        var overlap = minOverlap
        while (overlap <= maxOverlap) {
            var error = 0L
            var samples = 0
            var y = 0
            while (y < left.height) {
                var x = 0
                while (x < overlap) {
                    val leftPixel = left.getPixel(left.width - overlap + x, y)
                    val rightPixel = right.getPixel(x, y)
                    error += abs(((leftPixel shr 16) and 0xFF) - ((rightPixel shr 16) and 0xFF))
                    error += abs(((leftPixel shr 8) and 0xFF) - ((rightPixel shr 8) and 0xFF))
                    error += abs((leftPixel and 0xFF) - (rightPixel and 0xFF))
                    samples++
                    x += 8
                }
                y += sampleY
            }
            val mean = if (samples == 0) Long.MAX_VALUE else error / samples
            if (mean < bestError) {
                bestError = mean
                bestOverlap = overlap
            }
            overlap += step
        }
        return bestOverlap
    }

    private fun drawBlended(
        canvas: Canvas,
        bitmap: Bitmap,
        left: Int,
        overlap: Int,
        height: Int
    ) {
        val save = canvas.saveLayer(
            left.toFloat(),
            0f,
            (left + bitmap.width).toFloat(),
            height.toFloat(),
            null
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, left.toFloat(), 0f, paint)
        if (overlap > 1) {
            paint.shader = LinearGradient(
                left.toFloat(),
                0f,
                (left + overlap).toFloat(),
                0f,
                0x00FFFFFF,
                0xFFFFFFFF.toInt(),
                Shader.TileMode.CLAMP
            )
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            canvas.drawRect(
                left.toFloat(),
                0f,
                (left + overlap).toFloat(),
                height.toFloat(),
                paint
            )
        }
        canvas.restoreToCount(save)
    }

    private const val MAX_EDGE = 1600
}
