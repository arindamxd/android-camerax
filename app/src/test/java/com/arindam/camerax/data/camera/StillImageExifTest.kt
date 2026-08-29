package com.arindam.camerax.data.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import com.arindam.camerax.domain.model.EffectMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class StillImageExifTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun readOrientation_returnsStoredTag() {
        val file = folder.newFile("oriented.jpg")
        writeRawJpeg(file, width = 8, height = 6, color = Color.RED, orientation = ExifInterface.ORIENTATION_ROTATE_90)
        assertEquals(ExifInterface.ORIENTATION_ROTATE_90, StillImageExif.readOrientation(file))
    }

    @Test
    fun decodeSampled_appliesExifRotation() {
        val file = folder.newFile("portrait.jpg")
        writeRawJpeg(file, width = 30, height = 20, color = Color.BLUE, orientation = ExifInterface.ORIENTATION_ROTATE_90)
        val decoded = StillImageExif.decodeSampled(file, maxEdge = 64)
        assertNotNull(decoded)
        decoded!!.use {
            assertEquals(20, it.width)
            assertEquals(30, it.height)
        }
    }

    @Test
    fun writeJpeg_setsOrientationNormal() {
        val file = folder.newFile("upright.jpg")
        Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GREEN)
            StillImageExif.writeJpeg(this, file)
            recycle()
        }
        assertEquals(ExifInterface.ORIENTATION_NORMAL, StillImageExif.readOrientation(file))
    }

    @Test
    fun rewriteJpeg_rotatesPixelsAndClearsOrientationTag() {
        val file = folder.newFile("rewrite.jpg")
        writeRawJpeg(file, width = 24, height = 16, color = Color.YELLOW, orientation = ExifInterface.ORIENTATION_ROTATE_90)
        StillImageExif.rewriteJpeg(file) { bitmap -> bitmap }
        assertEquals(ExifInterface.ORIENTATION_NORMAL, StillImageExif.readOrientation(file))
        val decoded = StillImageExif.decodeSampled(file, maxEdge = 64)
        assertNotNull(decoded)
        decoded!!.use {
            assertEquals(16, it.width)
            assertEquals(24, it.height)
        }
    }

    @Test
    fun rewriteJpeg_appliesColorEffectAfterRotation() {
        val file = folder.newFile("effect.jpg")
        writeRawJpeg(file, width = 20, height = 12, color = Color.RED, orientation = ExifInterface.ORIENTATION_ROTATE_90)
        StillImageExif.rewriteJpeg(file, maxEdge = 64) { oriented ->
            ColorEffects.applyToBitmap(oriented, EffectMode.GRAYSCALE)
        }
        val decoded = StillImageExif.decodeSampled(file, maxEdge = 64)
        assertNotNull(decoded)
        decoded!!.use { bitmap ->
            assertEquals(12, bitmap.width)
            assertEquals(20, bitmap.height)
            val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
            val red = Color.red(center)
            val green = Color.green(center)
            val blue = Color.blue(center)
            assertTrue(red in green - 2..green + 2)
            assertTrue(red in blue - 2..blue + 2)
        }
    }

    @Test
    fun orientationMatrix_rotate90_swapsDimensions() {
        val bitmap = Bitmap.createBitmap(18, 10, Bitmap.Config.ARGB_8888)
        val rotated = StillImageExif.applyOrientation(bitmap, ExifInterface.ORIENTATION_ROTATE_90)
        try {
            assertEquals(10, rotated.width)
            assertEquals(18, rotated.height)
        } finally {
            if (rotated !== bitmap) rotated.recycle()
            bitmap.recycle()
        }
    }

    @Test
    fun orientationMatrix_rotate270_swapsDimensions() {
        val bitmap = Bitmap.createBitmap(16, 10, Bitmap.Config.ARGB_8888)
        val rotated = StillImageExif.applyOrientation(bitmap, ExifInterface.ORIENTATION_ROTATE_270)
        try {
            assertEquals(10, rotated.width)
            assertEquals(16, rotated.height)
        } finally {
            if (rotated !== bitmap) rotated.recycle()
            bitmap.recycle()
        }
    }

    @Test
    fun applyOrientation_flipHorizontal_preservesDimensions() {
        val bitmap = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888)
        val flipped = StillImageExif.applyOrientation(bitmap, ExifInterface.ORIENTATION_FLIP_HORIZONTAL)
        try {
            assertEquals(12, flipped.width)
            assertEquals(8, flipped.height)
        } finally {
            if (flipped !== bitmap) flipped.recycle()
            bitmap.recycle()
        }
    }

    @Test
    fun displaySize_swapsDimensionsForRotatedTag() {
        val file = folder.newFile("size.jpg")
        writeRawJpeg(file, width = 40, height = 24, color = Color.MAGENTA, orientation = ExifInterface.ORIENTATION_ROTATE_90)
        assertEquals(24 to 40, StillImageExif.displaySize(file))
    }

    @Test
    fun displaySize_keepsDimensionsForNormalTag() {
        val file = folder.newFile("normal.jpg")
        writeRawJpeg(file, width = 40, height = 24, color = Color.MAGENTA, orientation = ExifInterface.ORIENTATION_NORMAL)
        assertEquals(40 to 24, StillImageExif.displaySize(file))
    }

    private fun writeRawJpeg(
        file: File,
        width: Int,
        height: Int,
        color: Int,
        orientation: Int
    ) {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
            FileOutputStream(file).use { stream ->
                require(compress(Bitmap.CompressFormat.JPEG, 95, stream))
            }
            recycle()
        }
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
    }

    private inline fun Bitmap.use(block: (Bitmap) -> Unit) {
        try {
            block(this)
        } finally {
            recycle()
        }
    }
}
