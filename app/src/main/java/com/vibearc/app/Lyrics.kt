package com.vibearc.app

import android.content.Context
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.InflaterInputStream

internal data class LyricWord(val startMs: Long, val text: String)
internal data class LyricLine(val startMs: Long, val text: String, val words: List<LyricWord> = emptyList())

internal data class LyricsDocument(
    val syncedLines: List<LyricLine>,
    val plainLines: List<String>,
    val instrumental: Boolean,
    val source: String = "LRCLIB",
)

private val LrcTimestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?]")
private val EnhancedLrcTimestamp = Regex("<(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?>")
private val KrcLine = Regex("^\\[(\\d+),(\\d+)](.*)$")
private val KrcWord = Regex("<(\\d+),(\\d+),\\d+>([^<]*)")

private fun MatchResult.timestampMs(): Long {
    val minutes = groupValues[1].toLong()
    val seconds = groupValues[2].toLong()
    val millis = groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
    return (minutes * 60 + seconds) * 1_000 + millis
}

internal fun parseLrc(value: String): List<LyricLine> = value.lineSequence().flatMap { row ->
    val timestamps = LrcTimestamp.findAll(row).toList()
    val content = row.substringAfterLast(']', "")
    val wordMatches = EnhancedLrcTimestamp.findAll(content).toList()
    val words = wordMatches.mapIndexedNotNull { index, match ->
        val end = wordMatches.getOrNull(index + 1)?.range?.first ?: content.length
        content.substring(match.range.last + 1, end).takeIf(String::isNotBlank)
            ?.let { LyricWord(match.timestampMs(), it) }
    }
    val text = EnhancedLrcTimestamp.replace(content, "").trim()
    if (timestamps.isEmpty() || text.isEmpty()) emptySequence() else timestamps.asSequence().map { match ->
        LyricLine(match.timestampMs(), text, words)
    }
}.sortedBy(LyricLine::startMs).toList()

internal fun activeLyricIndex(lines: List<LyricLine>, positionMs: Long): Int =
    lines.indexOfLast { it.startMs <= positionMs }

internal fun activeLyricWordIndex(line: LyricLine, positionMs: Long): Int =
    line.words.indexOfLast { it.startMs <= positionMs }

internal fun parseKrc(value: String): List<LyricLine> = value.lineSequence().mapNotNull { row ->
    val match = KrcLine.matchEntire(row.trim()) ?: return@mapNotNull null
    val lineStart = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
    val words = KrcWord.findAll(match.groupValues[3]).mapNotNull { word ->
        val offset = word.groupValues[1].toLongOrNull() ?: return@mapNotNull null
        word.groupValues[3].takeIf(String::isNotBlank)?.let { LyricWord(lineStart + offset, it) }
    }.toList()
    words.takeIf(List<LyricWord>::isNotEmpty)?.let { LyricLine(lineStart, words.joinToString("") { it.text }.trim(), words) }
}.sortedBy(LyricLine::startMs).toList()

internal fun LyricsDocument.shifted(offsetMs: Long): LyricsDocument = copy(
    syncedLines = syncedLines.map { line ->
        line.copy(
            startMs = (line.startMs + offsetMs).coerceAtLeast(0L),
            words = line.words.map { it.copy(startMs = (it.startMs + offsetMs).coerceAtLeast(0L)) },
        )
    },
)

internal fun encodeLrc(lines: List<LyricLine>): String = lines.sortedBy(LyricLine::startMs).joinToString("\n") { line ->
    "[${lyricTimestamp(line.startMs)}]${line.text.replace('\n', ' ').replace('\r', ' ')}"
}

internal fun encodeEnhancedLrc(lines: List<LyricLine>): String = lines.sortedBy(LyricLine::startMs).joinToString("\n") { line ->
    val text = line.words.takeIf(List<LyricWord>::isNotEmpty)?.joinToString("") { word ->
        "<${lyricTimestamp(word.startMs)}>${word.text.replace('\n', ' ').replace('\r', ' ')}"
    } ?: line.text.replace('\n', ' ').replace('\r', ' ')
    "[${lyricTimestamp(line.startMs)}]$text"
}

