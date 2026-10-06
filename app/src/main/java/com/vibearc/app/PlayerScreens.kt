package com.vibearc.app

import android.text.format.DateUtils
import android.animation.ValueAnimator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PlayerScreen(
    padding: PaddingValues, player: Player, track: Track, isPlaying: Boolean,
    queue: List<Track>, shuffleEnabled: Boolean, repeatMode: Int, sleepRemainingMillis: Long,
    onBack: () -> Unit, onFavorite: (() -> Unit)?, onDownload: () -> Unit, onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit, onCycleSleepTimer: () -> Unit,
    dynamicArtworkColor: Boolean,
    lyricsAnimationEnabled: Boolean,
) {
    var page by rememberSaveable { mutableStateOf("player") }
    var menu by remember { mutableStateOf(false) }
    var audioInfo by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val downloadStore = remember(context) { OfflineDownloads.get(context) }
    val downloads by downloadStore.entries.collectAsState()
    val download = downloads.firstOrNull { it.id == downloadIdentity(track) }
    val downloadBusy = download?.status in setOf(DownloadStatus.QUEUED, DownloadStatus.RESOLVING, DownloadStatus.DOWNLOADING)
    val wavy = remember { context.wavySeekbarEnabled() }
    val background = MaterialTheme.colorScheme.background
    val tint = if (dynamicArtworkColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    BackHandler(page != "player") { page = "player" }
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(lerp(background, tint, .20f), lerp(background, tint, .10f), background),
    )).padding(padding)) {
        val artworkSize = minOf(maxWidth - 48.dp, maxHeight * .45f, 420.dp).coerceAtLeast(160.dp)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerAction(if (page == "player") "Close player" else "Back to player", if (page == "player") "down" else "back") {
                    if (page == "player") onBack() else page = "player"
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(when(page) { "queue" -> "PLAYING QUEUE"; "lyrics" -> "LYRICS"; else -> "NOW PLAYING" },
                        fontSize = 12.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Text(if(page == "lyrics") "${track.title} · ${track.artist}" else track.album.ifBlank { "Your music" },
                        maxLines = 1, overflow = TextOverflow.Ellipsis, color = MutedText, fontSize = 14.sp)
                }
                Box {
                    PlayerAction("Player options", "more") { menu = true }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Playing queue") }, onClick = { menu = false; page = "queue" })
                        DropdownMenuItem(text = { Text("Lyrics") }, onClick = { menu = false; page = "lyrics" })
                        DropdownMenuItem(text = { Text(if(sleepRemainingMillis == 0L) "Sleep timer: off" else "Sleep timer: ${(sleepRemainingMillis + 59_999) / 60_000} min") },
                            onClick = { menu = false; onCycleSleepTimer() })
                        DropdownMenuItem(text = { Text("Audio information") }, onClick = { menu = false; audioInfo = true })
                    }
                }
            }
            when (page) {
                "queue" -> PlayingQueueScreen(player, queue)
                "lyrics" -> LyricsScreen(player, track, isPlaying, wavy, lyricsAnimationEnabled)
                else -> {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        item {
                            TrackArtwork(track, "Artwork for ${track.title}. Swipe left for next track, right for previous track.",
                                Modifier.size(artworkSize).clip(RoundedCornerShape(28.dp)).pointerInput(player, track.catalogUri) {
                                    var distance = 0f
                                    val threshold = 56.dp.toPx()
                                    detectHorizontalDragGestures(
                                        onDragStart = { distance = 0f },
                                        onDragCancel = { distance = 0f },
                                        onDragEnd = {
                                            when (artworkSwipeStep(distance, threshold)) {
                                                1 -> if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                                                -1 -> if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                                            }
                                            distance = 0f
                                        },
                                        onHorizontalDrag = { change, amount -> change.consume(); distance += amount },
                                    )
                                }, sizePx = 768)
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(track.title, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(4.dp))
                                    Text(track.artist, style = MaterialTheme.typography.titleLarge, color = MutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                PlayerAction(if(track.isFavorite) "Remove from favorites" else "Add to favorites", if(track.isFavorite) "heartFilled" else "heart", enabled = onFavorite != null) { onFavorite?.invoke() }
                                Spacer(Modifier.width(8.dp))
                                PlayerAction(when(download?.status) {
                                    DownloadStatus.COMPLETED -> "Open downloaded song"
                                    DownloadStatus.QUEUED -> "Download queued"
                                    DownloadStatus.RESOLVING -> "Finding download audio"
                                    DownloadStatus.DOWNLOADING -> "Downloading ${((download?.progress ?: 0f) * 100).toInt()} percent"
                                    DownloadStatus.PAUSED -> "Resume song download"
                                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> "Retry song download"
                                    null -> "Download song"
                                }, if(download?.status == DownloadStatus.COMPLETED) "check" else "download",
                                    enabled = !downloadBusy, busy = downloadBusy, onClick = onDownload)
                                Spacer(Modifier.width(8.dp))
                                PlayerAction("Show lyrics", "quote") { page = "lyrics" }
                            }
                        }
                        item { PlayerTimeControls(player, track, isPlaying, wavy) }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                PlayerPill("Shuffle ${if(shuffleEnabled) "on" else "off"}", "shuffle", Modifier.weight(1f), shuffleEnabled, onToggleShuffle)
                                PlayerPill("AUDIO", "quality", Modifier.weight(1.4f), false, { audioInfo = true }, showText = true)
                                PlayerPill(when(repeatMode) { Player.REPEAT_MODE_ONE -> "Repeat one"; Player.REPEAT_MODE_ALL -> "Repeat all"; else -> "Repeat off" }, "repeat", Modifier.weight(1f), repeatMode != Player.REPEAT_MODE_OFF, onCycleRepeat)
                            }
                        }
                        item { TextButton(onClick = { page = "queue" }) { Glyph("playlist"); Spacer(Modifier.width(8.dp)); Text("Playing queue · ${queue.size}") } }
                    }
                }
            }
        }
    }
    if(audioInfo) AlertDialog(onDismissRequest = { audioInfo = false }, title = { Text("Audio information") },
        text = { Text("${if(track.uri.startsWith("https:")) "Online stream" else "Local audio"}\n${player.currentAudioDetails()?.label() ?: "Format not reported by source"}\nRequested source selection: ${if(context.prefersHighestAudioQuality()) "highest available" else "balanced"}.\n\nValues are reported by the active stream; VibeArc does not claim bit-perfect, lossless, or Hi-Res output.") },
        confirmButton = { TextButton(onClick = { audioInfo = false }) { Text("Done") } })
}

