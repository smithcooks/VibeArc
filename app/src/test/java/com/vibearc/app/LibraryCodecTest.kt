package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class LibraryCodecTest {
    @Test
    fun `round trip preserves track metadata and favorite state`() {
        val tracks = listOf(
            Track(
                "Rain | Sun",
                "Artist\nName",
                "Album",
                "content://music/1",
                true,
                durationMs = 183_000,
                artworkUri = "content://artwork/1",
                folder = "Night drives",
            ),
            Track("Night Drive", "VibeArc", "Singles", "content://music/2", false),
        )

        assertEquals(tracks, LibraryCodec.decode(LibraryCodec.encode(tracks)))
    }

    @Test
    fun `decode preserves old five field library rows`() {
        val oldTrack = Track("Old song", "Artist", "Album", "content://music/old", true)
        val oldRow = "T2xkIHNvbmc|QXJ0aXN0|QWxidW0|Y29udGVudDovL211c2ljL29sZA|1"

        assertEquals(listOf(oldTrack), LibraryCodec.decode(oldRow))
    }

    @Test
    fun `decode preserves seven field library rows`() {
        val oldTrack = Track(
            "Old song",
            "Artist",
            "Album",
            "content://music/old",
            true,
            durationMs = 183_000,
            artworkUri = "content://artwork/old",
        )
        val oldRow = "T2xkIHNvbmc|QXJ0aXN0|QWxidW0|Y29udGVudDovL211c2ljL29sZA|1|183000|Y29udGVudDovL2FydHdvcmsvb2xk"

        assertEquals(listOf(oldTrack), LibraryCodec.decode(oldRow))
    }

    @Test
    fun `folder name uses the parent segment of a document path`() {
        assertEquals("Albums", displayFolderFromPath("/document/primary:Music/Albums/song.mp3"))
        assertEquals("Imported", displayFolderFromPath("/document/42"))
        assertEquals("Imported", displayFolderFromPath(null))
    }

    @Test
    fun `decode ignores corrupted rows`() {
        assertEquals(emptyList<Track>(), LibraryCodec.decode("not-a-library-row"))
    }

    @Test
    fun `upsert replaces the same uri without losing favorite state`() {
        val saved = Track("Old title", "Artist", "Album", "content://music/1", true)
        val imported = Track("New title", "Artist", "Album", "content://music/1")

        assertEquals(listOf(imported.copy(isFavorite = true)), listOf(saved).upsert(imported))
    }

    @Test
    fun `toggle favorite changes only the matching uri`() {
        val first = Track("First", "Artist", "Album", "content://music/1")
        val second = Track("Second", "Artist", "Album", "content://music/2")

        assertEquals(listOf(first.copy(isFavorite = true), second), listOf(first, second).toggleFavorite(first.uri))
    }

    @Test
    fun `liking a catalog track adds it to the library`() {
        val catalogTrack = Track("Online", "Artist", "Album", "https://music.youtube.com/watch?v=track")

        assertEquals(listOf(catalogTrack.copy(isFavorite = true)), emptyList<Track>().toggleFavorite(catalogTrack))
    }

    @Test
    fun `playlist round trip preserves names and track uri references`() {
        val playlists = listOf(
            Playlist("road-trip", "Road | Trip\n2026", listOf("content://music/1", "content://music/2")),
            Playlist("quiet", "Quiet", emptyList()),
        )

        assertEquals(playlists, PlaylistCodec.decode(PlaylistCodec.encode(playlists)))
    }

    @Test
    fun `playlist operations create rename and delete playlists`() {
        val created = emptyList<Playlist>().createPlaylist("  Road Trip  ", id = "road-trip")
        val renamed = created.renamePlaylist("road-trip", "  Night Drive  ")

        assertEquals(listOf(Playlist("road-trip", "Night Drive")), renamed)
        assertEquals(emptyList<Playlist>(), renamed.deletePlaylist("road-trip"))
    }

    @Test
    fun `playlist track operations add once and remove by uri`() {
        val playlists = listOf(Playlist("mix", "Mix"))
        val withTrack = playlists
            .addTrackToPlaylist("mix", "content://music/1")
            .addTrackToPlaylist("mix", "content://music/1")

        assertEquals(listOf("content://music/1"), withTrack.single().trackUris)
        assertEquals(emptyList<String>(), withTrack.removeTrackFromPlaylist("mix", "content://music/1").single().trackUris)
    }

    @Test
    fun `backup JSON round trip preserves library and playlists`() {
        val tracks = listOf(Track("Night Drive", "VibeArc", "Singles", "content://music/1", true))
        val playlists = listOf(Playlist("mix", "My Mix", listOf("content://music/1")))

        assertEquals(
            VibeArcBackup(tracks, playlists),
            BackupCodec.decode(BackupCodec.encode(tracks, playlists)),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `backup rejects unsupported schema`() {
        BackupCodec.decode("""{"schema":99,"tracks":"","playlists":""}""")
    }

    @Test
    fun `restore merge keeps local data and updates matching playlists`() {
        val localTrack = Track("Local", "Artist", "Album", "content://music/local")
        val restoredTrack = Track("Restored", "Artist", "Album", "content://music/restored")
        val localPlaylists = listOf(Playlist("mix", "Old name"), Playlist("local", "Local only"))
        val backup = VibeArcBackup(
            tracks = listOf(restoredTrack),
            playlists = listOf(Playlist("mix", "Restored name", listOf(restoredTrack.uri))),
        )

        assertEquals(
            VibeArcBackup(
                tracks = listOf(localTrack, restoredTrack),
                playlists = listOf(
                    Playlist("mix", "Restored name", listOf(restoredTrack.uri)),
                    Playlist("local", "Local only"),
                ),
            ),
            mergeBackup(listOf(localTrack), localPlaylists, backup),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `backup reader rejects files over its limit`() {
        ByteArrayInputStream("12345".toByteArray()).readUtf8Limited(4)
    }

    @Test
    fun `M3U import matches library URIs and file names`() {
        val tracks = listOf(
            Track("First Song", "Artist", "Album", "content://music/first"),
            Track("Second Song", "Artist", "Album", "content://music/second"),
        )
        val contents = """
            #EXTM3U
            #EXTINF:180,First Song
            content://music/first
            /storage/emulated/0/Music/Second Song.mp3
            /storage/emulated/0/Music/Missing Song.mp3
        """.trimIndent()

        assertEquals(
            Playlist("imported", "Road Trip", listOf("content://music/first", "content://music/second")),
            parsePlaylistFile("Road Trip.m3u", contents, tracks, "imported"),
        )
    }

    @Test
    fun `CSV import skips headers and unknown tracks`() {
        val track = Track("First Song", "Artist", "Album", "content://music/first")
        val contents = "title,artist,uri\n\"First Song\",\"Artist\",\"content://music/first\"\nUnknown,Artist,missing"

        assertEquals(
            Playlist("csv", "Saved", listOf(track.uri)),
            parsePlaylistFile("Saved.csv", contents, listOf(track), "csv"),
        )
    }
}
