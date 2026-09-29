package com.vibearc.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import java.util.concurrent.CancellationException

internal data class OfflineFile(
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val sourceUri: String = "",
)

internal fun Track.canCopyOffline(): Boolean = Uri.parse(uri).scheme in setOf("content", "file")

internal fun safeOfflineFileName(value: String): String = value.trim()
    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    .take(120)
    .ifBlank { "audio" }

internal object OfflineFileCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(files: List<OfflineFile>): String = files.joinToString("\n") { file ->
        listOf(file.uri, file.name, file.sizeBytes.toString(), file.sourceUri)
            .joinToString("|") { encoder.encodeToString(it.toByteArray(UTF_8)) }
    }

    fun decode(value: String): List<OfflineFile> = value.lineSequence().mapNotNull { row ->
        val fields = row.split('|')
        if (fields.size !in 3..4) return@mapNotNull null
        runCatching {
            OfflineFile(
                String(decoder.decode(fields[0]), UTF_8),
                String(decoder.decode(fields[1]), UTF_8),
                String(decoder.decode(fields[2]), UTF_8).toLong().coerceAtLeast(0L),
                fields.getOrNull(3)?.let { String(decoder.decode(it), UTF_8) }.orEmpty(),
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

internal fun Context.deleteOfflineFile(file: OfflineFile): Boolean = runCatching {
    DocumentsContract.deleteDocument(contentResolver, Uri.parse(file.uri))
}.getOrDefault(false)
