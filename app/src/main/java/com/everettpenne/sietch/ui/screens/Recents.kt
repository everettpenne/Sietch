package com.everettpenne.sietch.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.everettpenne.glass.*
import com.everettpenne.sietch.data.Recent
import com.everettpenne.sietch.data.kindLabel
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.sietch.util.FolderStep
import com.everettpenne.sietch.util.formatRelative
import kotlinx.coroutines.delay

/** Folders and files the user opened recently, newest first. */
@Composable
fun RecentsScreen(nav: Navigator) {
    val context = LocalContext.current
    val graph = context.graph
    val recents by graph.recents.items.collectAsState()
    var msg by remember { mutableStateOf<String?>(null) }
    val opener = rememberOpener()

    fun open(recent: Recent) {
        graph.recents.record(recent.name, recent.treeUri, recent.documentId, recent.isDirectory, recent.mime)
        if (recent.isDirectory) nav.openFolder(listOf(FolderStep(recent.treeUri, recent.documentId, recent.name)))
        else if (!opener.open(graph.fileSystem.documentUri(recent.treeUri, recent.documentId), recent.name, recent.mime)) msg = "No app can open ${recent.name}"
    }

    GlassScreen(bars = {
        Column(Modifier.align(Alignment.TopCenter)) {
            GlassTopBar("Recents", actions = {
                if (recents.isNotEmpty()) GlassIconButton(Icons.Rounded.DeleteSweep, "Clear recents", { graph.recents.clear() })
            })
        }
    }) { contentPadding ->
        if (recents.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(Icons.Rounded.History, "Nothing here yet", "Folders and files you open will show up here.")
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = contentPadding.calculateTopPadding() + 16.dp, bottom = contentPadding.calculateBottomPadding(), start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(recents, key = { "${it.treeUri}:${it.documentId}" }) { recent ->
                FileRow(recent.name, recent.mime, recent.isDirectory, 0L, recent.visitedAt,
                    onClick = { open(recent) },
                    subtitle = "${kindLabel(recent.name, recent.mime, recent.isDirectory)}  ·  ${formatRelative(recent.visitedAt)}",
                    thumbnailUri = if (hasThumbnail(recent.name, recent.mime)) graph.fileSystem.documentUri(recent.treeUri, recent.documentId).toString() else null,
                    trailing = {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Icon(if (recent.isDirectory || com.everettpenne.sietch.data.inAppViewer(recent.name, recent.mime) != com.everettpenne.sietch.data.InApp.NONE) Icons.Rounded.ChevronRight else Icons.Rounded.OpenInNew, null,
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    })
            }
        }
        msg?.let { m ->
            Msg(m, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
            LaunchedEffect(m) { delay(2400); msg = null }
        }
    }
}
