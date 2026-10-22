package com.vibearc.app

internal enum class DownloadExportCollection { AUDIO, DOWNLOADS }

internal data class DownloadExportLocation(
    val collection: DownloadExportCollection,
    val directory: String,
) {
    val relativePath get() = "$directory/VibeArc/"
}

/**
 * Some Android MIME maps classify .webm as video/webm and do not recognize audio/webm.
 * Audio.Media then rejects the insert before any bytes can be saved. Downloads accepts
 * the genuine MIME without requiring an audio-only collection or changing the container.
 * Its permitted top-level directory is Download, not Music. Legacy direct-file exports
 * have no collection restriction and retain their existing Music location.
 */
internal fun downloadExportLocation(format: DownloadFormat, sdk: Int): DownloadExportLocation =
    if (sdk >= 29 && format == DownloadFormat.WEBM)
        DownloadExportLocation(DownloadExportCollection.DOWNLOADS, "Download")
    else DownloadExportLocation(DownloadExportCollection.AUDIO, "Music")
