package com.alex.steamshowcase.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object SteamColors {
    val Bg = Color(0xFF171A21)
    val Panel = Color(0xFF1B2838)
    val PanelLight = Color(0xFF2A475E)
    val Text = Color(0xFFC6D4DF)
    val TextDim = Color(0xFF8F98A0)
    val Accent = Color(0xFF66C0F4)
    val Green = Color(0xFF90BA3C)
    val InGame = Color(0xFF90BA3C)
    val Online = Color(0xFF57CBDE)
    val Offline = Color(0xFF898989)

    /** Steam-ish accent presets for the settings screen. */
    val presets: List<Pair<String, Long>> = listOf(
        "Steam blue" to 0xFF66C0F4,
        "In-game green" to 0xFF90BA3C,
        "Gold" to 0xFFE5B44F,
        "Coral" to 0xFFF07A6E,
        "Lavender" to 0xFFB49CF5,
        "Mint" to 0xFF6FE3B3,
        "White" to 0xFFF2F2F2,
    )
}

@Composable
fun SteamShowcaseTheme(accent: Long, content: @Composable () -> Unit) {
    val accentColor = Color(accent)
    val scheme = darkColorScheme(
        primary = accentColor,
        onPrimary = Color(0xFF0E141B),
        secondary = accentColor,
        onSecondary = Color(0xFF0E141B),
        background = SteamColors.Bg,
        onBackground = SteamColors.Text,
        surface = SteamColors.Panel,
        onSurface = SteamColors.Text,
        surfaceVariant = SteamColors.PanelLight,
        onSurfaceVariant = SteamColors.TextDim,
        outline = SteamColors.PanelLight,
    )
    val typography = Typography(
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        labelSmall = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
    )
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
