# In-app updates

Settings → About & Support → Check for updates checks the latest stable public release in `smithcooks/VibeArc`. A newer release opens the existing update dialog. Download update retrieves the APK inside VibeArc; Android still requires the user's installation approval and may require Allow installs from VibeArc.

Update reminders are enabled by default and can be disabled in the same settings section. They run when the app opens, not through a background polling service. Manual checks remain available with reminders disabled. Network or invalid-release errors must not be presented as an up-to-date result.

Downloads retain HTTPS/host allowlists, bounded file sizes and redirects, matching SHA-256 asset, package-name, newer version-code and signing-certificate checks. The release must include an APK and its matching `.apk.sha256` asset. Drafts and prereleases are not offered. No automatic installation or Play Protect bypass is added.

The downloader and installer already existed; this change makes them discoverable in Settings and adds reminder control. No new dependencies or permissions are needed. Native installation and unknown-source permission return still require phone testing.

Android reference: https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls()
