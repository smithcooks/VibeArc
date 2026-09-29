# VibeArc privacy notice

VibeArc stores the local library, playlists, appearance settings, selected
download folder, and connected-account display information on the device.
OAuth access tokens are kept only in memory and are requested again through
Google Play services; VibeArc does not store Google or Last.fm passwords.

When the user searches or plays online music, the app contacts YouTube Music
and YouTube endpoints and may use NewPipeExtractor as a bounded fallback. When
lyrics are opened, track metadata is sent to LRCLIB. When a public Last.fm
profile is connected, its username is sent to Last.fm. On launch, the app asks
GitHub for the latest VibeArc release metadata. Those services receive normal
network information such as the device IP address.

VibeArc does not include advertising or analytics SDKs. Local backups exclude
credentials and media files. Last.fm scrobbling and YouTube listening-history
submission are disabled in this build.

Before publishing VibeArc, replace this notice with publisher contact details,
the final data-retention policy, and links required by each distribution store.
