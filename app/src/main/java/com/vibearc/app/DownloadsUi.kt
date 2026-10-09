package com.vibearc.app

import com.vibearc.app.GlassButton as Button
import com.vibearc.app.GlassTextButton as OutlinedButton
import com.vibearc.app.GlassTextButton as TextButton
import com.vibearc.app.GlassButton as FilledTonalButton
import com.vibearc.app.GlassIconButton as IconButton
import com.vibearc.app.GlassFilledIconButton as FilledIconButton

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale


internal enum class DownloadFilter(val label: String) { All("All"), Lossless("Lossless / FLAC"), Lyrics("With lyrics (LRC)") }
internal enum class DownloadSort(val label: String) { Recent("Recent"), Title("Title"), Artist("Artist"), Album("Album"), Size("Size") }
internal fun selectDownloadEntries(entries: List<DownloadEntry>, query: String, filter: DownloadFilter, sort: DownloadSort, lyricsIds: Set<String>): List<DownloadEntry> {
    val words=query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return entries.filter { entry ->
        val text="${entry.track.title} ${entry.track.artist} ${entry.track.album}"
        words.all { text.contains(it,ignoreCase=true) } && when(filter) {
            DownloadFilter.All -> true
            DownloadFilter.Lossless -> entry.status==DownloadStatus.COMPLETED && entry.lossless
            DownloadFilter.Lyrics -> entry.status==DownloadStatus.COMPLETED && entry.id in lyricsIds
        }
    }.sortedWith(when(sort) {
        DownloadSort.Recent -> compareByDescending { it.completedAt.takeIf { time -> time>0 } ?: it.createdAt }
        DownloadSort.Title -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.track.title }
        DownloadSort.Artist -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.track.artist }
        DownloadSort.Album -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.track.album }
        DownloadSort.Size -> compareByDescending { it.bytes }
    })
}
private fun downloadSize(bytes: Long) = if(bytes<1024*1024) "${bytes/1024} KB" else String.format(Locale.getDefault(),"%.1f MB",bytes/(1024.0*1024))
internal fun downloadExtension(codec: String) = when {
    codec.contains("flac",true) -> "flac"
    codec.contains("mp4",true) || codec.contains("m4a",true) -> "m4a"
    codec.contains("aac",true) -> "aac"
    codec.contains("webm",true) -> "webm"
    codec.contains("mpeg",true) || codec.contains("mp3",true) -> "mp3"
    codec.contains("ogg",true) || codec.contains("opus",true) -> "ogg"
    codec.contains("wav",true) -> "wav"
    else -> "audio"
}

