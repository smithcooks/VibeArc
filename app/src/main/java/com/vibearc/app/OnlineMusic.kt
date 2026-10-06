package com.vibearc.app

import com.grack.nanojson.JsonArray
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeMusicClientVersion
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYoutubeMusicHeaders
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

internal enum class AudioFormat(val label: String, val extension: String, val mimeType: String) {
    ANY("Automatic", "audio", "audio/*"),
    AAC("AAC", "m4a", "audio/mp4"),
    OPUS("Opus", "opus", "audio/ogg"),
    MP3("MP3", "mp3", "audio/mpeg"),
    FLAC("FLAC", "flac", "audio/flac"),
}

internal enum class AudioQuality(val label: String, val maxBitrateKbps: Int?) {
    HIGHEST("Highest available", null),
    HIGH("High · up to 256 kbps", 256),
    BALANCED("Balanced · up to 160 kbps", 160),
    DATA_SAVER("Data saver · up to 96 kbps", 96),
}

internal data class AudioCandidate(
    val url: String,
    val bitrate: Int,
    val format: AudioFormat = AudioFormat.ANY,
    val mimeType: String = format.mimeType,
    val extension: String = format.extension,
    val codec: String = "",
    val sampleRate: Int = 0,
    val contentLength: Long = -1,
) {
    val isLossless: Boolean get() = format == AudioFormat.FLAC
    val isHiRes: Boolean get() = isLossless && sampleRate > 48_000
}

internal fun selectAudioCandidate(
    candidates: List<AudioCandidate>,
    format: AudioFormat = AudioFormat.ANY,
    quality: AudioQuality = AudioQuality.HIGHEST,
): AudioCandidate? {
    val matching = candidates.filter { it.url.isNotBlank() && (format == AudioFormat.ANY || it.format == format) }
    val withinLimit = quality.maxBitrateKbps?.let { limit -> matching.filter { it.bitrate in 1..limit } }.orEmpty()
    return (if (quality.maxBitrateKbps == null) matching else withinLimit.ifEmpty { matching })
        .maxByOrNull(AudioCandidate::bitrate)
}

internal fun selectAudioUrl(candidates: List<AudioCandidate>, preferHighestQuality: Boolean = true): String? {
    return selectAudioCandidate(
        candidates,
        quality = if (preferHighestQuality) AudioQuality.HIGHEST else AudioQuality.BALANCED,
    )?.url
}

internal fun selectPlaybackAudioCandidate(candidates: List<AudioCandidate>, format: AudioFormat, quality: AudioQuality): AudioCandidate? =
    selectAudioCandidate(candidates, format, quality) ?: selectAudioCandidate(candidates, AudioFormat.ANY, quality)

/** A tap joins an already-running prefetch instead of extracting the same song twice. */
internal class ResolvedAudioCache {
    private val entries = ConcurrentHashMap<String, Pair<Long, FutureTask<Track>>>()

    fun get(key: String, load: () -> Track): Track {
        val now = System.nanoTime()
        val entry = entries.compute(key) { _, cached ->
            cached?.takeIf { !it.second.isDone || now - it.first < 300_000_000_000L }
                ?: (now to FutureTask(load))
        }!!
        entry.second.run()
        try {
            return entry.second.get()
        } catch (error: ExecutionException) {
            entries.remove(key, entry)
            throw error.cause ?: error
        } finally {
            // ponytail: soft 60-entry limit; in-flight requests stay until they finish.
            if (entries.size > 60) entries.entries.firstOrNull { it.key != key && it.value.second.isDone }
                ?.let { entries.remove(it.key, it.value) }
        }
    }

    fun invalidate(source: String) {
        entries.keys.filter { it.substringBefore('|') == source }.forEach(entries::remove)
    }
}

internal val audioPrefetchPermits = kotlinx.coroutines.sync.Semaphore(2)

private val GoogleArtworkSize = Regex("=w\\d+-h\\d+[^?]*$")

