# VibeArc v1.0.0

**Good music. No noise.**

Signed Android release APK. Android 8.0+; version code 12; target Android 16.
Install over v0.9 or v1.0.0-beta without uninstalling to preserve app data.
Export a JSON backup before updating.

## What's changed

- Removed the beta label from the app version and APK filename.
- Lyrics use cleaned metadata and LRCLIB search fallback, then KuGou KRC and
  Lyrics.ovh. Empty responses no longer hide useful matches.
- Last.fm setup can be saved in Settings without rebuilding. Public profile
  configuration is separate from authenticated sign-in.
- Now Playing waits for actual playback. Scrobbling honors enabled/exclusion
  rules and only marks submissions complete after acknowledgement, with
  bounded retries for the active track.
- The Last.fm signer uses signed GET for auth/read requests and POST for writes;
  ignored submissions are no longer treated as success.
- In-app GitHub update checks and source/support links use smithcooks/VibeArc.
- Refreshed README, installation instructions, and integration documentation.

## Verification

- 94 Android unit tests and 3 signer regression tests passed.
- Release build and lint passed: 0 errors, 46 warnings, 1 hint.
- APK alignment and v2/v3 signature verification passed; the signing
  certificate matches the previous v0.9 release.
- APK size: 13,790,881 bytes.

SHA-256:

```text
90fde808ebfb1d7482b948f4a6f2a0b6d51c94e9ec567e5f42dc624164b6d4bf
```

## Before connecting accounts

Last.fm needs your API application and deployed HTTPS signer; no shared secret
or hosted signer is bundled. Live authenticated login/scrobbling is not yet
verified with a configured account. Follow `server/lastfm-signer/SETUP.md`.

YouTube Music uses an unofficial WebView/cookie session, not official Google
OAuth. Online availability depends on providers and restrictions. Download only
music you have permission to save. Lyrics/word timestamps are not universal,
and lossless source labels do not guarantee Hi-Res or bit-perfect device output.

This release is for sideloading. It is not a Google Play listing or Play Protect
certification. Existing online integrations remain experimental.