@Composable
internal fun OfflineDownloadsScreen(padding: PaddingValues, onPlay: (Track,List<Track>)->Unit, onBack: ()->Unit, onSettings: ()->Unit) {
    val context=LocalContext.current
    val direction=LocalLayoutDirection.current
    val store=remember(context) { OfflineDownloads.get(context) }
    val entries by store.entries.collectAsState()
    val scope=rememberCoroutineScope()
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var grouping by rememberSaveable { mutableStateOf("Songs") }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(DownloadSort.Recent) }
    var filter by rememberSaveable { mutableStateOf(DownloadFilter.All) }
    var lyricsIds by remember { mutableStateOf(emptySet<String>()) }
    var headerMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf<DownloadEntry?>(null) }
    var details by remember { mutableStateOf<DownloadEntry?>(null) }
    var files by remember { mutableStateOf(false) }
    fun action(start: Boolean=false, block: ()->Unit) {
        if(busy) return
        busy=true;error=""
        scope.launch {
            val result=withContext(Dispatchers.IO) { runCatching(block) }
            busy=false
            result.onFailure { error=it.message?.take(180) ?: "Could not update downloads. Saved files were preserved." }
            if(start && result.isSuccess) runCatching { DownloadService.start(context) }.onFailure { error="Could not start the download. Tap Retry." }
        }
    }
    LaunchedEffect(store) {
        withContext(Dispatchers.IO) { runCatching { store.load() } }.onFailure { error="Could not read saved downloads; no files were removed." }
        loaded=true
    }
    LaunchedEffect(entries) {
        lyricsIds=withContext(Dispatchers.IO) {
            entries.filter { it.status==DownloadStatus.COMPLETED && store.file(it.id,"lrc").isFile }.map { it.id }.toSet()
        }
    }
    val completed=remember(entries) { entries.filter { it.status==DownloadStatus.COMPLETED } }
    val ordered=remember(entries,query,filter,sort,lyricsIds) { selectDownloadEntries(entries,query,filter,sort,lyricsIds) }
    val visibleSaved=ordered.filter { it.status==DownloadStatus.COMPLETED }
    val queue=visibleSaved.map { it.track }
    val storage=completed.sumOf { it.bytes }
    val artists=entries.map { it.track.artist }.distinct().size
    val albums=entries.map { it.track.artist to it.track.album.ifBlank { "Unknown album" } }.distinct().size
    val tonalButtons=ButtonDefaults.filledTonalButtonColors(containerColor=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.onPrimaryContainer)
    val tonalIcons=IconButtonDefaults.filledTonalIconButtonColors(containerColor=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.onPrimaryContainer)
    fun play(songs: List<Track>) { if(songs.isNotEmpty()) onPlay(songs.first(),songs) }
    val exportFolder=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        if(folder!=null) action {
            completed.forEach { entry ->
                val local=store.offlineTrack(entry.track) ?: error("A saved file is unavailable")
                context.copyLocalTrackToFolder(local.copy(title="${entry.track.title}.${downloadExtension(entry.codec)}"),folder,{ !scope.isActive },{})
            }
        }
    }
    // ReferenceHeader owns the status-bar inset, like the app's Library header.
    Column(Modifier.fillMaxSize().padding(start=padding.calculateStartPadding(direction),end=padding.calculateEndPadding(direction),bottom=padding.calculateBottomPadding())) {
        ReferenceHeader("Downloads","${completed.size} songs · ${completed.map { it.track.artist }.distinct().size} artists · ${downloadSize(storage)}",onBack) {
            Box {
                IconButton(onClick={headerMenu=true},modifier=Modifier.size(48.dp).semantics { contentDescription="Download options" }) { Glyph("more") }
                DropdownMenu(headerMenu,{headerMenu=false}) {
                    DropdownMenuItem(text={Text("Download settings")},onClick={headerMenu=false;onSettings()})
                    DropdownMenuItem(text={Text("Retry unfinished")},enabled=!busy && entries.any { it.status in setOf(DownloadStatus.FAILED,DownloadStatus.PAUSED,DownloadStatus.CANCELLED) },onClick={
                        headerMenu=false;action(true) { store.enqueue(entries.filter { it.status in setOf(DownloadStatus.FAILED,DownloadStatus.PAUSED,DownloadStatus.CANCELLED) }.map { it.track }) }
                    })
                }
            }
        }
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            item {
                Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        listOf("Songs" to entries.size,"Artists" to artists,"Albums" to albums).forEach { (label,count) ->
                            TextButton(onClick={grouping=label},modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics {selected=grouping==label;role=Role.Tab},
                                colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface,containerColor=if(grouping==label) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)) {
                                Text("$label ($count)",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(query,{query=it.take(200)},modifier=Modifier.weight(1f),singleLine=true,
                        label={Text("Search songs, artists, albums")},leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(22.dp))
                    Box {
                        FilledTonalIconButton(onClick={sortMenu=true},colors=tonalIcons,modifier=Modifier.size(52.dp).semantics { contentDescription="Sort downloads: ${sort.label}" }) { Glyph("sort") }
                        DropdownMenu(sortMenu,{sortMenu=false}) { DownloadSort.entries.forEach { choice ->
                            DropdownMenuItem(text={Text(choice.label)},onClick={sort=choice;sortMenu=false},trailingIcon=if(sort==choice) {{Glyph("check")}} else null)
                        } }
                    }
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DownloadFilter.entries.forEach { choice -> FilterChip(filter==choice,{filter=choice},label={Text(choice.label,fontWeight=FontWeight.Bold)},modifier=Modifier.heightIn(min=48.dp),shape=RoundedCornerShape(16.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=MaterialTheme.colorScheme.primaryContainer,selectedLabelColor=MaterialTheme.colorScheme.onPrimaryContainer,labelColor=MaterialTheme.colorScheme.onSurface)) }
                }
            }
            item {
                ReferenceSurface {
                    Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Storage: ${downloadSize(storage)}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                            Text("Saved in VibeArc · ${completed.size} songs",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilledTonalButton(onClick={files=true},colors=tonalButtons,enabled=!busy,modifier=Modifier.padding(start=8.dp).heightIn(min=48.dp)) { Text("Files") }
                    }
                }
            }
            if(error.isNotBlank()) item { Text(error,color=MaterialTheme.colorScheme.error); TextButton(onClick={error=""}) {Text("Dismiss")} }
            if(busy || !loaded) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            item {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("$grouping (${if(grouping=="Songs") ordered.size else if(grouping=="Artists") ordered.map {it.track.artist}.distinct().size else ordered.map {it.track.artist to it.track.album}.distinct().size})",modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold)
                    FilledTonalIconButton(onClick={play(queue.shuffled())},colors=tonalIcons,enabled=queue.isNotEmpty(),modifier=Modifier.size(48.dp).semantics {contentDescription="Shuffle downloaded songs"}) { Glyph("shuffle") }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick={play(queue)},colors=tonalButtons,enabled=queue.isNotEmpty(),modifier=Modifier.heightIn(min=48.dp)) { Icon(Icons.Default.PlayArrow,null);Text("Play") }
                }
            }
            if(loaded && ordered.isEmpty()) item {
                Text(if(entries.isEmpty()) "Your offline library starts here" else "No matching downloads",style=MaterialTheme.typography.titleMedium)
                Text(if(entries.isEmpty()) "Long-press a song and choose Download. Saved songs play without a connection." else "Try another search or filter.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val groups=when(grouping) {
                "Artists" -> ordered.groupBy { it.track.artist }
                "Albums" -> ordered.groupBy { "${it.track.album.ifBlank { "Unknown album" }} · ${it.track.artist}" }
                else -> mapOf("" to ordered)
            }
            groups.forEach { (group,songs) ->
                if(group.isNotBlank()) item(key="group:$group") {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(group,Modifier.weight(1f),fontWeight=FontWeight.Bold)
                        IconButton(onClick={play(songs.filter {it.status==DownloadStatus.COMPLETED}.map {it.track})},enabled=songs.any {it.status==DownloadStatus.COMPLETED},modifier=Modifier.size(48.dp).semantics {contentDescription="Play $group"}) {Icon(Icons.Default.PlayArrow,null)}
                    }
                }
                itemsIndexed(songs,key={_,entry -> entry.id}) { index,entry ->
                    var menu by remember(entry.id) { mutableStateOf(false) }
                    val saved=entry.status==DownloadStatus.COMPLETED
                    ReferenceSurface(modifier=if(LocalMotionEnabled.current) Modifier.animateItem() else Modifier,shape=groupShape(index,songs.size)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                TrackArtwork(entry.track.copy(artworkUri=entry.artworkUri.ifBlank {entry.track.artworkUri}),null,Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),sizePx=160)
                                Column(Modifier.weight(1f)) {
                                    Text(cleanRecordingLabel(entry.track.title),fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    Text(listOf(cleanRecordingLabel(entry.track.artist),entry.track.album).filter(String::isNotBlank).joinToString(" · "),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    if(saved) Text(buildList {
                                        if(entry.track.durationMs>0) add("${entry.track.durationMs/60000}:${"%02d".format(entry.track.durationMs/1000%60)}")
                                        add(downloadSize(entry.bytes))
                                        if(entry.completedAt>0) add(DateFormat.getDateInstance(DateFormat.SHORT).format(Date(entry.completedAt)))
                                    }.joinToString(" · "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    else Text(entry.status.name.lowercase().replaceFirstChar {it.uppercase()}+" · ${downloadSize(entry.bytes)}",style=MaterialTheme.typography.bodySmall)
                                }
                                if(saved) FilledTonalIconButton(onClick={onPlay(entry.track,queue)},colors=tonalIcons,modifier=Modifier.size(48.dp).semantics {contentDescription="Play ${entry.track.title}"}) {Icon(Icons.Default.PlayArrow,null)}
                                Box {
                                    IconButton(onClick={menu=true},modifier=Modifier.size(48.dp).semantics {contentDescription="Options for ${entry.track.title}"}) {Glyph("more")}
                                    DropdownMenu(menu,{menu=false}) {
                                        if(entry.status in setOf(DownloadStatus.QUEUED,DownloadStatus.RESOLVING,DownloadStatus.DOWNLOADING)) {
                                            DropdownMenuItem(text={Text("Pause")},enabled=!busy,onClick={menu=false;action {store.update(entry.id){it.copy(status=DownloadStatus.PAUSED,autoResume=false,error="Paused")};DownloadService.interrupt(entry.id)}})
                                            DropdownMenuItem(text={Text("Cancel download")},enabled=!busy,onClick={menu=false;action {store.update(entry.id){it.copy(status=DownloadStatus.CANCELLED,autoResume=false,error="Cancelled")};DownloadService.interrupt(entry.id)}})
                                        } else if(!saved) DropdownMenuItem(text={Text(if(entry.status==DownloadStatus.PAUSED) "Resume" else "Retry")},enabled=!busy,onClick={menu=false;action(true){store.enqueue(listOf(entry.track))}})
                                        DropdownMenuItem(text={Text("Song information")},onClick={menu=false;details=entry})
                                        DropdownMenuItem(text={Text("Remove")},enabled=!busy,onClick={menu=false;delete=entry})
                                    }
                                }
                            }
                            if(saved && (entry.codec.isNotBlank() || entry.id in lyricsIds)) Row(Modifier.padding(top=6.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                if(entry.codec.isNotBlank()) DownloadBadge(entry.codec)
                                if(entry.id in lyricsIds) DownloadBadge("LRC")
                            }
                            if(entry.status==DownloadStatus.DOWNLOADING) LinearProgressIndicator(progress={entry.progress},modifier=Modifier.fillMaxWidth().padding(top=8.dp))
                            if(!saved && entry.error.isNotBlank()) Text(entry.error,Modifier.padding(top=6.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            if(entry.status in setOf(DownloadStatus.FAILED,DownloadStatus.PAUSED,DownloadStatus.CANCELLED)) TextButton(onClick={action(true){store.enqueue(listOf(entry.track))}},enabled=!busy) { Text(if(entry.status==DownloadStatus.PAUSED) "Resume" else "Retry") }
                        }
                    }
                }
            }
        }
    }
    delete?.let { entry -> AlertDialog(onDismissRequest={delete=null},title={Text("Remove ${entry.track.title}?")},text={Text(if(entry.ownsFile) "Remove this saved copy from VibeArc? Your library entry stays." else "Remove from Downloads? Your original phone file stays untouched.")},confirmButton={TextButton(onClick={delete=null;action {store.update(entry.id){it.copy(status=DownloadStatus.CANCELLED,autoResume=false)};DownloadService.interrupt(entry.id);DownloadService.awaitIdle(entry.id);store.remove(entry.id)}}){Text("Remove")}},dismissButton={TextButton(onClick={delete=null}){Text("Cancel")}}) }
    details?.let { entry -> AlertDialog(onDismissRequest={details=null},title={Text(cleanRecordingLabel(entry.track.title))},text={Text(listOf(entry.track.artist,entry.track.album,entry.codec,entry.bitrateKbps.takeIf{it>0}?.let{"$it kbps"}.orEmpty(),if(entry.lossless) "Lossless source" else "",entry.localUri,entry.error).filter(String::isNotBlank).joinToString("\n"))},confirmButton={TextButton(onClick={details=null}){Text("Close")}}) }
    if(files) AlertDialog(onDismissRequest={files=false},title={Text("Saved files")},text={Text("${completed.size} songs · ${downloadSize(storage)}\n\nAudio is saved in VibeArc's private storage. Export copies to a phone folder to browse them in your Files app. Originals stay in VibeArc.")},confirmButton={TextButton(onClick={files=false;exportFolder.launch(null)},enabled=completed.isNotEmpty()&&!busy){Text("Export files")}},dismissButton={TextButton(onClick={files=false}){Text("Close")}})
}

@Composable
private fun DownloadBadge(label: String) {
    Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(8.dp)) {
        Text(label,Modifier.padding(horizontal=8.dp,vertical=2.dp),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
    }
}