@Composable
private fun PlayingQueueScreen(player: Player, queue: List<Track>) {
    var reorder by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Up next", style = MaterialTheme.typography.headlineMedium)
                    Text("${queue.size} songs · ${if(queue.isEmpty()) 0 else player.currentMediaItemIndex + 1} playing", color = MutedText)
                }
                PlayerAction(if(reorder) "Finish reordering" else "Reorder queue", if(reorder) "check" else "sort") { reorder = !reorder }
            }
        }
        if(queue.isEmpty()) item { Text("Your queue is empty.", Modifier.padding(24.dp), color = MutedText) }
        items(queue.size, key = { "$it:${queue[it].uri}" }) { index ->
            val queued = queue[index]
            val current = index == player.currentMediaItemIndex
            ReferenceSurface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), highlighted = current) {
                Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).clickable { player.seekTo(index, 0L); player.play() }, verticalAlignment = Alignment.CenterVertically) {
                        TrackArtwork(queued, null, Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(queued.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(queued.artist, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if(current && !reorder) Glyph("equalizer", Modifier.size(18.dp).semantics { contentDescription = "Currently playing" })
                    if(reorder) {
                        IconButton(onClick = { player.moveMediaItem(index, index - 1) }, enabled = index > 0) { Glyph("up", Modifier.semantics { contentDescription = "Move ${queued.title} up" }) }
                        IconButton(onClick = { player.moveMediaItem(index, index + 1) }, enabled = index < queue.lastIndex) { Glyph("down", Modifier.semantics { contentDescription = "Move ${queued.title} down" }) }
                    } else PlayerAction("Remove ${queued.title} from queue", "delete") { player.removeMediaItem(index) }
                }
            }
        }
    }
}

