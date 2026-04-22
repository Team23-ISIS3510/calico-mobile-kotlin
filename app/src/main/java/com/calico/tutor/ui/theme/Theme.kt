package com.calico.tutor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    // Primary Colors - Calico Yellow
    primary = CalicoYellow,
    onPrimary = TextColorBlack,
    primaryContainer = CalicoYellow,
    onPrimaryContainer = TextColorBlack,
    
    // Secondary Colors - Calico Button Orange
    secondary = CalicoButtonOrange,
    onSecondary = TextColorBlack,
    secondaryContainer = CalicoBulletColor,
    onSecondaryContainer = TextColorBlack,
    
    // Tertiary Colors - Brand accent
    tertiary = CalicoOrange,
    onTertiary = TextColorBlack,
    tertiaryContainer = CalicoBulletColor,
    onTertiaryContainer = TextColorBlack,
    
    error = ErrorRed,
    onError = WhiteBase,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFF410002),
    
    // Backgrounds
    background = MainBackground,
    onBackground = TextColorBlack,
    surface = MenuBackground,
    onSurface = TextColorBlack,
    surfaceVariant = MaterialsBackground,
    onSurfaceVariant = IconColorBrown,
    
    outline = IconColorBrown,
    outlineVariant = OutlineVariant,
    scrim = PopupEffectOverlay
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
