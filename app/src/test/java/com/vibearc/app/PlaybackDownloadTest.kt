package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

class PlaybackDownloadTest {
    private val track = Track("Test recording", "Test artist", "Test album", "https://music.youtube.com/watch?v=abcdefghijk")
    private val mediaUrl = "https://rr1---sn-test.googlevideo.com/videoplayback?expire=123"

    @Test fun `main app downloads use the confirmed playback source and YouTube catalog`() {
        assertTrue(supportsPlaybackDownload(track))
        assertTrue(supportsPlaybackDownload(track, true))
        assertTrue(supportsPlaybackDownload(track.copy(uri=mediaUrl, sourceUri=track.uri), true))
        assertFalse(supportsPlaybackDownload(track, false))
        assertFalse(supportsPlaybackDownload(track.copy(uri="https://example.com/song"), true))
        assertFalse(supportsPlaybackDownload(track.copy(uri="file:///song", sourceUri=track.uri), true))
        assertFalse(isPlaybackDownloadUrl("https://googlevideo.com.attacker.example/audio"))
        assertFalse(isPlaybackDownloadUrl("https://user:password@googlevideo.com/audio"))
        assertFalse(isPlaybackDownloadUrl("http://googlevideo.com/audio"))
        assertFalse(isPlaybackDownloadUrl("https://googlevideo.com:444/audio"))
        assertTrue(isPlaybackDownloadUrl(mediaUrl))
        assertFalse(isLegalAudioUrl(mediaUrl)) // Manual sources remain unchanged.
    }

    @Test fun `resolved playback bytes replace a failed provider partial and report progress`() = withFile { file ->
        file.writeText("old provider bytes")
        val bytes = ByteArray(4096) { (it % 256).toByte() }
        val connection = FakeAudioConnection(bytes)
        var progress = 0L
        var attached = false
        downloadPlaybackAudio(track, file, { false }, { count, _ -> progress=count },
            resolve={ it.copy(uri=mediaUrl) }, invalidate={ fail("No refresh needed") }, open={ connection },
            connectionChanged={ attached=it!=null })
        assertArrayEquals(bytes, file.readBytes())
        assertEquals(4096L, progress)
        assertTrue(connection.disconnected)
        assertFalse(attached)
    }

    @Test fun `expired playback URL is refreshed once without mixing files`() = withFile { file ->
        var resolutions = 0
        var refreshes = 0
        var opens = 0
        val bytes = ByteArray(2048) { 7 }
        val expired = FakeAudioConnection(ByteArray(0), code=403)
        val fresh = FakeAudioConnection(bytes)
        downloadPlaybackAudio(track, file, { false }, { _, _ -> },
            resolve={ resolutions++; it.copy(uri=mediaUrl) },
            invalidate={ assertEquals(track.catalogUri, it); refreshes++ },
            open={ if(opens++==0) expired else fresh })
        assertEquals(2, resolutions)
        assertEquals(1, refreshes)
        assertArrayEquals(bytes, file.readBytes())
        assertTrue(expired.disconnected && fresh.disconnected)
    }

    @Test fun `cancelled transfer is not refreshed or retried`() = withFile { file ->
        var cancelled = false
        var resolutions = 0
        val connection = FakeAudioConnection(ByteArray(100_000))
        try {
            downloadPlaybackAudio(track, file, { cancelled }, { _, _ -> cancelled=true },
                resolve={ resolutions++; it.copy(uri=mediaUrl) },
                invalidate={ fail("Cancellation must not refresh") }, open={ connection })
            fail("Must pause")
        } catch (_: DownloadPaused) {}
        assertEquals(1, resolutions)
        assertTrue(connection.disconnected)
        assertTrue(file.length() in 1 until 100_000)
    }

    @Test fun `HTML truncated and private redirect responses cannot become saved audio`() = withFile { file ->
        for(connection in listOf(
            FakeAudioConnection(ByteArray(100), mime="text/html"),
            FakeAudioConnection(ByteArray(100), expectedSize=200),
            FakeAudioConnection(ByteArray(100), finalUrl="https://127.0.0.1/audio"),
            FakeAudioConnection(ByteArray(100), code=206, range="bytes 10-109/110"),
        )) {
            try {
                downloadPlaybackAudio(track, file, { false }, { _, _ -> },
                    resolve={ it.copy(uri=mediaUrl) }, invalidate={}, open={ connection })
                fail("Invalid payload must fail")
            } catch (error: IllegalStateException) {
                assertFalse(error.message.orEmpty().contains("127.0.0.1"))
            }
            assertTrue(connection.disconnected)
        }
    }

    private fun withFile(work: (File)->Unit) {
        val directory=Files.createTempDirectory("vibearc-playback-download").toFile()
        val file=File(directory,"track.part")
        try { work(file) } finally { file.delete(); directory.delete() }
    }

    private class FakeAudioConnection(
        private val bytes: ByteArray,
        private val code: Int=200,
        private val mime: String="audio/mp4",
        private val expectedSize: Long=bytes.size.toLong(),
        finalUrl: String="https://rr1---sn-test.googlevideo.com/videoplayback",
        private val range: String?=null,
    ): HttpURLConnection(URL(finalUrl)) {
        var disconnected=false
        override fun connect() {}
        override fun disconnect() { disconnected=true }
        override fun usingProxy()=false
        override fun getResponseCode()=code
        override fun getContentType()=mime
        override fun getContentLengthLong()=expectedSize
        override fun getHeaderField(name: String?)=if(name.equals("Content-Range",true)) range else null
        override fun getInputStream()=ByteArrayInputStream(bytes)
    }
}
