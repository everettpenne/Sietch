package com.everettpenne.sietch.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everettpenne.glass.*
import com.everettpenne.media.LocalImageViewer
import com.everettpenne.sietch.data.*
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.sietch.ui.theme.Sietch
import com.everettpenne.sietch.util.FolderStep
import com.everettpenne.sietch.util.RouteCodec
import com.everettpenne.sietch.util.formatBytes
import com.everettpenne.sietch.util.formatRelative
import com.everettpenne.sietch.util.opSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface BrowseState {
    data object Loading : BrowseState
    data class Ready(val nodes: List<Node>) : BrowseState
    data class Error(val message: String) : BrowseState
}

/** The pushed-screen bar for a folder: a back button, the breadcrumb (ancestors tappable, the current folder last) and the row actions. */
@Composable
private fun BrowseTopBar(steps: List<FolderStep>, onBack: () -> Unit, onJump: (Int) -> Unit, trailing: @Composable RowScope.() -> Unit) {
    val ink = glassPalette().ink
    Box(Modifier.fillMaxWidth().progressiveBlur(fromTop = true, tint = MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                steps.forEachIndexed { i, step ->
                    if (i > 0) Text("›", style = IosType.headline, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val last = i == steps.lastIndex
                    Text(step.name, style = if (last) IosType.headline else IosType.subheadline,
                        color = if (last) ink else Sietch.accent, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = if (last) Modifier else Modifier.clickable { onJump(i) })
                }
            }
            trailing()
        }
    }
}

