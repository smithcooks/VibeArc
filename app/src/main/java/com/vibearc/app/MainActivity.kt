package com.vibearc.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID

private val Ink = Color(0xFF0B0B0D)
private val Panel = Color(0xFF141416)
private val PanelRaised = Color(0xFF1B1B1E)
internal val Sand = Color(0xFFD7A24A)
private val Peach = Color(0xFFE5B963)
private val Paper = Color(0xFFF5F3EE)
internal val MutedText = Color(0xFF9C9A93)
private val FaintText = Color(0xFF6C6A65)
private val DisplayFont = FontFamily(Font(R.font.space_grotesk))
private val BodyFont = FontFamily(Font(R.font.manrope))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VibeArcTheme { VibeArcApp() } }
    }
}

@Composable
private fun VibeArcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Sand,
            onPrimary = Color(0xFF171006),
            primaryContainer = Color(0xFF3B2A0D),
            onPrimaryContainer = Paper,
            secondary = Peach,
            onSecondary = Color(0xFF301B0B),
            background = Ink,
            onBackground = Paper,
            surface = Panel,
            onSurface = Paper,
            surfaceVariant = PanelRaised,
            onSurfaceVariant = MutedText,
            outline = Color(0x29FFFFFF),
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(32.dp),
            extraLarge = RoundedCornerShape(40.dp),
        ),
        typography = Typography(
            displaySmall = TextStyle(fontFamily = DisplayFont, fontSize = 42.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold),
            headlineMedium = TextStyle(fontFamily = DisplayFont, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
            titleLarge = TextStyle(fontFamily = DisplayFont, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontFamily = BodyFont, fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontFamily = BodyFont, fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = TextStyle(fontFamily = BodyFont, fontSize = 12.sp, lineHeight = 16.sp),
            labelLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold),
            labelMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold),
        ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VibeArcApp() {
    val context = LocalContext.current
    val controllerFuture = remember {
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync()
    }
    var player by remember { mutableStateOf<Player?>(null) }

    DisposableEffect(controllerFuture) {
        controllerFuture.addListener(
            { player = controllerFuture.get() },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            player = null
            MediaController.releaseFuture(controllerFuture)
        }
    }

    val activePlayer = player

    var currentTab by remember { mutableStateOf(Tab.Home) }
    var library by remember { mutableStateOf(context.loadLibrary()) }
    var playlists by remember { mutableStateOf(context.loadPlaylists()) }
    var recentUris by remember { mutableStateOf(context.loadRecentUris()) }
    val restoredTrack = activePlayer?.currentMediaItem?.track
    var currentTrack by remember {
        mutableStateOf(library.firstOrNull { it.uri == restoredTrack?.uri } ?: restoredTrack)
    }
    var isPlaying by remember { mutableStateOf(activePlayer?.isPlaying == true) }
    var queueTracks by remember { mutableStateOf(activePlayer?.queueTracks().orEmpty()) }
    var shuffleEnabled by remember { mutableStateOf(activePlayer?.shuffleModeEnabled == true) }
    var playerRepeatMode by remember { mutableIntStateOf(activePlayer?.repeatMode ?: Player.REPEAT_MODE_OFF) }
    var sleepRemainingMillis by remember { mutableLongStateOf(0L) }

    DisposableEffect(activePlayer, library) {
        if (activePlayer == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.track?.let { track ->
                    currentTrack = library.firstOrNull { it.uri == track.uri } ?: track
                    recentUris = recentUris.recordRecentUri(mediaItem.mediaId)
                }
                queueTracks = activePlayer.queueTracks()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffleEnabled = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                playerRepeatMode = repeatMode
            }
        }
        activePlayer.addListener(listener)
        onDispose { activePlayer.removeListener(listener) }
    }

    LaunchedEffect(context) {
        while (currentCoroutineContext().isActive) {
            val deadline = context.loadSleepDeadlineMillis()
            sleepRemainingMillis = deadline?.minus(System.currentTimeMillis())?.coerceAtLeast(0L) ?: 0L
            delay(1_000)
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val imported = context.trackFrom(uri)
        library = library.upsert(imported).also(context::saveLibrary)
        currentTrack = library.first { it.uri == imported.uri }
        activePlayer?.let { connectedPlayer ->
            connectedPlayer.loadQueue(library, currentTrack!!)
            queueTracks = connectedPlayer.queueTracks()
            currentTab = Tab.Player
        }
    }

    val playTrack: (Track, List<Track>) -> Unit = { track, source ->
        if (activePlayer != null && isAllowedMediaUri(track.uri)) {
            currentTrack = track
            activePlayer.loadQueue(source, track)
            queueTracks = activePlayer.queueTracks()
            currentTab = Tab.Player
        }
    }
    val toggleFavorite: (Track) -> Unit = { track ->
        library = library.toggleFavorite(track.uri).also(context::saveLibrary)
        library.firstOrNull { it.uri == track.uri }?.let { updated ->
            if (currentTrack?.uri == updated.uri) currentTrack = updated
        }
    }
    val updatePlaylists: (List<Playlist>) -> Unit = { next ->
        playlists = next.also(context::savePlaylists)
    }
    val recentTracks = recentUris.mapNotNull { mediaId -> library.firstOrNull { it.uri == mediaId } }

    Scaffold(
        topBar = {
            if (currentTab != Tab.Player) {
            TopAppBar(
                title = {
                    Column {
                        Text("VibeArc", style = MaterialTheme.typography.titleLarge)
                        Text(currentTab.label.uppercase(), fontSize = 11.sp, color = Sand, letterSpacing = 1.4.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { currentTab = Tab.Settings }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
            )
            }
        },
        bottomBar = {
            if (currentTab != Tab.Player) Column(
                modifier = Modifier.fillMaxWidth().systemBarsPadding().padding(bottom = 12.dp),
            ) {
                if (currentTrack != null && activePlayer != null) {
                    MiniPlayer(
                        track = currentTrack!!,
                        isPlaying = isPlaying,
                        onOpen = { currentTab = Tab.Player },
                        onToggle = activePlayer::toggle,
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    color = Color(0xE62A2A2D),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x29FFFFFF)),
                    shadowElevation = 16.dp,
                ) {
                NavigationBar(containerColor = Color.Transparent) {
                    MainTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Sand,
                                selectedTextColor = Sand,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = FaintText,
                                unselectedTextColor = FaintText,
                            ),
                        )
                    }
                }
                }
            }
        },
        containerColor = Ink,
    ) { padding ->
        when (currentTab) {
            Tab.Home -> HomeScreen(
                padding = padding,
                recentTracks = recentTracks,
                onPlay = { track -> playTrack(track, recentTracks.ifEmpty { listOf(track) }) },
                onExplore = { currentTab = Tab.Search },
            )
            Tab.Search -> {
                SearchScreen(padding, library) { track -> playTrack(track, library.ifEmpty { listOf(track) }) }
            }
            Tab.Library -> LibraryScreen(
                padding = padding,
                tracks = library,
                playlists = playlists,
                onChooseFile = { filePicker.launch(arrayOf("audio/*")) },
                onPlay = playTrack,
                onToggleFavorite = toggleFavorite,
                onCreatePlaylist = { name ->
                    updatePlaylists(playlists.createPlaylist(name, UUID.randomUUID().toString()))
                },
                onRenamePlaylist = { id, name -> updatePlaylists(playlists.renamePlaylist(id, name)) },
                onDeletePlaylist = { id -> updatePlaylists(playlists.deletePlaylist(id)) },
                onAddToPlaylist = { id, uri -> updatePlaylists(playlists.addTrackToPlaylist(id, uri)) },
                onRemoveFromPlaylist = { id, uri -> updatePlaylists(playlists.removeTrackFromPlaylist(id, uri)) },
            )
            Tab.Downloads -> DownloadsScreen(padding, library, onPlay = playTrack)
            Tab.Player -> if (activePlayer != null && currentTrack != null) PlayerScreen(
                padding = padding,
                player = activePlayer,
                track = currentTrack!!,
                isPlaying = isPlaying,
                queue = queueTracks,
                shuffleEnabled = shuffleEnabled,
                repeatMode = playerRepeatMode,
                sleepRemainingMillis = sleepRemainingMillis,
                onFavorite = { toggleFavorite(currentTrack!!) },
                onToggleShuffle = { activePlayer.shuffleModeEnabled = !activePlayer.shuffleModeEnabled },
                onCycleRepeat = { activePlayer.repeatMode = activePlayer.repeatMode.nextRepeatMode() },
                onCycleSleepTimer = {
                    val nextMinutes = when {
                        sleepRemainingMillis == 0L -> 15
                        sleepRemainingMillis <= 15 * 60_000L -> 30
                        sleepRemainingMillis <= 30 * 60_000L -> 60
                        else -> 0
                    }
                    context.saveSleepDeadlineMillis(
                        nextMinutes.takeIf { it > 0 }?.let { System.currentTimeMillis() + it * 60_000L },
                    )
                },
            ) else EmptyPlayer(padding) { currentTab = Tab.Search }
            Tab.Settings -> SettingsScreen(padding)
        }
    }
}

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Home("Home", Icons.Default.Home),
    Search("Search", Icons.Default.Search),
    Library("Library", Icons.AutoMirrored.Filled.List),
    Downloads("Downloads", Icons.AutoMirrored.Filled.List),
    Player("Playing", Icons.Default.PlayArrow),
    Settings("Settings", Icons.Default.Settings),
}

