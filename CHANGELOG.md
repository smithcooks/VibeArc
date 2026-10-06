# Changelog

## [1.3.0] - 2026-10-06

### Changed

- Main-app downloads now use the playback downloader confirmed working in the separate test APK; the failing native extension engine is no longer shipped.
- Rebuilt Downloads with Songs/Artists/Albums, search, sorting, actual codec and lyrics filters, storage details, folder export, and Play/Shuffle controls.
- Wi-Fi and mobile data are allowed by default. Settings → Audio & Streaming → Download on Wi-Fi only provides an optional restriction.
- Existing main-app downloads and preferences remain intact; files in the separate test package are not automatically transferred.
- Online recording labels omit play-count decorations; lyrics fixes and cached LRC sidecars are included.

### Verification and limitations

- All 118 Android JVM tests passed; release lint and packaging succeeded. Existing lint warnings remain.
- APK signature and 16 KiB alignment verified; version code 17 uses the existing release certificate.
- Physical-device layout, folder export, and mobile-data transfer still need installation testing. Provider availability varies; no universal download or lossless guarantee is made.
- Android downloads on the website and README now point directly to this release. Windows and Linux remain coming soon.

## [1.0.0] - 2026-10-02

### Changed

- Removed the beta version label; version code 12 updates both v0.9 and the
  v1.0.0-beta testing APK without changing the signing certificate.
- GitHub update checks and support links now use smithcooks/VibeArc.
- Refreshed the repository README with current downloads, feature status,
  integration setup, and build instructions.

### Fixed

- Lyrics try LRCLIB first with cleaned YouTube metadata; timeouts, malformed or
  empty exact responses fall back to search, KuGou KRC, and Lyrics.ovh.
- Empty lyrics results no longer hide useful matches; provider calls no longer
  hold a global cache lock during network requests. KRC inflation is size-limited.
- Last.fm setup can be entered in Settings without rebuilding. Public profile
  setup is distinct from authenticated sign-in, with HTTPS endpoint validation.
- Now Playing waits for actual playback; disabled/excluded tracks cannot scrobble.
  A scrobble is marked complete only after acknowledgement, with bounded retries.
- The signer uses GET for read/auth methods and POST for writes, rejects unequal
  UTF-8 bearer tokens safely, and reports ignored submissions instead of success.

### Verification and limitations

- All 94 Android JVM tests and three signer regression tests passed. A deliberate
  signer-condition mutation was caught by the tests; the current-owner updater
  regression test also failed before the repository URL fix.
- LRCLIB returned synchronized lyrics for Kendrick Lamar's "Not Like Us" in
  a live metadata-only check. No authenticated Last.fm account was tested.
- Full Gradle testing, release lint, and APK packaging succeeded in an isolated
  build directory outside Documents, avoiding the sandbox's Java ZIP filesystem
  path error. Lint reported zero errors, 46 warnings, and one hint.
- The v1.0.0 APK uses version code 12 and the existing v0.9 signing
  certificate; APK signature and alignment checks passed. This signed sideload
  release is not Google Play or Play Protect certification.
- Live authenticated Last.fm requires your API application and deployed signer;
  credentials are not bundled. An APK alone cannot configure the external server.
- Lyrics availability and word timestamps depend on provider matches; every-song
  lyrics and universal word timing are not guaranteed.

## [0.9.1-beta] - 2026-10-02

### Fixed

- Streaming falls back to an available codec when a preferred codec is absent;
  download-format requirements remain strict and the player reports the actual codec.
- Visible Home songs pre-resolve with at most two speculative requests at once;
  taps join in-progress resolution instead of duplicating extraction.
- Initial YouTube audio requests use an open-ended byte range to avoid slow full responses.
- Rejected or expired stream URLs get one fresh-source attempt, followed by an error
  identifying the HTTP/provider/network failure rather than blaming the connection.

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

### Fixed

- Resolves YouTube catalog links in the shared playback service for Home, playlists,
  radio, and queue transitions; search selections no longer wait for prefetch.
- Restores cached YouTube Home shelves and account playlists before refreshing online,
  retains them on connection failures, and reuses downloaded artwork across restarts.
- Opens YouTube Music sign-in in an in-app sheet without waiting for account loading.
- Adds Home song-card long-press actions using the existing queue, playlist, like,
  radio, and download menu; avoids duplicate queue entries after stream prefetch.

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
