# VibeArc v1.10.4

- Import a public Spotify playlist from Settings > Library & Backup > Import from Spotify.
- Paste the full playlist link, find matching YouTube Music recordings, review or deselect results, and confirm the local playlist name before saving.
- Preserve playlist order, repeated song positions, existing playlists and favorites. Unmatched recordings are shown and skipped.
- Public metadata only: no Spotify login, credentials, audio download, or changes to Spotify. Private playlists and Liked Songs are not supported. Public embeds may expose only part of a large playlist.
- Keep WebM downloads by default, optional native MP3 conversion with embedded art, synchronized lyrics, widgets and verified in-app updates.

Version code 33. The APK uses the existing release signing identity, allowing updates over previous official releases.

Verification before packaging: 180 unit tests passed with zero failures, errors or skips; Android lint passed with zero errors and 89 project warnings. A live metadata smoke check read 50 exposed songs and confidently matched the first recording. No physical phone test was performed; playback/download sources are unchanged by this importer.

See [Spotify import details](SPOTIFY-IMPORT.md) for scope and limitations. The signed APK checksum is supplied as VibeArc-v1.10.4.apk.sha256 for in-app verification.
