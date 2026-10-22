package com.vibearc.app

internal enum class DownloadFormat(val label: String, val extension: String, val mime: String) {
    WEBM("WebM · original Opus", "webm", "audio/webm"),
    MP3("MP3 · converted on device", "mp3", "audio/mpeg");

    companion object {
        fun fromPreference(value: String?) = entries.firstOrNull { it.name == value } ?: WEBM
    }
}

internal enum class Mp3CopyState { NONE, QUEUED, RUNNING, COMPLETED, FAILED }

// Ignore the old setting once on upgrade: MP3 must be an explicit new opt-in.
// Subsequent user choices use this key and survive restarts/upgrades.
internal const val DownloadFormatPreferenceKey = "download_format_user_choice_v2"
internal fun readDownloadFormatPreference(read: (String)->String?): DownloadFormat =
    DownloadFormat.fromPreference(read(DownloadFormatPreferenceKey))

/** Never silently relabel AAC/MP4 or an Ogg stream as WebM. */
internal fun selectWebmDownload(candidates: List<AudioCandidate>): AudioCandidate? =
    selectAudioCandidate(candidates.filter {
        it.format == AudioFormat.OPUS &&
            (it.mimeType.substringBefore(';').trim().equals("audio/webm", true) || it.extension.equals("webm", true))
    }, AudioFormat.OPUS, AudioQuality.HIGHEST)

internal fun DownloadEntry.canCreateMp3Copy(): Boolean =
    status == DownloadStatus.COMPLETED && ownsFile && !codec.contains("mp3",true) && !codec.contains("mpeg",true) &&
        mp3CopyState !in setOf(Mp3CopyState.QUEUED, Mp3CopyState.RUNNING, Mp3CopyState.COMPLETED)
