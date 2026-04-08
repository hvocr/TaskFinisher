package com.taskfinisher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Brand palette ────────────────────────────────────────────────────────────

val Background     = Color(0xFF0F0F0F)
val Surface        = Color(0xFF1E1E1E)
val SurfaceVariant = Color(0xFF2A2A2A)
val CardColor      = Color(0xFF1E1E1E)
val Accent         = Color(0xFF3B82F6)   // Calm blue
val AccentTeal     = Color(0xFF14B8A6)   // Teal alternative
val OnAccent       = Color(0xFFFFFFFF)

// Importance / Energy color coding
val ColorHighImportance   = Color(0xFFEF4444)  // Red
val ColorMediumImportance = Color(0xFFF59E0B)  // Amber
val ColorLowImportance    = Color(0xFF6B7280)  // Gray

val ColorHighEnergy   = Color(0xFF8B5CF6)  // Purple
val ColorMediumEnergy = Color(0xFF3B82F6)  // Blue
val ColorLowEnergy    = Color(0xFF10B981)  // Green

val OverdueTint  = Color(0xFFEF4444)
val StreakGold   = Color(0xFFF59E0B)
val Big3Glow     = Color(0xFF3B82F620)
val TextPrimary  = Color(0xFFE5E7EB)
val TextSecondary = Color(0xFF9CA3AF)
val Divider      = Color(0xFF374151)

// ─── Dark color scheme ────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary          = Accent,
    onPrimary        = OnAccent,
    primaryContainer = Color(0xFF1D4ED8),
    secondary        = AccentTeal,
    onSecondary      = OnAccent,
    background       = Background,
    onBackground     = TextPrimary,
    surface          = Surface,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline          = Divider,
    error            = ColorHighImportance,
)

// ─── Typography — 3 sizes only ────────────────────────────────────────────────

val TaskFinisherTypography = Typography(
    // 20sp — headers / screen titles
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = TextPrimary
    ),
    // 16sp — card titles, section labels
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    // 14sp — body, subtitles, chips
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    )
)

// ─── Theme composable ─────────────────────────────────────────────────────────

@Composable
fun TaskFinisherTheme(content: @Composable () -> Unit) {
    // Dark mode only
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = TaskFinisherTypography,
        content     = content
    )
}
