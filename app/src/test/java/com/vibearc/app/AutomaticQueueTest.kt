package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AutomaticQueueTest {
    private fun song(id: Int) = Track("Song $id", "Artist", "", "https://music.youtube.com/watch?v=track$id")

    @Test fun `eight song shelf grows to thirty without replacing its order`() {
        val queued = (0..7).map(::song)
        val additions = automaticQueueAdditions(queued, (0..45).map(::song))
        assertEquals((8..29).map(::song), additions)
        assertEquals((0..29).map(::song), queued + additions)
    }

    @Test fun `manual play next and duplicate recordings stay intact`() {
        val queued = listOf(song(0), song(9), song(1))
        val resolvedDuplicate = song(9).copy(uri = "https://audio.example/9", sourceUri = song(9).uri)
        val invalid = song(100).copy(uri = "javascript:bad")
        assertEquals(listOf(song(2)), automaticQueueAdditions(queued,
            listOf(song(0), resolvedDuplicate, song(2), song(2), invalid)))
        assertEquals(listOf(song(0), song(9), song(1)), queued)
    }

    @Test fun `full playlists and empty provider results are preserved`() {
        assertEquals(emptyList<Track>(), automaticQueueAdditions((0..35).map(::song), (36..70).map(::song)))
        assertEquals(emptyList<Track>(), automaticQueueAdditions(listOf(song(0)), emptyList()))
    }
}
