package com.vibearc.app

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import java.net.HttpURLConnection
import java.net.URL

private const val MusicOrigin = "https://music.youtube.com"

internal data class YouTubeFeedSection(val title: String, val tracks: List<Track>)

internal fun parseYouTubeMusicAccount(json: String): YouTubeAccount {
    val renderer = jsonObjects(JsonParser.`object`().from(json))
        .firstNotNullOfOrNull { it["activeAccountHeaderRenderer"] as? JsonObject }
        ?: throw IllegalArgumentException("No active YouTube Music account")
    val name = (renderer["accountName"] as? JsonObject).displayText()
    val handle = (renderer["channelHandle"] as? JsonObject).displayText()
    require(name.isNotBlank()) { "Missing YouTube Music account name" }
    return YouTubeAccount(handle.ifBlank { name }, name, renderer.firstImageUrl())
}

internal fun parseYouTubeMusicPlaylists(json: String): List<YouTubePlaylist> =
    jsonObjects(JsonParser.`object`().from(json)).mapNotNull { wrapper ->
        val renderer = (wrapper["musicTwoRowItemRenderer"] ?: wrapper["musicResponsiveListItemRenderer"])
            as? JsonObject ?: return@mapNotNull null
        val browseId = renderer.firstNestedString("browseId")
        if (!browseId.startsWith("VL")) return@mapNotNull null
        val title = (renderer["title"] as? JsonObject).displayText().ifBlank {
            renderer.flexTexts().firstOrNull().orEmpty()
        }
        if (title.isBlank()) return@mapNotNull null
        val subtitle = (renderer["subtitle"] as? JsonObject).displayText() + " " +
            renderer.flexTexts().drop(1).joinToString(" ")
        YouTubePlaylist(
            id = browseId.removePrefix("VL"),
            title = title,
            itemCount = Regex("[0-9][0-9,]*").find(subtitle)?.value?.replace(",", "")?.toIntOrNull() ?: 0,
            artworkUrl = renderer.firstImageUrl(),
        )
    }.distinctBy(YouTubePlaylist::id).toList()

internal fun parseYouTubeMusicPlaylistItems(json: String, playlistTitle: String): YouTubePlaylistItemPage {
    val root = JsonParser.`object`().from(json)
    val items = jsonObjects(root).mapNotNull { wrapper ->
        val renderer = wrapper["musicResponsiveListItemRenderer"] as? JsonObject ?: return@mapNotNull null
        val videoId = renderer.firstNestedString("videoId")
        if (videoId.isBlank()) return@mapNotNull null
        val texts = renderer.flexTexts()
        val title = texts.firstOrNull().orEmpty()
        if (title.isBlank()) return@mapNotNull null
        val setVideoId = renderer.firstNestedString("playlistSetVideoId").ifBlank { videoId }
        YouTubePlaylistItem(
            id = setVideoId,
            track = Track(
                title = title,
                artist = texts.getOrNull(1).orEmpty().ifBlank { "YouTube Music" },
                album = playlistTitle,
                uri = "$MusicOrigin/watch?v=$videoId",
                durationMs = texts.firstNotNullOfOrNull(::parseClockMillis) ?: 0,
                artworkUri = renderer.firstImageUrl(),
                folder = "YouTube Music",
            ),
        )
    }.distinctBy(YouTubePlaylistItem::id).toList()
    val continuation = jsonObjects(root).firstNotNullOfOrNull { wrapper ->
        (wrapper["continuationCommand"] as? JsonObject)?.getString("token", "")?.ifBlank { null }
    }
    return YouTubePlaylistItemPage(items, continuation)
}

