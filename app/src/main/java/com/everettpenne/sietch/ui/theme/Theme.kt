package com.everettpenne.sietch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.everettpenne.glass.GlassColors
import com.everettpenne.glass.GlassTheme

/** Sietch's brand colours; the theme, type scale and glass kit come from the shared Glass library. */
object Sietch {
    /** Graphene teal: tab selection, icons, buttons. Cool and quiet, like the material it is named for. */
    val accent = Color(0xFF12B6C9)
    val teal = Color(0xFF0FA6C7)
    val red = Color(0xFFFF453A)
    val green = Color(0xFF34C759)
    val blue = Color(0xFF0A84FF)
    val sand = Color(0xFFE8A33D)
    val violet = Color(0xFFBF5AF2)
    val pink = Color(0xFFFF375F)
}

@Composable
fun SietchTheme(content: @Composable () -> Unit) =
    GlassTheme(GlassColors(accent = Sietch.accent, red = Sietch.red, green = Sietch.green), content)
