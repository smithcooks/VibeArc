package com.vibearc.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun HomeScreen(
    padding: PaddingValues, recentTracks: List<Track>, currentTrack: Track?, libraryTracks: List<Track>,
    onPlay: (Track, List<Track>) -> Unit, onExplore: () -> Unit,
    onLibrary: () -> Unit, onDiscover: () -> Unit, onSearch: (String) -> Unit,
) {
    val now = LocalDateTime.now()
    val pool = remember(libraryTracks, recentTracks, currentTrack) { homeFeedTracks(libraryTracks, recentTracks, currentTrack) }
    val hero = pool.firstOrNull()
    val artists = remember(pool) { pool.groupBy { it.artist }.filterKeys { it.isNotBlank() } }
    val albums = remember(pool) { pool.filter { it.album.isNotBlank() }.distinctBy { it.artist to it.album } }
    val favorites = remember(libraryTracks) { libraryTracks.filter(Track::isFavorite) }
    val fresh = remember(pool, recentTracks) { pool.filter { candidate -> recentTracks.none { it.uri == candidate.uri } && candidate.uri != currentTrack?.uri } }
    LazyColumn(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = padding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)) {
        item {
            Text(when(now.hour) { in 0..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }, style = MaterialTheme.typography.headlineMedium)
            Text(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")), color = MutedText)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().heightIn(min = 208.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface)) {
                if(hero != null) TrackArtwork(hero, null, Modifier.matchParentSize(), sizePx = 768)
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .4f), Color(0xF21D1D1D)))))
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = Color.White.copy(alpha = .18f), shape = CircleShape) {
                        Text("✦ MADE FOR YOU", Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontWeight = FontWeight.Bold)
                    }
                    Text(if(hero == null) "Your next favorite" else "Your daily mix", style = MaterialTheme.typography.headlineMedium)
                    Text(if(hero == null) "Discover a new sound on YouTube Music" else "A mix from the music on your device", color = Color(0xFFD0D0D0))
                    Button(onClick = { if(hero != null) onPlay(hero, pool) else onExplore() }, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                        Icon(if(hero == null) Icons.Default.Add else Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp)); Text(if(hero == null) "Discover" else "Play", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeShortcut("Liked Songs", "Your collection", favorites.firstOrNull(), Modifier.weight(1f), onLibrary)
                    HomeShortcut("Playlists", "Your favorites", null, Modifier.weight(1f), onLibrary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeShortcut("Mix", "From your library", hero, Modifier.weight(1f), onDiscover)
                    HomeShortcut("Discover", "Find your sound", recentTracks.lastOrNull(), Modifier.weight(1f), onExplore)
                }
            }
        }
        item { MusicShelf("Picked for you", "From your library and listening", pool, onPlay, onExplore) }
        item {
            val related = hero?.let { artists[it.artist].orEmpty().filterNot { song -> song.uri == it.uri } }.orEmpty()
            MusicShelf("Because you listened", hero?.title?.let { "More from the artist of $it" } ?: "Find a song to start exploring",
                related, onPlay, { onSearch(hero?.artist.orEmpty()) })
        }
        item { MusicShelf("Fresh finds", "Tracks in your library outside your recent rotation", fresh, onPlay, onExplore) }
        item { MusicShelf("Jump back in", "From your listening history", recentTracks, onPlay, onExplore) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ShelfHeading("Mixes to explore", "Your artists, fresh combinations", if(pool.isEmpty()) null else ({
                    val mixed = pool.shuffled(); onPlay(mixed.first(), mixed)
                }), "Shuffle")
                if(artists.isEmpty()) FeedEmpty("Your artist mixes will appear as you add music.", onExplore)
                else androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(artists.keys.toList(), key = { it }) { artist ->
                        val songs = artists.getValue(artist)
                        ShelfCard(songs.first(), "$artist mix", "${songs.size} tracks from your library") { onPlay(songs.first(), songs) }
                    }
                }
            }
        }
        if(hero != null) item { ArtistSpotlight(hero, { onPlay(hero, artists[hero.artist].orEmpty()) }, { onSearch(hero.artist) }) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ShelfHeading("Artists for you", "Worth another listen")
                if(artists.isEmpty()) FeedEmpty("Find artists by searching for a song you love.", onExplore)
                else androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(artists.keys.toList(), key = { it }) { artist ->
                        Column(Modifier.width(92.dp).clickable { onSearch(artist) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            TrackArtwork(artists.getValue(artist).first(), null, Modifier.size(88.dp).clip(CircleShape), sizePx = 240)
                            Spacer(Modifier.height(10.dp))
                            Text(artist, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item { MusicShelf("Favorites to revisit", "Your liked songs", favorites, onPlay, onLibrary) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ShelfHeading("Albums for you", "Albums from your collection")
                if(albums.isEmpty()) FeedEmpty("Add music to build your album collection.", onLibrary)
                else androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(albums, key = Track::uri) { album ->
                        ShelfCard(album, album.album, album.artist) {
                            val songs = pool.filter { it.artist == album.artist && it.album == album.album }
                            onPlay(album, songs)
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ShelfHeading("New releases", "Explore fresh drops and new albums", { onSearch("new releases") }, "See all")
                FeedEmpty("Personalized releases aren't connected yet. Explore new music with search.", { onSearch("new releases") })
            }
        }
        item { Text("Your music • Your space", Modifier.fillMaxWidth().padding(vertical = 8.dp), color = MutedText, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ShelfHeading(title: String, subtitle: String, action: (() -> Unit)? = null, actionLabel: String = "Play all") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if(action != null) ReferenceSurface(Modifier.clip(CircleShape).clickable(onClick = action), CircleShape) {
            Row(Modifier.heightIn(min = 44.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if(actionLabel == "Play all") Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                else Glyph(if(actionLabel == "Shuffle") "shuffle" else "chevron", Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp)); Text(actionLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MusicShelf(title: String, subtitle: String, tracks: List<Track>, onPlay: (Track, List<Track>) -> Unit, onExplore: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShelfHeading(title, subtitle, if(tracks.isEmpty()) null else ({ onPlay(tracks.first(), tracks) }))
        if(tracks.isEmpty()) FeedEmpty("No tracks here yet. Explore your music.", onExplore)
        else androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tracks, key = Track::uri) { track ->
                ShelfCard(track, track.title, track.artist) { onPlay(track, tracks) }
            }
        }
    }
}

@Composable
private fun ShelfCard(track: Track, title: String, subtitle: String, onPlay: () -> Unit) {
    Column(Modifier.width(148.dp).clickable(onClick = onPlay)) {
        Box {
            TrackArtwork(track, null, Modifier.size(148.dp).clip(RoundedCornerShape(20.dp)), sizePx = 384)
            Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = MutedText, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FeedEmpty(message: String, onExplore: () -> Unit) {
    ReferenceSurface {
        Row(Modifier.fillMaxWidth().clickable(onClick = onExplore).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Glyph("music", Modifier.size(28.dp))
            Text(message, Modifier.weight(1f).padding(horizontal = 14.dp), color = MutedText, style = MaterialTheme.typography.bodyMedium)
            Glyph("chevron", Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ArtistSpotlight(track: Track, onPlay: () -> Unit, onArtist: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface)) {
        TrackArtwork(track, null, Modifier.matchParentSize(), sizePx = 768)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xD9222222), Color(0xFC222222)))))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) {
                Text("✦ ARTIST SPOTLIGHT", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackArtwork(track, null, Modifier.size(72.dp).clip(CircleShape), sizePx = 240)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(track.artist, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(track.title, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onPlay, Modifier.weight(1f).heightIn(min = 46.dp)) { Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Artist") }
                OutlinedButton(onArtist, Modifier.weight(1f).heightIn(min = 46.dp)) { Text("View artist") }
            }
        }
    }
}

@Composable
private fun HomeShortcut(title:String,subtitle:String,track:Track?,modifier:Modifier,onClick:()->Unit) {
    ReferenceSurface(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp),verticalAlignment=Alignment.CenterVertically) {
            if(track!=null) TrackArtwork(track,null,Modifier.size(64.dp))
            else Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center) {
                Icon(Icons.Default.Favorite,null)
            }
            Column(Modifier.weight(1f).padding(horizontal=10.dp)) {
                Text(title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Bold)
                Text(subtitle,maxLines=1,overflow=TextOverflow.Ellipsis,color=MutedText,style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun StatsScreen(
    padding:PaddingValues, tracks:List<Track>, recent:List<Track>, lastFm:LastFmSnapshot?,
    lastFmBusy:Boolean, lastFmError:String?, onLastFmRefresh:()->Unit,
    onPlay:(Track)->Unit, onSearch:(String)->Unit,
) {
    var reverse by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(top=padding.calculateTopPadding()),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=16.dp,bottom=padding.calculateBottomPadding()+16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(lastFmBusy) item {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=3.dp); Text("Loading Last.fm…",color=MutedText)
            }
        }
        if(lastFmError != null) item {
            ReferenceSurface {
                Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(lastFmError,Modifier.weight(1f),color=MaterialTheme.colorScheme.error)
                    TextButton(onClick=onLastFmRefresh) { Text("Retry") }
                }
            }
        }
        if(lastFm != null) {
            item { Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface) { Text("Last.fm · ${lastFm.profile.username}",Modifier.padding(horizontal=14.dp,vertical=8.dp),fontWeight=FontWeight.Bold) } }
            item {
                ReferenceSurface {
                    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(24.dp)) {
                            Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Text(lastFm.profile.playCount.toString(),style=MaterialTheme.typography.displaySmall)
                                Text("Scrobbles",fontWeight=FontWeight.Bold)
                            }
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            listOf(lastFm.profile.trackCount to "Tracks",lastFm.profile.artistCount to "Artists",lastFm.profile.albumCount to "Albums").forEach { (count,label) ->
                                ReferenceSurface(Modifier.weight(1f),shape=RoundedCornerShape(18.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                        Text(count.toString(),fontWeight=FontWeight.Bold); Text(label,style=MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(lastFm.topTracks.isNotEmpty()) {
                item { SectionTitle("Top tracks on Last.fm") }
                items(lastFm.topTracks) { track ->
                    ReferenceRow(track.title,"${track.artist} · ${track.playCount} plays","stats",onClick={onSearch("${track.artist} ${track.title}")})
                }
            }
            if(lastFm.recentTracks.isNotEmpty()) {
                item { SectionTitle("Recent on Last.fm") }
                items(lastFm.recentTracks) { track ->
                    ReferenceRow(track.title,if(track.nowPlaying) "${track.artist} · Now playing" else track.artist,"clock",onClick={onSearch("${track.artist} ${track.title}")})
                }
            }
            item { SectionTitle("On this device") }
        }
        item {
            Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface) { Text("On this device",Modifier.padding(horizontal=14.dp,vertical=8.dp),fontWeight=FontWeight.Bold) }
        }
        item {
            ReferenceSurface {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(24.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(recent.size.toString(),style=MaterialTheme.typography.displaySmall)
                            Text("Recently played",fontWeight=FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(tracks.size to "Tracks",tracks.map{it.artist}.distinct().size to "Artists",tracks.map{it.album}.distinct().size to "Albums").forEach { (count,label) ->
                            ReferenceSurface(Modifier.weight(1f),shape=RoundedCornerShape(18.dp)) {
                                Column(Modifier.fillMaxWidth().padding(vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                    Text(count.toString(),fontWeight=FontWeight.Bold); Text(label,style=MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("List",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.weight(1f))
                TextButton(onClick={reverse=!reverse}) { Glyph("clock"); Spacer(Modifier.width(8.dp)); Text(if(reverse) "Oldest" else "Recent") }
            }
        }
        if(recent.isEmpty()) item {
            ReferenceSurface { Box(Modifier.fillMaxWidth().height(240.dp),contentAlignment=Alignment.Center) { Text("No tracks yet",color=MutedText) } }
        }
        items(if(reverse) recent.reversed() else recent,key=Track::uri) { TrackRow(it,{onPlay(it)}) }
    }
}

@Composable
internal fun LibraryScreen(
    padding:PaddingValues, tracks:List<Track>, playlists:List<Playlist>, onChooseFile:()->Unit, onGenerator:()->Unit,
    onPlay:(Track,List<Track>)->Unit, onToggleFavorite:(Track)->Unit, onCreatePlaylist:(String)->Unit,
    onRenamePlaylist:(String,String)->Unit, onDeletePlaylist:(String)->Unit,
    onAddToPlaylist:(String,String)->Unit, onRemoveFromPlaylist:(String,String)->Unit,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf("Newest first") }
    var sortOpen by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<Playlist?>(null) }
    var delete by remember { mutableStateOf<Playlist?>(null) }
    var menu by remember { mutableStateOf<String?>(null) }
    var addTrack by remember { mutableStateOf<Track?>(null) }
    var browseBy by remember { mutableStateOf("Tracks") }
    var group by remember { mutableStateOf<String?>(null) }
    val playlist=playlists.firstOrNull { it.id==selectedId }
    val selected = when(selectedId) { "__liked" -> tracks.filter(Track::isFavorite); "__all" -> tracks; else -> playlist?.trackUris.orEmpty().mapNotNull { uri->tracks.firstOrNull {it.uri==uri} } }
    val grouped = when(browseBy) { "Artists" -> selected.groupBy {it.artist}; "Albums" -> selected.groupBy{it.album}; "Folders" -> selected.groupBy{it.folder}; else -> emptyMap() }
    BackHandler(selectedId!=null) { if(group!=null) group=null else selectedId=null }
    Column(Modifier.fillMaxSize()) {
        ReferenceHeader(
            title=when(selectedId) { null->"Playlist"; "__liked"->"Liked Songs"; "__all"->"My Library"; else->playlist?.name.orEmpty() },
            subtitle=if(selectedId==null) "${playlists.size+1} Playlists" else "${selected.size} tracks",
            onBack=if(selectedId!=null) ({ if(group!=null) group=null else selectedId=null }) else null,
        ) {
            if(selectedId==null) {
                IconButton(onClick={create=true}) { Icon(Icons.Default.Add,"Create playlist") }
                Box {
                    TextButton(onClick={sortOpen=true}) { Glyph("sort",Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Sort",fontWeight=FontWeight.Bold) }
                    DropdownMenu(sortOpen,{sortOpen=false}) {
                        listOf("Newest first","Oldest first","Name","Track count").forEach { option ->
                            DropdownMenuItem(text={Text(option,fontWeight=FontWeight.Bold)},onClick={sort=option;sortOpen=false})
                        }
                    }
                }
            } else IconButton(onClick=onChooseFile) { Icon(Icons.Default.Add,"Add audio file") }
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=12.dp,bottom=padding.calculateBottomPadding()+72.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                if(selectedId==null) {
                    item { PlaylistTile("Liked Songs","${tracks.count(Track::isFavorite)} tracks",tracks.firstOrNull(Track::isFavorite),0,playlists.size+2,{selectedId="__liked"}) {} }
                    val ordered=when(sort) { "Name"->playlists.sortedBy{it.name.lowercase()}; "Track count"->playlists.sortedByDescending{it.trackUris.size}; "Oldest first"->playlists; else->playlists.reversed() }
                    items(ordered,key=Playlist::id) { p ->
                        PlaylistTile(p.name,"${p.trackUris.size} tracks",tracks.firstOrNull{it.uri in p.trackUris},1,3,{selectedId=p.id}) {
                            Box {
                                IconButton(onClick={menu=p.id}) { Glyph("more") }
                                DropdownMenu(menu==p.id,{menu=null}) {
                                    DropdownMenuItem(text={Text("Rename")},onClick={menu=null;rename=p})
                                    DropdownMenuItem(text={Text("Delete")},onClick={menu=null;delete=p})
                                }
                            }
                        }
                    }
                    item { PlaylistTile("My Library","${tracks.size} tracks · On this device",tracks.firstOrNull(),1,2,{selectedId="__all"}) {} }
                } else {
                    if(selectedId=="__all") item {
                        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            listOf("Tracks","Artists","Albums","Folders").forEach { label ->
                                FilterChip(browseBy==label,{browseBy=label;group=null},label={Text(label)})
                            }
                        }
                    }
                    if(grouped.isNotEmpty() && group==null) {
                        items(grouped.keys.sorted()) { key ->
                            ReferenceRow(key,"${grouped[key]?.size} tracks","album",onClick={group=key})
                        }
                    } else {
                        val visible=if(group!=null) grouped[group].orEmpty() else selected
                        if(visible.isNotEmpty()) item {
                            TextButton(onClick={onPlay(visible.first(),visible)}) { Icon(Icons.Default.PlayArrow,null); Text("Play all") }
                        }
                        if(visible.isEmpty()) item {
                            Column(Modifier.padding(24.dp)) {
                                Text("No tracks yet",style=MaterialTheme.typography.titleLarge)
                                Text("Add music to your library, then save it here.",color=MutedText)
                                TextButton(onClick=onChooseFile) { Text("Add audio file") }
                            }
                        }
                        items(visible,key=Track::uri) { track ->
                            TrackRow(track,{onPlay(track,visible)},onFavorite={onToggleFavorite(track)},isFavorite=track.isFavorite,
                                trailingIcon=if(playlist!=null) Icons.Default.Delete else Icons.AutoMirrored.Filled.List,
                                trailingDescription=if(playlist!=null) "Remove from playlist" else "Add to playlist",
                                onTrailingAction={if(playlist!=null) onRemoveFromPlaylist(playlist.id,track.uri) else addTrack=track})
                        }
                    }
                }
            }
            if(selectedId==null) FloatingActionButton(onClick=onGenerator,modifier=Modifier.align(Alignment.BottomEnd).padding(end=20.dp,bottom=padding.calculateBottomPadding()+8.dp),shape=CircleShape) { Glyph("spark") }
        }
    }
    if(create) PlaylistNameDialog("Create playlist","",{create=false},{onCreatePlaylist(it);create=false})
    rename?.let { p -> PlaylistNameDialog("Rename playlist",p.name,{rename=null},{onRenamePlaylist(p.id,it);rename=null}) }
    delete?.let { p ->
        AlertDialog(onDismissRequest={delete=null},title={Text("Delete ${p.name}?")},text={Text("Your audio files will be kept.")},
            confirmButton={TextButton(onClick={onDeletePlaylist(p.id);delete=null}){Text("Delete")}},
            dismissButton={TextButton(onClick={delete=null}){Text("Cancel")}})
    }
    addTrack?.let { track ->
        AlertDialog(onDismissRequest={addTrack=null},title={Text("Add to playlist")},
            text={Column { if(playlists.isEmpty()) Text("Create a playlist first.") else playlists.forEach { p ->
                TextButton(onClick={onAddToPlaylist(p.id,track.uri);addTrack=null}) { Text(p.name) }
            } }},
            confirmButton={TextButton(onClick={addTrack=null}){Text("Done")}})
    }
}

@Composable
private fun PlaylistTile(title:String,subtitle:String,track:Track?,index:Int,count:Int,onClick:()->Unit,actions:@Composable ()->Unit) {
    ReferenceSurface(Modifier.fillMaxWidth().clickable(onClick=onClick),groupShape(index,count)) {
        Row(Modifier.fillMaxWidth().padding(14.dp).heightIn(min=60.dp),verticalAlignment=Alignment.CenterVertically) {
            if(track!=null) TrackArtwork(track,null,Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)))
            else Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(14.dp)) {
                Box(Modifier.size(60.dp),contentAlignment=Alignment.Center) { Glyph("music") }
            }
            Column(Modifier.weight(1f).padding(horizontal=14.dp)) {
                Text(title,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(subtitle,color=MutedText,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

@Composable
internal fun DiscoverScreen(
    padding:PaddingValues, tracks:List<Track>, lastFmSeed:LastFmTrack?,
    lastFmRecommendations:List<LastFmTrack>, lastFmBusy:Boolean,
    onPlay:(Track)->Unit, onSearch:(String)->Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        if(lastFmSeed != null) {
            item { SectionTitle("Last.fm radio · ${lastFmSeed.artist}") }
            if(lastFmBusy) item { CircularProgressIndicator(Modifier.padding(18.dp).size(24.dp),strokeWidth=3.dp) }
            items(lastFmRecommendations) { track ->
                ReferenceRow(track.title,track.artist,"spark",onClick={onSearch("${track.artist} ${track.title}")})
            }
            item { SectionTitle("Rediscover your library") }
        } else item { Text("Rediscover your library",color=MutedText,modifier=Modifier.padding(bottom=12.dp)) }
        if(tracks.isEmpty()) item { ReferenceRow("Explore music","Search genres and moods on YouTube Music","discover",onClick={onSearch("")}) }
        items(tracks,key=Track::uri) { TrackRow(it,{onPlay(it)}) }
        item { TextButton(onClick={onSearch("")}) { Text("Find more music") } }
    }
}

@Composable
internal fun GeneratorScreen(padding:PaddingValues,onSearch:(String)->Unit) {
    val options=listOf(
        Triple("Top Tracks","Explore popular music","stats"),
        Triple("Recent Tracks","Find new releases","clock"),
        Triple("Song Radio","Search for songs to start a mix","music"),
        Triple("Similar Artists","Discover artists you might enjoy","account"),
        Triple("By Tag / Genre","Explore genres and moods","discover"),
        Triple("My Mix","Find music for your mood","shuffle"),
        Triple("My Recommendation","Explore new sounds","spark"),
        Triple("My Library","Search your saved music","playlist"),
    )
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        item { Text("Choose a starting point to discover music",color=MutedText,modifier=Modifier.padding(bottom=12.dp)) }
        items(options.size) { i ->
            val (title,subtitle,icon)=options[i]
            ReferenceRow(title,subtitle,icon,i,options.size,onClick={onSearch(when(i){0->"popular songs";1->"new releases";else->""})})
        }
    }
}
