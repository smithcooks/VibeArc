package com.vibearc.app;

/** Opt-in public-provider check; never prints signed audio URLs or account state. */
public final class CatalogProbe {
    public static void main(String[] args) throws Exception {
        CatalogItem artist = OnlineMusic.INSTANCE.searchCatalog("The Weeknd", CatalogKind.Artist).stream()
                .filter(item -> item.getTitle().equalsIgnoreCase("The Weeknd")).findFirst().orElseThrow();
        CatalogPage page = OnlineMusic.INSTANCE.browseCatalog(artist);
        if (page.getSongs().isEmpty()) throw new IllegalStateException("Artist browse returned no songs");
        System.out.printf("Artist: %s; songs=%d; releases=%d%n", artist.getTitle(), page.getSongs().size(), page.getAlbums().size());
        CatalogItem album = OnlineMusic.INSTANCE.searchCatalog("The Weeknd After Hours", CatalogKind.Album).stream().findFirst().orElseThrow();
        CatalogPage albumPage = OnlineMusic.INSTANCE.browseCatalog(album);
        if (albumPage.getSongs().isEmpty()) throw new IllegalStateException("Album browse returned no songs");
        System.out.printf("Album: %s; songs=%d%n", album.getTitle(), albumPage.getSongs().size());
        AudioCandidate audio = OnlineMusic.INSTANCE.audioCandidate(page.getSongs().get(0), AudioFormat.ANY, AudioQuality.HIGHEST);
        System.out.printf("Highest available source: %s; bitrate=%d kbps; sampleRate=%d Hz%n",
                audio.getFormat(), audio.getBitrate(), audio.getSampleRate());
    }
}
