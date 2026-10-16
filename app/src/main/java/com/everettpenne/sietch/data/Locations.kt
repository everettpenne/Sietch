package com.everettpenne.sietch.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** A folder the user granted Sietch through the system picker. [uri] is the persisted tree uri. */
@Serializable
data class Location(val name: String, val uri: String, val addedAt: Long)

/**
 * The granted folders, kept as a plain JSON file in the app's private storage. Nothing here is readable by any other app, and backups
 * are off, so the tokens never leave the phone.
 */
class Locations(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _items = MutableStateFlow<List<Location>>(emptyList())
    val items: StateFlow<List<Location>> = _items

    init { load() }

    fun add(name: String, uri: String) {
        _items.value = _items.value.filterNot { it.uri == uri } + Location(name, uri, System.currentTimeMillis())
        save()
    }

    fun remove(uri: String) { _items.value = _items.value.filterNot { it.uri == uri }; save() }

    fun rename(uri: String, name: String) {
        _items.value = _items.value.map { if (it.uri == uri) it.copy(name = name) else it }
        save()
    }

    fun has(uri: String) = _items.value.any { it.uri == uri }

    private fun save() = runCatching { file.writeText(json.encodeToString(_items.value)) }

    private fun load() {
        _items.value = if (file.exists()) runCatching { json.decodeFromString<List<Location>>(file.readText()) }.getOrDefault(emptyList()) else emptyList()
    }
}
