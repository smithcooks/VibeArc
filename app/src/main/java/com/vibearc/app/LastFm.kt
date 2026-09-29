package com.vibearc.app

import android.content.Context
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

internal data class LastFmProfile(
    val username: String,
    val displayName: String,
    val playCount: Long,
    val artistCount: Long,
    val albumCount: Long,
    val trackCount: Long,
    val artworkUrl: String,
)

internal data class LastFmTrack(
    val title: String,
    val artist: String,
    val album: String = "",
    val playCount: Long = 0,
    val artworkUrl: String = "",
    val playedAtSeconds: Long? = null,
    val nowPlaying: Boolean = false,
)

internal data class LastFmSnapshot(
    val profile: LastFmProfile,
    val topTracks: List<LastFmTrack>,
    val recentTracks: List<LastFmTrack>,
)

internal fun validLastFmUsername(value: String): String? = value.trim().takeIf {
    it.length in 1..64 && it.none(Char::isISOControl)
}

internal fun parseLastFmSnapshot(infoJson: String, topJson: String, recentJson: String): LastFmSnapshot {
    val user = parseLastFmRoot(infoJson).getObject("user")
    val profile = LastFmProfile(
        username = user.getString("name", "").requiredLastFm("username"),
        displayName = user.getString("realname", "").ifBlank { user.getString("name", "") },
        playCount = user.longString("playcount"),
        artistCount = user.longString("artist_count"),
        albumCount = user.longString("album_count"),
        trackCount = user.longString("track_count"),
        artworkUrl = user.imageUrl(),
    )
    val topTracks = parseLastFmRoot(topJson).getObject("toptracks").getArray("track")
        .mapNotNull { it as? JsonObject }
        .mapNotNull { track ->
            val title = track.getString("name", "")
            val artist = track.getObject("artist").getString("name", "")
            if (title.isBlank() || artist.isBlank()) null else LastFmTrack(
                title = title,
                artist = artist,
                playCount = track.longString("playcount"),
                artworkUrl = track.imageUrl(),
            )
        }
    val recentTracks = parseLastFmRoot(recentJson).getObject("recenttracks").getArray("track")
        .mapNotNull { it as? JsonObject }
        .mapNotNull { track ->
            val title = track.getString("name", "")
            val artist = track.getObject("artist").getString("#text", "")
            if (title.isBlank() || artist.isBlank()) null else LastFmTrack(
                title = title,
                artist = artist,
                album = track.getObject("album").getString("#text", ""),
                artworkUrl = track.imageUrl(),
                playedAtSeconds = track.getObject("date").getString("uts", "").toLongOrNull(),
                nowPlaying = track.getObject("@attr").getString("nowplaying", "") == "true",
            )
        }
    return LastFmSnapshot(profile, topTracks.take(20), recentTracks.take(20))
}

internal fun parseLastFmSimilarTracks(value: String): List<LastFmTrack> =
    parseLastFmRoot(value).getObject("similartracks").getArray("track")
        .mapNotNull { it as? JsonObject }
        .mapNotNull { track ->
            val title = track.getString("name", "")
            val artist = track.getObject("artist").getString("name", "")
            if (title.isBlank() || artist.isBlank()) null else LastFmTrack(
                title = title,
                artist = artist,
                artworkUrl = track.imageUrl(),
            )
        }.take(20)

private fun parseLastFmRoot(value: String): JsonObject = JsonParser.`object`().from(value).also { root ->
    root["error"]?.let { error("Last.fm request failed ($it)") }
}

private fun JsonObject.longString(name: String): Long = getString(name, "").toLongOrNull()?.coerceAtLeast(0) ?: 0

private fun JsonObject.imageUrl(): String = getArray("image").mapNotNull { it as? JsonObject }
    .map { it.getString("#text", "") }.lastOrNull(String::isNotBlank)
    ?.replace(Regex("^http://"), "https://").orEmpty()

private fun String.requiredLastFm(label: String): String = also { require(isNotBlank()) { "Missing $label" } }

internal object LastFmApi {
    // Public profile methods need only an API key; authenticated writes must stay server-signed.
    // https://www.last.fm/api/authspec
    private const val ApiRoot = "https://ws.audioscrobbler.com/2.0/"
    private const val MaxResponseBytes = 2 * 1024 * 1024

    fun load(username: String, apiKey: String): LastFmSnapshot {
        val user = requireNotNull(validLastFmUsername(username)) { "Invalid Last.fm username" }
        require(apiKey.isNotBlank()) { "Last.fm API key is not configured" }
        return parseLastFmSnapshot(
            get("user.getInfo", apiKey, "user" to user),
            get("user.getTopTracks", apiKey, "user" to user, "limit" to "20", "period" to "overall"),
            get("user.getRecentTracks", apiKey, "user" to user, "limit" to "20", "extended" to "0"),
        )
    }

    fun similarTracks(seed: LastFmTrack, apiKey: String): List<LastFmTrack> {
        // https://www.last.fm/api/show/track.getSimilar
        require(apiKey.isNotBlank()) { "Last.fm API key is not configured" }
        require(seed.artist.isNotBlank() && seed.title.isNotBlank()) { "A track and artist are required" }
        return parseLastFmSimilarTracks(get(
            "track.getSimilar",
            apiKey,
            "artist" to seed.artist.take(256),
            "track" to seed.title.take(256),
            "autocorrect" to "1",
            "limit" to "20",
        ))
    }

    private fun get(method: String, apiKey: String, vararg extras: Pair<String, String>): String {
        val parameters = listOf(
            "method" to method,
            "api_key" to apiKey,
            "format" to "json",
        ) + extras
        val query = parameters.joinToString("&") { (name, value) ->
            "$name=${URLEncoder.encode(value, UTF_8.name())}"
        }
        val connection = URL("$ApiRoot?$query").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "VibeArc/0.9.0 (Android music player)")
            val code = connection.responseCode
            check(code in 200..299) { "Last.fm request failed ($code)" }
            return connection.inputStream.use { it.readUtf8Limited(MaxResponseBytes) }
        } finally {
            connection.disconnect()
        }
    }
}

private const val LastFmPreferences = "lastfm_profile"
private const val LastFmUsernameKey = "username"

internal fun Context.loadLastFmUsername(): String? = validLastFmUsername(
    getSharedPreferences(LastFmPreferences, Context.MODE_PRIVATE)
        .getString(LastFmUsernameKey, "").orEmpty(),
)

internal fun Context.saveLastFmUsername(username: String?) {
    getSharedPreferences(LastFmPreferences, Context.MODE_PRIVATE).edit().apply {
        val valid = username?.let(::validLastFmUsername)
        if (valid == null) remove(LastFmUsernameKey) else putString(LastFmUsernameKey, valid)
    }.apply()
}
