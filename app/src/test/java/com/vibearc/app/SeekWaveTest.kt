package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class SeekWaveTest {
    @Test fun `single wave has exact amplitude and joins continuously across periods`() {
        assertEquals(0f, seekWaveY(0f, 28f, 3f), .0001f)
        assertEquals(3f, seekWaveY(7f, 28f, 3f), .0001f)
        assertEquals(-3f, seekWaveY(21f, 28f, 3f), .0001f)
        for (i in 0..28) assertEquals(seekWaveY(i.toFloat(), 28f, 3f), seekWaveY(i + 28f, 28f, 3f), .0001f)
    }
    @Test fun `invalid wavelength is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { seekWaveY(1f, 0f, 3f) }
    }
}
