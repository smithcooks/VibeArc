package com.vibearc.app

import android.content.Context
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

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

internal data class YouTubePlaylistItem(val id: String, val track: Track)

internal data class YouTubePlaylistItemPage(
    val items: List<YouTubePlaylistItem>,
    val nextPageToken: String?,
)

internal data class YouTubePlaylistSyncPlan(
    val addVideoIds: List<String>,
    val removeItemIds: List<String>,
    val remoteOnlyTracks: List<Track>,
    val unsupportedLocalUris: List<String>,
)

internal fun planYouTubePlaylistSync(
    localTrackUris: List<String>,
    remoteItems: List<YouTubePlaylistItem>,
): YouTubePlaylistSyncPlan {
    val localVideoIds = localTrackUris.mapNotNull(::youtubeVideoId).distinct()
    val remoteByVideoId = remoteItems.mapNotNull { item ->
        youtubeVideoId(item.track.uri)?.let { it to item }
    }.toMap()
    return YouTubePlaylistSyncPlan(
        addVideoIds = localVideoIds.filterNot(remoteByVideoId::containsKey),
        removeItemIds = remoteByVideoId.filterKeys { it !in localVideoIds }.values.map(YouTubePlaylistItem::id),
        remoteOnlyTracks = remoteByVideoId.filterKeys { it !in localVideoIds }.values.map(YouTubePlaylistItem::track),
        unsupportedLocalUris = localTrackUris.filter { youtubeVideoId(it) == null }.distinct(),
    )
}

internal data class YouTubeAccountData(
    val account: YouTubeAccount,
    val playlists: List<YouTubePlaylist>,
)

internal data class YouTubeAccountState(
    val account: YouTubeAccount,
    val selectedPlaylistIds: Set<String> = emptySet(),
)

internal data class YouTubePlaylistDiff(
    val remoteOnlyTracks: List<Track>,
    val localOnlyVideoIds: List<String>,
    val unsupportedLocalUris: List<String>,
)

internal fun previewYouTubePlaylistSync(
    localTrackUris: List<String>,
    remoteTracks: List<Track>,
): YouTubePlaylistDiff {
    val localVideoIds = localTrackUris.mapNotNull(::youtubeVideoId).distinct()
    val remoteVideoIds = remoteTracks.mapNotNull { youtubeVideoId(it.uri) }.toSet()
    return YouTubePlaylistDiff(
        remoteOnlyTracks = remoteTracks.filter { track ->
            youtubeVideoId(track.uri)?.let { it !in localVideoIds } == true
        }.distinctBy(Track::uri),
        localOnlyVideoIds = localVideoIds.filterNot(remoteVideoIds::contains),
        unsupportedLocalUris = localTrackUris.filter { youtubeVideoId(it) == null }.distinct(),
    )
}

private fun youtubeVideoId(value: String): String? = runCatching {
    val uri = URI(value)
    require(uri.scheme == "https" && uri.host in setOf("music.youtube.com", "www.youtube.com", "youtube.com"))
    uri.rawQuery.orEmpty().split('&').firstNotNullOfOrNull { part ->
        val fields = part.split('=', limit = 2)
        if (fields.firstOrNull() == "v") URLDecoder.decode(fields.getOrNull(1).orEmpty(), UTF_8.name())
            .takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,64}")) } else null
    }
}.getOrNull()

internal object YouTubeAccountStateCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(state: YouTubeAccountState): String = (
        listOf(state.account.channelId, state.account.displayName, state.account.artworkUrl) +
            state.selectedPlaylistIds.filter(String::isNotBlank).distinct().sorted()
        ).joinToString("\n") { encoder.encodeToString(it.toByteArray(UTF_8)) }

    fun decode(value: String): YouTubeAccountState? = runCatching {
        val fields = value.lineSequence().filter(String::isNotBlank)
            .map { String(decoder.decode(it), UTF_8) }.toList()
        require(fields.size >= 3 && fields[0].isNotBlank() && fields[1].isNotBlank())
        YouTubeAccountState(
            YouTubeAccount(fields[0], fields[1], fields[2]),
            fields.drop(3).filter(String::isNotBlank).toSet(),
        )
    }.getOrNull()
}

private const val YouTubeAccountPreferences = "youtube_account"
private const val YouTubeAccountStateKey = "state"

internal fun Context.loadYouTubeAccountState(): YouTubeAccountState? = YouTubeAccountStateCodec.decode(
    getSharedPreferences(YouTubeAccountPreferences, Context.MODE_PRIVATE)
        .getString(YouTubeAccountStateKey, "").orEmpty(),
)

