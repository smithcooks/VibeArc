# Home-screen widgets and lyrics lookup

Local v1.10.0 widget correction and in-app updates prepared for user testing on October 12, 2026. No public release or website update created for this work.

## Widget layout correction

The user's launcher screenshots exposed stretched match-parent surfaces and vertical weighted gaps. Visible glass now wraps content inside a transparent host; metadata, artwork and flat transport controls stay together. Short Android 12+ slots use a size-map compact fallback; older Android uses compact landscape and full portrait layouts. Remove/re-add existing widgets to get the corrected default grid footprint: existing launcher placements are not silently changed.

Regression tests cover all eight layouts, wrapping surfaces, vertical weights, supported RemoteViews classes, flat artwork and labelled 48 dp controls. A source-derived HTML preview checks 64 size/font combinations with fixture metadata and artwork. It is not an Android renderer or a phone screenshot. Native launcher sizing, accessibility and transport-action testing remain unverified because no device is connected.

## Widgets

Glass Player (compact controls) and Glass Album (larger artwork) use rounded translucent surfaces, specular edges, glass transport buttons and real metadata. Tap artwork/metadata to open VibeArc. Controls have 48 dp targets and accessible labels; unavailable skips are disabled.

The five supplied references add Glass Vinyl (record and central artwork), Glass Console (side transport rail), Glass Lyrics (current synchronized line), Glass Mini (round artwork and compact controls), and Glass Aurora (wide ocean-tinted player). Each has a separate launcher picker entry and resize constraints. No third-party branding or artwork is bundled.

Design contract: metadata and transport controls come first, text remains white against legible dark translucent surfaces, each control retains a 48 dp hit target, long titles ellipsize, and progress represents the actual recording. The vinyl grooves are decorative, not a fake visualization. Live wallpaper blur/refraction, spinning records and a fabricated waveform are not promised. The lyric card uses VibeArc's provider chain and manual timing overrides, shows the currently sung line, and opens the app when tapped.

Updates follow Media3 playback events and the existing service tick, with no frame loop or new timer. Progress/lyrics updates are limited to once per second and sent only when their displayed values change and a widget is placed. Artwork reuses the cache and is bounded to a 384 px transport thumbnail; background loading happens only when a widget exists. Previous metadata survives a process restart; starting a new queue after the service exits requires opening the app. No wallpaper permission is requested.

Android RemoteViews cannot host VibeArc's Compose glass shader or Apple's system glass renderer. This is actual translucency with static glass highlights, not live launcher-wallpaper refraction. Launchers control final dimensions and corner treatment. Phone, launcher and large-font testing remain required.

References:

- https://liquidglassdesign.com/gallery/audio-player-card-design-liquid-glass-effect
- Five user-supplied widget reference images (October 12, 2026).
- https://developer.apple.com/videos/play/wwdc2025/278/
- https://developer.apple.com/videos/play/wwdc2025/219/
- https://developer.apple.com/documentation/widgetkit/optimizing-your-widget-for-accented-rendering-mode-and-liquid-glass
- https://developer.android.com/develop/ui/views/appwidgets

## Lyrics

LRCLIB has synchronized lyrics for Flatline by Justin Bieber. A failing regression test reproduced retained `(Official Lyric Video)` and `[HD]` metadata; the cleaner now strips recognized presentation labels while preserving recording-version markers.

A SimpMusic fallback queries the validated YouTube video ID when LRCLIB misses, using the public API demonstrated in LastWave. Responses must indicate success and match known duration within 10 seconds. Empty, malformed and failed responses remain safe misses. KuGou and plain-text fallback remain. Memory keys include recording identity, preventing exact-video results leaking between same-name videos.

Reference: https://github.com/Clash-Projects/LastWave-Native/blob/main/app/src/main/java/com/lastwave/app/data/lyrics/SimpMusicLyricsApi.kt

No provider guarantees every recording. Titles without song links do not establish the exact failing video/uploader metadata. Manual matching, overrides and export remain available.

## v1.10.0 verification — October 12, 2026

- Signed v1.10.0 APK (code 28) assembled successfully with the existing release certificate. APK v2 signature and 16 KiB alignment verified; all seven widget receivers and the update FileProvider are packaged.

- Old layouts reproduced three regression failures; the corrected eight layouts pass all five layout regression checks, including the RemoteViews supported-class allowlist.
- The draft-release regression failed before the validation change and passes now. All 164 unit tests pass; lint reports zero errors, 87 warnings and one hint.
- Source-derived HTML previews pass 64 width/height/font combinations at the declared minimum sizes after correcting two overflowing footprints. Fixture artwork and text are used; these are not Android launcher screenshots.
- A read-only live GitHub check confirmed the public v1.8.2 release has both the APK and its matching checksum. No newer release was published for testing the installation path.
- Physical launcher, TalkBack, update download/installation and return from unknown-source settings are unverified: the user declined connecting a phone. No new dependency or permission was added.

## Baseline verification — v1.9.0

- Regression RED: the Flatline packaging test failed against the previous cleaner; GREEN: all 158 unit tests passed, including progress clamping and current-line lyric boundaries.
- All seven widgets, layouts and resources compiled; final lint reported zero errors, 87 warnings and one hint. Additional warnings cover modern preview/cell/description attributes ignored on older Android, static widget layout weights/baseline suggestions, a click-target wrapper and KTX suggestions.
- Live compiled-engine checks found LRCLIB synchronized lyrics for Flatline, its decorated lyric-video metadata, Wine Pon You by Doja Cat, One Of The Girls, Not Like Us and generic YouTube metadata for Not Like Us. No lyric text was printed.
- Metadata-only LRCLIB lookup confirmed synchronized entries for Tyga's 1 of 1; the user's exact recording was not identified.
- All seven widget XML layouts parsed; each contains three labelled 48 dp controls. Calculated worst bright-wallpaper white-text contrast is 4.69:1 for neutral, 5.24:1 for warm and 5.87:1 for ocean. No frame animation is added.
- SimpMusic parsing, duration checks, path validation and network-failure handling have synthetic unit coverage; a live SimpMusic result for the user's exact video was not verified.
- Local v1.9.0 APK (version code 27) assembled with the existing release certificate, verified APK signature and 16 KiB alignment. All seven widget receivers are packaged and 158 unit tests pass. The previous v1.8.2 APK is preserved.
- No connected phone, launcher, TalkBack or widget-action runtime test; installation and publication were not performed.
