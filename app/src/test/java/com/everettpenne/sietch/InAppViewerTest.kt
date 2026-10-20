package com.everettpenne.sietch

import com.everettpenne.sietch.data.InApp
import com.everettpenne.sietch.data.categoryOf
import com.everettpenne.sietch.data.inAppViewer
import org.junit.Assert.assertEquals
import org.junit.Test

class InAppViewerTest {
    @Test fun picturesVideoAndSoundStayInSietch() {
        assertEquals(InApp.IMAGE, inAppViewer("a.jpg", "image/jpeg"))
        assertEquals(InApp.PLAYER, inAppViewer("clip.mp4", "video/mp4"))
        assertEquals(InApp.PLAYER, inAppViewer("song.flac", "audio/flac"))
    }

    @Test fun everythingElseGoesToAnotherApp() {
        assertEquals(InApp.NONE, inAppViewer("doc.pdf", "application/pdf"))
        assertEquals(InApp.NONE, inAppViewer("logo.svg", "image/svg+xml"))
        assertEquals(InApp.NONE, inAppViewer("notes.txt", "text/plain"))
    }

    @Test fun anUnknownTypeIsJudgedByItsName() {
        assertEquals("video", categoryOf("holiday.MKV", "application/octet-stream", false))
        assertEquals(InApp.IMAGE, inAppViewer("scan.HEIC", "application/octet-stream"))
        assertEquals(InApp.PLAYER, inAppViewer("memo.m4a", null))
    }
}