internal fun parseYouTubeMusicFeed(json: String): List<YouTubeFeedSection> =
    jsonObjects(JsonParser.`object`().from(json)).mapNotNull { wrapper ->
        val shelf = (wrapper["musicCarouselShelfRenderer"] ?: wrapper["musicShelfRenderer"])
            as? JsonObject ?: return@mapNotNull null
        val title = jsonObjects(shelf["header"]).map { it.displayText() }
            .firstOrNull(String::isNotBlank).orEmpty()
        val sectionTitle = title.ifBlank { "Recommended" }
        val tracks = jsonObjects(shelf["contents"]).mapNotNull(::feedTrack)
            .map { it.copy(album = sectionTitle) }
            .distinctBy(Track::uri)
            .toList()
        tracks.takeIf(List<Track>::isNotEmpty)?.let { YouTubeFeedSection(sectionTitle, it) }
    }.distinctBy(YouTubeFeedSection::title).toList()

internal fun parseYouTubeMusicRadio(json: String): List<Track> =
    jsonObjects(JsonParser.`object`().from(json)).mapNotNull(::feedTrack).distinctBy(Track::uri).toList()

private fun feedTrack(wrapper: JsonObject): Track? {
    val renderer = (wrapper["musicTwoRowItemRenderer"] ?: wrapper["musicResponsiveListItemRenderer"]
        ?: wrapper["playlistPanelVideoRenderer"]) as? JsonObject ?: return null
    val videoId = renderer.firstNestedString("videoId").takeIf(String::isNotBlank) ?: return null
    val title = (renderer["title"] as? JsonObject).displayText().ifBlank { renderer.flexTexts().firstOrNull().orEmpty() }
    if (title.isBlank()) return null
    val subtitle = (renderer["subtitle"] as? JsonObject).displayText()
    val texts = renderer.flexTexts()
    val artist = texts.getOrNull(1).orEmpty().ifBlank { subtitle.substringBefore(" • ").ifBlank { "YouTube Music" } }
    return Track(
        title = title,
        artist = artist,
        album = "YouTube Music",
        uri = "$MusicOrigin/watch?v=$videoId",
        durationMs = (texts + subtitle).firstNotNullOfOrNull(::parseClockMillis) ?: 0,
        artworkUri = renderer.firstImageUrl(),
        folder = "YouTube Music",
    )
}

private fun parseClockMillis(value: String): Long? {
    if (!value.matches(Regex("[0-9]{1,2}(:[0-9]{2}){1,2}"))) return null
    return value.split(':').fold(0L) { total, part -> total * 60 + part.toLong() } * 1_000
}

private fun JsonObject.flexTexts(): List<String> = (this["flexColumns"] as? List<*>).orEmpty().mapNotNull { column ->
    val renderer = (column as? JsonObject)?.getObject("musicResponsiveListItemFlexColumnRenderer", null)
    (renderer?.get("text") as? JsonObject).displayText().ifBlank { null }
}

private fun JsonObject?.displayText(): String {
    if (this == null) return ""
    getString("simpleText", "").takeIf(String::isNotBlank)?.let { return it }
    return (this["runs"] as? List<*>).orEmpty().mapNotNull { run ->
        (run as? JsonObject)?.getString("text", "")?.ifBlank { null }
    }.joinToString("").trim()
}

private fun JsonObject.firstNestedString(key: String): String = jsonObjects(this)
    .map { it.getString(key, "") }.firstOrNull(String::isNotBlank).orEmpty()

private fun JsonObject.firstImageUrl(): String = jsonObjects(this)
    .map { it.getString("url", "") }
    .firstOrNull { it.startsWith("https://") }.orEmpty()

private fun jsonObjects(value: Any?): Sequence<JsonObject> = sequence {
    when (value) {
        is JsonObject -> {
            yield(value)
            value.values.forEach { yieldAll(jsonObjects(it)) }
        }
        is Iterable<*> -> value.forEach { yieldAll(jsonObjects(it)) }
    }
}

internal object YouTubeMusicSessionApi {
    private const val MaxResponseBytes = 4 * 1024 * 1024
    private const val MaxPages = 20
    @Volatile private var cachedConfig: WebConfig? = null

