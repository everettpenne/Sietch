package com.everettpenne.sietch.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.everettpenne.glass.*
import com.everettpenne.sietch.data.Location
import com.everettpenne.sietch.graph
import com.everettpenne.sietch.ui.components.*
import com.everettpenne.sietch.ui.theme.Sietch
import com.everettpenne.sietch.util.FolderStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** An ACTION_OPEN_DOCUMENT_TREE that also returns the flags the provider actually granted, so only those are persisted. */
private val OpenTree = object : ActivityResultContract<Unit, Pair<Uri, Int>?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<Uri, Int>? {
        val uri = intent?.data ?: return null
        return uri to (intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
    }
}

/** Sietch's home: the folders you have handed it, with a way to add another. Tapping one opens it. */
@Composable
fun PlacesScreen(nav: Navigator) {
    val context = LocalContext.current
    val graph = context.graph
    val locations by graph.locations.items.collectAsState()
    val scope = rememberCoroutineScope()

    var renaming by remember { mutableStateOf<Location?>(null) }
    var removing by remember { mutableStateOf<Location?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }

    val addLocation = rememberLauncherForActivityResult(OpenTree) { result ->
        if (result == null) return@rememberLauncherForActivityResult
        val (uri, flags) = result
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        val name = graph.fileSystem.treeName(uri.toString())
        graph.locations.add(name, uri.toString())
        graph.recents.record(name, uri.toString(), graph.fileSystem.rootOf(uri.toString()).documentId, isDirectory = true)
        msg = "Added $name"
    }

    GlassScreen(bars = {
        Column(Modifier.align(Alignment.TopCenter)) {
            GlassTopBar("Sietch", subtitle = "Your folders, only the ones you give it", actions = {
                GlassIconButton(Icons.Rounded.Settings, "Settings", nav::openSettings)
                GlassIconButton(Icons.Rounded.CreateNewFolder, "Add a folder", { addLocation.launch(Unit) })
            })
        }
    }) { contentPadding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = contentPadding.calculateTopPadding() + 16.dp, bottom = contentPadding.calculateBottomPadding(), start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (locations.isEmpty()) item {
                EmptyState(Icons.Rounded.FolderOpen, "No folders yet", "Tap the folder button above to grant Sietch a folder. It asks for nothing else.")
            }
            items(locations, key = { it.uri }) { location -> LocationCard(location, onOpen = {
                val root = graph.fileSystem.rootOf(location.uri)
                graph.recents.record(location.name, location.uri, root.documentId, isDirectory = true)
                nav.openFolder(listOf(FolderStep(location.uri, root.documentId, location.name)))
            }, onRename = { renaming = location }, onRemove = { removing = location }) }
            item {
                GlassButton("Add a folder", { addLocation.launch(Unit) }, Modifier.fillMaxWidth(), prominent = false)
                Text("Sietch reads nothing until you grant it a folder. Grants are stored on this phone only.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
        msg?.let { m ->
            Msg(m, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
            LaunchedEffect(m) { delay(2400); msg = null }
        }
    }

    renaming?.let { location ->
        RenameLocationSheet(location, onDismiss = { renaming = null }, onSave = { newName ->
            graph.locations.rename(location.uri, newName); renaming = null
        })
    }
    removing?.let { location ->
        GlassAlert("Remove this folder?", "Sietch forgets its access to ${location.name}. Your files are untouched.",
            confirmLabel = "Remove", destructive = true, onConfirm = {
                runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(location.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
                graph.locations.remove(location.uri); removing = null
            }, onDismiss = { removing = null })
    }
}

@Composable
private fun LocationCard(location: Location, onOpen: () -> Unit, onRename: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val ink = glassPalette().ink
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
        .glassSurface(RoundedCornerShape(20.dp), blurRadius = 6f, lensHeight = 16f, lensAmount = 28f)
        .clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FileIcon(location.name, null, isDirectory = true)
        Column(Modifier.weight(1f)) {
            Text(location.name, style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 1)
            Text("Open", style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = .55f), maxLines = 1)
        }
        Box(Modifier.size(44.dp).clickable { menu = true }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MoreHoriz, "More for ${location.name}", tint = ink.copy(alpha = .8f))
        }
        GlassDropdownMenu(menu, { menu = false }) {
            GlassMenuItem("Rename", { menu = false; onRename() }, icon = Icons.Rounded.Edit)
            GlassMenuItem("Remove access", { menu = false; onRemove() }, icon = Icons.Rounded.LinkOff, destructive = true)
        }
    }
}

@Composable
private fun RenameLocationSheet(location: Location, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(location.name) }
    GlassSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Rename folder", style = MaterialTheme.typography.titleLarge)
            GlassTextField(name, { name = it }, "Folder name", leading = Icons.Rounded.DriveFileRenameOutline, imeAction = ImeAction.Done, autoFocus = true,
                onSubmit = { if (name.isNotBlank()) onSave(name.trim()) })
            GlassButton("Save", { onSave(name.trim()) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank(), prominent = true)
            Spacer(Modifier.height(8.dp))
        }
    }
}
