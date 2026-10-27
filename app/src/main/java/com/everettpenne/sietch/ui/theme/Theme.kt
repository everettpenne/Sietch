package com.everettpenne.sietch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.everettpenne.glass.GlassColors
import com.everettpenne.glass.GlassTheme

/** Sietch's colours; the theme, type scale and glass kit come from the shared Glass library. The kind colours are shared by every file icon. */
object Sietch {
    /** Graphene teal, Sietch's colour: selection, icons, buttons. */
    val accent = Color(0xFF12B6C9)
    val red = Color(0xFFFF453A)
    val green = Color(0xFF34C759)
    val blue = Color(0xFF0A84FF)
    val sand = Color(0xFFFFB340)
    val violet = Color(0xFFBF5AF2)
    val pink = Color(0xFFFF375F)
    val teal = Color(0xFF40C8E0)
    val orange = Color(0xFFFF9F0A)
}

@Composable
fun SietchTheme(content: @Composable () -> Unit) =
    GlassTheme(GlassColors(accent = Sietch.accent, red = Sietch.red, green = Sietch.green), content)