/** The file list: open, select several, and act on them — copy, move, rename, share, delete, make a folder. */
@Composable
fun BrowseScreen(token: String, nav: Navigator, onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val steps = remember(token) { RouteCodec.decodePath(token).orEmpty() }
    val current = steps.lastOrNull()
    val folder = remember(token) { current?.let { Folder(it.treeUri, it.documentId) } }
    val title = remember(token) { current?.name ?: "Folder" }

    if (folder == null) {
        GlassScreen(bars = { GlassCenteredTopBar("Folder unavailable", onBack, Modifier.align(Alignment.TopCenter)) }) {
            Box(Modifier.fillMaxSize()) { EmptyState(Icons.Rounded.ErrorOutline, "Folder unavailable", "Its access may have been revoked.") }
        }
        return
    }

    val sortBy by graph.settings.sortBy.collectAsState()
    val sortOrder by graph.settings.sortOrder.collectAsState()
    val foldersFirst by graph.settings.foldersFirst.collectAsState()
    val showHidden by graph.settings.showHidden.collectAsState()

    var nonce by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<BrowseState>(BrowseState.Loading) }
    LaunchedEffect(folder, nonce) {
        state = BrowseState.Loading
        state = try {
            withContext(Dispatchers.IO) { BrowseState.Ready(graph.fileSystem.list(folder)) }
        } catch (e: Exception) { BrowseState.Error(e.message ?: "The folder did not answer.") }
    }

    val visible = remember(state, sortBy, sortOrder, foldersFirst, showHidden) {
        val all = (state as? BrowseState.Ready)?.nodes ?: emptyList()
        val filtered = if (showHidden) all else all.filter { !it.name.startsWith(".") }
        sortNodes(filtered, sortBy, sortOrder, foldersFirst)
    }

    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Node?>(null) }
    var details by remember { mutableStateOf<Node?>(null) }
    var deleting by remember { mutableStateOf<List<Node>?>(null) }
    var copyMove by remember { mutableStateOf<Pair<List<Node>, Boolean>?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val openImages = LocalImageViewer.current

    fun reload() { nonce++ }
    fun toggle(id: String) { selected = if (id in selected) selected - id else selected + id }

    fun openWith(node: Node) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(graph.fileSystem.documentUri(node), node.mime ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (runCatching { context.startActivity(intent) }.isFailure) msg = "No app can open ${node.name}"
    }

    fun share(nodes: List<Node>) {
        val uris = nodes.map { graph.fileSystem.documentUri(it) }
        val mime = nodes.firstOrNull()?.mime ?: "*/*"
        val intent = if (uris.size == 1) Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uris[0])
        else Intent(Intent.ACTION_SEND_MULTIPLE).setType(mime).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { context.startActivity(Intent.createChooser(intent, "Share")) }
    }

    fun doCopyMove(nodes: List<Node>, move: Boolean, target: Folder) {
        if (busy != null) return
        val gerund = if (move) "Moving" else "Copying"
        val verb = if (move) "Moved" else "Copied"
        busy = "$gerund…"
        scope.launch {
            var ok = 0
            var done = 0
            withContext(Dispatchers.IO) {
                for (n in nodes) {
                    if (if (move) graph.fileSystem.move(n, target) else graph.fileSystem.copy(n, target)) ok++
                    done++
                    if (nodes.size > 1) busy = "$gerund $done of ${nodes.size}…"
                }
            }
            busy = null; copyMove = null; selected = emptySet()
            msg = opSummary(verb, ok, nodes.size)
            reload()
        }
    }

    fun doDelete(nodes: List<Node>) {
        if (busy != null) return
        busy = "Deleting…"
        scope.launch {
            var ok = 0
            var done = 0
            withContext(Dispatchers.IO) {
                for (n in nodes) {
                    if (graph.fileSystem.delete(n)) ok++
                    done++
                    if (nodes.size > 1) busy = "Deleting $done of ${nodes.size}…"
                }
            }
            busy = null; deleting = null; selected = emptySet()
            msg = opSummary("Deleted", ok, nodes.size)
            reload()
        }
    }

    fun open(node: Node) {
        if (node.isDirectory) {
            graph.recents.record(node.name, folder.treeUri, node.documentId, isDirectory = true)
            nav.openFolder(steps + FolderStep(folder.treeUri, node.documentId, node.name))
        } else {
            graph.recents.record(node.name, folder.treeUri, node.documentId, isDirectory = false)
            if (node.mime?.startsWith("image/") == true) {
                val images = visible.filter { it.mime?.startsWith("image/") == true }
                val start = images.indexOfFirst { it.documentId == node.documentId }.coerceAtLeast(0)
                openImages(images.map { graph.fileSystem.documentUri(it).toString() }, start)
            } else openWith(node)
        }
    }

    GlassScreen(bars = {
        BrowseTopBar(steps, onBack, onJump = { i -> nav.backTo(steps.subList(0, i + 1)) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                GlassIconButton(Icons.Rounded.Search, "Search", nav::openSearch)
                GlassIconButton(Icons.Rounded.Sort, "Sort", { sortMenu = true })
                GlassIconButton(Icons.Rounded.MoreVert, "More", { moreMenu = true })
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(top = 120.dp, end = 16.dp)) {
            GlassDropdownMenu(sortMenu, { sortMenu = false }) {
                SortBy.entries.forEach { s -> GlassMenuItem(label(s), { graph.settings.setSortBy(s); sortMenu = false }, icon = Icons.Rounded.Sort, selected = s == sortBy) }
                GlassMenuItem("Ascending", { graph.settings.setSortOrder(SortOrder.ASC); sortMenu = false }, icon = Icons.Rounded.ArrowUpward, selected = sortOrder == SortOrder.ASC)
                GlassMenuItem("Descending", { graph.settings.setSortOrder(SortOrder.DESC); sortMenu = false }, icon = Icons.Rounded.ArrowDownward, selected = sortOrder == SortOrder.DESC)
            }
            GlassDropdownMenu(moreMenu, { moreMenu = false }) {
                GlassMenuItem("New folder", { moreMenu = false; newFolder = true }, icon = Icons.Rounded.CreateNewFolder)
                if ((state as? BrowseState.Ready)?.nodes?.isNotEmpty() == true) GlassMenuItem("Select all", { moreMenu = false; selected = visible.map { it.documentId }.toSet() }, icon = Icons.Rounded.SelectAll)
                GlassMenuItem(if (showHidden) "Hide hidden files" else "Show hidden files", { moreMenu = false; graph.settings.setShowHidden(!showHidden) }, icon = Icons.Rounded.Visibility, selected = showHidden)
            }
        }
        if (selected.isNotEmpty()) {
            SelectionBar(Modifier.align(Alignment.BottomCenter), selected.size, onClear = { selected = emptySet() },
                onShare = { share(visible.filter { it.documentId in selected }) },
                onCopy = { copyMove = visible.filter { it.documentId in selected } to false },
                onMove = { copyMove = visible.filter { it.documentId in selected } to true },
                onDelete = { deleting = visible.filter { it.documentId in selected } })
        }
    }) {
        when (val s = state) {
            is BrowseState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingRow() }
            is BrowseState.Error -> Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    EmptyState(Icons.Rounded.ErrorOutline, "Couldn't read this folder", s.message)
                    GlassButton("Try again", ::reload, prominent = false)
                }
            }
            is BrowseState.Ready -> if (visible.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Rounded.FolderOpen, "Nothing here", if (showHidden) "This folder is empty." else "This folder is empty, or its only entries are hidden.")
            } else LazyColumn(Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topContentPadding(), bottom = (if (selected.isNotEmpty()) 120.dp else 40.dp) + bottomContentPadding(), start = 12.dp, end = 12.dp)) {
                items(visible, key = { it.documentId }) { node ->
                    FileRow(node.name, node.mime, node.isDirectory, node.size, node.modified,
                        onClick = { if (selected.isNotEmpty()) toggle(node.documentId) else open(node) },
                        onLongClick = { if (selected.isEmpty()) selected = setOf(node.documentId) else toggle(node.documentId) },
                        selected = node.documentId in selected,
                        trailing = {
                            if (selected.isEmpty()) NodeMenu(node,
                                onOpen = { open(node) },
                                onRename = { renaming = node },
                                onDetails = { details = node },
                                onShare = { share(listOf(node)) },
                                onCopy = { copyMove = listOf(node) to false },
                                onMove = { copyMove = listOf(node) to true },
                                onDelete = { deleting = listOf(node) })
                        })
                }
            }
        }
        msg?.let { m ->
            Msg(m, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
            LaunchedEffect(m) { delay(2400); msg = null }
        }
    }

    if (newFolder) NewFolderSheet(title, onDismiss = { newFolder = false }, onCreate = { name ->
        scope.launch {
            val made = withContext(Dispatchers.IO) { graph.fileSystem.createFolder(folder, name) }
            newFolder = false
            if (made == null) msg = "Couldn't create \"$name\"" else reload()
        }
    })
    renaming?.let { node -> RenameSheet(node, onDismiss = { renaming = null }, onRename = { newName ->
        scope.launch {
            val ok = withContext(Dispatchers.IO) { graph.fileSystem.rename(node, newName) }
            renaming = null
            if (ok) reload() else msg = "Couldn't rename \"${node.name}\""
        }
    }) }
    details?.let { node -> DetailsSheet(node, steps.joinToString(" / ") { it.name }) { details = null } }
    deleting?.let { nodes -> GlassAlert("Delete ${if (nodes.size == 1) "\"${nodes[0].name}\"" else "${nodes.size} items"}?",
        "This removes ${if (nodes.size == 1) "it" else "them"} permanently. There is no recycle bin.",
        confirmLabel = "Delete", destructive = true, onConfirm = { deleting = null; doDelete(nodes) }, onDismiss = { deleting = null }) }
    copyMove?.let { (nodes, move) -> FolderPicker(onPick = { copyMove = null; doCopyMove(nodes, move, it) }, onDismiss = { copyMove = null }) }
    busy?.let { BusyOverlay(it) }
}

