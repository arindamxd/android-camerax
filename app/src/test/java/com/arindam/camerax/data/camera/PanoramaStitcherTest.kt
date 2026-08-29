package com.arindam.camerax.data.camera

import android.graphics.Color
import android.media.ExifInterface
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
class PanoramaStitcherTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun stitch_singleOrientedFrame_writesUprightJpeg() {
        val frame = folder.newFile("frame.jpg")
        writeRawJpeg(frame, width = 40, height = 24, color = Color.CYAN, orientation = ExifInterface.ORIENTATION_ROTATE_90)
        val output = PanoramaStitcher.stitch(listOf(frame), folder.root)
        assertTrue(output.exists())
        assertEquals(ExifInterface.ORIENTATION_NORMAL, StillImageExif.readOrientation(output))
        val decoded = StillImageExif.decodeSampled(output, maxEdge = 128)
        assertNotNull(decoded)
        decoded!!.use {
            assertEquals(24, it.width)
            assertEquals(40, it.height)
        }
    }

    @Test
    fun stitch_multipleOrientedFrames_producesWideUprightOutput() {
        val left = folder.newFile("left.jpg")
        val right = folder.newFile("right.jpg")
        writeRawJpeg(left, width = 50, height = 80, color = Color.RED, orientation = ExifInterface.ORIENTATION_ROTATE_90)
        writeRawJpeg(right, width = 50, height = 80, color = Color.BLUE, orientation = ExifInterface.ORIENTATION_ROTATE_90)
        val output = PanoramaStitcher.stitch(listOf(left, right), folder.root)
        assertEquals(ExifInterface.ORIENTATION_NORMAL, StillImageExif.readOrientation(output))
        val decoded = StillImageExif.decodeSampled(output, maxEdge = 256)
        assertNotNull(decoded)
        decoded!!.use {
            assertEquals(50, it.height)
            assertTrue(it.width > it.height)
        }
    }

    private fun writeRawJpeg(
        file: File,
        width: Int,
        height: Int,
        color: Int,
        orientation: Int
    ) {
        android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
            FileOutputStream(file).use { stream ->
                require(compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, stream))
            }
            recycle()
        }
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
    }

    private inline fun android.graphics.Bitmap.use(block: (android.graphics.Bitmap) -> Unit) {
        try {
            block(this)
        } finally {
            recycle()
        }
    }
}
