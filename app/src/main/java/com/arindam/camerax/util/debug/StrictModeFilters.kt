package com.arindam.camerax.util.debug

/**
 * Classifies StrictMode disk stacks that originated in system_server / OEM hooks during a Binder
 * call (ColorOS UIFirst, accessibility proxy, etc.). Those cannot be fixed in app code.
 */
internal object StrictModeFilters {

    private const val APP_PACKAGE = "com.arindam.camerax."

    fun shouldSuppressDiskViolation(stack: Array<StackTraceElement>): Boolean {
        if (isBinderPropagatedDiskNoise(stack)) return true
        return isPlatformOrOemOnlyStack(stack.asSequence().map { it.className })
    }

    /**
     * Disk I/O in system_server attributed to the app thread via
     * [android.os.StrictMode.readAndHandleBinderCallViolations].
     */
    fun isBinderPropagatedDiskNoise(stack: Array<StackTraceElement>): Boolean {
        if (stack.none { it.isBinderAttributionFrame() }) return false
        val ioIndex = stack.indexOfFirst { isIoFrame(it.className) }
        if (ioIndex < 0) return true
        return stack.take(ioIndex).none { isAppPackage(it.className) }
    }

    fun isPlatformOrOemOnlyStack(classNames: Sequence<String>): Boolean =
        classNames.none { !isPlatformOrOemClass(it) }

    fun isAppPackage(className: String): Boolean = className.startsWith(APP_PACKAGE)

    private fun StackTraceElement.isBinderAttributionFrame(): Boolean =
        methodName.contains("readAndHandleBinderCallViolations") ||
            className.contains("readAndHandleBinderCallViolations")

    private fun isIoFrame(className: String): Boolean =
        className.startsWith("libcore.io.") || className.startsWith("java.io.")

    fun isPlatformOrOemClass(className: String): Boolean =
        className.startsWith("android.") ||
            className.startsWith("com.android.") ||
            className.startsWith("java.") ||
            className.startsWith("javax.") ||
            className.startsWith("kotlin.") ||
            className.startsWith("kotlinx.") ||
            className.startsWith("libcore.") ||
            className.startsWith("dalvik.") ||
            className.startsWith("com.oplus.") ||
            className.startsWith("com.coloros.") ||
            className.startsWith("com.heytap.") ||
            className.startsWith("com.oneplus.") ||
            className.startsWith("com.miui.") ||
            className.startsWith("miui.") ||
            className.startsWith("com.samsung.") ||
            className.startsWith("com.huawei.") ||
            className.startsWith("com.vivo.") ||
            className.startsWith("com.iqoo.")
}
