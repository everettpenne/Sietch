package com.everettpenne.sietch.util

import java.util.Base64

/** One step of a folder's path: the granted tree, the folder's id inside it, and its display name. */
data class FolderStep(val treeUri: String, val documentId: String, val name: String)

/**
 * Folds a folder's identity into one URL-safe token for navigation. SAF document ids are opaque and can contain "/", so they are
 * Base64url-encoded (an alphabet with no "/") and joined with "."; the route never has to guess where one part ends and the next begins.
 * A breadcrumb is a list of steps joined with "~", which neither the encoder nor "." produce, so a whole path rides one route argument.
 */
object RouteCodec {
    private fun enc(s: String) = Base64.getUrlEncoder().encodeToString(s.toByteArray(Charsets.UTF_8))
    private fun dec(s: String) = String(Base64.getUrlDecoder().decode(s), Charsets.UTF_8)

    fun folder(treeUri: String, documentId: String, name: String): String = "${enc(treeUri)}.${enc(documentId)}.${enc(name)}"

    /** A full breadcrumb (root first) as one token. */
    fun path(steps: List<FolderStep>): String = steps.joinToString("~") { folder(it.treeUri, it.documentId, it.name) }

    fun decodeFolder(token: String): Triple<String, String, String>? = runCatching {
        val p = token.split('.')
        require(p.size == 3) { "bad folder token" }
        Triple(dec(p[0]), dec(p[1]), dec(p[2]))
    }.getOrNull()

    /** The steps of [path]; null when the token is malformed. */
    fun decodePath(token: String): List<FolderStep>? = runCatching {
        token.split("~").map { part ->
            val p = part.split('.')
            require(p.size == 3) { "bad folder step" }
            FolderStep(dec(p[0]), dec(p[1]), dec(p[2]))
        }
    }.getOrNull()
}