private fun lyricTimestamp(value: Long): String {
    val milliseconds = value.coerceAtLeast(0L)
    val totalSeconds = milliseconds / 1_000
    return "%02d:%02d.%03d".format(totalSeconds / 60, totalSeconds % 60, milliseconds % 1_000)
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
private fun LyricsDocument.hasLyrics(): Boolean = instrumental || syncedLines.isNotEmpty() || plainLines.isNotEmpty()

private fun List<Pair<String, String>>.encoded(): String = joinToString("&") { (name, value) ->
    "$name=${URLEncoder.encode(value, UTF_8.name())}"
}

internal fun fetchLrclibLyrics(track: Track, request: (String) -> Pair<Int, String?>): LyricsDocument? {
    val title = lyricsSearchTitle(track.title)
    val artist = lyricsSearchArtist(track.artist)
    if (title.isBlank() || artist.isBlank()) return null
    val parameters = buildList {
        add("track_name" to title)
        add("artist_name" to artist)
        track.album.takeIf { it.isNotBlank() && it != "YouTube Music" }?.let { add("album_name" to it) }
        (track.durationMs / 1_000).takeIf { it in 1..3_600 }?.let { add("duration" to it.toString()) }
    }
    runCatching {
        val (code, body) = request("https://lrclib.net/api/get?${parameters.encoded()}")
        if (code in 200..299) body?.let(::parseLyricsResponse)?.takeIf { it.hasLyrics() } else null
    }.getOrNull()?.let { return it }
    return runCatching {
        val search = listOf("track_name" to title, "artist_name" to artist)
        val (code, body) = request("https://lrclib.net/api/search?${search.encoded()}")
        if (code in 200..299) body?.let { parseLyricsSearchResponse(it, track) } else null
    }.getOrNull()
}

internal fun parseLyricsSearchResponse(value: String, track: Track): LyricsDocument? {
    val wantedTitle = normalizedMetadata(lyricsSearchTitle(track.title))
    val wantedArtist = normalizedMetadata(lyricsSearchArtist(track.artist))
    if (wantedTitle.isBlank() || wantedArtist.isBlank()) return null
    return JsonParser.array().from(value).mapNotNull { it as? JsonObject }
        .filter { result ->
            val title = normalizedMetadata(result.getString("trackName", ""))
            val artist = normalizedMetadata(result.getString("artistName", ""))
            lyricsDocument(result).hasLyrics() && title.isNotBlank() && artist.isNotBlank() &&
                (title.contains(wantedTitle) || wantedTitle.contains(title)) &&
                (artist.contains(wantedArtist) || wantedArtist.contains(artist))
        }
        .maxByOrNull { result ->
            val durationDifference = kotlin.math.abs(
                ((result["duration"] as? Number)?.toDouble() ?: 0.0) * 1_000 - track.durationMs,
            )
            (if (result.getString("syncedLyrics", "").isNotBlank()) 2 else 0) +
                (if (track.durationMs > 0 && durationDifference <= 3_000) 1 else 0)
        }
        ?.let(::lyricsDocument)
}

internal object LyricsProvider {
    private const val MaxResponseBytes = 512 * 1024
    private val cache = LinkedHashMap<String, LyricsDocument>()

    fun fetch(track: Track, title: String = track.title, artist: String = track.artist): LyricsDocument? {
        val key = "$title\n$artist\n${track.album}\n${track.durationMs}"
        synchronized(cache) { cache[key] }?.let { return it }
        if (title.isBlank() || artist.isBlank()) return null
        val queryTrack = track.copy(title = lyricsSearchTitle(title), artist = lyricsSearchArtist(artist))
        val result = fetchLrclibLyrics(queryTrack, ::request) ?: fetchKrc(queryTrack) ?: fetchPlainLyrics(queryTrack)
        return result?.let { remember(key, it) }
    }

    private fun remember(key: String, document: LyricsDocument): LyricsDocument = document.also {
        synchronized(cache) {
            cache[key] = it
            if (cache.size > 8) cache.remove(cache.keys.first())
        }
    }

    private fun fetchKrc(track: Track): LyricsDocument? = runCatching {
        val query = listOf(
            "ver" to "1", "man" to "yes", "client" to "pc",
            "keyword" to "${lyricsSearchArtist(track.artist)} - ${lyricsSearchTitle(track.title)}",
            "duration" to track.durationMs.takeIf { it > 0 }?.toString().orEmpty(), "hash" to "",
        )
        val search = request("https://lyrics.kugou.com/search?${query.encoded()}").second ?: return null
        val wantedTitle = normalizedMetadata(lyricsSearchTitle(track.title))
        val wantedArtist = normalizedMetadata(lyricsSearchArtist(track.artist))
        val candidates = JsonParser.`object`().from(search).getArray("candidates")
            .mapNotNull { it as? JsonObject }
            .filter {
                val song = normalizedMetadata(it.getString("song", ""))
                val singer = normalizedMetadata(it.getString("singer", ""))
                song.isNotBlank() && singer.isNotBlank() &&
                    (song.contains(wantedTitle) || wantedTitle.contains(song)) &&
                    (singer.contains(wantedArtist) || wantedArtist.contains(singer))
            }
        val candidate = candidates.minByOrNull {
            kotlin.math.abs(it.getLong("duration", track.durationMs) - track.durationMs)
        } ?: return null
        val download = request("https://lyrics.kugou.com/download?${listOf(
            "ver" to "1", "client" to "pc", "id" to candidate.getString("id", ""),
            "accesskey" to candidate.getString("accesskey", ""), "fmt" to "krc", "charset" to "utf8",
        ).encoded()}").second ?: return null
        val content = JsonParser.`object`().from(download).getString("content", "").takeIf(String::isNotBlank) ?: return null
        val lines = decodeKrc(content)?.let(::parseKrc).orEmpty()
        lines.takeIf(List<LyricLine>::isNotEmpty)?.let { LyricsDocument(it, emptyList(), false, "KuGou KRC") }
    }.getOrNull()

    private fun fetchPlainLyrics(track: Track): LyricsDocument? = runCatching {
        fun path(value: String) = URLEncoder.encode(value, UTF_8.name()).replace("+", "%20")
        val response = request("https://api.lyrics.ovh/v1/${path(track.artist)}/${path(track.title)}")
        if (response.first !in 200..299) return null
        val lines = response.second?.let { JsonParser.`object`().from(it).getString("lyrics", "") }
            ?.lineSequence()?.map(String::trim)?.filter(String::isNotBlank)?.toList().orEmpty()
        lines.takeIf(List<String>::isNotEmpty)?.let { LyricsDocument(emptyList(), it, false, "Lyrics.ovh") }
    }.getOrNull()

    private fun request(url: String): Pair<Int, String?> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "VibeArc/${BuildConfig.VERSION_NAME} (Android music player)")
            val code = connection.responseCode
            return code to if (code in 200..299) {
                connection.inputStream.use { it.readUtf8Limited(MaxResponseBytes) }
            } else null
        } finally {
            connection.disconnect()
        }
    }
}

