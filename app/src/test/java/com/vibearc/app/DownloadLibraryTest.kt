package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class DownloadLibraryTest {
    @Test fun `exports keep the measured container instead of relabeling raw AAC`() {
        assertEquals("m4a",downloadExtension("MP4 audio"))
        assertEquals("aac",downloadExtension("audio/aac"))
        assertEquals("webm",downloadExtension("WebM audio"))
        assertEquals("flac",downloadExtension("FLAC"))
        assertEquals("mp3",downloadExtension("MPEG audio"))
    }
    @Test fun `mobile data is allowed by default and wifi restriction is optional`() {
        assertFalse(DefaultDownloadWifiOnly)
        assertTrue(downloadNetworkAllowed(true,true,false,DefaultDownloadWifiOnly))
        assertTrue(downloadNetworkAllowed(true,true,true,false))
        assertFalse(downloadNetworkAllowed(true,true,false,true))
        assertTrue(downloadNetworkAllowed(true,true,true,true))
        assertFalse(downloadNetworkAllowed(false,true,true,false))
        assertFalse(downloadNetworkAllowed(true,false,false,false))
    }
    @Test fun `search and filters use real saved metadata and lyrics availability`() {
        val a=DownloadEntry(Track("Song A","Singer","Album A","https://example.com/a"),DownloadStatus.COMPLETED,lossless=true,bytes=2048,completedAt=1)
        val b=DownloadEntry(Track("Song B","Other","Album B","https://example.com/b"),DownloadStatus.COMPLETED,bytes=1024,completedAt=2)
        val pending=DownloadEntry(Track("Pending","Singer","","https://example.com/c"),DownloadStatus.FAILED)
        val entries=listOf(a,b,pending)
        assertEquals(listOf(pending,a),selectDownloadEntries(entries,"singer",DownloadFilter.All,DownloadSort.Title,emptySet()))
        assertEquals(listOf(a),selectDownloadEntries(entries,"",DownloadFilter.Lossless,DownloadSort.Recent,emptySet()))
        assertEquals(listOf(b),selectDownloadEntries(entries,"album b",DownloadFilter.Lyrics,DownloadSort.Recent,setOf(b.id)))
        assertTrue(selectDownloadEntries(entries,"",DownloadFilter.Lyrics,DownloadSort.Recent,emptySet()).isEmpty())
        assertEquals(listOf(pending,b,a),selectDownloadEntries(entries,"",DownloadFilter.All,DownloadSort.Recent,emptySet()))
    }
}
