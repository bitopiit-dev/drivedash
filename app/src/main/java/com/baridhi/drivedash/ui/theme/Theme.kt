package com.baridhi.drivedash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Night-drive palette: deep indigo sky with neon instrument colours. */
object Dash {
    val SkyTop = Color(0xFF070B24)
    val SkyBottom = Color(0xFF1C0E3A)
    val Panel = Color(0x14FFFFFF)
    val PanelEdge = Color(0x22FFFFFF)

    val Cyan = Color(0xFF00E5FF)
    val Blue = Color(0xFF3D7BFF)
    val Lime = Color(0xFFB6FF3B)
    val Amber = Color(0xFFFFB020)
    val Magenta = Color(0xFFFF2E93)
    val Red = Color(0xFFFF3B5C)
    val Violet = Color(0xFFA15CFF)

    val Text = Color(0xFFF4F6FF)
    val TextDim = Color(0x99F4F6FF)
    val TextFaint = Color(0x55F4F6FF)
}

@Composable
fun DriveDashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Dash.Cyan,
            secondary = Dash.Magenta,
            tertiary = Dash.Lime,
            background = Dash.SkyTop,
            surface = Dash.SkyTop,
            onPrimary = Dash.SkyTop,
            onBackground = Dash.Text,
            onSurface = Dash.Text
        ),
        content = content
    )
}