private val MainTabs = listOf(Tab.Home, Tab.Search, Tab.Library, Tab.Downloads)

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    recentTracks: List<Track>,
    onPlay: (Track) -> Unit,
    onExplore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Text("Good music.\nNo noise.", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("Your library and YouTube Music, shaped around what is playing.", color = MutedText)
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.55f)
                    .clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(Color(0xFF33230B), PanelRaised)))
                    .clickable(onClick = onExplore),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x22000000), Color(0xE6000000)),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("DISCOVER", color = Sand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Find your next track", style = MaterialTheme.typography.headlineMedium)
                    Text("Search YouTube Music", color = MutedText)
                    FilledIconButton(onClick = onExplore, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.Search, contentDescription = "Search music")
                    }
                }
            }
        }
        if (recentTracks.isNotEmpty()) {
            item { SectionTitle("Recently played") }
            items(recentTracks, key = Track::uri) { track ->
                TrackRow(track, onPlay = { onPlay(track) })
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    padding: PaddingValues,
    tracks: List<Track>,
    playlists: List<Playlist>,
    onChooseFile: () -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddToPlaylist: (String, String) -> Unit,
    onRemoveFromPlaylist: (String, String) -> Unit,
) {
    var mode by remember { mutableStateOf(LibraryMode.Tracks) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var trackToAdd by remember { mutableStateOf<Track?>(null) }
    val visibleTracks = if (mode == LibraryMode.Favorites) tracks.filter(Track::isFavorite) else tracks
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }
    val playlistTracks = selectedPlaylist?.trackUris.orEmpty().mapNotNull { uri -> tracks.firstOrNull { it.uri == uri } }
    val groupedTracks = remember(tracks, mode) {
        when (mode) {
            LibraryMode.Artists -> tracks.groupBy { it.artist.ifBlank { "Unknown artist" } }
            LibraryMode.Albums -> tracks.groupBy { it.album.ifBlank { "Unknown album" } }
            LibraryMode.Folders -> tracks.groupBy { it.folder.ifBlank { "Imported" } }
            else -> emptyMap()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Text("Your library", style = MaterialTheme.typography.headlineMedium) }
        item {
            Button(onClick = onChooseFile, modifier = Modifier.fillMaxWidth()) {
                Text("Add audio file")
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibraryMode.entries.forEach { option ->
                    FilterChip(
                        selected = mode == option,
                        onClick = {
                            mode = option
                            selectedGroup = null
                            if (option != LibraryMode.Playlists) selectedPlaylistId = null
                        },
                        label = {
                            Text(
                                when (option) {
                                    LibraryMode.Tracks -> "Tracks"
                                    LibraryMode.Artists -> "Artists"
                                    LibraryMode.Albums -> "Albums"
                                    LibraryMode.Folders -> "Folders"
                                    LibraryMode.Favorites -> "Favorites (${tracks.count(Track::isFavorite)})"
                                    LibraryMode.Playlists -> "Playlists (${playlists.size})"
                                },
                            )
                        },
                    )
                }
            }
        }

        if (mode == LibraryMode.Playlists) {
            if (selectedPlaylist == null) {
                item {
                    Button(onClick = { showCreateDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create playlist")
                    }
                }
                if (playlists.isEmpty()) {
                    item { EmptyLibraryCard("No playlists yet", "Create one to arrange tracks for any mood.") }
                }
                items(playlists, key = Playlist::id) { playlist ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PanelRaised),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().clickable { selectedPlaylistId = playlist.id },
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(playlist.name, fontWeight = FontWeight.Bold)
                            Text("${playlist.trackUris.size} tracks", color = MutedText)
                        }
                    }
                }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedPlaylistId = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to playlists")
                        }
                        Text(selectedPlaylist.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { playlistToRename = selectedPlaylist }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename ${selectedPlaylist.name}")
                        }
                        IconButton(onClick = { playlistToDelete = selectedPlaylist }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${selectedPlaylist.name}")
                        }
                    }
                }
                if (playlistTracks.isEmpty()) {
                    item { EmptyLibraryCard("This playlist is empty", "Use the playlist button beside a track to add it.") }
                } else {
                    item {
                        Button(
                            onClick = { onPlay(playlistTracks.first(), playlistTracks) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play playlist")
                        }
                    }
                    items(playlistTracks, key = Track::uri) { track ->
                        TrackRow(
                            track = track,
                            onPlay = { onPlay(track, playlistTracks) },
                            trailingIcon = Icons.Default.Delete,
                            trailingDescription = "Remove ${track.title} from ${selectedPlaylist.name}",
                            onTrailingAction = { onRemoveFromPlaylist(selectedPlaylist.id, track.uri) },
                        )
                    }
                }
            }
        } else if (mode == LibraryMode.Artists || mode == LibraryMode.Albums || mode == LibraryMode.Folders) {
            val selectedTracks = selectedGroup?.let(groupedTracks::get).orEmpty()
            if (selectedGroup == null) {
                if (groupedTracks.isEmpty()) {
                    item { EmptyLibraryCard("Nothing to browse yet", "Add audio files to browse your music here.") }
                }
                items(groupedTracks.entries.sortedBy { it.key.lowercase() }, key = { it.key }) { group ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PanelRaised),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().clickable { selectedGroup = group.key },
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(group.key, fontWeight = FontWeight.Bold)
                            Text("${group.value.size} ${if (group.value.size == 1) "track" else "tracks"}", color = MutedText)
                        }
                    }
                }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedGroup = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(selectedGroup.orEmpty(), style = MaterialTheme.typography.titleLarge)
                    }
                }
                if (selectedTracks.isNotEmpty()) {
                    item {
                        Button(
                            onClick = { onPlay(selectedTracks.first(), selectedTracks) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play all")
                        }
                    }
                    items(selectedTracks, key = Track::uri) { track ->
                        TrackRow(track = track, onPlay = { onPlay(track, selectedTracks) })
                    }
                }
            }
        } else {
            if (visibleTracks.isEmpty()) {
                item {
                    EmptyLibraryCard(
                        if (mode == LibraryMode.Favorites) "No favorites yet" else "Your library is empty",
                        if (mode == LibraryMode.Favorites) "Tap the heart beside a track to save it here."
                        else "Add an audio file to keep it in VibeArc.",
                    )
                }
            }
            items(visibleTracks, key = Track::uri) { track ->
                TrackRow(
                    track = track,
                    onPlay = { onPlay(track, visibleTracks) },
                    onFavorite = { onToggleFavorite(track) },
                    isFavorite = track.isFavorite,
                    trailingIcon = Icons.AutoMirrored.Filled.List.takeIf { playlists.isNotEmpty() },
                    trailingDescription = "Add ${track.title} to a playlist",
                    onTrailingAction = { trackToAdd = track }.takeIf { playlists.isNotEmpty() },
                )
            }
        }
    }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "Create playlist",
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onSave = {
                onCreatePlaylist(it)
                showCreateDialog = false
            },
        )
    }
    playlistToRename?.let { playlist ->
        PlaylistNameDialog(
            title = "Rename playlist",
            initialName = playlist.name,
            onDismiss = { playlistToRename = null },
            onSave = {
                onRenamePlaylist(playlist.id, it)
                playlistToRename = null
            },
        )
    }
    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete ${playlist.name}?") },
            text = { Text("The playlist will be removed. Your audio files stay in the library.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePlaylist(playlist.id)
                    selectedPlaylistId = null
                    playlistToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { playlistToDelete = null }) { Text("Cancel") } },
        )
    }
    trackToAdd?.let { track ->
        AlertDialog(
            onDismissRequest = { trackToAdd = null },
            title = { Text("Add ${track.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = {
                                onAddToPlaylist(playlist.id, track.uri)
                                trackToAdd = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(playlist.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { trackToAdd = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DownloadsScreen(
    padding: PaddingValues,
    tracks: List<Track>,
    onPlay: (Track, List<Track>) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Downloads", style = MaterialTheme.typography.headlineMedium)
            Text("Music saved on this device", color = MutedText)
        }
        if (tracks.isEmpty()) {
            item { EmptyLibraryCard("Nothing downloaded", "Add local audio from Library to listen offline.") }
        } else {
            items(tracks, key = Track::uri) { track ->
                TrackRow(track, onPlay = { onPlay(track, tracks) })
            }
        }
    }
}

@Composable
private fun EmptyPlayer(padding: PaddingValues, onExplore: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding).background(Ink), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nothing playing", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onExplore) { Text("Find music") }
        }
    }
}

private enum class LibraryMode { Tracks, Artists, Albums, Folders, Favorites, Playlists }

@Composable
private fun EmptyLibraryCard(title: String, message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelRaised),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = MutedText)
        }
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PlayerScreen(
    padding: PaddingValues,
    player: Player,
    track: Track,
    isPlaying: Boolean,
    queue: List<Track>,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    sleepRemainingMillis: Long,
    onFavorite: (() -> Unit)?,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onCycleSleepTimer: () -> Unit,
) {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var ambientAccent by remember(track.uri) { mutableStateOf(Sand) }

    LaunchedEffect(player, isPlaying) {
        while (currentCoroutineContext().isActive) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(1L)
            delay(if (isPlaying) 500 else 1_000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ambientAccent.copy(alpha = 0.48f), Ink, Ink)))
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            TrackArtwork(
                track = track,
                contentDescription = "Artwork for ${track.title}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large),
                onAccent = { ambientAccent = it },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.headlineMedium)
                    Text("${track.artist} • ${track.album}", color = MutedText)
                }
                if (onFavorite != null) {
                    IconButton(onClick = onFavorite) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = if (track.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (track.isFavorite) Peach else MutedText,
                        )
                    }
                }
            }
        }
        item {
            Slider(
                value = position.coerceAtMost(duration).toFloat(),
                onValueChange = { position = it.toLong() },
                onValueChangeFinished = { player.seekTo(position) },
                valueRange = 0f..duration.toFloat(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(DateUtils.formatElapsedTime(position / 1_000), color = MutedText)
                Text(DateUtils.formatElapsedTime(duration / 1_000), color = MutedText)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = player::seekToPreviousMediaItem,
                    enabled = player.hasPreviousMediaItem(),
                    modifier = Modifier.size(56.dp).clearAndSetSemantics { contentDescription = "Previous track" },
                ) {
                    Text("‹", fontSize = 42.sp)
                }
                FilledIconButton(
                    onClick = player::toggle,
                    modifier = Modifier.size(76.dp).clearAndSetSemantics {
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    },
                ) {
                    if (isPlaying) {
                        Text("Ⅱ", fontSize = 30.sp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(38.dp))
                    }
                }
                IconButton(
                    onClick = player::seekToNextMediaItem,
                    enabled = player.hasNextMediaItem(),
                    modifier = Modifier.size(56.dp).clearAndSetSemantics { contentDescription = "Next track" },
                ) {
                    Text("›", fontSize = 42.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(
                    selected = shuffleEnabled,
                    onClick = onToggleShuffle,
                    label = { Text(if (shuffleEnabled) "Shuffle on" else "Shuffle off") },
                )
                FilterChip(
                    selected = repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = onCycleRepeat,
                    label = { Text(repeatMode.repeatLabel()) },
                )
            }
        }
        item {
            TextButton(onClick = onCycleSleepTimer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (sleepRemainingMillis == 0L) "Sleep timer off"
                    else "Sleep in ${((sleepRemainingMillis + 59_999) / 60_000)} min",
                )
            }
        }
        item { SectionTitle("Queue") }
        if (queue.isEmpty()) {
            item { Text("The queue is empty.", color = MutedText) }
        } else {
            items(queue.indices.toList(), key = { index -> "$index-${queue[index].uri}" }) { index ->
                val queuedTrack = queue[index]
                TrackRow(
                    track = queuedTrack,
                    onPlay = { player.seekTo(index, 0L) },
                    trailingIcon = Icons.AutoMirrored.Filled.List.takeIf { index == player.currentMediaItemIndex },
                    trailingDescription = "Currently playing",
                )
            }
        }
    }
}

