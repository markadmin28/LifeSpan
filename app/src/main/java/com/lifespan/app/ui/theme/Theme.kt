package com.lifespan.app.ui.theme

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

private val DarkColors = darkColorScheme(
    primary = Sky,
    onPrimary = Color(0xFF04121F),
    primaryContainer = Color(0xFF0B4A6F),
    onPrimaryContainer = Color(0xFFD8F3FF),
    secondary = Violet,
    onSecondary = Color(0xFF23103A),
    tertiary = Indigo,
    onTertiary = Color(0xFF10163A),
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnDark,
    onSurface = OnDark,
    error = Danger,
)

private val LightColors = lightColorScheme(
    primary = SkyDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEEFF),
    onPrimaryContainer = Color(0xFF04121F),
    secondary = Violet,
    onSecondary = Color.White,
    tertiary = Indigo,
    onTertiary = Color.White,
    background = CoolBgLight,
    surface = CoolSurfaceLight,
    surfaceVariant = Color(0xFFE6ECFA),
    onBackground = CoolInk,
    onSurface = CoolInk,
    error = Danger,
)

@Composable
fun LifeSpanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the brand motif is used instead of the device wallpaper palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
