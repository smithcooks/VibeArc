package com.vibearc.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioTuningTest {
    @Test
    fun `equalizer values are bounded and studio curve is additive`() {
        val user = FloatArray(15) { if (it == 0) 20f else -20f }

        assertArrayEquals(
            floatArrayOf(12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f, -12f),
            effectiveEqualizerGains(user, studioMaster = true),
            0.001f,
        )
    }

    @Test
    fun `crossfade uses equal power gains`() {
        assertEquals(1f, crossfadeGains(0f).outgoing, .001f)
        assertEquals(0f, crossfadeGains(0f).incoming, .001f)
        assertEquals(0f, crossfadeGains(1f).outgoing, .001f)
        assertEquals(1f, crossfadeGains(1f).incoming, .001f)
        assertEquals(0.707f, crossfadeGains(.5f).incoming, .001f)
    }

    @Test
    fun `lossless and hi res labels describe the source only`() {
        assertEquals("Lossless source", sourceQualityLabel("audio/flac", 44_100))
        assertEquals("Hi-Res source · output verification required", sourceQualityLabel("audio/flac", 96_000))
        assertEquals("Lossy source", sourceQualityLabel("audio/opus", 48_000))
    }
}
