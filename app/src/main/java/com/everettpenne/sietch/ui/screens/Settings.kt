package com.everettpenne.sietch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.everettpenne.sietch.BuildConfig
import com.everettpenne.sietch.data.Access
import com.everettpenne.sietch.data.SortBy
import com.everettpenne.sietch.data.SortOrder
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.glass.*
import com.everettpenne.updater.collectStatus

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val access by rememberAccess()
    val showHidden by graph.settings.showHidden.collectAsState()
    val foldersFirst by graph.settings.foldersFirst.collectAsState()
    val sortBy by graph.settings.sortBy.collectAsState()
    val sortOrder by graph.settings.sortOrder.collectAsState()
    val useTrash by graph.settings.useTrash.collectAsState()
    val days by graph.settings.trashDays.collectAsState()
    val mediaLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) {}

    GlassScreen(bars = { GlassCenteredTopBar("Settings", onBack, Modifier.align(Alignment.TopCenter)) }) { contentPadding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding(), start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Group("Access", footer = "Sietch asks for the least that makes each feature work. Both can be turned off again in Android's settings; the app then shows what it is missing.") {
                NavRow(Icons.Rounded.PermMedia, "Photos, videos and audio", if (access.media) "Allowed" else "Not allowed. Tap to allow") { if (!access.media) mediaLauncher.launch(Access.mediaPermissions) }
                NavRow(Icons.Rounded.FolderOpen, "All files access", if (access.allFiles) "Allowed. Tap to review" else "Not allowed. Tap to allow") { runCatching { context.startActivity(Access.allFilesIntent(context)) } }
            }
            Group("Browsing") {
                SwitchRow(Icons.Rounded.Folder, "Folders first", foldersFirst, graph.settings::setFoldersFirst)
                SwitchRow(Icons.Rounded.Visibility, "Show hidden files", showHidden, graph.settings::setShowHidden)
            }
            Group("Default sort", footer = "How folders list their contents; you can also change it from the sort button in any folder.") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Labeled("Sort by") { SortBy.entries.forEach { s -> GlassChip(s == sortBy, { graph.settings.setSortBy(s) }) { Text(sortLabel(s)) } } }
                    Labeled("Direction") {
                        GlassChip(sortOrder == SortOrder.ASC, { graph.settings.setSortOrder(SortOrder.ASC) }) { Text("Ascending") }
                        GlassChip(sortOrder == SortOrder.DESC, { graph.settings.setSortOrder(SortOrder.DESC) }) { Text("Descending") }
                    }
                }
            }
            Group("Recycle bin", footer = "With the recycle bin on, deleting moves a file aside so you can restore it. Off, deleting removes it for good after a confirmation.") {
                SwitchRow(Icons.Rounded.Delete, "Use the recycle bin", useTrash, graph.settings::setUseTrash)
                if (useTrash) Column(Modifier.padding(16.dp)) {
                    Labeled("Keep deleted items for") { listOf(7, 15, 30).forEach { d -> GlassChip(d == days, { graph.settings.setTrashDays(d) }) { Text("$d days") } } }
                }
            }
            Group("About") {
                com.everettpenne.updater.LocalUpdater.current?.let { u -> NavRow(Icons.Rounded.SystemUpdate, "Check for Updates", u.collectStatus()) { u.onSettingsRowClick() } }
                Info(Icons.Rounded.Info, "Sietch ${BuildConfig.VERSION_NAME}", "A file manager with iOS-style liquid glass")
                Info(Icons.Rounded.Lock, "Private by design", "No analytics, no tracking, no accounts. Nothing about your files leaves the phone.")
            }
        }
    }
}
