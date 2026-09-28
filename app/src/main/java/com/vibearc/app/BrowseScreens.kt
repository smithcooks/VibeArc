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
    padding: PaddingValues, recentTracks: List<Track>, currentTrack: Track?,
    onPlay: (Track) -> Unit, onExplore: () -> Unit, onOpenPlayer: () -> Unit,
    onLibrary: () -> Unit, onDiscover: () -> Unit,
) {
    val now = LocalDateTime.now()
    val hero = currentTrack ?: recentTracks.firstOrNull()
    LazyColumn(Modifier.fillMaxSize().padding(top=padding.calculateTopPadding()), contentPadding=PaddingValues(start=16.dp,end=16.dp,top=16.dp,bottom=padding.calculateBottomPadding()+16.dp), verticalArrangement=Arrangement.spacedBy(24.dp)) {
        item {
            Text(when(now.hour) { in 0..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }, style=MaterialTheme.typography.headlineMedium)
            Text(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")), color=MutedText)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().heightIn(min=208.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface)) {
                if(hero!=null) TrackArtwork(hero,null,Modifier.matchParentSize(),sizePx=768)
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.4f),Color(0xF21D1D1D)))))
                Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Surface(color=Color.White.copy(alpha=.18f),shape=CircleShape) {
                        Text("✦ MADE FOR YOU",Modifier.padding(horizontal=12.dp,vertical=5.dp),fontWeight=FontWeight.Bold)
                    }
                    Text(if(hero==null) "Your next favorite" else "Your daily mix",style=MaterialTheme.typography.headlineMedium)
                    Text(if(hero==null) "Discover a new sound on YouTube Music" else "Rediscover the music you've been listening to",color=Color(0xFFD0D0D0))
                    Button(onClick={ if(hero!=null) onPlay(hero) else onExplore() },contentPadding=PaddingValues(horizontal=24.dp,vertical=12.dp)) {
                        Icon(if(hero==null) Icons.Default.Add else Icons.Default.PlayArrow,null)
                        Spacer(Modifier.width(8.dp)); Text(if(hero==null) "Discover" else "Play",fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    HomeShortcut("Liked Songs","Your collection",null,Modifier.weight(1f),onLibrary)
                    HomeShortcut("Playlists","Your favorites",null,Modifier.weight(1f),onLibrary)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    HomeShortcut("Mix","Made for you",hero,Modifier.weight(1f),onDiscover)
                    HomeShortcut("Discover","Find your sound",recentTracks.lastOrNull(),Modifier.weight(1f),onExplore)
                }
            }
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Picked for you",style=MaterialTheme.typography.titleLarge)
                    Text("From your listening",color=MutedText)
                }
                if(recentTracks.isNotEmpty()) {
                    RoundAction("Shuffle recent tracks",{ onPlay(recentTracks.random()) }) { Glyph("shuffle") }
                    TextButton(onClick={ onPlay(recentTracks.first()) }) { Icon(Icons.Default.PlayArrow,null); Text("Play all") }
                }
            }
        }
        if(recentTracks.isEmpty()) item {
            Text("Play your first song to start your collection.",color=MutedText)
            TextButton(onClick=onExplore) { Text("Search music") }
        }
        items(recentTracks,key=Track::uri) { track -> TrackRow(track,{ onPlay(track) }) }
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
internal fun StatsScreen(padding:PaddingValues,tracks:List<Track>,recent:List<Track>,onPlay:(Track)->Unit) {
    var reverse by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(top=padding.calculateTopPadding()),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=16.dp,bottom=padding.calculateBottomPadding()+16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
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
internal fun DiscoverScreen(padding:PaddingValues,tracks:List<Track>,onPlay:(Track)->Unit,onSearch:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        item { Text("Rediscover your library",color=MutedText,modifier=Modifier.padding(bottom=12.dp)) }
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
