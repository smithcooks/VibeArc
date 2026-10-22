package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class DownloadExportTest {
    @Test fun `WebM avoids audio-only MediaStore on every supported scoped storage version`() {
        for(sdk in 29..36) {
            val location=downloadExportLocation(DownloadFormat.WEBM,sdk)
            assertEquals(DownloadExportCollection.DOWNLOADS,location.collection)
            assertEquals("Download/VibeArc/",location.relativePath)
        }
    }

    @Test fun `MP3 and legacy exports keep Music location`() {
        for(sdk in 26..36) {
            val location=downloadExportLocation(DownloadFormat.MP3,sdk)
            assertEquals(DownloadExportCollection.AUDIO,location.collection)
            assertEquals("Music/VibeArc/",location.relativePath)
        }
        for(sdk in 26..28) {
            assertEquals("Music/VibeArc/",downloadExportLocation(DownloadFormat.WEBM,sdk).relativePath)
        }
    }

    @Test fun `export compatibility never relabels or converts genuine WebM`() {
        assertEquals("audio/webm",DownloadFormat.WEBM.mime)
        assertEquals("webm",DownloadFormat.WEBM.extension)
        assertEquals("audio/mpeg",DownloadFormat.MP3.mime)
        assertEquals("mp3",DownloadFormat.MP3.extension)
    }
}
