package com.arindam.camerax.util.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class NightModeTest {

    @Test
    fun fromPref_parsesKnownValues() {
        assertEquals(NightMode.OFF, NightMode.fromPref("off"))
        assertEquals(NightMode.ON, NightMode.fromPref("on"))
        assertEquals(NightMode.SYSTEM, NightMode.fromPref("system"))
    }

    @Test
    fun fromPref_defaultsToOnWhenMissingOrUnknown() {
        assertEquals(NightMode.ON, NightMode.fromPref(null))
        assertEquals(NightMode.ON, NightMode.fromPref("invalid"))
    }

    @Test
    fun fromPref_isCaseInsensitive() {
        assertEquals(NightMode.SYSTEM, NightMode.fromPref("System"))
        assertEquals(NightMode.OFF, NightMode.fromPref("OFF"))
    }
}
