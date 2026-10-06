package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files

class DownloadTransferTest {
    @Test fun `range resume requires matching start and validator`() {
        assertTrue(canResumeDownload(10, 206, "bytes 10-19/20", "etag", "etag"))
        assertFalse(canResumeDownload(10, 200, null, "etag", "etag"))
        assertFalse(canResumeDownload(10, 206, "bytes 0-19/20", "etag", "etag"))
        assertFalse(canResumeDownload(10, 206, "bytes 10-19/20", "old", "new"))
        assertFalse(canResumeDownload(10, 206, "bytes 10-19/20", "", ""))
        assertFalse(canResumeDownload(10,206,"bytes 10-garbage/*","etag","etag"))
        assertFalse(canResumeDownload(10,206,"bytes 10-30/20","etag","etag"))
        assertFalse(canResumeDownload(10,206,"bytes 10-19/20","W/\"etag\"","W/\"etag\""))
    }
    @Test fun `copy stops on pause and preserves partial bytes`() {
        val dir = Files.createTempDirectory("vibearc-download-test").toFile()
        val file = java.io.File(dir, "track.part")
        try {
            var progressed = false
            try {
                copyAudioBytes(ByteArrayInputStream(ByteArray(100_000)), file, false, 100_000, { progressed }, { _, _ -> progressed=true })
                fail("Must pause")
            } catch (_: DownloadPaused) {}
            assertTrue(file.length() in 1 until 100_000)
        } finally { file.delete(); dir.delete() }
    }
    @Test fun `bounded copy rejects truncated response`() {
        val dir = Files.createTempDirectory("vibearc-download-test").toFile()
        val file = java.io.File(dir, "track.part")
        try {
            try { copyAudioBytes(ByteArrayInputStream(ByteArray(10)), file, false, 20, { false }, { _, _ -> }); fail("Must fail") }
            catch (_: java.io.EOFException) {}
        } finally { file.delete(); dir.delete() }
    }
}
