package com.everettpenne.sietch.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.everettpenne.media.LocalImageViewer
import com.everettpenne.media.LocalVideoViewer
import com.everettpenne.sietch.data.InApp
import com.everettpenne.sietch.data.inAppViewer

/** Opens files the way every screen should: pictures and video in Sietch itself, the rest in another app. */
class Opener(
    private val context: android.content.Context,
    private val images: (List<String>, Int) -> Unit,
    private val video: (String, String?) -> Unit,
) {
    /**
     * Opens [uri]. [pictures] is the set of picture addresses to swipe through when this is a picture (the folder's, in the order shown); without
     * it the viewer shows just this one. Returns false when nothing could open it (no app for the type).
     */
    fun open(uri: Uri, name: String, mime: String?, pictures: List<String>? = null): Boolean = when (inAppViewer(name, mime)) {
        InApp.IMAGE -> {
            val all = pictures?.takeIf { uri.toString() in it } ?: listOf(uri.toString())
            images(all, all.indexOf(uri.toString()).coerceAtLeast(0)); true
        }
        InApp.PLAYER -> { video(uri.toString(), name); true }
        InApp.NONE -> openWith(uri, mime)
    }

    /** Hands the file to another app (the system's chooser decides which). */
    fun openWith(uri: Uri, mime: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, mime ?: "*/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}

@Composable
fun rememberOpener(): Opener {
    val context = LocalContext.current
    val images = LocalImageViewer.current
    val video = LocalVideoViewer.current
    return remember(context, images, video) { Opener(context, images, video) }
}
