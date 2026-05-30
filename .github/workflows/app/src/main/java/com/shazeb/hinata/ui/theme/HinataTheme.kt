package com.shazeb.hinata.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HinataColors = darkColorScheme(
    primary = Color(0xFFFF6B9D),
    secondary = Color(0xFFBB86FC),
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF1A1A2E),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun HinataTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HinataColors,
        content = content
    )
}
