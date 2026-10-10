package com.vibearc.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun PlaylistArtworkHeader(title: String, songs: List<Track>, onPlay: (Track, List<Track>) -> Unit) {
    Box(Modifier.fillMaxWidth().height(288.dp).clip(RoundedCornerShape(32.dp))) {
        val covers = songs.filter { it.artworkUri.isNotBlank() }.distinctBy(Track::artworkUri).take(4)
        Column(Modifier.fillMaxSize()) {
            repeat(2) { row -> Row(Modifier.weight(1f)) {
                repeat(2) { column -> Box(Modifier.weight(1f).fillMaxHeight()) {
                    covers.getOrNull(row * 2 + column)?.let { TrackArtwork(it, null, Modifier.matchParentSize(), sizePx = 384) }
                } }
            } }
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .12f), Color.Black.copy(alpha = .88f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 2, color = Color.White, overflow = TextOverflow.Ellipsis)
            Text("${songs.size} songs", color = Color.White.copy(alpha = .86f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton({ songs.firstOrNull()?.let { onPlay(it, songs) } }, enabled = songs.isNotEmpty()) { Glyph("play"); Spacer(Modifier.width(8.dp)); Text("Play") }
                GlassButton({ val mixed = songs.shuffled(); mixed.firstOrNull()?.let { onPlay(it, mixed) } }, enabled = songs.isNotEmpty()) { Glyph("shuffle"); Spacer(Modifier.width(8.dp)); Text("Shuffle") }
            }
        }
    }
}

@Composable
internal fun CatalogResultRow(item: CatalogItem, featured: Boolean = false, onOpen: () -> Unit) {
    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().motionClickable(onClick = onOpen).padding(if(featured) 16.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TrackArtwork(item.artworkTrack(), null, Modifier.size(if(featured) 72.dp else 48.dp)
                .clip(if(item.kind == CatalogKind.Artist) CircleShape else RoundedCornerShape(14.dp)))
            Column(Modifier.weight(1f)) {
                if(featured) Text("TOP ${item.kind.name.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MutedText)
                Text(item.title, fontWeight = if(featured) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.subtitle.ifBlank { item.kind.name }, color = MutedText, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            RoundAction("Open ${item.title}", onOpen) { Glyph("chevron") }
        }
    }
    if(featured) ReferenceSurface(Modifier.fillMaxWidth(), content = content) else content()
}

@Composable
internal fun CatalogDetailScreen(item: CatalogItem, padding: PaddingValues, onBack: () -> Unit,
    onOpen: (CatalogItem) -> Unit, onPlay: (Track, List<Track>) -> Unit) {
    var page by remember(item.id) { mutableStateOf<CatalogPage?>(null) }
    var busy by remember(item.id) { mutableStateOf(true) }
    var failure by remember(item.id) { mutableStateOf<String?>(null) }
    var attempt by remember(item.id) { mutableIntStateOf(0) }
    var allSongs by remember(item.id) { mutableStateOf(false) }
    var expandedAbout by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id, attempt) {
        busy = true; failure = null
        try { page = withContext(Dispatchers.IO) { OnlineMusic.browseCatalog(item) } }
        catch (error: Exception) {
            if(error is CancellationException) throw error
            failure = "Could not load this ${item.kind.name.lowercase()}. Check your connection and retry."
        } finally { busy = false }
    }
    val shown = page?.item ?: item
    val songs = page?.songs.orEmpty()
    Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundAction("Back", onBack) { Glyph("back") }
            Text(shown.title, Modifier.weight(1f).padding(horizontal = 16.dp), fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if(songs.isNotEmpty()) RoundAction("Play ${shown.title}", { onPlay(songs.first(), songs) }) { Glyph("play") }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
            bottom = padding.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(if(item.kind == CatalogKind.Artist) 280.dp else 300.dp).clip(RoundedCornerShape(32.dp))) {
                    TrackArtwork(shown.artworkTrack(), null, Modifier.matchParentSize(), sizePx = 768)
                    Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .78f)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        Text(shown.title, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(shown.subtitle.ifBlank { "YouTube Music ${shown.kind.name.lowercase()}" }, color = Color.White.copy(alpha = .86f))
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton({ songs.firstOrNull()?.let { onPlay(it, songs) } }, Modifier.weight(1f), enabled = songs.isNotEmpty()) {
                        Glyph("play"); Spacer(Modifier.width(8.dp)); Text("Play")
                    }
                    GlassButton({ val shuffled = songs.shuffled(); shuffled.firstOrNull()?.let { onPlay(it, shuffled) } },
                        Modifier.weight(1f), enabled = songs.isNotEmpty()) {
                        Glyph("shuffle"); Spacer(Modifier.width(8.dp)); Text("Shuffle")
                    }
                }
            }
            if(busy) item { Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp)); Text("Loading ${item.kind.name.lowercase()}…", color = MutedText)
            } }
            failure?.let { message -> item {
                Text(message, color = MaterialTheme.colorScheme.error)
                GlassTextButton({ attempt++ }) { Text("Retry") }
            } }
            if(page?.description?.isNotBlank() == true) item {
                Text("About", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(page!!.description, color = MutedText, maxLines = if(expandedAbout) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
                GlassTextButton({ expandedAbout = !expandedAbout }) { Text(if(expandedAbout) "Read less" else "Read more") }
            }
            item { Text(if(item.kind == CatalogKind.Artist) "Songs" else "${songs.size} songs", style = MaterialTheme.typography.headlineSmall) }
            val visibleSongs = if(item.kind == CatalogKind.Artist && !allSongs) songs.take(6) else songs
            items(visibleSongs, key = Track::uri) { song -> CatalogSongRow(song) { onPlay(song, songs) } }
            if(item.kind == CatalogKind.Artist && songs.size > 6) item {
                GlassTextButton({ allSongs = !allSongs }, Modifier.fillMaxWidth()) { Text(if(allSongs) "Show fewer songs" else "Show all songs") }
            }
            if(!busy && failure == null && songs.isEmpty()) item { Text("No playable songs were returned by the provider.", color = MutedText) }
            val releases = page?.albums.orEmpty()
            if(releases.isNotEmpty()) item {
                CatalogShelf("Albums", releases.filterNot { it.subtitle.contains(Regex("(?i)single|EP")) }, onOpen)
                CatalogShelf("Singles & EPs", releases.filter { it.subtitle.contains(Regex("(?i)single|EP")) }, onOpen)
            }
        }
    }
}

