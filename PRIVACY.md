# VibeArc privacy notice

VibeArc stores the local library, playlists, lyrics edits and offsets, playback
history, appearance/audio settings, selected download folder, and connected-
account information on the device. A YouTube Music login remains in Android's
WebView cookie store until the user disconnects. A Last.fm session key is kept
in private app storage. VibeArc does not store account passwords, disables
Android cloud backup, and excludes account credentials from JSON backups.

When the user searches or plays online music, the app contacts YouTube Music
and YouTube endpoints and may use NewPipeExtractor as a bounded fallback. When
lyrics are opened, track metadata may be sent to KuGou, LRCLIB, or Lyrics.ovh.
When Last.fm is connected, the username and playback metadata are sent to
Last.fm through the configured VibeArc signer for profile data, Now Playing,
radio, and scrobbling. On launch, the app asks GitHub for release metadata; an
accepted update downloads its APK and checksum from GitHub. These services
receive normal network information such as the device IP address.

VibeArc does not include advertising or analytics SDKs. Files exported to a
user-selected shared folder can be read by other apps with access to that
folder. YouTube listening-history submission is not implemented in this build.

Before publishing VibeArc, replace this notice with publisher contact details,
the final data-retention policy, and links required by each distribution store.
