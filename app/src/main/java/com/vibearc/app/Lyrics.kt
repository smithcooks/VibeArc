package com.vibearc.app

import com.grack.nanojson.JsonParser
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

internal fun parseLyricsResponse(value: String): LyricsDocument {
    val root = JsonParser.`object`().from(value)
    val plain = root.getString("plainLyrics", "")
        .lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
    return LyricsDocument(
        syncedLines = parseLrc(root.getString("syncedLyrics", "")),
        plainLines = plain,
        instrumental = root["instrumental"] == true,
    )
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
            track.album.takeIf(String::isNotBlank)?.let { add("album_name" to it) }
            (track.durationMs / 1_000).takeIf { it in 1..3_600 }?.let { add("duration" to it.toString()) }
        }.joinToString("&") { (name, value) ->
            "$name=${URLEncoder.encode(value, UTF_8.name())}"
        }
        val connection = URL("https://lrclib.net/api/get?$parameters").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "VibeArc/0.8.0 (Android music player)")
            return when (connection.responseCode) {
                404 -> null
                in 200..299 -> parseLyricsResponse(
                    connection.inputStream.use { it.readUtf8Limited(MaxResponseBytes) },
                ).also {
                    cache[key] = it
                    if (cache.size > 8) cache.remove(cache.keys.first())
                }
                else -> error("Lyrics service returned ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }
}
