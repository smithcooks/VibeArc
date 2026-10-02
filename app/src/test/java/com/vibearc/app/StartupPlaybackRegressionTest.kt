package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class StartupPlaybackRegressionTest {
    @Test fun `YouTube queues retain catalog identity instead of expired audio URLs`() {
        val catalog = "https://music.youtube.com/watch?v=video-1"
        val resolved = Track("Song", "Artist", "Album", "https://audio.example/expired", sourceUri = catalog)
        assertEquals(catalog, playbackSourceUri(resolved))
        assertEquals("content://media/1", playbackSourceUri(resolved.copy(uri = "content://media/1")))
    }

    @Test fun `watch pages are resolved to audio while local and direct media are untouched`() {
        val catalog = "https://music.youtube.com/watch?v=video-1"
        assertEquals("https://audio.example/live", resolvePlaybackSource(catalog) { track ->
            assertEquals(catalog, track.uri)
            track.copy(uri = "https://audio.example/live", sourceUri = catalog)
        })
        for (uri in listOf("content://media/1", "https://audio.example/song")) {
            assertEquals(uri, resolvePlaybackSource(uri) { error("Must not resolve direct audio") })
        }
        assertThrows(IllegalStateException::class.java) {
            resolvePlaybackSource(catalog) { it }
        }
    }

    @Test fun `startup cache restores playlists feed and artwork without stream URLs`() {
        val track = Track("Song", "Artist", "Album", "https://music.youtube.com/watch?v=video-1",
            artworkUri = "https://img.example/cover.jpg")
        val snapshot = YouTubeBrowseSnapshot(
            YouTubeAccountData(YouTubeAccount("@smith", "Smith", "https://img.example/avatar.jpg"),
                listOf(YouTubePlaylist("PL-1", "Night", 1, "https://img.example/list.jpg"))),
            listOf(YouTubeFeedSection("For you", listOf(track))),
        )
        assertEquals(snapshot, YouTubeBrowseCacheCodec.decode(YouTubeBrowseCacheCodec.encode(snapshot)))
        val resolved = track.copy(uri = "https://audio.example/expired", sourceUri = track.uri)
        val resolvedSnapshot = snapshot.copy(sections = listOf(YouTubeFeedSection("For you", listOf(resolved))))
        assertEquals(snapshot, YouTubeBrowseCacheCodec.decode(YouTubeBrowseCacheCodec.encode(resolvedSnapshot)))
        assertNull(YouTubeBrowseCacheCodec.decode("broken"))
    }

    @Test fun `search prefetch does not duplicate the chosen song in its queue`() {
        val catalog = Track("Song", "Artist", "Album", "https://music.youtube.com/watch?v=video-1")
        val resolved = catalog.copy(uri = "https://audio.example/live", sourceUri = catalog.uri)
        assertEquals(listOf(catalog.uri), playbackQueue(listOf(catalog), resolved).map(Track::catalogUri))
        val downloaded = resolved.copy(uri = "content://media/1")
        assertEquals(listOf(downloaded), playbackQueue(listOf(catalog), downloaded))
    }
}
