# V8 performance test build

Approved screenshot layout is unchanged. This is a performance checkpoint, not the completed V8 release.

## Changes

- Artwork cache: 12 MiB, at most two simultaneous loads, cancellable queued loads.
- Decode list thumbnails near their display size (160 px target); keep player/hero artwork at a 768 px target without upscaling.
- Color changes no longer restart image downloads. Accent extraction runs off the UI thread.
- Local metadata extraction runs off the UI thread and reports unreadable files.
- Playback progress updates are confined to the seek-bar subtree; dragging is not overwritten by the timer.
- `performance` APK: R8 code optimization enabled, non-debuggable, existing debug test certificate. Optional resource shrinking is off: the Windows sandbox denies the shrinker's ZipFS access to the generated resource archive. This retains unused resources, without disabling code optimization. Not a production-signed release.
- Rhino compatibility rules follow [NewPipe's Android rules](https://github.com/TeamNewPipe/NewPipe/blob/dev/app/proguard-rules.pro); the extractor's [interpreted JavaScript path](https://github.com/TeamNewPipe/NewPipeExtractor/blob/v0.26.5/extractor/src/main/java/org/schabi/newpipe/extractor/utils/JavaScript.java) has a runnable smoke test. Desktop-only JSR-223 and JDK linker warnings are scoped, not globally suppressed.

## Rebuild

`gradlew.bat testDebugUnitTest lintDebug assemblePerformance`

If the default debug-keystore lock is inaccessible, use an authorized copy of the same key in the ignored `.android/` directory and pass `-PtestKeystore=.android/performance-test.keystore`. Do not generate a different key or uninstall the app to work around an update-signature mismatch.

## Evidence and limits

The sizing test checks that a 1024×1024 input uses sample size 4 for thumbnails (256×256 decoded pixels, one-sixteenth the pixel count). This is not a measured FPS improvement. No physical-device performance result is claimed. Automated tests and lint are recorded at APK handoff.

## Manual Samsung M12 check

1. Install as an update; retain existing app data. Stop if Android reports a signature conflict.
2. With Liquid Glass off, repeatedly scroll search results and Settings, then switch Feed/Playlists/Stats.
3. Play a track, drag the seek bar, return to Feed and reopen the player. Verify audio continues and the back action works.
4. Repeat with Liquid Glass on and Dynamic Now Playing enabled. Check image quality and artwork colors.
5. Import a local audio file; confirm the app stays responsive. Test online search/playback because this build enables code shrinking.
6. Report the screen/action that still stutters, glass setting, and whether the phone was hot or in power-saving mode.

## Still required for full V8

Connected YouTube account and two-way playlist sync; optional Last.fm account/scrobbling; synced lyrics; built-in equalizer; actual stream-format selection; supported downloads/folder selection; real discovery/generator integration; final security/release verification. JSON backup/restore and local CSV, TSV, M3U/M3U8, or TXT playlist imports are now connected. The 28 September UI pass adds the player/queue/lyrics layouts and lightweight wavy seek bar. Settings descriptions must continue to state what is unavailable. No lossless, bit-perfect, or zero-buffering guarantee is made.
