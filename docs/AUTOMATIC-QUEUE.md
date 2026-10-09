# Automatic online queue

Playing from a short online shelf starts the chosen song immediately. The playback service then appends recommendations until the queue has up to 30 songs. Existing long playlists are left intact. Local-only playback does not start an online recommendation request.

The service uses the existing YouTube Music radio, artist search and personalized Home providers. Requests run on a background-priority worker. Added entries contain title, artist and artwork metadata; audio is resolved when playback needs it rather than fetching 30 streams up front.

Manual Play next keeps its position. Existing recordings and duplicate provider entries are excluded by catalog identity. A delayed response is discarded if the user has replaced the queue, and current entries are checked again before appending. Service teardown cancels the worker and prevents late player edits.

The target is 30 songs, not repeated copies of a smaller result. If the phone is offline or providers return too few unique recordings, the existing queue keeps playing. A later song transition can retry filling a short queue. This source change does not create an endless queue or alter saved playlists.

Regression tests cover an eight-song shelf growing to 30, catalog aliases, duplicates, invalid entries, manual order, long playlists and empty results. All 138 release unit tests passed, along with lint and release optimization/profile generation. The new asynchronous provider path still needs a device test with the updated APK.
