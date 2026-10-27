package com.everettpenne.sietch.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.everettpenne.sietch.data.Node
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.util.opSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * What a screen can do to files: open, share, copy, move, rename, make a folder, delete. One of these per screen, so Browse, a category, Recents
 * and Search all behave the same way and say the same things. [busy] and [msg] drive the progress overlay and the toast pill ([FileOpsHost]).
 */
@Stable
class FileOps(private val context: Context, private val scope: CoroutineScope, private val opener: Opener) {
    private val graph = context.graph
    var busy by mutableStateOf<String?>(null)
    var msg by mutableStateOf<String?>(null)

    /** Tells the media library about changed paths, so categories, Recents and Search follow what just happened. */
    fun rescan(paths: List<String>) {
        if (paths.isEmpty()) return
        runCatching { MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null) }
    }

    fun open(node: Node, pictures: List<String>? = null) {
        if (!opener.open(File(node.path), node.mime, pictures)) msg = "No app can open ${node.name}"
    }

    fun openWith(node: Node) {
        if (!opener.openWith(File(node.path), node.mime)) msg = "No app can open ${node.name}"
    }

    fun share(nodes: List<Node>) {
        val files = nodes.filter { !it.isDirectory }
        if (files.isEmpty()) { msg = "Folders can't be shared"; return }
        val uris = files.map { shareUri(context, File(it.path)) }
        val mime = files.map { it.mime ?: "*/*" }.distinct().singleOrNull() ?: "*/*"
        val intent = if (uris.size == 1) Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uris[0])
        else Intent(Intent.ACTION_SEND_MULTIPLE).setType(mime).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { context.startActivity(Intent.createChooser(intent, "Share")) }.onFailure { msg = "Couldn't share" }
        if (files.size < nodes.size) msg = "Folders were left out"
    }

    private fun run(label: String, summary: (ok: Int, total: Int) -> String, nodes: List<Node>, onDone: () -> Unit, work: (Node) -> Pair<Boolean, List<String>>) {
        if (busy != null) return
        busy = "$label…"
        scope.launch {
            var ok = 0; var done = 0
            val touched = ArrayList<String>()
            withContext(Dispatchers.IO) {
                for (n in nodes) {
                    val (success, paths) = work(n)
                    if (success) { ok++; touched += paths }
                    done++
                    if (nodes.size > 1) busy = "$label $done of ${nodes.size}…"
                }
            }
            rescan(touched)
            busy = null
            msg = summary(ok, nodes.size)
            onDone()
        }
    }

    fun copyMove(nodes: List<Node>, move: Boolean, target: File, onDone: () -> Unit) =
        run(if (move) "Moving" else "Copying", { ok, total -> opSummary(if (move) "Moved" else "Copied", ok, total) }, nodes, onDone) { n ->
            val src = File(n.path)
            val result = if (move) graph.fs.move(src, target) else graph.fs.copy(src, target)
            (result != null) to (listOfNotNull(result?.path) + if (move) listOf(n.path) else emptyList())
        }

    /** Moves to the recycle bin when it is on, otherwise removes for good. The caller has already asked the user. */
    fun delete(nodes: List<Node>, onDone: () -> Unit) {
        val bin = graph.settings.useTrash.value
        run("Deleting", { ok, total ->
            if (bin && ok == total && total > 0) "Moved ${if (total == 1) "1 item" else "$total items"} to the recycle bin" else opSummary("Deleted", ok, total)
        }, nodes, onDone) { n ->
            val f = File(n.path)
            val ok = if (bin) graph.trash.trash(f) else graph.fs.deleteRecursively(f)
            ok to listOf(n.path)
        }
    }

    fun rename(node: Node, newName: String, onDone: () -> Unit) {
        scope.launch {
            val f = File(node.path)
            val ok = withContext(Dispatchers.IO) { graph.fs.rename(f, newName) }
            if (ok) { rescan(listOf(node.path, File(f.parentFile, newName).path)); onDone() } else msg = "Couldn't rename \"${node.name}\""
        }
    }

    fun createFolder(parent: File, name: String, onDone: () -> Unit) {
        scope.launch {
            val made = withContext(Dispatchers.IO) { graph.fs.createFolder(parent, name) }
            if (made == null) msg = "Couldn't create \"$name\"" else onDone()
        }
    }
}

@Composable
fun rememberFileOps(): FileOps {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val opener = rememberOpener()
    return remember(context, scope, opener) { FileOps(context, scope, opener) }
}

/** Draws a [FileOps]' progress overlay and toast pill. Place inside the screen's content box. */
@Composable
fun androidx.compose.foundation.layout.BoxScope.FileOpsHost(ops: FileOps) {
    ops.msg?.let { m ->
        Msg(m, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
        LaunchedEffect(m) { delay(2400); if (ops.msg == m) ops.msg = null }
    }
    ops.busy?.let { BusyOverlay(it) }
}
