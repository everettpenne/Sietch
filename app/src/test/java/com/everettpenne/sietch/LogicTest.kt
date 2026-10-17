package com.everettpenne.sietch

import com.everettpenne.sietch.data.*
import com.everettpenne.sietch.util.FolderStep
import com.everettpenne.sietch.util.RouteCodec
import com.everettpenne.sietch.util.formatBytes
import com.everettpenne.sietch.util.opSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogicTest {
    private fun node(id: String, name: String, mime: String? = null, size: Long = 0, modified: Long = 0, dir: Boolean = false) =
        Node(Folder("content://x/tree/t", "t"), id, name, mime, size, modified, dir)

    @Test
    fun categories() {
        assertEquals("folder", categoryOf("a", null, true))
        assertEquals("image", categoryOf("photo.png", "image/png", false))
        assertEquals("video", categoryOf("clip.mp4", "video/mp4", false))
        assertEquals("audio", categoryOf("song.mp3", "audio/mpeg", false))
        assertEquals("pdf", categoryOf("report.pdf", null, false))
        assertEquals("text", categoryOf("notes.txt", "text/plain", false))
        assertEquals("archive", categoryOf("archive.zip", null, false))
        assertEquals("app", categoryOf("thing.apk", null, false))
        assertEquals("document", categoryOf("sheet.xlsx", null, false))
        assertEquals("other", categoryOf("blob.xyz", null, false))
    }

    @Test
    fun kindLabelsAreHuman() {
        assertEquals("Folder", kindLabel("Downloads", null, true))
        assertEquals("Image", kindLabel("photo.png", "image/png", false))
        assertEquals("PDF", kindLabel("report.pdf", null, false))
        assertEquals("File", kindLabel("blob.xyz", null, false))
    }

    @Test
    fun byteSizes() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("512 B", formatBytes(512))
        assertEquals("1.5 KB", formatBytes(1536))
        assertEquals("2.0 KB", formatBytes(2048))
        assertEquals("1.0 MB", formatBytes(1_048_576))
        assertEquals("1.0 GB", formatBytes(1_073_741_824))
    }

    @Test
    fun sortKeepsFoldersFirstAndHonoursDirection() {
        val items = listOf(
            node("1", "beta.txt", "text/plain", size = 20),
            node("2", "Alpha", dir = true),
            node("3", "alpha.txt", "text/plain", size = 10),
            node("4", "Beta", dir = true),
        )
        val byNameAsc = sortNodes(items, SortBy.NAME, SortOrder.ASC, foldersFirst = true).map { it.name }
        assertEquals(listOf("Alpha", "Beta", "alpha.txt", "beta.txt"), byNameAsc)

        val byNameDesc = sortNodes(items, SortBy.NAME, SortOrder.DESC, foldersFirst = true).map { it.name }
        assertEquals(listOf("Beta", "Alpha", "beta.txt", "alpha.txt"), byNameDesc) // folders stay on top, sorted descending too

        val bySize = sortNodes(items, SortBy.SIZE, SortOrder.ASC, foldersFirst = true).map { it.name }
        assertEquals(listOf("Alpha", "Beta", "alpha.txt", "beta.txt"), bySize) // 10 then 20 bytes

        val noFoldersFirst = sortNodes(items, SortBy.NAME, SortOrder.ASC, foldersFirst = false).map { it.name }
        assertEquals(listOf("Alpha", "alpha.txt", "Beta", "beta.txt"), noFoldersFirst) // a pure name sort, folders mixed in
    }

    @Test
    fun folderRouteSurvivesSlashesAndSpaces() {
        val treeUri = "content://com.android.externalstorage.documents/tree/primary%3ADownload"
        val documentId = "primary:Download/a folder/with slashes"
        val name = "My Folder"
        val token = RouteCodec.folder(treeUri, documentId, name)
        val decoded = RouteCodec.decodeFolder(token)
        assertEquals(treeUri, decoded?.first)
        assertEquals(documentId, decoded?.second)
        assertEquals(name, decoded?.third)
    }

    @Test
    fun badRouteIsNull() {
        assertNull(RouteCodec.decodeFolder("only-one-part"))
        assertNull(RouteCodec.decodeFolder("a.b.c.d"))
    }

    @Test
    fun folderPathRoundTripsAndPreservesOrder() {
        val steps = listOf(
            FolderStep("content://x/tree/t", "primary:Download", "Download"),
            FolderStep("content://x/tree/t", "primary:Download/a folder/with slashes", "a folder"),
            FolderStep("content://x/tree/t", "primary:Download/a folder/with slashes/2024", "2024"),
        )
        val token = RouteCodec.path(steps)
        assertEquals(steps, RouteCodec.decodePath(token))
    }

    @Test
    fun folderPathBadInputsAreNull() {
        assertNull(RouteCodec.decodePath(""))
        assertNull(RouteCodec.decodePath("a.b.c")) // not valid base64url parts
        assertNull(RouteCodec.decodePath("Download.folder")) // two parts, not three per step
    }

    @Test
    fun operationSummariesAreHonest() {
        assertEquals("Copied 3", opSummary("Copied", 3, 3))
        assertEquals("Copied 2, 1 failed", opSummary("Copied", 2, 3))
        assertEquals("Deleted failed", opSummary("Deleted", 0, 3))
        assertEquals("Moved 0", opSummary("Moved", 0, 0))
    }
}
