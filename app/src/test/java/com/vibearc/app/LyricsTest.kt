package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTest {
    @Test
    fun `LRC parser reads timestamps and skips metadata`() {
        val lyrics = """
            [ar:Artist]
            [00:01.25]First line
            [01:02.003]Second line
        """.trimIndent()

        assertEquals(
            listOf(LyricLine(1_250, "First line"), LyricLine(62_003, "Second line")),
            parseLrc(lyrics),
        )
    }

    @Test
    fun `active lyric follows playback position`() {
        val lines = listOf(LyricLine(1_000, "One"), LyricLine(2_500, "Two"))

        assertEquals(-1, activeLyricIndex(lines, 500))
        assertEquals(0, activeLyricIndex(lines, 2_000))
        assertEquals(1, activeLyricIndex(lines, 3_000))
    }

    @Test
    fun `enhanced LRC parser keeps word timestamps`() {
        val line = parseLrc("[00:01.00]<00:01.00>Hello <00:01.50>world").single()

        assertEquals("Hello world", line.text)
        assertEquals(
            listOf(LyricWord(1_000, "Hello "), LyricWord(1_500, "world")),
            line.words,
        )
        assertEquals(0, activeLyricWordIndex(line, 1_200))
        assertEquals(1, activeLyricWordIndex(line, 1_700))
    }

    @Test
    fun `lyrics response prefers synchronized lines`() {
        val json = """{"instrumental":false,"plainLyrics":"Plain","syncedLyrics":"[00:01.00]Synced"}"""

        assertEquals(
            LyricsDocument(listOf(LyricLine(1_000, "Synced")), listOf("Plain"), false),
            parseLyricsResponse(json),
        )
    }

    @Test
    fun `lyrics search selects matching cleaned metadata instead of the first result`() {
        val response = """[
            {"trackName":"Other","artistName":"Someone","duration":200,"instrumental":false,"plainLyrics":"Wrong","syncedLyrics":""},
            {"trackName":"Loser","artistName":"Tame Impala","duration":232,"instrumental":false,"plainLyrics":"Right","syncedLyrics":"[00:01.00]Right"}
        ]""".trimIndent()
        val track = Track(
            "Loser (Official Music Video)",
            "Tame Impala - Topic",
            "YouTube Music",
            "https://example.com/loser",
            durationMs = 232_000,
        )

        assertEquals("Loser", lyricsSearchTitle(track.title))
        assertEquals("Tame Impala", lyricsSearchArtist(track.artist))
        assertEquals("Right", parseLyricsSearchResponse(response, track)?.syncedLines?.single()?.text)
    }

    @Test
    fun `LRC export preserves millisecond timestamps`() {
        assertEquals(
            "[00:01.250]First\n[01:02.003]Second",
            encodeLrc(listOf(LyricLine(1_250, "First"), LyricLine(62_003, "Second"))),
        )
        assertEquals("A_B.lrc", lyricsFileName("A/B"))
    }
}
