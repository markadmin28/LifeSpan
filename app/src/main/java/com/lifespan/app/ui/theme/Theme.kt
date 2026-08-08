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
    primary = OrangeSoft,
    onPrimary = Color(0xFF3A1A00),
    primaryContainer = Color(0xFF7A3B10),
    onPrimaryContainer = Cream,
    secondary = Gold,
    onSecondary = Color(0xFF3A2A00),
    tertiary = LightBlue,
    onTertiary = Color(0xFF042231),
    background = WarmBgDark,
    surface = WarmSurfaceDark,
    surfaceVariant = Color(0xFF3A2E24),
    onBackground = WarmOnDark,
    onSurface = WarmOnDark,
    error = Danger,
)

private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF3A1A00),
    secondary = Gold,
    onSecondary = Color(0xFF3A2A00),
    tertiary = LightBlueDeep,
    onTertiary = Color.White,
    background = Cream,
    surface = CreamElevated,
    surfaceVariant = Color(0xFFF3E4D0),
    onBackground = WarmInk,
    onSurface = WarmInk,
    error = Danger,
)

@Composable
fun LifeSpanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the warm brand motif is used instead of the device wallpaper palette.
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
