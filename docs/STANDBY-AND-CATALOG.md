# v1.8.0 local test build

## Design contract

Artwork is the visual anchor; VibeArc's theme-aware glass controls carry the actions. The supplied Canva layout defines Standby: artwork and transport controls on the left, animated synchronized lyrics on the right in landscape, stacked in portrait. Native controls remain at least 48 dp and text remains readable in both themes. Existing reduced-motion and lyrics-animation switches apply.

Search separates real song, artist, album and playlist identities rather than grouping songs into fake catalog entries. A featured result leads a compact list. Artist pages show actual artwork, supplied biography, songs and release shelves; albums open their own track lists. Do not invent audience counts, verification badges, genre tags, releases or lyrics. Failed provider requests have retry and empty states.

Settings uses five categories without removing account, audio, appearance, backup or support controls. Seek progress stays independently animated; the single cached wave uses a 5 dp stroke. Keep album swipe, downloads and long-press song actions working.

## Audio policy

Review reference: [LastWave YouTubeStreamExtractor](https://github.com/Clash-Projects/LastWave-Native/blob/main/app/src/main/java/com/lastwave/app/data/music/YouTubeStreamExtractor.kt). It ranks native renditions by codec preference and reported bitrate; its downloader can request Opus. No transcoding can recover detail absent from the original recording.

The test candidate should select the highest supplied recording, prefer Opus at equal compressed bitrates, retain genuine lossless sources when available, and save original audio bytes. Show reported codec, bitrate and sample rate instead of a quality slogan. YouTube availability, account entitlement, headphones, Bluetooth and Android's mixer still constrain the result. No universal FLAC, Hi-Res or bit-perfect-output guarantee.

## Approval gate

Build a signed APK locally. Do not publish source, APK, website changes or a GitHub release until Smith has tested and approved it. Verify navigation, portrait/landscape Standby, live lyrics, downloads, and the actual sound/format on the phone before making performance claims.
