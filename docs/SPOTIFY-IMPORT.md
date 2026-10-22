# Spotify playlist import

Settings → Library & Backup → **Import from Spotify**.

Paste a full public `https://open.spotify.com/playlist/...` link, a locale/embed variant, `spotify:playlist:...`, or a 22-character playlist ID. Wait for all playlist pages and matching, review the full-list/unavailable/matched/unmatched counts, deselect unwanted results, choose a local playlist name, and confirm Import. Nothing is saved before confirmation. Cancelling discards the preview. Re-import a previously truncated playlist link to retrieve its missing entries; existing saved playlists are not automatically changed.

This is a one-way local metadata import. Spotify audio, passwords, browser cookies and user-account tokens are not imported. The public web client issues anonymous guest tokens and session cookies, held only in memory during the fetch. Public titles/artists/durations are matched against VibeArc's existing YouTube Music catalog. Playback and download use the matched YouTube source, not Spotify audio. No developer key or Spotify login is required; no changes sync back to Spotify.

Matching considers title, artist and duration and rejects unrequested covers, live versions, remixes and similar alternates. It is not guaranteed exact; each proposed recording is shown for review. Unmatched songs are listed and skipped. Original playlist order and duplicate positions are preserved; duplicate metadata requests are avoided. Existing local playlists and favorites are retained. Library tracks and the new playlist are saved in one preferences edit.

## Coverage and limits

- Public playlist metadata only; private playlists and Liked Songs require a separate authenticated integration and are not supported here.
- Version 1.10.5 replaces the 100-song embed preview with full public playlist pagination. The collector checks the requested playlist ID, source total and page offsets, and reports unreadable entries separately. A failed, repeated or truncated page fails visibly rather than silently importing a partial list.
- Short `spotify.link` links are not followed; copy the full playlist link from Spotify instead.
- No arbitrary URL fetching or redirects. Fixed HTTPS endpoint allowlists, bounded responses, connection/read timeouts and pagination safety limits apply (1-10,000 entries, at most 200 pages). Matching uses at most three concurrent searches. Closing the sheet cancels pending work; a blocked HTTP read may finish at its timeout.
- Besides Spotify endpoints, the client reads public data-only configuration from code.thetadev.de/ThetaDev/spotify-secrets. No playlist ID or session token is sent to that host. CDN JavaScript is inspected as text for public query identifiers, never executed by the importer. See [privacy details](../PRIVACY.md) and the APK's license notices.
- A provider layout change, regional block, rate limit, missing recording or network failure can prevent import. Nothing is silently saved on a read failure.

## Reference investigation

Earlier VibeArc releases used a public embed fallback that could stop at 100 songs. The full importer uses a native Kotlin public-metadata adaptation referencing [Aran404/SpotAPI](https://github.com/Aran404/SpotAPI), with GPL-3.0 notices and corresponding source included. No account-token flow or Spotify audio streaming is added. This unofficial integration can break when upstream formats or restrictions change.

- [LastWave Spotify importer](https://github.com/Clash-Projects/LastWave-native/blob/main/app/src/main/java/com/lastwave/app/data/playlist/SpotifyPlaylistImporter.kt)
- [LastWave import manager](https://github.com/Clash-Projects/LastWave-native/blob/main/app/src/main/java/com/lastwave/app/data/playlist/PlaylistImportManager.kt)
- [Spotify API migration constraints](https://developer.spotify.com/documentation/web-api/tutorials/february-2026-migration-guide)

Tests cover accepted/rejected links, scoped public parsing, repeated songs, escaped JSON, bad/oversized pages, conservative recording matching and non-destructive library merges. UI uses existing glass controls and theme colors, inline validation, accessible progress/errors, a virtualized scrollable review and 48 dp action targets. Physical phone visual/interaction checks remain separate from host compilation and unit tests.

## Verification

- Version 1.10.5: 194 tests passed; lint reports zero errors and 84 warnings.
- Regression cases include 317 and 341 entries, short pages, unavailable rows, repeated positions, changed totals and incomplete/repeated pages.
- The user confirmed a complete 317-song import in the separate test APK before promotion. The newly combined main APK has not been tested on a phone here.
- Canonical tracked main/test source hashes match the signed build checkout; package, signature and APK alignment checks passed.
