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

/** Opens files the way every screen should: pictures, video, saved web pages and web shortcuts in Sietch itself, the rest in another app. */
class Opener(
    private val context: android.content.Context,
    private val images: (List<String>, Int) -> Unit,
    private val video: (String, String?) -> Unit,
    private val page: (String, String) -> Unit = { _, _ -> },
    private val web: (String) -> Unit = {},
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
        InApp.WEB -> { openPage(uri, name, mime); true }
        InApp.NONE -> openWith(uri, mime)
    }

    /**
     * A saved page is read here (up to a size no page needs to pass) and handed to the browser as a document: it is shown with JavaScript off and
     * no access to files, so it can only be read. A web shortcut opens the address it holds. Either one that cannot be read goes to another app.
     */
    private fun openPage(uri: Uri, name: String, mime: String?) {
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        kotlin.concurrent.thread(name = "sietch-open-page") {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(64 * 1024)
                    while (out.size() < MAX_PAGE_BYTES) { val n = input.read(buf, 0, minOf(buf.size, MAX_PAGE_BYTES - out.size())); if (n < 0) break; out.write(buf, 0, n) }
                    out.toString(Charsets.UTF_8.name())
                }
            }.getOrNull()
            main.post {
                val isShortcut = name.substringAfterLast('.', "").lowercase() in com.everettpenne.sietch.data.SHORTCUT_EXTENSIONS
                when {
                    text == null -> openWith(uri, mime)
                    isShortcut -> com.everettpenne.browser.BrowserUrls.shortcutTarget(text)?.let(web) ?: openWith(uri, mime)
                    else -> page(text, name)
                }
            }
        }
    }

    private companion object { const val MAX_PAGE_BYTES = 8 * 1024 * 1024 }

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
    val page = com.everettpenne.browser.LocalHtmlViewer.current
    val web = com.everettpenne.browser.LocalBrowser.current
    return remember(context, images, video, page, web) { Opener(context, images, video, page, web) }
}
