package com.everettpenne.sietch.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.everettpenne.sietch.data.InApp
import com.everettpenne.sietch.data.SHORTCUT_EXTENSIONS
import com.everettpenne.sietch.data.inAppViewer
import com.everettpenne.media.LocalImageViewer
import com.everettpenne.media.LocalVideoViewer
import java.io.File

/** The address another app can read one of our files at, for as long as the intent that carries it lives. */
fun shareUri(context: Context, file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

/** Opens files the way every screen should: pictures, video, saved web pages and web shortcuts in Sietch itself, the rest in another app. */
class Opener(
    private val context: Context,
    private val images: (List<String>, Int) -> Unit,
    private val video: (String, String?) -> Unit,
    private val playlist: (List<com.everettpenne.player.VideoSource>, Int) -> Unit = { _, _ -> },
    private val page: (String, String) -> Unit = { _, _ -> },
    private val web: (String) -> Unit = {},
) {
    /**
     * Opens [file]. [pictures] is the set of picture paths to swipe through when this is a picture (the folder's, in the order shown); without it
     * the viewer shows just this one. Returns false when nothing could open it (no app for the type).
     */
    fun open(file: File, mime: String?, pictures: List<String>? = null, queue: List<String>? = null): Boolean = when (inAppViewer(file.name, mime)) {
        InApp.IMAGE -> {
            val all = (pictures?.takeIf { file.path in it } ?: listOf(file.path)).map { Uri.fromFile(File(it)).toString() }
            images(all, all.indexOf(Uri.fromFile(file).toString()).coerceAtLeast(0)); true
        }
        InApp.PLAYER -> {
            // a video opens among the folder's other videos (a sound among its other sounds), in the order shown, so next, previous and auto-play walk the folder
            val all = queue?.takeIf { file.path in it }
            if (all != null) playlist(all.map { com.everettpenne.player.VideoSource(Uri.fromFile(File(it)).toString(), File(it).name) }, all.indexOf(file.path))
            else video(Uri.fromFile(file).toString(), file.name)
            true
        }
        InApp.WEB -> { openPage(file, mime); true }
        InApp.NONE -> openWith(file, mime)
    }

    /**
     * A saved page is read here (up to a size no page needs to pass) and handed to the browser as a document: it is shown with JavaScript off and
     * no access to files, so it can only be read. A web shortcut opens the address it holds. Either one that cannot be read goes to another app.
     */
    private fun openPage(file: File, mime: String?) {
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        kotlin.concurrent.thread(name = "sietch-open-page") {
            val text = runCatching {
                file.inputStream().use { input ->
                    val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(64 * 1024)
                    while (out.size() < MAX_PAGE_BYTES) { val n = input.read(buf, 0, minOf(buf.size, MAX_PAGE_BYTES - out.size())); if (n < 0) break; out.write(buf, 0, n) }
                    out.toString(Charsets.UTF_8.name())
                }
            }.getOrNull()
            main.post {
                val isShortcut = file.extension.lowercase() in SHORTCUT_EXTENSIONS
                when {
                    text == null -> openWith(file, mime)
                    isShortcut -> com.everettpenne.browser.BrowserUrls.shortcutTarget(text)?.let(web) ?: openWith(file, mime)
                    else -> page(text, file.name)
                }
            }
        }
    }

    private companion object { const val MAX_PAGE_BYTES = 8 * 1024 * 1024 }

    /** Hands the file to another app (the system's chooser decides which). */
    fun openWith(file: File, mime: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(shareUri(context, file), mime ?: "*/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}

@Composable
fun rememberOpener(): Opener {
    val context = LocalContext.current
    val images = LocalImageViewer.current
    val video = LocalVideoViewer.current
    val playlist = com.everettpenne.player.LocalVideoPlaylist.current
    val page = com.everettpenne.browser.LocalHtmlViewer.current
    val web = com.everettpenne.browser.LocalBrowser.current
    return remember(context, images, video, playlist, page, web) { Opener(context, images, video, playlist, page, web) }
}
