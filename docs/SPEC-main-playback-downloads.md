# Main-app download migration

Promote the playback downloader confirmed on the user's phone; remove the native extension engine and its controls from the shipped app. Keep the existing jobs JSON and owned files unchanged. No GitHub publication is authorized.

Visual contract: compact, rounded VibeArc-themed download library matching the supplied reference. Header statistics, Songs/Artists/Albums tabs, search/sort, honest format/lyrics filters, storage, playback controls, and compact rows. Use native menus/ripples; no extra decorative animation. Existing theme tokens and system font scaling remain active.

Network contract: mobile data and Wi-Fi are allowed by default. A new persistent Wi-Fi-only preference in Settings replaces the old implicit Wi-Fi-only default. Network changes pause transfers safely; a permitted connection resumes the queued job.

Verification: regression tests for main-app eligibility, network policy, download filtering, and legacy metadata; release lint, signed APK/package inspection, absence of native engine/JNA. Physical-device layout and mobile-network testing remain with the user.
