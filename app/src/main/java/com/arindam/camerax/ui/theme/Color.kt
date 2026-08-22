package com.arindam.camerax.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Modern camera palette. Accent is an electric mint lifted from the launcher body
 * (`#51BBA8` → `#00E8BE`) so chrome pops on near-black glass; surfaces stay neutral
 * charcoal (not muddy teal-tinted gray).
 */

// region Light
val md_theme_light_primary = Color(0xFF006B5C)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF7AFFE6)
val md_theme_light_onPrimaryContainer = Color(0xFF00201B)
val md_theme_light_secondary = Color(0xFF3E4947)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFDCE5E2)
val md_theme_light_onSecondaryContainer = Color(0xFF1A211F)
val md_theme_light_tertiary = Color(0xFF4A4458)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFE8DEF8)
val md_theme_light_onTertiaryContainer = Color(0xFF1D192B)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFF7F9F8)
val md_theme_light_onBackground = Color(0xFF141918)
val md_theme_light_surface = Color(0xFFFCFCFC)
val md_theme_light_onSurface = Color(0xFF141918)
val md_theme_light_surfaceVariant = Color(0xFFDCE5E2)
val md_theme_light_onSurfaceVariant = Color(0xFF3E4947)
val md_theme_light_outline = Color(0xFF6E7976)
val md_theme_light_inverseOnSurface = Color(0xFFEFF2F1)
val md_theme_light_inverseSurface = Color(0xFF282D2C)
val md_theme_light_inversePrimary = Color(0xFF00E8BE)
val md_theme_light_shadow = Color(0xFF000000)
val md_theme_light_surfaceTint = Color(0xFF006B5C)
val md_theme_light_outlineVariant = Color(0xFFBDC9C5)
val md_theme_light_scrim = Color(0xFF000000)
// endregion

// region Dark
val md_theme_dark_primary = Color(0xFF00E8BE)
val md_theme_dark_onPrimary = Color(0xFF00382F)
val md_theme_dark_primaryContainer = Color(0xFF005245)
val md_theme_dark_onPrimaryContainer = Color(0xFF7AFFE6)
val md_theme_dark_secondary = Color(0xFFBFC9C6)
val md_theme_dark_onSecondary = Color(0xFF2A3331)
val md_theme_dark_secondaryContainer = Color(0xFF404947)
val md_theme_dark_onSecondaryContainer = Color(0xFFDCE5E2)
val md_theme_dark_tertiary = Color(0xFFCCC2DC)
val md_theme_dark_onTertiary = Color(0xFF332D41)
val md_theme_dark_tertiaryContainer = Color(0xFF4A4458)
val md_theme_dark_onTertiaryContainer = Color(0xFFE8DEF8)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF070809)
val md_theme_dark_onBackground = Color(0xFFE6E9E8)
val md_theme_dark_surface = Color(0xFF0E1012)
val md_theme_dark_onSurface = Color(0xFFE6E9E8)
val md_theme_dark_surfaceVariant = Color(0xFF1A1D21)
val md_theme_dark_onSurfaceVariant = Color(0xFFBFC9C6)
val md_theme_dark_outline = Color(0xFF899390)
val md_theme_dark_inverseOnSurface = Color(0xFF070809)
val md_theme_dark_inverseSurface = Color(0xFFE6E9E8)
val md_theme_dark_inversePrimary = Color(0xFF006B5C)
val md_theme_dark_shadow = Color(0xFF000000)
val md_theme_dark_surfaceTint = Color(0xFF00E8BE)
val md_theme_dark_outlineVariant = Color(0xFF1A1D21)
val md_theme_dark_scrim = Color(0xFF000000)
// endregion

/**
 * Live viewfinder chrome (always over a dark preview). Prefer [cameraAccent] /
 * [ThemedOverlayChrome.accent] on Settings / Gallery / Tools so Light theme gets a
 * deeper teal with readable contrast.
 */
val CameraAccent = Color(0xFF00E8BE)
/** Light-theme accent — same mint family, darker for white surfaces. */
val CameraAccentLight = Color(0xFF006B5C)
val CameraOnAccent = Color(0xFF000000)
val CameraOnAccentLight = Color(0xFFFFFFFF)
val CameraDanger = Color(0xFFFF375F)
val CameraGlass = Color(0x73000000)
val CameraGlassStrong = Color(0xA6000000)
val CameraOnGlass = Color(0xFFFFFFFF)
val CameraOnGlassMuted = Color(0x99FFFFFF)
