package com.vibearc.app

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.util.Base64

internal class LyricsDiskCache(context: Context) {
    private val directory = File(context.filesDir,"lyrics")
    fun read(track: Track): LyricsDocument? = runCatching {
        val file = File(directory,"${downloadIdentity(track)}.txt")
        if (!file.isFile) return null
        val fields = AtomicFile(file).openRead().use { it.readUtf8Limited(512*1024) }.split('|')
        require(fields.size==4)
        fun decode(i: Int) = String(Base64.getDecoder().decode(fields[i]),Charsets.UTF_8)
        LyricsDocument(parseLrc(decode(2)),decode(3).lines().filter(String::isNotBlank),fields[1]=="1",decode(0))
    }.getOrNull()
    fun save(track: Track, document: LyricsDocument) {
        directory.mkdirs()
        val atomic = AtomicFile(File(directory,"${downloadIdentity(track)}.txt"))
        fun encode(s: String) = Base64.getEncoder().encodeToString(s.toByteArray(Charsets.UTF_8))
        val raw = listOf(encode(document.source),if(document.instrumental) "1" else "0",encode(encodeEnhancedLrc(document.syncedLines)),encode(document.plainLines.joinToString("\n"))).joinToString("|")
        val out = atomic.startWrite()
        try { out.write(raw.toByteArray()); atomic.finishWrite(out) } catch(e: Exception) { atomic.failWrite(out); throw e }
    }
}
