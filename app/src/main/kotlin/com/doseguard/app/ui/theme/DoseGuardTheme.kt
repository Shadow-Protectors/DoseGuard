package com.doseguard.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand palette ────────────────────────────────────────────────────────────
val NavyPrimary      = Color(0xFF0D47A1)
val BluePrimary      = Color(0xFF1565C0)
val BlueLight        = Color(0xFFE3F2FD)
val AccentCyan       = Color(0xFF0288D1)

// ── Status / hazard ──────────────────────────────────────────────────────────
val StatusSafe       = Color(0xFF10B981)
val StatusSafeBg     = Color(0xFFDCFCE7)
val StatusModerate   = Color(0xFFF59E0B)
val StatusModerateBg = Color(0xFFFEF3C7)
val StatusHigh       = Color(0xFFF97316)
val StatusHighBg     = Color(0xFFFFEDD5)
val StatusCritical   = Color(0xFFEF4444)
val StatusCriticalBg = Color(0xFFFEE2E2)

// ── Surface ──────────────────────────────────────────────────────────────────
val SurfaceBg        = Color(0xFFF4F7FA)
val CardWhite        = Color(0xFFFFFFFF)
val CardStroke       = Color(0xFFE2E8F0)
val TextPrimary      = Color(0xFF0F172A)
val TextSecondary    = Color(0xFF475569)
val TextMuted        = Color(0xFF94A3B8)

private val DoseGuardColorScheme = lightColorScheme(
    primary          = NavyPrimary,
    onPrimary        = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = NavyPrimary,
    secondary        = AccentCyan,
    onSecondary      = Color.White,
    background       = SurfaceBg,
    onBackground     = TextPrimary,
    surface          = CardWhite,
    onSurface        = TextPrimary,
    surfaceVariant   = Color(0xFFEFF6FF),
    outline          = CardStroke,
    error            = StatusCritical,
    onError          = Color.White,
)

@Composable
fun DoseGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DoseGuardColorScheme,
        typography  = DoseGuardTypography,
        content     = content
    )
}

/** Map risk level string → status color for consistent styling across screens. */
fun riskColor(risk: String): Color = when (risk.uppercase()) {
    "SAFE"     -> StatusSafe
    "MODERATE" -> StatusModerate
    "HIGH"     -> StatusHigh
    "CRITICAL" -> StatusCritical
    else       -> TextMuted
}

fun riskBgColor(risk: String): Color = when (risk.uppercase()) {
    "SAFE"     -> StatusSafeBg
    "MODERATE" -> StatusModerateBg
    "HIGH"     -> StatusHighBg
    "CRITICAL" -> StatusCriticalBg
    else       -> SurfaceBg
}
