// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.dagger.hilt.android) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

// App version
ext["versionMajor"] = 1
ext["versionMinor"] = 6
ext["versionPatch"] = 8

// X.Y.Z; X = Major, Y = minor, Z = Patch / XYYZZM; M = Module (tv, mobile)
ext["versionNameBase"] = "${ext["versionMajor"]}.${ext["versionMinor"]}.${ext["versionPatch"]}"
ext["versionCodeBase"] = (ext["versionMajor"] as Int) * 10000 + (ext["versionMinor"] as Int) * 100 + (ext["versionPatch"] as Int)

// SDK and tools
ext["compileSdk"] = 36
ext["minSdkVersion"] = 21
ext["minTvSdkVersion"] = 21 // TV was introduced with Lollipop, min SDK should be 21.
ext["targetSdkVersion"] = 36

tasks.register<Delete>("clean") {
    delete(
        rootProject.layout.buildDirectory,
        rootProject.subprojects.map { it.layout.buildDirectory }
    )
}

tasks.register("printNativeDebugSymbols") {
    dependsOn("bundleRelease") // ensures bundleRelease runs first
    doLast {
        val symbolsDir = layout.buildDirectory.dir("outputs/native-debug-symbols/release").get().asFile
        val symbolsZip = symbolsDir.resolve("native-debug-symbols.zip")
        if (symbolsZip.exists()) {
            println("Native debug symbols generated at: ${symbolsZip.absolutePath}")
        } else {
            println("No native-debug-symbols.zip found in $symbolsDir")
        }
    }
}
