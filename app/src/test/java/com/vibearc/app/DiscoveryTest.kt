package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveryTest {
    private val local = Track("Local", "Artist", "Album", "content://local")
    private val liked = Track("Liked", "Artist", "Album", "content://liked", isFavorite = true)
    private val online = Track("Online", "Artist 2", "YouTube Music", "https://music.youtube.com/watch?v=online")

    @Test
    fun `recommendation blend prefers online sources and removes duplicates`() {
        assertEquals(
            listOf(online.uri, local.uri, liked.uri),
            blendDiscoveryTracks(
                youtube = listOf(online),
                lastFm = listOf(online.copy(title = "Duplicate"), local),
                recent = listOf(local),
                library = listOf(liked),
            ).map(Track::uri),
        )
    }

    @Test
    fun `generator produces playable queues instead of search shortcuts`() {
        assertEquals(
            listOf(liked.uri, local.uri, online.uri),
            generatedPlaylist(
                choice = GeneratorChoice.TopTracks,
                library = listOf(local, liked),
                recent = listOf(local),
                recommendations = listOf(online),
                radio = emptyList(),
                seed = local,
            ).map(Track::uri),
        )
        assertEquals(
            listOf(online.uri),
            generatedPlaylist(
                choice = GeneratorChoice.SongRadio,
                library = emptyList(),
                recent = emptyList(),
                recommendations = emptyList(),
                radio = listOf(online),
                seed = local,
            ).map(Track::uri),
        )
    }
}
