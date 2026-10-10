# VibeArc v1.8.0 — local test candidate

Built on October 10, 2026. This build is not published. GitHub and the website remain on v1.7.0 until Smith tests and approves this candidate.

## Changes

- Player menu: Standby Mode with artwork/controls and existing animated lyrics, side-by-side in landscape and stacked in portrait. The screen stays awake only while Standby is open.
- Artwork-led player, featured search results, real artist/album/playlist navigation, release shelves, and saved-playlist cover collages.
- Settings grouped into five categories; all existing controls remain available. Seek wave stroke increased to 5 dp.
- Highest available source selected by default; genuine lossless sources rank first, then reported bitrate, with Opus preferred at equal compressed bitrate. Downloads retain original source bytes, with no conversion or invented quality labels. The player displays reported codec/bitrate.

## Verification

- Release build, release unit tests and Android lint completed successfully.
- 143 tests passed, zero failures/errors. Quality regression tests failed against the previous selector and passed with this change.
- Lint: zero errors, 49 warnings and one hint. Warnings include existing JavaScript-enabled login WebView, application-context singleton detection, storage API and Kotlin-extension suggestions; this is not a clean security-audit claim.
- Live provider probe: The Weeknd returned five songs and 20 releases; After Hours returned 14 tracks. Highest available test source: Opus, 160 kbps, 48 kHz.
- Package `com.vibearc.app`, version code 24, version name 1.8.0, minimum SDK 26, target SDK 36; no debuggable flag.
- APK v2 signature verified, same release certificate as v1.7.0. Zip alignment check passed, with embedded baseline profile present.
- APK size: 6,614,619 bytes.
- SHA-256: `54ee06bb25f7bde9bd56e1bc7981dcd7b063c88d964737532406f2247231e365`.
- Release certificate SHA-256: `b20d00765244ce4112a9b1c160aca932c686e64f54830bde35ea123e09506c0c`.

## Phone test before approval

Install over the existing signed VibeArc APK without uninstalling. Check player album swipes, queue, play/pause, downloads on Wi-Fi/mobile data, and animated lyrics. Open player menu > Standby Mode, rotate into landscape and back, then exit. Search tracks/artists/albums/playlists; open an artist, a release and its songs. Check both themes, large fonts and Settings back navigation.

No physical-device UI, frame-time, audio-output or end-to-end download test was performed for this candidate. Artist pages currently show the songs/releases supplied by the initial provider page, not the full paginated catalog. Previously saved audio is unchanged. Provider access, the original recording, Android's mixer and headphones/Bluetooth constrain quality; universal FLAC/Hi-Res/bit-perfect output is not guaranteed.
