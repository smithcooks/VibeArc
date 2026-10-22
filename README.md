<div align="center">

<img src="docs/icon.png" alt="VibeArc logo" width="120" height="120">

# VibeArc

**Your music, your space.**

A native Android music player with liquid glass, artwork-driven colors, synchronized lyrics, and optional YouTube Music connectivity.

<p align="center">
  <a href="https://github.com/smithcooks/VibeArc/stargazers"><img src="https://img.shields.io/github/stars/smithcooks/VibeArc?style=for-the-badge&color=A7D8C7&labelColor=242424" alt="GitHub stars"></a>
  <a href="https://github.com/smithcooks/VibeArc/forks"><img src="https://img.shields.io/github/forks/smithcooks/VibeArc?style=for-the-badge&color=F4BACB&labelColor=242424" alt="GitHub forks"></a>
  <a href="#connected-features"><img src="https://img.shields.io/badge/Client-YouTube%20Music-FF0000?style=for-the-badge&logo=youtubemusic&logoColor=white&labelColor=242424" alt="YouTube Music client"></a>
  <a href="#getting-started"><img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=242424" alt="Platform Android"></a>
</p>

<p align="center">
  <a href="https://github.com/smithcooks/VibeArc/releases/tag/v1.10.5"><img src="https://img.shields.io/badge/Release-v1.10.5-A7D8C7?style=for-the-badge&labelColor=242424" alt="Release v1.10.5"></a>
  <a href="#tech-stack--architecture"><img src="https://img.shields.io/badge/UI-Kotlin%20%2B%20Compose-C4B5FD?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=242424" alt="Kotlin and Jetpack Compose"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL%20v3-F4BACB?style=for-the-badge&labelColor=242424" alt="GPL v3 license"></a>
</p>

<p align="center">
  <a href="https://github.com/smithcooks/VibeArc/releases/download/v1.10.5/VibeArc-v1.10.5.apk"><img src="https://img.shields.io/badge/Download-Android%20APK-3548D5?style=for-the-badge&logo=android&logoColor=white" alt="Download the Android APK"></a>
  <a href="https://smithcooks.github.io/VibeArc/"><img src="https://img.shields.io/badge/Website-Explore%20VibeArc-242424?style=for-the-badge&logo=githubpages&logoColor=A7D8C7" alt="Visit the VibeArc website"></a>
  <a href="https://github.com/smithcooks/VibeArc/issues"><img src="https://img.shields.io/badge/Feedback-Report%20an%20issue-242424?style=for-the-badge&logo=github&logoColor=F4BACB" alt="Report an issue"></a>
</p>

<p align="center">
  <a href="https://buymeachai.ezee.li/Smith_cooks"><img src="https://buymeachai.ezee.li/assets/images/buymeachai-button.png" alt="Buy Me A Chai" width="200" height="49"></a>
  &nbsp;
  <a href="https://github.com/sponsors/smithcooks"><img src="https://img.shields.io/badge/Sponsor-GitHub-ea4aaa?style=for-the-badge&logo=githubsponsors&logoColor=white" alt="GitHub Sponsor" height="34"></a>
</p>

<sub>Support is optional. GitHub Sponsors is not active yet; its button currently opens Smith’s GitHub profile.</sub>

</div>

<br>

## Watch VibeArc in motion

<a href="https://smithcooks.github.io/VibeArc/#promo"><img src="docs/promo-poster.png" width="320" height="569" alt="Watch the 30-second VibeArc cinematic promo on the website"></a>

