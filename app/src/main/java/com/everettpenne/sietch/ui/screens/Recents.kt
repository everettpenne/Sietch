package com.everettpenne.sietch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.everettpenne.sietch.data.Node
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.glass.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The newest files on the phone, from the media library's index, so it is instant and needs no scan of its own. */
@Composable
fun RecentsScreen(nav: Navigator) {
    val graph = LocalContext.current.graph
    val access by rememberAccess()
    var nodes by remember { mutableStateOf<List<Node>?>(null) }
    var nonce by remember { mutableIntStateOf(0) }
    LaunchedEffect(access, nonce) { nodes = if (access.any) withContext(Dispatchers.IO) { graph.media.recent(120) } else emptyList() }
    val list = rememberListState()
    val ops = rememberFileOps()
    GlassScreen(bars = {
        Column(Modifier.align(Alignment.TopCenter)) { GlassTopBar("Recents", subtitle = "Newest files first") }
        ListSelectionBar(list, nodes.orEmpty(), ops)
    }) { contentPadding ->
        val n = nodes
        when {
            !access.any -> Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding() + 16.dp).padding(horizontal = 16.dp)) { AccessPrompt(access, needAllFiles = false, onGranted = { nonce++ }) }
            n == null -> Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding()), contentAlignment = Alignment.TopCenter) { LoadingRow() }
            n.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState(Icons.Rounded.History, "No recent files", "New photos, downloads and documents show up here.") }
            else -> FileListBody(n, list, ops, contentPadding, subtitle = { x -> x.path.substringBeforeLast('/').removePrefix(graph.root.path).trimStart('/').ifEmpty { "Internal storage" } })
        }
        FileOpsHost(ops)
    }
    ListDialogs(list, ops, null, onChanged = { nonce++ })
}
