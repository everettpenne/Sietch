package com.everettpenne.sietch

import android.app.Application
import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import com.everettpenne.sietch.data.LocalFs
import com.everettpenne.sietch.data.MediaIndex
import com.everettpenne.sietch.data.Settings
import com.everettpenne.sietch.data.TrashStore
import java.io.File

/** The app's shared parts, built once: the file operations, the media index, the recycle bin and the preferences. */
class Graph(val app: Context) {
    val root: File = Environment.getExternalStorageDirectory()
    val fs = LocalFs(mimeOf = { name -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase()) })
    val media = MediaIndex(app.contentResolver, root.path)
    val settings = Settings(app)
    // The bin lives in the app's own folder on shared storage, which is on the same volume as the files it holds, so deleting is a rename and not a copy.
    val trash = TrashStore(File(app.getExternalFilesDir(null) ?: app.filesDir, ".recycle"), File(app.filesDir, "recycle.json"), fs)
}

class SietchApp : Application(), coil3.SingletonImageLoader.Factory {
    lateinit var graph: Graph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = Graph(this)
        forgetFolderGrants()
        // Items past their retention period are removed once per launch, off the main thread.
        kotlin.concurrent.thread(name = "sietch-purge") { runCatching { graph.trash.purgeExpired(graph.settings.trashDays.value) } }
    }

    /**
     * Versions before 1.0 kept a list of folders you had granted through Android's folder picker. 1.0 browses with "All files access" instead, so those
     * grants are no longer used. They are released here, once, so the system does not keep access that nothing in the app can show or remove, and the
     * two small files that listed them are deleted.
     */
    private fun forgetFolderGrants() {
        val prefs = getSharedPreferences("migration", MODE_PRIVATE)
        if (prefs.getBoolean("folderGrantsForgotten", false)) return
        runCatching { contentResolver.persistedUriPermissions.forEach { contentResolver.releasePersistableUriPermission(it.uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } }
        java.io.File(filesDir, "locations.json").delete()
        java.io.File(filesDir, "recents.json").delete()
        prefs.edit().putBoolean("folderGrantsForgotten", true).apply()
    }

    /** Thumbnails for video files too: Coil reads a frame from the file with the platform's own decoder. */
    override fun newImageLoader(context: coil3.PlatformContext): coil3.ImageLoader =
        coil3.ImageLoader.Builder(context).components { add(coil3.video.VideoFrameDecoder.Factory()) }.build()
}

val Context.graph: Graph get() = (applicationContext as SietchApp).graph
