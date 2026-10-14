package com.vibearc.app

internal data class WidgetPlayback(
    val title: String, val artist: String, val artwork: String,
    val playing: Boolean, val previous: Boolean, val next: Boolean, val hasTrack: Boolean,
    val progress: Int = 0, val lyric: String = "",
) {
    val playLabel: String get() = if (playing) "Pause" else "Play"
}

internal fun widgetPlayback(track: Track?, playing: Boolean, previous: Boolean, next: Boolean) = WidgetPlayback(
    track?.title?.let(::cleanRecordingLabel)?.ifBlank { "Unknown track" } ?: "VibeArc",
    track?.artist?.let(::cleanRecordingLabel)?.ifBlank { "Unknown artist" } ?: "Tap to choose your music",
    track?.artworkUri.orEmpty(), playing && track != null, previous && track != null,
    next && track != null, track != null,
)

internal fun widgetProgress(positionMs: Long, durationMs: Long): Int =
    if (durationMs <= 0L) 0 else ((positionMs.coerceIn(0L, durationMs).toDouble() / durationMs) * 100).toInt()

internal fun widgetLyric(document: LyricsDocument?, positionMs: Long): String = when {
    document == null -> "Lyrics unavailable — tap to open"
    document.instrumental -> "Instrumental"
    document.syncedLines.isEmpty() -> "Open VibeArc for unsynced lyrics"
    else -> document.syncedLines.getOrNull(activeLyricIndex(document.syncedLines, positionMs))?.text
        ?: "Lyrics begin shortly"
}
