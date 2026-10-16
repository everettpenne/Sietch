package com.everettpenne.sietch.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/**
 * A folder's address inside a granted tree: the tree uri string the user handed Sietch, plus the folder's document id relative to
 * that tree (the tree's own id for the root). This is the one shape DocumentsContract can list, create and rename against reliably —
 * a bare document uri cannot list its own children. The uri stays a String so the model is plain and testable.
 */
data class Folder(val treeUri: String, val documentId: String)

/** One entry in a folder. [folder] is where it lives; [documentId] is its id relative to that tree. */
data class Node(
    val folder: Folder,
    val documentId: String,
    val name: String,
    val mime: String?,
    val size: Long,
    val modified: Long,
    val isDirectory: Boolean,
)

/** A match from a recursive search, with the breadcrumb of the folder it was found in. */
data class SearchHit(val node: Node, val path: String)

/**
 * Everything Sietch does to files goes through the Storage Access Framework's DocumentsContract, never through a storage permission.
 * The app only ever touches the trees the user handed it, and each operation is a single provider call (or a plain stream copy), so
 * there is no file-path guessing, no broad access, and nothing that works outside the granted folders.
 */
class FileSystem(private val resolver: ContentResolver) {

    fun rootOf(treeUri: String): Folder = Folder(treeUri, DocumentsContract.getTreeDocumentId(Uri.parse(treeUri)))

    /** The human name of a granted tree, by asking the provider; falls back to the uri's last segment. */
    fun treeName(treeUri: String): String = try {
        val parsed = Uri.parse(treeUri)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(parsed, DocumentsContract.getTreeDocumentId(parsed))
        val name = resolver.query(docUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) ?: "" else ""
        }.orEmpty()
        name.ifBlank { parsed.lastPathSegment ?: "Folder" }
    } catch (_: Exception) { Uri.parse(treeUri).lastPathSegment ?: "Folder" }

    /** The document uri for a node, for opening it with another app or asking the provider to act on it. */
    fun documentUri(node: Node): Uri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(node.folder.treeUri), node.documentId)

    private fun folderUri(folder: Folder): Uri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(folder.treeUri), folder.documentId)

    private val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    /** The children of [folder], in provider order. Throws if the provider refuses; the caller decides how to show that. */
    fun list(folder: Folder): List<Node> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(Uri.parse(folder.treeUri), folder.documentId)
        val cursor = resolver.query(uri, projection, null, null, null)
            ?: throw IllegalStateException("The folder did not answer.")
        cursor.use { c ->
            val out = ArrayList<Node>()
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: id
                val mime = c.getString(2)
                val size = if (c.isNull(3)) 0L else c.getLong(3)
                val modified = if (c.isNull(4)) 0L else c.getLong(4)
                out += Node(folder, id, name, mime, size, modified, mime == DocumentsContract.Document.MIME_TYPE_DIR)
            }
            return out
        }
    }

    /** Children that are safe to walk during a search: an unreadable folder simply contributes nothing. */
    private fun listOrEmpty(folder: Folder): List<Node> = try { list(folder) } catch (_: Exception) { emptyList() }

    fun createFolder(parent: Folder, name: String): Node? = try {
        val uri = DocumentsContract.createDocument(resolver, folderUri(parent), DocumentsContract.Document.MIME_TYPE_DIR, name)
        Node(parent, DocumentsContract.getDocumentId(uri), name, DocumentsContract.Document.MIME_TYPE_DIR, 0L, 0L, true)
    } catch (_: Exception) { null }

    fun rename(node: Node, newName: String): Boolean = try {
        DocumentsContract.renameDocument(resolver, documentUri(node), newName) != null
    } catch (_: Exception) { false }

    fun delete(node: Node): Boolean = try {
        DocumentsContract.deleteDocument(resolver, documentUri(node))
    } catch (_: Exception) { false }

    fun copy(source: Node, targetParent: Folder): Boolean = try { copyInto(source, targetParent); true } catch (_: Exception) { false }

    fun move(source: Node, targetParent: Folder): Boolean {
        if (source.folder == targetParent) return false
        // Providers that support it move in one call; otherwise copy and delete.
        try {
            val moved = DocumentsContract.moveDocument(resolver, documentUri(source), folderUri(source.folder), folderUri(targetParent))
            if (moved != null) return true
        } catch (_: Exception) { }
        return try { copyInto(source, targetParent); delete(source) } catch (_: Exception) { false }
    }

    private fun copyInto(source: Node, targetParent: Folder) {
        val srcUri = documentUri(source)
        if (source.isDirectory) {
            val made = DocumentsContract.createDocument(resolver, folderUri(targetParent), source.mime ?: DocumentsContract.Document.MIME_TYPE_DIR, source.name)
                ?: throw IllegalStateException("Could not create ${source.name}")
            val child = Node(targetParent, DocumentsContract.getDocumentId(made), source.name, source.mime, 0L, 0L, true)
            for (entry in list(Folder(source.folder.treeUri, source.documentId))) copyInto(entry, Folder(targetParent.treeUri, child.documentId))
        } else {
            val mime = source.mime ?: "application/octet-stream"
            val made = DocumentsContract.createDocument(resolver, folderUri(targetParent), mime, source.name)
                ?: throw IllegalStateException("Could not create ${source.name}")
            resolver.openInputStream(srcUri)!!.use { input ->
                resolver.openOutputStream(made, "w")!!.use { output -> input.copyTo(output) }
            }
        }
    }

    /**
     * A breadth-first walk of [root] for names containing [query], bounded to [limit] hits and a sane depth, so a huge tree cannot
     * freeze the phone. Hidden names are skipped unless [includeHidden].
     */
    fun search(root: Folder, query: String, includeHidden: Boolean, limit: Int = 400): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val lower = q.lowercase()
        val hits = ArrayList<SearchHit>()
        fun walk(folder: Folder, path: String, depth: Int) {
            if (hits.size >= limit || depth > 10) return
            for (child in listOrEmpty(folder)) {
                if (hits.size >= limit) return
                if (!includeHidden && child.name.startsWith(".")) continue
                if (child.name.lowercase().contains(lower)) hits += SearchHit(child, path)
                if (child.isDirectory) walk(Folder(folder.treeUri, child.documentId), if (path.isEmpty()) child.name else "$path/${child.name}", depth + 1)
            }
        }
        walk(root, "", 0)
        return hits
    }
}
