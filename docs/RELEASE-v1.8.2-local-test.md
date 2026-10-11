# VibeArc 1.8.2 — fullscreen Standby candidate

Standby uses one screen-filling glass surface. Back, STANDBY MODE, Show/Hide lyrics and the existing player menu are inside that surface. The surrounding card margins have been removed; controls still respect Android's system-bar safe areas.

The normal artwork-and-controls mode, split lyrics mode, artwork swipes, keep-screen-awake behavior and measured active-verse centering are retained. Playback, audio quality and download behavior are unchanged. No new dependency or permission is added.

This is a local test candidate only. Phone layout validation and an actual screenshot require a connected, authorized Android device. Do not publish until the user approves.

## Build verification

- Version 1.8.2, code 26; non-debuggable release APK, 6,630,999 bytes.
- 148 unit tests passed; lint has zero errors, 49 existing warnings and one hint.
- Release signature verified against the previous signing identity; 16 KiB ZIP alignment passed.
- SHA-256: `dad8af405824400f08f1f154c598a0e7282432379579aa3f03d8c10e9b730fc8`.
- Source layout assertions accept the screen-filling surface and integrated header, and reject the previous outer-card layout. These are not rendered UI tests.
- No Android device was detected; portrait/landscape rendering, touch interaction and a real screenshot remain unverified. No GitHub or website publication occurred.
