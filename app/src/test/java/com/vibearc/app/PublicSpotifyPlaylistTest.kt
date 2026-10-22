package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class PublicSpotifyPlaylistTest {
    private val id="0123456789abcdefghijkl"
    private val trackId="abcdefghijkl0123456789"
    private val song=SpotifyImportSong("spotify:track:$trackId","A song","An artist",180_000)
    private fun page(total:Int=341,offset:Int=0,rows:String=row())="""{"data":{"playlistV2":{"uri":"spotify:playlist:$id","name":"Public list","content":{"totalCount":$total,"pagingInfo":{"offset":$offset},"items":[$rows]}}}}"""
    private fun row(uri:String="spotify:track:$trackId")="""{"itemV2":{"data":{"uri":"$uri","name":"A song","artists":{"items":[{"profile":{"name":"An artist"}}]},"trackDuration":{"totalMilliseconds":180000}}}}"""

    @Test fun `parser reads native public playlist recording fields`() {
        val result=parsePublicSpotifyPage(page(),id)
        assertEquals(341,result.total)
        assertEquals(song,result.songs.single())
        assertEquals(0,result.offset)
    }
    @Test fun `reads all 341 positions across four pages and preserves repetitions`() {
        val requests=mutableListOf<Int>();val progress=mutableListOf<Int>()
        val result=collectPublicSpotifyPlaylist(id,{done,_ ->progress+=done}) { offset ->
            requests+=offset
            val size=minOf(100,341-offset)
            PublicSpotifyPage("My list",341,size,List(size){song},offset)
        }
        assertEquals(listOf(0,100,200,300),requests)
        assertEquals(listOf(100,200,300,341),progress)
        assertEquals(341,result.songs.size)
        assertEquals(341,result.sourceTotal)
        assertEquals(0,result.unreadableEntries)
    }
    @Test fun `pagination advances by received row count not requested limit`() {
        val requests=mutableListOf<Int>()
        val result=collectPublicSpotifyPlaylist(id,{_,_->}) { offset ->
            requests+=offset
            val size=minOf(25,101-offset)
            PublicSpotifyPage("My list",101,size,List(size){song},offset)
        }
        assertEquals(listOf(0,25,50,75,100),requests)
        assertEquals(101,result.songs.size)
    }
    @Test fun `reads the user confirmed 317 song size without the old 100 song cutoff`() {
        val offsets=mutableListOf<Int>()
        val result=collectPublicSpotifyPlaylist(id,{_,_->}) { offset ->
            offsets+=offset
            val size=minOf(100,317-offset)
            PublicSpotifyPage("317 songs",317,size,List(size) { i -> song.copy(title="Song ${offset+i}") },offset)
        }
        assertEquals(listOf(0,100,200,300),offsets)
        assertEquals(317,result.sourceTotal)
        assertEquals((0 until 317).map { "Song $it" },result.songs.map { it.title })
    }
    @Test fun `unavailable rows count toward completeness but are reported separately`() {
        val parsed=parsePublicSpotifyPage(page(total=3,rows="${row()},${row("spotify:episode:$trackId")},{\"itemV2\":{\"data\":null}}"),id)
        val result=collectPublicSpotifyPlaylist(id,{_,_->}){parsed}
        assertEquals(3,result.sourceTotal)
        assertEquals(1,result.songs.size)
        assertEquals(2,result.unreadableEntries)
    }
    @Test fun `truncated changed repeated and overflowing pages fail instead of returning partial list`() {
        for(mode in 0..3) {
            try {
                collectPublicSpotifyPlaylist(id,{_,_->}) { offset ->
                    if(offset==0) PublicSpotifyPage("List",101,100,List(100){song},0)
                    else when(mode) {
                        0 -> PublicSpotifyPage("List",101,0,emptyList(),100)
                        1 -> PublicSpotifyPage("List",102,1,listOf(song),100)
                        2 -> PublicSpotifyPage("List",101,1,listOf(song),0)
                        else -> PublicSpotifyPage("List",101,2,listOf(song,song),100)
                    }
                }
                fail("Invalid page must fail")
            } catch(_:IllegalArgumentException) { }
        }
    }
    @Test fun `parser rejects errors wrong playlist and missing total`() {
        for(raw in listOf("{\"errors\":[{}]}",page().replace("spotify:playlist:$id","spotify:playlist:$trackId"),page().replace("\"totalCount\":341,",""))) {
            try { parsePublicSpotifyPage(raw,id); fail("Invalid data must fail") } catch(_:Exception) { }
        }
    }
    @Test fun `query hash extraction is scoped to playlist operation`() {
        val hash="a".repeat(64)
        assertEquals(hash,publicPlaylistQueryHash("[\"fetchPlaylist\",\"query\",\"$hash\"]"))
        assertNull(publicPlaylistQueryHash("[\"anotherOperation\",\"query\",\"$hash\"]"))
    }
    @Test fun `public session code validates untrusted configuration`() {
        assertEquals("011844",spotifyGuestCode(List(25){44},1_700_000_000_000))
        try {spotifyGuestCode(listOf(999),0);fail("Reject invalid config")} catch(_:IllegalArgumentException) { }
    }
}

