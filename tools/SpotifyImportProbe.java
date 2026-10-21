package com.vibearc.app;

/** Opt-in smoke test: public metadata and one catalog match; no audio is fetched. */
public final class SpotifyImportProbe {
    public static void main(String[] args) {
        String input=args.length>0 ? args[0] : "https://open.spotify.com/playlist/37i9dQZEVXbMDoHDwVN2tF";
        SpotifyImportPlaylist playlist=SpotifyPlaylistImporter.INSTANCE.fetch(input);
        if(playlist.getSongs().isEmpty()) throw new IllegalStateException("Empty public playlist");
        SpotifyImportSong song=playlist.getSongs().get(0);
        java.util.List<Track> candidates=OnlineMusic.INSTANCE.search(song.getTitle()+" "+song.getArtist());
        Track matched=SpotifyPlaylistImportKt.matchSpotifySong(song,candidates);
        System.out.printf("Public playlist: %d exposed songs; first song candidates=%d; confidentMatch=%s%n",playlist.getSongs().size(),candidates.size(),matched!=null);
        if(matched==null) throw new IllegalStateException("No confident match for first exposed song");
    }
}
