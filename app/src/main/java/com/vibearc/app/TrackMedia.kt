package com.vibearc.app

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

@Composable
internal fun TrackArtwork(
    track: Track,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onAccent: (Color) -> Unit = {},
    sizePx: Int = 160,
) {
    val accentCallback by rememberUpdatedState(onAccent)
    var artwork by remember(track.artworkUri, sizePx) {
        mutableStateOf(
            track.artworkUri.takeIf(String::isNotBlank)
                ?.let { ArtworkCache.peek(it, sizePx) }
                ?.bitmap?.asImageBitmap(),
        )
    }
    LaunchedEffect(track.artworkUri, sizePx) {
        val loaded = track.artworkUri.takeIf(String::isNotBlank)?.let {
            ArtworkCache.load(it, sizePx)
        }
        artwork = loaded?.bitmap?.asImageBitmap()
        loaded?.let { accentCallback(it.accent) }
    }
    val loadedArtwork = artwork
    if (loadedArtwork == null) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(Color(0xFF4A3426), Color(0xFF181513))),
            ),
        )
    } else {
        Image(
            bitmap = loadedArtwork,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

internal fun android.graphics.Bitmap.averageAccent(): Color {
    val sample = android.graphics.Bitmap.createScaledBitmap(this, 12, 12, true)
    val pixels = IntArray(144).also { sample.getPixels(it, 0, 12, 0, 0, 12, 12) }
    val colorful = pixels.filter { pixel ->
        val max = maxOf(android.graphics.Color.red(pixel), android.graphics.Color.green(pixel), android.graphics.Color.blue(pixel))
        val min = minOf(android.graphics.Color.red(pixel), android.graphics.Color.green(pixel), android.graphics.Color.blue(pixel))
        max - min > 24 && max > 70
    }.ifEmpty { pixels.toList() }
    val red = colorful.sumOf(android.graphics.Color::red) / colorful.size
    val green = colorful.sumOf(android.graphics.Color::green) / colorful.size
    val blue = colorful.sumOf(android.graphics.Color::blue) / colorful.size
    if (sample !== this) sample.recycle()
    return Color(red, green, blue)
}

internal fun Context.trackFrom(uri: Uri): Track {
    val fileName = contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }.orEmpty()
    val fallbackTitle = fileName.substringBeforeLast('.').ifBlank { "Local audio" }
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val artworkUri = retriever.embeddedPicture
                ?.takeIf(ByteArray::isNotEmpty)
                ?.let { cacheArtwork(uri, it) }
                .orEmpty()
            Track(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf(String::isNotBlank) ?: fallbackTitle,
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf(String::isNotBlank) ?: "On this device",
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.takeIf(String::isNotBlank) ?: "Imported",
                uri = uri.toString(),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
                artworkUri = artworkUri,
                folder = displayFolderFromPath(uri.path),
            )
        } finally {
            retriever.release()
        }
    }.getOrElse {
        Track(fallbackTitle, "On this device", "Imported", uri.toString())
    }
}

private fun Context.cacheArtwork(uri: Uri, bytes: ByteArray): String {
    val directory = File(filesDir, "artwork").apply { mkdirs() }
    return File(directory, "${uri.toString().hashCode()}.image")
        .apply { writeBytes(bytes) }
        .toURI()
        .toString()
}
