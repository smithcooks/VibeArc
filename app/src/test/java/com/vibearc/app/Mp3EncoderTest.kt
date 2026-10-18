package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class Mp3EncoderTest {
    @Test fun `JNI rejects invalid PCM windows before reading native memory`() {
        val handle=NativeMp3.create(48_000,2)
        try {
            val direct=ByteBuffer.allocateDirect(64)
            for((offset,size) in listOf(-1 to 16,60 to 16,0 to 3,0 to 65536)) {
                try { NativeMp3.encode(handle,direct,offset,size,false,ByteArray(24576));fail("Invalid PCM window accepted") }
                catch(_: IllegalArgumentException) { }
            }
            try { NativeMp3.encode(handle,ByteBuffer.allocate(64),0,16,false,ByteArray(24576));fail("Heap JNI buffer accepted") }
            catch(_: IllegalArgumentException) { }
            try { NativeMp3.encode(handle,direct,0,16,false,ByteArray(1));fail("Small output buffer accepted") }
            catch(_: IllegalArgumentException) { }
        } finally { NativeMp3.destroy(handle) }
    }

    @Test fun `low sample-rate PCM is safely resampled without overflowing native output`() {
        val out=ByteArrayOutputStream()
        Mp3Encoder(8000,1,out).use { encoder ->
            val pcm=ByteBuffer.allocateDirect(8000*2).order(ByteOrder.LITTLE_ENDIAN)
            repeat(8000) { pcm.putShort((sin(it*2*Math.PI*440/8000)*10000).toInt().toShort()) }
            pcm.flip();encoder.write(pcm,false);encoder.finish()
        }
        assertTrue(out.size()>35000)
    }

    @Test fun `native direct buffers respect window and close is idempotent`() {
        val out=ByteArrayOutputStream()
        val pcm=ByteBuffer.allocateDirect(16+4096*4+16).order(ByteOrder.LITTLE_ENDIAN)
        pcm.position(16)
        repeat(4096) { pcm.putShort(1200);pcm.putShort(-1200) }
        val end=pcm.position()
        pcm.limit(end);pcm.position(16)
        val encoder=Mp3Encoder(48_000,2,out)
        encoder.write(pcm,false)
        assertEquals(end,pcm.position())
        encoder.finish();encoder.close();encoder.close()
        try { encoder.finish();fail("Closed native handle reused") }
        catch(_: IllegalStateException) { }
        assertTrue(out.size()>0)
    }

    @Test fun `PCM becomes real 320 kbps MPEG layer three audio not a renamed container`() {
        for (channels in 1..2) {
            val output = ByteArrayOutputStream()
            Mp3Encoder(48_000, channels, output).use { encoder ->
                repeat(50) { block ->
                    val pcm = ByteBuffer.allocate(960 * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
                    repeat(960) { frame ->
                        val sample = (sin((block * 960 + frame) * 2 * Math.PI * 440 / 48_000) * 12_000).toInt().toShort()
                        repeat(channels) { pcm.putShort(sample) }
                    }
                    pcm.flip()
                    encoder.write(pcm, false)
                }
                encoder.finish()
            }
            val bytes = output.toByteArray()
            assertTrue(bytes.size > 35_000)
            assertEquals(255, bytes[0].toInt() and 255)
            assertEquals(0xfa, bytes[1].toInt() and 0xfe) // MPEG-1 Layer III, not MP4/WebM.
            assertEquals(14, (bytes[2].toInt() ushr 4) and 15) // 320 kbps.
            java.io.File("build/mp3-test-$channels.mp3").writeBytes(bytes)
        }
    }

    @Test fun `float decoder output is supported and invalid channel layouts fail`() {
        val out = ByteArrayOutputStream()
        Mp3Encoder(44_100, 2, out).use { encoder ->
            val pcm = ByteBuffer.allocate(4096 * 4).order(ByteOrder.LITTLE_ENDIAN)
            repeat(4096) { pcm.putFloat(if (it % 2 == 0) 0.25f else -0.25f) }
            pcm.flip()
            encoder.write(pcm, true)
            encoder.finish()
        }
        assertTrue(out.size() > 0)
        try { Mp3Encoder(48_000, 6, out); fail("Must reject unsupported multichannel PCM") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun `MP3 names cannot escape Music directory and always have mp3 extension`() {
        val name = mp3DownloadName(Track("../Song.webm", "A/B", "", "https://example.com/song"))
        assertTrue(name.endsWith(".mp3"))
        assertFalse(name.contains('/'))
        assertFalse(name.contains('\\'))
        assertFalse(name.contains("\u0000"))
    }
}
