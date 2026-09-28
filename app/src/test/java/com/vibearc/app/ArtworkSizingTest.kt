package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class ArtworkSizingTest {
    @Test fun thumbnailsUseSixteenTimesLessDecodedMemory() {
        val sample = artworkSampleSize(1024, 1024, 160)
        assertEquals(4, sample)
        assertEquals(16, sample * sample)
        assertEquals(1, artworkSampleSize(720, 720, 768))
    }
    @Test fun oversizedInputIsRejected() {
        assertArrayEquals(byteArrayOf(1, 2), readBounded(byteArrayOf(1, 2).inputStream(), 2))
        assertThrows(IllegalArgumentException::class.java) {
            readBounded(byteArrayOf(1, 2, 3).inputStream(), 2)
        }
    }
}
