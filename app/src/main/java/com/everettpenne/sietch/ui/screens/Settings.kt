package com.everettpenne.sietch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.everettpenne.glass.*
import com.everettpenne.sietch.BuildConfig
import com.everettpenne.sietch.data.SortBy
import com.everettpenne.sietch.data.SortOrder
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.updater.collectStatus

@Composable
fun SettingsScreen(nav: Navigator, onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val showHidden by graph.settings.showHidden.collectAsState()
    val foldersFirst by graph.settings.foldersFirst.collectAsState()
    val sortBy by graph.settings.sortBy.collectAsState()
    val sortOrder by graph.settings.sortOrder.collectAsState()
    val locations by graph.locations.items.collectAsState()

    GlassScreen(bars = { GlassCenteredTopBar("Settings", onBack, Modifier.align(Alignment.TopCenter)) }) { contentPadding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding(), start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Group("Browsing") {
                SwitchRow(Icons.Rounded.Folder, "Folders first", foldersFirst, graph.settings::setFoldersFirst)
                SwitchRow(Icons.Rounded.Visibility, "Show hidden files", showHidden, graph.settings::setShowHidden)
            }
            Group("Default sort", footer = "How folders list their contents; you can also change it from the sort button in any folder.") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Labeled("Sort by") {
                        SortBy.entries.forEach { s -> GlassChip(s == sortBy, { graph.settings.setSortBy(s) }) { Text(label(s)) } }
                    }
                    Labeled("Direction") {
                        GlassChip(sortOrder == SortOrder.ASC, { graph.settings.setSortOrder(SortOrder.ASC) }) { Text("Ascending") }
                        GlassChip(sortOrder == SortOrder.DESC, { graph.settings.setSortOrder(SortOrder.DESC) }) { Text("Descending") }
                    }
                }
            }
            Group("Folders", footer = "Sietch stores its access tokens in its own private storage, on this phone only. Add and remove folders from the Places tab.") {
                Info(Icons.Rounded.FolderSpecial, "Granted folders", "${locations.size} on this device")
            }
            Group("About") {
                com.everettpenne.updater.LocalUpdater.current?.let { u -> NavRow(Icons.Rounded.SystemUpdate, "Check for Updates", u.collectStatus()) { u.onSettingsRowClick() } }
                Info(Icons.Rounded.Info, "Sietch ${BuildConfig.VERSION_NAME}", "A file navigator with iOS-style liquid glass")
                Info(Icons.Rounded.Lock, "Private by design", "No storage permissions, no analytics, no tracking. It reads only the folders you hand it.")
            }
        }
    }
}

private fun label(s: SortBy) = when (s) {
    SortBy.NAME -> "Name"; SortBy.SIZE -> "Size"; SortBy.MODIFIED -> "Modified"; SortBy.TYPE -> "Type"
}
