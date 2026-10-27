package com.everettpenne.sietch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Search by file name across everything the media library has indexed. Folders are not part of that index, so they are not found here. */
@Composable
fun SearchScreen(nav: Navigator, onBack: () -> Unit, showBack: Boolean = false) {
    val graph = LocalContext.current.graph
    val access by rememberAccess()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Node>?>(null) }
    var searching by remember { mutableStateOf(false) }
    val list = rememberListState()
    val ops = rememberFileOps()
    var nonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(query, access, nonce) {
        if (query.isBlank()) { results = null; searching = false; return@LaunchedEffect }
        searching = true
        delay(250)
        results = withContext(Dispatchers.IO) { graph.media.search(query) }
        searching = false
    }

    GlassScreen(bars = {
        Column(Modifier.align(Alignment.TopCenter)) { GlassTopBar("Search", onBack = if (showBack) onBack else null) }
        ListSelectionBar(list, results.orEmpty(), ops)
    }) { contentPadding ->
        if (list.selecting && results != null) FileListBody(results.orEmpty(), list, ops, contentPadding, subtitle = { it.path.substringBeforeLast('/').removePrefix(graph.root.path).trimStart('/').ifEmpty { "Internal storage" } })
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding(), start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { GlassTextField(query, { query = it }, "Search your files", modifier = Modifier.fillMaxWidth()) }
            when {
                !access.any -> item { AccessPrompt(access, needAllFiles = false, onGranted = { nonce++ }) }
                query.isBlank() -> item { EmptyState(Icons.Rounded.Search, "Search your files", "Matches by file name.") }
                searching -> item { LoadingRow() }
                results.isNullOrEmpty() -> item { EmptyState(Icons.Rounded.SearchOff, "Nothing found", "No file name contains \"$query\".") }
                else -> {
                    val hits = results.orEmpty()
                    item { Text("${hits.size} ${if (hits.size == 1) "result" else "results"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp)) }
                    items(hits.size, key = { hits[it].path }) { i ->
                        val n = hits[i]
                        FileRow(n.name, n.mime, false, n.size, n.modified, onClick = { ops.open(n) }, onLongClick = { list.selected = setOf(n.path) },
                            subtitle = n.path.substringBeforeLast('/').removePrefix(graph.root.path).trimStart('/').ifEmpty { "Internal storage" },
                            thumbnailUri = if (hasThumbnail(n.name, n.mime)) "file://${n.path}" else null)
                    }
                }
            }
        }
        FileOpsHost(ops)
    }
    ListDialogs(list, ops, null, onChanged = { nonce++ })
}