A 30-second cinematic look at the real app. [Watch in your browser](https://smithcooks.github.io/VibeArc/#promo) or [download the MP4](https://github.com/smithcooks/VibeArc/releases/download/v1.4.0/VibeArc-launch.mp4). This promo has no audio.

<br>

<div align="center">
  <a href="docs/screenshots/home.jpg"><img src="docs/screenshots/home.jpg" width="31%" alt="VibeArc Home with daily mix and floating mini-player"></a>
  <a href="docs/screenshots/player.jpg"><img src="docs/screenshots/player.jpg" width="31%" alt="VibeArc Now Playing with artwork and playback controls"></a>
  <a href="docs/screenshots/search.jpg"><img src="docs/screenshots/search.jpg" width="31%" alt="VibeArc music search results"></a>
  <br><br>
  <a href="docs/screenshots/library.jpg"><img src="docs/screenshots/library.jpg" width="31%" alt="VibeArc library and liked tracks"></a>
  <a href="docs/screenshots/discovery.jpg"><img src="docs/screenshots/discovery.jpg" width="31%" alt="VibeArc artist spotlight and recommendations"></a>
  <a href="docs/screenshots/appearance.jpg"><img src="docs/screenshots/appearance.jpg" width="31%" alt="VibeArc accent choices and liquid glass settings"></a>
  <br><br>
  <a href="docs/screenshots/accounts.jpg"><img src="docs/screenshots/accounts.jpg" width="31%" alt="VibeArc connected account and playlist settings"></a>
  <a href="docs/screenshots/about.jpg"><img src="docs/screenshots/about.jpg" width="31%" alt="VibeArc About screen showing version 0.9.0 beta"></a>
  <p><sub>Screenshots captured from VibeArc v0.9 beta. Unaltered app UI; the current download is v1.10.5. Click a screenshot to view it in full.</sub></p>
</div>

<br>

## <img src="https://api.iconify.design/lucide:sparkles.svg?color=%23A7D8C7" width="20" height="20" alt=""> Overview

**VibeArc** brings your collection and online discoveries into a native Android player. Large artwork, a floating mini-player, and a palette that follows your music keep listening at the center.

Built with **Kotlin, Jetpack Compose, Material 3, and Media3**, it gives you a personal library, queue, lyrics, and appearance controls without hiding them behind busy menus.

---

## What's new in v1.10.5

- **Full Spotify playlist import:** Open **Settings > Library & Backup > Import from Spotify**, paste a public playlist link, and review matching YouTube Music recordings. The importer now reads every page against the reported total instead of stopping at the 100-song preview. The separate test APK was user-confirmed with 317 songs. Order and repeated positions are preserved; unavailable and unmatched entries are reported. Private playlists and Liked Songs are unsupported. Re-import an old link to retrieve previously missing entries. [Import details](docs/SPOTIFY-IMPORT.md).

- **WebM saving fixed:** Fixes Android's `Unsupported MIME type audio/webm` error during single-song and playlist downloads. New WebM files on Android 10+ are saved in **Download/VibeArc**; MP3 stays in **Music/VibeArc**. Existing files and in-app lyrics are untouched.
- **MP3 is opt-in:** WebM is first and default, with no conversion. The old saved MP3 preference resets once on upgrade. Choose MP3 explicitly under **Settings → Playback & Downloads → Download format**, or convert a saved song later. New/retried jobs use the current choice; already-active/queued jobs retain their captured format.
- **Convert later:** In **Downloads → song options → Create MP3 copy**, convert an owned saved download without downloading it again. Native on-device LAME creates a 320 kbps MP3 with embedded cover art in **Music/VibeArc**. The original and in-app lyrics remain intact; conversion can be cancelled or retried.
- **No Last.fm integration:** Sign-in, remote profile/radio requests, and scrobbling have been removed. Your local library and listening statistics remain.
- **Updates inside VibeArc:** Check and download the latest stable APK from **Settings → About & Support**. Downloads verify the checksum, package, newer version, and signing certificate before Android asks you to approve installation. Optional reminders check when the app opens; they can be turned off.
- **More resilient lyrics matching:** Cleaner lyric-video metadata and a video-ID-based SimpMusic fallback complement the existing providers. Lyrics still depend on the recording and provider availability.

[Download v1.10.5](https://github.com/smithcooks/VibeArc/releases/download/v1.10.5/VibeArc-v1.10.5.apk) · [Release notes](https://github.com/smithcooks/VibeArc/releases/tag/v1.10.5) · [Widget & lyrics details](docs/WIDGETS-AND-LYRICS.md) · [Update details](docs/IN-APP-UPDATES.md)

All **194 unit tests pass**; lint reports zero errors (84 warnings). The signed main-app APK and 16 KiB alignment checks passed. The user verified the full-playlist engine in the separate test APK; the newly combined APK and Android MediaStore fix still need phone testing. Native desktop timing is not a phone-speed guarantee. Widgets use translucent surfaces and static highlights, not live wallpaper blur or refraction.

---

## <img src="https://api.iconify.design/lucide:layers.svg?color=%23A7D8C7" width="20" height="20" alt=""> Key Features

| Icon | Feature | Highlight |
| :---: | :--- | :--- |
| <img src="https://api.iconify.design/lucide:play-circle.svg?color=%23FF0000" width="20" height="20" alt=""> | **YouTube Music client** | Search tracks, artists, albums, and playlists; connect an account and import playlists through an experimental integration. |
| <img src="https://api.iconify.design/lucide:music.svg?color=%23A7D8C7" width="20" height="20" alt=""> | **Your library & queue** | Browse tracks, artists, albums, and folders. Long-press to like, queue, save to a playlist, or download. |
| <img src="https://api.iconify.design/lucide:mic.svg?color=%23F4BACB" width="20" height="20" alt=""> | **Synchronized lyrics** | Line highlighting, timed words when supplied, manual matching/editing, timing offsets, and `.lrc` export. |
| <img src="https://api.iconify.design/lucide:arrow-down-to-line.svg?color=%23A7D8C7" width="20" height="20" alt=""> | **Offline downloads** | Save playable online songs using the tested playback downloader. Browse, search, filter, and export your offline collection; mobile data is allowed by default. |
| <img src="https://api.iconify.design/lucide:compass.svg?color=%2362C7D7" width="20" height="20" alt=""> | **Discovery & radio** | Browse mixes, artist spotlights, recommendations, and radio from available provider and listening data. |
| <img src="https://api.iconify.design/lucide:palette.svg?color=%23A7D8C7" width="20" height="20" alt=""> | **Make it yours** | Artwork-driven accents, wallpaper colors, AMOLED mode, optional liquid glass, custom fonts, and three launcher icons. |
| <img src="https://api.iconify.design/lucide:headphones.svg?color=%23F4BACB" width="20" height="20" alt=""> | **Playback controls** | Background playback, lock-screen controls, shuffle, repeat, sleep timer, audio tuning, and crossfade where supported. |
| <img src="https://api.iconify.design/lucide:panels-top-left.svg?color=%23A7D8C7" width="20" height="20" alt=""> | **Home-screen widgets** | Seven glass-style designs with artwork, playback controls, compact layouts, and a synchronized lyric card. |
| <img src="https://api.iconify.design/lucide:refresh-cw.svg?color=%23F4BACB" width="20" height="20" alt=""> | **In-app updates** | Download verified stable-release APKs in Settings, with optional startup reminders and Android-approved installation. |
| <img src="https://api.iconify.design/lucide:archive.svg?color=%23A7D8C7" width="20" height="20" alt=""> | **Backup & restore** | Export and restore local data through JSON backups; account credentials are excluded. |

---

<a id="connected-features"></a>

## <img src="https://api.iconify.design/lucide:plug.svg?color=%23A7D8C7" width="20" height="20" alt=""> Connected Features

**YouTube Music** uses an unofficial, experimental in-app WebView session, not official Google OAuth. Cookies stay in the device’s WebView session. Supported playlist synchronization is manually reviewed and confirmed. Automatic background sync and YouTube playback-history submission are **not implemented**. Provider changes, regional restrictions, and verification challenges can interrupt access.

Listening history and library statistics stay on the device. Last.fm sign-in, profile/radio requests, and scrobbling are no longer included.

**Lyrics** use LRCLIB, a SimpMusic video-ID fallback, KuGou KRC, and Lyrics.ovh. Availability varies by recording; word highlighting needs word timestamps. Manual matching and offsets help when automatic results are imperfect.

<details>
<summary><strong>Downloads, audio quality, and device limitations</strong></summary>

- Download only music you own or have permission to save, and respect provider terms.
- WebM is the default and retains the original available Opus audio. Optional MP3 conversion uses native LAME on the device; 320 kbps encoding does not improve the source or create lossless/Hi-Res audio. A missing WebM source is reported, never silently renamed from AAC/MP4.
- The 15-band equalizer uses DynamicsProcessing on supported Android 9+ devices. Audio effects depend on Android and your hardware.
- Studio Master is a tuning preset, not a quality certification. Lossless sources do not guarantee bit-perfect or Hi-Res device output.
- Discovery results depend on provider data and account setup, not guaranteed complete personalization.

</details>

---

<a id="tech-stack--architecture"></a>

## <img src="https://api.iconify.design/lucide:cpu.svg?color=%23A7D8C7" width="20" height="20" alt=""> Tech Stack & Architecture

- **Language & UI:** Kotlin · Jetpack Compose · Material 3
- **Audio engine:** AndroidX Media3 / ExoPlayer
- **Online sources:** Experimental YouTube Music client · NewPipeExtractor
- **Lyrics:** LRCLIB · SimpMusic · KuGou KRC · Lyrics.ovh
- **Project layout:** Android app in `app/`, launch website in `docs/`, checks in `tools/`, implementation notes in `tasks/`

---

<a id="getting-started"></a>

## <img src="https://api.iconify.design/lucide:rocket.svg?color=%23A7D8C7" width="20" height="20" alt=""> Getting Started

1. Download [**VibeArc-v1.10.5.apk**](https://github.com/smithcooks/VibeArc/releases/download/v1.10.5/VibeArc-v1.10.5.apk) from the [official release](https://github.com/smithcooks/VibeArc/releases/tag/v1.10.5).
2. Allow installation for the browser or file manager you used, then install on **Android 8.0+ (API 26)**.
3. Add local music or explore compatible online sources.
4. Choose your accent, build your queue, and press play.

Export a JSON backup before updating. Version code **34** and the existing release certificate support updates over earlier main-app releases; do not uninstall or clear data first. Downloads in the separate test app stay in that app’s storage. Check the [APK SHA-256 checksum](https://github.com/smithcooks/VibeArc/releases/download/v1.10.5/VibeArc-v1.10.5.apk.sha256). Existing versions with the in-app updater can get this release from Settings → About & Support. Sideloading warnings may appear; VibeArc is not a Google Play listing or Play Protect certification.

**Android is available now. Windows and Linux are coming soon.**

---

## <img src="https://api.iconify.design/lucide:terminal.svg?color=%23A7D8C7" width="20" height="20" alt=""> Building from Source

Requirements: **JDK 21**, Android SDK **36**, Build Tools **36.0.0**, and the included Gradle **8.14.3** wrapper. Configure the SDK in Android Studio or an untracked `local.properties`.

```powershell
git clone https://github.com/smithcooks/VibeArc.git
cd VibeArc
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease
node --test server/lastfm-signer/worker.test.mjs
```

On macOS/Linux, use `./gradlew` instead of `.\gradlew.bat`. Release builds require your own untracked `keystore.properties` and keystore. Signing keys and credentials are not distributed. Node.js is needed for signer tests/tooling, not to build the app.

---

## <img src="https://api.iconify.design/lucide:message-circle.svg?color=%2362C7D7" width="20" height="20" alt=""> Community & Support

- **Website:** [Explore VibeArc](https://smithcooks.github.io/VibeArc/)
- **Updates:** [Releases](https://github.com/smithcooks/VibeArc/releases) · [Changelog](CHANGELOG.md)
- **Feedback:** [Report a bug or suggest an improvement](https://github.com/smithcooks/VibeArc/issues)
- **Support Smith:** [Buy Me a Chai](https://buymeachai.ezee.li/Smith_cooks) · [GitHub Sponsors](https://github.com/sponsors/smithcooks) (not active yet)

Include your phone model, Android version, reproduction steps, and a public song link if relevant. **Never share account cookies, session tokens, or API secrets.** Focused pull requests and device-testing feedback are welcome.

---

## <img src="https://api.iconify.design/lucide:shield-check.svg?color=%23F4BACB" width="20" height="20" alt=""> Privacy, License & Attribution

No advertising or analytics SDKs. Library and preferences stay locally; connected services receive requests for their features. Android cloud backup is disabled. Read the [privacy notice](PRIVACY.md), [GPL v3 license](LICENSE), and [third-party notices](THIRD_PARTY_NOTICES.md).

VibeArc’s UI and this README’s layout are inspired by [LastWave](https://github.com/Clash-Projects/LastWave-Native). VibeArc is an independent project, with its own screenshots, branding, and feature documentation.

> [!NOTE]
> VibeArc is not affiliated with or endorsed by Google, YouTube Music, Last.fm, or any other music service. Online integrations remain experimental. Use content only with the appropriate permission and follow provider terms. Album artwork in the screenshots belongs to its respective owners.

---

<div align="center">
  <p><b>VibeArc</b> is built with ❤️ by <a href="https://github.com/smithcooks">Smith</a>.</p>
</div>
