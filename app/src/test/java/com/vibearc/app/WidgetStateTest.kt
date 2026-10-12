package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class WidgetStateTest {
    @Test fun `progress uses real duration and clamps invalid positions`() {
        assertEquals(25, widgetProgress(50_000, 200_000))
        assertEquals(0, widgetProgress(-1, 200_000))
        assertEquals(100, widgetProgress(300_000, 200_000))
        assertEquals(0, widgetProgress(50_000, -1))
    }
    @Test fun `lyrics show the currently sung line rather than the next line`() {
        val document = LyricsDocument(parseLrc("[00:01.00]First\n[00:03.00]Second"), emptyList(), false)
        assertEquals("Lyrics begin shortly", widgetLyric(document, 0))
        assertEquals("First", widgetLyric(document, 2_999))
        assertEquals("Second", widgetLyric(document, 3_000))
        assertEquals("Lyrics unavailable — tap to open", widgetLyric(null, 5_000))
    }
    @Test fun `empty queue never claims active playback or exposes skip actions`() {
        val state = widgetPlayback(null, true, true, true)
        assertEquals("VibeArc", state.title)
        assertFalse(state.playing)
        assertFalse(state.previous)
        assertFalse(state.next)
        assertFalse(state.hasTrack)
    }
    @Test fun `widget metadata removes play counts and uses real queue capabilities`() {
        val state = widgetPlayback(Track("Song • 128M plays", "Artist", "", "content://song"), true, false, true)
        assertEquals("Song", state.title)
        assertEquals("Pause", state.playLabel)
        assertFalse(state.previous)
        assertTrue(state.next)
    }
    @Test fun `paused track remains available to resume`() {
        val state = widgetPlayback(Track("Song", "Artist", "", "content://song"), false, true, false)
        assertEquals("Play", state.playLabel)
        assertTrue(state.hasTrack)
    }
}
