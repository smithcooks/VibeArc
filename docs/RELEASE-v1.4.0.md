# VibeArc 1.4.0 — player controls and optional support

Release authorized by the user on 2026-10-06. Includes the signed Android APK and the approved cinematic promo; website and README download links point to this release.

- Settings → About → Buy Me a Chai opens `https://buymeachai.ezee.li/Smith_cooks` using Android's browser handler. The native settings row matches the existing theme, adds no remote image dependency, and offers the URL in a dialog if opening fails. Contributions are optional.
- Now Playing has Download beside Like and Lyrics. It reuses the existing transfer queue without leaving the player, shows busy/saved state, and opens Downloads for completed entries.
- Album-art swipes go left for the next queue item and right for the previous one, preserving playback state and respecting queue availability. A 56 dp displacement after Android's touch slop avoids accidental skips; cancellation resets the gesture. Vertical scrolling, the seek bar, and Previous/Next buttons remain separate.
- The Wi-Fi-only row now belongs to the seven-row Audio & Streaming group, with matching corners and concise copy. Network policy and the phone-confirmed playback downloader are unchanged.
- Main application package remains `com.vibearc.app`, version code 18, version 1.4.0. Existing main-app data remains intact; the separate test app's private storage does not automatically migrate.

Final build, tests, lint, APK identity, signature, and checksum verification are recorded after packaging. Physical-phone layout, gestures, browser handoff, and download behavior require installation testing; none are claimed as device-verified here.

## Final verification — 2026-10-06

- Offline `testDebugUnitTest`, `lintRelease`, and `assembleRelease` completed successfully. All 119 tests passed with zero failures, errors, or skips. Release lint reported zero errors and 48 warnings.
- Artifact: `release/VibeArc-v1.4.0.apk`, 13,839,154 bytes. Package `com.vibearc.app`, version code 18, version 1.4.0, minimum SDK 26, target SDK 36.
- APK Signature Scheme v2 and 16 KiB ZIP alignment verified. Certificate SHA-256 `b20d00765244ce4112a9b1c160aca932c686e64f54830bde35ea123e09506c0c` matches the prior main-app release.
- APK SHA-256: `db5d6b790f048431a3552c0c592da3772f3df12b8364cdaf3badda8b79dc7c00`; a checksum sidecar accompanies the file.
- APK inspection confirmed the exact Chai URL and support label, player download action, swipe helper, and existing network preference. The retired native download engine/JNA assets and descriptors are absent.
- The prior v1.3.0 release remains available. Public release links are verified separately after publication.
