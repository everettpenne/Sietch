package com.everettpenne.sietch.data

import android.content.ContentResolver
import android.os.Bundle
import android.provider.MediaStore

/** The browsable groups on the Home screen, the way a phone's file manager sorts what it finds. */
enum class Category(val label: String, val needsAllFiles: Boolean) {
    IMAGES("Images", false),
    VIDEOS("Videos", false),
    AUDIO("Audio", false),
    DOCUMENTS("Documents", true),
    DOWNLOADS("Downloads", true),
    APPS("Apps", true),
    ARCHIVES("Archives", true),
}

/** A SQL selection and its arguments. */
data class Selection(val where: String, val args: Array<String>)

private const val DOC_MIMES = "'application/pdf','application/msword','application/vnd.ms-excel','application/vnd.ms-powerpoint','application/rtf','application/epub+zip'," +
    "'application/vnd.openxmlformats-officedocument.wordprocessingml.document','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'," +
    "'application/vnd.openxmlformats-officedocument.presentationml.presentation','application/vnd.oasis.opendocument.text','application/vnd.oasis.opendocument.spreadsheet'"

/** Not a hidden file, not inside a hidden folder, and not in the per-app Android/ folders (app data and Sietch's own recycle bin). */
private const val VISIBLE = "mime_type IS NOT NULL AND _data NOT LIKE '%/.%' AND _data NOT LIKE '%/Android/data/%' AND _data NOT LIKE '%/Android/obb/%'"

/** The media-library filter for a category. [root] is the storage root (for "Downloads"). Pure, so it is unit-tested. */
fun selectionFor(category: Category, root: String): Selection = when (category) {
    Category.IMAGES -> Selection("media_type = 1 AND $VISIBLE", emptyArray())
    Category.VIDEOS -> Selection("media_type = 3 AND $VISIBLE", emptyArray())
    Category.AUDIO -> Selection("media_type = 2 AND $VISIBLE", emptyArray())
    Category.DOCUMENTS -> Selection("(mime_type IN ($DOC_MIMES) OR mime_type LIKE 'text/%') AND $VISIBLE", emptyArray())
    Category.DOWNLOADS -> Selection("_data LIKE ? AND $VISIBLE", arrayOf("${escapeLike(root)}/Download/%"))
    Category.APPS -> Selection("(mime_type = 'application/vnd.android.package-archive' OR _data LIKE '%.apk') AND $VISIBLE", emptyArray())
    Category.ARCHIVES -> Selection("(_data LIKE '%.zip' OR _data LIKE '%.7z' OR _data LIKE '%.rar' OR _data LIKE '%.tar' OR _data LIKE '%.gz') AND $VISIBLE", emptyArray())
}

/** Escapes the characters that mean something inside a LIKE pattern, so a file named "100%" is searched for literally. */
fun escapeLike(s: String): String = s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

fun searchSelection(query: String): Selection = Selection("_display_name LIKE ? ESCAPE '\\' AND $VISIBLE", arrayOf("%${escapeLike(query.trim())}%"))

/**
 * A read-only view of the media library's file table. It is what makes categories, Recents and Search instant: the system has already indexed every
 * file, so Sietch does not walk the disk. What it can see depends on the permissions held (see [AccessState]).
 */
class MediaIndex(private val resolver: ContentResolver, private val root: String) {
    private val uri = MediaStore.Files.getContentUri("external")
    private val columns = arrayOf("_data", "_size", "date_modified", "mime_type")

    private fun query(selection: Selection, limit: Int?, newestFirst: Boolean = true): List<Node> {
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection.where)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selection.args)
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf("date_modified"))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, if (newestFirst) ContentResolver.QUERY_SORT_DIRECTION_DESCENDING else ContentResolver.QUERY_SORT_DIRECTION_ASCENDING)
            if (limit != null) putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        }
        val out = ArrayList<Node>()
        runCatching {
            resolver.query(uri, columns, args, null)?.use { c ->
                while (c.moveToNext()) {
                    val path = c.getString(0) ?: continue
                    out += Node(path, path.substringAfterLast('/'), false, c.getLong(1), c.getLong(2) * 1000, c.getString(3))
                }
            }
        }
        return out
    }

    fun list(category: Category, limit: Int? = null): List<Node> = query(selectionFor(category, root), limit)

    /** How many files are in a category (0 when the permission is missing). */
    fun count(category: Category): Int {
        val s = selectionFor(category, root)
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, s.where)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, s.args)
        }
        return runCatching { resolver.query(uri, arrayOf("_id"), args, null)?.use { it.count } ?: 0 }.getOrDefault(0)
    }

    /** The newest files of any kind. */
    fun recent(limit: Int): List<Node> = query(Selection(VISIBLE, emptyArray()), limit)

    fun search(query: String, limit: Int = 300): List<Node> = if (query.isBlank()) emptyList() else query(searchSelection(query), limit)
}
