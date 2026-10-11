# VibeArc v1.8.1 — local test candidate

October 11, 2026. Do not publish this candidate, change the website download, or create a GitHub release until Smith tests and approves it.

## Changes

- Standby opens in artwork mode: large cover on the left, song/artist and playback/seek controls on the right in landscape.
- Show lyrics switches to smaller artwork with controls below on the left and synchronized lyrics on the right; Hide lyrics returns to artwork mode. Portrait adapts to a stacked layout.
- Artwork size reserves room for transport and seek controls on short landscape screens. Existing album swipes, glass controls and keep-awake behavior remain.
- Active lyrics use measured line height and viewport coordinates to center wrapped verses. Half-viewport padding allows first/last lines to center, and centering waits for that padding to finish measuring.
- Lyrics edit/export actions have a separate footer and no longer overlap the text. Existing animation and reduced-motion settings remain active.

## Audio and provider review

The working download engine and highest-available audio selection are unchanged. No lossy-to-FLAC conversion or false Studio Master label was added.

LastWave's current [lossless resolver](https://github.com/Clash-Projects/LastWave-Native/blob/main/app/src/main/java/com/lastwave/app/data/lossless/LosslessMusicApi.kt) uses an enabled HTTP add-on, requests `hi_res` and evaluates returned stream metadata. Its [settings](https://github.com/Clash-Projects/LastWave-Native/blob/main/app/src/main/java/com/lastwave/app/data/local/SettingsPreferences.kt) default the add-on URL to empty. No usable public setup URL was found in the repository; the add-on service is also excluded in its [gitignore](https://github.com/Clash-Projects/LastWave-Native/blob/main/.gitignore). Genuine online 24-bit/192 kHz FLAC integration remains unconnected until a working authorized source is available.

## Phone test

Open the player menu > Standby Mode in landscape. Check large artwork and visible previous/play/next/seek controls. Tap Show lyrics and verify the current sung line is centered, including wrapped, first and last verses. Seek forward/back, switch tracks, hide lyrics and rotate to portrait. Test with lyrics animation off and reduced motion enabled. Check larger fonts and both themes. Exit Standby and confirm the normal player remains usable.

USB debugging was reconnected and authorized briefly, then the device disappeared from the debugging connection. Native phone UI verification was not completed during this build; no pixel-perfect or frame-time claim is made.

## Build verification

- Release assembly and lint completed successfully; 148 unit tests passed with zero failures/errors. Five new tests cover centering and artwork sizing. An isolated incorrect-centering mutation failed two of those tests; production source was not mutated.
- Lint: zero errors, 49 warnings and one hint. Existing warnings remain; this is not a full security audit.
- Package `com.vibearc.app`, version name 1.8.1, version code 25, minimum SDK 26, target SDK 36, not debuggable.
- APK v2 signature verified and release certificate matches earlier APKs; zip alignment check passed.
- APK size: 6,631,003 bytes.
- SHA-256: `c553b5350c7b7476b989109d909b2bcc3994932bce43009521552da90b182020`.
- Certificate SHA-256: `b20d00765244ce4112a9b1c160aca932c686e64f54830bde35ea123e09506c0c`.
- Install over the existing signed VibeArc APK without uninstalling. Keep this build local until phone testing and approval.
