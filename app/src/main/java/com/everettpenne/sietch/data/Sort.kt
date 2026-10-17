package com.everettpenne.sietch.data

import java.util.Locale

enum class SortBy { NAME, SIZE, MODIFIED, TYPE }
enum class SortOrder { ASC, DESC }

/** The broad kind of a file, used to group by type and to pick its icon and colour. */
fun categoryOf(name: String, mime: String?, isDirectory: Boolean): String {
    if (isDirectory) return "folder"
    val m = mime.orEmpty()
    val n = name.lowercase(Locale.ROOT)
    return when {
        m.startsWith("image/") -> "image"
        m.startsWith("video/") -> "video"
        m.startsWith("audio/") -> "audio"
        m == "application/pdf" || n.endsWith(".pdf") -> "pdf"
        m == "text/plain" || n.endsWith(".txt") || n.endsWith(".md") || n.endsWith(".log") -> "text"
        m.startsWith("text/") || n.endsWith(".xml") || n.endsWith(".json") || n.endsWith(".csv") -> "text"
        n.endsWith(".zip") || n.endsWith(".tar") || n.endsWith(".gz") || n.endsWith(".7z") || n.endsWith(".rar") -> "archive"
        n.endsWith(".apk") || n.endsWith(".apks") || n.endsWith(".aab") -> "app"
        n.endsWith(".doc") || n.endsWith(".docx") || n.endsWith(".xls") || n.endsWith(".xlsx") || n.endsWith(".ppt") || n.endsWith(".pptx") || n.endsWith(".odt") || n.endsWith(".ods") -> "document"
        n.endsWith(".html") || n.endsWith(".htm") || n.endsWith(".epub") -> "document"
        else -> "other"
    }
}

fun Node.category(): String = categoryOf(name, mime, isDirectory)

/** The human name of a file's broad kind, for subtitles and details rows. */
fun kindLabel(name: String, mime: String?, isDirectory: Boolean): String = when (categoryOf(name, mime, isDirectory)) {
    "folder" -> "Folder"
    "image" -> "Image"
    "video" -> "Video"
    "audio" -> "Audio"
    "pdf" -> "PDF"
    "text" -> "Text"
    "archive" -> "Archive"
    "app" -> "App"
    "document" -> "Document"
    else -> "File"
}

/** Sorts a folder the way a file manager should: folders first (optionally), then the chosen key, names last and always ascending. */
fun sortNodes(nodes: List<Node>, by: SortBy, order: SortOrder, foldersFirst: Boolean): List<Node> {
    if (nodes.size < 2) return nodes
    return nodes.sortedWith(Comparator { a, b ->
        if (foldersFirst) {
            val fa = if (a.isDirectory) 0 else 1
            val fb = if (b.isDirectory) 0 else 1
            if (fa != fb) return@Comparator fa - fb
        }
        val primary = when (by) {
            SortBy.NAME -> a.name.lowercase(Locale.ROOT).compareTo(b.name.lowercase(Locale.ROOT))
            SortBy.SIZE -> a.size.compareTo(b.size)
            SortBy.MODIFIED -> a.modified.compareTo(b.modified)
            SortBy.TYPE -> a.category().compareTo(b.category())
        }
        if (primary != 0) return@Comparator if (order == SortOrder.ASC) primary else -primary
        a.name.lowercase(Locale.ROOT).compareTo(b.name.lowercase(Locale.ROOT))
    })
}
