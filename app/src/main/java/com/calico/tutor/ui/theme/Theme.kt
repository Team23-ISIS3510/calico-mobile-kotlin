package com.calico.tutor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = WhiteBase,
    primaryContainer = Color(0xFFFFECD9),
    onPrimaryContainer = Color(0xFF3F2500),
    
    secondary = SecondaryOrange,
    onSecondary = WhiteBase,
    secondaryContainer = Color(0xFFFFE5CC),
    onSecondaryContainer = Color(0xFF4D2800),
    
    tertiary = AccentMagenta,
    onTertiary = WhiteBase,
    tertiaryContainer = Color(0xFFF8D8E8),
    onTertiaryContainer = Color(0xFF3D1F3D),
    
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
