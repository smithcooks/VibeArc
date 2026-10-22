package com.vibearc.app

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

internal fun publishDownloadAudio(context: Context, source: File, track: Track, format: DownloadFormat): Uri {
    val name=mp3DownloadName(track).removeSuffix(".mp3")+".${format.extension}"
    val location=downloadExportLocation(format,Build.VERSION.SDK_INT)
    if (Build.VERSION.SDK_INT >= 29) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, location.relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if(location.collection==DownloadExportCollection.AUDIO) {
                put(MediaStore.Audio.Media.TITLE, track.title)
                put(MediaStore.Audio.Media.ARTIST, track.artist)
                put(MediaStore.Audio.Media.ALBUM, track.album)
            }
        }
        val collection=when(location.collection) {
            DownloadExportCollection.AUDIO -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            DownloadExportCollection.DOWNLOADS -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val uri = resolver.insert(collection, values)
            ?: error("Could not create ${format.extension.uppercase()} in ${location.relativePath}")
        try {
            resolver.openOutputStream(uri, "w")?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: error("Could not write ${format.extension.uppercase()} in ${location.relativePath}")
            check(resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) == 1)
            return uri
        } catch (error: Exception) { runCatching { resolver.delete(uri, null, null) }; throw error }
    }
    @Suppress("DEPRECATION")
    val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "VibeArc")
    check(folder.isDirectory || folder.mkdirs()) { "Allow storage permission to save MP3 files" }
    val file = File(folder, name.substringBeforeLast('.') + "-${UUID.randomUUID()}.${format.extension}")
    val pending = File.createTempFile(".vibearc-", ".part", folder)
    var moved = false
    try {
        FileOutputStream(pending).use { output -> source.inputStream().use { it.copyTo(output) }; output.fd.sync() }
        java.nio.file.Files.move(pending.toPath(), file.toPath()) // No replacement of another phone file.
        moved = true
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(format.mime), null)
        return Uri.fromFile(file)
    } catch (error: Exception) { if(moved) file.delete(); throw error }
    finally { pending.delete() }
}

/** Roll back only the fresh export if persisting its download record fails. */
internal fun discardFreshMp3(context: Context, uri: Uri) {
    if (uri.scheme == "content") context.contentResolver.delete(uri, null, null)
    else if (uri.scheme == "file") File(checkNotNull(uri.path)).delete()
}
