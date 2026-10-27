package com.everettpenne.sietch.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class TrashItem(
    val id: String,
    val originalPath: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val deletedAt: Long,
)

/**
 * The recycle bin: a deleted file is moved (not copied) into [dir] under a random id, and a small index remembers where it came from and when.
 * Moving keeps it instant and keeps its bytes on the same volume. Items older than the retention period are removed by [purgeExpired].
 *
 * Thread-safe: every public call takes the lock, so a delete on the UI's background thread and the launch-time purge cannot interleave.
 */
class TrashStore(private val dir: File, private val index: File, private val fs: LocalFs, private val now: () -> Long = System::currentTimeMillis) {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()
    private val state = MutableStateFlow(load())
    val items: StateFlow<List<TrashItem>> = state

    private fun load(): List<TrashItem> = runCatching { json.decodeFromString<List<TrashItem>>(index.readText()) }.getOrDefault(emptyList())
        // an entry whose payload is gone (cleared by hand, or lost) is only noise
        .filter { File(dir, it.id).exists() }

    private fun save(list: List<TrashItem>) {
        state.value = list
        index.parentFile?.mkdirs()
        // write beside, then rename: a crash mid-write must not leave a half-written index that forgets every item
        val tmp = File(index.path + ".tmp")
        tmp.writeText(json.encodeToString(list))
        if (!tmp.renameTo(index)) { index.writeText(json.encodeToString(list)); tmp.delete() }
    }

    /** Moves [file] into the bin. Returns false if it could not be moved (then it is left exactly where it was). */
    fun trash(file: File): Boolean = synchronized(lock) {
        if (!file.exists()) return false
        dir.mkdirs()
        val id = UUID.randomUUID().toString()
        val item = TrashItem(id, file.absolutePath, file.name, file.isDirectory, fs.sizeOf(file), now())
        if (!fs.moveExact(file, File(dir, id))) return false
        save(state.value + item)
        true
    }

    /** Puts an item back where it came from (or beside that name if it is taken, or if its folder is gone it is recreated). Returns the restored file. */
    fun restore(id: String): File? = synchronized(lock) {
        val item = state.value.firstOrNull { it.id == id } ?: return null
        val src = File(dir, id)
        val parent = File(item.originalPath).parentFile ?: return null
        if (!parent.exists() && !parent.mkdirs()) return null
        val dest = File(parent, fs.uniqueName(parent, item.name))
        if (!fs.moveExact(src, dest)) return null
        save(state.value.filter { it.id != id })
        dest
    }

    fun deleteForever(id: String): Boolean = synchronized(lock) {
        val gone = fs.deleteRecursively(File(dir, id))
        if (gone) save(state.value.filter { it.id != id })
        gone
    }

    /** Removes everything. Returns how many items could not be deleted. */
    fun empty(): Int = synchronized(lock) {
        val failed = state.value.count { !fs.deleteRecursively(File(dir, it.id)) }
        save(state.value.filter { File(dir, it.id).exists() })
        failed
    }

    /** Deletes items that have been in the bin longer than [days] days. Returns how many were removed. */
    fun purgeExpired(days: Int): Int = synchronized(lock) {
        val cutoff = now() - days * 86_400_000L
        val old = state.value.filter { it.deletedAt < cutoff }
        old.forEach { fs.deleteRecursively(File(dir, it.id)) }
        if (old.isNotEmpty()) save(state.value.filter { it !in old })
        old.size
    }

    /** Whole days left before [item] is removed automatically (0 on its last day). */
    fun daysLeft(item: TrashItem, retentionDays: Int): Int = (retentionDays - ((now() - item.deletedAt) / 86_400_000L).toInt()).coerceAtLeast(0)

    /** Where an item's bytes are kept while it is in the bin (for thumbnails and previews). */
    fun payloadOf(item: TrashItem): File = File(dir, item.id)

    fun totalSize(): Long = state.value.sumOf { it.size }
}
