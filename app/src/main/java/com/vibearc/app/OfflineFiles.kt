package com.vibearc.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.nio.charset.StandardCharsets.UTF_8
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.concurrent.CancellationException

internal data class OfflineFile(
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val sourceUri: String = "",
    val format: String = "",
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val lossless: Boolean = false,
    val hiRes: Boolean = false,
)

internal val OfflineFile.qualityLabel: String get() = buildList {
    format.takeIf(String::isNotBlank)?.let(::add)
    if (lossless) add("Lossless")
    if (hiRes) add("Hi-Res")
    bitrateKbps.takeIf { it > 0 }?.let { add("$it kbps") }
    sampleRateHz.takeIf { it > 0 }?.let { add("${it / 1_000.0} kHz") }
}.joinToString(" · ")

internal fun Track.canCopyOffline(): Boolean = Uri.parse(uri).scheme in setOf("content", "file") ||
    Uri.parse(catalogUri).host in setOf("music.youtube.com", "www.youtube.com", "youtu.be")

internal fun isTrustedOnlineAudioUrl(value: String): Boolean = runCatching {
    val url = URL(value)
    url.protocol == "https" && (url.host == "googlevideo.com" || url.host.endsWith(".googlevideo.com"))
}.getOrDefault(false)

internal fun safeOfflineFileName(value: String): String = value.trim()
    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    .take(120)
    .ifBlank { "audio" }

internal object OfflineFileCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(files: List<OfflineFile>): String = files.joinToString("\n") { file ->
        listOf(file.uri, file.name, file.sizeBytes.toString(), file.sourceUri, file.format,
            file.bitrateKbps.toString(), file.sampleRateHz.toString(), file.lossless.toString(), file.hiRes.toString())
            .joinToString("|") { encoder.encodeToString(it.toByteArray(UTF_8)) }
    }

    fun decode(value: String): List<OfflineFile> = value.lineSequence().mapNotNull { row ->
        val fields = row.split('|')
        if (fields.size !in setOf(3, 4, 9)) return@mapNotNull null
        runCatching {
            OfflineFile(
                String(decoder.decode(fields[0]), UTF_8),
                String(decoder.decode(fields[1]), UTF_8),
                String(decoder.decode(fields[2]), UTF_8).toLong().coerceAtLeast(0L),
                fields.getOrNull(3)?.let { String(decoder.decode(it), UTF_8) }.orEmpty(),
                fields.getOrNull(4)?.let { String(decoder.decode(it), UTF_8) }.orEmpty(),
                fields.getOrNull(5)?.let { String(decoder.decode(it), UTF_8).toIntOrNull() ?: 0 } ?: 0,
                fields.getOrNull(6)?.let { String(decoder.decode(it), UTF_8).toIntOrNull() ?: 0 } ?: 0,
                fields.getOrNull(7)?.let { String(decoder.decode(it), UTF_8).toBooleanStrictOrNull() ?: false } ?: false,
                fields.getOrNull(8)?.let { String(decoder.decode(it), UTF_8).toBooleanStrictOrNull() ?: false } ?: false,
            )
        }.getOrNull()
    }.toList()
}

private const val OfflinePreferences = "offline_files"

internal fun Context.loadOfflineFolder(): Uri? = getSharedPreferences(OfflinePreferences, Context.MODE_PRIVATE)
    .getString("folder", null)?.let(Uri::parse)

internal fun Context.saveOfflineFolder(uri: Uri) {
    getSharedPreferences(OfflinePreferences, Context.MODE_PRIVATE).edit().putString("folder", uri.toString()).apply()
}

internal fun Context.loadOfflineFiles(): List<OfflineFile> = OfflineFileCodec.decode(
    getSharedPreferences(OfflinePreferences, Context.MODE_PRIVATE).getString("files", "").orEmpty(),
)

internal fun Context.saveOfflineFiles(files: List<OfflineFile>) {
    getSharedPreferences(OfflinePreferences, Context.MODE_PRIVATE).edit()
        .putString("files", OfflineFileCodec.encode(files)).apply()
}

