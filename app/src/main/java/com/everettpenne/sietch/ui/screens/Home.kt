package com.everettpenne.sietch.ui.screens

import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.everettpenne.sietch.data.Category
import com.everettpenne.sietch.data.Node
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.sietch.ui.theme.Sietch
import com.everettpenne.sietch.util.formatBytes
import com.everettpenne.glass.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class Storage(val total: Long, val free: Long) { val used get() = total - free }

private fun readStorage(root: File): Storage = runCatching { StatFs(root.path).let { Storage(it.blockCountLong * it.blockSizeLong, it.availableBytes) } }.getOrDefault(Storage(0, 0))

/**
 * Sietch's home: how full the phone is, the categories (with counts), the usual folders, the recycle bin, and the newest files. What the user has
 * not allowed yet is said plainly at the top, with the one button that fixes it.
 */
@Composable
fun HomeScreen(nav: Navigator, openRecents: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val access by rememberAccess()
    var granted by remember { mutableIntStateOf(0) }
    var storage by remember { mutableStateOf(Storage(0, 0)) }
    var counts by remember { mutableStateOf<Map<Category, Int>>(emptyMap()) }
    var recent by remember { mutableStateOf<List<Node>>(emptyList()) }
    val trashed by graph.trash.items.collectAsState()
    val ops = rememberFileOps()
    val state = rememberListState()

    LaunchedEffect(access, granted) {
        withContext(Dispatchers.IO) {
            storage = readStorage(graph.root)
            counts = Category.entries.filter { access.allFiles || !it.needsAllFiles }.associateWith { graph.media.count(it) }
            recent = if (access.any) graph.media.recent(6) else emptyList()
        }
    }

    GlassScreen(bars = {
        Column(Modifier.align(Alignment.TopCenter)) {
            GlassTopBar("Sietch", actions = { GlassIconButton(Icons.Rounded.Settings, "Settings", nav::openSettings) })
        }
    }) { contentPadding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { AccessPrompt(access, needAllFiles = !access.allFiles, onGranted = { granted++ }) }
            item { StorageCard(storage, onClick = { nav.openFolder(graph.root.path) }) }
            item { SectionTitle("Categories") }
            items(Category.entries.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { c ->
                        val available = access.allFiles || !c.needsAllFiles
                        CategoryTile(c, count = if (available) counts[c] else null, enabled = available, modifier = Modifier.weight(1f), onClick = { nav.openCategory(c) })
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { SectionTitle("Folders") }
            item {
                Group("") {
                    val quick = listOf(
                        Triple("Internal storage", Icons.Rounded.PhoneAndroid, graph.root),
                        Triple("Camera", Icons.Rounded.PhotoCamera, File(graph.root, "DCIM")),
                        Triple("Pictures", Icons.Rounded.Image, File(graph.root, "Pictures")),
                        Triple("Documents", Icons.Rounded.Description, File(graph.root, "Documents")),
                        Triple("Downloads", Icons.Rounded.Download, File(graph.root, "Download")),
                    ).filter { it.third.isDirectory || it.third == graph.root }
                    quick.forEach { (title, icon, dir) -> NavRow(icon, title, null) { nav.openFolder(dir.path) } }
                }
            }
            item {
                Group("") {
                    NavRow(Icons.Rounded.Delete, "Recycle bin", if (trashed.isEmpty()) "Empty" else "${trashed.size} ${if (trashed.size == 1) "item" else "items"} · ${formatBytes(graph.trash.totalSize())}", nav::openTrash)
                }
            }
            if (recent.isNotEmpty()) {
                item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SectionTitle("Recent files") }
                    Text("See all", style = MaterialTheme.typography.bodyMedium, color = Sietch.accent, modifier = Modifier.clickable(onClick = openRecents).padding(10.dp))
                } }
                items(recent, key = { it.path }) { node ->
                    FileRow(node.name, node.mime, false, node.size, node.modified, onClick = { ops.open(node) }, thumbnailUri = if (hasThumbnail(node.name, node.mime)) "file://${node.path}" else null)
                }
            }
        }
        FileOpsHost(ops)
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp))

@Composable
private fun StorageCard(s: Storage, onClick: () -> Unit) {
    val ink = glassPalette().ink
    val fraction = if (s.total > 0) s.used.toFloat() / s.total else 0f
    Column(Modifier.fillMaxWidth().glassSurface(RoundedCornerShape(24.dp), blurRadius = 6f, lensHeight = 16f, lensAmount = 28f).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.PhoneAndroid, null, tint = Sietch.accent)
            Text("Internal storage", style = MaterialTheme.typography.titleSmall, color = ink, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = ink.copy(alpha = .5f))
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(ink.copy(alpha = .12f))) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Sietch.accent))
        }
        Text(if (s.total > 0) "${formatBytes(s.used)} used of ${formatBytes(s.total)}  ·  ${formatBytes(s.free)} free" else "Storage size unavailable",
            style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = .65f))
    }
}

@Composable
private fun CategoryTile(c: Category, count: Int?, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ink = glassPalette().ink
    Row(modifier.heightIn(min = 64.dp).glassSurface(RoundedCornerShape(20.dp), blurRadius = 6f, lensHeight = 16f, lensAmount = 28f)
        .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).background(c.color().copy(alpha = .16f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(c.icon(), null, tint = c.color(), modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(c.label, style = MaterialTheme.typography.titleSmall, color = if (enabled) ink else ink.copy(alpha = .5f), maxLines = 1)
            Text(if (!enabled) "Needs all files" else count?.let { "$it" } ?: "—", style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = .55f), maxLines = 1)
        }
    }
}
