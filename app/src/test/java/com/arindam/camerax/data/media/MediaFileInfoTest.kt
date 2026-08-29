package com.arindam.camerax.data.media

import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import com.arindam.camerax.data.camera.StillImageExif
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class MediaFileInfoTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun mediaFileInfo_usesExifAwareDimensions() {
        val file = folder.newFile("portrait.jpg")
        Bitmap.createBitmap(36, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.CYAN)
            FileOutputStream(file).use { stream ->
                require(compress(Bitmap.CompressFormat.JPEG, 95, stream))
            }
            recycle()
        }
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val info = mediaFileInfo(file)
        assertEquals(StillImageExif.displaySize(file).first, info.width)
        assertEquals(StillImageExif.displaySize(file).second, info.height)
        assertEquals(24, info.width)
        assertEquals(36, info.height)
    }
}
