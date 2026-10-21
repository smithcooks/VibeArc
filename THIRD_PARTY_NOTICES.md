# Third-party notices

Optional native MP3 conversion uses LAME 3.100 under LGPL-2.0-or-later. The encoder
source, upstream COPYING, archive checksum and build configuration are in
app/src/main/cpp; native_mp3.c is VibeArc's JNI bridge. The original archive is
https://downloads.sourceforge.net/project/lame/lame/3.100/lame-3.100.tar.gz.
LAME is distributed as a shared library. License and attribution are bundled in
assets/licenses. Source and replacement/relinking obligations must be preserved
when distributing the APK. The former jump3r Java encoder is no longer used.

VibeArc uses AndroidX and Media3 components distributed under the Apache
License 2.0 and NewPipeExtractor, which is licensed under GPL-3.0-or-later.
Distributing a build
that includes NewPipeExtractor requires a GPL-compatible VibeArc source release
and the corresponding source obligations to be met.

Network features access YouTube/YouTube Music, Spotify public playlist metadata, KuGou, LRCLIB, Lyrics.ovh, SimpMusic,
and GitHub.
Their names, artwork, metadata, lyrics, and other content remain subject to the
respective service terms and rights holders. VibeArc does not grant rights to
download or redistribute third-party media.

Authoritative project and service terms:

- https://github.com/TeamNewPipe/NewPipeExtractor
- https://www.apache.org/licenses/LICENSE-2.0
- https://developers.google.com/youtube/terms/developer-policies
- https://www.youtube.com/static?template=terms
- https://lrclib.net

This is a release checklist notice, not legal advice. Preserve dependency
license texts and complete a publisher review before public distribution.