    @Synchronized
    fun clearConfig() { cachedConfig = null }

    private data class WebConfig(val apiKey: String, val clientVersion: String, val visitorData: String)

    fun load(): YouTubeAccountData {
        val account = parseYouTubeMusicAccount(post("account/account_menu", contextBody()))
        val playlists = parseYouTubeMusicPlaylists(
            post("browse", contextBody { value("browseId", "FEmusic_liked_playlists") }),
        )
        return YouTubeAccountData(account, playlists)
    }

    fun loadPlaylist(playlist: YouTubePlaylist): List<Track> = loadPlaylistItems(playlist)
        .map(YouTubePlaylistItem::track).distinctBy(Track::uri)

    fun loadHomeFeed(): List<YouTubeFeedSection> = parseYouTubeMusicFeed(
        post("browse", contextBody { value("browseId", "FEmusic_home") }),
    )

    fun loadRadio(track: Track): List<Track> {
        val videoId = youtubeVideoId(track.uri) ?: error("This track has no YouTube video id")
        return parseYouTubeMusicRadio(post("next", contextBody {
            value("videoId", videoId)
            value("playlistId", "RDAMVM$videoId")
            value("isAudioOnly", true)
        })).filterNot { it.uri == track.uri }
    }

    fun loadPlaylistItems(playlist: YouTubePlaylist): List<YouTubePlaylistItem> {
        requireApiId(playlist.id, "playlist")
        return buildList {
            var continuation: String? = null
            repeat(MaxPages) {
                val body = if (continuation == null) {
                    contextBody { value("browseId", "VL${playlist.id}") }
                } else {
                    contextBody { value("continuation", continuation) }
                }
                val page = parseYouTubeMusicPlaylistItems(post("browse", body), playlist.title)
                addAll(page.items)
                continuation = page.nextPageToken
                if (continuation == null) return@buildList
            }
        }.distinctBy(YouTubePlaylistItem::id)
    }

    fun createPlaylist(title: String): String {
        val cleanTitle = title.trim().also { require(it.isNotEmpty() && it.length <= 150) }
        val response = post("playlist/create", contextBody {
            value("title", cleanTitle)
            value("privacyStatus", "PRIVATE")
        })
        return jsonObjects(JsonParser.`object`().from(response)).map { it.getString("playlistId", "") }
            .firstOrNull(String::isNotBlank) ?: throw IllegalStateException("YouTube Music did not create the playlist")
    }

    fun renamePlaylist(playlistId: String, title: String) {
        editPlaylist(playlistId, "ACTION_SET_PLAYLIST_NAME") { value("playlistName", title.trim()) }
    }

    fun deletePlaylist(playlistId: String) {
        requireApiId(playlistId, "playlist")
        post("playlist/delete", contextBody { value("playlistId", playlistId) })
    }

    fun addVideoToPlaylist(playlistId: String, videoId: String) {
        editPlaylist(playlistId, "ACTION_ADD_VIDEO") {
            value("addedVideoId", videoId)
            value("dedupeOption", "DEDUPE_OPTION_SKIP")
        }
    }

    fun removePlaylistItem(playlistId: String, itemId: String) {
        editPlaylist(playlistId, "ACTION_REMOVE_VIDEO") { value("setVideoId", itemId) }
    }

    fun setLiked(videoId: String, liked: Boolean) {
        requireApiId(videoId, "video")
        post(if (liked) "like/like" else "like/removelike", contextBody {
            `object`("target").value("videoId", videoId).end()
        })
    }

    private fun editPlaylist(
        playlistId: String,
        action: String,
        writeAction: com.grack.nanojson.JsonStringWriter.() -> Unit,
    ) {
        requireApiId(playlistId, "playlist")
        val body = JsonWriter.string().`object`()
        appendContext(body)
        body.value("playlistId", playlistId)
        val actionWriter = body.array("actions").`object`().value("action", action)
        writeAction.invoke(actionWriter)
        post("browse/edit_playlist", actionWriter.end().end().end().done())
    }

