package com.vibearc.app
import org.junit.Assert.*
import org.junit.Test

class DownloadCodecTest {
    @Test fun `legacy saved files remain accessible without deleting external originals`() {
        val track=Track("Song","Artist","Album","https://catalog.example/song",isFavorite=true)
        val entry=legacyOfflineEntry(OfflineFile("content://folder/song","Song.mp3",1234,track.uri),listOf(track))
        assertEquals(track.catalogUri,entry.track.catalogUri)
        assertEquals("content://folder/song",entry.localUri)
        assertTrue(entry.track.isFavorite)
        assertFalse(entry.ownsFile)
        assertEquals(DownloadStatus.COMPLETED,entry.status)
    }
    @Test fun `jobs retain metadata progress authorization and offline paths across restart`() {
        val e = DownloadEntry(Track("Song | 夜", "Artist", "Album", "https://catalog.example/a", true, 200_000),
            DownloadStatus.PAUSED, "https://artist.example/audio.flac", "direct", "Artist permission", "flac",
            DownloadQuality.LOSSLESS, 1200, 96000, true, 100, 200, "etag", "file:///saved/audio", "file:///saved/art", "Paused", 123, 456)
        assertEquals(listOf(e),DownloadCodec.decode(DownloadCodec.encode(listOf(e))))
    }
    @Test fun `invalid job state is not silently turned into an empty library`() {
        try { DownloadCodec.decode("[{\"status\":\"made-up\"}]"); fail("Must reject corrupt state") } catch (_: Exception) {}
    }
    @Test fun `actual FLAC bit depth survives restart and old records stay compatible`() {
        val entry=DownloadEntry(Track("Song","Artist","","https://catalog.example/song"),bitDepth=24)
        val json=DownloadCodec.encode(listOf(entry))
        assertEquals(24,DownloadCodec.decode(json).single().bitDepth)
        assertEquals(0,DownloadCodec.decode(json.replace(",\"bitDepth\":24","")).single().bitDepth)
    }
}
