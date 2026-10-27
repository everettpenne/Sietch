package com.everettpenne.sietch.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.everettpenne.sietch.data.Access
import com.everettpenne.sietch.data.AccessState
import com.everettpenne.sietch.ui.theme.Sietch
import com.everettpenne.glass.GlassButton
import com.everettpenne.glass.glassPalette
import com.everettpenne.glass.glassSurface

/** What the user has allowed, re-read whenever the app comes back to the front (they may have just flipped a switch in system settings). */
@Composable
fun rememberAccess(): State<AccessState> {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(Access.state(context)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) state.value = Access.state(context) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return state
}

/**
 * Says what is missing and offers the one step that fixes it: the media permission prompt, or the system's "All files access" screen.
 * Draws nothing when the screen's needs are met, so a screen can always place it.
 */
@Composable
fun AccessPrompt(access: AccessState, needAllFiles: Boolean, modifier: Modifier = Modifier, onGranted: () -> Unit = {}) {
    val context = LocalContext.current
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onGranted() }
    val needMedia = !access.media
    val needFiles = needAllFiles && !access.allFiles
    if (!needMedia && !needFiles) return
    val ink = glassPalette().ink
    Column(modifier.fillMaxWidth().glassSurface(RoundedCornerShape(24.dp), blurRadius = 6f, lensHeight = 16f, lensAmount = 28f).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Lock, null, tint = Sietch.accent, modifier = Modifier.size(22.dp))
            Text(if (needFiles) "Allow access to your files" else "Allow access to your media", style = MaterialTheme.typography.titleSmall, color = ink)
        }
        Text(
            if (needFiles) "To browse every folder, and to list documents, downloads and apps, Android needs you to switch on All files access for Sietch. Nothing leaves your phone."
            else "To show your photos, videos and audio, Sietch asks Android for read access to them. Nothing leaves your phone.",
            style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = .7f))
        if (needFiles) GlassButton("Open All files access", { runCatching { context.startActivity(Access.allFilesIntent(context)) } }, Modifier.fillMaxWidth(), prominent = true)
        else GlassButton("Allow media access", { mediaLauncher.launch(Access.mediaPermissions) }, Modifier.fillMaxWidth(), prominent = true)
        if (needFiles && needMedia) GlassButton("Allow media only", { mediaLauncher.launch(Access.mediaPermissions) }, Modifier.fillMaxWidth(), prominent = false)
    }
}
