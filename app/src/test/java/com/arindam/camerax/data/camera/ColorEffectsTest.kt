package com.arindam.camerax.data.camera

import android.graphics.Bitmap
import android.graphics.Color
import com.arindam.camerax.domain.model.EffectMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorEffectsTest {

    @Test
    fun androidMatrix_noneIsNull() {
        assertNull(ColorEffects.androidMatrix(EffectMode.NONE))
    }

    @Test
    fun androidMatrix_effectModesReturnMatrix() {
        EffectMode.entries.filter { it != EffectMode.NONE }.forEach { mode ->
            assertNotNull(ColorEffects.androidMatrix(mode))
        }
    }

    @Test
    fun applyToBitmap_none_returnsSameBitmap() {
        val source = redPixel()
        assertSame(source, ColorEffects.applyToBitmap(source, EffectMode.NONE))
    }

    @Test
    fun render_returnsSoftwareArgbBitmap() {
        val source = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val result = ColorEffects.render(source, EffectMode.GRAYSCALE)
        assertEquals(Bitmap.Config.ARGB_8888, result.config)
        assertEquals(4, result.width)
        assertEquals(4, result.height)
        assertNotSame(source, result)
    }

    @Test
    fun render_preservesDimensionsForAllEffects() {
        val source = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888)
        EffectMode.entries.forEach { mode ->
            val result = ColorEffects.render(source, mode)
            assertEquals(8, result.width)
            assertEquals(6, result.height)
        }
    }

    @Test
    fun applyToBitmap_effectModesReturnNewBitmap() {
        val source = redPixel()
        EffectMode.entries.filter { it != EffectMode.NONE }.forEach { mode ->
            val result = ColorEffects.applyToBitmap(source, mode)
            assertNotSame(source, result)
            assertEquals(Bitmap.Config.ARGB_8888, result.config)
        }
    }

    private fun redPixel(): Bitmap =
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, Color.RED)
        }
}
