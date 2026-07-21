package com.example.reminera.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.reminera.ui.settings.ThemeMode

private val SandBackground = Color(0xFFFDFBF7)
private val CardSurface = Color(0xFFF3EFE6)
private val MutedClay = Color(0xFFD97706)
private val SoftSage = Color(0xFF059669)
private val DeepEarthText = Color(0xFF2D2623)
private val MutedEarthText = Color(0xFF786C66)

private val DeepEspresso = Color(0xFF1A1615)
private val DarkSurface = Color(0xFF2A2624)
private val DarkOnSurface = Color(0xFFE8E2DC)

private val LightColorScheme = lightColorScheme()

private val DarkColorScheme = darkColorScheme()

private val AmoledBlackColorScheme = darkColorScheme(
    background = Color.Black,
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF1A1A1A),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCCCCCC),
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF888888),
    onSecondary = Color.Black,
    outline = Color(0xFF444444)
)

private val WarmTerracottaLightColorScheme = lightColorScheme(
    background = SandBackground,
    surface = CardSurface,
    surfaceVariant = Color(0xFFE8E0D6),
    onBackground = DeepEarthText,
    onSurface = DeepEarthText,
    onSurfaceVariant = MutedEarthText,
    primary = MutedClay,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = SoftSage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    outline = Color(0xFFD4C9BD),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val WarmTerracottaDarkColorScheme = darkColorScheme(
    background = DeepEspresso,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF3A3532),
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = Color(0xFFB0A89F),
    primary = MutedClay,
    onPrimary = Color(0xFF1A1105),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = SoftSage,
    onSecondary = Color(0xFF052E1D),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF1A1105),
    outline = Color(0xFF5A524A),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

@Composable
fun RemineraTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.AMOLED_BLACK -> AmoledBlackColorScheme
        ThemeMode.WARM_TERRACOTTA -> if (isSystemDark) WarmTerracottaDarkColorScheme else WarmTerracottaLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                themeMode != ThemeMode.DARK && themeMode != ThemeMode.AMOLED_BLACK && !(themeMode == ThemeMode.WARM_TERRACOTTA && isSystemDark)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
