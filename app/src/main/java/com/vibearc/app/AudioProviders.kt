package com.vibearc.app

import java.net.URI
import java.security.MessageDigest

// Retain persisted quality names and IDs so existing saved files remain compatible.
internal enum class DownloadQuality { LOW, NORMAL, HIGH, VERY_HIGH, LOSSLESS }
internal fun qualityForSource(bitrate: Int, lossless: Boolean): DownloadQuality? = when {
    lossless -> DownloadQuality.LOSSLESS
    bitrate<=0 -> null
    bitrate<=96 -> DownloadQuality.LOW
    bitrate<=160 -> DownloadQuality.NORMAL
    bitrate<=256 -> DownloadQuality.HIGH
    else -> DownloadQuality.VERY_HIGH
}
internal fun downloadIdentity(track: Track): String = MessageDigest.getInstance("SHA-256")
    .digest(track.catalogUri.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

/** Non-playback URLs still cannot impersonate a supported service audio source. */
internal fun isLegalAudioUrl(value: String): Boolean = runCatching {
    val u = URI(value)
    val h = u.host?.lowercase()?.removeSurrounding("[", "]") ?: return false
    val blocked = listOf("youtube.com", "youtu.be", "googlevideo.com", "spotify.com", "music.apple.com")
    u.scheme == "https" && u.rawUserInfo == null && u.port in listOf(-1, 443) &&
        blocked.none { h == it || h.endsWith(".$it") } &&
        h != "localhost" && !h.endsWith(".local") && !h.contains(':') &&
        !h.matches(Regex("(?:\\d{1,3}\\.){3}\\d{1,3}")) && value.length <= 4096
}.getOrDefault(false)
