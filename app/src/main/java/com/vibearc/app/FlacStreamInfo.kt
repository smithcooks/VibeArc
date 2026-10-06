package com.vibearc.app

internal data class FlacFormat(val sampleRateHz: Int,val bitDepth: Int,val channels: Int)

/** RFC 9639 STREAMINFO parameters describe the file, not provenance or bit-perfect device output. */
internal object FlacStreamInfo {
    @JvmStatic fun read(bytes: ByteArray): FlacFormat? {
        if(bytes.size<42 || bytes[0]!='f'.code.toByte() || bytes[1]!='L'.code.toByte() || bytes[2]!='a'.code.toByte() || bytes[3]!='C'.code.toByte()) return null
        if((bytes[4].toInt() and 0x7f)!=0 || bytes[5].toInt()!=0 || bytes[6].toInt()!=0 || bytes[7].toInt()!=34) return null
        var packed=0L
        for(i in 18..25) packed=(packed shl 8) or (bytes[i].toLong() and 255)
        val rate=((packed ushr 44) and 0xfffff).toInt()
        val depth=(((packed ushr 36) and 31)+1).toInt()
        val channels=(((packed ushr 41) and 7)+1).toInt()
        return FlacFormat(rate,depth,channels).takeIf { rate>0 && depth in 4..32 }
    }
}
