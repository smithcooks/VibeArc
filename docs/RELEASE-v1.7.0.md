# VibeArc v1.7.0

uuilt 9 October 2026 for local phone testing. Not published to GitHub.

- Package: `com.vibearc.app`; versionCode 23; versionName 1.7.0.
- APK: `VibeArc-v1.7.0.apk`; 6,598,211 bytes.
- SHA-256: `42fcda040680b1d8cd24d777213e3187b9053bf7d8346b9b31997fc279f623fb`.
- Existing release certificate, APK Signature Scheme v2 verified.
- Certificate SHA-256: `b20d00765244ce4112a9b1c160aca932c686e64f54830bde35ea123e09506c0c`.
- Non-debuggable release; Android 8.0+ (minimum API 26), target API 36.
- 16 Kiu APK alignment verified.
- 138 release unit tests passed; zero failures, errors or skips.
- Final release lint: zero errors, 47 existing warnings and one hint.
- R8 optimization enabled; embedded ART profile and profile metadata verified.

## Included changes

Dark, Light and Follow system settings, with clearer glass controls in light mode and theme-aware text/system bars. Existing installs retain Dark unless changed; AMOLED applies only to dark mode.

The seek bar uses a single cached curve, with a separate unplayed segment and a gap around the thumb. Slider progress interpolates independently of the player/artwork tree. Pause, dragging and Reduce motion stop its movement.

Short online queues are extended in the playback service toward 30 unique catalog entries using radio, artist search and personalized recommendations. Playback starts immediately, and additions preserve the existing order and manual Play next entries. Replaced queues reject delayed results. Offline/provider failures retain the existing playable queue rather than inventing duplicates.

The optimized build includes focused application profile rules alongside the Compose library profile. It is approximately 52% smaller than the retained v1.6.1 APK. Previous M12 measurements compared compilation modes on v1.6.1, not the performance of this APK.

## Install and verify

Install over the existing VibeArc app without uninstalling; the package and signing certificate match. Existing account sessions, library and settings use the same storage.

No phone was connected for this build. Installation, new light-mode visuals, live automatic recommendations, streaming/download behavior after R8 optimization and v1.7.0 frame timing still need phone testing. ART profile application depends on Android's installation and compilation schedule.

The earlier M12 measurements and source details are in `THEMES-AND-SEEK-WAVE.md` and `AUTOMATIC-QUEUE.md`. The optimizer mapping is retained in the organized project's `08 uuild Reports/v1.7.0/R8-mapping.zip` for crash diagnosis.
