package com.arindam.camerax.util.debug

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrictModeFiltersTest {

    @Test
    fun oplusUiFirstStack_isPlatformNoise() {
        assertTrue(
            StrictModeFilters.shouldSuppressDiskViolation(
                stack(
                    "android.os.StrictMode\$AndroidBlockGuardPolicy" to "onReadFromDisk",
                    "libcore.io.BlockGuardOs" to "access",
                    "java.io.File" to "exists",
                    "com.oplus.uifirst.Utils" to "writeProcNode",
                    "com.android.server.wm.TaskFragment" to "startPausing",
                    "android.os.Binder" to "execTransact"
                )
            )
        )
    }

    @Test
    fun finishActivityBinderStack_isPlatformNoise() {
        assertTrue(
            StrictModeFilters.shouldSuppressDiskViolation(
                stack(
                    "android.os.StrictMode\$AndroidBlockGuardPolicy" to "onReadFromDisk",
                    "libcore.io.BlockGuardOs" to "access",
                    "java.io.File" to "exists",
                    "com.oplus.uifirst.Utils" to "writeProcNode",
                    "com.android.server.wm.TransitionController" to "requestStartTransition",
                    "android.os.Binder" to "execTransact",
                    "android.os.StrictMode" to "readAndHandleBinderCallViolations",
                    "android.app.ActivityClient" to "finishActivity",
                    "android.app.Activity" to "finish",
                    "com.arindam.camerax.ui.home.HomeActivity" to "onBackPressed"
                )
            )
        )
    }

    @Test
    fun inputManagerBinderStack_isPlatformNoise() {
        assertTrue(
            StrictModeFilters.shouldSuppressDiskViolation(
                stack(
                    "android.os.StrictMode\$AndroidBlockGuardPolicy" to "onReadFromDisk",
                    "libcore.io.BlockGuardOs" to "access",
                    "java.io.File" to "exists",
                    "com.android.server.hans.binderproxy.OplusBinderProxyManager" to "isAlivePid",
                    "android.os.Binder" to "execTransact",
                    "android.os.StrictMode" to "readAndHandleBinderCallViolations",
                    "android.hardware.input.InputManagerGlobal" to "getInputDevice",
                    "android.view.ViewRootImpl" to "isTypingKey"
                )
            )
        )
    }

    @Test
    fun appDiskStack_isNotNoise() {
        assertFalse(
            StrictModeFilters.shouldSuppressDiskViolation(
                stack(
                    "android.os.StrictMode\$AndroidBlockGuardPolicy" to "onReadFromDisk",
                    "libcore.io.BlockGuardOs" to "access",
                    "java.io.File" to "listFiles",
                    "com.arindam.camerax.data.media.FileMediaRepository" to "list",
                    "com.arindam.camerax.ui.home.gallery.GalleryViewModel" to "load"
                )
            )
        )
    }

    @Test
    fun androidxLibraryStack_isNotNoise() {
        assertFalse(
            StrictModeFilters.shouldSuppressDiskViolation(
                stack(
                    "android.os.StrictMode\$AndroidBlockGuardPolicy" to "onReadFromDisk",
                    "java.io.FileInputStream" to "<init>",
                    "androidx.preference.PreferenceManager" to "getDefaultSharedPreferences",
                    "com.arindam.camerax.CameraX" to "handleDayNightTheme"
                )
            )
        )
    }

    private fun stack(vararg frames: Pair<String, String>): Array<StackTraceElement> =
        frames.map { (className, methodName) ->
            StackTraceElement(className, methodName, "File.kt", 1)
        }.toTypedArray()
}
