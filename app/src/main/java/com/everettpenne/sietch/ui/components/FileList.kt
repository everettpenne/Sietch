package com.everettpenne.sietch.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everettpenne.sietch.data.Node
import com.everettpenne.sietch.graph
import com.everettpenne.glass.GlassAlert
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** What is selected and which dialog is open on a list screen. One per screen; the screen's bars read it, [FileListBody] and [ListDialogs] drive it. */
@Stable
class ListState {
    var selected by mutableStateOf<Set<String>>(emptySet())
    var renaming by mutableStateOf<Node?>(null)
    var details by mutableStateOf<Node?>(null)
    var deleting by mutableStateOf<List<Node>?>(null)
    var copyMove by mutableStateOf<Pair<List<Node>, Boolean>?>(null)
    var newFolder by mutableStateOf(false)

    val selecting: Boolean get() = selected.isNotEmpty()
    fun toggle(path: String) { selected = if (path in selected) selected - path else selected + path }
    fun clear() { selected = emptySet() }
    fun chosen(nodes: List<Node>): List<Node> = nodes.filter { it.path in selected }
}

@Composable
fun rememberListState() = remember { ListState() }

/** The bottom bar for a list screen while something is selected. */
@Composable
fun androidx.compose.foundation.layout.BoxScope.ListSelectionBar(state: ListState, nodes: List<Node>, ops: FileOps) {
    if (!state.selecting) return
    SelectionBar(Modifier.align(androidx.compose.ui.Alignment.BottomCenter), state.selected.size, onClear = state::clear,
        onShare = { ops.share(state.chosen(nodes)) },
        onCopy = { state.copyMove = state.chosen(nodes) to false },
        onMove = { state.copyMove = state.chosen(nodes) to true },
        onDelete = { state.deleting = state.chosen(nodes) })
}

/** The files as a list or a thumbnail grid. Tapping selects when something is already selected; a long-press starts selecting. */
@Composable
fun FileListBody(
    nodes: List<Node>,
    state: ListState,
    ops: FileOps,
    contentPadding: PaddingValues,
    gallery: Boolean = false,
    /** A second line for a row (a path, a date); null for the default size and date. */
    subtitle: ((Node) -> String?)? = null,
    onOpenFolder: (Node) -> Unit = {},
) {
    val graph = LocalContext.current.graph
    fun thumb(n: Node): String? = if (!n.isDirectory && hasThumbnail(n.name, n.mime)) "file://${n.path}" else null
    val bottom = (if (state.selecting) 120.dp else 40.dp) + contentPadding.calculateBottomPadding()
    fun open(n: Node) {
        if (n.isDirectory) onOpenFolder(n)
        else ops.open(n, nodes.filter { !it.isDirectory && com.everettpenne.sietch.data.inAppViewer(it.name, it.mime) == com.everettpenne.sietch.data.InApp.IMAGE }.map { it.path })
    }
    if (gallery) LazyVerticalGrid(GridCells.Adaptive(108.dp), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = contentPadding.calculateTopPadding() + 4.dp, bottom = bottom, start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        gridItems(nodes, key = { it.path }) { node ->
            GalleryTile(node, thumb(node), selected = node.path in state.selected, selecting = state.selecting,
                onClick = { if (state.selecting) state.toggle(node.path) else open(node) },
                onLongClick = { if (!state.selecting) state.selected = setOf(node.path) else state.toggle(node.path) })
        }
    } else LazyColumn(Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = contentPadding.calculateTopPadding(), bottom = bottom, start = 12.dp, end = 12.dp)) {
        items(nodes, key = { it.path }) { node ->
            FileRow(node.name, node.mime, node.isDirectory, node.size, node.modified,
                onClick = { if (state.selecting) state.toggle(node.path) else open(node) },
                onLongClick = { if (!state.selecting) state.selected = setOf(node.path) else state.toggle(node.path) },
                selected = node.path in state.selected,
                subtitle = subtitle?.invoke(node),
                thumbnailUri = thumb(node),
                trailing = {
                    if (state.selecting) SelectCircle(node.path in state.selected)
                    else NodeMenu(node,
                        onOpen = { if (node.isDirectory) onOpenFolder(node) else ops.openWith(node) },
                        onRename = { state.renaming = node },
                        onDetails = { state.details = node },
                        onShare = { ops.share(listOf(node)) },
                        onCopy = { state.copyMove = listOf(node) to false },
                        onMove = { state.copyMove = listOf(node) to true },
                        onDelete = { state.deleting = listOf(node) })
                })
        }
    }
}

/**
 * The dialogs and sheets a list screen can open. [parentDir] is the folder the screen shows (null for category/search screens, which have no
 * "new folder"); [onChanged] is called after anything that changes the files so the screen reloads.
 */
@Composable
fun ListDialogs(state: ListState, ops: FileOps, parentDir: File?, onChanged: () -> Unit) {
    val graph = LocalContext.current.graph
    val useTrash by graph.settings.useTrash.collectAsState()
    if (state.newFolder && parentDir != null) NewFolderSheet(folderTitle(parentDir, graph.root), onDismiss = { state.newFolder = false }, onCreate = { name ->
        state.newFolder = false
        ops.createFolder(parentDir, name, onChanged)
    })
    state.renaming?.let { node -> RenameSheet(node, onDismiss = { state.renaming = null }, onRename = { newName ->
        state.renaming = null
        ops.rename(node, newName, onChanged)
    }) }
    state.details?.let { node -> DetailsSheet(node) { state.details = null } }
    state.deleting?.let { nodes ->
        val what = if (nodes.size == 1) "\"${nodes[0].name}\"" else "${nodes.size} items"
        if (useTrash) GlassAlert("Move $what to the recycle bin?", "You can restore ${if (nodes.size == 1) "it" else "them"} from the recycle bin for ${graph.settings.trashDays.value} days.",
            confirmLabel = "Move to bin", destructive = true, onConfirm = { state.deleting = null; state.clear(); ops.delete(nodes, onChanged) }, onDismiss = { state.deleting = null })
        else GlassAlert("Delete $what?", "This removes ${if (nodes.size == 1) "it" else "them"} permanently. The recycle bin is turned off.",
            confirmLabel = "Delete", destructive = true, onConfirm = { state.deleting = null; state.clear(); ops.delete(nodes, onChanged) }, onDismiss = { state.deleting = null })
    }
    state.copyMove?.let { (nodes, move) -> FolderPicker(onPick = { target -> state.copyMove = null; state.clear(); ops.copyMove(nodes, move, target, onChanged) }, onDismiss = { state.copyMove = null }) }
}
