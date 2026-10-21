# VibeArc privacy notice

VibeArc stores the local library, playlists, lyrics edits and offsets, playback
history, appearance/audio settings, selected download folder, and connected-
account information on the device. A YouTube Music login remains in Android's
WebView cookie store until the user disconnects. Last.fm integration is not
included. VibeArc does not store account passwords, disables
Android cloud backup, and excludes account credentials from JSON backups.

When the user searches or plays online music, the app contacts YouTube Music
and YouTube endpoints and may use NewPipeExtractor as a bounded fallback. When
lyrics are opened or a lyrics widget is placed, track metadata may be sent to
KuGou, LRCLIB, or Lyrics.ovh.
If metadata lookup finds no match, the song's YouTube video ID and normal
network information may be sent to the SimpMusic lyrics database. Home-screen
widgets reuse artwork and playback metadata; they do not read your wallpaper.
When you choose Import from Spotify, VibeArc reads public playlist metadata
from Spotify and searches YouTube Music using song titles and artists. It does
not request Spotify passwords, account tokens or audio. Imports are saved only
after your confirmation. Public previews may expose only part of a playlist.
On launch, the app asks GitHub for release metadata; an
accepted update downloads its APK and checksum from GitHub. These services
receive normal network information such as the device IP address.

VibeArc does not include advertising or analytics SDKs. Files exported to a
user-selected shared folder can be read by other apps with access to that
folder. YouTube listening-history submission is not implemented in this build.

Local data remains until you remove it through the app or uninstall/clear app
storage. Exported audio and JSON backups remain in the chosen folder until you
delete them. Disconnecting an account removes VibeArc's connected session;
external services manage their own account data and retention independently.

For VibeArc questions, use https://github.com/smithcooks/VibeArc/issues.
Do not post account credentials or other private information in public issues.
This GitHub sideload release does not imply distribution-store privacy approval.
