package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun `semantic versions compare numerically`() {
        assertTrue(isNewerVersion("0.10.0", "0.9.9"))
        assertEquals(false, isNewerVersion("0.9.0", "0.9.0-beta"))
    }

    @Test
    fun `release response accepts only the VibeArc GitHub page`() {
        val json = """{
            "tag_name":"v0.10.0",
            "html_url":"https://github.com/Akumukage/VibeArc/releases/tag/v0.10.0",
            "assets":[
              {"name":"VibeArc-v0.10.0.apk","browser_download_url":"https://github.com/Akumukage/VibeArc/releases/download/v0.10.0/VibeArc-v0.10.0.apk"},
              {"name":"VibeArc-v0.10.0.apk.sha256","browser_download_url":"https://github.com/Akumukage/VibeArc/releases/download/v0.10.0/VibeArc-v0.10.0.apk.sha256"}
            ]
        }"""
        assertEquals(
            AppUpdate(
                "0.10.0",
                "https://github.com/Akumukage/VibeArc/releases/tag/v0.10.0",
                "https://github.com/Akumukage/VibeArc/releases/download/v0.10.0/VibeArc-v0.10.0.apk",
                "https://github.com/Akumukage/VibeArc/releases/download/v0.10.0/VibeArc-v0.10.0.apk.sha256",
            ),
            parseAppUpdate(json),
        )
        assertEquals(null, parseAppUpdate("""{"tag_name":"v9","html_url":"https://evil.example/release"}"""))
        assertEquals(null, parseAppUpdate(json.replace("github.com/Akumukage/VibeArc/releases/download", "evil.example/download")))
        assertEquals(null, parseAppUpdate(json.replace("v0.10.0/VibeArc", "v0.10.0/%2e%2e/VibeArc")))
    }

    @Test
    fun `checksum accepts one sha256 token`() {
        val hash = "a".repeat(64)
        assertEquals(hash, parseSha256("$hash  VibeArc.apk\n"))
        assertEquals(null, parseSha256("not-a-checksum"))
    }
}
