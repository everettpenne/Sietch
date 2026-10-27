package com.everettpenne.sietch.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.everettpenne.sietch.data.Node
import com.everettpenne.sietch.data.category
import com.everettpenne.sietch.ui.theme.Sietch
import com.everettpenne.glass.*

/** The select indicator shown at the end of each row in selection mode: an empty ring, or a filled accent disc with a check. */
@Composable
fun SelectCircle(selected: Boolean) {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(22.dp).clip(CircleShape)
            .background(if (selected) Sietch.accent else Color.Transparent)
            .border(2.dp, if (selected) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f), CircleShape),
            contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Rounded.Check, "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/** The bar that replaces the toolbar while items are selected. [onMove] and [onCopy] are null where those make no sense (the recycle bin). */
@Composable
fun SelectionBar(modifier: Modifier, count: Int, onClear: () -> Unit, onShare: (() -> Unit)?, onCopy: (() -> Unit)?, onMove: (() -> Unit)?, onDelete: () -> Unit) {
    val ink = glassPalette().ink
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)
        .glassSurface(RoundedCornerShape(28.dp), blurRadius = 8f, lensHeight = 20f, lensAmount = 32f)
        .padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassCircleButton(onClear, size = 40.dp, description = "Clear selection") { Icon(Icons.Rounded.Close, null, tint = ink, modifier = Modifier.size(22.dp)) }
        Text(if (count == 1) "1 selected" else "$count selected", style = IosType.subheadline.copy(fontWeight = FontWeight.SemiBold), color = ink, modifier = Modifier.padding(horizontal = 6.dp))
        Spacer(Modifier.weight(1f))
        if (onShare != null) SelectionAction(Icons.Rounded.Share, "Share", ink, onShare)
        if (onCopy != null) SelectionAction(Icons.Rounded.FileCopy, "Copy", ink, onCopy)
        if (onMove != null) SelectionAction(Icons.Rounded.DriveFileMove, "Move", ink, onMove)
        SelectionAction(Icons.Rounded.Delete, "Delete", Sietch.red, onDelete)
    }
}

@Composable
private fun SelectionAction(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, description, tint = tint, modifier = Modifier.size(24.dp)) }
}

/** The "•••" on a row: its own actions, as a glass menu anchored to the button. */
@Composable
fun NodeMenu(
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
            if (!node.isDirectory) GlassMenuItem("Share", { open = false; onShare() }, icon = Icons.Rounded.Share)
            GlassMenuItem("Copy to…", { open = false; onCopy() }, icon = Icons.Rounded.FileCopy)
            GlassMenuItem("Move to…", { open = false; onMove() }, icon = Icons.Rounded.DriveFileMove)
            GlassMenuItem("Delete", { open = false; onDelete() }, icon = Icons.Rounded.Delete, destructive = true)
        }
    }
}

/**
 * One entry in the gallery: a square of the picture or the video's first frame (with a play mark), or, for folders and other files, the kind icon
 * with the name beneath. Selection works as in the list: long-press starts it, and a tick shows what is chosen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryTile(node: Node, thumbnail: String?, selected: Boolean, selecting: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.aspectRatio(1f).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
        .then(if (selected) Modifier.border(3.dp, Sietch.accent, shape) else Modifier)
        .combinedClickable(onClickLabel = node.name, onClick = onClick, onLongClick = onLongClick)) {
        if (thumbnail != null) {
            FileIcon(node.name, node.mime, false, Modifier.align(Alignment.Center))
            AsyncImage(thumbnail, node.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (node.category() == "video") Icon(Icons.Rounded.PlayCircle, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(34.dp))
        } else Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            FileIcon(node.name, node.mime, node.isDirectory)
            Spacer(Modifier.height(6.dp))
            Text(node.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
        if (selecting) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape)
            .background(if (selected) Sietch.accent else Color.Black.copy(alpha = .35f)).border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}
