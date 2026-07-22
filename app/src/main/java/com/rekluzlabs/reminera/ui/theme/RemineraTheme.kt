package com.rekluzlabs.reminera.ui.theme

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
import com.rekluzlabs.reminera.ui.settings.ThemeMode

private val SandBackground = Color(0xFFFDFBF7)
private val CardSurface = Color(0xFFF3EFE6)
private val MutedClay = Color(0xFFD97706)
private val SoftSage = Color(0xFF059669)
private val DeepEarthText = Color(0xFF2D2623)
private val MutedEarthText = Color(0xFF786C66)

private val DeepEspresso = Color(0xFF1A1615)
private val DarkSurface = Color(0xFF2A2624)
private val DarkOnSurface = Color(0xFFE8E2DC)

// Cinnamon and cream
private val CinnamonBackground = Color(0xFFFDF6F2)
private val CinnamonCardSurface = Color(0xFFFAECE7)
private val CinnamonPrimary = Color(0xFFD85A30)
private val CinnamonText = Color(0xFF4A1B0C)
private val CinnamonMutedText = Color(0xFF8B5A45)
private val CinnamonDarkBg = Color(0xFF1F1512)
private val CinnamonDarkSurface = Color(0xFF2E211C)
private val CinnamonDarkOnSurface = Color(0xFFF5DCD2)

// Dusty rose and copper
private val RoseBackground = Color(0xFFFDF7F5)
private val RoseCardSurface = Color(0xFFFBEAF0)
private val RosePrimary = Color(0xFFD4537E)
private val RoseCopperAccent = Color(0xFFB5651D)
private val RoseText = Color(0xFF4B1528)
private val RoseMutedText = Color(0xFF8B5568)
private val RoseDarkBg = Color(0xFF1E1216)
private val RoseDarkSurface = Color(0xFF2C1B22)
private val RoseDarkOnSurface = Color(0xFFF5DCE4)

// Olive and brass
private val OliveBackground = Color(0xFFFBFAF3)
private val OliveCardSurface = Color(0xFFEAF3DE)
private val OlivePrimary = Color(0xFF639922)
private val OliveBrassAccent = Color(0xFFEF9F27)
private val OliveText = Color(0xFF23300F)
private val OliveMutedText = Color(0xFF5B6B45)
private val OliveDarkBg = Color(0xFF141B0A)
private val OliveDarkSurface = Color(0xFF1F2810)
private val OliveDarkOnSurface = Color(0xFFE6F3D6)

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

