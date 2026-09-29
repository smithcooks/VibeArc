package com.vibearc.app

import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

internal data class LyricLine(val startMs: Long, val text: String)

internal data class LyricsDocument(
    val syncedLines: List<LyricLine>,
    val plainLines: List<String>,
    val instrumental: Boolean,
)

private val LrcTimestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?]")

internal fun parseLrc(value: String): List<LyricLine> = value.lineSequence().flatMap { row ->
    val timestamps = LrcTimestamp.findAll(row).toList()
    val text = row.substringAfterLast(']', "").trim()
    if (timestamps.isEmpty() || text.isEmpty()) emptySequence() else timestamps.asSequence().map { match ->
        val minutes = match.groupValues[1].toLong()
        val seconds = match.groupValues[2].toLong()
        val millis = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        LyricLine((minutes * 60 + seconds) * 1_000 + millis, text)
    }
}.sortedBy(LyricLine::startMs).toList()

internal fun activeLyricIndex(lines: List<LyricLine>, positionMs: Long): Int =
    lines.indexOfLast { it.startMs <= positionMs }

internal fun encodeLrc(lines: List<LyricLine>): String = lines.sortedBy(LyricLine::startMs).joinToString("\n") { line ->
    val totalSeconds = line.startMs.coerceAtLeast(0L) / 1_000
    "[%02d:%02d.%03d]%s".format(
        totalSeconds / 60,
        totalSeconds % 60,
        line.startMs.coerceAtLeast(0L) % 1_000,
        line.text.replace('\n', ' ').replace('\r', ' '),
    )
}

internal fun lyricsFileName(title: String): String = title.trim()
    .replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80).ifBlank { "lyrics" } + ".lrc"

private fun lyricsDocument(root: JsonObject): LyricsDocument {
    val plain = root.getString("plainLyrics", "")
        .lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
    return LyricsDocument(
        syncedLines = parseLrc(root.getString("syncedLyrics", "")),
        plainLines = plain,
        instrumental = root["instrumental"] == true,
    )
}

internal fun parseLyricsResponse(value: String): LyricsDocument =
    lyricsDocument(JsonParser.`object`().from(value))

private val LyricsTitleSuffix = Regex(
    "(?i)\\s*[\\[(](?:official\\s+)?(?:music\\s+)?(?:video|audio|lyrics?|visuali[sz]er)[\\])]\\s*$",
)
private val TopicArtistSuffix = Regex("(?i)\\s+-\\s+topic\\s*$")
private val MetadataSeparator = Regex("[^\\p{L}\\p{N}]+")

internal fun lyricsSearchTitle(value: String): String = value.replace(LyricsTitleSuffix, "").trim()
internal fun lyricsSearchArtist(value: String): String = value.replace(TopicArtistSuffix, "").trim()
private fun normalizedMetadata(value: String): String = value.lowercase().replace(MetadataSeparator, " ").trim()

internal fun parseLyricsSearchResponse(value: String, track: Track): LyricsDocument? {
    val wantedTitle = normalizedMetadata(lyricsSearchTitle(track.title))
    val wantedArtist = normalizedMetadata(lyricsSearchArtist(track.artist))
    return JsonParser.array().from(value).mapNotNull { it as? JsonObject }
        .filter { result ->
            val title = normalizedMetadata(result.getString("trackName", ""))
            val artist = normalizedMetadata(result.getString("artistName", ""))
            title.isNotBlank() && artist.isNotBlank() &&
                (title.contains(wantedTitle) || wantedTitle.contains(title)) &&
                (artist.contains(wantedArtist) || wantedArtist.contains(artist))
        }
        .maxByOrNull { result ->
            val durationDifference = kotlin.math.abs(
                result.getLong("duration", 0L) * 1_000 - track.durationMs,
            )
            (if (result.getString("syncedLyrics", "").isNotBlank()) 2 else 0) +
                (if (track.durationMs > 0 && durationDifference <= 3_000) 1 else 0)
        }
        ?.let(::lyricsDocument)
}

internal object LyricsProvider {
    private const val MaxResponseBytes = 512 * 1024
    private val cache = LinkedHashMap<String, LyricsDocument>()

    @Synchronized
    fun fetch(track: Track): LyricsDocument? {
        val key = "${track.title}\n${track.artist}\n${track.album}\n${track.durationMs}"
        cache[key]?.let { return it }
        if (track.title.isBlank() || track.artist.isBlank()) return null
        val parameters = buildList {
            add("track_name" to track.title)
            add("artist_name" to track.artist)
            track.album.takeIf { it.isNotBlank() && it != "YouTube Music" }?.let { add("album_name" to it) }
            (track.durationMs / 1_000).takeIf { it in 1..3_600 }?.let { add("duration" to it.toString()) }
        }
        val exact = request("https://lrclib.net/api/get?${parameters.encoded()}")
        val result = when (exact.first) {
            404 -> {
                val search = listOf(
                    "track_name" to lyricsSearchTitle(track.title),
                    "artist_name" to lyricsSearchArtist(track.artist),
                )
                val fallback = request("https://lrclib.net/api/search?${search.encoded()}")
                when (fallback.first) {
                    in 200..299 -> fallback.second?.let { parseLyricsSearchResponse(it, track) }
                    else -> error("Lyrics service returned ${fallback.first}")
                }
            }
            in 200..299 -> exact.second?.let(::parseLyricsResponse)
            else -> error("Lyrics service returned ${exact.first}")
        }
        return result?.also {
            cache[key] = it
            if (cache.size > 8) cache.remove(cache.keys.first())
        }
    }

    private fun List<Pair<String, String>>.encoded(): String = joinToString("&") { (name, value) ->
        "$name=${URLEncoder.encode(value, UTF_8.name())}"
    }

    private fun request(url: String): Pair<Int, String?> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "VibeArc/0.9.0 (Android music player)")
            val code = connection.responseCode
            return code to if (code in 200..299) {
                connection.inputStream.use { it.readUtf8Limited(MaxResponseBytes) }
            } else null
        } finally {
            connection.disconnect()
        }
    }
}
