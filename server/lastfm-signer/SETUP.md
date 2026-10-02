# Last.fm signer setup

1. Create a Last.fm API application and a Cloudflare Worker account.
2. From this directory, set `LASTFM_API_KEY`, `LASTFM_SHARED_SECRET`, and a long random `VIBEARC_CLIENT_TOKEN` with `wrangler secret put`.
3. Deploy with `wrangler deploy`.
4. In VibeArc Settings → Last.fm, enter the HTTPS Worker URL and the same client token, then save setup. The public API key and username are sufficient for public profiles/statistics without scrobbling. Build-time defaults in untracked `local.properties` (`LASTFM_API_KEY`, `LASTFM_SIGNER_URL`, `LASTFM_SIGNER_TOKEN`) remain supported.
5. Tap Authorize, approve VibeArc in the browser, return to VibeArc, and tap Finish sign-in. The session persists privately on the device. Changing provider configuration or username disconnects the previous session.

The Last.fm shared secret stays in the Worker. Runtime credentials are excluded from JSON and Android backups. A build-time client token is packaged in the APK; do not embed it in APKs shared with friends. A shared client token grants access to this signer, so keep it private and rotate it if exposed.

Redeploy the updated Worker: reads/authentication use signed GET requests, writes use POST, and ignored submissions return HTTP 422 instead of false success. Run `node --test worker.test.mjs` before deployment.

Now Playing starts only during actual playback. Scrobbling must be enabled, authenticated, and not excluded; eligible tracks are submitted after half the listening duration or four minutes, whichever comes first (tracks must exceed 30 seconds). Failed scrobbles retry at most twice while the current track remains active, with one minute between attempts. This is not a persistent offline scrobble queue.
