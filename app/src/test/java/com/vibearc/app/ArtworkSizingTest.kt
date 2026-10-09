package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class ArtworkSizingTest {
    @Test fun prefetchUsesActualNextThenPreviousWithoutCurrentOrDuplicates() {
        assertEquals(listOf("shuffled-next", "previous"), nearbyArtworkUris("current", "shuffled-next", "previous"))
        assertEquals(listOf("next"), nearbyArtworkUris("current", "next", "next"))
        assertEquals(emptyList<String>(), nearbyArtworkUris("current", "current", null))
        assertEquals(emptyList<String>(), nearbyArtworkUris(null, "", " "))
        assertEquals(listOf("previous"), nearbyArtworkUris("current", null, "previous"))
    }
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
    @Test fun googleArtworkMatchesItsDisplayTarget() {
        assertEquals(
            "https://lh3.googleusercontent.com/cover=w160-h160-l90-rj",
            artworkUrlForTarget("https://lh3.googleusercontent.com/cover=w1024-h1024-l90-rj", 160),
        )
        assertEquals("https://example.com/cover.jpg", artworkUrlForTarget("https://example.com/cover.jpg", 160))
    }
}
