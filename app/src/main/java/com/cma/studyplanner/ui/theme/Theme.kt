package com.cma.studyplanner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat

// App Colors
val Indigo = Color(0xFF6366F1)
val IndigoLight = Color(0xFFEEF0FF)
val Green = Color(0xFF10B981)
val Amber = Color(0xFFF59E0B)
val Red = Color(0xFFEF4444)
val Sidebar = Color(0xFF1E1E2E)
val SidebarText = Color(0xFFA5A7C4)
val BgLight = Color(0xFFF0F2F7)
val CardLight = Color(0xFFFFFFFF)
val TextDark = Color(0xFF1E1E2E)
val Muted = Color(0xFF8B8FA3)
val Border = Color(0xFFE4E6EF)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoLight,
    onPrimaryContainer = Indigo,
    secondary = Green,
    tertiary = Amber,
    error = Red,
    background = BgLight,
    surface = CardLight,
    onBackground = TextDark,
    onSurface = TextDark,
    outline = Border,
    surfaceVariant = Color(0xFFE8E9F0)
)

@Composable
fun CMATheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Sidebar.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
