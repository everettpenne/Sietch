package com.everettpenne.sietch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.everettpenne.sietch.data.TrashItem
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.sietch.util.formatBytes
import com.everettpenne.glass.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The recycle bin: what was deleted, how long each item has left, and a way to put it back or remove it for good. */
@Composable
fun TrashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val items by graph.trash.items.collectAsState()
    val days by graph.settings.trashDays.collectAsState()
    val ops = rememberFileOps()
    val scope = rememberCoroutineScope()
    var confirmEmpty by remember { mutableStateOf(false) }
    var forever by remember { mutableStateOf<TrashItem?>(null) }
    val sorted = remember(items) { items.sortedByDescending { it.deletedAt } }

    fun restore(item: TrashItem) = scope.launch {
        val back = withContext(Dispatchers.IO) { graph.trash.restore(item.id) }
        if (back != null) { ops.rescan(listOf(back.path)); ops.msg = "Restored ${item.name}" } else ops.msg = "Couldn't restore ${item.name}"
    }

    GlassScreen(bars = {
        Column(Modifier.align(Alignment.TopCenter)) {
            GlassCenteredTopBar("Recycle bin", onBack, subtitle = if (items.isEmpty()) null else "${items.size} ${if (items.size == 1) "item" else "items"} · ${formatBytes(graph.trash.totalSize())}", trailing = {
                if (items.isNotEmpty()) GlassIconButton(Icons.Rounded.DeleteSweep, "Empty the recycle bin", { confirmEmpty = true })
            })
        }
    }) { contentPadding ->
        if (sorted.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(Icons.Rounded.Delete, "The recycle bin is empty", "Deleted files stay here for $days days before they are removed.")
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 24.dp, start = 12.dp, end = 12.dp)) {
            items(sorted, key = { it.id }) { item ->
                val left = graph.trash.daysLeft(item, days)
                var menu by remember { mutableStateOf(false) }
                FileRow(item.name, null, item.isDirectory, item.size, item.deletedAt, onClick = { menu = true },
                    subtitle = "${if (item.isDirectory) "Folder" else formatBytes(item.size)}  ·  ${if (left == 0) "removed today" else "$left ${if (left == 1) "day" else "days"} left"}",
                    thumbnailUri = if (hasThumbnail(item.name, null)) "file://${graph.trash.payloadOf(item).path}" else null,
                    trailing = {
                        Box {
                            Box(Modifier.size(44.dp).clickable { menu = true }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MoreHoriz, "More for ${item.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            GlassDropdownMenu(menu, { menu = false }) {
                                GlassMenuItem("Restore", { menu = false; restore(item) }, icon = Icons.Rounded.Restore)
                                GlassMenuItem("Delete forever", { menu = false; forever = item }, icon = Icons.Rounded.DeleteForever, destructive = true)
                            }
                        }
                    })
            }
            item { Text("Items are removed automatically after $days days. Change this in Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
        }
        FileOpsHost(ops)
    }
    if (confirmEmpty) GlassAlert("Empty the recycle bin?", "${items.size} ${if (items.size == 1) "item is" else "items are"} removed permanently. This can't be undone.", confirmLabel = "Empty", destructive = true,
        onConfirm = { confirmEmpty = false; scope.launch { val failed = withContext(Dispatchers.IO) { graph.trash.empty() }; ops.msg = if (failed == 0) "Recycle bin emptied" else "$failed couldn't be removed" } },
        onDismiss = { confirmEmpty = false })
    forever?.let { item -> GlassAlert("Delete \"${item.name}\" forever?", "This can't be undone.", confirmLabel = "Delete", destructive = true,
        onConfirm = { forever = null; scope.launch { withContext(Dispatchers.IO) { graph.trash.deleteForever(item.id) } } }, onDismiss = { forever = null }) }
}
