package com.vibearc.app

import java.io.Closeable
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Native LAME; decoded PCM crosses JNI in blocks, never sample-by-sample in Kotlin. */
internal class Mp3Encoder(sampleRate: Int, private val channels: Int, private val output: OutputStream) : Closeable {
    private var handle: Long
    private val encoded = ByteArray(24_576)
    private var heapScratch: ByteBuffer? = null
    private var finished = false
    val outputSampleRate: Int = if(sampleRate == 48_000) 48_000 else 44_100
    private val inputFramesPerBlock = minOf(8192L,8192L*sampleRate/outputSampleRate).toInt().coerceAtLeast(1)

    init {
        require(channels in 1..2 && sampleRate in 8_000..192_000) { "Unsupported MP3 input format" }
        handle = NativeMp3.create(sampleRate,channels)
        check(handle != 0L) { "Could not initialize native MP3 encoder" }
    }

    @Synchronized fun write(pcm: ByteBuffer, floatPcm: Boolean) {
        check(!finished && handle != 0L) { "MP3 encoder is closed" }
        pcm.order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = channels * if (floatPcm) 4 else 2
        require(pcm.remaining() % frameBytes == 0) { "Incomplete decoded PCM frame" }
        while (pcm.hasRemaining()) {
            val bytes = minOf(inputFramesPerBlock * frameBytes, pcm.remaining())
            val buffer = if(pcm.isDirect) pcm else {
                val scratch = heapScratch ?: ByteBuffer.allocateDirect(8192 * 2 * 4).also { heapScratch=it }
                scratch.clear()
                val window=pcm.duplicate().apply { limit(position()+bytes) }
                scratch.put(window); scratch.flip(); scratch
            }
            val size = NativeMp3.encode(handle,buffer,if(pcm.isDirect) pcm.position() else 0,bytes,floatPcm,encoded)
            output.write(encoded, 0, size)
            pcm.position(pcm.position()+bytes)
        }
    }

    @Synchronized fun finish() {
        check(!finished && handle != 0L) { "MP3 encoder is closed" }
        val size = NativeMp3.finish(handle,encoded)
        output.write(encoded, 0, size)
        finished = true
    }

    @Synchronized override fun close() {
        if(handle != 0L) { NativeMp3.destroy(handle);handle=0 }
    }
}

internal object NativeMp3 {
    init { System.loadLibrary("vibearcmp3") }
    external fun create(sampleRate: Int, channels: Int): Long
    external fun encode(handle: Long, pcm: ByteBuffer, offset: Int, bytes: Int, floating: Boolean, output: ByteArray): Int
    external fun finish(handle: Long, output: ByteArray): Int
    external fun destroy(handle: Long)
}

internal fun mp3DownloadName(track: Track): String =
    safeOfflineFileName("${track.artist} - ${track.title}".replace(Regex("[\\p{Cntrl}]"), "_")).take(110) + ".mp3"
