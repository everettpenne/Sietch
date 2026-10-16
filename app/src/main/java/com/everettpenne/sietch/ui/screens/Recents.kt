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
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.sietch.util.formatRelative

/** Folders and files the user opened recently, newest first. */
@Composable
fun RecentsScreen(nav: Navigator) {
    val context = LocalContext.current
    val graph = context.graph
    val recents by graph.recents.items.collectAsState()

    fun open(recent: Recent) {
        graph.recents.record(recent.name, recent.treeUri, recent.documentId, recent.isDirectory)
        if (recent.isDirectory) nav.openFolder(recent.treeUri, recent.documentId, recent.name)
        else {
            val node = com.everettpenne.sietch.data.Node(
                com.everettpenne.sietch.data.Folder(recent.treeUri, recent.documentId), recent.documentId,
                recent.name, null, 0L, 0L, false)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(graph.fileSystem.documentUri(node), "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    GlassScreen(bars = {
        Column(Modifier.align(Alignment.TopCenter)) {
            GlassTopBar("Recents", actions = {
                if (recents.isNotEmpty()) GlassIconButton(Icons.Rounded.DeleteSweep, "Clear recents", { graph.recents.clear() })
            })
        }
    }) {
        if (recents.isEmpty()) Box(Modifier.fillMaxSize()) {
            EmptyState(Icons.Rounded.History, "Nothing here yet", "Folders and files you open will show up here.")
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = TopBarHeight + 16.dp, bottom = bottomContentPadding(), start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(recents, key = { "${it.treeUri}:${it.documentId}" }) { recent ->
                FileRow(recent.name, null, recent.isDirectory, 0L, recent.visitedAt,
                    onClick = { open(recent) },
                    subtitle = "${if (recent.isDirectory) "Folder" else "File"}  ·  ${formatRelative(recent.visitedAt)}",
                    trailing = {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Icon(if (recent.isDirectory) Icons.Rounded.ChevronRight else Icons.Rounded.OpenInNew, null,
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    })
            }
        }
    }
}
