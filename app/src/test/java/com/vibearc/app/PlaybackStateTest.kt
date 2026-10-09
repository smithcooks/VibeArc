package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun `play next keeps shuffle enabled and preserves the other shuffled entries`() {
        assertEquals(listOf(4, 1, 2, 0, 3), prioritizeNextInShuffle(listOf(2, 4, 1, 0, 3), 1, 2))
        assertEquals(listOf(0, 1), prioritizeNextInShuffle(listOf(1, 0), 0, 1))
    }

    @Test
    fun `only a single next insertion is prioritized not a replacement removal or reorder`() {
        val previous = listOf("previous", "current", "tail")
        assertEquals(true, isNextQueueInsertion(previous, listOf("previous", "current", "current", "tail"), 1))
        assertEquals(false, isNextQueueInsertion(previous, listOf("previous", "current", "tail", "added"), 1))
        assertEquals(false, isNextQueueInsertion(previous, listOf("new", "queue", "items", "here"), 1))
        assertEquals(false, isNextQueueInsertion(previous, listOf("previous", "tail", "current"), 1))
        assertEquals(false, isNextQueueInsertion(previous, listOf("current", "tail"), 0))
        assertEquals(false, isNextQueueInsertion(emptyList(), listOf("first"), -1))
    }

    @Test
    fun `adding a song plays it immediately after the current song without replacing the queue`() {
        val queue = mutableListOf("Earlier song", "Freaked Out", "Existing next", "Existing last")
        queue.add(nextQueueInsertionIndex(1, queue.size), "One Of The Girls")
        assertEquals(listOf("Earlier song", "Freaked Out", "One Of The Girls", "Existing next", "Existing last"), queue)
    }

    @Test
    fun `play next handles empty queues and the final song`() {
        assertEquals(0, nextQueueInsertionIndex(-1, 0))
        assertEquals(3, nextQueueInsertionIndex(2, 3))
        assertEquals(0, nextQueueInsertionIndex(-1, 3))
    }

    @Test
    fun `artwork swipes go left for next and right for previous only beyond the threshold`() {
        assertEquals(1, artworkSwipeStep(-80f, 56f))
        assertEquals(-1, artworkSwipeStep(80f, 56f))
        assertEquals(1, artworkSwipeStep(-56f, 56f))
        assertEquals(-1, artworkSwipeStep(56f, 56f))
        assertEquals(0, artworkSwipeStep(55f, 56f))
        assertEquals(0, artworkSwipeStep(-55f, 56f))
        assertEquals(0, artworkSwipeStep(0f, 56f))
        assertEquals(0, artworkSwipeStep(Float.NaN, 56f))
        assertEquals(0, artworkSwipeStep(Float.POSITIVE_INFINITY, 56f))
        assertEquals(0, artworkSwipeStep(80f, 0f))
    }

    @Test
    fun `home feed includes current music and keeps playable unique tracks`() {
        val current = Track("Current", "Artist", "Album", "https://example.com/current")
        val recent = Track("Recent", "Artist", "Album", "content://media/recent")
        val local = Track("Local", "Other", "Album", "content://media/local")
        val invalid = Track("Invalid", "", "", "javascript:bad")
        assertEquals(listOf(current, recent, local), homeFeedTracks(listOf(local, recent, invalid), listOf(recent), current))
        assertEquals(emptyList<Track>(), homeFeedTracks(emptyList(), emptyList(), null))
    }
    @Test
    fun `closing now playing returns to the previous screen with a safe home fallback`() {
        assertEquals(Tab.Library, playerReturnTab(Tab.Library))
        assertEquals(Tab.Home, playerReturnTab(Tab.Player))
    }

    @Test
    fun `player and child screens keep separate back destinations`() {
        assertEquals(Tab.Search, backDestination(Tab.Player, Tab.Home, Tab.Search))
        assertEquals(Tab.Home, backDestination(Tab.Search, Tab.Home, Tab.Search))
        assertEquals(Tab.Library, backDestination(Tab.Settings, Tab.Library, Tab.Search))
    }

    @Test
    fun `selected online track is added to the queue instead of falling back to demo`() {
        val local = Track("First Light", "Local artist", "Signals", "content://music/first-light")
        val online = Track("Ocean Eyes", "Billie Eilish", "YouTube Music", "https://audio.example/ocean")

        assertEquals(online, playbackQueue(listOf(local), online).first())
    }

    @Test
    fun `playback only accepts secure remote or local media uris`() {
        assertEquals(true, isAllowedMediaUri("https://audio.example/song"))
        assertEquals(true, isAllowedMediaUri("content://media/song"))
        assertEquals(false, isAllowedMediaUri("http://audio.example/song"))
        assertEquals(false, isAllowedMediaUri("javascript:alert(1)"))
        assertEquals(false, isAllowedMediaUri(""))
    }

    @Test
    fun `recent uri codec round trips reserved characters`() {
        val uris = listOf("content://music/one|two", "content://music/line\nbreak")

        assertEquals(uris, RecentUriCodec.decode(RecentUriCodec.encode(uris)))
    }

    @Test
    fun `record recent uri keeps newest unique items first and limits history`() {
        val history = (1..22).fold(emptyList<String>()) { current, number ->
            current.recordRecentUri("uri-$number")
        }.recordRecentUri("uri-10")

        assertEquals("uri-10", history.first())
        assertEquals(20, history.size)
        assertEquals(1, history.count { it == "uri-10" })
    }
}