@Composable
private fun CatalogShelf(title: String, items: List<CatalogItem>, onOpen: (CatalogItem) -> Unit) {
    if(items.isEmpty()) return
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 16.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(items, key = CatalogItem::id) { album ->
            Column(Modifier.width(160.dp).motionClickable { onOpen(album) }) {
                TrackArtwork(album.artworkTrack(), null, Modifier.size(160.dp).clip(RoundedCornerShape(24.dp)), sizePx = 384)
                Spacer(Modifier.height(8.dp)); Text(album.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(album.subtitle.ifBlank { "Album" }, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 2)
            }
        }
    }
}

@Composable
internal fun CatalogSongRow(song: Track, featured: Boolean = false, onRemove: (() -> Unit)? = null, onPlay: () -> Unit) {
    var options by remember(song.catalogUri) { mutableStateOf(false) }
    val row: @Composable () -> Unit = {
        SongActionTarget(song, onPlay, Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(if(featured) 16.dp else 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TrackArtwork(song, null, Modifier.size(if(featured) 72.dp else 48.dp).clip(RoundedCornerShape(14.dp)))
                Column(Modifier.weight(1f)) {
                    if(featured) Text("TOP SONG", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Text(song.title, fontWeight = if(featured) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = MutedText, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if(onRemove != null) RoundAction("Remove ${song.title} from playlist", onRemove) { Glyph("delete") }
                RoundAction(if(featured) "Play ${song.title}" else "Options for ${song.title}", { if(featured) onPlay() else options = true }) {
                    Glyph(if(featured) "play" else "more")
                }
            }
        }
    }
    if(featured) ReferenceSurface(Modifier.fillMaxWidth(), content = row) else row()
    if(options) SongOptionsSheet(song, song) { options = false }
}
