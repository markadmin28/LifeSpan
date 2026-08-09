package com.lifespan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lifespan.app.data.prefs.Accent

private val CoolDark = darkColorScheme(
    primary = Sky,
    onPrimary = Color(0xFF04121F),
    primaryContainer = Color(0xFF0B4A6F),
    onPrimaryContainer = Color(0xFFD8F3FF),
    secondary = Violet,
    tertiary = Indigo,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnDark,
    onSurface = OnDark,
    error = Danger,
)

private val CoolLight = lightColorScheme(
    primary = SkyDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEEFF),
    onPrimaryContainer = Color(0xFF04121F),
    secondary = Violet,
    tertiary = Indigo,
    background = CoolBgLight,
    surface = CoolSurfaceLight,
    surfaceVariant = Color(0xFFE6ECFA),
    onBackground = CoolInk,
    onSurface = CoolInk,
    error = Danger,
)

private val WarmDark = darkColorScheme(
    primary = WOrangeSoft,
    onPrimary = Color(0xFF3A1A00),
    primaryContainer = Color(0xFF7A3B10),
    onPrimaryContainer = WCream,
    secondary = WGold,
    tertiary = WLightBlue,
    background = WBgDark,
    surface = WSurfaceDark,
    surfaceVariant = Color(0xFF3A2E24),
    onBackground = WOnDark,
    onSurface = WOnDark,
    error = Danger,
)

private val WarmLight = lightColorScheme(
    primary = WOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF3A1A00),
    secondary = WGold,
    tertiary = WLightBlueDeep,
    background = WCream,
    surface = WCreamElevated,
    surfaceVariant = Color(0xFFF3E4D0),
    onBackground = WInk,
    onSurface = WInk,
    error = Danger,
)

@Composable
fun LifeSpanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Accent = Accent.COOL,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (accent) {
        Accent.COOL -> if (darkTheme) CoolDark else CoolLight
        Accent.WARM -> if (darkTheme) WarmDark else WarmLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
