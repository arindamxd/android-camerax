package com.arindam.camerax.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MediaFilesTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun listedCaptures_keepsWhitelistAndSortsNewestFirst() {
        val older = folder.newFile("older.jpg").apply { setLastModified(1_000L) }
        val newer = folder.newFile("newer.mp4").apply { setLastModified(2_000L) }
        folder.newFile("notes.txt")
        val listed = listedCaptures(folder.root)
        assertEquals(listOf(newer, older), listed)
    }

    @Test
    fun extensionHelpers() {
        assertTrue(File("clip.MP4").isVideoCapture())
        assertTrue(File("raw.dng").isRawCapture())
        assertTrue(File("still.JPEG").isStillCapture())
        assertFalse(File("notes.txt").isCaptureFile())
        assertTrue(File("shot.heic").isCaptureFile())
    }
}
