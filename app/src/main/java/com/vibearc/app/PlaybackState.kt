package com.vibearc.app

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

private const val RecentLimit = 20

internal fun homeFeedTracks(library: List<Track>, recent: List<Track>, current: Track?): List<Track> =
    (listOfNotNull(current) + recent + library).filter { isAllowedMediaUri(it.uri) }.distinctBy(Track::uri)

private val AllowedMediaSchemes = setOf("https", "content", "file", "android.resource")

internal fun isYouTubeWatchUri(value: String): Boolean = runCatching {
    val uri = java.net.URI(value)
    uri.scheme == "https" && when (uri.host?.lowercase()) {
        "youtu.be" -> uri.path.trim('/').isNotBlank()
        "youtube.com", "www.youtube.com", "music.youtube.com", "m.youtube.com" ->
            uri.path == "/watch" && uri.rawQuery.orEmpty().split('&').any { it.startsWith("v=") && it.length > 2 }
        else -> false
    }
}.getOrDefault(false)

internal fun playbackSourceUri(track: Track): String =
    if (track.uri.startsWith("https://") && isYouTubeWatchUri(track.catalogUri)) track.catalogUri else track.uri

internal fun resolvePlaybackSource(value: String, resolve: (Track) -> Track): String {
    if (!isYouTubeWatchUri(value)) return value
    val audio = resolve(Track("", "YouTube Music", "", value)).uri
    check(isAllowedMediaUri(audio) && !isYouTubeWatchUri(audio)) { "No playable audio source was returned" }
    return audio
}

internal fun isAllowedMediaUri(value: String): Boolean = runCatching {
    val uri = java.net.URI(value)
    uri.scheme?.lowercase() in AllowedMediaSchemes &&
        (uri.scheme != "https" || !uri.host.isNullOrBlank())
}.getOrDefault(false)

internal fun playbackQueue(tracks: List<Track>, startTrack: Track): List<Track> {
    require(isAllowedMediaUri(startTrack.uri)) { "Unsupported media URI" }
    val queue = tracks.ifEmpty { listOf(startTrack) }
    val playable = queue.filter { isAllowedMediaUri(it.uri) }.distinctBy(Track::catalogUri)
    return if (playable.any { it.catalogUri == startTrack.catalogUri }) {
        playable.map { if (it.catalogUri == startTrack.catalogUri) startTrack else it }
    } else listOf(startTrack) + playable
}

internal fun List<String>.recordRecentUri(uri: String): List<String> {
    require(uri.isNotBlank())
    return (listOf(uri) + filterNot { it == uri }).take(RecentLimit)
}

internal fun backDestination(current: Tab, lastMain: Tab, playerReturn: Tab): Tab =
    playerReturnTab(if (current == Tab.Player) playerReturn else lastMain)

internal object RecentUriCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(uris: List<String>): String = uris.joinToString("\n") {
        encoder.encodeToString(it.toByteArray(UTF_8))
    }

    fun decode(value: String): List<String> = value.lineSequence().mapNotNull { row ->
        if (row.isBlank()) return@mapNotNull null
        runCatching { String(decoder.decode(row), UTF_8) }.getOrNull()
    }.toList()
}
