package com.everettpenne.sietch.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** The few preferences a navigator needs. Backed by SharedPreferences; values flow so a change applies everywhere at once. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val showHidden = MutableStateFlow(prefs.getBoolean("showHidden", false))
    val foldersFirst = MutableStateFlow(prefs.getBoolean("foldersFirst", true))
    val sortBy = MutableStateFlow(SortBy.valueOf(prefs.getString("sortBy", SortBy.NAME.name) ?: SortBy.NAME.name))
    val sortOrder = MutableStateFlow(SortOrder.valueOf(prefs.getString("sortOrder", SortOrder.ASC.name) ?: SortOrder.ASC.name))

    fun setShowHidden(v: Boolean) { showHidden.value = v; prefs.edit().putBoolean("showHidden", v).apply() }
    fun setFoldersFirst(v: Boolean) { foldersFirst.value = v; prefs.edit().putBoolean("foldersFirst", v).apply() }
    fun setSortBy(v: SortBy) { sortBy.value = v; prefs.edit().putString("sortBy", v.name).apply() }
    fun setSortOrder(v: SortOrder) { sortOrder.value = v; prefs.edit().putString("sortOrder", v.name).apply() }
}
