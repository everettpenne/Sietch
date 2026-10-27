package com.everettpenne.sietch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everettpenne.sietch.data.*
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.util.formatBytes
import com.everettpenne.sietch.util.formatRelative
import com.everettpenne.glass.GlassButton
import com.everettpenne.glass.GlassIconButton
import com.everettpenne.glass.GlassSheet
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun NewFolderSheet(parentName: String, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    GlassSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New folder", style = MaterialTheme.typography.titleLarge)
            Text("Created inside $parentName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GlassTextField(name, { name = it }, "Folder name", leading = Icons.Rounded.CreateNewFolder, imeAction = ImeAction.Done, autoFocus = true,
                onSubmit = { if (name.isNotBlank()) onCreate(name.trim()) })
            GlassButton("Create", { onCreate(name.trim()) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank(), prominent = true)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun RenameSheet(node: Node, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(node.name) }
    GlassSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Rename", style = MaterialTheme.typography.titleLarge)
            GlassTextField(name, { name = it }, "Name", leading = Icons.Rounded.DriveFileRenameOutline, imeAction = ImeAction.Done, autoFocus = true,
                onSubmit = { if (name.isNotBlank() && name != node.name) onRename(name.trim()) })
            GlassButton("Save", { onRename(name.trim()) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && name != node.name, prominent = true)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Kind, size (a folder's is worked out on the spot), date and location of one entry. */
@Composable
fun DetailsSheet(node: Node, onDismiss: () -> Unit) {
    val graph = LocalContext.current.graph
    var size by remember(node.path) { mutableStateOf<Long?>(if (node.isDirectory) null else node.size) }
    LaunchedEffect(node.path) { if (node.isDirectory) size = withContext(Dispatchers.IO) { graph.fs.sizeOf(File(node.path)) } }
    GlassSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FileIcon(node.name, node.mime, node.isDirectory)
                Column {
                    Text(node.name, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                    Text(if (node.isDirectory) "Folder" else (node.mime ?: "File"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DetailRow("Kind", kindLabel(node.name, node.mime, node.isDirectory))
            DetailRow("Size", size?.let { formatBytes(it) } ?: "Counting…")
            if (node.modified > 0) DetailRow("Modified", formatRelative(node.modified))
            DetailRow("Where", File(node.path).parent.orEmpty())
            GlassButton("Done", onDismiss, Modifier.fillMaxWidth(), prominent = true)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) = Row {
    Text(label, Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
}

/** A folder chooser for copy/move: starts at the storage root and navigates down. "Use this folder" uses the folder on screen. */
@Composable
fun FolderPicker(onPick: (File) -> Unit, onDismiss: () -> Unit) {
    val graph = LocalContext.current.graph
    val stack = remember { mutableStateListOf(graph.root) }
    val current = stack.last()
    var children by remember { mutableStateOf<List<Node>?>(null) }
    LaunchedEffect(current) {
        children = runCatching { withContext(Dispatchers.IO) { graph.fs.list(current).filter { it.isDirectory && !it.name.startsWith(".") } } }.getOrNull()
    }
    GlassSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Choose a folder", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stack.size > 1) GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Up", { stack.removeAt(stack.lastIndex) }, size = 36)
                Text(folderTitle(current, graph.root), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    modifier = Modifier.padding(start = if (stack.size > 1) 8.dp else 0.dp))
            }
            val folders = sortNodes(children.orEmpty(), SortBy.NAME, SortOrder.ASC, true)
            if (children != null && folders.isEmpty()) Text("No folders inside.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                folders.forEach { child ->
                    Row(Modifier.fillMaxWidth().clickable { stack.add(File(child.path)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FileIcon(child.name, null, true); Text(child.name, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            GlassButton("Use \"${folderTitle(current, graph.root)}\"", { onPick(current) }, Modifier.fillMaxWidth(), prominent = true)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** A folder's name for titles: "Internal storage" for the root itself. */
fun folderTitle(dir: File, root: File): String = if (dir.canonicalPath == root.canonicalPath) "Internal storage" else dir.name
