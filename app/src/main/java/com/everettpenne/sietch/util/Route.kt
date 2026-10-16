package com.everettpenne.sietch.util

import java.util.Base64

/**
 * Folds a folder's identity into one URL-safe token for navigation. SAF document ids are opaque and can contain "/", so they are
 * Base64url-encoded (an alphabet with no "/") and joined with "."; the route never has to guess where one part ends and the next begins.
 */
object RouteCodec {
    private fun enc(s: String) = Base64.getUrlEncoder().encodeToString(s.toByteArray(Charsets.UTF_8))
    private fun dec(s: String) = String(Base64.getUrlDecoder().decode(s), Charsets.UTF_8)

    fun folder(treeUri: String, documentId: String, name: String): String = "${enc(treeUri)}.${enc(documentId)}.${enc(name)}"

    fun decodeFolder(token: String): Triple<String, String, String>? = runCatching {
        val p = token.split('.')
        require(p.size == 3) { "bad folder token" }
        Triple(dec(p[0]), dec(p[1]), dec(p[2]))
    }.getOrNull()
}
