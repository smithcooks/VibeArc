package com.vibearc.app

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

internal fun playableOnlineTrack(track: Track, resolvedTracks: Map<String, Track>): Track? =
    resolvedTracks[track.uri]

@Composable
internal fun SearchScreen(padding: PaddingValues, tracks: List<Track>, onPlay: (Track) -> Unit) {
    var query by remember { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf(emptyList<Track>()) }
    var onlineError by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val resolvedOnlineTracks = remember { mutableStateMapOf<String, Track>() }
    val resolvingOnlineUris = remember { mutableStateListOf<String>() }
    val unavailableOnlineUris = remember { mutableStateListOf<String>() }
    val resolutionPermits = remember { Semaphore(2) }
    val context = LocalContext.current
    val preferHighestQuality = context.prefersHighestAudioQuality()
    val focusManager = LocalFocusManager.current
    val localResults = tracks.filter { track ->
        query.isBlank() || listOf(track.title, track.artist, track.album).any { it.contains(query, true) }
    }
    LaunchedEffect(query) {
        val requestedQuery = query.trim()
        onlineResults = emptyList()
        onlineError = null
        hasSearched = false
        resolvedOnlineTracks.clear()
        resolvingOnlineUris.clear()
        unavailableOnlineUris.clear()
        if (requestedQuery.isBlank()) {
            searching = false
        } else {
            delay(600)
            searching = true
            hasSearched = true
            try {
                onlineResults = withContext(Dispatchers.IO) { OnlineMusic.search(requestedQuery) }
            } catch (_: Exception) {
                onlineResults = emptyList()
                onlineError = "Online search is unavailable right now. Try again later."
            } finally {
                searching = false
            }
        }
    }
    LaunchedEffect(onlineResults, preferHighestQuality) {
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
                                    OnlineMusic.resolve(track, preferHighestQuality)
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
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.large,
            ) {
                androidx.compose.foundation.layout.Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Find any track", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tracks, artists, albums") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    )
                    Text(
                        "YouTube Music searches automatically. Restricted tracks remain unavailable.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
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
        if (onlineResults.isNotEmpty()) item { SectionTitle("Online") }
        items(onlineResults, key = { "online:${it.uri}" }) { track ->
            val playableTrack = playableOnlineTrack(track, resolvedOnlineTracks)
            val unavailable = track.uri in unavailableOnlineUris
            TrackRow(
                track,
                enabled = playableTrack != null,
                onPlay = { playableTrack?.let(onPlay) },
            )
            if (unavailable) Text(
                "Unavailable on this connection",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
            )
        }
        if (localResults.isNotEmpty()) item { SectionTitle("On this device") }
        if (localResults.isEmpty() && onlineResults.isEmpty() && query.isNotBlank() && !searching) {
            item { Text("No local tracks match “$query”.", color = MutedText) }
        }
        items(localResults, key = { "local:${it.uri}" }) { track ->
            TrackRow(track, onPlay = { onPlay(track) })
        }
    }
}
