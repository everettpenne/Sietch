package com.everettpenne.sietch.data

import java.io.File
import java.io.IOException

/**
 * The file operations, on plain paths. This needs "All files access" for anything outside the app's own folders; without it the calls
 * simply fail (and say so), they never throw past this class. Kept free of Android types so it is unit-tested on the JVM.
 */
class LocalFs(private val mimeOf: (String) -> String? = { null }) {

    /** The entries of [dir]. Throws [IOException] when it cannot be read, so the screen can say why instead of showing "empty". */
    fun list(dir: File): List<Node> {
        val files = dir.listFiles() ?: throw IOException(if (dir.isDirectory) "Sietch may not read this folder." else "This folder is gone.")
        return files.map { nodeOf(it) }
    }

    fun nodeOf(f: File): Node = Node(f.path, f.name, f.isDirectory, if (f.isDirectory) 0 else f.length(), f.lastModified(), if (f.isDirectory) null else mimeOf(f.name))

    /** A name that does not exist in [dir] yet: [name], or "name (1).ext", "name (2).ext" and so on. */
    fun uniqueName(dir: File, name: String): String {
        if (!File(dir, name).exists()) return name
        val dot = name.lastIndexOf('.').takeIf { it > 0 }
        val stem = if (dot != null) name.substring(0, dot) else name
        val ext = if (dot != null) name.substring(dot) else ""
        var i = 1
        while (File(dir, "$stem ($i)$ext").exists()) i++
        return "$stem ($i)$ext"
    }

    /** A name a user may give a file: not empty, not "." or "..", no path separators. */
    fun validName(name: String): Boolean = name.isNotBlank() && name != "." && name != ".." && '/' !in name && '\u0000' !in name

    fun createFolder(parent: File, name: String): File? {
        if (!validName(name)) return null
        val dir = File(parent, name)
        return if (!dir.exists() && dir.mkdir()) dir else null
    }

    /** Renames in place. Refuses to overwrite an existing entry. */
    fun rename(file: File, newName: String): Boolean {
        if (!validName(newName)) return false
        val target = File(file.parentFile ?: return false, newName)
        if (target.exists()) return false
        return file.renameTo(target)
    }

    /** True when [target] is [source] or lies inside it, which would copy or move a folder into itself. */
    fun isInside(source: File, target: File): Boolean {
        val s = source.canonicalFile; val t = target.canonicalFile
        return t == s || t.path.startsWith(s.path + File.separator)
    }

    /** Copies [source] into [targetDir] under a name that does not clash. Returns the copy, or null if anything failed (a partial copy is removed). */
    fun copy(source: File, targetDir: File): File? {
        if (!source.exists() || !targetDir.isDirectory || isInside(source, targetDir)) return null
        val dest = File(targetDir, uniqueName(targetDir, source.name))
        val ok = runCatching { copyInto(source, dest) }.getOrDefault(false)
        if (!ok) deleteRecursively(dest)
        return if (ok) dest else null
    }

    private fun copyInto(source: File, dest: File): Boolean {
        if (source.isDirectory) {
            if (!dest.mkdir()) return false
            val children = source.listFiles() ?: return false
            return children.all { copyInto(it, File(dest, it.name)) }
        }
        source.inputStream().use { input -> dest.outputStream().use { out -> input.copyTo(out) } }
        dest.setLastModified(source.lastModified())
        return true
    }

    /** Moves [source] into [targetDir]: a rename where the file system allows it, otherwise a copy then a delete (so a failed copy loses nothing). */
    fun move(source: File, targetDir: File): File? {
        if (!source.exists() || !targetDir.isDirectory || isInside(source, targetDir)) return null
        if (source.parentFile?.canonicalFile == targetDir.canonicalFile) return source
        val dest = File(targetDir, uniqueName(targetDir, source.name))
        if (source.renameTo(dest)) return dest
        val copy = copy(source, targetDir) ?: return null
        return if (deleteRecursively(source)) copy else { deleteRecursively(copy); null }
    }

    /** Moves [source] to exactly [dest] (which must not exist): a rename where possible, else copy then delete. A failure leaves [source] untouched. */
    fun moveExact(source: File, dest: File): Boolean {
        if (dest.exists() || !source.exists()) return false
        if (source.renameTo(dest)) return true
        val ok = runCatching { copyInto(source, dest) }.getOrDefault(false)
        if (ok && deleteRecursively(source)) return true
        deleteRecursively(dest)
        return false
    }

    /** Deletes a file, or a folder and everything in it. True only if it is gone afterwards. */
    fun deleteRecursively(f: File): Boolean = f.deleteRecursively() || !f.exists()

    /** The total size of a file, or of everything inside a folder. */
    fun sizeOf(f: File): Long = if (f.isDirectory) (f.listFiles() ?: emptyArray()).sumOf { sizeOf(it) } else f.length()
}