private val KrcKey = byteArrayOf(
    0x40, 0x47, 0x61, 0x77, 0x5e, 0x32, 0x74, 0x47, 0x51, 0x36, 0x31, 0x2d,
    0xce.toByte(), 0xd2.toByte(), 0x6e, 0x69,
)

internal fun decodeKrc(base64: String): String? = runCatching {
    val encrypted = Base64.getMimeDecoder().decode(base64)
    require(encrypted.size > 4 && encrypted.copyOfRange(0, 4).decodeToString() == "krc1")
    val compressed = ByteArray(encrypted.size - 4) { index ->
        (encrypted[index + 4].toInt() xor KrcKey[index % KrcKey.size].toInt()).toByte()
    }
    InflaterInputStream(ByteArrayInputStream(compressed)).use { input ->
        input.readUtf8Limited(512 * 1024)
    }
}.getOrNull()

internal class LyricsOverrideStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("vibearc_lyrics_overrides", Context.MODE_PRIVATE)

    fun load(track: Track): Pair<LyricsDocument, Long>? {
        val key = track.lyricsKey()
        val raw = preferences.getString("lyrics_$key", null) ?: return null
        val offset = preferences.getLong("offset_$key", 0L)
        val synced = parseLrc(raw)
        val document = if (synced.isNotEmpty()) LyricsDocument(synced, emptyList(), false, "Manual")
        else LyricsDocument(emptyList(), raw.lineSequence().map(String::trim).filter(String::isNotBlank).toList(), false, "Manual")
        return document.shifted(offset) to offset
    }

    fun save(track: Track, raw: String, offsetMs: Long) {
        preferences.edit().putString("lyrics_${track.lyricsKey()}", raw).putLong("offset_${track.lyricsKey()}", offsetMs).apply()
    }

    fun clear(track: Track) {
        preferences.edit().remove("lyrics_${track.lyricsKey()}").remove("offset_${track.lyricsKey()}").apply()
    }

    private fun Track.lyricsKey(): String = MessageDigest.getInstance("SHA-256")
        .digest(catalogUri.toByteArray(UTF_8)).joinToString("") { "%02x".format(it) }
}
