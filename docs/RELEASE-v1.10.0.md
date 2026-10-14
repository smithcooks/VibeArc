# VibeArc v1.10.0

## New and improved

- Seven glass-style Android home-screen widgets: Player, Album, Vinyl, Console, Lyrics, Mini and Aurora. Content-sized panels, flat playback icons, sharper bounded artwork and compact fallbacks replace stretched cards.
- In-app updates under Settings → About & Support. Download verified stable-release APKs without visiting the website, with optional startup reminders and Android-approved installation.
- More resilient lyrics matching: recognized lyric-video/HD decorations are cleaned, recording-aware caching avoids mixing videos, and a validated SimpMusic video-ID fallback complements LRCLIB, KuGou and plain lyrics providers.

Remove and re-add existing widgets for their new default sizes. Export a JSON backup before updating; install over the existing main app without uninstalling or clearing data.

## Download and verification

- Android 8.0+; package `com.vibearc.app`; version code 28.
- `VibeArc-v1.10.0.apk`: 6,660,923 bytes, signed with the existing release certificate.
- SHA-256: `2186ff4e237bde1b5c426e43841eb97ba6760ad6d1db019b118f265be231be97`.
- APK v2 signature and 16 KiB alignment verified; all 164 unit tests pass.
- Lint: zero errors, 87 warnings and one hint. Source-derived widget previews checked 64 size/font combinations; these are not native launcher screenshots.

## Known limits

Actual launcher appearance, TalkBack, and the update download/installation permission-return flow have not been verified on a phone for this build. Android widgets use translucent surfaces and static highlights, not live wallpaper refraction. Lyrics depend on the exact recording and provider availability. Saved/streamed quality depends on the source; no invented Hi-Res quality or Play Protect certification is promised.

The previous v1.8.2 release remains available. Website links can be reverted independently; Android normally blocks installing an older version code over this release. Preserve your data and wait for a signed higher-version fix rather than uninstalling to downgrade.
