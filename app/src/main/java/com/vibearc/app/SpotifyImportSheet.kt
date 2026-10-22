package com.vibearc.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpotifyImportSheet(onDismiss: ()->Unit, onImport: (String,List<Track>)->Unit) {
    val scope=rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<SpotifyImportPreview?>(null) }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var job by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    fun close() { job?.cancel();onDismiss() }
    ModalBottomSheet(onDismissRequest={close()},containerColor=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).navigationBarsPadding().imePadding().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item { Text("Import from Spotify",style=MaterialTheme.typography.headlineSmall) }
            item { Text("Paste a public playlist link. Songs are matched to YouTube Music; Spotify audio is not imported.",style=MaterialTheme.typography.bodyMedium) }
            if(preview==null) {
                item { OutlinedTextField(input,{input=it.take(2048);error=""},Modifier.fillMaxWidth(),label={Text("Spotify playlist link")},singleLine=true,enabled=!busy,
                    isError=input.isNotBlank() && spotifyPlaylistId(input)==null,
                    supportingText={Text(if(input.isNotBlank() && spotifyPlaylistId(input)==null) "Use an open.spotify.com playlist link or spotify:playlist: URI." else "Public playlists only. Private playlists and Liked Songs are not supported.")}) }
                item { Text("All playlist pages are checked against Spotify's reported total before matching songs. A failed page does not save a partial playlist.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if(busy) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                item { Text(status,Modifier.semantics { liveRegion=LiveRegionMode.Polite }) }
            }
            if(error.isNotBlank()) item { Text(error,Modifier.semantics { liveRegion=LiveRegionMode.Assertive },color=MaterialTheme.colorScheme.error) }
            preview?.let { result ->
                result.playlist.sourceTotal?.let { total -> item { Text("Full list read: $total entries. ${result.playlist.unreadableEntries} unavailable or unreadable entries skipped.",style=MaterialTheme.typography.bodySmall) } }
                item { OutlinedTextField(name,{name=it.take(200)},Modifier.fillMaxWidth(),label={Text("Playlist name")},singleLine=true) }
                val matched=result.matches.count { it.track!=null }
                item { Text("${result.matches.size} songs · $matched matched · ${result.matches.size-matched} unmatched · ${selected.size} selected",Modifier.semantics { liveRegion=LiveRegionMode.Polite },style=MaterialTheme.typography.bodySmall) }
                    itemsIndexed(result.matches) { index,match ->
                        Row(Modifier.fillMaxWidth().heightIn(min=64.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(index in selected,{checked -> selected=if(checked) selected+index else selected-index},enabled=match.track!=null,
                                modifier=Modifier.semantics { contentDescription="Import ${match.song.title} by ${match.song.artist}" })
                            Column(Modifier.weight(1f)) {
                                Text(match.song.title,style=MaterialTheme.typography.titleSmall)
                                Text(match.song.artist,style=MaterialTheme.typography.bodySmall)
                                Text(match.track?.let { "Match: ${it.title} — ${it.artist}" } ?: "No confident match — skipped",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
            }
            }
            Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                GlassTextButton(onClick={close()},modifier=Modifier.heightIn(min=48.dp)) { Text(if(busy) "Cancel" else "Close") }
                if(preview==null) GlassButton(enabled=!busy && spotifyPlaylistId(input)!=null,onClick={
                    error="";busy=true;status="Reading public playlist…"
                    job=scope.launch {
                        val running=currentCoroutineContext()[Job]!!
                        try {
                            val playlist=runInterruptible(Dispatchers.IO) { SpotifyPlaylistImporter.fetch(input) { done,total ->
                                scope.launch { if(running.isActive) status="Step 1 of 2: reading playlist $done / $total" }
                            } }
                            status="Matching ${playlist.songs.size} songs…"
                            val result=SpotifyPlaylistImporter.match(playlist) { done,total ->
                                scope.launch { if(running.isActive) status="Matching songs: $done / $total" }
                            }
                            preview=result;name=playlist.name
                            selected=result.matches.indices.filter { result.matches[it].track!=null }.toSet()
                        } catch(e: CancellationException) { throw e }
                        catch(e: Exception) { error=e.message?.take(250) ?: "Could not read this public playlist. Check your connection and retry." }
                        finally { busy=false }
                    }
                },modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Text("Find songs") }
                else GlassButton(enabled=selected.isNotEmpty() && name.isNotBlank(),onClick={
                    runCatching {
                        val tracks=preview!!.matches.mapIndexedNotNull { index,match -> match.track.takeIf { index in selected } }
                        onImport(name.trim(),tracks);close()
                    }.onFailure {error="Could not save this playlist. Nothing was imported."}
                },modifier=Modifier.weight(1f).heightIn(min=48.dp)) {Text("Import ${selected.size} songs")}
            }
        }
    }
}
