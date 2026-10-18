package com.vibearc.app

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream

/** ID3v2.3 lives inside the MP3 itself, including a standard front-cover APIC frame. */
internal class Mp3Tags(val title: String, val artist: String, val album: String, val coverJpeg: ByteArray?) {
    fun writeTo(output: OutputStream) {
        val frames = ByteArrayOutputStream()
        fun frame(id: String, body: ByteArray) {
            DataOutputStream(frames).apply {
                writeBytes(id); writeInt(body.size); writeShort(0); write(body)
            }
        }
        listOf("TIT2" to title, "TPE1" to artist, "TALB" to album).forEach { (id, value) ->
            if (value.isNotBlank()) frame(id, byteArrayOf(1) +
                value.replace("\u0000", "").take(4096).toByteArray(Charsets.UTF_16) + byteArrayOf(0, 0))
        }
        coverJpeg?.let { cover ->
            require(cover.size in 3..2_097_152 && cover[0] == 0xff.toByte() && cover[1] == 0xd8.toByte()) {
                "Invalid MP3 cover image"
            }
            frame("APIC", byteArrayOf(0) + "image/jpeg\u0000".toByteArray(Charsets.ISO_8859_1) +
                byteArrayOf(3, 0) + cover) // Encoding, MIME, front-cover type, empty description, JPEG.
        }
        val body = frames.toByteArray()
        output.write(byteArrayOf(73, 68, 51, 3, 0, 0))
        for (shift in intArrayOf(21, 14, 7, 0)) output.write((body.size ushr shift) and 127)
        output.write(body)
    }
}
