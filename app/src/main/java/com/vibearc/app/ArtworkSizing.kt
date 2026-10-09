package com.vibearc.app

import java.io.InputStream
import java.io.ByteArrayOutputStream

private val GoogleArtworkDimensions = Regex("=w\\d+-h\\d+")

internal fun nearbyArtworkUris(current: String?, next: String?, previous: String?): List<String> =
    listOfNotNull(next, previous).filter { it.isNotBlank() && it != current }.distinct()

internal fun artworkUrlForTarget(url: String, target: Int): String =
    if ("googleusercontent.com" in url) {
        url.replace(GoogleArtworkDimensions, "=w$target-h$target")
    } else url

internal fun artworkSampleSize(width: Int, height: Int, target: Int): Int {
    require(width > 0 && height > 0 && target > 0)
    var sample = 1
    while (maxOf(width, height) / (sample * 2) >= target) sample *= 2
    return sample
}

internal fun readBounded(input: InputStream, limit: Int): ByteArray {
    require(limit > 0)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        require(output.size() <= limit - count) { "File exceeds the size limit" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
