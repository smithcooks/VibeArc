# VibeArc

Native Android music-player demo built with Kotlin, Jetpack Compose, Material 3, and Media3.

## Run

Open this folder in Android Studio and run the `app` configuration, or use:

```powershell
.\gradlew.bat assembleDebug
```

The local player works without network access or accounts. v0.8 also includes
experimental, account-free search and playback for supported public YouTube
Music results; that feature requires a network connection and is not guaranteed
for protected, restricted, or unavailable tracks.

## Current scope

- Liquid dark interface with artwork-driven Now Playing color and floating glass navigation
- Home, Search, Library, Downloads, Now Playing, and Settings destinations
- Playback of user-selected audio files with no bundled demo audio
- Background playback, media notification, lock-screen controls, seek, and mini-player
- Persistent multi-track library, search, and favorites
- Native title, artist, album, duration, and embedded artwork metadata
- Artist, album, and source-folder library browsing
- Persistent playlists with create, rename, delete, add, and remove actions
- Playback queue with previous/next, shuffle, repeat, and recently played history
- Background-safe sleep timer controls
- Midnight, Peach, and Mono launcher-icon choices
- Direct InnerTube search with a bounded NewPipe search fallback
- On-demand public-stream resolution through NewPipeExtractor
- Automatic YouTube Music search after a short typing pause
- High-resolution online artwork and a persistent highest-quality audio option
- Loading, empty, unavailable, and offline-friendly search states

Current milestone: v0.8.0-demo is an experimental local build and is not
considered a Google Play-safe production integration. See
[`tasks/plan.md`](tasks/plan.md) for the remaining limits.

## License

VibeArc is free software licensed under the GNU General Public License version
3 or any later version. See [`LICENSE`](LICENSE) and
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

Copyright © 2026 VibeArc contributors.
