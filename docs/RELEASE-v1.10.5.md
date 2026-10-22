# VibeArc v1.10.5

Main-app release for Android 8.0+ (API 26), versionCode 34. Install over the
existing main app without uninstalling or clearing data; the signing certificate
is unchanged. The separate playlist-test app remains separate.

- Promotes the public Spotify playlist pagination engine after the user confirmed
  that the separate test APK fetched every song in a 317-song playlist.
- Fetches every page against the source total instead of using the 100-song embed
  preview. Shows retrieval/matching progress and unavailable/unmatched counts.
- A failed page or changed total fails visibly; no silent partial-list fallback.
- Public playlist metadata only: recordings are still matched to YouTube Music;
  no Spotify audio, Spotify login, developer key, or private-playlist support.
- Fixes Android's `Unsupported MIME type audio/webm` publication error. New WebM
  exports on Android 10+ use Download/VibeArc; MP3 exports remain in Music/VibeArc.
- WebM is first and default, with no conversion. Old MP3 format preferences are
  reset once; explicit new choices remain saved. Create MP3 copy remains manual.

The importer uses unofficial public web-client compatibility and can be affected
by Spotify-side restrictions or changes. A complete source list does not guarantee
that every entry will have a matching playable YouTube Music recording. Review the
matched/unmatched preview before saving. Existing imported playlists are not
automatically changed; import the link again to get previously missing entries.

Protocol reference and GPL license notices are included in the APK's main assets
and in the corresponding source archive delivered alongside the APK. The separate
playlist-test APK is unchanged.

Download from the release assets, the [official website](https://smithcooks.github.io/VibeArc/),
or **Settings > About & Support > Check for updates** in an older VibeArc version
with in-app updates. Android still requires installation approval.

Verification: 194 tests passed; Android lint reports zero errors and 84 warnings.
APK signature, package/version and 16 KiB alignment checks passed. The combined
APK and WebM MediaStore fix have not yet been tested on a physical phone here.

APK SHA-256:
`38a6cb8e6dcd7dabeb9b3fa86a782da82be497234f51991d211080dfb31aec4f`
