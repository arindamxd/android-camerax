package com.arindam.camerax.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.arindam.camerax.util.theme.applyEdgeToEdgeBars

/**
 * Created by Arindam Karmakar on 12/09/23.
 */
private val lightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim
)

private val darkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim
)

/**
 * Material3 theme. Live viewfinder passes [isDarkTheme] true so chrome and system-bar icons
 * stay dark-canvas / light-tint regardless of Settings Light / Dark / System.
 */
@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isDynamicColor: Boolean = true,
    applySystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    val dynamicColor = isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        dynamicColor && isDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !isDarkTheme -> dynamicLightColorScheme(LocalContext.current)
        isDarkTheme -> darkColors
        else -> lightColors
    }
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )

    val view = LocalView.current
    if (applySystemBars && !view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            activity.applyEdgeToEdgeBars(lightIcons = isDarkTheme)
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = CameraTypography,
        shapes = shapes,
        content = content
    )
}

/** Glass chrome over photos / confirm; follows Light / Dark unlike live viewfinder tokens. */
data class ThemedOverlayChrome(
    val canvas: Color,
    val glass: Color,
    val stroke: Color,
    val onGlass: Color,
    val muted: Color,
    val scrim: Color,
    val chipIdle: Color
)

@Composable
fun themedOverlayChrome(): ThemedOverlayChrome {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return if (dark) {
        ThemedOverlayChrome(
            canvas = Color.Black,
            glass = CameraGlassStrong,
            stroke = Color.White.copy(alpha = 0.14f),
            onGlass = CameraOnGlass,
            muted = CameraOnGlassMuted,
            scrim = Color.Black,
            chipIdle = Color.White.copy(alpha = 0.08f)
        )
    } else {
        ThemedOverlayChrome(
            canvas = scheme.background,
            glass = Color.White.copy(alpha = 0.82f),
            stroke = Color.Black.copy(alpha = 0.12f),
            onGlass = scheme.onBackground,
            muted = scheme.onBackground.copy(alpha = 0.55f),
            scrim = Color.White,
            chipIdle = Color.Black.copy(alpha = 0.06f)
        )
    }
}

internal tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
