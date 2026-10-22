# Main-app WebM export compatibility

Scope: stable v1.10.4 source, isolated on `fix/webm-download-export`. The separate
`test/spotify-public-playlists` branch and its delivered APK are not changed.

## Cause

`publishDownloadAudio` inserted both MP3 and WebM into `MediaStore.Audio.Media`.
Android versions whose MIME map does not recognize `audio/webm` reject that insert
with `IllegalArgumentException: Unsupported MIME type audio/webm`. This occurs
after transfer and validation, during public-file publication. Every queued song
uses that function, so a playlist can fail on every entry for the same reason.

Android's MediaProvider permits arbitrary MIME types for generic collections but
validates MIME compatibility for the audio collection. Generic collections also
have different permitted top-level directories:

- [Android MediaProvider source](https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/android12-release/src/com/android/providers/media/MediaProvider.java)

## Change

- Android 10+: publish genuine WebM using `MediaStore.Downloads`, under
  `Download/VibeArc/`, with its original `audio/webm` MIME and `.webm` extension.
- MP3 remains in `MediaStore.Audio.Media`, under `Music/VibeArc/`.
- Android 8–9 direct-file exports retain `Music/VibeArc/`.
- Use only common media columns for Downloads; artist/album columns stay on audio
  inserts. Retain pending publication, close-before-publish, and rollback on error.
- Do not transcode, rename WebM to MP3/MP4, lower quality, move existing files,
  change playlist import, or discard in-app lyrics.
- Correct the Downloads/Settings descriptions so they no longer promise that all
  public files are in Music.
- Reset the legacy MP3 format preference to WebM on upgrade. MP3 is opt-in using
  a new preference key; a subsequent explicit choice remains saved. WebM is first
  in Settings, with no conversion. New/retried jobs use the current format choice;
  already-running jobs retain their captured format. Manual Create MP3 copy stays
  available and never starts automatically.

## Regression coverage

`DownloadExportTest` covers scoped-storage routing for Android API 29–36,
unchanged MP3/legacy paths, and genuine format labels. `PlaybackDownloadTest`
also exercises 100 mocked WebM transfers, asserting unchanged bytes, connection
cleanup, and compatible export routing. These are host-side tests, not a physical
phone or real Android MediaProvider integration test.

`DownloadFormatTest` also checks that a legacy MP3 preference does not enable
conversion automatically, WebM is the first option, and a new explicit MP3 choice
persists until the user selects WebM again.

Final verification: `:app:testReleaseUnitTest --offline` succeeded against the
stable main-app source with both changes. No APK was assembled or published.

Phone acceptance check for the next main-app APK: select WebM, retry a failed
song and then a playlist, confirm completion/playback and public `.webm` files in
Download/VibeArc. Separately check MP3 publication and Create MP3 copy in
Music/VibeArc. No GitHub release or website deployment is part of this fix.
