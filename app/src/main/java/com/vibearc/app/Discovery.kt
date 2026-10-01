package com.vibearc.app

internal enum class GeneratorChoice(val title: String, val subtitle: String, val icon: String) {
    TopTracks("Top Tracks", "Favorites and recent listening", "stats"),
    RecentTracks("Recent Tracks", "What you played lately", "clock"),
    SongRadio("Song Radio", "Related tracks from your current song", "music"),
    SimilarArtists("Similar Artists", "Artists related to your listening", "account"),
    Genre("By Tag / Genre", "Recommendations across genres and moods", "discover"),
    MyMix("My Mix", "A blend of history, favorites, and discovery", "shuffle"),
    Recommendations("My Recommendation", "YouTube Music and Last.fm picks", "spark"),
    Library("My Library", "Rediscover music you saved", "playlist"),
}

internal fun blendDiscoveryTracks(
    youtube: List<Track>,
    lastFm: List<Track>,
    recent: List<Track>,
    library: List<Track>,
): List<Track> = distinctTracks(youtube + lastFm + recent + library)

internal fun generatedPlaylist(
    choice: GeneratorChoice,
    library: List<Track>,
    recent: List<Track>,
    recommendations: List<Track>,
    radio: List<Track>,
    seed: Track?,
): List<Track> = distinctTracks(when (choice) {
    GeneratorChoice.TopTracks -> library.filter(Track::isFavorite) + recent + recommendations + library
    GeneratorChoice.RecentTracks -> recent
    GeneratorChoice.SongRadio -> radio
    GeneratorChoice.SimilarArtists -> recommendations.filterNot { seed != null && it.artist.equals(seed.artist, true) }
    GeneratorChoice.Genre -> recommendations
    GeneratorChoice.MyMix -> recent + library.filter(Track::isFavorite) + recommendations + library
    GeneratorChoice.Recommendations -> recommendations
    GeneratorChoice.Library -> library
}).take(60)

private fun distinctTracks(tracks: List<Track>): List<Track> = tracks
    .filter { it.uri.isNotBlank() }
    .distinctBy(Track::catalogUri)
    .take(60)
