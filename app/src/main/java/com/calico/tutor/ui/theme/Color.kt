package com.calico.tutor.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors - Calico Brand Identity
val CalicoYellow = Color(0xFFFDB61E)      // Main Color – Calico #FDB61E
val CalicoOrange = Color(0xFFFF9505)      // Calico Darker #FF9505
val CalicoDarker = Color(0xFFFF9505)      // Alias for consistency

// Action & Interactive Elements
val CalicoButtonOrange = Color(0xFFFAA324) // Calico Buttons #FAA324
val CalicoBulletColor = Color(0xFFE8DECF)  // Calico Bullet Color #E8DECF

// Text & Iconography
val TextColorBlack = Color(0xFF000000)     // Text Color #000000
val IconColorBrown = Color(0xFF9E7A47)    // Icons Color #9E7A47

// Backgrounds & Surfaces
val MainBackground = Color(0xFFFCFAF7)    // Main Background #FCFAF7
val LoginBackground = Color(0xFFF5F0E5)   // Login Background #F5F0E5
val MaterialsBackground = Color(0xFFF5F0E5) // Materials Background #F5F0E5
val MenuBackground = Color(0xFFFFFFFF)    // Menu Background #FFFFFF

// Overlays & Effects
val PopupEffectOverlay = Color(0x66141414) // Popup Effect #141414 (40% opacity = 0x66)

// Status & Status Indicators (for occupancy/demand visualization)
val StatusHighRed = Color(0xFFE53935)      // High occupancy indicator
val StatusMediumYellow = Color(0xFFFDD835) // Medium occupancy indicator
val StatusLowGreen = Color(0xFF43A047)     // Low occupancy indicator

// Error Colors
val ErrorCardBackground = Color(0xFFFFEBEE) // Light red background for errors
val ErrorCardText = Color(0xFFC62828)       // Dark red text for errors

// Legacy Aliases (for backward compatibility with existing screens)
val CreamBackground = LoginBackground      // Legacy: maps to LoginBackground (#F5F0E5)
val CreamInput = LoginBackground           // Legacy: maps to LoginBackground (#F5F0E5)
val BrownText = IconColorBrown             // Legacy: maps to IconColorBrown (#9E7A47)
val BeigeButton = CalicoBulletColor        // Legacy: maps to CalicoBulletColor (#E8DECF)

// Semantic Aliases for Material Theme Compatibility
val PrimaryOrange = CalicoOrange         // Primary for Material theme
val SecondaryOrange = CalicoButtonOrange  // Secondary for Material theme
val PrimaryDark = CalicoOrange
val AccentMagenta = Color(0xFFCF3476)

// Additional UI Colors
val SecondaryBlue = Color(0xFF1E88E5)
val AccentPurple = Color(0xFF8E44AD)
val SuccessGreen = Color(0xFF27AE60)
val WarningOrange = Color(0xFFE67E22)
val ErrorRed = Color(0xFFE74C3C)

// Neutral Colors
val DarkGray = Color(0xFF2C3E50)
val MediumGray = Color(0xFF95A5A6)
val LightGray = Color(0xFFECF0F1)
val WhiteBase = Color(0xFFFFFFFF)

// Text & Surface Colors
val OnSurface = TextColorBlack          // Use design spec black for text
val OutlineVariant = Color(0xFFCAC7D0)

// Background & Surface
val Background = MainBackground          // Use design spec main background
val Surface = MenuBackground            // Use design spec menu background (white)
val SurfaceVariant = MaterialsBackground // Use design spec materials background
val OnBackground = TextColorBlack