@Composable
private fun LyricsScreen(player: Player, track: Track, isPlaying: Boolean, wavy: Boolean, animationEnabled: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var request by remember(track.uri) { mutableIntStateOf(0) }
    var loading by remember(track.uri) { mutableStateOf(true) }
    var failed by remember(track.uri) { mutableStateOf(false) }
    var lyrics by remember(track.uri) { mutableStateOf<LyricsDocument?>(null) }
    var position by remember(track.uri) { mutableLongStateOf(0L) }
    var editor by remember(track.uri) { mutableStateOf(false) }
    var searchTitle by remember(track.uri,track.title) { mutableStateOf(track.title) }
    var searchArtist by remember(track.uri,track.artist) { mutableStateOf(track.artist) }
    var manualRaw by remember(track.uri) { mutableStateOf("") }
    var offsetText by remember(track.uri) { mutableStateOf("0") }
    val overrideStore = remember(context) { LyricsOverrideStore(context) }
    val listState = rememberLazyListState()
    val motionEnabled = animationEnabled && ValueAnimator.areAnimatorsEnabled()
    val saveLyrics = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val lines = lyrics?.syncedLines.orEmpty()
        if (uri != null && lines.isNotEmpty()) scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                        it.write(encodeLrc(lines))
                    } ?: error("Could not open lyrics file")
                }.isSuccess
            }
            android.widget.Toast.makeText(context, if(saved) "Synced lyrics saved" else "Could not save lyrics", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(track.catalogUri, track.title, track.artist, request) {
        loading = true
        failed = false
        if(track.durationMs<=0 && player.duration<=0) {
            // Let Media3 publish its duration before a title-only fallback is considered.
            repeat(30) { if(player.duration<=0) delay(100) }
        }
        val query = track.copy(durationMs=track.durationMs.takeIf { it>0 } ?: player.duration.coerceAtLeast(0))
        val local = withContext(Dispatchers.IO) { overrideStore.load(query) }
        val result = if(local != null && request == 0) Result.success(local.first) else withContext(Dispatchers.IO) {
            val disk = LyricsDiskCache(context)
            val saved = if(request==0) disk.read(query) else null
            runCatching { saved ?: LyricsProvider.fetch(query, searchTitle, searchArtist)?.also { document ->
                // A cache write failure must never hide successfully fetched lyrics.
                runCatching { disk.save(query,document) }
            } }
        }
        lyrics = result.getOrNull()
        local?.second?.let { offsetText=it.toString() }
        failed = result.isFailure
        loading = false
    }
    LaunchedEffect(player, track.uri, isPlaying) {
        while(isActive) {
            position = player.currentPosition.coerceAtLeast(0L)
            delay(if(isPlaying) 250 else 1_000)
        }
    }
    val activeLine = activeLyricIndex(lyrics?.syncedLines.orEmpty(), position)
    LaunchedEffect(activeLine, motionEnabled) {
        if(activeLine >= 0) {
            if(motionEnabled) listState.animateScrollToItem((activeLine - 1).coerceAtLeast(0))
            else listState.scrollToItem((activeLine - 1).coerceAtLeast(0))
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
            Text(
                when {
                    loading -> "LYRICS • LOADING"
                    lyrics?.syncedLines?.any { it.words.isNotEmpty() } == true -> "WORD SYNC • ${lyrics?.source.orEmpty().uppercase()}"
                    lyrics?.syncedLines?.isNotEmpty() == true -> "LINE SYNC • ${lyrics?.source.orEmpty().uppercase()}"
                    lyrics != null -> "LYRICS • ${lyrics?.source.orEmpty().uppercase()}"
                    else -> "LYRICS • UNAVAILABLE"
                },
                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            when {
                loading -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator()
                    Text("Finding lyrics…", color = MutedText)
                }
                failed -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lyrics could not be loaded.", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Check your connection and try again.", color = MutedText)
                    TextButton(onClick = { request++ }) { Text("Try again") }
                }
                lyrics == null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("No lyrics found.", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("No matching lyrics were found across KuGou, LRCLIB, or Lyrics.ovh.", color = MutedText)
                }
                lyrics?.instrumental == true -> Text("Instrumental", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                lyrics?.syncedLines?.isEmpty() == true && lyrics?.plainLines?.isEmpty() == true ->
                    Text("No lyrics found.", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                lyrics?.syncedLines?.isNotEmpty() == true -> LazyColumn(
                    Modifier.fillMaxSize(), state = listState,
                    contentPadding = PaddingValues(vertical = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(lyrics!!.syncedLines.size) { index ->
                        val line = lyrics!!.syncedLines[index]
                        val activeWord = if(index == activeLine) activeLyricWordIndex(line, position) else -1
                        val animatedText = if(line.words.isEmpty()) null else buildAnnotatedString {
                            line.words.forEachIndexed { wordIndex, word ->
                                withStyle(SpanStyle(color = when {
                                    index != activeLine -> MutedText.copy(alpha = .45f)
                                    wordIndex == activeWord -> MaterialTheme.colorScheme.primary
                                    wordIndex < activeWord -> MaterialTheme.colorScheme.onSurface
                                    else -> MutedText.copy(alpha = .55f)
                                })) { append(word.text) }
                            }
                        }
                        val target=lyricMotionTarget(index==activeLine,motionEnabled)
                        val scale by animateFloatAsState(target.first,if(motionEnabled) spring(dampingRatio=.62f,stiffness=380f) else snap(),label="lyric scale")
                        val lift by animateFloatAsState(target.second,if(motionEnabled) spring(dampingRatio=.62f,stiffness=380f) else snap(),label="lyric lift")
                        val density=LocalDensity.current.density
                        Text(
                            animatedText ?: androidx.compose.ui.text.AnnotatedString(line.text),
                            modifier=Modifier.fillMaxWidth().padding(end=12.dp).graphicsLayer {
                                scaleX=scale;scaleY=scale;translationY=lift*density
                                transformOrigin=TransformOrigin(0f,.5f)
                            },
                            color = if(index == activeLine) MaterialTheme.colorScheme.onSurface else MutedText.copy(alpha = .45f),
                            fontSize = 26.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(lyrics!!.plainLines) { line ->
                        Text(line, fontSize = 23.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomEnd).padding(bottom = 12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                PlayerAction("Search or edit lyrics", "more") {
                    manualRaw=lyrics?.syncedLines?.takeIf(List<LyricLine>::isNotEmpty)?.let(::encodeEnhancedLrc)
                        ?: lyrics?.plainLines?.joinToString("\n").orEmpty()
                    editor=true
                }
                if(lyrics?.syncedLines?.isNotEmpty() == true) PlayerAction("Save synchronized lyrics", "download") {
                    saveLyrics.launch(lyricsFileName(track.title))
                }
                PlayerAction(if(expanded) "Show playback controls" else "Expand lyrics", "expand") { expanded = !expanded }
            }
        }
        if(!expanded) PlayerTimeControls(player, track, isPlaying, wavy, compact = true)
    }
    if(editor) AlertDialog(
        onDismissRequest={editor=false},
        title={Text("Lyrics search & timing")},
        text={
            Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(searchTitle,{searchTitle=it.take(160)},label={Text("Title")},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(searchArtist,{searchArtist=it.take(160)},label={Text("Artist")},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(offsetText,{offsetText=it.filter { c -> c.isDigit() || c=='-' }.take(7)},label={Text("Timing offset (ms)")},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(manualRaw,{manualRaw=it.take(80_000)},label={Text("LRC, enhanced LRC, or plain lyrics")},minLines=5,maxLines=10,modifier=Modifier.fillMaxWidth())
                Text("Use +milliseconds when lyrics appear early and −milliseconds when they appear late.",style=MaterialTheme.typography.bodySmall,color=MutedText)
                TextButton(onClick={overrideStore.clear(track);editor=false;request++}) { Text("Remove manual version") }
            }
        },
        confirmButton={
            Row {
                TextButton(onClick={editor=false;request++}) { Text("Search providers") }
                Button(onClick={
                    val offset=offsetText.toLongOrNull()?.coerceIn(-30_000L,30_000L)?:0L
                    overrideStore.save(track,manualRaw,offset)
                    lyrics=overrideStore.load(track)?.first
                    editor=false
                },enabled=manualRaw.isNotBlank()) { Text("Save") }
            }
        },
        dismissButton={TextButton(onClick={editor=false}){Text("Cancel")}},
    )
}

@Composable
private fun PlayerAction(label: String, glyph: String, size: androidx.compose.ui.unit.Dp = 48.dp, enabled: Boolean = true, busy: Boolean = false, onClick: () -> Unit) {
    ReferenceSurface(shape = CircleShape) {
        IconButton(onClick, Modifier.size(size).semantics { contentDescription = label }, enabled = enabled) {
            if (busy) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSurface, strokeWidth = 2.dp)
            else Glyph(glyph, Modifier.size(25.dp), if(enabled) MaterialTheme.colorScheme.onSurface else MutedText.copy(alpha = .45f))
        }
    }
}

@Composable
private fun PlayerPill(label: String, glyph: String, modifier: Modifier, selected: Boolean, onClick: () -> Unit, showText: Boolean = false) {
    ReferenceSurface(modifier.clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = label }, CircleShape, highlighted = selected) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Glyph(glyph, Modifier.size(22.dp), if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if(showText) { Spacer(Modifier.width(6.dp)); Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            if(label == "Repeat one") Text("1", fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerTimeControls(player: Player, track: Track, isPlaying: Boolean, wavy: Boolean, compact: Boolean = false) {
    var position by remember(track.uri) { mutableLongStateOf(0L) }
    var duration by remember(track.uri) { mutableLongStateOf(track.durationMs.coerceAtLeast(1L)) }
    var dragging by remember(track.uri) { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = .16f)
    LaunchedEffect(player, track.uri, isPlaying) {
        while(isActive) {
            if(!dragging) position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0 } ?: track.durationMs.coerceAtLeast(1L)
            delay(if(isPlaying) 500 else 1_000)
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if(compact) 8.dp else 16.dp)) {
        Slider(value = position.coerceAtMost(duration).toFloat(), onValueChange = { dragging = true; position = it.toLong() },
            onValueChangeFinished = { player.seekTo(position); dragging = false }, valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Playback position" },
            thumb = { Box(Modifier.size(18.dp).background(accent, CircleShape)) },
            track = { state ->
                Canvas(Modifier.fillMaxWidth().height(30.dp)) {
                    val end = size.width * (state.value / duration.toFloat()).coerceIn(0f, 1f)
                    drawLine(inactive, Offset(0f, center.y), Offset(size.width, center.y), 4.dp.toPx(), StrokeCap.Round)
                    if(wavy && isPlaying && end > 0f) {
                        val path = Path()
                        val wavelength = 22.dp.toPx()
                        val amplitude = 6.dp.toPx()
                        val edgeLength = 10.dp.toPx()
                        val phase = (position % 1_200L) / 1_200f * (Math.PI * 2).toFloat()
                        val points = (end / 2.dp.toPx()).toInt().coerceAtLeast(1)
                        for(i in 0..points) {
                            val x = end * i / points
                            val edge = minOf(1f, x / edgeLength, (end - x) / edgeLength).coerceAtLeast(0f)
                            val y = center.y + kotlin.math.sin((x / wavelength * Math.PI * 2).toFloat() + phase) * amplitude * edge
                            if(i == 0) path.moveTo(x,y) else path.lineTo(x,y)
                        }
                        drawPath(path, accent, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                    } else {
                        drawLine(accent, Offset(0f, center.y), Offset(end, center.y), 4.dp.toPx(), StrokeCap.Round)
                    }
                }
            })
        if(!compact) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(DateUtils.formatElapsedTime(position / 1_000), color = MutedText, fontWeight = FontWeight.Bold)
            Text("−" + DateUtils.formatElapsedTime((duration - position).coerceAtLeast(0L) / 1_000), color = MutedText, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            if(compact) Text(DateUtils.formatElapsedTime(position / 1_000), fontSize = 12.sp, color = MutedText)
            PlayerAction("Previous track", "previous", if(compact) 46.dp else 60.dp, player.hasPreviousMediaItem()) { player.seekToPreviousMediaItem() }
            FilledIconButton(onClick = { if(player.isPlaying) player.pause() else player.play() }, modifier = Modifier.size(if(compact) 56.dp else 78.dp).semantics { contentDescription = if(isPlaying) "Pause" else "Play" }) {
                if(isPlaying) Glyph("pause", Modifier.size(30.dp), MaterialTheme.colorScheme.onPrimary)
                else Icon(Icons.Default.PlayArrow, "Play", Modifier.size(34.dp))
            }
            PlayerAction("Next track", "next", if(compact) 46.dp else 60.dp, player.hasNextMediaItem()) { player.seekToNextMediaItem() }
            if(compact) Text("−" + DateUtils.formatElapsedTime((duration - position).coerceAtLeast(0L) / 1_000), fontSize = 12.sp, color = MutedText)
        }
    }
}
