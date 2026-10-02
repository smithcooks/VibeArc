package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTest {
    @Test
    fun `free lyrics search survives failed or empty exact lookup`() {
        val track = Track("Loser (Official Music Video)", "Tame Impala - Topic", "YouTube Music", "https://example.com/song", durationMs = 232_000)
        val search = """[{"trackName":"Loser","artistName":"Tame Impala","duration":232.5,"syncedLyrics":"[00:01.00]Matched"}]"""
        for (failure in listOf("timeout", "empty", "bad json", "http")) {
            val urls = mutableListOf<String>()
            val lyrics = fetchLrclibLyrics(track) { url ->
                urls += url
                if (url.contains("/search?")) 200 to search else when (failure) {
                    "timeout" -> throw java.net.SocketTimeoutException()
                    "empty" -> 200 to """{"plainLyrics":null,"syncedLyrics":null}"""
                    "bad json" -> 200 to "not json"
                    else -> 503 to null
                }
            }
            assertEquals("Matched", lyrics?.syncedLines?.single()?.text)
            assertEquals(2, urls.size)
            assertEquals(false, urls.first().contains("Official"))
            assertEquals(false, urls.first().contains("Topic"))
        }
    }

    @Test
    fun `instrumental exact result does not trigger another provider`() {
        var requests = 0
        val lyrics = fetchLrclibLyrics(Track("Song", "Artist", "Album", "content://song")) {
            requests++
            200 to """{"instrumental":true}"""
        }
        assertEquals(true, lyrics?.instrumental)
        assertEquals(1, requests)
    }

    @Test
    fun `empty search candidate cannot hide real lyrics`() {
        val response = """[
            {"trackName":"Song","artistName":"Artist","duration":120,"syncedLyrics":null,"plainLyrics":null},
            {"trackName":"Song","artistName":"Artist","duration":125,"plainLyrics":"Found"}
        ]"""
        assertEquals(listOf("Found"), parseLyricsSearchResponse(response, Track("Song", "Artist", "", "content://song", durationMs=120_000))?.plainLines)
    }

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
    fun `enhanced LRC editor round trip preserves word timestamps`() {
        val line = LyricLine(1_000, "Hello world", listOf(LyricWord(1_000, "Hello "), LyricWord(1_500, "world")))

        assertEquals(listOf(line), parseLrc(encodeEnhancedLrc(listOf(line))))
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

    @Test
    fun `KRC parser converts word offsets into absolute timestamps`() {
        val lines = parseKrc("[1000,1800]<0,400,0>Hello <450,500,0>world")

        assertEquals(
            listOf(
                LyricLine(
                    1_000,
                    "Hello world",
                    listOf(LyricWord(1_000, "Hello "), LyricWord(1_450, "world")),
                ),
            ),
            lines,
        )
    }

    @Test
    fun `lyrics offset moves line and word timestamps without going negative`() {
        val original = LyricsDocument(
            listOf(LyricLine(500, "Hi", listOf(LyricWord(500, "Hi")))),
            emptyList(),
            false,
        )

        assertEquals(0, original.shifted(-1_000).syncedLines.single().startMs)
        assertEquals(750, original.shifted(250).syncedLines.single().words.single().startMs)
    }
}