private val CinnamonCreamLightColorScheme = lightColorScheme(
    background = CinnamonBackground,
    surface = CinnamonCardSurface,
    surfaceVariant = Color(0xFFF0DCD3),
    onBackground = CinnamonText,
    onSurface = CinnamonText,
    onSurfaceVariant = CinnamonMutedText,
    primary = CinnamonPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5C4B3),
    onPrimaryContainer = Color(0xFF4A1B0C),
    secondary = Color(0xFF854F0B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFAC775),
    onSecondaryContainer = Color(0xFF412402),
    tertiary = Color(0xFF993C1D),
    onTertiary = Color.White,
    outline = Color(0xFFDDC3B8),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val CinnamonCreamDarkColorScheme = darkColorScheme(
    background = CinnamonDarkBg,
    surface = CinnamonDarkSurface,
    surfaceVariant = Color(0xFF3D2C25),
    onBackground = CinnamonDarkOnSurface,
    onSurface = CinnamonDarkOnSurface,
    onSurfaceVariant = Color(0xFFD1AA9B),
    primary = CinnamonPrimary,
    onPrimary = Color(0xFF2E0D02),
    primaryContainer = Color(0xFF712B13),
    onPrimaryContainer = Color(0xFFF5C4B3),
    secondary = Color(0xFFFAC775),
    onSecondary = Color(0xFF412402),
    secondaryContainer = Color(0xFF854F0B),
    onSecondaryContainer = Color(0xFFFAEEDA),
    tertiary = Color(0xFFF0997B),
    onTertiary = Color(0xFF4A1B0C),
    outline = Color(0xFF5C433A),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

private val DustyRoseCopperLightColorScheme = lightColorScheme(
    background = RoseBackground,
    surface = RoseCardSurface,
    surfaceVariant = Color(0xFFF0DBE1),
    onBackground = RoseText,
    onSurface = RoseText,
    onSurfaceVariant = RoseMutedText,
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4C0D1),
    onPrimaryContainer = Color(0xFF72243E),
    secondary = RoseCopperAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFD3B6),
    onSecondaryContainer = Color(0xFF4A2E0D),
    tertiary = Color(0xFF993556),
    onTertiary = Color.White,
    outline = Color(0xFFDCC2CA),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DustyRoseCopperDarkColorScheme = darkColorScheme(
    background = RoseDarkBg,
    surface = RoseDarkSurface,
    surfaceVariant = Color(0xFF3D2833),
    onBackground = RoseDarkOnSurface,
    onSurface = RoseDarkOnSurface,
    onSurfaceVariant = Color(0xFFD1A9B6),
    primary = RosePrimary,
    onPrimary = Color(0xFF2E0616),
    primaryContainer = Color(0xFF72243E),
    onPrimaryContainer = Color(0xFFF4C0D1),
    secondary = Color(0xFFD9A566),
    onSecondary = Color(0xFF3D2708),
    secondaryContainer = RoseCopperAccent,
    onSecondaryContainer = Color(0xFFEFD3B6),
    tertiary = Color(0xFFED93B1),
    onTertiary = Color(0xFF4B1528),
    outline = Color(0xFF5C434B),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

private val OliveBrassLightColorScheme = lightColorScheme(
    background = OliveBackground,
    surface = OliveCardSurface,
    surfaceVariant = Color(0xFFDDE7CC),
    onBackground = OliveText,
    onSurface = OliveText,
    onSurfaceVariant = OliveMutedText,
    primary = OlivePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC0DD97),
    onPrimaryContainer = Color(0xFF173404),
    secondary = OliveBrassAccent,
    onSecondary = Color(0xFF412402),
    secondaryContainer = Color(0xFFFAC775),
    onSecondaryContainer = Color(0xFF412402),
    tertiary = Color(0xFF3B6D11),
    onTertiary = Color.White,
    outline = Color(0xFFC7D3B4),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val OliveBrassDarkColorScheme = darkColorScheme(
    background = OliveDarkBg,
    surface = OliveDarkSurface,
    surfaceVariant = Color(0xFF2E3A1B),
    onBackground = OliveDarkOnSurface,
    onSurface = OliveDarkOnSurface,
    onSurfaceVariant = Color(0xFFB8CC9E),
    primary = Color(0xFF97C459),
    onPrimary = Color(0xFF173404),
    primaryContainer = Color(0xFF3B6D11),
    onPrimaryContainer = Color(0xFFC0DD97),
    secondary = Color(0xFFFAC775),
    onSecondary = Color(0xFF412402),
    secondaryContainer = Color(0xFF854F0B),
    onSecondaryContainer = Color(0xFFFAEEDA),
    tertiary = Color(0xFFC0DD97),
    onTertiary = Color(0xFF173404),
    outline = Color(0xFF4C5C3A),
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
        ThemeMode.CINNAMON_CREAM -> if (isSystemDark) CinnamonCreamDarkColorScheme else CinnamonCreamLightColorScheme
        ThemeMode.DUSTY_ROSE_COPPER -> if (isSystemDark) DustyRoseCopperDarkColorScheme else DustyRoseCopperLightColorScheme
        ThemeMode.OLIVE_BRASS -> if (isSystemDark) OliveBrassDarkColorScheme else OliveBrassLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            val isFollowSystemTheme = themeMode == ThemeMode.WARM_TERRACOTTA ||
                    themeMode == ThemeMode.CINNAMON_CREAM ||
                    themeMode == ThemeMode.DUSTY_ROSE_COPPER ||
                    themeMode == ThemeMode.OLIVE_BRASS
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                themeMode != ThemeMode.DARK && themeMode != ThemeMode.AMOLED_BLACK && !(isFollowSystemTheme && isSystemDark)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}