internal fun highResolutionArtworkUrl(url: String): String =
    if ("googleusercontent.com" in url && GoogleArtworkSize.containsMatchIn(url)) {
        url.replace(GoogleArtworkSize, "=w1024-h1024-l90-rj")
    } else {
        url
    }

internal fun parseInnertubeSearch(json: String): List<Track> {
    val renderers = mutableListOf<JsonObject>()
    collectMusicRenderers(JsonParser.`object`().from(json), renderers)
    return renderers.mapNotNull { renderer ->
        val videoId = renderer.getObject("playlistItemData").getString("videoId", "")
        val columns = renderer.getArray("flexColumns")
        val title = columnRuns(columns, 0).firstOrNull()?.getString("text", "").orEmpty()
        if (videoId.isBlank() || title.isBlank()) return@mapNotNull null
        val metadataRuns = rendererRuns(renderer)
        val artist = metadataRuns.firstNotNullOfOrNull { run ->
            run.takeIf { it.musicPageType() == "MUSIC_PAGE_TYPE_ARTIST" || it.browseId().startsWith("UC") }
                ?.getString("text", "")
        }?.takeIf(String::isNotBlank) ?: columnRuns(columns, 1).map { it.getString("text", "").trim() }
            .firstOrNull { text -> text.isNotBlank() && text !in setOf("•", "·", "Song", "Video", "Album", "Single") &&
                !text.matches(DurationPattern) && !text.contains(Regex("(?i)\\b(plays|views|subscribers)\\b")) }
            ?: "YouTube Music"
        val album = metadataRuns.firstNotNullOfOrNull { run ->
            run.takeIf { it.musicPageType() == "MUSIC_PAGE_TYPE_ALBUM" || it.browseId().startsWith("MPRE") }
                ?.getString("text", "")
        }?.takeIf(String::isNotBlank) ?: "YouTube Music"
        val durationMs = metadataRuns.asSequence()
            .map { it.getString("text", "") }
            .firstOrNull { it.matches(DurationPattern) }
            ?.split(':')
            ?.fold(0L) { total, part -> total * 60 + part.toLong() }
            ?.times(1_000)
            ?: 0L
        Track(
            title = title,
            artist = artist,
            album = album,
            uri = "https://music.youtube.com/watch?v=$videoId",
            durationMs = durationMs,
            artworkUri = renderer.getObject("thumbnail")
                .getObject("musicThumbnailRenderer")
                .getObject("thumbnail")
                .getArray("thumbnails")
                .mapNotNull { it as? JsonObject }
                .map { it.getString("url", "") }
                .lastOrNull(String::isNotBlank)
                ?.let(::highResolutionArtworkUrl)
                .orEmpty(),
            folder = "YouTube Music",
        ).withoutPlayCounts()
    }.distinctBy(Track::uri).take(20)
}

private val DurationPattern = Regex("^(?:\\d+:)?\\d{1,2}:\\d{2}$")

private fun collectMusicRenderers(value: Any?, output: MutableList<JsonObject>) {
    when (value) {
        is JsonObject -> {
            value.getObject("musicResponsiveListItemRenderer", null)?.let(output::add)
            value.values.forEach { child -> collectMusicRenderers(child, output) }
        }
        is JsonArray -> value.forEach { child -> collectMusicRenderers(child, output) }
    }
}

private fun columnRuns(columns: JsonArray, index: Int): List<JsonObject> {
    val column = columns.getOrNull(index) as? JsonObject ?: return emptyList()
    val runs = column.getObject("musicResponsiveListItemFlexColumnRenderer")
        .getObject("text")
        .getArray("runs")
    return runs.mapNotNull { it as? JsonObject }
}

