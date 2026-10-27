package com.everettpenne.sietch.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** The preferences. Backed by SharedPreferences; values flow so a change applies everywhere at once. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val showHidden = MutableStateFlow(prefs.getBoolean("showHidden", false))
    val foldersFirst = MutableStateFlow(prefs.getBoolean("foldersFirst", true))
    val sortBy = MutableStateFlow(SortBy.entries.firstOrNull { it.name == prefs.getString("sortBy", null) } ?: SortBy.NAME)
    val sortOrder = MutableStateFlow(SortOrder.entries.firstOrNull { it.name == prefs.getString("sortOrder", null) } ?: SortOrder.ASC)
    /** Folders show as a grid of thumbnails instead of a list. */
    val gallery = MutableStateFlow(prefs.getBoolean("gallery", false))
    /** Deleting moves to the recycle bin (on) or removes for good after a confirmation (off). */
    val useTrash = MutableStateFlow(prefs.getBoolean("useTrash", true))
    /** Days an item stays in the recycle bin before it is removed automatically. */
    val trashDays = MutableStateFlow(prefs.getInt("trashDays", 30))

    fun setShowHidden(v: Boolean) { showHidden.value = v; prefs.edit().putBoolean("showHidden", v).apply() }
    fun setFoldersFirst(v: Boolean) { foldersFirst.value = v; prefs.edit().putBoolean("foldersFirst", v).apply() }
    fun setSortBy(v: SortBy) { sortBy.value = v; prefs.edit().putString("sortBy", v.name).apply() }
    fun setSortOrder(v: SortOrder) { sortOrder.value = v; prefs.edit().putString("sortOrder", v.name).apply() }
    fun setGallery(v: Boolean) { gallery.value = v; prefs.edit().putBoolean("gallery", v).apply() }
    fun setUseTrash(v: Boolean) { useTrash.value = v; prefs.edit().putBoolean("useTrash", v).apply() }
    fun setTrashDays(v: Int) { trashDays.value = v; prefs.edit().putInt("trashDays", v).apply() }
}
