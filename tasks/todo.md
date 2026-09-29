# VibeArc online-provider tasks

## Phase 1: release and license gate

- [ ] Decide whether VibeArc will be GPLv3-compatible and publish corresponding source.
- [ ] Confirm v0.7 is an experimental GitHub build, not a Google Play production release.

## Phase 2: provider foundation

- [x] Add direct public, unauthenticated InnerTube song search and metadata.
- [x] Add a fixture test for the response-to-track mapping.
- [x] Fall back to NewPipe search when the direct response changes or fails.

### Checkpoint

- [x] Provider unit tests pass.

## Phase 3: playback and fallback

- [x] Add pinned NewPipeExtractor v0.26.5 and API 26 core-library desugaring.
- [x] Resolve public playback URLs only when a user presses play.
- [x] Use NewPipeExtractor as the secondary search and playback resolver.
- [x] Connect remote tracks to Media3 without persisting or downloading media URLs.

### Checkpoint

- [x] A live public search-to-audio smoke test passes.
- [x] Unsupported content fails with a clear in-app message.
- [x] Local-file playback still passes its existing tests.

## Phase 4: user experience and reliability

- [x] Add source labeling and provider-specific loading, empty, and error states.
- [ ] Add timeouts, cancellation, expiring-URL refresh, and minimal provider diagnostics.
- [ ] Test offline, slow-network, removed-content, region-blocked, and fallback flows.

## Release gate

- [ ] Publish privacy, copyright, attribution, and GPL notices.
- [ ] Keep offline downloads, DRM/ad bypasses, geographic bypasses, and credential capture out of scope.
- [ ] Do not call the provider stack production-ready or ship it to Google Play without written permission or a sanctioned replacement.

## v0.9 connected library

### Slice 1: safe account continuity

- [x] Bump the app to v0.9 and keep v0.8 release files untouched.
- [x] Persist the connected YouTube account summary without persisting OAuth access tokens.
- [x] Reacquire an authorized token through Google Play services on launch.
- [x] Add disconnect and account switching; clear cached account data and sync selections on disconnect.
- [x] Persist per-playlist sync selections and expose them in Settings.

### Checkpoint

- [x] Account/session and selection unit tests pass.
- [x] Debug APK builds; cached state contains only account display data and selected playlist IDs.

### Slice 2: playlist synchronization

- [x] Add a read-only local/remote playlist diff; baseline-backed conflict states remain before remote writes.
- [x] Add a non-destructive foreground pull for the selected YouTube playlists.
- [x] Add opt-in official YouTube playlist/item writes using `youtube.force-ssl`.
- [x] Confirm destructive remote deletes and never silently overwrite conflicts.
- [ ] Add bounded foreground sync, cancellation, timeouts, and useful failure messages.
- [ ] Add scheduled sync only after foreground sync is proven reliable.

#### Safe-write acceptance

- [x] Load playlist-item IDs required for official delete operations.
- [x] Preview remote additions/removals before applying a two-way sync.
- [x] Require an explicit second confirmation for remote removals.
- [x] Never send a remote write from automatic/background sync.
- [x] Cover sync planning and playlist-item parsing with focused unit tests.

### Slice 3: Last.fm

- [x] Load the Last.fm API key from local/CI configuration, never source control.
- [x] Add public profile, listening statistics, top tracks, and recent tracks.
- [x] Add Last.fm track-radio recommendations to Discover.
- [ ] Choose a server-side signing design before browser auth, Now Playing, or scrobbling; never embed the shared secret or collect a password.

### Slice 4: compliant downloads and quality

- [x] Add SAF folder selection, progress, cancellation, retry, cleanup, and storage reporting for user-owned local audio.
- [x] Export legitimately retrieved synchronized lyrics as `.lrc` through the system file picker.
- [x] Report the selected stream's available codec, bitrate, sample rate, and channels; do not advertise unavailable lossless tiers.
- [ ] Keep YouTube media downloading/audio extraction excluded without written approval.

### Slice 5: playback and discovery

- [ ] Add crossfade and built-in EQ only where Media3/device support is measurable and stable.
- [x] Add enhanced-LRC word timing when provider data contains word timestamps; retain line timing fallback.
- [ ] Build recommendations, radio, releases, and generator from connected official data sources.
- [ ] Keep bit-perfect, Hi-Res, lossless, and Studio Master as capability reporting—not guarantees.

### Slice 6: release

- [x] Add automatic HTTPS update metadata checks with a fixed, validated GitHub release URL.
- [x] Add privacy, provider-attribution, copyright-boundary, and GPL-obligation notices.
- [ ] Verify downloaded artifacts before install; requires stable production signing and release hashes.
- [ ] Configure the user-owned production keystore and build the release AAB.
- [ ] Complete the security review and YouTube OAuth/API compliance process.
- [ ] Publish tested v0.9 source and artifacts to GitHub.

## v0.8 Liquid build

The 27 September screenshot UI request supersedes the Liquid HTML styling. See `docs/UI_REFERENCE_REBUILD.md` for implemented layouts, functional limits and the outstanding phone comparison.

- [x] Rebuild the app shell, browse screens, search and settings around the supplied monochrome screenshots.
- [ ] Compare rendered phone screenshots against all supplied references; check large text and budget-phone scrolling.

- [x] Apply the Liquid foundation, launcher icon, direct startup, and Player back navigation.
- [x] Pre-resolve visible online results so Play starts a ready stream immediately.
- [x] Add persisted appearance controls: AMOLED, dynamic color, accent, artwork tint, and optional glass.
- [ ] Finish responsive Home, Search, Library, Downloads, Player, and Settings layouts.
- [ ] Add wavy seek, queue UI, and synced-lyrics UI with an unavailable state.
- [ ] Add streaming/download quality controls and equalizer entry point.
- [ ] Add validated JSON backup and restore without credentials or media files.
- [ ] Add compliant account/playlist sync only through official OAuth-supported APIs.
- [ ] Verify budget-phone performance, release signing, permissions, notices, and the final APK.
