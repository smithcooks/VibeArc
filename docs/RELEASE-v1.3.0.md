# VibeArc 1.3.0 — main-app offline downloads

The user confirmed that the separate playback-download test APK downloads successfully on their phone. This release promotes that same resolver/transfer implementation to the main `com.vibearc.app` package. The native SpotiFLAC host, JNA dependency, extension store, provider packages, and archive/direct-provider queue are no longer built or shipped.

Downloads now has a reference-style compact library: header totals, Songs/Artists/Albums tabs, search, sorting, All/Lossless/With Lyrics filters, storage details, Play/Shuffle, and per-song menus for information, pause, cancel, retry, and removal. Badges describe actual saved metadata; no FLAC/transcoding guarantees are invented. Previously matched cached synchronized lyrics can be saved alongside audio. Files exports audio copies through Android's folder picker; private app storage is not mislabeled as a public Music folder.

Wi-Fi and mobile data are allowed by default, including on upgrade from the old implicit Wi-Fi-only default. Settings → Audio & Streaming → Download on Wi-Fi only enables the optional restriction. Carrier charges may apply. A connection that fails Android's internet validation pauses the job, retaining saved state.

The main app's existing jobs JSON, catalog identities, saved audio, local originals, library, accounts, and theme preferences remain in place. Downloads stored by the separate test package remain in that separate app's private storage; Android does not automatically merge them into the main package. Retired extension sources/tests/assets are recoverably archived under `legacy-download-engine`; the former root module/native sources are excluded from the app build.

Verification is recorded after the final build. The user authorized GitHub release publication and website download updates on 2026-10-06. Physical-device layout, Files export, and mobile-data transfer still require installation testing; no phone is attached to this build environment.

## Final build verification — 2026-10-05

- Offline `testDebugUnitTest`, `lintRelease`, and `assembleRelease` completed successfully on the final sources. All 118 unit tests passed, with no errors, failures, or skips. Release lint has no errors; existing warnings remain.
- Artifact: `release/VibeArc-v1.3.0.apk`, 13,839,150 bytes. Package `com.vibearc.app`, version 1.3.0, version code 17, minimum SDK 26, target SDK 36.
- APK Signature Scheme v2 verified. RSA-4096 signing certificate SHA-256: `b20d00765244ce4112a9b1c160aca932c686e64f54830bde35ea123e09506c0c`, matching the existing main/test APK certificate. 16 KiB ZIP alignment verified.
- APK SHA-256: `1c3731ac4282cc0e830d752561f0ce04d1e350b9b7efdeb2fd4fe23d2204e63c`. A checksum sidecar accompanies the APK.
- APK inspection found no retired native download-engine assets, library names, JNA descriptors, NativeDownloads descriptor, or ExtensionStore descriptor. The new `playback_wifi_only` preference is present.
- Phone layout, folder export, and actual mobile-data downloads remain unverified here. GitHub publication uses this exact verified APK and matching source, without including signing keys or account credentials.
