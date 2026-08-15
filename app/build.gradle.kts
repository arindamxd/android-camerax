import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

if (file("google-services.json").exists()) {
    pluginManager.apply(libs.plugins.google.services.get().pluginId)
    pluginManager.apply(libs.plugins.firebase.crashlytics.get().pluginId)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}
val storeFileProvider: String? = localProperties.getProperty("storeFile")
val storePasswordProvider: String? = localProperties.getProperty("storePassword")
val keyAliasProvider: String? = localProperties.getProperty("keyAlias")
val keyPasswordProvider: String? = localProperties.getProperty("keyPassword")
val hasReleaseSigning = listOf(
    storeFileProvider,
    storePasswordProvider,
    keyAliasProvider,
    keyPasswordProvider
).all { !it.isNullOrBlank() }

android {
    namespace = "com.arindam.camerax"
    compileSdk = rootProject.extra["compileSdk"] as Int

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(storeFileProvider!!)
                storePassword = storePasswordProvider
                keyAlias = keyAliasProvider
                keyPassword = keyPasswordProvider
            }
        }
    }

    defaultConfig {
        applicationId = "com.arindam.camerax"
        minSdk = rootProject.extra["minSdkVersion"] as Int
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        versionCode = rootProject.extra["versionCodeBase"] as Int
        versionName = rootProject.extra["versionNameBase"] as String
        vectorDrawables.useSupportLibrary = true
        if (hasReleaseSigning) {
            signingConfig = signingConfigs.getByName("release")
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk.abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64") )
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            resValue("string", "app_version", "${defaultConfig.versionName} (build ${defaultConfig.versionCode}$applicationIdSuffix)")
            resValue("string", "app_name", "CameraX [D]")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            resValue("string", "app_version", "${defaultConfig.versionName} (build ${defaultConfig.versionCode}.release)")
            resValue("string", "app_name", "CameraX")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk.debugSymbolLevel = "SYMBOL_TABLE"
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // Set the source of tests to same for both Unit and Instrumented tests
    /*sourceSets {
        String sharedTestDir = 'src/test/java'
        test {
            java.srcDir sharedTestDir
        }
        androidTest {
            java.srcDir sharedTestDir
        }
    }*/

    // Necessary for Robolectric (Unit tests)
    /*testOptions {
        unitTests {
            includeAndroidResources = true
        }
    }*/
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.preference.ktx)

    // Lifecycle and LiveData
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation library
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.androidx.camera.effects)
    implementation(libs.androidx.camera.mlkit.vision)
    implementation(libs.mlkit.face.detection)

    // EXIF
    implementation(libs.androidx.exifinterface)

    // Coil
    implementation(libs.coil.compose)

    // Material Design
    implementation(libs.material)

    // Lottie
    implementation(libs.lottie)

    // Firebase SDK
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics)

    // [CONFLICT FIX] Guava
    api(libs.guava)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // [VERIFY]
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Compose Material Design 3
    implementation(libs.compose.material3)

    // Android Studio Preview support
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)

    // Optional - Included automatically by material, only add when you need
    // the icons but not the material library (e.g. when using Material3 or a
    // custom design system based on Foundation)
    implementation(libs.compose.material.icons.core)
    // Optional - Add full set of material icons
    implementation(libs.compose.material.icons.extended)
    // Optional - Add window size utils
    //implementation(libs.compose.material3.window.size)

    // Optional - Integration with activities
    implementation(libs.androidx.activity.compose)
    // Optional - Integration with ViewModels
    //implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    // Optional - Integration with LiveData
    //implementation("androidx.compose.runtime:runtime-livedata")
    // Optional - Integration with RxJava
    //implementation("androidx.compose.runtime:runtime-rxjava2")

    implementation(libs.androidx.paging.compose)

    // Unit testing
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.rules)
    testImplementation(libs.androidx.runner)
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.robolectric)

    // Instrumented testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.espresso.core)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
