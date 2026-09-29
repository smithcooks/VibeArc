package com.vibearc.app

import androidx.media3.common.C
import androidx.media3.common.Player

internal data class AudioDetails(
    val mimeType: String?,
    val bitrate: Int,
    val sampleRate: Int,
    val channelCount: Int,
) {
    fun label(): String {
        val values = buildList {
            mimeType?.let { add(codecLabel(it)) }
            bitrate.takeIf { it > 0 }?.let { add("${it / 1_000} kbps") }
            sampleRate.takeIf { it > 0 }?.let { add(if (it % 1_000 == 0) "${it / 1_000} kHz" else "${it / 1_000.0} kHz") }
            channelCount.takeIf { it > 0 }?.let { add("$it ch") }
        }
        return values.joinToString(" · ").ifBlank { "Format not reported by source" }
    }
}

internal fun Player.currentAudioDetails(): AudioDetails? = currentTracks.groups.asSequence()
    .filter { it.type == C.TRACK_TYPE_AUDIO }
    .flatMap { group -> (0 until group.length).asSequence().filter(group::isTrackSelected).map(group::getTrackFormat) }
    .maxByOrNull { it.bitrate }
    ?.let { AudioDetails(it.sampleMimeType, it.bitrate, it.sampleRate, it.channelCount) }

private fun codecLabel(mimeType: String): String = when (mimeType.lowercase()) {
    "audio/mp4a-latm", "audio/aac" -> "AAC"
    "audio/opus" -> "Opus"
    "audio/flac" -> "FLAC"
    "audio/mpeg" -> "MP3"
    "audio/vorbis" -> "Vorbis"
    else -> mimeType.substringAfter('/').uppercase()
}
