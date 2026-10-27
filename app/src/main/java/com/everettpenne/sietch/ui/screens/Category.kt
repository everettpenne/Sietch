package com.everettpenne.sietch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everettpenne.sietch.data.*
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.glass.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Every file of one kind, newest first, wherever it is on the phone. Images and videos open as a thumbnail grid. */
@Composable
fun CategoryScreen(category: Category, nav: Navigator, onBack: () -> Unit) {
    val graph = LocalContext.current.graph
    val access by rememberAccess()
    val available = access.allFiles || (!category.needsAllFiles && access.media)
    val sortBy by graph.settings.sortBy.collectAsState()
    val sortOrder by graph.settings.sortOrder.collectAsState()
    var nodes by remember { mutableStateOf<List<Node>?>(null) }
    var nonce by remember { mutableIntStateOf(0) }
    var gallery by remember { mutableStateOf(category == Category.IMAGES || category == Category.VIDEOS) }
    var sortMenu by remember { mutableStateOf(false) }
    // The category screens sort by date by default (newest first), and by the user's choice once they pick one here.
    var custom by remember { mutableStateOf<Pair<SortBy, SortOrder>?>(null) }
    LaunchedEffect(category, access, nonce) {
        if (!available) { nodes = emptyList(); return@LaunchedEffect }
        nodes = withContext(Dispatchers.IO) { graph.media.list(category) }
    }
    val shown = remember(nodes, custom) { nodes?.let { n -> custom?.let { (by, order) -> sortNodes(n, by, order, false) } ?: n } }
    val list = rememberListState()
    val ops = rememberFileOps()

    GlassScreen(bars = {
        Column(Modifier.align(Alignment.TopCenter)) {
            GlassCenteredTopBar(category.label, onBack, subtitle = shown?.takeIf { available }?.let { "${it.size} ${if (it.size == 1) "file" else "files"}" }, trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    GlassIconButton(if (gallery) Icons.Rounded.ViewList else Icons.Rounded.GridView, if (gallery) "Show as list" else "Show as gallery", { gallery = !gallery })
                    GlassIconButton(Icons.Rounded.Sort, "Sort", { sortMenu = true })
                }
            })
        }
        val topInset = (LocalGlassBarInsets.current?.top ?: TopBarHeight).coerceAtLeast(TopBarHeight)
        Box(Modifier.align(Alignment.TopEnd).padding(top = topInset - 2.dp, end = 16.dp)) {
            GlassDropdownMenu(sortMenu, { sortMenu = false }) {
                GlassMenuItem("Newest first", { custom = null; sortMenu = false }, icon = Icons.Rounded.Schedule, selected = custom == null)
                listOf(SortBy.NAME, SortBy.SIZE, SortBy.TYPE).forEach { s ->
                    GlassMenuItem(sortLabel(s), { custom = s to SortOrder.ASC; sortMenu = false }, icon = Icons.Rounded.Sort, selected = custom?.first == s)
                }
                GlassMenuItem("Reverse", { custom = (custom?.first ?: SortBy.MODIFIED) to (if (custom?.second == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC); sortMenu = false }, icon = Icons.Rounded.SwapVert)
            }
        }
        ListSelectionBar(list, shown.orEmpty(), ops)
    }) { contentPadding ->
        when {
            !available -> Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding() + 16.dp).padding(horizontal = 16.dp)) { AccessPrompt(access, needAllFiles = category.needsAllFiles, onGranted = { nonce++ }) }
            shown == null -> Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding()), contentAlignment = Alignment.TopCenter) { LoadingRow() }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState(category.icon(), "No ${category.label.lowercase()}", "Nothing in this category yet.") }
            else -> FileListBody(shown, list, ops, contentPadding, gallery = gallery, subtitle = { n -> "${com.everettpenne.sietch.util.formatBytes(n.size)}  ·  ${n.path.substringBeforeLast('/').removePrefix(graph.root.path).trimStart('/').ifEmpty { "Internal storage" }}" })
        }
        FileOpsHost(ops)
    }
    ListDialogs(list, ops, null, onChanged = { nonce++ })
}
