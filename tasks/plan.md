# Implementation Plan: VibeArc v0.7 to v1.0

## Overview

Replace the planned Audius integration with an experimental YouTube music
provider. VibeArc will use a keyless InnerTube client for primary search and
metadata, a YT client profile for public playback resolution, and
NewPipeExtractor as the secondary resolver. Local files remain fully supported.

This is technically feasible for a sideloaded/open-source experiment, but it is
not a stable or policy-compliant route to a Google Play production release.

## Architecture decisions

- Keep online sources behind one small `MusicProvider` contract so local
  playback and the UI do not depend on InnerTube or NewPipe response shapes.
- Use InnerTube first; retry once through NewPipeExtractor only for supported
  public content and known extraction failures.
- Validate all provider responses and expose typed unavailable, region-blocked,
  authentication-required, and temporary-failure results.
- Resolve stream URLs only when playback starts. Do not persist expiring URLs.
- Cache metadata briefly, but never download or cache YouTube media.
- Do not accept Google credentials, cookies, passkeys, or CAPTCHA solutions.
- Do not bypass DRM, advertisements, geographic restrictions, age gates,
  membership checks, or bot challenges.

## Feasibility

| Capability | Status | Boundary |
|---|---|---|
| Search public YouTube/YouTube Music metadata | Implementable, experimental | Unofficial InnerTube responses can change without notice. |
| Show titles, artists, artwork, albums, and durations | Implementable | Validate missing or changed fields and attribute YouTube as the source. |
| Resolve and play public streams with Media3 | Technically implementable | URLs expire; extraction may fail or be blocked. Not promised as a stable service. |
| Fall back from InnerTube to NewPipeExtractor | Implementable | Use one bounded fallback, not retry loops or restriction bypasses. |
| Queues, favorites, playlists, and recent history | Implementable | Store provider IDs and metadata, not media files or expiring stream URLs. |
| Account-free playback of every track | Not implementable | Private, premium, age-gated, members-only, DRM, region-blocked, and bot-protected content cannot be guaranteed. |
| Permanent reliable access | Not implementable | InnerTube is undocumented and extractor updates routinely follow YouTube changes. |
| Offline YouTube downloads | Excluded | Conflicts with YouTube's published restrictions and the project safety boundary. |
| Ad removal, DRM bypass, or geographic bypass | Excluded | Will not be implemented. |
| Play Store-safe background YouTube audio | Not implementable with this design | YouTube policies prohibit isolated audio and background playback for API clients. |
| Closed-source distribution with NewPipeExtractor | Not compatible | NewPipeExtractor is GPLv3-or-later; distribution requires a GPL-compatible open-source release and corresponding source. |
| Production `v1.0` using this stack without approval | Blocked | Requires written YouTube permission/licensing or replacement with a sanctioned music catalog/provider. |

## Version roadmap

### v0.7 — Experimental provider foundation

- Define the provider contract and typed errors.
- Add InnerTube search and metadata for public, unauthenticated content.
- Resolve playback on demand through the YT client, with NewPipeExtractor as a
  bounded fallback.
- Add clear YouTube attribution, provider error states, and a local-files-only
  fallback when online playback fails.
- Ship only as an experimental GitHub build until licensing and policy review is
  complete.

### v0.8 — Liquid experience and personalization

- Finish the supplied Liquid design system across every screen, with a
  responsive floating navigation stack and an optional low-cost opaque mode.
- Add persisted AMOLED, dynamic-color, artwork-accent, glass, motion, density,
  typography, and accent controls without weakening accessibility defaults.
- Finish the Now Playing experience with immediate resolved playback, a wavy
  seek bar, queue access, and synced-lyrics UI when timed lyrics are available.
- Expand playback settings with honest streaming-quality reporting, download
  quality preferences, equalizer entry points, and JSON backup/restore.
- Keep account sync and downloads behind compliant provider boundaries: use
  official OAuth for accounts, never capture credentials, and never persist or
  download restricted YouTube media.
