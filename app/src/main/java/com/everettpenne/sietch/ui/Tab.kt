package com.everettpenne.sietch.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.everettpenne.glass.GlassTabSpec

/** Sietch's top-level destinations. Home and Recents sit in the glass tab capsule; Search is the round button beside it. */
enum class Tab(override val route: String, override val label: String, override val icon: ImageVector) : GlassTabSpec {
    Home("home", "Home", Icons.Rounded.Home),
    Recents("recents", "Recents", Icons.Rounded.History),
    Search("search", "Search", Icons.Rounded.Search);

    companion object { val bar = listOf(Home, Recents) }
}
