# v0.9.1 playback check — 2026-10-02

The supplied public album `MPREb_KTxpHsPdd8z` contains recording `T6eK-2OQtew`.
The PC live check can resolve and fetch its audio. The reported phone failure has
not been independently reproduced on a Samsung M12.

## Measurements and fixes

- Requesting FLAC on this recording previously threw “selected format is not available”.
  The playback-only fallback now returns a real Opus/WebM source (HTTP 206).
  Downloads still reject a codec the source does not offer.
- Resolving a cold source took 3.7–6.4 seconds; reusing its source took less than
  1 ms. Home pre-resolution moves this work before the tap when possible.
  A concurrent prefetch/tap regression check proves one lookup, not two.
- Fetching the first 64 KiB without a Range header took 1,766 and 2,176 ms.
  Open-ended `Range: bytes=0-` took 54 and 66 ms on the same recording.
  The shared player resolver supplies that header; Media3 still generates its
  own position-specific range for seeks. No custom downloader or buffer reduction.
- HTTP 403/410 gets at most one source-cache refresh per current song, not an
  unbounded retry loop. Failure logs contain only codes/classes, not signed URLs,
  cookies, or account details; the toast distinguishes HTTP, verification, and network errors.

## Reproduce

Run `gradlew.bat -I tools/playback-probe.gradle :app:probeOnlinePlayback
-PprobeUrls=https://music.youtube.com/watch?v=T6eK-2OQtew` with network access.
This opt-in probe checks the real source and audio-container prefix, not audible
Android playback. No network-dependent tests are silently skipped in the unit suite.

89 unit tests pass; an isolated reversed range condition fails its regression test.
Phone tap-to-audible timing, full-song playback, and source restrictions remain
device/network checks. Install this signed beta over the previous build; do not
uninstall or clear data. Prior APKs are retained; rebuilding the prior commit with
a higher version code permits a data-preserving rollback if needed.
