# Player and feed UI — 28 September 2026

The seven supplied screenshots define this UI pass. Preserve VibeArc branding, actual track data, and yesterday's budget-phone improvements.

- Now Playing: full-height artwork-tinted background, centered compact header, large rounded square art, title/artist with heart and lyrics actions, thin seek track, large circular transport controls, shuffle/audio/repeat pills.
- Queue: separate back-navigable page, queue count/current position, compact rounded rows, current-track indicator, remove action.
- Lyrics: separate page with track header, source/status chip, spacious central lyric region, optional expanded layout, persistent playback controls. No invented lyrics or source badges. Until a provider is connected, show a clear unavailable state.
- Feed: horizontal album-art shelves, jump-back-in history, artist mixes, spotlight card, circular artist tiles, favorite/album shelves. Use real local data and explain unconnected discovery/new-release sources.
- No fabricated update banner, artist portraits, bitrate, Last.fm connection, or recommendation service. Add update UI when a verified newer VibeArc version exists.
- Responsive: scroll on compact/tall-font screens; labels truncate instead of forcing controls off-screen. All player pages have visible back navigation and Android Back handling. Reuse cached, size-bounded artwork; no blur or continuously running background effects.

Verification: Kotlin/unit tests/lint plus APK packaging; user will inspect the APK manually on the Samsung M12. Pixel-level fidelity and device frame timing remain unverified until that check.
