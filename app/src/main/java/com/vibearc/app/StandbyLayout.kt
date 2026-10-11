package com.vibearc.app

internal fun lyricCenterScrollDelta(viewportStart: Int, viewportEnd: Int, lineOffset: Int, lineHeight: Int): Float =
    lineOffset + lineHeight / 2f - (viewportStart + viewportEnd) / 2f

internal fun standbyCoverSize(width: Float, height: Float, lyrics: Boolean): Float =
    minOf(width, if(lyrics) (height - 144f).coerceAtLeast(0f) else height, if(lyrics) 200f else 400f).coerceAtLeast(0f)
