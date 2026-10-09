package com.vibearc.app

internal const val AutomaticQueueSize = 30

internal fun automaticQueueAdditions(queued: List<Track>, candidates: List<Track>): List<Track> {
    val existing = queued.mapTo(hashSetOf(), Track::catalogUri)
    return candidates.asSequence().filter { isAllowedMediaUri(it.uri) && it.catalogUri !in existing }
        .distinctBy(Track::catalogUri).take((AutomaticQueueSize - queued.size).coerceAtLeast(0))
        .map(Track::withoutPlayCounts).toList()
}
