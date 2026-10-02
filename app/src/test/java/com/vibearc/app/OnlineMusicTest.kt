package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class OnlineMusicTest {
    @Test fun `initial YouTube audio requests are ranged and explicit seek ranges are preserved`() {
        val song = "https://music.youtube.com/watch?v=T6eK-2OQtew"
        assertEquals(mapOf("Range" to "bytes=0-"), playbackRequestHeaders(song, emptyMap()))
        val seek = mapOf("range" to "bytes=65536-131071")
        assertEquals(seek, playbackRequestHeaders(song, seek))
        assertEquals(emptyMap<String, String>(), playbackRequestHeaders("content://music/1", emptyMap()))
        assertEquals(true, shouldRefreshAudioSource(song, 403, false))
        assertEquals(false, shouldRefreshAudioSource(song, 403, true))
        assertEquals(false, shouldRefreshAudioSource(song, 404, false))
        assertEquals(false, shouldRefreshAudioSource("https://audio.example/song", 403, false))
    }
    @Test fun `playback falls back when preferred codec is unavailable but downloads stay strict`() {
        val sources = listOf(AudioCandidate("https://audio.example/opus", 160, AudioFormat.OPUS))
        assertEquals(sources.single(), selectPlaybackAudioCandidate(sources, AudioFormat.FLAC, AudioQuality.HIGHEST))
        assertNull(selectAudioCandidate(sources, AudioFormat.FLAC))
    }

    @Test fun `simultaneous prefetch and playback extract the source only once and failed lookups retry`() {
        val cache = ResolvedAudioCache()
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val started = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val workers = java.util.concurrent.Executors.newFixedThreadPool(2)
        val track = Track("Song", "Artist", "Album", "https://audio.example/song")
        try {
            val first = workers.submit<Track> { cache.get("song") {
                calls.incrementAndGet(); started.countDown()
                check(release.await(5, java.util.concurrent.TimeUnit.SECONDS)); track
            } }
            check(started.await(5, java.util.concurrent.TimeUnit.SECONDS))
            val second = workers.submit<Track> { cache.get("song") { calls.incrementAndGet(); track } }
            release.countDown()
            assertEquals(track, first.get(5, java.util.concurrent.TimeUnit.SECONDS))
            assertEquals(track, second.get(5, java.util.concurrent.TimeUnit.SECONDS))
            assertEquals(1, calls.get())
            cache.invalidate("song")
            assertThrows(IllegalStateException::class.java) { cache.get("song") { error("failed") } }
            assertEquals(track, cache.get("song") { track })
        } finally { release.countDown(); workers.shutdownNow() }
    }
    @Test
    fun `blank searches are rejected before a network request`() {
        assertThrows(IllegalArgumentException::class.java) { OnlineMusic.search("   ") }
    }

    @Test
    fun `highest bitrate playable audio is selected`() {
        val candidates = listOf(
            AudioCandidate("", 320),
            AudioCandidate("https://audio.example/low", 64),
            AudioCandidate("https://audio.example/balanced", 160),
            AudioCandidate("https://audio.example/high", 256),
        )

        assertEquals("https://audio.example/high", selectAudioUrl(candidates))
        assertEquals("https://audio.example/balanced", selectAudioUrl(candidates, preferHighestQuality = false))
        assertNull(selectAudioUrl(listOf(AudioCandidate("", 128))))
    }

    @Test
    fun `format and quality preferences select a real matching source`() {
        val candidates = listOf(
            AudioCandidate("https://audio.example/aac-128", 128, AudioFormat.AAC, sampleRate = 44_100),
            AudioCandidate("https://audio.example/opus-96", 96, AudioFormat.OPUS, sampleRate = 48_000),
            AudioCandidate("https://audio.example/opus-160", 160, AudioFormat.OPUS, sampleRate = 48_000),
            AudioCandidate("https://audio.example/flac", 1_200, AudioFormat.FLAC, sampleRate = 96_000),
        )

        assertEquals(
            "https://audio.example/opus-96",
            selectAudioCandidate(candidates, AudioFormat.OPUS, AudioQuality.DATA_SAVER)?.url,
        )
        assertEquals(
            "https://audio.example/opus-160",
            selectAudioCandidate(candidates, AudioFormat.OPUS, AudioQuality.HIGH)?.url,
        )
        assertNull(selectAudioCandidate(candidates, AudioFormat.MP3, AudioQuality.HIGHEST))
        assertEquals(true, candidates.last().isLossless)
        assertEquals(true, candidates.last().isHiRes)
    }

    @Test
    fun `google artwork is requested at high resolution`() {
        assertEquals(
            "https://lh3.googleusercontent.com/cover=w1024-h1024-l90-rj",
            highResolutionArtworkUrl("https://lh3.googleusercontent.com/cover=w120-h120-l90-rj"),
        )
    }

    @Test
    fun `visible search results prefetch but catalog tracks remain tappable immediately`() {
        val results = (1..20).map { number ->
            Track("Track $number", "Artist", "YouTube Music", "https://music.youtube.com/watch?v=$number")
        }
        val visible = visibleOnlineTracks(
            results,
            setOf("online:${results[1].uri}", "online:${results[2].uri}"),
        )
        val resolved = results[1].copy(uri = "https://audio.example/track-2")

        assertEquals(listOf(results[1], results[2]), visible)
        assertEquals(results[1], playableOnlineTrack(results[1], emptyMap()))
        assertEquals(resolved, playableOnlineTrack(results[1], mapOf(results[1].uri to resolved)))
    }

    @Test
    fun `innerTube song response maps to a playable catalog track`() {
        val response = """
            {
              "contents": [{
                "musicResponsiveListItemRenderer": {
                  "playlistItemData": {"videoId": "abc123"},
                  "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [
                    {"url": "https://lh3.googleusercontent.com/cover=w60-h60"},
                    {"url": "https://lh3.googleusercontent.com/cover=w120-h120"}
                  ]}}},
                  "flexColumns": [
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [{"text": "Ocean Eyes"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                      {"text": "Billie Eilish", "navigationEndpoint": {"browseEndpoint": {
                        "browseId": "UCartist",
                        "browseEndpointContextSupportedConfigs": {"browseEndpointContextMusicConfig": {
                          "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                        }}
                      }}}
                    ]}}},
                    {"musicResponsiveListItemFlexColumnRenderer": {"text": {"runs": [
                      {"text": "Hit Me Hard and Soft", "navigationEndpoint": {"browseEndpoint": {
                        "browseId": "MPREalbum",
                        "browseEndpointContextSupportedConfigs": {"browseEndpointContextMusicConfig": {
                          "pageType": "MUSIC_PAGE_TYPE_ALBUM"
                        }}
                      }}}
                    ]}}}
                  ],
                  "fixedColumns": [
                    {"musicResponsiveListItemFixedColumnRenderer": {"text": {"runs": [{"text": "3:20"}]}}}
                  ]
                }
              }]
            }
        """.trimIndent()

        assertEquals(
            listOf(
                Track(
                    title = "Ocean Eyes",
                    artist = "Billie Eilish",
                    album = "Hit Me Hard and Soft",
                    uri = "https://music.youtube.com/watch?v=abc123",
                    durationMs = 200_000,
                    artworkUri = "https://lh3.googleusercontent.com/cover=w1024-h1024-l90-rj",
                    folder = "YouTube Music",
                ),
            ),
            parseInnertubeSearch(response),
        )
    }
}