internal fun Context.saveYouTubeAccountState(state: YouTubeAccountState) {
    getSharedPreferences(YouTubeAccountPreferences, Context.MODE_PRIVATE).edit()
        .putString(YouTubeAccountStateKey, YouTubeAccountStateCodec.encode(state)).apply()
}

internal fun Context.clearYouTubeAccountState() {
    getSharedPreferences(YouTubeAccountPreferences, Context.MODE_PRIVATE).edit().clear().apply()
}

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
    val page = parseYouTubePlaylistItems(json, playlistTitle)
    return YouTubePlaylistTrackPage(page.items.map(YouTubePlaylistItem::track), page.nextPageToken)
}

internal fun parseYouTubePlaylistItems(json: String, playlistTitle: String): YouTubePlaylistItemPage {
    val root = parseRoot(json)
    val items = root.getArray("items").mapNotNull { value ->
        val item = value as? JsonObject ?: return@mapNotNull null
        val snippet = item.getObject("snippet")
        val itemId = item.getString("id", "")
        val videoId = item.getObject("contentDetails").getString("videoId", "")
        val title = snippet.getString("title", "")
        if (itemId.isBlank() || videoId.isBlank() || title.isBlank() || title.startsWith('[')) return@mapNotNull null
        YouTubePlaylistItem(
            itemId,
            Track(
                title = title,
                artist = snippet.getString("videoOwnerChannelTitle", "YouTube Music")
                    .removeSuffix(" - Topic").ifBlank { "YouTube Music" },
                album = playlistTitle,
                uri = "https://music.youtube.com/watch?v=$videoId",
                artworkUri = snippet.thumbnailUrl(),
                folder = "YouTube Music",
            ),
        )
    }.take(50)
    return YouTubePlaylistItemPage(items, root.getString("nextPageToken", "").ifBlank { null })
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
        return loadPlaylistItems(accessToken, playlist).map(YouTubePlaylistItem::track)
            .distinctBy(Track::uri)
    }

    fun loadPlaylistItems(accessToken: String, playlist: YouTubePlaylist): List<YouTubePlaylistItem> {
        require(accessToken.isNotBlank()) { "Missing authorization" }
        require(playlist.id.isNotBlank()) { "Missing playlist" }
        return buildList {
            var pageToken: String? = null
            repeat(MaxPlaylistPages) {
                val suffix = pageToken?.let { "&pageToken=${URLEncoder.encode(it, Charsets.UTF_8.name())}" }.orEmpty()
                val page = parseYouTubePlaylistItems(
                    get(
                        "$ApiBase/playlistItems?part=snippet,contentDetails&playlistId=${URLEncoder.encode(playlist.id, Charsets.UTF_8.name())}&maxResults=50$suffix",
                        accessToken,
                    ),
                    playlist.title,
                )
                addAll(page.items)
                pageToken = page.nextPageToken
                if (pageToken == null) return@buildList
            }
        }.distinctBy(YouTubePlaylistItem::id)
    }

    fun addVideoToPlaylist(accessToken: String, playlistId: String, videoId: String) {
        requireApiId(playlistId, "playlist")
        requireApiId(videoId, "video")
        val body = JsonWriter.string().`object`()
            .`object`("snippet")
            .value("playlistId", playlistId)
            .`object`("resourceId")
            .value("kind", "youtube#video")
            .value("videoId", videoId)
            .end().end().end().done()
        request("POST", "$ApiBase/playlistItems?part=snippet", accessToken, body)
    }

    fun removePlaylistItem(accessToken: String, itemId: String) {
        requireApiId(itemId, "playlist item")
        request(
            "DELETE",
            "$ApiBase/playlistItems?id=${URLEncoder.encode(itemId, Charsets.UTF_8.name())}",
            accessToken,
        )
    }

    private fun get(url: String, accessToken: String): String {
        return request("GET", url, accessToken)
    }

    private fun request(method: String, url: String, accessToken: String, body: String? = null): String {
        require(accessToken.isNotBlank()) { "Missing authorization" }
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                val bytes = body.toByteArray(UTF_8)
                require(bytes.size <= 64 * 1024) { "YouTube request is too large" }
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val code = connection.responseCode
            check(code in 200..299) { "YouTube request failed ($code)" }
            return if (code == HttpURLConnection.HTTP_NO_CONTENT) "" else {
                connection.inputStream.use { it.readUtf8Limited(MaxResponseBytes) }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun requireApiId(value: String, label: String) {
        require(value.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Invalid $label id" }
    }
}