    private fun contextBody(writeFields: com.grack.nanojson.JsonStringWriter.() -> Unit = {}): String {
        val writer = JsonWriter.string().`object`()
        appendContext(writer)
        writeFields.invoke(writer)
        return writer.end().done()
    }

    private fun appendContext(writer: com.grack.nanojson.JsonStringWriter) {
        val config = config()
        writer.`object`("context").`object`("client")
            .value("clientName", "WEB_REMIX")
            .value("clientVersion", config.clientVersion)
            .value("hl", "en")
            .value("gl", "US")
            .apply { if (config.visitorData.isNotBlank()) value("visitorData", config.visitorData) }
            .end().end()
    }

    private fun post(endpoint: String, body: String): String {
        val config = config()
        val cookies = YouTubeWebSession.cookieHeader() ?: error("YouTube Music session expired")
        val authorization = YouTubeWebSession.authorization() ?: error("YouTube Music session expired")
        return request("$MusicOrigin/youtubei/v1/$endpoint?key=${config.apiKey}", body) { connection ->
            connection.setRequestProperty("Cookie", cookies)
            connection.setRequestProperty("Authorization", authorization)
            connection.setRequestProperty("Origin", MusicOrigin)
            connection.setRequestProperty("X-Origin", MusicOrigin)
            connection.setRequestProperty("Referer", "$MusicOrigin/")
            connection.setRequestProperty("X-YouTube-Client-Name", "67")
            connection.setRequestProperty("X-YouTube-Client-Version", config.clientVersion)
            if (config.visitorData.isNotBlank()) connection.setRequestProperty("X-Goog-Visitor-Id", config.visitorData)
        }
    }

    @Synchronized
    private fun config(): WebConfig = cachedConfig ?: run {
        val html = request(MusicOrigin, null) { connection ->
            YouTubeWebSession.cookieHeader()?.let { connection.setRequestProperty("Cookie", it) }
        }
        fun find(name: String) = Regex("\\\"$name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
            .find(html)?.groupValues?.get(1).orEmpty()
        WebConfig(
            apiKey = find("INNERTUBE_API_KEY").ifBlank { "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30" },
            clientVersion = find("INNERTUBE_CONTEXT_CLIENT_VERSION").ifBlank {
                throw IllegalStateException("Could not read YouTube Music client version")
            },
            visitorData = find("VISITOR_DATA"),
        ).also { cachedConfig = it }
    }

    private fun request(
        url: String,
        body: String?,
        headers: (HttpURLConnection) -> Unit,
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36")
            connection.setRequestProperty("Accept", if (body == null) "text/html" else "application/json")
            headers(connection)
            if (body != null) {
                val bytes = body.toByteArray(Charsets.UTF_8)
                require(bytes.size <= 128 * 1024)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            check(connection.responseCode in 200..299) { "YouTube Music request failed (${connection.responseCode})" }
            return connection.inputStream.use { it.readUtf8Limited(MaxResponseBytes) }
        } finally {
            connection.disconnect()
        }
    }

    private fun requireApiId(value: String, label: String) {
        require(value.matches(Regex("[A-Za-z0-9_-]{1,128}"))) { "Invalid $label id" }
    }
}

private fun youtubeVideoId(value: String): String? = runCatching {
    val uri = java.net.URI(value)
    when (uri.host?.lowercase()) {
        "youtu.be" -> uri.path.trim('/').substringBefore('/').takeIf(String::isNotBlank)
        "youtube.com", "www.youtube.com", "music.youtube.com" -> uri.rawQuery.orEmpty().split('&')
            .firstNotNullOfOrNull { part -> part.substringAfter('=', "").takeIf { part.substringBefore('=') == "v" } }
        else -> null
    }
}.getOrNull()?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,64}")) }
