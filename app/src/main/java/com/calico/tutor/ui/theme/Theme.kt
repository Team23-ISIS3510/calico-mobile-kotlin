package com.calico.tutor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = DarkGray,
    primaryContainer = Color(0xFFFFF8E1),
    onPrimaryContainer = Color(0xFF3F3000),
    
    secondary = SecondaryBlue,
    onSecondary = WhiteBase,
    secondaryContainer = Color(0xFFE1F5FF),
    onSecondaryContainer = Color(0xFF001A33),
    
    tertiary = AccentPurple,
    onTertiary = WhiteBase,
    tertiaryContainer = Color(0xFFF3E5F5),
    onTertiaryContainer = Color(0xFF2D1B38),
    
    error = ErrorRed,
    onError = WhiteBase,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFF410002),
    
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = MediumGray,
    
    outline = Color(0xFF99889A),
    outlineVariant = OutlineVariant,
    scrim = Color(0xFF000000)
)

@Composable
fun CalicoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = CalicoTypography,
        content = content
    )
}
