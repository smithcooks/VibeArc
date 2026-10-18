package com.vibearc.app

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream

internal fun convertDownloadToMp3(input: File, output: File, tags: Mp3Tags, cancelled: () -> Boolean, progress: (Int) -> Unit) {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var encoder: Mp3Encoder? = null
    var started = false
    var success = false
    val conversionStarted = System.nanoTime()
    try {
        extractor.setDataSource(input.absolutePath)
        val tracks = (0 until extractor.trackCount).map { it to extractor.getTrackFormat(it) }
        check(tracks.none { it.second.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("video/") }) { "This source is not audio-only" }
        val (index, format) = tracks.single { it.second.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/") }
        extractor.selectTrack(index)
        val duration = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
        format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        decoder = codec
        codec.configure(format, null, null, 0); codec.start(); started = true
        FileOutputStream(output).use { stream ->
          stream.buffered(64 * 1024).use { buffered ->
            tags.writeTo(buffered)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var floatPcm = false
            var lastOutput = System.nanoTime()
            var decodedBytes = 0L
            var lastDiskCheck = 0L
            while (!outputDone) {
                if (cancelled()) throw DownloadPaused()
                check(System.nanoTime() - lastOutput < 30_000_000_000L) { "Audio decoder stopped responding" }
                if (!inputDone) {
                    val slot = codec.dequeueInputBuffer(10_000)
                    if (slot >= 0) {
                        val buffer = codec.getInputBuffer(slot) ?: error("Audio decoder input unavailable")
                        val count = extractor.readSampleData(buffer, 0)
                        if (count < 0) { codec.queueInputBuffer(slot, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                        else { codec.queueInputBuffer(slot, 0, count, extractor.sampleTime, 0); extractor.advance() }
                    }
                }
                val slot = codec.dequeueOutputBuffer(info, 10_000)
                if (slot == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    check(encoder == null) { "Decoded audio format changed mid-song" }
                    val pcm = codec.outputFormat
                    val encoding = if (pcm.containsKey(MediaFormat.KEY_PCM_ENCODING)) pcm.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                    check(encoding in setOf(AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_FLOAT)) { "Unsupported decoded PCM format" }
                    floatPcm = encoding == AudioFormat.ENCODING_PCM_FLOAT
                    encoder = Mp3Encoder(pcm.getInteger(MediaFormat.KEY_SAMPLE_RATE), pcm.getInteger(MediaFormat.KEY_CHANNEL_COUNT), buffered)
                    lastOutput = System.nanoTime()
                } else if (slot >= 0) {
                    try {
                        if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val now=System.nanoTime()
                            if(now-lastDiskCheck>=1_000_000_000L) {
                                check(output.parentFile!!.usableSpace > 4L * 1024 * 1024) { "Not enough storage for MP3 conversion" }
                                lastDiskCheck=now
                            }
                            val pcm = codec.getOutputBuffer(slot) ?: error("Audio decoder output unavailable")
                            pcm.position(info.offset); pcm.limit(info.offset + info.size)
                            checkNotNull(encoder) { "Missing decoded audio format" }.write(pcm, floatPcm)
                            decodedBytes += info.size
                            if (duration > 0) progress((info.presentationTimeUs * 100 / duration).toInt().coerceIn(0, 99))
                        }
                        outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        lastOutput = System.nanoTime()
                    } finally { codec.releaseOutputBuffer(slot, false) }
                }
            }
            check(decodedBytes > 0) { "No playable audio was decoded" }
            encoder!!.finish(); buffered.flush(); stream.fd.sync()
          }
        }
        check(output.length() > 0) { "MP3 output is empty" }
        success = true
        android.util.Log.i("VibeArcMp3", "Native conversion completed in ${(System.nanoTime()-conversionStarted)/1_000_000} ms")
        progress(100)
    } finally {
        encoder?.close()
        if (started) runCatching { decoder?.stop() }
        decoder?.release(); extractor.release()
        if (!success) output.delete()
    }
}
