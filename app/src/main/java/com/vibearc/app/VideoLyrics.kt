package com.vibearc.app

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import java.net.URI
import java.net.URLDecoder
import kotlin.math.abs

internal fun lyricsVideoId(value: String): String? = runCatching {
    val uri = URI(value)
    require(uri.scheme == "https")
    when (uri.host?.lowercase()) {
        "youtu.be" -> uri.path.trim('/').takeIf { '/' !in it }
        "youtube.com", "www.youtube.com", "music.youtube.com" -> uri.rawQuery.orEmpty().split('&')
            .firstOrNull { it.substringBefore('=') == "v" }?.substringAfter('=')
            ?.let { URLDecoder.decode(it, "UTF-8") }
        else -> null
    }?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
}.getOrNull()

internal fun parseVideoLyrics(value: String, durationMs: Long): LyricsDocument? {
    val root = JsonParser.`object`().from(value)
    if (root["success"] != true) return null
    return root.getArray("data").mapNotNull { it as? JsonObject }.mapNotNull { row ->
        val seconds = (row["duration"] as? Number)?.toDouble()
        if (durationMs > 0 && (seconds == null || abs(seconds * 1000 - durationMs) > 10_000)) return@mapNotNull null
        val rich = parseLrc(row.getString("richSyncLyrics", ""))
        val synced = rich.ifEmpty { parseLrc(row.getString("syncedLyrics", "")) }
        val plain = row.getString("plainLyrics", "").lines().map(String::trim).filter(String::isNotBlank)
        if (synced.isEmpty() && plain.isEmpty()) null else
            LyricsDocument(synced, plain, false, "SimpMusic") to abs((seconds ?: 0.0) * 1000 - durationMs)
    }.minByOrNull { it.second }?.first
}

internal fun fetchVideoLyrics(track: Track, request: (String) -> Pair<Int, String?>): LyricsDocument? {
    val id = lyricsVideoId(track.catalogUri) ?: return null
    return runCatching {
        val (status, body) = request("https://api-lyrics.simpmusic.org/v1/$id")
        if (status in 200..299 && body != null) parseVideoLyrics(body, track.durationMs) else null
    }.getOrNull()
}
