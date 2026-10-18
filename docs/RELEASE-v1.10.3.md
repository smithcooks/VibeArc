# VibeArc v1.10.3

- WebM/Opus downloads by default, without automatic MP3 conversion.
- Optional MP3 for new downloads in Settings → Playback & Downloads → Download format.
- Create MP3 copy from a saved song's options, without downloading the audio again. Original audio and lyrics stay in VibeArc.
- Native on-device LAME conversion at 320 kbps with embedded cover art and song metadata. Progress, cancellation, retry and restart recovery.
- Public audio files saved in Music/VibeArc for file-manager access.
- Last.fm integration removed; local listening statistics and the library preserved.
- Normal release name: VibeArc v1.10.3. Version code 32, compatible with the existing release signing identity.

See [download format details](DOWNLOAD-FORMATS.md) for storage behavior and quality limits. MP3 conversion does not improve the source's quality. Conversion timing and Android behavior have not been measured on the user's phone.

Verification: all 173 unit tests passed, with zero failures, errors or skips. Lint reports zero errors, 88 warnings and one hint. An opt-in live smoke test resolved a public recording to audio/webm, read a 65,536-byte prefix over HTTP 206, and verified its EBML/WebM signature. This does not substitute for Android device testing.

The exact signed APK checksum is attached as VibeArc-v1.10.3.apk.sha256 so existing in-app update checks can verify it.
