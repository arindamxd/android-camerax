package com.arindam.camerax.support

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue

/**
 * Runtime grants for instrumented tests.
 *
 * [androidx.test.rule.GrantPermissionRule] and plain `pm grant` fail on Android 16+ /
 * some OEM devices (`GRANT_RUNTIME_PERMISSIONS`). Fall back to tapping the system dialog.
 */
object InstrumentedTestPermissions {

    val cameraAndMicrophone = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private const val DIALOG_TIMEOUT_MS = 15_000L

    /**
     * Ensures camera permission is granted. Call while the target activity is visible so the
     * permission dialog can appear (for example right after [HomeActivity] launches).
     */
    fun ensureCamera() {
        val context = targetContext()
        if (isGranted(context, Manifest.permission.CAMERA)) return

        grantViaShellWithIdentity(Manifest.permission.CAMERA)
        if (isGranted(context, Manifest.permission.CAMERA)) return

        acceptSystemPermissionDialogs()

        assertTrue(
            "Camera permission was not granted. Approve the system permission dialog on the device.",
            isGranted(context, Manifest.permission.CAMERA)
        )
    }

    private fun targetContext(): Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun grantViaShellWithIdentity(permission: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiAutomation = instrumentation.uiAutomation
        val pkg = targetContext().packageName
        try {
            uiAutomation.adoptShellPermissionIdentity(
                "android.permission.GRANT_RUNTIME_PERMISSIONS"
            )
            uiAutomation.executeShellCommand("pm grant $pkg $permission").close()
        } catch (_: Exception) {
            // OEM / API may still block shell grant — dialog fallback runs next.
        } finally {
            uiAutomation.dropShellPermissionIdentity()
        }
    }

    /** Taps through one or more runtime permission dialogs (camera, microphone, …). */
    private fun acceptSystemPermissionDialogs() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val deadline = System.currentTimeMillis() + DIALOG_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline &&
            !isGranted(targetContext(), Manifest.permission.CAMERA)
        ) {
            if (clickPermissionAllow(device)) {
                device.waitForIdle(500)
                continue
            }
            device.waitForIdle(250)
        }
    }

    private fun clickPermissionAllow(device: UiDevice): Boolean {
        val selectors = listOf(
            By.text("Allow"),
            By.text("While using the app"),
            By.text("Only this time"),
            By.res("com.android.permissioncontroller", "permission_allow_foreground_only_button"),
            By.res("com.android.permissioncontroller", "permission_allow_button"),
            By.res("com.android.permissioncontroller", "permission_allow_one_time_button")
        )
        for (selector in selectors) {
            val node = device.findObject(selector) ?: continue
            node.click()
            return true
        }
        return false
    }
}
