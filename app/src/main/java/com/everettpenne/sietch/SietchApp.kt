package com.everettpenne.sietch

import android.app.Application
import android.content.Context
import com.everettpenne.sietch.data.FileSystem
import com.everettpenne.sietch.data.Locations
import com.everettpenne.sietch.data.Recents
import com.everettpenne.sietch.data.Settings
import java.io.File

/** The app's shared parts, built once: the file system, the granted folders, the recents and the preferences. */
class Graph(app: Context) {
    val fileSystem = FileSystem(app.contentResolver)
    val locations = Locations(File(app.filesDir, "locations.json"))
    val recents = Recents(File(app.filesDir, "recents.json"))
    val settings = Settings(app)
}

class SietchApp : Application() {
    lateinit var graph: Graph
        private set

    override fun onCreate() { super.onCreate(); graph = Graph(this) }
}

val Context.graph: Graph get() = (applicationContext as SietchApp).graph
