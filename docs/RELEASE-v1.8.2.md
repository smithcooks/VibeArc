# VibeArc v1.8.2

## What's new since v1.7.0

- Standby Mode: a screen-filling glass player with Back, mode title, Show/Hide lyrics and the player menu inside the panel.
- Artwork mode shows large album art with music controls alongside it. Lyrics mode places smaller artwork and controls on the left and synchronized lyrics on the right in landscape.
- Active lyric centering accounts for measured line height; lyric actions no longer cover the verse.
- Catalog browsing adds artist, album and playlist search/details, with actual artist songs and releases where the source supplies them.
- Highest-available source selection for playback and original-audio downloads. Actual quality depends on the provider; this release does not promise online Hi-Res FLAC or bit-perfect output.

## Install

Android 8.0 or newer. APK version 1.8.2, version code 26, signed with the existing VibeArc release certificate. Export a backup before updating; install over the main app without uninstalling it.

SHA-256: `dad8af405824400f08f1f154c598a0e7282432379579aa3f03d8c10e9b730fc8`.

## Verification and known limits

148 unit tests passed. Lint reported zero errors, 49 existing warnings and one hint. APK signature and 16 KiB ZIP alignment verified. The latest fullscreen layout was not checked on a connected physical device. Online availability and artwork loading depend on the source and connection. Android may recommend a Play Protect scan for a sideloaded APK; signing does not guarantee suppression of that prompt.
