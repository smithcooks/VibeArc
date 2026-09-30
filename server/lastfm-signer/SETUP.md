# Last.fm signer setup

1. Create a Last.fm API application and a Cloudflare Worker account.
2. From this directory, set `LASTFM_API_KEY`, `LASTFM_SHARED_SECRET`, and a long random `VIBEARC_CLIENT_TOKEN` with `wrangler secret put`.
3. Deploy with `wrangler deploy`.
4. Put the HTTPS Worker URL in `LASTFM_SIGNER_URL` and the same client token in `LASTFM_SIGNER_TOKEN` in the app's untracked `local.properties`.

The Last.fm shared secret stays in the Worker. The client token is packaged in a personal-use APK, so revoke and replace it if that APK is shared.
