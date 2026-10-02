package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastFmTest {
    @Test
    fun `public profile responses become a listening snapshot`() {
        val info = """{"user":{"name":"Smith_2","realname":"Smith","playcount":"1200","artist_count":"42","album_count":"31","track_count":"280","image":[{"#text":"http://img/small","size":"small"},{"#text":"https://img/large","size":"large"}]}}"""
        val top = """{"toptracks":{"track":[{"name":"Loser","playcount":"19","artist":{"name":"Tame Impala"},"image":[{"#text":"https://img/loser","size":"large"}]}]}}"""
        val recent = """{"recenttracks":{"track":[{"name":"Borderline","artist":{"#text":"Tame Impala"},"album":{"#text":"The Slow Rush"},"@attr":{"nowplaying":"true"},"image":[]}]}}"""

        val snapshot = parseLastFmSnapshot(info, top, recent)

        assertEquals("Smith_2", snapshot.profile.username)
        assertEquals(1_200L, snapshot.profile.playCount)
        assertEquals(42L, snapshot.profile.artistCount)
        assertEquals("https://img/large", snapshot.profile.artworkUrl)
        assertEquals(LastFmTrack("Loser", "Tame Impala", "", 19, "https://img/loser"), snapshot.topTracks.single())
        assertEquals(true, snapshot.recentTracks.single().nowPlaying)
    }

    @Test
    fun `last fm username validation rejects blank and control characters`() {
        assertEquals("Smith_2", validLastFmUsername("  Smith_2  "))
        assertNull(validLastFmUsername(""))
        assertNull(validLastFmUsername("bad\nname"))
        assertNull(validLastFmUsername("x".repeat(65)))
    }

    @Test
    fun `similar track responses become playable search suggestions`() {
        val json = """{"similartracks":{"track":[{"name":"Let It Happen","artist":{"name":"Tame Impala"},"match":"0.91","image":[{"#text":"http://img/let-it-happen","size":"large"}]},{"name":"","artist":{"name":"Unknown"}}]}}"""

        assertEquals(
            listOf(LastFmTrack("Let It Happen", "Tame Impala", artworkUrl = "https://img/let-it-happen")),
            parseLastFmSimilarTracks(json),
        )
    }

    @Test
    fun `official scrobble threshold rejects short and excluded tracks`() {
        val track = Track("Loser", "Tame Impala", "Currents", "content://song", durationMs = 300_000)

        assertEquals(150_000L, lastFmScrobbleThresholdMs(track.durationMs))
        assertEquals(true, shouldScrobble(track, 150_000, emptySet()))
        assertEquals(false, shouldScrobble(track, 149_999, emptySet()))
        assertEquals(false, shouldScrobble(track, 300_000, setOf(track.uri)))
        assertEquals(false, shouldScrobble(track, 300_000, emptySet(), enabled = false))
        assertNull(lastFmScrobbleThresholdMs(30_000))
        assertEquals(240_000L, lastFmScrobbleThresholdMs(900_000))
    }

    @Test
    fun `profile setup is distinct from authenticated setup and rejects unsafe endpoints`() {
        val key = "a".repeat(32)
        assertEquals(true, LastFmConfiguration(key).profileConfigured)
        assertEquals(false, LastFmConfiguration(key).signerConfigured)
        assertNull(lastFmConfigurationError(LastFmConfiguration(key, "https://signer.example", "client-token")))
        for (url in listOf("http://signer.example", "https://user:pass@signer.example", "https://signer.example?token=secret", "https://signer.example/#fragment")) {
            assertEquals(false, LastFmConfiguration(key, url, "client-token").signerConfigured)
        }
        assertEquals(false, LastFmConfiguration(key, "https://signer.example", "bad\r\ntoken").signerConfigured)
        assertEquals(false, validLastFmAuthorizationUrl("https://evil.example/api/auth/"))
        assertEquals(false, validLastFmAuthorizationUrl("https://www.last.fm.evil.example/api/auth/"))
        assertEquals(true, validLastFmAuthorizationUrl("https://www.last.fm/api/auth/?token=test"))
    }
}
