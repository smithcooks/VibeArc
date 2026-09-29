package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeAccountTest {
    @Test
    fun `cached account state round trips without credentials`() {
        val state = YouTubeAccountState(
            account = YouTubeAccount("channel-1", "Smith", "https://img.example/avatar.jpg"),
            selectedPlaylistIds = setOf("playlist-b", "playlist-a"),
        )

        val encoded = YouTubeAccountStateCodec.encode(state)

        assertEquals(state, YouTubeAccountStateCodec.decode(encoded))
        assertEquals(false, encoded.contains("access_token"))
    }

    @Test
    fun `cached account state rejects damaged data`() {
        assertEquals(null, YouTubeAccountStateCodec.decode("not-a-state"))
    }

    @Test
    fun `playlist diff separates pull push and unsupported local tracks`() {
        val remote = listOf(
            Track("Shared", "Artist", "Mix", "https://music.youtube.com/watch?v=shared"),
            Track("Remote", "Artist", "Mix", "https://music.youtube.com/watch?v=remote"),
        )

        assertEquals(
            YouTubePlaylistDiff(
                remoteOnlyTracks = listOf(remote[1]),
                localOnlyVideoIds = listOf("local"),
                unsupportedLocalUris = listOf("content://music/device-only"),
            ),
            previewYouTubePlaylistSync(
                listOf(
                    "https://music.youtube.com/watch?v=shared",
                    "https://www.youtube.com/watch?v=local&list=mix",
                    "content://music/device-only",
                ),
                remote,
            ),
        )
    }

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

    @Test
    fun `created playlist response requires an id`() {
        assertEquals("playlist-new", parseCreatedYouTubePlaylistId("""{"id":"playlist-new"}"""))
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
                "id":"item-1",
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

    @Test
    fun `playlist item response retains ids needed for safe deletion`() {
        val response = """
            {"items":[{
              "id":"item-remote",
              "contentDetails":{"videoId":"video-remote"},
              "snippet":{"title":"Remote song","videoOwnerChannelTitle":"Artist"}
            }]}
        """.trimIndent()

        assertEquals(
            listOf("item-remote"),
            parseYouTubePlaylistItems(response, "Mix").items.map(YouTubePlaylistItem::id),
        )
    }

    @Test
    fun `two way sync plan adds local videos and names remote removals`() {
        val remote = listOf(
            YouTubePlaylistItem("item-shared", Track("Shared", "Artist", "Mix", "https://music.youtube.com/watch?v=shared")),
            YouTubePlaylistItem("item-remote", Track("Remote", "Artist", "Mix", "https://music.youtube.com/watch?v=remote")),
        )

        assertEquals(
            YouTubePlaylistSyncPlan(
                addVideoIds = listOf("local"),
                removeItemIds = listOf("item-remote"),
                remoteOnlyTracks = listOf(remote[1].track),
                unsupportedLocalUris = listOf("content://music/device-only"),
            ),
            planYouTubePlaylistSync(
                localTrackUris = listOf(
                    "https://music.youtube.com/watch?v=shared",
                    "https://www.youtube.com/watch?v=local",
                    "content://music/device-only",
                ),
                remoteItems = remote,
            ),
        )
    }
}
