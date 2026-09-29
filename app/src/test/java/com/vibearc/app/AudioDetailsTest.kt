package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioDetailsTest {
    @Test
    fun `audio details report only observed values`() {
        assertEquals(
            "AAC · 128 kbps · 44.1 kHz · 2 ch",
            AudioDetails("audio/mp4a-latm", 128_000, 44_100, 2).label(),
        )
    }

    @Test
    fun `unknown audio details do not claim a quality tier`() {
        assertEquals("Format not reported by source", AudioDetails(null, -1, -1, -1).label())
    }
}
