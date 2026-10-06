package com.vibearc.app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal fun visibleOnlineTracks(results: List<Track>, visibleKeys: Set<*>): List<Track> =
    results.filter { track -> "online:${track.uri}" in visibleKeys }

internal fun playableOnlineTrack(track: Track, resolvedTracks: Map<String, Track>): Track =
    resolvedTracks[track.uri] ?: track

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun SearchScreen(padding: PaddingValues, tracks: List<Track>, initialQuery: String = "", onBack: () -> Unit = {}, onPlay: (Track) -> Unit) {
    var query by remember { mutableStateOf(initialQuery) }
    var category by remember { mutableStateOf("Tracks") }
    var onlineResults by remember { mutableStateOf(emptyList<Track>()) }
    var onlineError by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val resolvedOnlineTracks = remember { mutableStateMapOf<String, Track>() }
    val resolvingOnlineUris = remember { mutableStateListOf<String>() }
    val unavailableOnlineUris = remember { mutableStateListOf<String>() }
    val resolutionPermits = audioPrefetchPermits
    val context = LocalContext.current
    val playlists = remember { context.loadPlaylists() }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val streamFormat = context.streamAudioFormat()
    val streamQuality = context.streamAudioQuality()
    val focusManager = LocalFocusManager.current
    val localResults = tracks.filter { track ->
        query.isBlank() || listOf(track.title, track.artist, track.album).any { it.contains(query, true) }
    }
    LaunchedEffect(query, category) {
        val requestedQuery = query.trim()
        onlineResults = emptyList()
        onlineError = null
        hasSearched = false
        resolvedOnlineTracks.clear()
        resolvingOnlineUris.clear()
        unavailableOnlineUris.clear()
        if (requestedQuery.isBlank() || category == "Playlists") {
            searching = false
        } else {
            delay(250)
            searching = true
            hasSearched = true
            try {
                if(!networkAllowsDownload(context,false)) onlineError="You're offline. Saved and phone music are still available in Library and Downloads."
                else onlineResults = withContext(Dispatchers.IO) { OnlineMusic.search(requestedQuery) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                onlineResults = emptyList()
                onlineError = "Online search is unavailable right now. Try again later."
            } finally {
                searching = false
            }
        }
    }
    LaunchedEffect(onlineResults, streamFormat, streamQuality) {
        snapshotFlow {
            visibleOnlineTracks(
                onlineResults,
                listState.layoutInfo.visibleItemsInfo.map { item -> item.key }.toSet(),
            )
        }.distinctUntilChanged().collect { visibleTracks ->
            visibleTracks.forEach { track ->
                if (
                    track.uri !in resolvedOnlineTracks &&
                    track.uri !in resolvingOnlineUris &&
                    track.uri !in unavailableOnlineUris
                ) {
                    resolvingOnlineUris += track.uri
                    launch {
                        try {
                            resolvedOnlineTracks[track.uri] = withContext(Dispatchers.IO) {
                                resolutionPermits.withPermit {
                                    OnlineMusic.resolve(track, streamFormat, streamQuality)
                                }
                            }
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            unavailableOnlineUris += track.uri
                        } finally {
                            resolvingOnlineUris -= track.uri
                        }
                    }
                }
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(bottomStart=24.dp,bottomEnd=24.dp)) {
            Column(Modifier.statusBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    RoundAction("Back",onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,null) }
                    TextField(
                        value=query,onValueChange={query=it},modifier=Modifier.weight(1f),
                        placeholder={Text("Search YouTube Music…",maxLines=1)},
                        leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,shape=CircleShape,
                        colors=TextFieldDefaults.colors(
                            focusedContainerColor=MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent),
                        keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
                        keyboardActions=KeyboardActions(onSearch={focusManager.clearFocus()}),
                    )
                }
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Tracks","Artists","Albums","Playlists").forEach { label ->
                        FilterChip(category==label,{category=label;selectedPlaylist=null},label={Text(label,fontWeight=FontWeight.Bold)},
                            shape=CircleShape,modifier=Modifier.height(36.dp))
                    }
                }
            }
        }
    LazyColumn(
        state=listState,modifier=Modifier.weight(1f).imePadding(),
        contentPadding=PaddingValues(start=16.dp,end=16.dp,top=20.dp,bottom=padding.calculateBottomPadding()+20.dp),
        verticalArrangement=Arrangement.spacedBy(4.dp),
    ) {
        if(query.isBlank() && category != "Playlists") {
            item {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Glyph("discover",Modifier.size(18.dp)); Text("Explore genres & moods",fontWeight=FontWeight.Bold)
                }
                Spacer(Modifier.height(18.dp))
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    listOf("Pop","Rock","Hip-Hop","Lo-Fi","Electronic","Indie","R&B","Bollywood","Jazz","Metal","Acoustic","Chill","Anime","Classical","Synthwave").forEach { genre ->
                        SuggestionChip(onClick={query=genre},label={Text(genre,fontWeight=FontWeight.Bold)},
                            shape=RoundedCornerShape(12.dp),
                            colors=SuggestionChipDefaults.suggestionChipColors(containerColor=MaterialTheme.colorScheme.surfaceVariant))
                    }
                }
            }
        }
        if (searching) item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                Text("Searching the public catalog…", color = MutedText)
            }
        }
        onlineError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        if (hasSearched && !searching && onlineResults.isEmpty() && onlineError == null) {
            item { Text("No public tracks found for “$query”.", color = MutedText) }
        }
        if(category == "Playlists") {
            val matches = playlists.filter { it.name.contains(query, true) }
            if(selectedPlaylist == null) {
                if(matches.isEmpty()) item { Text("No saved playlists match this search.", color=MutedText) }
                items(matches, key={"playlist:${it.id}"}) { playlist ->
                    ReferenceRow(playlist.name,"${playlist.trackUris.size} tracks · On this device","playlist",onClick={selectedPlaylist=playlist})
                }
            } else {
                item { TextButton(onClick={selectedPlaylist=null}) { Text("Back to playlists") } }
                val playlistTracks=selectedPlaylist!!.trackUris.mapNotNull { uri->tracks.firstOrNull { it.uri==uri } }
                items(playlistTracks, key={"playlist-track:${it.uri}"}) { track-> TrackRow(track,{onPlay(track)}) }
            }
        }
        if(category in listOf("Artists", "Albums") && query.isNotBlank()) {
            val groups=(onlineResults+localResults).groupBy { if(category=="Artists") it.artist else it.album }
            items(groups.keys.sorted(),key={ "group:$it" }) { name ->
                ReferenceRow(name,"${groups[name]?.size} matching tracks",if(category=="Artists") "account" else "album",onClick={ query=name; category="Tracks" })
            }
        }
        if(category=="Tracks") {
        if (onlineResults.isNotEmpty()) item { Text("Songs",fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=12.dp)) }
        items(onlineResults, key = { "online:${it.uri}" }) { track ->
            val playableTrack = playableOnlineTrack(track, resolvedOnlineTracks)
            val unavailable = track.uri in unavailableOnlineUris
            TrackRow(
                track,
                playableTrack = playableTrack,
                onPlay = { onPlay(playableTrack) },
            )
            if (unavailable) Text(
                "Unavailable on this connection",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
            )
        }
        if (localResults.isNotEmpty() && query.isNotBlank()) item { SectionTitle("On this device") }
        if (localResults.isEmpty() && onlineResults.isEmpty() && query.isNotBlank() && !searching) {
            item { Text("No local tracks match “$query”.", color = MutedText) }
        }
        items(if(query.isBlank()) emptyList() else localResults, key = { "local:${it.uri}" }) { track ->
            TrackRow(track, onPlay = { onPlay(track) })
        }
    }
    }
    }
}
