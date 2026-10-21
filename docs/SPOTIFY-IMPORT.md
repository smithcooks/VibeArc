# Spotify playlist import

Settings → Library & Backup → **Import from Spotify**.

Paste a full public `https://open.spotify.com/playlist/...` link, a locale/embed variant, `spotify:playlist:...`, or a 22-character playlist ID. Read the public playlist, wait for matching, review the exposed/matched/unmatched counts, deselect unwanted results, choose a local playlist name, and confirm Import. Nothing is saved before confirmation. Cancelling discards the preview.

This is a one-way local metadata import. Spotify audio, passwords, cookies and account tokens are not fetched. Public track titles/artists/durations are matched against VibeArc's existing YouTube Music catalog. Playback and download continue to use the matched YouTube source, not Spotify audio. It does not sync changes back to Spotify.

Matching considers title, artist and duration and rejects unrequested covers, live versions, remixes and similar alternates. It is not guaranteed exact; each proposed recording is shown for review. Unmatched songs are listed and skipped. Original playlist order and duplicate positions are preserved; duplicate metadata requests are avoided. Existing local playlists and favorites are retained. Library tracks and the new playlist are saved in one preferences edit.

## Coverage and limits

- Public playlist metadata only; private playlists and Liked Songs require a separate authenticated integration and are not supported here.
- Spotify public embeds can expose only part of a large playlist. The UI explicitly reports **exposed songs**, not the full playlist total. No completeness guarantee is made.
- Short `spotify.link` links are not followed; copy the full playlist link from Spotify instead.
- No arbitrary URL fetching or redirects. The importer builds a fixed Spotify embed URL from a validated ID, uses connection/read timeouts, limits the page to 2 MiB and exposed rows to 500, and matches at most three unique songs concurrently. Closing the sheet cancels pending work; an already-blocked HTTP read may finish at its timeout.
- A provider layout change, regional block, rate limit, missing recording or network failure can prevent import. Nothing is silently saved on a read failure.

## Reference investigation

LastWave Native uses an authenticated API path when a personal token exists, with a public embed `__NEXT_DATA__` fallback, then searches YouTube Music and saves matched tracks. VibeArc implements its own public-link flow against the observed embed entity/trackList structure; it does not copy LastWave account-token handling.

- [LastWave Spotify importer](https://github.com/Clash-Projects/LastWave-native/blob/main/app/src/main/java/com/lastwave/app/data/playlist/SpotifyPlaylistImporter.kt)
- [LastWave import manager](https://github.com/Clash-Projects/LastWave-native/blob/main/app/src/main/java/com/lastwave/app/data/playlist/PlaylistImportManager.kt)
- [Spotify API migration constraints](https://developer.spotify.com/documentation/web-api/tutorials/february-2026-migration-guide)

Tests cover accepted/rejected links, scoped public parsing, repeated songs, escaped JSON, bad/oversized pages, conservative recording matching and non-destructive library merges. UI uses existing glass controls and theme colors, inline validation, accessible progress/errors, a virtualized scrollable review and 48 dp action targets. Physical phone visual/interaction checks remain separate from host compilation and unit tests.

## Verification

- Release source compilation and `:app:testReleaseUnitTest` passed: 180 tests, zero failures, errors or skips.
- `:app:lintRelease` passed with zero errors and 89 project warnings; no lint issues point to either new Spotify importer Kotlin file.
- Public-network smoke check read 50 exposed songs from a public Spotify playlist and found a confident match for its first song among 20 catalog results. This checks metadata only, not audio playback or download.
- Canonical source and build checkout hashes match for all seven changed/new Kotlin implementation and test files.
- Phone UI/interaction testing has not been performed. APK packaging and publication are tracked separately in the release verification report.