private fun rendererRuns(renderer: JsonObject): List<JsonObject> =
    listOf("flexColumns", "fixedColumns").flatMap { key ->
        renderer.getArray(key).flatMap { columnValue ->
            val column = columnValue as? JsonObject ?: return@flatMap emptyList()
            val text = column.getObject("musicResponsiveListItemFlexColumnRenderer", null)
                ?.getObject("text")
                ?: column.getObject("musicResponsiveListItemFixedColumnRenderer")
                    .getObject("text")
            text.getArray("runs").mapNotNull { it as? JsonObject }
        }
    }

private fun JsonObject.browseId(): String = getObject("navigationEndpoint")
    .getObject("browseEndpoint")
    .getString("browseId", "")

private fun JsonObject.musicPageType(): String = getObject("navigationEndpoint")
    .getObject("browseEndpoint")
    .getObject("browseEndpointContextSupportedConfigs")
    .getObject("browseEndpointContextMusicConfig")
    .getString("pageType", "")

internal object OnlineMusic {
    private val youtube = ServiceList.YouTube
    private val resolvedAudio = ResolvedAudioCache()

    init {
        NewPipe.init(ExtractorDownloader)
    }

    fun search(query: String): List<Track> {
        val cleanQuery = query.trim()
        require(cleanQuery.isNotEmpty()) { "Search query cannot be blank" }
        return try {
            searchInnertube(cleanQuery)
        } catch (_: Exception) {
            searchWithNewPipe(cleanQuery)
        }
    }

    private fun searchInnertube(query: String): List<Track> {
        val version = getYoutubeMusicClientVersion()
        val requestBody = JsonWriter.string()
            .`object`()
                .`object`("context")
                    .`object`("client")
                        .value("clientName", "WEB_REMIX")
                        .value("clientVersion", version)
                        .value("hl", "en-GB")
                        .value("gl", Locale.getDefault().country.ifBlank { "US" })
                        .value("platform", "DESKTOP")
                        .value("utcOffsetMinutes", TimeZone.getDefault().rawOffset / 60_000)
                    .end()
                    .`object`("request").array("internalExperimentFlags").end().value("useSsl", true).end()
                    .`object`("user").value("lockedSafetyMode", false).end()
                .end()
                .value("query", query.trim())
                .value("params", "Eg-KAQwIARAAGAAgACgAMABqChAEEAUQAxAKEAk%3D")
            .end()
            .done()
            .toByteArray(Charsets.UTF_8)
        val response = NewPipe.getDownloader().postWithContentTypeJson(
            "https://music.youtube.com/youtubei/v1/search?prettyPrint=false",
            getYoutubeMusicHeaders(),
            requestBody,
        )
        check(response.responseCode() in 200..299) { "InnerTube search failed (${response.responseCode()})" }
        return parseInnertubeSearch(response.responseBody()).ifEmpty {
            error("InnerTube returned no playable songs")
        }
    }

    private fun searchWithNewPipe(query: String): List<Track> {
        val extractor = youtube.getSearchExtractor(query.trim(), listOf(MUSIC_SONGS), "")
        extractor.fetchPage()
        return extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .take(20)
            .map { item ->
                Track(
                    title = item.name,
                    artist = item.uploaderName?.takeIf(String::isNotBlank) ?: "YouTube Music",
                    album = "YouTube Music",
                    uri = item.url,
                    durationMs = item.duration.coerceAtLeast(0) * 1_000,
                    artworkUri = item.thumbnails.maxByOrNull { image ->
                        image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
                    }?.url?.let(::highResolutionArtworkUrl).orEmpty(),
                    folder = "YouTube Music",
                )
            }
    }

    fun resolve(
        track: Track,
        format: AudioFormat = AudioFormat.ANY,
        quality: AudioQuality = AudioQuality.HIGHEST,
    ): Track {
        val source = playbackSourceUri(track)
        val key = "$source|$format|$quality"
        val cached = resolvedAudio.get(key) { resolveUncached(track, source, format, quality) }
        return track.copy(title = track.title.ifBlank { cached.title },
            artist = track.artist.takeUnless { it.isBlank() || it == "YouTube Music" } ?: cached.artist,
            uri = cached.uri, sourceUri = track.catalogUri,
            durationMs = track.durationMs.takeIf { it > 0 } ?: cached.durationMs,
            artworkUri = track.artworkUri.ifBlank { cached.artworkUri })
    }

