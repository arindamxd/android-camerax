package com.arindam.camerax.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.arindam.camerax.R

private val googleFonts = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val CameraFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = googleFonts)
)

/** HUD chips, timers, zoom, ISO/shutter, and other metadata. */
val CameraMono = FontFamily(
    Font(googleFont = GoogleFont("Space Mono"), fontProvider = googleFonts)
)

private val materialDefaults = Typography()

val CameraTypography = Typography(
    displayLarge = materialDefaults.displayLarge.copy(fontFamily = CameraFontFamily),
    displayMedium = materialDefaults.displayMedium.copy(fontFamily = CameraFontFamily),
    displaySmall = materialDefaults.displaySmall.copy(fontFamily = CameraFontFamily),
    headlineLarge = materialDefaults.headlineLarge.copy(fontFamily = CameraFontFamily),
    headlineMedium = materialDefaults.headlineMedium.copy(fontFamily = CameraFontFamily),
    headlineSmall = materialDefaults.headlineSmall.copy(fontFamily = CameraFontFamily),
    titleLarge = materialDefaults.titleLarge.copy(fontFamily = CameraFontFamily),
    titleMedium = materialDefaults.titleMedium.copy(fontFamily = CameraFontFamily),
    titleSmall = materialDefaults.titleSmall.copy(fontFamily = CameraFontFamily),
    bodyLarge = materialDefaults.bodyLarge.copy(fontFamily = CameraFontFamily),
    bodyMedium = materialDefaults.bodyMedium.copy(fontFamily = CameraFontFamily),
    bodySmall = materialDefaults.bodySmall.copy(fontFamily = CameraFontFamily),
    labelLarge = materialDefaults.labelLarge.copy(fontFamily = CameraFontFamily),
    labelMedium = materialDefaults.labelMedium.copy(fontFamily = CameraFontFamily),
    labelSmall = materialDefaults.labelSmall.copy(fontFamily = CameraFontFamily)
)
