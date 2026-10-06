package com.vibearc.app;

import java.net.HttpURLConnection;
import java.net.URL;

/** Explicitly opt-in live diagnostic; never runs as part of the unit-test suite. */
public final class LyricsProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !"--live".equals(args[0])) {
            System.out.println("Skipped: pass --live to contact the real lyrics providers.");
            return;
        }
        probe("one-of-the-girls", track("One Of The Girls", "The Weeknd", 244_000));
        probe("not-like-us", track("Not Like Us", "Kendrick Lamar", 274_000));
        probe("generic-youtube-artist", track("Not Like Us", "YouTube Music", 274_000));
    }

    private static Track track(String title, String artist, long durationMs) {
        return new Track(title, artist, "YouTube Music", "https://music.youtube.com/watch?v=diagnostic",
                false, durationMs, "", "YouTube Music", "");
    }

    private static void probe(String name, Track track) {
        long started = System.nanoTime();
        try {
            LyricsDocument result = LyricsProvider.INSTANCE.fetch(track, track.getTitle(), track.getArtist());
            boolean hasLyrics = result != null && (result.getInstrumental()
                    || !result.getSyncedLines().isEmpty() || !result.getPlainLines().isEmpty());
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            System.out.printf("%s elapsedMs=%d hasLyrics=%s synchronized=%s provider=%s%n",
                    name, elapsedMs, hasLyrics,
                    result != null && !result.getSyncedLines().isEmpty(),
                    result == null ? "none" : result.getSource());
            if (!hasLyrics) tlsDiagnostic(name);
        } catch (Throwable error) {
            System.out.printf("%s elapsedMs=%d failure=%s%n", name,
                    (System.nanoTime() - started) / 1_000_000, error.getClass().getSimpleName());
            tlsDiagnostic(name);
        }
    }

    // Does not print lyrics, query parameters, credentials, or response bodies.
    private static void tlsDiagnostic(String name) {
        HttpURLConnection connection = null;
        long started = System.nanoTime();
        try {
            connection = (HttpURLConnection) new URL("https://lrclib.net/api/get?track_name=Not+Like+Us&artist_name=Kendrick+Lamar").openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(8_000);
            connection.setReadTimeout(12_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "VibeArc/lyrics-diagnostic (Android music player)");
            System.out.printf("%s lrclibHttp=%d elapsedMs=%d%n", name, connection.getResponseCode(),
                    (System.nanoTime() - started) / 1_000_000);
        } catch (Exception error) {
            System.out.printf("%s lrclibHttpFailure=%s elapsedMs=%d%n", name,
                    error.getClass().getSimpleName(), (System.nanoTime() - started) / 1_000_000);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
