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
