# Theme and seek-wave update — 9 October 2026

## Approved direction

Apple Music-style floating glass, clearer in light mode. Preserve VibeArc's screen structure, action labels and native Android touch/accessibility behavior. Dark, Light and Follow system are persisted in the existing appearance preferences. Keep existing installs dark; only System responds to phone appearance. AMOLED must never override explicit Light.

References reviewed:

- https://developer.apple.com/videos/play/wwdc2025/219/ — floating controls, adaptive legibility, Regular and Clear material distinctions.
- https://developer.apple.com/videos/play/wwdc2025/323/ — glass and prominent glass button treatments.
- https://developer.android.com/develop/ui/compose/performance/phases — keep frame-varying drawing state in the drawing phase.

This is an Android interpretation, not Apple's proprietary refraction renderer. Standard actions share tint, top-edge highlights and touch illumination. The existing floating backdrop effect remains limited to supported surfaces/devices; each small button does not get an additional live blur. Text and controls remain more opaque than decorative backgrounds for legibility.

## Seek-bar findings and change

The old track drew a full-width straight line beneath its animated wave. It also reconstructed the wave with up to 221 sine/point operations per frame, while displayed playback position changed in 500 ms steps.

The replacement draws the inactive segment only after the thumb, clips a single active wave before it, and caches the wave geometry by drawing bounds/theme. On an animation-only frame the curve receives one translation, not point reconstruction. The native Material slider still handles touch, keyboard and progress semantics. Progress interpolation is isolated from the player/artwork tree. Reduced motion freezes the curve and removes progress interpolation; disabling Wavy Seekbar gives a straight track.

## Performance evidence and limits

The path-operation reduction is a source-level work count, not a device frame-time benchmark of the new slider. The Samsung M12 was connected on 9 October and the installed v1.6.1 was profiled before any new APK was installed. A mixed interaction sample recorded 697 missed deadlines out of 1,657 frames (42.06%), with a 101 ms 95th percentile. UI/layout work dominated the trace; GPU frame times were much lower. Some AndroidOwner measure/layout slices exceeded 100 ms.

The same four tab swipes (350 ms per swipe, one-second pauses) were repeated twice per configuration:

| Installed v1.6.1 configuration | Missed deadlines, run 1 / run 2 | 95th percentile, run 1 / run 2 |
| --- | --- | --- |
| ART verify, glass enabled | 44.44% / 43.08% | 117 ms / 125 ms |
| ART full compilation, glass enabled | 9.89% / 9.64% | 38 ms / 34 ms |
| ART full compilation, glass disabled | 5.45% / 4.79% | 27 ms / 28 ms |

Android 13 reported thermal status 1 in the sampled runs. Full compilation was restored and Liquid Glass was restored to enabled before the phone disconnected. App data was preserved. Raw local diagnostics are in `build/m12-performance/` and are not intended for publishing.

These results justify addressing install-time compilation overhead and show that live glass has a separate cost. They do not prove the new source is stutter-free: the theme/slider changes and optimized release have not yet been installed or compared on the phone. Release R8 optimization is now enabled, and `app/src/main/baseline-prof.txt` adds a focused application profile alongside the existing Compose library profile. ART profile application depends on the device/install path; these rules do not force every sideloaded APK to be fully compiled immediately.

Official configuration references: https://developer.android.com/develop/ui/compose/performance and https://developer.android.com/topic/performance/baselineprofiles/manually-create-measure.

Before shipping, compare the same populated library in both themes on the Samsung M12: feed scroll, rapid tab taps, swipes, now-playing/lyrics seeking, paused/resumed playback, large text, TalkBack, Liquid Glass off and Reduce motion. Check light-mode contrast over both bright and dark artwork. Preserve the previous APK for that comparison.

## Source validation

The final offline Gradle run completed successfully with 138 unit tests, zero failures/errors, and lint reporting zero errors, 47 existing warnings and one hint. Release R8 optimization and ART profile compilation both completed. The generated release profile is 5,152 bytes with 630 bytes of format metadata. These are build checks, not a substitute for a new-APK phone test.

The archived v1.6.1 APK is unchanged (SHA-256 `a153d7f459d480d6bf4dd9fb629753cda96e32571de613e33bb5db13074367b4`). At the user's request, this source update was subsequently packaged as v1.7.0 on 9 October 2026; see `RELEASE-v1.7.0.md`. It has not been published to GitHub.

The phone disconnected before its temporary trace could be removed. At the next authorized connection, delete only `/data/local/tmp/vibearc-ui-trace.txt`, `/data/local/tmp/vibearc-performance-screen.png` and `/data/local/tmp/vibearc-ui.xml`; the trace is approximately 185 MB. The local diagnostics remain under the ignored build directory.
