package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class SpotifyPlaylistImportTest {
    private val id="0123456789abcdefghijkl"
    private val songId="abcdefghijkl0123456789"
    private val track=Track("Night Lights","Example Artist","Album","https://music.youtube.com/watch?v=example1234",durationMs=180_000)
    private fun page(rows: String,extra: String="")="""<script type="application/json" id="__NEXT_DATA__">{"props":{"pageProps":{"state":{"data":{"entity":{"type":"playlist","uri":"spotify:playlist:$id","title":"My playlist","trackList":[$rows]}}}}}$extra}</script>"""
    private fun row(title: String="Night Lights",uri: String="spotify:track:$songId")="""{"uri":"$uri","title":"$title","subtitle":"Example Artist","duration":180000}"""

    @Test fun `accepts full locale embed URI and bare playlist forms`() {
        for(input in listOf(id,"spotify:playlist:$id","https://open.spotify.com/playlist/$id?si=abc","https://open.spotify.com/intl-de/playlist/$id","https://open.spotify.com/embed/playlist/$id"))
            assertEquals(id,spotifyPlaylistId(input))
    }
    @Test fun `rejects other hosts private collections credentials ports and malformed IDs`() {
        for(input in listOf("http://open.spotify.com/playlist/$id","https://open.spotify.com.evil.test/playlist/$id","https://evil.test/?url=https://open.spotify.com/playlist/$id","https://user@open.spotify.com/playlist/$id","https://open.spotify.com:444/playlist/$id","https://open.spotify.com/collection/tracks","https://open.spotify.com/track/$id","https://spotify.link/short","spotify:playlist:abc","https://open.spotify.com/playlist/$id/more"))
            assertNull(input,spotifyPlaylistId(input))
    }
    @Test fun `reads only requested playlist rows preserving order and repeated songs`() {
        val result=parseSpotifyPlaylist(page(row()+","+row("Second")+","+row()+","+row("Podcast","spotify:episode:$songId")),id)
        assertEquals("My playlist",result.name)
        assertEquals(listOf("Night Lights","Second","Night Lights"),result.songs.map { it.title })
        assertEquals(180_000L,result.songs.first().durationMs)
    }
    @Test fun `handles escaped script JSON and excludes unrelated recommendations`() {
        val raw=page(row(),""", "recommendation":${row("Unrelated")}""")
        assertEquals(1,parseSpotifyPlaylist(raw,id).songs.size)
        assertEquals(1,parseSpotifyPlaylist(raw.replace("\"props\"","&quot;props&quot;"),id).songs.size)
    }
    @Test fun `missing malformed different playlist and oversized pages fail without import`() {
        for(html in listOf("<html>Sign in</html>",page(""),page(row()).replace("spotify:playlist:$id","spotify:playlist:$songId"),"x".repeat(2*1024*1024+1))) {
            try { parseSpotifyPlaylist(html,id);fail("Invalid page must fail") } catch(_: Exception) {}
        }
    }
    @Test fun `matching prefers correct artist duration and avoids covers and wrong recordings`() {
        val song=SpotifyImportSong(songId,"Night Lights","Example Artist",180_000)
        assertEquals(track,matchSpotifySong(song,listOf(track.copy(title="Night Lights cover"),track.copy(artist="Someone Else"),track.copy(durationMs=250_000),track)))
        assertEquals(track,matchSpotifySong(song.copy(artist="Example Artist, Featured Guest"),listOf(track)))
        assertNull(matchSpotifySong(song,listOf(track.copy(uri="https://open.spotify.com/track/$songId"),track.copy(title="Another Song"))))
    }
    @Test fun `imports preserve favorites existing playlists order and duplicate positions`() {
        val before=VibeArcBackup(listOf(track.copy(isFavorite=true)),listOf(Playlist("old","Existing",listOf(track.uri))))
        val second=track.copy(title="Another Song",uri="https://music.youtube.com/watch?v=other123456")
        val imported=mergeSpotifyImport(before," New playlist ",listOf(track,second,track),"new")
        assertEquals(before.playlists.first(),imported.playlists.first())
        assertEquals(listOf(track.uri,second.uri,track.uri),imported.playlists.last().trackUris)
        assertEquals("New playlist",imported.playlists.last().name)
        assertTrue(imported.tracks.first { it.uri==track.uri }.isFavorite)
        assertEquals(2,imported.tracks.size)
        assertNotEquals(playlistRowKey(0,track),playlistRowKey(2,track))
    }
}