    fun invalidate(source: String) = resolvedAudio.invalidate(source)

    private fun resolveUncached(track: Track, source: String, format: AudioFormat, quality: AudioQuality): Track {
        val info = StreamInfo.getInfo(youtube, source)
        val streamUrl = selectPlaybackAudioCandidate(info.audioStreams.map(::audioCandidate), format, quality)?.url
            ?: error("No playable audio source is available for this track.")
        return track.copy(
            title = track.title.ifBlank { info.name },
            artist = track.artist.takeUnless { it == "YouTube Music" }
                ?: info.uploaderName?.takeIf(String::isNotBlank)
                ?: "YouTube Music",
            uri = streamUrl,
            sourceUri = track.catalogUri,
            durationMs = track.durationMs.takeIf { it > 0 } ?: info.duration.coerceAtLeast(0) * 1_000,
            artworkUri = track.artworkUri.ifBlank {
                info.thumbnails.maxByOrNull { image ->
                    image.width.coerceAtLeast(0) * image.height.coerceAtLeast(0)
                }?.url?.let(::highResolutionArtworkUrl).orEmpty()
            },
        )
    }

    fun resolve(track: Track, preferHighestQuality: Boolean): Track = resolve(
        track,
        quality = if (preferHighestQuality) AudioQuality.HIGHEST else AudioQuality.BALANCED,
    )

    fun audioCandidate(
        track: Track,
        format: AudioFormat,
        quality: AudioQuality,
    ): AudioCandidate {
        val info = StreamInfo.getInfo(youtube, playbackSourceUri(track))
        return selectAudioCandidate(info.audioStreams.map(::audioCandidate), format, quality)
            ?: error("The selected format is not available for this track.")
    }
}

private fun audioCandidate(stream: org.schabi.newpipe.extractor.stream.AudioStream): AudioCandidate {
    val codec = stream.codec.orEmpty()
    val formatName = stream.format?.name.orEmpty()
    val format = when {
        codec.contains("flac", true) || formatName.contains("flac", true) -> AudioFormat.FLAC
        codec.contains("opus", true) || formatName.contains("opus", true) -> AudioFormat.OPUS
        codec.contains("mp3", true) || formatName.contains("mp3", true) -> AudioFormat.MP3
        codec.contains("aac", true) || codec.contains("mp4a", true) || formatName.contains("m4a", true) -> AudioFormat.AAC
        else -> AudioFormat.ANY
    }
    return AudioCandidate(
        stream.content.takeIf { stream.isUrl }.orEmpty(), stream.averageBitrate, format,
        stream.format?.mimeType ?: format.mimeType, stream.format?.suffix ?: format.extension,
        codec, stream.itagItem?.sampleRate ?: 0, stream.itagItem?.contentLength ?: -1,
    )
}

private object ExtractorDownloader : Downloader() {
    private const val UserAgent =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
    override fun execute(request: Request): Response {
        val connection = URL(request.url()).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.httpMethod()
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", UserAgent)
            request.headers().forEach { (name, values) ->
                connection.setRequestProperty(name, values.joinToString(", "))
            }
            request.dataToSend()?.let { data ->
                connection.doOutput = true
                connection.outputStream.use { it.write(data) }
            }
            val code = connection.responseCode
            if (code == 429) throw ReCaptchaException("YouTube requested verification", request.url())
            val body = if (request.httpMethod() == "HEAD") "" else {
                (if (code >= 400) connection.errorStream else connection.inputStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
            }
            val headers = connection.headerFields
                .filterKeys { it != null }
                .mapKeys { it.key!! }
            return Response(code, connection.responseMessage, headers, body, connection.url.toString())
        } finally {
            connection.disconnect()
        }
    }
}