internal fun Context.copyLocalTrackToFolder(
    track: Track,
    folder: Uri,
    cancelled: () -> Boolean,
    onProgress: (Float) -> Unit,
): OfflineFile {
    val source = Uri.parse(track.uri)
    require(source.scheme in setOf("content", "file")) { "Only local audio can be copied offline" }
    val metadata = contentResolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            if (!cursor.moveToFirst()) null else {
                cursor.getString(0).orEmpty() to cursor.getLong(1).coerceAtLeast(0L)
            }
        }
    val name = safeOfflineFileName(metadata?.first.orEmpty().ifBlank { track.title })
    val size = metadata?.second ?: 0L
    val parent = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
    val output = DocumentsContract.createDocument(
        contentResolver,
        parent,
        contentResolver.getType(source) ?: "audio/*",
        name,
    ) ?: error("The selected folder could not create a file")
    try {
        val input = contentResolver.openInputStream(source) ?: error("Could not open local audio")
        val destination = contentResolver.openOutputStream(output, "w") ?: error("Could not write destination")
        var copied = 0L
        input.use { sourceStream ->
            destination.use { outputStream ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    if (cancelled()) throw CancellationException("Offline copy cancelled")
                    val count = sourceStream.read(buffer)
                    if (count < 0) break
                    outputStream.write(buffer, 0, count)
                    copied += count
                    if (size > 0) onProgress((copied.toFloat() / size).coerceIn(0f, 1f))
                }
            }
        }
        onProgress(1f)
        return OfflineFile(output.toString(), name, copied, track.uri)
    } catch (error: Throwable) {
        runCatching { DocumentsContract.deleteDocument(contentResolver, output) }
        throw error
    }
}

internal fun Context.copyTrackToFolder(
    track: Track,
    folder: Uri,
    format: AudioFormat,
    quality: AudioQuality,
    cancelled: () -> Boolean,
    onProgress: (Float) -> Unit,
): OfflineFile {
    if (Uri.parse(track.uri).scheme in setOf("content", "file")) {
        return copyLocalTrackToFolder(track, folder, cancelled, onProgress)
    }
    val candidate = OnlineMusic.audioCandidate(track.copy(uri = track.catalogUri), format, quality)
    require(isTrustedOnlineAudioUrl(candidate.url)) { "The provider returned an untrusted audio location" }
    val connection = openTrustedAudioConnection(candidate.url)
    val responseType = connection.contentType.orEmpty().substringBefore(';').lowercase()
    val size = connection.contentLengthLong.takeIf { it >= 0 } ?: candidate.contentLength
    require(size <= MaxOfflineAudioBytes) { "This audio file is too large" }
    require(responseType.startsWith("audio/") || responseType == "application/octet-stream") {
        "The provider did not return audio"
    }
    val extension = candidate.extension.lowercase().takeIf { it in setOf("m4a", "mp4", "webm", "opus", "mp3", "flac") }
        ?: candidate.format.extension
    val name = safeOfflineFileName("${track.artist} - ${track.title}.$extension")
    val parent = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
    val output = DocumentsContract.createDocument(
        contentResolver,
        parent,
        candidate.mimeType.takeIf { it.startsWith("audio/") } ?: "audio/*",
        name,
    ) ?: error("The selected folder could not create a file")
    try {
        val destination = contentResolver.openOutputStream(output, "w") ?: error("Could not write destination")
        var copied = 0L
        connection.inputStream.use { sourceStream ->
            destination.use { outputStream ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    if (cancelled()) throw CancellationException("Offline download cancelled")
                    val count = sourceStream.read(buffer)
                    if (count < 0) break
                    copied += count
                    require(copied <= MaxOfflineAudioBytes) { "This audio file is too large" }
                    outputStream.write(buffer, 0, count)
                    if (size > 0) onProgress((copied.toFloat() / size).coerceIn(0f, 1f))
                }
            }
        }
        onProgress(1f)
        return OfflineFile(
            output.toString(), name, copied, track.catalogUri, candidate.format.label,
            candidate.bitrate, candidate.sampleRate, candidate.isLossless, candidate.isHiRes,
        )
    } catch (error: Throwable) {
        runCatching { DocumentsContract.deleteDocument(contentResolver, output) }
        throw error
    } finally {
        connection.disconnect()
    }
}

private const val MaxOfflineAudioBytes = 1024L * 1024 * 1024

private fun openTrustedAudioConnection(value: String): HttpURLConnection {
    var next = value
    repeat(4) {
        require(isTrustedOnlineAudioUrl(next)) { "The provider returned an untrusted audio location" }
        val connection = URL(next).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "VibeArc/0.9.0 (Android music player)")
        val code = connection.responseCode
        if (code in 200..299) return connection
        val redirect = connection.getHeaderField("Location")
        connection.disconnect()
        if (code !in 300..399 || redirect.isNullOrBlank()) error("Audio download failed ($code)")
        next = URL(URL(next), redirect).toString()
    }
    error("Too many audio redirects")
}

internal fun Context.deleteOfflineFile(file: OfflineFile): Boolean = runCatching {
    DocumentsContract.deleteDocument(contentResolver, Uri.parse(file.uri))
}.getOrDefault(false)
