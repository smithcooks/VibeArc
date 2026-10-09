package com.vibearc.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.util.AtomicFile
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
    private var diskDirectory: File? = null

    fun initialize(cacheDir: File) {
        diskDirectory = File(cacheDir, "online-artwork").apply { mkdirs() }
    }

    fun peek(uri: String, target: Int): Artwork? = cache.get("$uri@$target")
        ?: cache.snapshot().entries.firstOrNull { (key) -> key.startsWith("$uri@") }?.value

    suspend fun load(uri: String, target: Int): Artwork? {
        val key = "$uri@$target"
        fun cached(): Artwork? = cache.get(key) ?: cache.snapshot().entries.firstOrNull { (candidate) ->
            candidate.startsWith("$uri@") && (candidate.substringAfterLast('@').toIntOrNull() ?: 0) >= target
        }?.let { cache.get(it.key) }
        cached()?.let { return it }
        // Loads waiting for a slot cancel when their row leaves composition.
        return withContext(Dispatchers.IO) {
            permits.withPermit {
                cached() ?: fetch(uri, target)?.also { cache.put(key, it) }
            }
        }
    }

    fun clear() = cache.evictAll()

    private fun fetch(value: String, target: Int): Artwork? = runCatching {
        val uri = Uri.parse(value)
        val bytes = if (uri.scheme == "https") {
            val artworkUrl = artworkUrlForTarget(value, target)
            val name = java.security.MessageDigest.getInstance("SHA-256").digest(artworkUrl.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val diskFile = diskDirectory?.let { File(it, name) }
            val saved = diskFile?.takeIf { it.isFile }?.let { file ->
                synchronized(this) {
                    runCatching { AtomicFile(file).openRead().use { readBounded(it, 8 * 1024 * 1024) } }.getOrNull()
                }
            }
            saved ?: run {
                val connection = URL(artworkUrl).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 8_000
                    connection.readTimeout = 8_000
                    connection.setRequestProperty("User-Agent", "VibeArc/0.9.0")
                    check(connection.responseCode in 200..299)
                    connection.inputStream.use { readBounded(it, 8 * 1024 * 1024) }.also { downloaded ->
                        runCatching { synchronized(this) {
                            diskFile?.let { file ->
                                val atomic = AtomicFile(file)
                                val output = atomic.startWrite()
                                try {
                                    output.write(downloaded)
                                    atomic.finishWrite(output)
                                } catch (failure: Exception) {
                                    atomic.failWrite(output)
                                    throw failure
                                }
                            }
                            val files = diskDirectory?.listFiles().orEmpty().filter { it.name.length == 64 }.sortedBy(File::lastModified)
                            var size = files.sumOf(File::length)
                            for (file in files) {
                                if (size <= 48 * 1024 * 1024) break
                                val length = file.length()
                                if (file.delete()) size -= length
                            }
                        } }
                    }
                } finally { connection.disconnect() }
            }
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
