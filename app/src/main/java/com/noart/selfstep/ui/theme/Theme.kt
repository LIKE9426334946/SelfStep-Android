package com.noart.selfstep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = ForestLight,
    onPrimaryContainer = Ink,
    secondary = Amber,
    onSecondary = Color.White,
    secondaryContainer = AmberSoft,
    onSecondaryContainer = Color(0xFF5A2E00),
    error = Rose,
    errorContainer = RoseSoft,
    background = WarmWhite,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Color(0xFF53645E),
    outline = Color(0xFF86968F),
    outlineVariant = Color(0xFFD4DED8)
)

private val DarkColors = darkColorScheme(
    primary = ForestDark,
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF0A503C),
    onPrimaryContainer = Color(0xFFC1F4DF),
    secondary = Color(0xFFFFB86F),
    onSecondary = Color(0xFF4A2700),
    secondaryContainer = Color(0xFF653A0A),
    onSecondaryContainer = Color(0xFFFFDCC1),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF8C3B3B),
    background = Night,
    onBackground = Color(0xFFE2EAE5),
    surface = NightSurface,
    onSurface = Color(0xFFE2EAE5),
    surfaceVariant = NightCard,
    onSurfaceVariant = Color(0xFFB9C9C0),
    outline = Color(0xFF83938B),
    outlineVariant = Color(0xFF3B4A42)
)

@Composable
fun SelfStepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SelfStepTypography,
        content = content
    )
}
