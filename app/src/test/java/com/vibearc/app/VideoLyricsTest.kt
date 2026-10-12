package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class VideoLyricsTest {
    @Test fun `only validated YouTube ids become provider paths`() {
        assertEquals("abcdefghijk", lyricsVideoId("https://music.youtube.com/watch?v=abcdefghijk&list=x"))
        assertEquals("abcdefghijk", lyricsVideoId("https://youtu.be/abcdefghijk"))
        assertNull(lyricsVideoId("https://evil.example/watch?v=abcdefghijk"))
        assertNull(lyricsVideoId("https://youtube.com/watch?v=../secret"))
        assertNull(lyricsVideoId("http://youtube.com/watch?v=abcdefghijk"))
    }
    @Test fun `rich video lyrics retain word times and attribution`() {
        val doc = parseVideoLyrics("""{"success":true,"data":[{"duration":219,"richSyncLyrics":"[00:01.00]<00:01.00>Hello <00:01.50>world"}]}""", 219_000)!!
        assertEquals(2, doc.syncedLines.single().words.size)
        assertEquals("SimpMusic", doc.source)
    }
    @Test fun `wrong duration and empty first result do not mask a valid match`() {
        val body = """{"success":true,"data":[{"duration":219},{"duration":90,"syncedLyrics":"[00:01.00]Wrong"},{"duration":219,"syncedLyrics":"[00:01.00]Right"}]}"""
        assertEquals("Right", parseVideoLyrics(body, 219_000)?.syncedLines?.single()?.text)
        assertNull(parseVideoLyrics(body, 400_000))
    }
    @Test fun `provider failures and unsupported local files remain safe misses`() {
        val track = Track("Flatline", "Justin Bieber", "", "https://music.youtube.com/watch?v=abcdefghijk", durationMs=219_000)
        assertNull(fetchVideoLyrics(track) { throw java.net.SocketTimeoutException() })
        assertNull(fetchVideoLyrics(track) { 200 to "not json" })
        assertNull(fetchVideoLyrics(track.copy(uri="content://song")) { error("No network call expected") })
    }
}
