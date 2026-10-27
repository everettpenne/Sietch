package com.everettpenne.sietch

import com.everettpenne.sietch.data.*
import com.everettpenne.sietch.util.formatBytes
import com.everettpenne.sietch.util.opSummary
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LogicTest {
    @get:Rule val tmp = TemporaryFolder()
    private val fs = LocalFs()

    private fun file(dir: File, name: String, text: String = "x"): File = File(dir, name).also { it.parentFile?.mkdirs(); it.writeText(text) }

    // ---- LocalFs ----
    @Test fun uniqueNameKeepsExtension() {
        val d = tmp.newFolder()
        assertEquals("a.txt", fs.uniqueName(d, "a.txt"))
        file(d, "a.txt"); assertEquals("a (1).txt", fs.uniqueName(d, "a.txt"))
        file(d, "a (1).txt"); assertEquals("a (2).txt", fs.uniqueName(d, "a.txt"))
        file(d, "noext"); assertEquals("noext (1)", fs.uniqueName(d, "noext"))
        file(d, ".hidden"); assertEquals(".hidden (1)", fs.uniqueName(d, ".hidden"))
    }

    @Test fun namesAreValidated() {
        for (bad in listOf("", "  ", ".", "..", "a/b", "a\u0000b")) assertFalse("'$bad'", fs.validName(bad))
        assertTrue(fs.validName("Holiday photos 2026"))
        assertNull(fs.createFolder(tmp.newFolder(), "../escape"))
    }

    @Test fun renameRefusesToOverwrite() {
        val d = tmp.newFolder(); val a = file(d, "a.txt", "A"); file(d, "b.txt", "B")
        assertFalse(fs.rename(a, "b.txt"))
        assertEquals("A", a.readText()); assertEquals("B", File(d, "b.txt").readText())
        assertTrue(fs.rename(a, "c.txt")); assertTrue(File(d, "c.txt").exists()); assertFalse(a.exists())
    }

    @Test fun copyKeepsSourceAndAvoidsClash() {
        val src = tmp.newFolder(); val dst = tmp.newFolder()
        val f = file(src, "a.txt", "hello"); file(dst, "a.txt", "old")
        val copy = fs.copy(f, dst)!!
        assertEquals("a (1).txt", copy.name); assertEquals("hello", copy.readText())
        assertEquals("old", File(dst, "a.txt").readText()); assertTrue(f.exists())
    }

    @Test fun copyAndMoveFolderRecursively() {
        val src = tmp.newFolder(); val dst = tmp.newFolder()
        file(src, "dir/one.txt", "1"); file(src, "dir/deep/two.txt", "2")
        val copy = fs.copy(File(src, "dir"), dst)!!
        assertEquals("2", File(copy, "deep/two.txt").readText()); assertTrue(File(src, "dir/one.txt").exists())
        val moved = fs.move(File(src, "dir"), dst)!!
        assertEquals("dir (1)", moved.name); assertFalse(File(src, "dir").exists()); assertEquals("1", File(moved, "one.txt").readText())
    }

    @Test fun folderCannotGoIntoItself() {
        val d = tmp.newFolder(); val sub = File(d, "sub").also { it.mkdir() }
        assertNull(fs.copy(d, sub)); assertNull(fs.move(d, sub)); assertNull(fs.copy(d, d))
        assertTrue(fs.isInside(d, sub)); assertFalse(fs.isInside(sub, d))
        // a sibling whose name merely starts the same is not "inside"
        val sibling = File(d.parentFile, d.name + "2").also { it.mkdir() }
        assertFalse(fs.isInside(d, sibling))
    }

    @Test fun moveToSameFolderIsANoOp() {
        val d = tmp.newFolder(); val f = file(d, "a.txt")
        assertEquals(f, fs.move(f, d)); assertTrue(f.exists()); assertEquals(1, d.listFiles()!!.size)
    }

    @Test fun listingUnreadableFolderThrows() {
        assertThrows(java.io.IOException::class.java) { fs.list(File(tmp.root, "missing")) }
    }

    @Test fun sizeOfFolderSumsFiles() {
        val d = tmp.newFolder(); file(d, "a", "123"); file(d, "s/b", "45")
        assertEquals(5L, fs.sizeOf(d))
    }

    // ---- TrashStore ----
    private var clock = 1_000_000_000L
    private fun store(): Triple<TrashStore, File, File> {
        val bin = File(tmp.newFolder(), "bin"); val idx = File(tmp.newFolder(), "idx.json")
        return Triple(TrashStore(bin, idx, fs) { clock }, bin, idx)
    }

    @Test fun trashAndRestoreRoundTrip() {
        val (t, bin, _) = store(); val home = tmp.newFolder()
        val f = file(home, "doc.txt", "keep me")
        assertTrue(t.trash(f)); assertFalse(f.exists()); assertEquals(1, t.items.value.size)
        val item = t.items.value[0]
        assertEquals("doc.txt", item.name); assertEquals(7L, item.size); assertTrue(File(bin, item.id).exists())
        val back = t.restore(item.id)!!
        assertEquals(f.absolutePath, back.absolutePath); assertEquals("keep me", back.readText()); assertTrue(t.items.value.isEmpty())
    }

    @Test fun restoreDoesNotOverwriteANewerFile() {
        val (t, _, _) = store(); val home = tmp.newFolder()
        val f = file(home, "a.txt", "old"); t.trash(f)
        file(home, "a.txt", "new")
        val back = t.restore(t.items.value[0].id)!!
        assertEquals("a (1).txt", back.name); assertEquals("old", back.readText()); assertEquals("new", File(home, "a.txt").readText())
    }

    @Test fun restoreRecreatesAMissingFolder() {
        val (t, _, _) = store(); val home = tmp.newFolder()
        val f = file(home, "gone/inner/a.txt", "x"); t.trash(f)
        File(home, "gone").deleteRecursively()
        assertEquals("x", t.restore(t.items.value[0].id)!!.readText())
    }

    @Test fun trashingAFolderKeepsItsContents() {
        val (t, _, _) = store(); val home = tmp.newFolder()
        file(home, "d/a.txt", "1"); file(home, "d/s/b.txt", "22")
        assertTrue(t.trash(File(home, "d"))); assertFalse(File(home, "d").exists())
        assertTrue(t.items.value[0].isDirectory); assertEquals(3L, t.items.value[0].size)
        t.restore(t.items.value[0].id); assertEquals("22", File(home, "d/s/b.txt").readText())
    }

    @Test fun trashingAMissingFileFailsCleanly() {
        val (t, _, _) = store(); assertFalse(t.trash(File(tmp.root, "nope"))); assertTrue(t.items.value.isEmpty())
    }

    @Test fun purgeRemovesOnlyExpiredItems() {
        val (t, bin, _) = store(); val home = tmp.newFolder()
        t.trash(file(home, "old.txt")); val oldId = t.items.value[0].id
        clock += 20 * 86_400_000L
        t.trash(file(home, "new.txt"))
        assertEquals(0, t.purgeExpired(30)); assertEquals(2, t.items.value.size)
        clock += 15 * 86_400_000L   // old is now 35 days, new is 15
        assertEquals(1, t.purgeExpired(30))
        assertEquals(listOf("new.txt"), t.items.value.map { it.name }); assertFalse(File(bin, oldId).exists())
    }

    @Test fun daysLeftCountsDown() {
        val (t, _, _) = store(); t.trash(file(tmp.newFolder(), "a")); val item = t.items.value[0]
        assertEquals(30, t.daysLeft(item, 30)); clock += 3 * 86_400_000L + 5000; assertEquals(27, t.daysLeft(item, 30))
        clock += 100 * 86_400_000L; assertEquals(0, t.daysLeft(item, 30))
    }

    @Test fun emptyAndDeleteForever() {
        val (t, bin, _) = store(); val home = tmp.newFolder()
        t.trash(file(home, "a")); t.trash(file(home, "b")); val first = t.items.value[0]
        assertTrue(t.deleteForever(first.id)); assertEquals(1, t.items.value.size); assertFalse(File(bin, first.id).exists())
        assertEquals(0, t.empty()); assertTrue(t.items.value.isEmpty())
    }

    @Test fun indexSurvivesARestartAndDropsOrphans() {
        val bin = File(tmp.newFolder(), "bin"); val idx = File(tmp.newFolder(), "idx.json"); val home = tmp.newFolder()
        val a = TrashStore(bin, idx, fs) { clock }
        a.trash(file(home, "keep")); a.trash(file(home, "lost"))
        File(bin, a.items.value.first { it.name == "lost" }.id).deleteRecursively()   // payload vanished
        val b = TrashStore(bin, idx, fs) { clock }
        assertEquals(listOf("keep"), b.items.value.map { it.name })
    }

    @Test fun corruptIndexStartsEmptyInsteadOfCrashing() {
        val idx = File(tmp.newFolder(), "idx.json").also { it.writeText("{not json") }
        assertTrue(TrashStore(File(tmp.newFolder(), "bin"), idx, fs) { clock }.items.value.isEmpty())
    }

    // ---- MediaIndex selections ----
    @Test fun likeEscaping() {
        assertEquals("100\\%\\_done", escapeLike("100%_done"))
        assertEquals("a\\\\b", escapeLike("a\\b"))
        val s = searchSelection("  50% off ")
        assertEquals("%50\\% off%", s.args[0]); assertTrue(s.where.contains("ESCAPE"))
    }

    @Test fun everyCategoryHidesHiddenAndAppData() {
        for (c in Category.entries) {
            val s = selectionFor(c, "/storage/emulated/0")
            assertTrue("$c", s.where.contains("NOT LIKE '%/.%'")); assertTrue("$c", s.where.contains("/Android/data/"))
            assertEquals("$c args match placeholders", s.where.count { it == '?' }, s.args.size)
        }
        assertEquals("/storage/emulated/0/Download/%", selectionFor(Category.DOWNLOADS, "/storage/emulated/0").args[0])
        assertEquals(listOf(Category.DOCUMENTS, Category.DOWNLOADS, Category.APPS, Category.ARCHIVES), Category.entries.filter { it.needsAllFiles })
    }

    // ---- sorting, kinds, formatting ----
    private fun n(name: String, dir: Boolean = false, size: Long = 0, mod: Long = 0) = Node("/x/$name", name, dir, size, mod)

    @Test fun sortFoldersFirstThenKey() {
        val list = listOf(n("b.txt", size = 5), n("Zeta", dir = true), n("a.txt", size = 9), n("alpha", dir = true))
        assertEquals(listOf("alpha", "Zeta", "a.txt", "b.txt"), sortNodes(list, SortBy.NAME, SortOrder.ASC, true).map { it.name })
        assertEquals(listOf("a.txt", "b.txt", "alpha", "Zeta").let { listOf("a.txt", "alpha", "b.txt", "Zeta") }, sortNodes(list, SortBy.NAME, SortOrder.ASC, false).map { it.name })
        assertEquals(listOf("a.txt", "b.txt"), sortNodes(list, SortBy.SIZE, SortOrder.DESC, true).drop(2).map { it.name })
    }

    @Test fun kinds() {
        assertEquals("image", categoryOf("a.HEIC", null, false)); assertEquals("pdf", categoryOf("a.pdf", null, false))
        assertEquals("app", categoryOf("x.apk", "application/octet-stream", false)); assertEquals("folder", categoryOf("x.apk", null, true))
        assertEquals(InApp.IMAGE, inAppViewer("a.png", "image/png")); assertEquals(InApp.NONE, inAppViewer("a.svg", "image/svg+xml"))
        assertEquals(InApp.PLAYER, inAppViewer("a.mkv", null)); assertEquals(InApp.WEB, inAppViewer("a.html", "text/html"))
    }

    @Test fun formatting() {
        assertEquals("0 B", formatBytes(0)); assertEquals("1.0 KB", formatBytes(1024)); assertEquals("1.5 MB", formatBytes(1_572_864)); assertEquals("—", formatBytes(-1))
        assertEquals("Moved 3", opSummary("Moved", 3, 3)); assertEquals("Moved 2, 1 failed", opSummary("Moved", 2, 3)); assertEquals("Moved failed", opSummary("Moved", 0, 2))
    }

    @Test fun breadcrumbFromRoot() {
        val root = File("/storage/emulated/0")
        val crumbs = com.everettpenne.sietch.ui.screens.breadcrumb(File("/storage/emulated/0/DCIM/Camera"), root)
        assertEquals(listOf("/storage/emulated/0", "/storage/emulated/0/DCIM", "/storage/emulated/0/DCIM/Camera"), crumbs.map { it.path })
        assertEquals(listOf("/storage/emulated/0"), com.everettpenne.sietch.ui.screens.breadcrumb(root, root).map { it.path })
    }
}
