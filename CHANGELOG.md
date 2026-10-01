# Changelog

## [0.9.0-beta] - Unreleased

### Added

- Authenticated Last.fm sessions through a server-side signer, Now Playing submission,
  automatic rule-based scrobbling, and per-track exclusions.
- Online single-track, album, and playlist downloads with cancellation, retry, storage
  cleanup, codec/quality preferences, and real lossless/Hi-Res source reporting.
- Native 15-band equalizer, configurable crossfade, USB bit-perfect path checks,
  and optional clarity processing using Android audio effects.
- KuGou KRC, LRCLIB, and Lyrics.ovh lyrics with word/line synchronization,
  manual matching, editing, timing offsets, and LRC export.
- Personalized YouTube Music shelves, Last.fm/YouTube discovery blending,
  playable song/artist radio, and functional generated queues.
- In-app GitHub update download with checksum, package, version, and signer verification
  before handing the APK to Android's package installer.

### Security

- Keeps the Last.fm shared secret server-side and validates online download hosts,
  redirects, media types, and file-size limits before writing to phone storage.
- Restricts update sharing to a cache-only FileProvider and validates every update redirect.

## [0.8.0-demo] - 2026-09-25

### Changed

- Rebuilt the interface around the supplied Liquid design system, including its
  dark tokens, typography, floating navigation, and immersive Now Playing view.
- Replaced the default launcher icon with the supplied glowing V artwork.
- Removed the bundled demo track and the in-app startup screen.

### Security

- Disabled cleartext network traffic and app-data backup.
- Rejected unsupported playback URI schemes.
- Added optional private release-keystore configuration while keeping secrets
  out of source control.

## [0.7.0-demo] - 2026-09-25

### Added

- Experimental public YouTube Music search with InnerTube and a bounded
  NewPipeExtractor fallback.
- On-demand public audio-stream resolution for supported tracks.
- Automatic search after a short typing pause.
- A persistent setting to prefer the highest available audio bitrate.

### Fixed

- Online selections now play the resolved track instead of the bundled demo.
- Search results now show artist, album, duration, and high-resolution artwork.

### Known limitations

- Protected, restricted, unavailable, or bot-challenged tracks may not play.
- This experimental build is not intended for Google Play distribution.

## [0.6.0-demo] - 2026-09-23

### Added

- Selectable launcher icons and expanded local-library metadata browsing.
