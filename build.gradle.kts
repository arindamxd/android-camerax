// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

// App version
ext["versionMajor"] = 1
ext["versionMinor"] = 7
ext["versionPatch"] = 0

// X.Y.Z; X = Major, Y = minor, Z = Patch / XYYZZM; M = Module (tv, mobile)
ext["versionNameBase"] = "${ext["versionMajor"]}.${ext["versionMinor"]}.${ext["versionPatch"]}"
ext["versionCodeBase"] = (ext["versionMajor"] as Int) * 10000 + (ext["versionMinor"] as Int) * 100 + (ext["versionPatch"] as Int)

// SDK and tools
ext["compileSdk"] = 37
ext["minSdkVersion"] = 23
ext["minTvSdkVersion"] = 23
ext["targetSdkVersion"] = 37

tasks.register<Delete>("clean") {
    description = "Deletes the root and module build directories."
    delete(
        rootProject.layout.buildDirectory,
        rootProject.subprojects.map { it.layout.buildDirectory }
    )
}

tasks.register("printNativeDebugSymbols") {
    description = "Prints the path of the release native debug symbols zip after bundling."
    dependsOn(":app:bundleRelease")
    doLast {
        val symbolsDir = project(":app").layout.buildDirectory
            .dir("outputs/native-debug-symbols/release")
            .get()
            .asFile
        val symbolsZip = symbolsDir.resolve("native-debug-symbols.zip")
        if (symbolsZip.exists()) {
            println("Native debug symbols generated at: ${symbolsZip.absolutePath}")
        } else {
            println("No native-debug-symbols.zip found in $symbolsDir")
        }
    }
}
