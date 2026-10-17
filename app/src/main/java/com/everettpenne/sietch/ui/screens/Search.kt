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
import com.everettpenne.sietch.data.SearchHit
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.sietch.util.FolderStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Search every granted folder at once, by name. Results open their containing folder or, for files, the right app. */
@Composable
fun SearchScreen(nav: Navigator, onBack: () -> Unit, showBack: Boolean = false) {
    val context = LocalContext.current
    val graph = context.graph
    val locations by graph.locations.items.collectAsState()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchHit>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query, locations) {
        if (query.isBlank()) { results = null; searching = false; return@LaunchedEffect }
        searching = true
        delay(280)
        val q = query
        val showHidden = graph.settings.showHidden.value
        results = withContext(Dispatchers.IO) {
            val out = ArrayList<SearchHit>()
            for (loc in locations) {
                out += graph.fileSystem.search(graph.fileSystem.rootOf(loc.uri), q, showHidden)
                if (out.size >= 400) break
            }
            out
        }
        searching = false
    }

    fun openHit(hit: SearchHit) {
        val node = hit.node
        if (node.isDirectory) {
            graph.recents.record(node.name, node.folder.treeUri, node.documentId, isDirectory = true)
            nav.openFolder(listOf(FolderStep(node.folder.treeUri, node.documentId, node.name)))
        } else {
            graph.recents.record(node.name, node.folder.treeUri, node.documentId, isDirectory = false)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(graph.fileSystem.documentUri(node), node.mime ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(intent) }.onFailure { msg = "No app can open ${node.name}" }
        }
    }

    GlassScreen(bars = {
        Column(Modifier.align(Alignment.TopCenter)) {
            GlassTopBar("Search", onBack = if (showBack) onBack else null)
        }
    }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = TopBarHeight + 8.dp, bottom = bottomContentPadding(), start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                GlassTextField(query, { query = it }, "Search every folder", modifier = Modifier.fillMaxWidth())
            }
            if (locations.isEmpty()) item {
                EmptyState(Icons.Rounded.FolderOpen, "No folders to search", "Grant Sietch a folder from Places first.")
            } else when {
                query.isBlank() -> item { EmptyState(Icons.Rounded.Search, "Search your folders", "Matches by file or folder name.") }
                searching -> item { LoadingRow() }
                results.isNullOrEmpty() -> item { EmptyState(Icons.Rounded.SearchOff, "Nothing found", "No name contains \"$query\".") }
                else -> items(results.orEmpty(), key = { "${it.node.folder.treeUri}:${it.node.documentId}" }) { hit ->
                    FileRow(hit.node.name, hit.node.mime, hit.node.isDirectory, hit.node.size, hit.node.modified,
                        onClick = { openHit(hit) },
                        subtitle = hit.path.ifEmpty { "folder root" })
                }
            }
        }
        msg?.let { m ->
            Msg(m, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
            LaunchedEffect(m) { delay(2400); msg = null }
        }
    }
}