private fun label(s: SortBy) = when (s) {
    SortBy.NAME -> "Name"; SortBy.SIZE -> "Size"; SortBy.MODIFIED -> "Modified"; SortBy.TYPE -> "Type"
}

/** The "•••" on a row: its own actions, as a glass menu anchored to the button. */
@Composable
private fun NodeMenu(
    node: Node, onOpen: () -> Unit, onRename: () -> Unit, onDetails: () -> Unit, onShare: () -> Unit, onCopy: () -> Unit, onMove: () -> Unit, onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(Modifier.size(44.dp).clickable { open = true }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MoreHoriz, "More for ${node.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        GlassDropdownMenu(open, { open = false }) {
            GlassMenuItem(if (node.isDirectory) "Open" else "Open with…", { open = false; onOpen() }, icon = Icons.Rounded.OpenInNew)
            GlassMenuItem("Rename", { open = false; onRename() }, icon = Icons.Rounded.Edit)
            GlassMenuItem("Details", { open = false; onDetails() }, icon = Icons.Rounded.Info)
            GlassMenuItem("Share", { open = false; onShare() }, icon = Icons.Rounded.Share)
            GlassMenuItem("Copy to…", { open = false; onCopy() }, icon = Icons.Rounded.FileCopy)
            GlassMenuItem("Move to…", { open = false; onMove() }, icon = Icons.Rounded.DriveFileMove)
            GlassMenuItem("Delete", { open = false; onDelete() }, icon = Icons.Rounded.Delete, destructive = true)
        }
    }
}

