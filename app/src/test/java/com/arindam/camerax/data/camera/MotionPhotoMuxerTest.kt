package com.arindam.camerax.data.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MotionPhotoMuxerTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun extractVideo_succeedsWithMinimumMp4Payload() {
        val still = folder.newFile("still.jpg").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        }
        val video = folder.newFile("clip.mp4").apply {
            writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
        val output = folder.newFile("motion.jpg")
        MotionPhotoMuxer.mux(still, video, output)
        val extracted = folder.newFile("extracted.mp4")
        assertNotNull(MotionPhotoMuxer.extractVideo(output, extracted))
        assertEquals(8, extracted.length())
    }

    @Test
    fun mux_writesMotionPhotoXmp() {
        val still = folder.newFile("still.jpg").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        }
        val video = folder.newFile("clip.mp4").apply { writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)) }
        val output = folder.newFile("motion.jpg")
        MotionPhotoMuxer.mux(still, video, output)
        assertTrue(MotionPhotoMuxer.isMotionPhoto(output))
        val extracted = folder.newFile("extracted.mp4")
        assertNotNull(MotionPhotoMuxer.extractVideo(output, extracted))
        assertEquals(video.length(), extracted.length())
    }

    @Test
    fun isMotionPhoto_falseForPlainJpeg() {
        val still = folder.newFile("plain.jpg").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        }
        assertFalse(MotionPhotoMuxer.isMotionPhoto(still))
    }

    @Test
    fun isMotionPhoto_falseForUltraHdrStyleTrailingBytes() {
        // JPEG Ultra HDR appends a gain map after the primary EOI — must not look like motion.
        val still = folder.newFile("uhdr.jpg").apply {
            writeBytes(
                byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()) +
                    ByteArray(64) { 0x42 }
            )
        }
        assertFalse(MotionPhotoMuxer.isMotionPhoto(still))
    }

    @Test
    fun isMotionPhoto_falseForVideo() {
        assertFalse(MotionPhotoMuxer.isMotionPhoto(folder.newFile("clip.mp4")))
    }
}
