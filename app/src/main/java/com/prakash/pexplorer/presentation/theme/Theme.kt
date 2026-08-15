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
    primary = Color(0xFF17627A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8E8F5),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = Color(0xFF4E6268),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E7EC),
    onSecondaryContainer = Color(0xFF0A1E23),
    tertiary = Color(0xFF6C5A7D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251331),
    background = Color(0xFFF8FAFB),
    onBackground = Color(0xFF191C1D),
    surface = Color(0xFFF8FAFB),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFDCE4E7),
    onSurfaceVariant = Color(0xFF40484B),
    outline = Color(0xFF70797C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DD3E9),
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004C60),
    onPrimaryContainer = Color(0xFFB8E8F5),
    secondary = Color(0xFFB5CBD0),
    onSecondary = Color(0xFF203337),
    secondaryContainer = Color(0xFF36494D),
    onSecondaryContainer = Color(0xFFD1E7EC),
    tertiary = Color(0xFFDABCE6),
    onTertiary = Color(0xFF3B2947),
    tertiaryContainer = Color(0xFF523E5B),
    onTertiaryContainer = Color(0xFFF2DAFF),
    background = Color(0xFF101415),
    onBackground = Color(0xFFE1E3E4),
    surface = Color(0xFF101415),
    onSurface = Color(0xFFE1E3E4),
    surfaceVariant = Color(0xFF40484B),
    onSurfaceVariant = Color(0xFFC0C8CA),
    outline = Color(0xFF8A9396)
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
