package com.everettpenne.sietch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everettpenne.sietch.data.*
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.sietch.ui.theme.Sietch
import com.everettpenne.glass.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface BrowseState {
    data object Loading : BrowseState
    data class Ready(val nodes: List<Node>) : BrowseState
    data class Error(val message: String) : BrowseState
}

/** The folders from the storage root down to [dir], root first. A path outside the root (it cannot happen from the UI) is shown as itself. */
fun breadcrumb(dir: File, root: File): List<File> {
    val chain = generateSequence(dir) { it.parentFile }.takeWhile { it.path.length >= root.path.length && it.path.startsWith(root.path) }.toList().reversed()
    return chain.ifEmpty { listOf(dir) }
}

@Composable
private fun BrowseTopBar(crumbs: List<File>, root: File, onBack: () -> Unit, onJump: (File) -> Unit, trailing: @Composable RowScope.() -> Unit) {
    val ink = glassPalette().ink
    Box(Modifier.fillMaxWidth().glassTopBar().progressiveBlur(fromTop = true, tint = MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState(Int.MAX_VALUE)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                crumbs.forEachIndexed { i, dir ->
                    if (i > 0) Text("›", style = IosType.headline, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val last = i == crumbs.lastIndex
                    Text(folderTitle(dir, root), style = if (last) IosType.headline else IosType.subheadline, color = if (last) ink else Sietch.accent, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = if (last) Modifier else Modifier.clickable { onJump(dir) })
                }
            }
            trailing()
        }
    }
}

/** The file list for one folder: open, select several, and act on them: copy, move, rename, share, delete (to the recycle bin), make a folder. */
@Composable
fun BrowseScreen(path: String, nav: Navigator, onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val dir = remember(path) { File(path) }
    val access by rememberAccess()
    val sortBy by graph.settings.sortBy.collectAsState()
    val sortOrder by graph.settings.sortOrder.collectAsState()
    val foldersFirst by graph.settings.foldersFirst.collectAsState()
    val showHidden by graph.settings.showHidden.collectAsState()
    val gallery by graph.settings.gallery.collectAsState()

    var nonce by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<BrowseState>(BrowseState.Loading) }
    LaunchedEffect(path, nonce, access.allFiles) {
        if (!access.allFiles) return@LaunchedEffect
        if (state !is BrowseState.Ready) state = BrowseState.Loading
        state = try { withContext(Dispatchers.IO) { BrowseState.Ready(graph.fs.list(dir)) } } catch (e: Exception) { BrowseState.Error(e.message ?: "The folder did not answer.") }
    }
    val visible = remember(state, sortBy, sortOrder, foldersFirst, showHidden) {
        val all = (state as? BrowseState.Ready)?.nodes ?: emptyList()
        sortNodes(if (showHidden) all else all.filter { !it.name.startsWith(".") }, sortBy, sortOrder, foldersFirst)
    }
    val list = rememberListState()
    val ops = rememberFileOps()
    var sortMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    val crumbs = remember(path) { breadcrumb(dir, graph.root) }
    fun reload() { nonce++ }

    GlassScreen(bars = {
        BrowseTopBar(crumbs, graph.root, onBack, onJump = { nav.backTo(it.path) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                GlassIconButton(Icons.Rounded.Search, "Search", nav::openSearch)
                GlassIconButton(if (gallery) Icons.Rounded.ViewList else Icons.Rounded.GridView, if (gallery) "Show as list" else "Show as gallery", { graph.settings.setGallery(!gallery) })
                GlassIconButton(Icons.Rounded.Sort, "Sort", { sortMenu = true })
                GlassIconButton(Icons.Rounded.MoreVert, "More", { moreMenu = true })
            }
        }
        val topInset = (LocalGlassBarInsets.current?.top ?: TopBarHeight).coerceAtLeast(TopBarHeight)
        Box(Modifier.align(Alignment.TopEnd).padding(top = topInset - 2.dp, end = 16.dp)) {
            GlassDropdownMenu(sortMenu, { sortMenu = false }) {
                SortBy.entries.forEach { s -> GlassMenuItem(sortLabel(s), { graph.settings.setSortBy(s); sortMenu = false }, icon = Icons.Rounded.Sort, selected = s == sortBy) }
                GlassMenuItem("Ascending", { graph.settings.setSortOrder(SortOrder.ASC); sortMenu = false }, icon = Icons.Rounded.ArrowUpward, selected = sortOrder == SortOrder.ASC)
                GlassMenuItem("Descending", { graph.settings.setSortOrder(SortOrder.DESC); sortMenu = false }, icon = Icons.Rounded.ArrowDownward, selected = sortOrder == SortOrder.DESC)
            }
            GlassDropdownMenu(moreMenu, { moreMenu = false }) {
                GlassMenuItem("New folder", { moreMenu = false; list.newFolder = true }, icon = Icons.Rounded.CreateNewFolder)
                if (visible.isNotEmpty()) GlassMenuItem("Select all", { moreMenu = false; list.selected = visible.map { it.path }.toSet() }, icon = Icons.Rounded.SelectAll)
                GlassMenuItem(if (showHidden) "Hide hidden files" else "Show hidden files", { moreMenu = false; graph.settings.setShowHidden(!showHidden) }, icon = Icons.Rounded.Visibility, selected = showHidden)
                GlassMenuItem("Recycle bin", { moreMenu = false; nav.openTrash() }, icon = Icons.Rounded.Delete)
            }
        }
        ListSelectionBar(list, visible, ops)
    }) { contentPadding ->
        if (!access.allFiles) Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding() + 16.dp).padding(horizontal = 16.dp)) {
            AccessPrompt(access, needAllFiles = true)
        } else when (val s = state) {
            is BrowseState.Loading -> Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding()), contentAlignment = Alignment.TopCenter) { LoadingRow() }
            is BrowseState.Error -> Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    EmptyState(Icons.Rounded.ErrorOutline, "Couldn't read this folder", s.message)
                    GlassButton("Try again", ::reload, prominent = false)
                }
            }
            is BrowseState.Ready -> if (visible.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val onlyHidden = !showHidden && s.nodes.isNotEmpty()
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EmptyState(Icons.Rounded.FolderOpen, "Nothing here", if (onlyHidden) "Everything in this folder is hidden." else "This folder is empty.")
                    if (onlyHidden) GlassButton("Show hidden files", { graph.settings.setShowHidden(true) }, prominent = false)
                }
            } else FileListBody(visible, list, ops, contentPadding, gallery = gallery, onOpenFolder = { nav.openFolder(it.path) })
        }
        FileOpsHost(ops)
    }
    ListDialogs(list, ops, dir, onChanged = ::reload)
}

fun sortLabel(s: SortBy) = when (s) { SortBy.NAME -> "Name"; SortBy.SIZE -> "Size"; SortBy.MODIFIED -> "Modified"; SortBy.TYPE -> "Type" }