@Composable
private fun MiniPlayer(track: Track, isPlaying: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        color = Color(0xE62A2A2D),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x29FFFFFF)),
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackArtwork(track, null, Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(track.artist, color = MutedText, fontSize = 12.sp, maxLines = 1)
            }
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(48.dp).clearAndSetSemantics {
                    contentDescription = if (isPlaying) "Pause" else "Play"
                },
            ) {
                if (isPlaying) {
                    Text("Ⅱ", fontSize = 20.sp)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
            }
        }
    }
}

@Composable
internal fun TrackRow(
    track: Track,
    onPlay: () -> Unit,
    onFavorite: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingDescription: String = "Track action",
    onTrailingAction: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(track, null, Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.Bold)
            Text(
                buildString {
                    append("${track.artist} • ${track.album}")
                    if (track.durationMs > 0) append(" • ${DateUtils.formatElapsedTime(track.durationMs / 1_000)}")
                },
                color = MutedText,
            )
        }
        if (onFavorite == null) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play ${track.title}", tint = Sand)
        } else {
            IconButton(onClick = onFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = if (isFavorite) "Remove ${track.title} from favorites" else "Add ${track.title} to favorites",
                    tint = if (isFavorite) Peach else MutedText,
                )
            }
        }
        if (trailingIcon != null) {
            if (onTrailingAction == null) {
                Icon(trailingIcon, contentDescription = trailingDescription, tint = Sand)
            } else {
                IconButton(onClick = onTrailingAction) {
                    Icon(trailingIcon, contentDescription = trailingDescription)
                }
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

private fun Player.loadQueue(tracks: List<Track>, startTrack: Track, playNow: Boolean = true) {
    val queue = playbackQueue(tracks, startTrack)
    val startIndex = queue.indexOfFirst { it.uri == startTrack.uri }
    setMediaItems(queue.map(Track::toMediaItem), startIndex, 0L)
    prepare()
    if (playNow) play()
}

private fun Player.toggle() = if (isPlaying) pause() else play()

private fun Player.queueTracks(): List<Track> =
    (0 until mediaItemCount).map { index -> getMediaItemAt(index).track }

private fun Track.toMediaItem(): MediaItem {
    require(isAllowedMediaUri(uri)) { "Unsupported media URI" }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setExtras(Bundle().apply { putLong("durationMs", durationMs) })
        .apply {
            artworkUri.takeIf(String::isNotBlank)?.let { setArtworkUri(Uri.parse(it)) }
        }
        .build()
    return MediaItem.Builder()
        .setMediaId(uri)
        .setUri(Uri.parse(uri))
        .setMediaMetadata(metadata)
        .build()
}

private val MediaItem.track: Track
    get() = Track(
        title = mediaMetadata.title?.toString() ?: "Unknown track",
        artist = mediaMetadata.artist?.toString() ?: "On this device",
        album = mediaMetadata.albumTitle?.toString() ?: "Imported",
        uri = mediaId,
        durationMs = mediaMetadata.extras?.getLong("durationMs") ?: 0L,
        artworkUri = mediaMetadata.artworkUri?.toString().orEmpty(),
    )

private fun Int.nextRepeatMode(): Int = when (this) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

private fun Int.repeatLabel(): String = when (this) {
    Player.REPEAT_MODE_ALL -> "Repeat all"
    Player.REPEAT_MODE_ONE -> "Repeat one"
    else -> "Repeat off"
}
