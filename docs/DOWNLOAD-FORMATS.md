# Download formats

New installations and upgrades default to **WebM**. Settings → Playback & Downloads → Download format offers:

- **WebM · original Opus:** highest available genuine WebM/Opus source, without re-encoding. New files are exported to Download/VibeArc on Android 10+, or Music/VibeArc on Android 8-9. The generic Downloads collection avoids Android's `Unsupported MIME type audio/webm` rejection in the audio-only collection. If the recording has no WebM source, the job reports failure rather than mislabelling AAC or MP4.
- **MP3 · converted on device:** download the highest available playback source, decode locally and encode with native LAME at 320 kbps, embedding JPEG cover art and title/artist/album metadata. This is lossy conversion, not a quality upgrade. It takes extra time.

WebM is first and conversion is off by default. Version 1.10.5 ignores the old saved MP3 preference once on upgrade; a new explicit MP3 choice remains saved. The current choice is captured when a new job is enqueued or an unfinished job is retried. Changing the preference does not alter active or queued jobs or existing files. Manual Create MP3 copy never starts automatically.

## Convert a saved download later

Open Downloads → a song's three-dot menu → **Create MP3 copy**. This is available for owned, completed non-MP3 downloads. It reads the saved original directly; there is no audio re-download or conversion server. Imported external originals are not modified.

Conversion is serialized in the foreground download service. The original remains playable and its lyric sidecar is preserved. Progress, cancellation and retry are shown on the saved song. A process interruption records a retryable conversion error without hiding or corrupting the original. Successful copies are accessible via **Open MP3 copy** and the phone file manager in Music/VibeArc.

Public exports are separate copies. Removing a song from VibeArc does not delete its public WebM or MP3 exports. MP3 creation needs additional free storage for the temporary output and public copy. Cover art already saved by VibeArc works offline; fetching missing artwork needs a connection. Android 8–9 may request storage permission. Android 10+ uses pending MediaStore writes and removes an incomplete fresh export on failure.

## Quality and verification boundaries

WebM retains source quality; MP3 cannot restore detail absent from that source. No 24-bit/192 kHz or lossless claim is made. Native LAME is dynamically linked for four Android ABIs and its LGPL notices and source are included in the repository. A five-minute synthetic PCM probe encoded in 9.75 seconds on the build PC; this is not a phone benchmark or an under-two-minute guarantee.

No phone was connected for this release's tests. Unit tests cover defaults, strict WebM selection, persistence/backward compatibility, and conversion eligibility. Android decoder, launcher, file-manager visibility, and installation should still be exercised on the target phone.
