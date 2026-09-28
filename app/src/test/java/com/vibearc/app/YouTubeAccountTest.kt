package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeAccountTest {
    @Test
    fun `account response maps channel identity`() {
        val response = """
            {"items":[{"id":"channel-1","snippet":{"title":"Smith","thumbnails":{"high":{"url":"https://img.example/avatar.jpg"}}}}]}
        """.trimIndent()

        assertEquals(
            YouTubeAccount("channel-1", "Smith", "https://img.example/avatar.jpg"),
            parseYouTubeAccount(response),
        )
    }

    @Test
    fun `playlist response maps owned playlists and next page`() {
        val response = """
            {
              "nextPageToken":"next-page",
              "items":[{
                "id":"playlist-1",
                "snippet":{"title":"Night Drive","thumbnails":{"medium":{"url":"https://img.example/playlist.jpg"}}},
                "contentDetails":{"itemCount":12}
              }]
            }
        """.trimIndent()

        assertEquals(
            YouTubePlaylistPage(
                playlists = listOf(YouTubePlaylist("playlist-1", "Night Drive", 12, "https://img.example/playlist.jpg")),
                nextPageToken = "next-page",
            ),
            parseYouTubePlaylistPage(response),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `account response rejects missing channel`() {
        parseYouTubeAccount("""{"items":[]}""")
    }

    @Test
    fun `playlist items map to playable tracks`() {
        val response = """
            {
              "nextPageToken":"more",
              "items":[{
                "contentDetails":{"videoId":"video-1"},
                "snippet":{
                  "title":"Night Song",
                  "videoOwnerChannelTitle":"The Artist - Topic",
                  "thumbnails":{"high":{"url":"https://img.example/song.jpg"}}
                }
              }]
            }
        """.trimIndent()

        assertEquals(
            YouTubePlaylistTrackPage(
                tracks = listOf(
                    Track(
                        title = "Night Song",
                        artist = "The Artist",
                        album = "Night Drive",
                        uri = "https://music.youtube.com/watch?v=video-1",
                        artworkUri = "https://img.example/song.jpg",
                        folder = "YouTube Music",
                    ),
                ),
                nextPageToken = "more",
            ),
            parseYouTubePlaylistTracks(response, "Night Drive"),
        )
    }
}
