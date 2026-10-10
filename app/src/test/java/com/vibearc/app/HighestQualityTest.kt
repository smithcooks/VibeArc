package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class HighestQualityTest {
    @Test fun `highest keeps the best supplied lossless recording ahead of compressed bitrate labels`() {
        val sources = listOf(AudioCandidate("compressed", 320, AudioFormat.MP3),
            AudioCandidate("lossless", 256, AudioFormat.FLAC))
        assertEquals("lossless", selectAudioCandidate(sources)?.url)
    }
    @Test fun `equal bitrate highest sources prefer Opus without choosing a lower bitrate rendition`() {
        val sources = listOf(AudioCandidate("aac", 160, AudioFormat.AAC),
            AudioCandidate("opus", 160, AudioFormat.OPUS), AudioCandidate("low-opus", 96, AudioFormat.OPUS))
        assertEquals("opus", selectAudioCandidate(sources)?.url)
    }
}
