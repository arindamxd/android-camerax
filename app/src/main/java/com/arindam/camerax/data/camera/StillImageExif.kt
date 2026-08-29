package com.arindam.camerax.data.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Data: EXIF orientation helpers for still JPEG processing.
 * CameraX stores sensor-oriented pixels with a rotation tag; any decode / re-encode path must
 * rotate pixels and reset [ExifInterface.TAG_ORIENTATION] to [ExifInterface.ORIENTATION_NORMAL].
 */
object StillImageExif {

    fun readOrientation(file: File): Int =
        ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

    /**
     * Decodes [file] down to [maxEdge] on the longest side, then applies EXIF orientation so
     * returned pixels match how the image should be displayed.
     */
    fun decodeSampled(file: File, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val longest = max(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longest / sample > maxEdge) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        ) ?: return null
        return applyOrientation(decoded, readOrientation(file))
    }

    fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = orientationMatrix(orientation, bitmap.width, bitmap.height) ?: return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    /** Display width and height after applying [ExifInterface.TAG_ORIENTATION]. */
    fun displaySize(file: File): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return 0 to 0
        return when (readOrientation(file)) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE -> height to width
            else -> width to height
        }
    }

    /** Writes [bitmap] as JPEG with upright pixels ([ExifInterface.ORIENTATION_NORMAL]). */
    fun writeJpeg(bitmap: Bitmap, file: File, quality: Int = 95) {
        FileOutputStream(file).use { stream ->
            require(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                "Unable to compress JPEG"
            }
        }
        val exif = ExifInterface(file.absolutePath)
        exif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL.toString()
        )
        exif.saveAttributes()
    }

    /**
     * Decodes with EXIF applied, runs [transform], and overwrites [file] with an upright JPEG.
     * Returns [file] unchanged when decode fails.
     */
    fun rewriteJpeg(
        file: File,
        quality: Int = 95,
        maxEdge: Int = DEFAULT_MAX_EDGE,
        transform: (Bitmap) -> Bitmap
    ): File {
        val oriented = decodeSampled(file, maxEdge) ?: return file
        val output = transform(oriented)
        if (output !== oriented) oriented.recycle()
        writeJpeg(output, file, quality)
        if (output !== oriented) output.recycle()
        return file
    }

    internal fun orientationMatrix(orientation: Int, width: Int, height: Int): Matrix? {
        val matrix = Matrix()
        return when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.setScale(-1f, 1f)
                matrix
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.setRotate(180f)
                matrix.postTranslate(width.toFloat(), height.toFloat())
                matrix
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setScale(1f, -1f)
                matrix
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
                matrix.postTranslate(height.toFloat(), 0f)
                matrix
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.setRotate(90f)
                matrix.postTranslate(height.toFloat(), 0f)
                matrix
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
                matrix.postTranslate(0f, width.toFloat())
                matrix
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.setRotate(270f)
                matrix.postTranslate(0f, width.toFloat())
                matrix
            }
            else -> null
        }
    }

    private const val DEFAULT_MAX_EDGE = 8192
}
