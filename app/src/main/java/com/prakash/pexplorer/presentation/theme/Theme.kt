package com.prakash.pexplorer.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.prakash.pexplorer.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFFC45E00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF3A1E00),
    secondary = Color(0xFF55677A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE7F4),
    onSecondaryContainer = Color(0xFF12202E),
    tertiary = Color(0xFF1E88E5),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3E7FF),
    onTertiaryContainer = Color(0xFF0A3B63),
    background = Color(0xFFF8FAFE),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF8FAFE),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E8F0),
    onSurfaceVariant = Color(0xFF41484E),
    outline = Color(0xFF737980)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB77A),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF5C3200),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFB7C6D8),
    onSecondary = Color(0xFF223242),
    secondaryContainer = Color(0xFF38495B),
    onSecondaryContainer = Color(0xFFDCE7F4),
    tertiary = Color(0xFF9CC8FF),
    onTertiary = Color(0xFF003258),
    tertiaryContainer = Color(0xFF00497E),
    onTertiaryContainer = Color(0xFFD3E7FF),
    background = Color(0xFF101417),
    onBackground = Color(0xFFE2E5E8),
    surface = Color(0xFF101417),
    onSurface = Color(0xFFE2E5E8),
    surfaceVariant = Color(0xFF41484E),
    onSurfaceVariant = Color(0xFFC1C7CC),
    outline = Color(0xFF8B939B)
)

private val ExplorerTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Medium)
    )
}

@Composable
fun PExplorerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ExplorerTypography,
        content = content
    )
}
