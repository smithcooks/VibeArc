package com.vibearc.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
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
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import java.util.UUID

private val Ink = Color(0xFF101010)
private val Panel = Color(0xFF242424)
private val PanelRaised = Color(0xFF303030)
private val Peach = Color(0xFFE5B963)
private val Paper = Color(0xFFE6E6E6)
internal val MutedText = Color(0xFFC2C2C2)
private val FaintText = Color(0xFFB5B5B5)
private val BodyFont = FontFamily(
    Font(R.font.manrope_regular, weight = FontWeight.Normal),
    Font(R.font.manrope_bold, weight = FontWeight.Bold),
)
private val DisplayFont = BodyFont
private const val MaxBackupBytes = 8 * 1024 * 1024
private const val YouTubeReadOnlyScope = "https://www.googleapis.com/auth/youtube.readonly"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            var appearance by remember { mutableStateOf(this@MainActivity.loadAppearanceConfig()) }
            VibeArcApp(appearance) { updated ->
                appearance = updated
                this@MainActivity.saveAppearanceConfig(updated)
            }
        }
    }
}

@Composable
private fun VibeArcTheme(
    appearance: AppearanceConfig,
    artworkAccentArgb: Long?,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val accent = Color(appearance.activeAccentArgb(artworkAccentArgb))
    val artworkColorsActive = appearance.dynamicNowPlayingEnabled && artworkAccentArgb != null
    val neutral = appearance.accentPreset == AccentPreset.Mono && !artworkColorsActive
    val configuredScheme = if (
        appearance.dynamicColorEnabled && !artworkColorsActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = if (accent.luminance() > 0.45f) Color(0xFF171006) else Paper,
            primaryContainer = if (neutral) Color(0xFF484848) else lerp(Panel, accent, 0.30f),
            onPrimaryContainer = Paper,
            secondary = accent,
            onSecondary = Color(0xFF301B0B),
            background = if (neutral) Ink else lerp(Ink, accent, 0.05f),
            onBackground = Paper,
            surface = if (neutral) Panel else lerp(Panel, accent, 0.08f),
            onSurface = Paper,
            surfaceVariant = if (neutral) PanelRaised else lerp(PanelRaised, accent, 0.10f),
            onSurfaceVariant = MutedText,
            outline = lerp(Color(0xFF4B4947), accent, 0.22f),
        )
    }
    val colorScheme = if (appearance.amoledMode) configuredScheme.copy(
        background = Color.Black,
        surface = lerp(Color.Black, accent, 0.06f),
        surfaceVariant = lerp(Color(0xFF101012), accent, 0.10f),
    ) else configuredScheme
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(32.dp),
            extraLarge = RoundedCornerShape(40.dp),
        ),
        typography = Typography(
            displaySmall = TextStyle(fontFamily = if (appearance.applicationFontEnabled) DisplayFont else FontFamily.Default, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
            headlineMedium = TextStyle(fontFamily = if (appearance.applicationFontEnabled) DisplayFont else FontFamily.Default, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
            titleLarge = TextStyle(fontFamily = if (appearance.applicationFontEnabled) DisplayFont else FontFamily.Default, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontFamily = if (appearance.applicationFontEnabled) DisplayFont else FontFamily.Default, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontFamily = if (appearance.applicationFontEnabled) BodyFont else FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontFamily = if (appearance.applicationFontEnabled) BodyFont else FontFamily.Default, fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = TextStyle(fontFamily = if (appearance.applicationFontEnabled) BodyFont else FontFamily.Default, fontSize = 12.sp, lineHeight = 16.sp),
            labelLarge = TextStyle(fontFamily = if (appearance.applicationFontEnabled) BodyFont else FontFamily.Default, fontWeight = FontWeight.SemiBold),
            labelMedium = TextStyle(fontFamily = if (appearance.applicationFontEnabled) BodyFont else FontFamily.Default, fontWeight = FontWeight.SemiBold),
        ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VibeArcApp(
    appearance: AppearanceConfig,
    onAppearanceChange: (AppearanceConfig) -> Unit,
) {
    val context = LocalContext.current
    val controllerFuture = remember {
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync()
    }
    var player by remember { mutableStateOf<Player?>(null) }
    val uiScope = rememberCoroutineScope()
    val authorizationClient = remember(context) { Identity.getAuthorizationClient(context) }
    val cachedYouTubeState = remember { context.loadYouTubeAccountState() }
    var youtubeAccountData by remember { mutableStateOf<YouTubeAccountData?>(null) }
    var youtubeAccountBusy by remember { mutableStateOf(cachedYouTubeState != null) }
    var youtubeSyncBusy by remember { mutableStateOf(false) }
    var youtubeAccessToken by remember { mutableStateOf<String?>(null) }
    var youtubeSelectedPlaylistIds by remember {
        mutableStateOf(cachedYouTubeState?.selectedPlaylistIds.orEmpty())
    }
    var lastFmUsername by remember { mutableStateOf(context.loadLastFmUsername()) }
    var lastFmSnapshot by remember { mutableStateOf<LastFmSnapshot?>(null) }
    var lastFmRecommendations by remember { mutableStateOf<List<LastFmTrack>>(emptyList()) }
    var lastFmRecommendationsBusy by remember { mutableStateOf(false) }
    var lastFmBusy by remember { mutableStateOf(false) }
    var lastFmError by remember { mutableStateOf<String?>(null) }
    var lastFmRefresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(lastFmUsername, lastFmRefresh) {
        val username = lastFmUsername ?: return@LaunchedEffect
        if (BuildConfig.LASTFM_API_KEY.isBlank()) {
            lastFmError = "This build does not include a Last.fm API key."
            return@LaunchedEffect
        }
        lastFmBusy = true
        lastFmError = null
        val loaded = withContext(Dispatchers.IO) {
            runCatching { LastFmApi.load(username, BuildConfig.LASTFM_API_KEY) }
        }
        lastFmSnapshot = loaded.getOrNull()
        lastFmRecommendations = emptyList()
        lastFmError = loaded.exceptionOrNull()?.let { "Could not load the Last.fm profile." }
        lastFmBusy = false
    }

    val connectLastFm: (String) -> Unit = { value ->
        val username = validLastFmUsername(value)
        if (username == null) {
            android.widget.Toast.makeText(context, "Enter a valid Last.fm username", android.widget.Toast.LENGTH_LONG).show()
        } else {
            context.saveLastFmUsername(username)
            lastFmUsername = username
            lastFmSnapshot = null
            lastFmRecommendations = emptyList()
            lastFmRefresh++
        }
    }
    val disconnectLastFm: () -> Unit = {
        context.saveLastFmUsername(null)
        lastFmUsername = null
        lastFmSnapshot = null
        lastFmRecommendations = emptyList()
        lastFmError = null
    }

    fun loadYouTubeAccount(accessToken: String?, announce: Boolean) {
        if (accessToken.isNullOrBlank()) {
            youtubeAccountBusy = false
            if (announce) android.widget.Toast.makeText(context, "Google did not provide YouTube access", android.widget.Toast.LENGTH_LONG).show()
        } else uiScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { YouTubeAccountApi.load(accessToken) }.getOrNull()
            }
            youtubeAccountBusy = false
            if (loaded == null) {
                if (announce) android.widget.Toast.makeText(context, "Could not load your YouTube account", android.widget.Toast.LENGTH_LONG).show()
            } else {
                val saved = context.loadYouTubeAccountState()
                youtubeSelectedPlaylistIds = saved?.selectedPlaylistIds
                    ?.takeIf { saved.account.channelId == loaded.account.channelId }.orEmpty()
                youtubeAccessToken = accessToken
                youtubeAccountData = loaded
                context.saveYouTubeAccountState(YouTubeAccountState(loaded.account, youtubeSelectedPlaylistIds))
                if (announce) android.widget.Toast.makeText(context, "Connected ${loaded.account.displayName}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun acceptYouTubeAuthorization(authorization: AuthorizationResult?, announce: Boolean) {
        loadYouTubeAccount(authorization?.accessToken, announce)
    }

    val youtubeAuthorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            youtubeAccountBusy = false
            return@rememberLauncherForActivityResult
        }
        val authorization = runCatching {
            authorizationClient.getAuthorizationResultFromIntent(result.data)
        }.getOrNull()
        acceptYouTubeAuthorization(authorization, announce = true)
    }

    fun requestYouTubeAuthorization(allowResolution: Boolean, announce: Boolean) {
        if (!youtubeAccountBusy) {
            youtubeAccountBusy = true
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(YouTubeReadOnlyScope)))
                .build()
            authorizationClient.authorize(request)
                .addOnSuccessListener { authorization ->
                    val resolution = authorization.pendingIntent
                    if (allowResolution && authorization.hasResolution() && resolution != null) {
                        youtubeAuthorizationLauncher.launch(
                            IntentSenderRequest.Builder(resolution.intentSender).build(),
                        )
                    } else if (authorization.hasResolution()) {
                        youtubeAccountBusy = false
                        youtubeAccountData = null
                    } else {
                        acceptYouTubeAuthorization(authorization, announce)
                    }
                }
                .addOnFailureListener {
                    youtubeAccountBusy = false
                    if (announce) android.widget.Toast.makeText(context, "Could not connect your Google account", android.widget.Toast.LENGTH_LONG).show()
                }
        }
    }
    val connectYouTube: () -> Unit = { requestYouTubeAuthorization(allowResolution = true, announce = true) }

    fun disconnectYouTube(reconnect: Boolean) {
        if (youtubeAccountBusy) return
        youtubeAccountBusy = true
        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).revokeAccess().addOnCompleteListener { task ->
            youtubeAccessToken = null
            youtubeAccountData = null
            youtubeSelectedPlaylistIds = emptySet()
            context.clearYouTubeAccountState()
            youtubeAccountBusy = false
            when {
                reconnect && task.isSuccessful -> connectYouTube()
                reconnect -> android.widget.Toast.makeText(context, "Could not revoke the current account; reconnect manually", android.widget.Toast.LENGTH_LONG).show()
                task.isSuccessful -> android.widget.Toast.makeText(context, "YouTube account disconnected", android.widget.Toast.LENGTH_SHORT).show()
                else -> android.widget.Toast.makeText(context, "Local account data removed; Google access could not be revoked", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(cachedYouTubeState?.account?.channelId) {
        if (cachedYouTubeState != null) {
            youtubeAccountBusy = false
            requestYouTubeAuthorization(allowResolution = false, announce = false)
        }
    }

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
    var lastContentTab by remember { mutableStateOf(Tab.Home) }
    var playerBackTab by remember { mutableStateOf(Tab.Home) }
    var library by remember { mutableStateOf(context.loadLibrary()) }
    var playlists by remember { mutableStateOf(context.loadPlaylists()) }
    var recentUris by remember { mutableStateOf(context.loadRecentUris()) }

    LaunchedEffect(currentTab, lastFmSnapshot) {
        if (currentTab != Tab.Discover || lastFmRecommendations.isNotEmpty()) return@LaunchedEffect
        val seed = lastFmSnapshot?.topTracks?.firstOrNull() ?: return@LaunchedEffect
        lastFmRecommendationsBusy = true
        lastFmRecommendations = withContext(Dispatchers.IO) {
            runCatching { LastFmApi.similarTracks(seed, BuildConfig.LASTFM_API_KEY) }.getOrDefault(emptyList())
        }
        lastFmRecommendationsBusy = false
    }
    var searchSeed by remember { mutableStateOf("") }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var artworkAccentArgb by remember(currentTrack?.uri) { mutableStateOf<Long?>(null) }
    LaunchedEffect(currentTrack?.uri, currentTrack?.artworkUri, appearance.dynamicNowPlayingEnabled) {
        artworkAccentArgb = currentTrack?.artworkUri?.takeIf {
            appearance.dynamicNowPlayingEnabled && it.isNotBlank()
        }?.let {
            ArtworkCache.load(it, 160)?.accent?.toArgb()?.toLong()?.and(0xFFFFFFFFL)
        }
    }
    var isPlaying by remember { mutableStateOf(activePlayer?.isPlaying == true) }
    var queueTracks by remember { mutableStateOf(activePlayer?.queueTracks().orEmpty()) }
    var shuffleEnabled by remember { mutableStateOf(activePlayer?.shuffleModeEnabled == true) }
    var playerRepeatMode by remember { mutableIntStateOf(activePlayer?.repeatMode ?: Player.REPEAT_MODE_OFF) }
    var sleepRemainingMillis by remember { mutableLongStateOf(0L) }

    val openPlayer = {
        if (currentTab != Tab.Player) playerBackTab = currentTab
        currentTab = Tab.Player
    }
    val navigateBack = {
        currentTab = backDestination(currentTab, lastContentTab, playerBackTab)
    }
    val closePlayer = navigateBack
    BackHandler(enabled = currentTab !in MainTabs, onBack = navigateBack)
    val navigate: (Tab) -> Unit = { tab ->
        if (currentTab in MainTabs) lastContentTab = currentTab
        currentTab = tab
    }

    LaunchedEffect(activePlayer, library) {
        val connectedPlayer = activePlayer ?: return@LaunchedEffect
        connectedPlayer.currentMediaItem?.track?.let { restored ->
            currentTrack = library.firstOrNull { it.uri == restored.uri } ?: restored
        }
        isPlaying = connectedPlayer.isPlaying
        queueTracks = connectedPlayer.queueTracks()
        shuffleEnabled = connectedPlayer.shuffleModeEnabled
        playerRepeatMode = connectedPlayer.repeatMode
    }

    DisposableEffect(activePlayer, library) {
        if (activePlayer == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                queueTracks = activePlayer.queueTracks()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem == null) currentTrack = null
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
        uiScope.launch {
            val imported = withContext(Dispatchers.IO) { runCatching { context.trackFrom(uri) }.getOrNull() }
            if (imported == null) {
                android.widget.Toast.makeText(context, "Could not read this audio file. Please choose it again.", android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            library = library.upsert(imported).also(context::saveLibrary)
            currentTrack = library.first { it.uri == imported.uri }
            activePlayer?.let { connectedPlayer ->
                connectedPlayer.loadQueue(library, currentTrack!!)
                queueTracks = connectedPlayer.queueTracks()
                openPlayer()
            }
        }
    }

    val backupWriter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        uiScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                        it.write(BackupCodec.encode(library, playlists))
                    } ?: error("Could not open backup file")
                }.isSuccess
            }
            android.widget.Toast.makeText(
                context,
                if (saved) "VibeArc backup saved" else "Could not save the backup",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }
    val backupReader = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        uiScope.launch {
            val backup = withContext(Dispatchers.IO) {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)?.use {
                        it.readUtf8Limited(MaxBackupBytes)
                    } ?: error("Could not open backup file")
                    BackupCodec.decode(json)
                }.getOrNull()
            }
            if (backup == null) {
                android.widget.Toast.makeText(context, "This is not a valid VibeArc backup", android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            val merged = mergeBackup(library, playlists, backup)
            library = merged.tracks.also(context::saveLibrary)
            playlists = merged.playlists.also(context::savePlaylists)
            android.widget.Toast.makeText(
                context,
                "Restored ${backup.tracks.size} tracks and ${backup.playlists.size} playlists",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }
    val playlistReader = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        uiScope.launch {
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    val contents = context.contentResolver.openInputStream(uri)?.use {
                        it.readUtf8Limited(MaxBackupBytes)
                    } ?: error("Could not open playlist file")
                    val fileName = Uri.decode(uri.lastPathSegment.orEmpty())
                        .substringAfterLast('/').substringAfterLast(':')
                        .ifBlank { "Imported playlist.txt" }
                    parsePlaylistFile(fileName, contents, library, UUID.randomUUID().toString())
                }.getOrNull()
            }
            if (imported == null) {
                android.widget.Toast.makeText(context, "Could not import this playlist", android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            playlists = (playlists + imported).also(context::savePlaylists)
            android.widget.Toast.makeText(
                context,
                "Imported ${imported.name} · ${imported.trackUris.size} matched tracks",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    val playTrack: (Track, List<Track>) -> Unit = { track, source ->
        if (activePlayer != null && isAllowedMediaUri(track.uri)) {
            currentTrack = track
            activePlayer.loadQueue(source, track)
            queueTracks = activePlayer.queueTracks()
            openPlayer()
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
    fun mergeYouTubePlaylist(remotePlaylist: YouTubePlaylist, remoteTracks: List<Track>): Int {
        val id = "youtube:${remotePlaylist.id}"
        val currentUris = playlists.firstOrNull { it.id == id }?.trackUris.orEmpty()
        val diff = previewYouTubePlaylistSync(currentUris, remoteTracks)
        library = remoteTracks.fold(library) { current, track -> current.upsert(track) }.also(context::saveLibrary)
        val localPlaylist = Playlist(id, remotePlaylist.title, (currentUris + remoteTracks.map(Track::uri)).distinct())
        playlists = (playlists.filterNot { it.id == id } + localPlaylist).also(context::savePlaylists)
        return diff.remoteOnlyTracks.size
    }

    val importYouTubePlaylist: (YouTubePlaylist) -> Unit = { remotePlaylist ->
        val accessToken = youtubeAccessToken
        if (accessToken == null) {
            android.widget.Toast.makeText(context, "Connect your YouTube account again", android.widget.Toast.LENGTH_LONG).show()
        } else uiScope.launch {
            val importedTracks = withContext(Dispatchers.IO) {
                runCatching { YouTubeAccountApi.loadPlaylist(accessToken, remotePlaylist) }.getOrNull()
            }
            when {
                importedTracks == null -> android.widget.Toast.makeText(context, "Could not import this playlist", android.widget.Toast.LENGTH_LONG).show()
                importedTracks.isEmpty() -> android.widget.Toast.makeText(context, "This playlist has no available tracks", android.widget.Toast.LENGTH_LONG).show()
                else -> {
                    mergeYouTubePlaylist(remotePlaylist, importedTracks)
                    android.widget.Toast.makeText(context, "Imported ${remotePlaylist.title}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val pullSelectedYouTubePlaylists: () -> Unit = {
        val accessToken = youtubeAccessToken
        val selected = youtubeAccountData?.playlists.orEmpty()
            .filter { it.id in youtubeSelectedPlaylistIds }
        if (accessToken == null) {
            android.widget.Toast.makeText(context, "Connect your YouTube account again", android.widget.Toast.LENGTH_LONG).show()
        } else if (selected.isEmpty()) {
            android.widget.Toast.makeText(context, "Select at least one playlist", android.widget.Toast.LENGTH_SHORT).show()
        } else if (!youtubeSyncBusy) uiScope.launch {
            youtubeSyncBusy = true
            val loaded = withContext(Dispatchers.IO) {
                selected.mapNotNull { playlist ->
                    runCatching { YouTubeAccountApi.loadPlaylist(accessToken, playlist) }
                        .getOrNull()?.let { playlist to it }
                }
            }
            val added = loaded.sumOf { (playlist, tracks) -> mergeYouTubePlaylist(playlist, tracks) }
            youtubeSyncBusy = false
            val failures = selected.size - loaded.size
            val message = buildString {
                append("Pulled $added new track")
                if (added != 1) append('s')
                if (failures > 0) append(" · $failures playlist${if (failures == 1) "" else "s"} failed")
            }
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    VibeArcTheme(appearance, artworkAccentArgb) {
    CompositionLocalProvider(LocalGlass provides appearance.liquidGlassEnabled) {
    Scaffold(
        topBar = {
            if (currentTab !in listOf(Tab.Player, Tab.Search, Tab.Library)) ReferenceHeader(
                title = when(currentTab) { Tab.Home -> "Home"; Tab.Stats -> "Stats"; else -> currentTab.label },
                onBack = if (currentTab !in MainTabs) navigateBack else null,
            ) {
                if (currentTab in MainTabs) {
                    RoundAction("Discover", { navigate(Tab.Discover) }) { Glyph("discover") }
                    Spacer(Modifier.width(8.dp))
                    RoundAction("Search", { searchSeed = ""; navigate(Tab.Search) }) { Icon(Icons.Default.Search, null) }
                    Spacer(Modifier.width(8.dp))
                    RoundAction("Settings", { navigate(Tab.Settings) }) {
                        if(currentTab == Tab.Stats) Glyph("account") else Icon(Icons.Default.Settings, null)
                    }
                }
            }
        },
        bottomBar = {
            if (currentTab in MainTabs) Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (currentTrack != null && activePlayer != null) MiniPlayer(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    onOpen = openPlayer,
                    onToggle = activePlayer::toggle,
                    onNext = activePlayer::seekToNextMediaItem,
                )
                ReferenceSurface(Modifier.widthIn(max = 340.dp).padding(horizontal = 28.dp), shape = CircleShape) {
                    Row(Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        MainTabs.forEach { tab ->
                            val selected = currentTab == tab
                            Surface(
                                modifier = Modifier.clickable { currentTab = tab },
                                color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                shape = CircleShape,
                            ) {
                                Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (tab == Tab.Home) Icon(Icons.Default.Home, if(selected) null else "Feed")
                                    else Glyph(if(tab == Tab.Stats) "stats" else "playlist", Modifier.semantics { contentDescription = tab.label })
                                    if(selected) Text(if(tab == Tab.Home) "Feed" else tab.label, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (currentTab) {
            Tab.Home -> HomeScreen(
                padding = padding,
                recentTracks = recentTracks,
                currentTrack = currentTrack,
                libraryTracks = library,
                onPlay = playTrack,
                onExplore = { searchSeed = ""; navigate(Tab.Search) },
                onLibrary = { navigate(Tab.Library) },
                onDiscover = { navigate(Tab.Discover) },
                onSearch = { query -> searchSeed = query; navigate(Tab.Search) },
            )
            Tab.Search -> {
                SearchScreen(padding, library, searchSeed, navigateBack) { track -> playTrack(track, listOf(track)) }
            }
            Tab.Library -> LibraryScreen(
                padding = padding,
                tracks = library,
                playlists = playlists,
                onChooseFile = { filePicker.launch(arrayOf("audio/*")) },
                onGenerator = { navigate(Tab.Generator) },
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
                onBack = closePlayer,
                onFavorite = if (library.any { it.uri == currentTrack!!.uri }) ({ toggleFavorite(currentTrack!!) }) else null,
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
                dynamicArtworkColor = appearance.dynamicNowPlayingEnabled,
            ) else EmptyPlayer(padding) { currentTab = Tab.Search }
            Tab.Settings -> SettingsScreen(
                padding, appearance, onAppearanceChange,
                youtubeAccountData = youtubeAccountData,
                youtubeAccountBusy = youtubeAccountBusy,
                youtubeSyncBusy = youtubeSyncBusy,
                youtubeSelectedPlaylistIds = youtubeSelectedPlaylistIds,
                onConnectYouTube = connectYouTube,
                onDisconnectYouTube = { disconnectYouTube(reconnect = false) },
                onSwitchYouTubeAccount = { disconnectYouTube(reconnect = true) },
                onToggleYouTubePlaylistSync = { playlistId ->
                    youtubeSelectedPlaylistIds = if (playlistId in youtubeSelectedPlaylistIds) {
                        youtubeSelectedPlaylistIds - playlistId
                    } else {
                        youtubeSelectedPlaylistIds + playlistId
                    }
                    youtubeAccountData?.account?.let { account ->
                        context.saveYouTubeAccountState(YouTubeAccountState(account, youtubeSelectedPlaylistIds))
                    }
                },
                onPullSelectedYouTubePlaylists = pullSelectedYouTubePlaylists,
                onImportYouTubePlaylist = importYouTubePlaylist,
                lastFmUsername = lastFmUsername,
                lastFmBusy = lastFmBusy,
                lastFmConfigured = BuildConfig.LASTFM_API_KEY.isNotBlank(),
                lastFmError = lastFmError,
                onConnectLastFm = connectLastFm,
                onRefreshLastFm = { lastFmRefresh++ },
                onDisconnectLastFm = disconnectLastFm,
                onDownloads = { navigate(Tab.Downloads) },
                onBackup = { backupWriter.launch("VibeArc-backup.json") },
                onRestore = { backupReader.launch(arrayOf("application/json", "text/plain")) },
                onImportPlaylist = {
                    playlistReader.launch(arrayOf("text/*", "audio/x-mpegurl", "application/vnd.apple.mpegurl"))
                },
            )
            Tab.Stats -> StatsScreen(
                padding, library, recentTracks, lastFmSnapshot, lastFmBusy, lastFmError,
                onLastFmRefresh = { lastFmRefresh++ },
                onPlay = { track -> playTrack(track, recentTracks) },
                onSearch = { query -> searchSeed = query; navigate(Tab.Search) },
            )
            Tab.Discover -> DiscoverScreen(
                padding, library, lastFmSnapshot?.topTracks?.firstOrNull(),
                lastFmRecommendations, lastFmRecommendationsBusy,
                onPlay = { track -> playTrack(track, library) },
                onSearch = { query -> searchSeed = query; navigate(Tab.Search) },
            )
            Tab.Generator -> GeneratorScreen(padding) { query ->
                searchSeed = query
                navigate(Tab.Search)
            }
        }
    }
    }
    }
}

internal enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Home("Home", Icons.Default.Home),
    Search("Search", Icons.Default.Search),
    Library("Playlists", Icons.AutoMirrored.Filled.List),
    Stats("Stats", Icons.AutoMirrored.Filled.List),
    Discover("Discover", Icons.Default.Search),
    Generator("Generator", Icons.AutoMirrored.Filled.List),
    Downloads("Downloads", Icons.AutoMirrored.Filled.List),
    Player("Playing", Icons.Default.PlayArrow),
    Settings("Settings", Icons.Default.Settings),
}

internal fun playerReturnTab(candidate: Tab): Tab = if (candidate == Tab.Player) Tab.Home else candidate

private val MainTabs = listOf(Tab.Home, Tab.Stats, Tab.Library)

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
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Offline music", style = MaterialTheme.typography.titleLarge)
                        Text("Ready without a connection", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${tracks.size}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
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
    Box(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = MutedText)
        }
    }
}

@Composable
internal fun PlaylistNameDialog(
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
private fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(30.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackArtwork(track, null, Modifier.size(58.dp).clip(RoundedCornerShape(20.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
                Text("›", fontSize = 32.sp)
            }
        }
    }
}

@Composable
internal fun TrackRow(
    track: Track,
    onPlay: () -> Unit,
    enabled: Boolean = true,
    onFavorite: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingDescription: String = "Track action",
    onTrailingAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onPlay),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
    ) {
    Row(
        Modifier.fillMaxWidth().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(track, null, Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onFavorite == null) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play ${track.title}",
                tint = if (enabled) MaterialTheme.colorScheme.primary else FaintText,
            )
        } else {
            IconButton(onClick = onFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = if (isFavorite) "Remove ${track.title} from favorites" else "Add ${track.title} to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MutedText,
                )
            }
        }
        if (trailingIcon != null) {
            if (onTrailingAction == null) {
                Icon(trailingIcon, contentDescription = trailingDescription, tint = MaterialTheme.colorScheme.primary)
            } else {
                IconButton(onClick = onTrailingAction) {
                    Icon(trailingIcon, contentDescription = trailingDescription)
                }
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
