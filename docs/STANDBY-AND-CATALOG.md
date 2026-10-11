# Standby and catalog behavior

## Design contract

Artwork is the visual anchor; VibeArc's theme-aware glass controls carry the actions. Standby opens with large artwork on the left and title, artist, transport and seek controls on the right in landscape. Show lyrics switches to smaller artwork with controls below on the left and animated synchronized lyrics on the right; Hide lyrics returns to artwork mode. Portrait uses stacked content. The compact header and height-based artwork budget keep controls visible on short landscape screens; enlarged text can scroll. Native controls remain at least 48 dp. Existing reduced-motion and lyrics-animation switches apply.

Synchronized lyrics center the actual measured active line, including wrapped verses and the first/last line, using half-viewport content padding. The edit/export toolbar reserves its own footer instead of overlapping the lyric text. Untimed plain lyrics remain manually scrollable.

Search separates real song, artist, album and playlist identities rather than grouping songs into fake catalog entries. A featured result leads a compact list. Artist pages show actual artwork, supplied biography, songs and release shelves; albums open their own track lists. Do not invent audience counts, verification badges, genre tags, releases or lyrics. Failed provider requests have retry and empty states.

Settings uses five categories without removing account, audio, appearance, backup or support controls. Seek progress stays independently animated; the single cached wave uses a 5 dp stroke. Keep album swipe, downloads and long-press song actions working.

## Audio policy

Review reference: [LastWave YouTubeStreamExtractor](https://github.com/Clash-Projects/LastWave-Native/blob/main/app/src/main/java/com/lastwave/app/data/music/YouTubeStreamExtractor.kt). It ranks native renditions by codec preference and reported bitrate; its downloader can request Opus. No transcoding can recover detail absent from the original recording.

The test candidate should select the highest supplied recording, prefer Opus at equal compressed bitrates, retain genuine lossless sources when available, and save original audio bytes. Show reported codec, bitrate and sample rate instead of a quality slogan. YouTube availability, account entitlement, headphones, Bluetooth and Android's mixer still constrain the result. No universal FLAC, Hi-Res or bit-perfect-output guarantee.

## Approval gate

Build a signed APK locally. Do not publish source, APK, website changes or a GitHub release until Smith has tested and approved it. Verify navigation, portrait/landscape Standby, live lyrics, downloads, and the actual sound/format on the phone before making performance claims.

## Genuine Hi-Res dependency

On October 11, 2026, review of LastWave's current `LosslessMusicApi.getCredentials` and `resolveStream` showed that its Hi-Res path requires an enabled, user-configured HTTP add-on URL. `SettingsPreferences` defaults that URL to empty. The resolver requests `hi_res`, checks returned stream metadata and falls back when unavailable; YouTube extraction remains a separate compressed-audio path. Source: [LastWave lossless resolver](https://github.com/Clash-Projects/LastWave-Native/blob/main/app/src/main/java/com/lastwave/app/data/lossless/LosslessMusicApi.kt), [add-on settings](https://github.com/Clash-Projects/LastWave-Native/blob/main/app/src/main/java/com/lastwave/app/data/local/SettingsPreferences.kt).

Do not relabel YouTube audio as 24-bit/192 kHz FLAC or transcode it to create a false quality claim. A working, authorized Hi-Res add-on/provider is still needed before online Studio Master integration can be implemented and verified. This Standby patch does not add that backend.
