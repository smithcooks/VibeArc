package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

class Mp3TagsTest {
    @Test fun `cover is embedded in APIC with unicode title and unchanged audio`() {
        // Synthetic 32x32 red JPEG; Android test compilation excludes desktop AWT.
        val cover=Base64.getDecoder().decode("/9j/4AAQSkZJRgABAgAAAQABAAD//gAPTGF2YzYzLjEuMTAxAP/bAEMACAQEBAQEBQUFBQUFBgYGBgYGBgYGBgYGBgYGBgcHBwgICAcHBwYGBwcICAgICQkJCAgICAkJCgoKDAwLCw4ODhERFP/EAE0AAQEAAAAAAAAAAAAAAAAAAAAGAQEBAQAAAAAAAAAAAAAAAAAABgcQAQAAAAAAAAAAAAAAAAAAAAARAQAAAAAAAAAAAAAAAAAAAAD/wAARCAAgACADASIAAhEAAxEA/9oADAMBAAIRAxEAPwCLASbfwAAAAAH/2Q==")
        val out=ByteArrayOutputStream()
        Mp3Tags("Café 音楽", "Artist", "Album", cover).writeTo(out)
        val tag=out.toByteArray()
        assertEquals("ID3",tag.copyOfRange(0,3).decodeToString())
        assertEquals(3,tag[3].toInt())
        val size=(6..9).fold(0) { sum,i -> (sum shl 7) or (tag[i].toInt() and 127) }
        assertEquals(tag.size-10,size)
        val frames=linkedMapOf<String,ByteArray>()
        var offset=10
        while(offset<tag.size) {
            val id=tag.copyOfRange(offset,offset+4).decodeToString()
            val length=ByteBuffer.wrap(tag,offset+4,4).int
            frames[id]=tag.copyOfRange(offset+10,offset+10+length)
            offset+=10+length
        }
        assertEquals(tag.size,offset)
        val title=frames.getValue("TIT2")
        assertEquals(1,title[0].toInt())
        assertEquals("Café 音楽",String(title.copyOfRange(1,title.size-2),Charsets.UTF_16))
        val apic=frames.getValue("APIC")
        val prefix=byteArrayOf(0)+"image/jpeg\u0000".toByteArray()+byteArrayOf(3,0)
        assertArrayEquals(prefix,apic.copyOfRange(0,prefix.size))
        assertArrayEquals(cover,apic.copyOfRange(prefix.size,apic.size))
        Mp3Encoder(48_000,2,out).use { encoder ->
            val pcm=ByteBuffer.allocate(48_000*4).order(ByteOrder.LITTLE_ENDIAN)
            repeat(48_000) { i ->
                val sample=(kotlin.math.sin(i*2*Math.PI*440/48_000)*12_000).toInt().toShort()
                pcm.putShort(sample);pcm.putShort(sample)
            }
            pcm.flip();encoder.write(pcm,false);encoder.finish()
        }
        assertEquals(255,out.toByteArray()[tag.size].toInt() and 255)
        java.io.File("build/mp3-cover-test.mp3").writeBytes(out.toByteArray())
        java.io.File("build/mp3-cover-test.jpg").writeBytes(cover)
    }

    @Test fun `missing artwork still writes song metadata and invalid artwork is rejected`() {
        val out=ByteArrayOutputStream()
        Mp3Tags("Song", "Artist", "", null).writeTo(out)
        assertFalse(out.toByteArray().decodeToString().contains("APIC"))
        for(cover in listOf(byteArrayOf(1,2,3),ByteArray(2_097_153))) {
            try { Mp3Tags("Song","Artist","",cover).writeTo(ByteArrayOutputStream());fail("Invalid image accepted") }
            catch(_: IllegalArgumentException) { }
        }
    }
}
