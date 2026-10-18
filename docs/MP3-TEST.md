# MP3-only download test build

Historical notes for the superseded local test. Public v1.10.3 uses WebM by
default and optional MP3; see [Download formats](DOWNLOAD-FORMATS.md). In that
release, saved WebM downloads can be converted without removing or downloading
the original again. The APK name is simply VibeArc-v1.10.3.apk.

Local-only version: 1.10.3-native-mp3-test, version code 31. Do not publish this build.
Use a version code above 31 for a later public update to test installations.

Last.fm integration is removed from this test version: account/sign-in UI,
scrobbling/Now Playing, remote profile/radio requests and build-time credentials.
On opening the updated app, only its retired lastfm_profile preferences are
deleted. Local recent history, library, playlists and downloads stay intact.
YouTube Music and on-device discovery/stats continue to work.

This version replaces jump3r's Java encoding loop with native LAME 3.100, compiled
from the pinned upstream source in app/src/main/cpp/lame. Android NDK r28c and
CMake 3.22.1 build arm64-v8a, armeabi-v7a, x86_64 and x86 with -O3 and 16 KiB ELF
alignment. Encoder settings stay 320 kbps CBR, quality 2, joint stereo (mono for
mono input); no lower-quality speed preset and no Java encoder fallback.

Decoded PCM crosses JNI in bounded blocks. MediaCodec direct buffers avoid a
per-sample Kotlin loop; heap buffers use a reusable direct scratch block. Native
code validates the buffer window and safely handles mono, stereo, float PCM,
resampling and finalization. Storage-space checks occur once per second rather
than once per decoded packet. Conversion duration is logged under VibeArcMp3.

The goal is under 120 seconds for typical 3–5 minute songs. This is a performance
target, not a measured phone guarantee. No phone is connected for this build.
Download/network time, thermal throttling, long recordings and device speed vary.
There is no artificial two-minute cutoff that would discard a valid conversion.

Windows native tests: with JDK 21 in JAVA_HOME and MinGW GCC on PATH, run
`./tools/build-native-host.ps1` before Gradle unit tests. Gradle's test JVM loads
build/native-host/vibearcmp3.dll. The tests exercise the same native/JNI source,
not a test-only Java encoder. The opt-in NativeMp3Benchmark main class encodes a
synthetic five-minute stereo workload; desktop results are not phone results.

Acceptance criteria:

- New online downloads decode the original source and encode actual 320 kbps
  MPEG-1 Layer III audio. Changing an extension is not conversion.
- Completed MP3 copies appear in `Music/VibeArc` in the phone's file manager.
  Android 10+ uses MediaStore pending rows; Android 8–9 asks for storage permission.
- Title, artist, album and front-cover JPEG are embedded in ID3v2.3 inside the MP3.
  Cover bytes are checked with Android's metadata reader before publication. A
  recording with an artwork URL fails explicitly if the cover cannot be fetched,
  rather than silently exporting a coverless file. No cover is invented when the
  recording has no artwork URL. The private JPEG remains for VibeArc's library.
- Artwork and lyrics requests start alongside the audio download. Conversion
  displays its actual percentage. Lyrics network fallback has a 25-second budget
  (an already-blocked connection operation may finish after that deadline), and
  publication does not wait for lyrics. No encoder-quality reduction was made.
- An additional private MP3 supports VibeArc offline playback. Removing a download
  inside VibeArc leaves its exported Music file alone. This uses space for two MP3 copies.
- Available synced or plain lyrics are cached privately. Missing lyrics must not
  make an otherwise valid audio download fail. Provider coverage is not guaranteed.
- Older downloads/local originals retain their format and contents. To convert an
  older online download, remove its private copy and download that song again.
- Cancel/pause/failure must not expose an incomplete MP3. Encoding happens on the
  existing serialized background service, with bounded PCM buffers.
- Streaming is unchanged. MP3 transcoding is lossy; 320 kbps is an encoding setting,
  not a promise of Hi-Res or recovery of detail absent from the source.

Phone test: download one AAC/MP4 source and one Opus/WebM source; confirm `.mp3`
files in Music/VibeArc and play them in a separate player. Enable airplane mode,
play both in VibeArc and open available lyrics. Pause/cancel another conversion,
retry it, and confirm no incomplete public file. Existing v1.10.0 app data should
remain when installing this same-signer, higher-version-code APK as an update.
Check that the external player shows the cover/title/artist even when opening the
MP3 outside VibeArc. Existing exported MP3s are not rewritten: redownload a song
for embedded cover art, and keep or manually remove its old public copy as desired.

Test reports and exact APK checksum are supplied with the built artifact.
