package com.everettpenne.sietch.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.everettpenne.glass.IosType
import com.everettpenne.glass.frostedGlass
import com.everettpenne.glass.glassPalette
import com.everettpenne.glass.glassSurface
import com.everettpenne.sietch.data.categoryOf
import com.everettpenne.sietch.ui.theme.Sietch
import com.everettpenne.sietch.util.FolderStep
import com.everettpenne.sietch.util.formatBytes
import com.everettpenne.sietch.util.formatRelative

/** Where a tap goes. Pure callbacks, so screens do not know about navigation. */
interface Navigator {
    /** Open a folder, given its full breadcrumb (root first, the folder itself last). */
    fun openFolder(steps: List<FolderStep>)
    /** Pop back to an ancestor folder already on the navigation stack, given its breadcrumb. */
    fun backTo(steps: List<FolderStep>)
    fun openSearch()
    fun openSettings()
}

@Composable
fun LoadingRow() = Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp) }

/** A quiet, self-dismissing glass pill for transient feedback ("Copied 3", "No app can open …"). */
@Composable
fun Msg(text: String, modifier: Modifier = Modifier) {
    val ink = glassPalette().ink
    Box(modifier.glassSurface(RoundedCornerShape(percent = 50), blurRadius = 8f, lensHeight = 10f, lensAmount = 20f, shadowed = true)) {
        Text(text, style = IosType.subheadline, color = ink, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
    }
}

/** A full-screen, blocking progress overlay for copy/move/delete: a dimmed page and a glass card with a spinner and the running count. */
@Composable
fun BusyOverlay(text: String) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = 40.dp)
            .glassSurface(RoundedCornerShape(28.dp), blurRadius = 8f, lensHeight = 16f, lensAmount = 32f, shadowed = true)
            .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 3.dp)
            Text(text, style = IosType.subheadline, color = glassPalette().ink, maxLines = 1)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** The icon and colour for a file, by its kind: the same quiet pastels the other glass apps use for their accents. */
@Composable
fun FileIcon(name: String, mime: String?, isDirectory: Boolean, modifier: Modifier = Modifier) {
    val (icon, color) = when (categoryOf(name, mime, isDirectory)) {
        "folder" -> Icons.Rounded.Folder to Sietch.sand
        "image" -> Icons.Rounded.Image to Sietch.green
        "video" -> Icons.Rounded.Movie to Sietch.violet
        "audio" -> Icons.Rounded.AudioFile to Sietch.pink
        "pdf" -> Icons.Rounded.PictureAsPdf to Sietch.red
        "text" -> Icons.Rounded.Article to Sietch.blue
        "archive" -> Icons.Rounded.FolderZip to Sietch.sand
        "app" -> Icons.Rounded.Android to Sietch.green
        "document" -> Icons.Rounded.Description to Sietch.blue
        else -> Icons.Rounded.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(modifier.size(40.dp).background(color.copy(alpha = .14f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
    }
}

/** One entry in a folder, in a recents list, or in a search result. Long-press selects it where selection exists. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    name: String,
    mime: String?,
    isDirectory: Boolean,
    size: Long,
    modified: Long,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    thumbnailUri: String? = null,
) {
    val meta = buildList {
        if (!isDirectory) add(formatBytes(size))
        add(formatRelative(modified))
    }.filter { it.isNotEmpty() }.joinToString("  ·  ").ifEmpty { null }
    Row(modifier.fillMaxWidth()
        .background(if (selected) Sietch.accent.copy(alpha = .16f) else Color.Transparent, RoundedCornerShape(16.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RowLeading(name, mime, isDirectory, thumbnailUri)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (selected) Sietch.accent else MaterialTheme.colorScheme.onSurface)
            val line = subtitle ?: meta
            if (line != null) Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) trailing()
    }
}

/** Whether a file has a picture of its own to show: pictures, and video (its first frame). */
fun hasThumbnail(name: String, mime: String?): Boolean = when (com.everettpenne.sietch.data.categoryOf(name, mime, false)) {
    "image" -> mime != "image/svg+xml" && !name.lowercase().endsWith(".svg")
    "video" -> true
    else -> false
}

/** The row's leading tile: a live thumbnail for pictures and video, the pastel kind icon otherwise (and beneath a thumbnail that has not loaded). */
@Composable
private fun RowLeading(name: String, mime: String?, isDirectory: Boolean, thumbnailUri: String?) {
    if (thumbnailUri != null && !isDirectory && hasThumbnail(name, mime)) {
        Box(Modifier.size(40.dp)) {
            FileIcon(name, mime, isDirectory)
            AsyncImage(model = thumbnailUri, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(10.dp)))
            if (com.everettpenne.sietch.data.categoryOf(name, mime, false) == "video")
                Icon(Icons.Rounded.PlayCircle, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(18.dp))
        }
    } else FileIcon(name, mime, isDirectory)
}

/** A glass text field: the search box. */
@Composable
fun GlassTextField(
    value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier,
    leading: ImageVector = Icons.Rounded.Search, imeAction: ImeAction = ImeAction.Search, onSubmit: () -> Unit = {},
    autoFocus: Boolean = false,
) {
    val ink = glassPalette().ink
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { if (autoFocus) focusRequester.requestFocus() }
    val submit = { onSubmit(); keyboard?.hide() }
    BasicTextField(value, onValueChange, singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink), cursorBrush = SolidColor(ink),
        modifier = modifier.fillMaxWidth().height(52.dp).frostedGlass(RoundedCornerShape(percent = 50), lensHeight = 10f, lensAmount = 20f)
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { submit() }, onGo = { submit() }, onSearch = { submit() }, onSend = { submit() }),
        decorationBox = { inner ->
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(leading, null, tint = ink.copy(alpha = .6f), modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) { if (value.isEmpty()) Text(placeholder, color = ink.copy(alpha = .45f), style = MaterialTheme.typography.bodyLarge); inner() }
                if (value.isNotEmpty()) Box(Modifier.size(44.dp).clickable { onValueChange("") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Clear", tint = ink.copy(alpha = .6f), modifier = Modifier.size(20.dp))
                }
            }
        })
}
