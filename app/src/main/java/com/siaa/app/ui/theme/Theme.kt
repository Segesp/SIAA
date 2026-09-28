package com.siaa.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLightBlue,
    onPrimary = BackgroundDark,
    primaryContainer = PrimaryDarkBlue,
    onPrimaryContainer = OnSurfaceDark,
    secondary = SecondaryLightTeal,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = AccentError,
    outline = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceLight,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = SecondaryTeal,
    onSecondary = SurfaceLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = AccentError,
    outline = Color(0xFFCBD5E1)
)

private val HighContrastColorScheme = darkColorScheme(
    primary = HcPrimaryDark,
    onPrimary = HcOnPrimaryDark,
    primaryContainer = Color(0xFF333300),
    onPrimaryContainer = HcPrimaryDark,
    secondary = Color(0xFF00FFFF),
    onSecondary = Color(0xFF000000),
    background = HcBackgroundDark,
    onBackground = HcOnSurfaceDark,
    surface = HcSurfaceDark,
    onSurface = HcOnSurfaceDark,
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFEEEEEE),
    error = Color(0xFFFF4444),
    outline = Color(0xFFFFFFFF)
)

@Composable
fun SIAATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast -> HighContrastColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
