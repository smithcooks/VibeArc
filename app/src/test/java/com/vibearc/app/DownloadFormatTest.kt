package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class DownloadFormatTest {
    private val track=Track("Song","Artist","Album","https://www.youtube.com/watch?v=abcdefghijk")

    @Test fun `old MP3 preference is not automatically opted in after upgrade`() {
        val preferences=mutableMapOf("download_format" to "MP3")
        assertEquals(DownloadFormat.WEBM,readDownloadFormatPreference(preferences::get))
        assertEquals(DownloadFormat.WEBM,DownloadFormat.entries.first())
        preferences[DownloadFormatPreferenceKey]=DownloadFormat.MP3.name
        assertEquals(DownloadFormat.MP3,readDownloadFormatPreference(preferences::get))
        assertEquals(DownloadFormat.MP3,readDownloadFormatPreference(preferences::get)) // Restart keeps explicit choice.
        preferences[DownloadFormatPreferenceKey]=DownloadFormat.WEBM.name
        assertEquals(DownloadFormat.WEBM,readDownloadFormatPreference(preferences::get))
    }

    @Test fun `new and upgraded settings default to WebM unless explicitly opted into MP3`() {
        assertEquals(DownloadFormat.WEBM,DownloadFormat.fromPreference(null))
        assertEquals(DownloadFormat.WEBM,DownloadFormat.fromPreference("unknown"))
        assertEquals(DownloadFormat.MP3,DownloadFormat.fromPreference("MP3"))
        assertEquals(DownloadFormat.WEBM,DownloadEntry(track).downloadFormat)
    }

    @Test fun `WebM selects highest genuine Opus without silently falling back to AAC or Ogg`() {
        val aac=AudioCandidate("aac",256,AudioFormat.AAC,"audio/mp4","m4a")
        val ogg=AudioCandidate("ogg",320,AudioFormat.OPUS,"audio/ogg","opus")
        val webm=AudioCandidate("webm",160,AudioFormat.OPUS,"audio/webm","webm")
        assertEquals(webm,selectWebmDownload(listOf(aac,ogg,webm,webm.copy(url="low",bitrate=96))))
        assertNull(selectWebmDownload(listOf(aac,ogg)))
    }

    @Test fun `conversion state preserves original completed download and survives restart`() {
        val entry=DownloadEntry(track,DownloadStatus.COMPLETED,codec="WebM Opus",localUri="file:///original.audio",bytes=123,
            mp3CopyState=Mp3CopyState.RUNNING,mp3CopyMessage="Converting to MP3 · 50%")
        assertEquals(entry,DownloadCodec.decode(DownloadCodec.encode(listOf(entry))).single())
        assertFalse(entry.canCreateMp3Copy())
        assertTrue(entry.copy(mp3CopyState=Mp3CopyState.FAILED).canCreateMp3Copy())
        assertFalse(entry.copy(codec="MP3",mp3CopyState=Mp3CopyState.NONE).canCreateMp3Copy())
        assertFalse(entry.copy(ownsFile=false,mp3CopyState=Mp3CopyState.NONE).canCreateMp3Copy())
        assertFalse(entry.copy(status=DownloadStatus.DOWNLOADING,mp3CopyState=Mp3CopyState.NONE).canCreateMp3Copy())
    }

    @Test fun `queued format and published MP3 copy persist independently of original export`() {
        val entry=DownloadEntry(track,DownloadStatus.COMPLETED,codec="WebM Opus",exportedUri="content://media/webm/1",
            downloadFormat=DownloadFormat.WEBM,mp3CopyState=Mp3CopyState.COMPLETED,mp3CopyUri="content://media/mp3/2")
        assertEquals(entry,DownloadCodec.decode(DownloadCodec.encode(listOf(entry))).single())
        val oldJson=DownloadCodec.encode(listOf(DownloadEntry(track))).replace(",\"downloadFormat\":\"WEBM\"", "")
            .replace(",\"mp3CopyState\":\"NONE\"", "").replace(",\"mp3CopyUri\":\"\"", "").replace(",\"mp3CopyMessage\":\"\"", "")
        assertEquals(DownloadEntry(track,createdAt=0).copy(createdAt=DownloadCodec.decode(oldJson).single().createdAt),DownloadCodec.decode(oldJson).single())
    }
}
