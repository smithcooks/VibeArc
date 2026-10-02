package com.vibearc.app;

import java.net.HttpURLConnection;
import java.net.URI;

/** Opt-in live smoke check; never logs signed stream URLs or account credentials. */
public final class PlaybackProbe {
    public static void main(String[] args) throws Exception {
        for (String source : args) {
            Track track = new Track("", "YouTube Music", "", source, false, 0, "", "", "");
            for (int attempt = 0; attempt < 2; attempt++) {
                long started = System.nanoTime();
                Track resolved = OnlineMusic.INSTANCE.resolve(track, AudioFormat.valueOf(System.getProperty("probeFormat", "ANY")), AudioQuality.HIGHEST);
                long resolvedAt = System.nanoTime();
                HttpURLConnection connection = (HttpURLConnection) URI.create(resolved.getUri()).toURL().openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                if (Boolean.parseBoolean(System.getProperty("probeRange", "true"))) {
                    connection.setRequestProperty("Range", System.getProperty("probeRangeHeader",
                            PlaybackStateKt.playbackRequestHeaders(source, java.util.Collections.emptyMap()).get("Range")));
                }
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36");
                try {
                    int status = connection.getResponseCode();
                    byte[] prefix = status < 400 ? connection.getInputStream().readNBytes(65536) : new byte[0];
                    int bytes = prefix.length;
                    System.out.printf("attempt=%d resolveMs=%d fetchMs=%d status=%d mime=%s bytes=%d%n",
                            attempt, (resolvedAt - started) / 1000000, (System.nanoTime() - resolvedAt) / 1000000,
                            status, connection.getContentType(), bytes);
                    if (status >= 400 || bytes == 0) throw new IllegalStateException("Audio fetch failed: HTTP " + status);
                    if (connection.getContentType().contains("webm") &&
                            (bytes < 4 || prefix[0] != 0x1a || prefix[1] != 0x45 || prefix[2] != (byte) 0xdf || prefix[3] != (byte) 0xa3)) {
                        throw new IllegalStateException("Response is not a WebM audio container");
                    }
                } finally {
                    connection.disconnect();
                }
            }
        }
    }
}
