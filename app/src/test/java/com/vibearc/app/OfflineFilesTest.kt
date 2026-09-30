package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineFilesTest {
    @Test
    fun `offline file names remove provider separators`() {
        assertEquals("Artist _ Song_.mp3", safeOfflineFileName("Artist / Song?.mp3"))
        assertEquals("audio", safeOfflineFileName("  "))
    }

    @Test
    fun `offline records round trip`() {
        val records = listOf(OfflineFile("content://tree/file-1", "Song.mp3", 1_024))
        assertEquals(records, OfflineFileCodec.decode(OfflineFileCodec.encode(records)))
    }

    @Test
    fun `only trusted https audio hosts can be downloaded`() {
        assertEquals(true, isTrustedOnlineAudioUrl("https://rr1---sn.example.googlevideo.com/videoplayback"))
        assertEquals(false, isTrustedOnlineAudioUrl("http://rr1---sn.example.googlevideo.com/videoplayback"))
        assertEquals(false, isTrustedOnlineAudioUrl("https://googlevideo.com.evil.example/audio"))
    }
}
