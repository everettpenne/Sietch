package com.everettpenne.sietch.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** A folder or file the user opened recently, remembered by tree + document id so it can be re-opened later. */
@Serializable
data class Recent(val name: String, val treeUri: String, val documentId: String, val isDirectory: Boolean, val visitedAt: Long)

/** Recently opened folders and files, capped and kept in the app's private storage. */
class Recents(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _items = MutableStateFlow<List<Recent>>(emptyList())
    val items: StateFlow<List<Recent>> = _items

    init { load() }

    fun record(name: String, treeUri: String, documentId: String, isDirectory: Boolean) {
        val now = System.currentTimeMillis()
        val rest = _items.value.filterNot { it.treeUri == treeUri && it.documentId == documentId }
        _items.value = (listOf(Recent(name, treeUri, documentId, isDirectory, now)) + rest).take(50)
        save()
    }

    fun clear() { _items.value = emptyList(); save() }

    private fun save() = runCatching { file.writeText(json.encodeToString(_items.value)) }

    private fun load() {
        _items.value = if (file.exists()) runCatching { json.decodeFromString<List<Recent>>(file.readText()) }.getOrDefault(emptyList()) else emptyList()
    }
}
