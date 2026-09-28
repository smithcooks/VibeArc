package com.vibearc.app

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal data class YouTubeAccount(
    val channelId: String,
    val displayName: String,
    val artworkUrl: String,
)

internal data class YouTubePlaylist(
    val id: String,
    val title: String,
    val itemCount: Int,
    val artworkUrl: String,
)

internal data class YouTubePlaylistPage(
    val playlists: List<YouTubePlaylist>,
    val nextPageToken: String?,
)

internal data class YouTubePlaylistTrackPage(
    val tracks: List<Track>,
    val nextPageToken: String?,
)

internal data class YouTubeAccountData(
    val account: YouTubeAccount,
    val playlists: List<YouTubePlaylist>,
)

internal fun parseYouTubeAccount(json: String): YouTubeAccount {
    val item = parseRoot(json).getArray("items").firstOrNull() as? JsonObject
        ?: throw IllegalArgumentException("No YouTube channel was found")
    val snippet = item.getObject("snippet")
    return YouTubeAccount(
        channelId = item.getString("id", "").required("channel id"),
        displayName = snippet.getString("title", "").required("channel title"),
        artworkUrl = snippet.thumbnailUrl(),
    )
}

internal fun parseYouTubePlaylistPage(json: String): YouTubePlaylistPage {
    val root = parseRoot(json)
    val playlists = root.getArray("items").mapNotNull { value ->
        val item = value as? JsonObject ?: return@mapNotNull null
        val snippet = item.getObject("snippet")
        val id = item.getString("id", "")
        val title = snippet.getString("title", "")
        if (id.isBlank() || title.isBlank()) return@mapNotNull null
        YouTubePlaylist(
            id = id,
            title = title,
            itemCount = ((item.getObject("contentDetails")["itemCount"] as? Number)?.toInt() ?: 0)
                .coerceAtLeast(0),
            artworkUrl = snippet.thumbnailUrl(),
        )
    }.take(50)
    return YouTubePlaylistPage(playlists, root.getString("nextPageToken", "").ifBlank { null })
}

internal fun parseYouTubePlaylistTracks(json: String, playlistTitle: String): YouTubePlaylistTrackPage {
    val root = parseRoot(json)
    val tracks = root.getArray("items").mapNotNull { value ->
        val item = value as? JsonObject ?: return@mapNotNull null
        val snippet = item.getObject("snippet")
        val videoId = item.getObject("contentDetails").getString("videoId", "")
        val title = snippet.getString("title", "")
        if (videoId.isBlank() || title.isBlank() || title.startsWith('[')) return@mapNotNull null
        Track(
            title = title,
            artist = snippet.getString("videoOwnerChannelTitle", "YouTube Music")
                .removeSuffix(" - Topic").ifBlank { "YouTube Music" },
            album = playlistTitle,
            uri = "https://music.youtube.com/watch?v=$videoId",
            artworkUri = snippet.thumbnailUrl(),
            folder = "YouTube Music",
        )
    }.take(50)
    return YouTubePlaylistTrackPage(tracks, root.getString("nextPageToken", "").ifBlank { null })
}

private fun parseRoot(json: String): JsonObject = runCatching { JsonParser.`object`().from(json) }
    .getOrElse { throw IllegalArgumentException("Invalid YouTube response", it) }

private fun String.required(label: String): String = also {
    require(isNotBlank()) { "Missing $label" }
}

private fun JsonObject.thumbnailUrl(): String {
    val thumbnails = getObject("thumbnails")
    return listOf("maxres", "standard", "high", "medium", "default")
        .firstNotNullOfOrNull { key -> thumbnails.getObject(key, null)?.getString("url", "")?.takeIf(String::isNotBlank) }
        .orEmpty()
}

internal object YouTubeAccountApi {
    private const val ApiBase = "https://www.googleapis.com/youtube/v3"
    private const val MaxResponseBytes = 2 * 1024 * 1024
    private const val MaxPlaylistPages = 20

    fun load(accessToken: String): YouTubeAccountData {
        require(accessToken.isNotBlank()) { "Missing authorization" }
        val account = parseYouTubeAccount(get("$ApiBase/channels?part=snippet&mine=true", accessToken))
        val playlists = buildList {
            var pageToken: String? = null
            repeat(MaxPlaylistPages) {
                val suffix = pageToken?.let { "&pageToken=${URLEncoder.encode(it, Charsets.UTF_8.name())}" }.orEmpty()
                val page = parseYouTubePlaylistPage(
                    get("$ApiBase/playlists?part=snippet,contentDetails&mine=true&maxResults=50$suffix", accessToken),
                )
                addAll(page.playlists)
                pageToken = page.nextPageToken
                if (pageToken == null) return@buildList
            }
        }
        return YouTubeAccountData(account, playlists.distinctBy(YouTubePlaylist::id))
    }

    fun loadPlaylist(accessToken: String, playlist: YouTubePlaylist): List<Track> {
        require(accessToken.isNotBlank()) { "Missing authorization" }
        require(playlist.id.isNotBlank()) { "Missing playlist" }
        return buildList {
            var pageToken: String? = null
            repeat(MaxPlaylistPages) {
                val suffix = pageToken?.let { "&pageToken=${URLEncoder.encode(it, Charsets.UTF_8.name())}" }.orEmpty()
                val page = parseYouTubePlaylistTracks(
                    get(
                        "$ApiBase/playlistItems?part=snippet,contentDetails&playlistId=${URLEncoder.encode(playlist.id, Charsets.UTF_8.name())}&maxResults=50$suffix",
                        accessToken,
                    ),
                    playlist.title,
                )
                addAll(page.tracks)
                pageToken = page.nextPageToken
                if (pageToken == null) return@buildList
            }
        }.distinctBy(Track::uri)
    }

    private fun get(url: String, accessToken: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            check(code in 200..299) { "YouTube request failed ($code)" }
            return connection.inputStream.use { it.readUtf8Limited(MaxResponseBytes) }
        } finally {
            connection.disconnect()
        }
    }
}
