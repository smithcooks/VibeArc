package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class RecordingLabelsTest {
    @Test fun `stored metadata removes popularity without changing identity likes or credits`() {
        val original=Track("Loser • 128 million plays","Tame Impala, 128M plays","Album","https://catalog.example/loser",true,222_000)
        val saved=LibraryCodec.decode(LibraryCodec.encode(listOf(original))).single()
        assertEquals("Loser",saved.title)
        assertEquals("Tame Impala",saved.artist)
        assertEquals(original.catalogUri,saved.catalogUri)
        assertTrue(saved.isFavorite)
        assertEquals("The Weeknd, JENNIE & Lily Rose Depp",cleanRecordingLabel("The Weeknd, JENNIE & Lily Rose Depp"))
        assertEquals("100 Million",cleanRecordingLabel("100 Million"))
    }
}
