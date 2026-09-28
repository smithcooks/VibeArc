package com.vibearc.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal data class Artwork(val bitmap: Bitmap, val accent: Color)

internal object ArtworkCache {
    // URI + display size identifies immutable artwork for this process; eviction is byte-bounded.
    private val cache = object : LruCache<String, Artwork>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Artwork) = value.bitmap.allocationByteCount
    }
    private val permits = Semaphore(2)

    suspend fun load(uri: String, target: Int): Artwork? {
        val key = "$uri@$target"
        cache.get(key)?.let { return it }
        // Loads waiting for a slot cancel when their row leaves composition.
        return withContext(Dispatchers.IO) {
            permits.withPermit {
                cache.get(key) ?: fetch(uri, target)?.also { cache.put(key, it) }
            }
        }
    }

    fun clear() = cache.evictAll()

    private fun fetch(value: String, target: Int): Artwork? = runCatching {
        val uri = Uri.parse(value)
        val bytes = if (uri.scheme == "https") {
            val connection = URL(value).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("User-Agent", "VibeArc/0.8")
                check(connection.responseCode in 200..299)
                connection.inputStream.use { readBounded(it, 8 * 1024 * 1024) }
            } finally { connection.disconnect() }
        } else if (uri.scheme == "file") {
            File(requireNotNull(uri.path)).inputStream().use { readBounded(it, 8 * 1024 * 1024) }
        } else return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        require(options.outWidth in 1..16_384 && options.outHeight in 1..16_384)
        options.inSampleSize = artworkSampleSize(options.outWidth, options.outHeight, target)
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        Artwork(bitmap, bitmap.averageAccent())
    }.getOrNull()
}