@Composable
private fun SelectionBar(modifier: Modifier, count: Int, onClear: () -> Unit, onShare: () -> Unit, onCopy: () -> Unit, onMove: () -> Unit, onDelete: () -> Unit) {
    val ink = glassPalette().ink
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)
        .glassSurface(RoundedCornerShape(28.dp), blurRadius = 8f, lensHeight = 20f, lensAmount = 32f)
        .padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassCircleButton(onClear, size = 40.dp, description = "Clear selection") { Icon(Icons.Rounded.Close, null, tint = ink, modifier = Modifier.size(22.dp)) }
        Text(if (count == 1) "1 selected" else "$count selected", style = IosType.subheadline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = ink, modifier = Modifier.padding(horizontal = 6.dp))
        Spacer(Modifier.weight(1f))
        SelectionAction(Icons.Rounded.Share, "Share", ink, onShare)
        SelectionAction(Icons.Rounded.FileCopy, "Copy", ink, onCopy)
        SelectionAction(Icons.Rounded.DriveFileMove, "Move", ink, onMove)
        SelectionAction(Icons.Rounded.Delete, "Delete", Sietch.red, onDelete)
    }
}

@Composable
private fun SelectionAction(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, description, tint = tint, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun NewFolderSheet(parentName: String, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    GlassSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New folder", style = MaterialTheme.typography.titleLarge)
            Text("Created inside $parentName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GlassTextField(name, { name = it }, "Folder name", leading = Icons.Rounded.CreateNewFolder)
            GlassButton("Create", { onCreate(name.trim()) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank(), prominent = true)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RenameSheet(node: Node, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(node.name) }
    GlassSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Rename", style = MaterialTheme.typography.titleLarge)
            GlassTextField(name, { name = it }, "Name", leading = Icons.Rounded.DriveFileRenameOutline)
            GlassButton("Save", { onRename(name.trim()) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && name != node.name, prominent = true)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailsSheet(node: Node, locationName: String, onDismiss: () -> Unit) {
    GlassSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FileIcon(node.name, node.mime, node.isDirectory)
                Column {
                    Text(node.name, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                    Text(if (node.isDirectory) "Folder" else (node.mime ?: "File"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row { DetailLabel("Kind"); Text(node.category().replaceFirstChar { it.uppercase() }, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge) }
            if (!node.isDirectory) Row { DetailLabel("Size"); Text(formatBytes(node.size), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge) }
            if (node.modified > 0) Row { DetailLabel("Modified"); Text(formatRelative(node.modified), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge) }
            Row { DetailLabel("In"); Text(locationName, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            GlassButton("Done", onDismiss, Modifier.fillMaxWidth(), prominent = true)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailLabel(text: String) = Text(text, Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

/** A folder chooser for copy/move: the granted folders, then navigate down. "Use this folder" uses the folder on screen. */
@Composable
private fun FolderPicker(onPick: (Folder) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val locations by graph.locations.items.collectAsState()
    val stack = remember { mutableStateListOf<Pair<Folder, String>>() }
    val current = stack.lastOrNull()?.first
    var children by remember { mutableStateOf<List<Node>?>(null) }
    LaunchedEffect(current) {
        children = if (current == null) null else runCatching { withContext(Dispatchers.IO) { graph.fileSystem.list(current) } }.getOrNull()
    }

    GlassSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Choose a folder", style = MaterialTheme.typography.titleLarge)
            if (current != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stack.size > 1) GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Up", { stack.removeAt(stack.lastIndex) }, size = 36)
                    else Box(Modifier.size(36.dp).clickable { stack.clear() }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.FolderSpecial, "Back to folders", tint = Sietch.accent) }
                    Text(stack.joinToString(" / ") { it.second }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (current == null) {
                if (locations.isEmpty()) Text("No folders granted yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                locations.forEach { loc ->
                    Row(Modifier.fillMaxWidth().clickable { stack.add(graph.fileSystem.rootOf(loc.uri) to loc.name) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FileIcon(loc.name, null, true); Text(loc.name, style = MaterialTheme.typography.titleSmall)
                    }
                }
            } else {
                val folders = (children ?: emptyList()).filter { it.isDirectory }.let { sortNodes(it, SortBy.NAME, SortOrder.ASC, true) }
                if (folders.isEmpty()) Text("No folders inside.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                folders.forEach { child ->
                    Row(Modifier.fillMaxWidth().clickable { stack.add(Folder(current.treeUri, child.documentId) to child.name) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FileIcon(child.name, child.mime, true); Text(child.name, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            GlassButton(if (current == null) "Choose a folder" else "Use \"${stack.last().second}\"", { if (current != null) onPick(current) }, Modifier.fillMaxWidth(), enabled = current != null, prominent = true)
            Spacer(Modifier.height(8.dp))
        }
    }
}