- Verify release signing, permissions, startup behavior, and performance on
  budget-class devices before publishing a v0.8 release.

### v0.9 — Connected library beta

Build this as complete vertical slices, with a usable checkpoint after each
slice instead of wiring every screen to unfinished services at once.

1. Persist only the Google account summary and playlist-sync choices. Reacquire
   short-lived OAuth tokens through Google Play services; never store raw tokens.
   Add disconnect and account switching before adding any remote writes.
2. Add per-playlist sync selection and a read-only sync preview. Add opt-in
   remote writes only through the official YouTube Data API with the
   `youtube.force-ssl` scope, destructive-change confirmation, and explicit
   local/remote conflict handling.
3. Add foreground sync first. Add scheduled sync only after foreground sync is
   reliable, cancellable, bounded, and observable.
4. Add Last.fm public statistics and recommendations when an API key is supplied
   outside source control. Browser authentication, Now Playing, and scrobbling
   require a server-side signing strategy; do not embed the Last.fm shared secret
   in the APK or collect a Last.fm password.
5. Add Storage Access Framework folder selection, progress, retry, cancellation,
   and cleanup for user-owned local files or explicitly licensed direct-download
   sources. YouTube audiovisual downloads, audio extraction, and offline copies
   remain excluded without YouTube's prior written approval.
6. Report the real format/bitrate exposed by each playable source. Do not label a
   lossy source as FLAC, lossless, Hi-Res, bit-perfect, or Studio Master.
7. Extend lyrics to word timestamps only when a licensed/provider response
   contains them; keep line-synced LRC as the honest fallback and allow `.lrc`
   export for lyrics VibeArc legitimately retrieved.
8. Add update metadata checking and verified artifacts, then complete release
   signing, AAB generation, security review, notices, and provider compliance.

The official YouTube Data API supports playlist CRUD and playlist-item CRUD. It
does not expose a supported endpoint for writing YouTube Music listening history,
so history sync stays unavailable unless Google adds one. YouTube media downloads,
isolated/background audio, and the current unofficial playback stack are not a
Play Store production path.

### v1.0 — Conditional production release

- Release as production only after the catalog/playback source has written
  permission or a compliant licensed provider replaces the experimental stack.
- If that gate is not met, keep the InnerTube/NewPipe build pre-1.0 and label it
  experimental.

## Dependency and license gate

NewPipeExtractor supports independent Android use, but its documentation says
that projects below Android API 33 require core-library desugaring. VibeArc has
`minSdk 26`, so v0.7 includes that configuration. Its GPLv3-or-later license also
means VibeArc must adopt a compatible license and publish corresponding source
before distributing this build. The dependency and build remain local until
that licensing decision is explicit.

## Primary risks

| Risk | Impact | Mitigation |
|---|---|---|
| YouTube changes internal responses or signatures | High | Provider boundary, fixtures, one fallback, rapid dependency updates. |
| Short-lived or throttled stream URLs | High | Resolve at play time; refresh once; surface failure clearly. |
| GPL obligations are missed | High | License gate before adding NewPipeExtractor. |
| Store rejection or YouTube enforcement | High | Do not call this production-ready; use a sanctioned provider for store release. |
| Unexpected provider data | Medium | Validate at the provider boundary and reject malformed results. |

## Source basis

- YouTube Terms restrict automated access, downloading, and unapproved content
  use: https://www.youtube.com/static?template=terms
- YouTube Developer Policies prohibit scraping, isolated audio, background
  playback, offline copies, ad interference, and restriction bypasses:
  https://developers.google.com/youtube/terms/developer-policies
- YouTube's policy guide repeats the download, audio separation, and background
  playback restrictions:
  https://developers.google.com/youtube/terms/developer-policies-guide
- NewPipeExtractor usage, Android desugaring requirement, supported services,
  and GPL terms:
  https://github.com/TeamNewPipe/NewPipeExtractor/blob/dev/README.md

Reviewed on 2026-09-24. This plan is an engineering assessment, not legal advice.
