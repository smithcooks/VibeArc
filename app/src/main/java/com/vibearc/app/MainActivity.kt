package com.vibearc.app

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

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

private data class YouTubeSyncPreview(
    val playlist: YouTubePlaylist,
    val remoteItems: List<YouTubePlaylistItem>,
    val plan: YouTubePlaylistSyncPlan,
    val hasLocalPlaylist: Boolean,
)

private data class TrackActionsHost(
    val playlists: List<Playlist>,
    val isFavorite: (Track) -> Boolean,
    val addToQueue: (Track) -> Unit,
    val toggleFavorite: (Track) -> Unit,
    val addToPlaylist: (Track, String) -> Unit,
    val download: (Track) -> Unit,
    val startSongRadio: (Track) -> Unit,
    val startArtistRadio: (Track) -> Unit,
)

private val LocalTrackActions = staticCompositionLocalOf<TrackActionsHost?> { null }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArtworkCache.initialize(cacheDir)
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
    val cachedYouTubeState = remember { context.loadYouTubeAccountState() }
    val cachedYouTubeBrowse = remember { context.loadYouTubeBrowseSnapshot() }
    var youtubeAccountData by remember { mutableStateOf(cachedYouTubeBrowse?.data) }
    var youtubeAccountBusy by remember { mutableStateOf(false) }
    var showYouTubeLogin by remember { mutableStateOf(false) }
    var youtubeRefresh by remember { mutableIntStateOf(0) }
    var youtubeAccountJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var youtubeSyncBusy by remember { mutableStateOf(false) }
    var youtubeSyncPreview by remember { mutableStateOf<List<YouTubeSyncPreview>?>(null) }
    var confirmRemoteRemoval by remember { mutableStateOf(false) }
    var youtubeSelectedPlaylistIds by remember {
        mutableStateOf(cachedYouTubeState?.selectedPlaylistIds.orEmpty())
    }
    var lastFmConfiguration by remember { mutableStateOf(context.loadLastFmConfiguration()) }
    var lastFmSession by remember { mutableStateOf(context.loadLastFmSession()) }
    var lastFmUsername by remember { mutableStateOf(lastFmSession?.username ?: context.loadLastFmUsername()) }
    var lastFmPendingToken by remember { mutableStateOf(context.loadLastFmPendingToken()) }
    var lastFmSnapshot by remember { mutableStateOf<LastFmSnapshot?>(null) }
    var lastFmRecommendations by remember { mutableStateOf<List<LastFmTrack>>(emptyList()) }
    var lastFmPlayable by remember { mutableStateOf<List<Track>>(emptyList()) }
    var lastFmRecommendationsBusy by remember { mutableStateOf(false) }
    var lastFmRequests by remember { mutableIntStateOf(0) }
    val lastFmBusy = lastFmRequests > 0
    var lastFmError by remember { mutableStateOf<String?>(null) }
    var lastFmRefresh by remember { mutableIntStateOf(0) }
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var youtubeFeedSections by remember { mutableStateOf(cachedYouTubeBrowse?.sections.orEmpty()) }
    var youtubeFeedBusy by remember { mutableStateOf(false) }
    var radioTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var radioBusy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        availableUpdate = withContext(Dispatchers.IO) {
            runCatching { AppUpdateChecker.available(BuildConfig.VERSION_NAME) }.getOrNull()
        }
    }

    LaunchedEffect(lastFmUsername, lastFmRefresh, lastFmConfiguration) {
        val username = lastFmUsername ?: return@LaunchedEffect
        val config = lastFmConfiguration
        if (!config.profileConfigured) {
            lastFmError = "Open Settings → Last.fm to enter your API key or signer configuration."
            return@LaunchedEffect
        }
        lastFmRequests++
        try {
            lastFmError = null
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    if (config.apiKey.isNotBlank()) LastFmApi.load(username, config.apiKey)
                    else LastFmSignerApi.load(config.signerUrl, config.clientToken, username)
                }
            }
            lastFmSnapshot = loaded.getOrNull()
            lastFmRecommendations = emptyList()
            lastFmError = loaded.exceptionOrNull()?.let { it.message?.take(200) ?: "Could not load the Last.fm profile." }
        } finally { lastFmRequests-- }
    }

    val startLastFmAuth: () -> Unit = {
        val config = lastFmConfiguration
        if (!config.signerConfigured) {
            lastFmError = "Enter your HTTPS signer URL and client token in Last.fm settings first."
        } else uiScope.launch {
            lastFmRequests++
            try {
                lastFmError = null
                val result = withContext(Dispatchers.IO) {
                    runCatching { LastFmSignerApi.beginAuthorization(config.signerUrl, config.clientToken) }
                }
                result.getOrNull()?.let { authorization ->
                    context.saveLastFmPendingToken(authorization.token)
                    lastFmPendingToken = authorization.token
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authorization.authorizationUrl))) }
                        .onFailure { lastFmError = "Could not open Last.fm authorization." }
                }
                result.exceptionOrNull()?.let { lastFmError = it.message ?: "Could not start Last.fm authorization." }
            } finally { lastFmRequests-- }
        }
    }
    val finishLastFmAuth: () -> Unit = finish@{
        val token = lastFmPendingToken ?: return@finish
        val config = lastFmConfiguration
        uiScope.launch {
            lastFmRequests++
            try {
                lastFmError = null
                val result = withContext(Dispatchers.IO) {
                    runCatching { LastFmSignerApi.completeAuthorization(config.signerUrl, config.clientToken, token) }
                }
                result.getOrNull()?.let { session ->
                    context.saveLastFmSession(session)
                    lastFmSession = session
                    lastFmUsername = session.username
                    lastFmPendingToken = null
                    lastFmRefresh++
                }
                result.exceptionOrNull()?.let { lastFmError = it.message ?: "Finish authorization in the browser, then try again." }
            } finally { lastFmRequests-- }
        }
    }
    val disconnectLastFm: () -> Unit = {
        context.saveLastFmSession(null)
        context.saveLastFmUsername(null)
        lastFmSession = null
        lastFmPendingToken = null
        lastFmUsername = null
        lastFmSnapshot = null
        lastFmRecommendations = emptyList()
        lastFmError = null
    }

    fun loadYouTubeAccount(announce: Boolean) {
        youtubeAccountJob?.cancel()
        if (!YouTubeWebSession.isAuthenticated()) {
            youtubeAccountBusy = false
            if (announce) android.widget.Toast.makeText(context, "YouTube Music sign-in was not completed", android.widget.Toast.LENGTH_LONG).show()
        } else uiScope.launch {
            youtubeAccountBusy = true
            val loaded = withContext(Dispatchers.IO) {
                runCatching { YouTubeMusicSessionApi.load() }.getOrNull()
            }
            youtubeAccountBusy = false
            if (loaded == null) {
                if (announce) android.widget.Toast.makeText(context, "Could not load your YouTube account", android.widget.Toast.LENGTH_LONG).show()
            } else {
                val saved = context.loadYouTubeAccountState()
                youtubeSelectedPlaylistIds = saved?.selectedPlaylistIds
                    ?.takeIf { saved.account.channelId == loaded.account.channelId }.orEmpty()
                val previousAccount = youtubeAccountData?.account?.channelId
                if (previousAccount != null && previousAccount != loaded.account.channelId) {
                    youtubeFeedSections = emptyList()
                    youtubeRefresh++
                }
                youtubeAccountData = loaded
                context.saveYouTubeAccountState(YouTubeAccountState(loaded.account, youtubeSelectedPlaylistIds))
                if (announce) android.widget.Toast.makeText(context, "Connected ${loaded.account.displayName}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }.also { youtubeAccountJob = it }
    }

    val connectYouTube: () -> Unit = {
        showYouTubeLogin = true
    }

    fun disconnectYouTube(reconnect: Boolean) {
        youtubeAccountJob?.cancel()
        youtubeAccountBusy = true
        YouTubeWebSession.clear {
            uiScope.launch {
                youtubeAccountData = null
                youtubeFeedSections = emptyList()
                youtubeRefresh++
                youtubeSelectedPlaylistIds = emptySet()
                context.clearYouTubeAccountState()
                youtubeAccountBusy = false
                if (reconnect) connectYouTube()
                else android.widget.Toast.makeText(context, "YouTube Music account disconnected", android.widget.Toast.LENGTH_SHORT).show()
                withContext(Dispatchers.IO) { YouTubeMusicSessionApi.clearConfig() }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (YouTubeWebSession.isAuthenticated()) loadYouTubeAccount(announce = false)
        else {
            youtubeAccountBusy = false
        }
    }

    LaunchedEffect(youtubeRefresh) {
        if (!YouTubeWebSession.isAuthenticated()) return@LaunchedEffect
        youtubeFeedBusy = true
        try {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { YouTubeMusicSessionApi.loadHomeFeed() }.getOrNull()
            }
            if (!loaded.isNullOrEmpty()) youtubeFeedSections = loaded
        } finally {
            youtubeFeedBusy = false
        }
    }

    LaunchedEffect(youtubeAccountData, youtubeFeedSections) {
        youtubeAccountData?.let { data ->
            val snapshot = YouTubeBrowseSnapshot(data, youtubeFeedSections)
            val encoded = withContext(Dispatchers.IO) { YouTubeBrowseCacheCodec.encode(snapshot) }
            context.saveYouTubeBrowseSnapshot(encoded)
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
    var offlineFolder by remember { mutableStateOf(context.loadOfflineFolder()) }
    var offlineFiles by remember { mutableStateOf(context.loadOfflineFiles()) }
    var offlineCopyTrack by remember { mutableStateOf<Track?>(null) }
    var offlineCopyProgress by remember { mutableStateOf(0f) }
    var offlineCopyError by remember { mutableStateOf<Track?>(null) }
    var offlineCancelSignal by remember { mutableStateOf<AtomicBoolean?>(null) }
    val offlineDownloads = remember(context) { OfflineDownloads.get(context) }
    LaunchedEffect(offlineDownloads) { withContext(Dispatchers.IO) { runCatching { offlineDownloads.load() } } }

    LaunchedEffect(lastFmSnapshot) {
        if (lastFmRecommendations.isNotEmpty()) return@LaunchedEffect
        val seed = lastFmSnapshot?.topTracks?.firstOrNull() ?: return@LaunchedEffect
        lastFmRecommendationsBusy = true
        lastFmRecommendations = withContext(Dispatchers.IO) {
            runCatching {
                if (lastFmConfiguration.apiKey.isNotBlank()) LastFmApi.similarTracks(seed, lastFmConfiguration.apiKey)
                else LastFmSignerApi.similarTracks(lastFmConfiguration.signerUrl, lastFmConfiguration.clientToken, seed)
            }.getOrDefault(emptyList())
        }
        lastFmRecommendationsBusy = false
    }
    LaunchedEffect(lastFmRecommendations) {
        lastFmPlayable = withContext(Dispatchers.IO) {
            lastFmRecommendations.take(4).mapNotNull { recommendation ->
                runCatching { OnlineMusic.search("${recommendation.artist} ${recommendation.title}").firstOrNull() }
                    .getOrNull()
            }
        }
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
            currentTrack = restored.copy(isFavorite=library.firstOrNull { it.catalogUri==restored.catalogUri }?.isFavorite ?: restored.isFavorite)
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
                    currentTrack = track.copy(isFavorite=library.firstOrNull { it.catalogUri==track.catalogUri }?.isFavorite ?: track.isFavorite)
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

    val offlineFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val saved = runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }.isSuccess
        if (saved) {
            context.saveOfflineFolder(uri)
            offlineFolder = uri
        } else {
            android.widget.Toast.makeText(context, "This folder could not be saved", android.widget.Toast.LENGTH_LONG).show()
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
            val saved = (if (isYouTubeWatchUri(track.catalogUri)) track.copy(uri = track.catalogUri, sourceUri = "") else track).withoutPlayCounts()
            library = library.upsert(saved).also(context::saveLibrary)
            fun withSavedArtwork(song: Track): Track {
                val art=offlineDownloads.entries.value.firstOrNull { it.id==downloadIdentity(song) && it.status==DownloadStatus.COMPLETED }?.artworkUri
                return (if(art.isNullOrBlank()) song else song.copy(artworkUri=art)).withoutPlayCounts()
            }
            val displayed=withSavedArtwork(track)
            currentTrack = displayed
            activePlayer.loadQueue(source.map(::withSavedArtwork), displayed)
            queueTracks = activePlayer.queueTracks()
            openPlayer()
        } else if (activePlayer == null) {
            android.widget.Toast.makeText(context, "Player is connecting. Tap play again in a moment.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val toggleFavorite: (Track) -> Unit = { track ->
        library = library.toggleFavorite(track).also(context::saveLibrary)
        library.firstOrNull { it.uri == track.uri }?.let { updated ->
            if (currentTrack?.uri == updated.uri) currentTrack = updated
        }
    }
    val updatePlaylists: (List<Playlist>) -> Unit = { next ->
        playlists = next.also(context::savePlaylists)
    }
    val recentTracks = recentUris.mapNotNull { mediaId -> library.firstOrNull { it.uri == mediaId } }
    val onlineHomeTracks = youtubeFeedSections.flatMap(YouTubeFeedSection::tracks)
    val discoveryTracks = remember(onlineHomeTracks, lastFmPlayable, recentTracks, library) {
        blendDiscoveryTracks(onlineHomeTracks, lastFmPlayable, recentTracks, library)
    }
    fun startRadio(track: Track, artistOnly: Boolean) {
        if (radioBusy) return
        radioBusy = true
        uiScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                var candidates = if (!artistOnly && YouTubeWebSession.isAuthenticated()) {
                    runCatching { YouTubeMusicSessionApi.loadRadio(track) }.getOrDefault(emptyList())
                } else emptyList()
                if (candidates.isEmpty()) {
                    val query = if (artistOnly) track.artist else "${track.artist} ${track.title} radio"
                    candidates = runCatching { OnlineMusic.search(query) }.getOrDefault(emptyList())
                }
                candidates.filterNot { it.catalogUri == track.catalogUri }.distinctBy(Track::catalogUri).take(40)
            }
            radioBusy = false
            radioTracks = loaded
            if (loaded.isEmpty()) {
                android.widget.Toast.makeText(context, "No radio tracks were found", android.widget.Toast.LENGTH_LONG).show()
            } else {
                playTrack(loaded.first(), loaded)
            }
        }
    }
    fun copyTracksOffline(requested: List<Track>, showDownloads: Boolean = true) {
        if(requested.isEmpty()) return
        library = requested.fold(library) { saved,track -> saved.upsert(track) }.also(context::saveLibrary)
        if (showDownloads) navigate(Tab.Downloads)
        uiScope.launch {
            val result=withContext(Dispatchers.IO) { runCatching { offlineDownloads.enqueue(requested) } }
            if(result.isSuccess) runCatching { DownloadService.start(context) }.onFailure {
                android.widget.Toast.makeText(context,"Tap Retry in Downloads to start the transfer",android.widget.Toast.LENGTH_LONG).show()
            } else android.widget.Toast.makeText(context,"Could not queue downloads; saved files were preserved",android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val copyTrackOffline: (Track) -> Unit = { copyTracksOffline(listOf(it)) }
    val addTrackToQueue: (Track) -> Unit = { track ->
        when {
            activePlayer == null -> android.widget.Toast.makeText(context, "Player is not ready", android.widget.Toast.LENGTH_SHORT).show()
            !isAllowedMediaUri(track.uri) -> android.widget.Toast.makeText(context, "This song is not ready to queue", android.widget.Toast.LENGTH_SHORT).show()
            else -> {
                activePlayer.addMediaItem(track.toMediaItem())
                if (activePlayer.playbackState == Player.STATE_IDLE) activePlayer.prepare()
                queueTracks = activePlayer.queueTracks()
                android.widget.Toast.makeText(context, "Added to queue", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val addTrackToPlaylist: (Track, String) -> Unit = { track, playlistId ->
        library = library.upsert(track).also(context::saveLibrary)
        updatePlaylists(playlists.addTrackToPlaylist(playlistId, track.uri))
        android.widget.Toast.makeText(context, "Added to playlist", android.widget.Toast.LENGTH_SHORT).show()
    }
    val downloadFromTrackMenu: (Track) -> Unit = { track ->
        copyTrackOffline(track)
    }
    fun mergeYouTubePlaylist(remotePlaylist: YouTubePlaylist, remoteTracks: List<Track>): Int {
        val id = "youtube:${remotePlaylist.id}"
        val currentUris = playlists.firstOrNull { it.id == id }?.trackUris.orEmpty()
        val diff = previewYouTubePlaylistSync(currentUris, remoteTracks)
        library = remoteTracks.fold(library) { current, track -> current.upsert(track) }.also(context::saveLibrary)
        val localPlaylist = Playlist(id, remotePlaylist.title, (currentUris + remoteTracks.map(Track::uri)).distinct())
        playlists = (playlists.filterNot { it.id == id } + localPlaylist).also(context::savePlaylists)
        return diff.remoteOnlyTracks.size
    }

    fun replaceLocalWithYouTubePlaylist(remotePlaylist: YouTubePlaylist, remoteTracks: List<Track>) {
        val id = "youtube:${remotePlaylist.id}"
        library = remoteTracks.fold(library) { current, track -> current.upsert(track) }.also(context::saveLibrary)
        playlists = (playlists.filterNot { it.id == id } +
            Playlist(id, remotePlaylist.title, remoteTracks.map(Track::uri).distinct())).also(context::savePlaylists)
    }

    val importYouTubePlaylist: (YouTubePlaylist) -> Unit = { remotePlaylist ->
        if (!YouTubeWebSession.isAuthenticated()) {
            android.widget.Toast.makeText(context, "Connect your YouTube account again", android.widget.Toast.LENGTH_LONG).show()
        } else uiScope.launch {
            val importedTracks = withContext(Dispatchers.IO) {
                runCatching { YouTubeMusicSessionApi.loadPlaylist(remotePlaylist) }.getOrNull()
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
    val previewSelectedYouTubePlaylists: () -> Unit = {
        val selected = youtubeAccountData?.playlists.orEmpty()
            .filter { it.id in youtubeSelectedPlaylistIds }
        if (!YouTubeWebSession.isAuthenticated()) {
            android.widget.Toast.makeText(context, "Connect your YouTube account again", android.widget.Toast.LENGTH_LONG).show()
        } else if (selected.isEmpty()) {
            android.widget.Toast.makeText(context, "Select at least one playlist", android.widget.Toast.LENGTH_SHORT).show()
        } else if (!youtubeSyncBusy) uiScope.launch {
            youtubeSyncBusy = true
            val loaded = withContext(Dispatchers.IO) {
                selected.mapNotNull { playlist ->
                    runCatching { YouTubeMusicSessionApi.loadPlaylistItems(playlist) }
                        .getOrNull()?.let { items ->
                            val local = playlists.firstOrNull { it.id == "youtube:${playlist.id}" }
                            YouTubeSyncPreview(
                                playlist,
                                items,
                                planYouTubePlaylistSync(
                                    local?.trackUris ?: items.map { it.track.uri },
                                    items,
                                ),
                                local != null,
                            )
                        }
                }
            }
            youtubeSyncBusy = false
            val failures = selected.size - loaded.size
            if (loaded.isNotEmpty()) youtubeSyncPreview = loaded
            if (failures > 0) {
                android.widget.Toast.makeText(
                    context,
                    "$failures playlist${if (failures == 1) "" else "s"} could not be compared",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    val createYouTubePlaylist: (String) -> Unit = { title ->
        if (!YouTubeWebSession.isAuthenticated()) {
            android.widget.Toast.makeText(context, "Connect your YouTube account again", android.widget.Toast.LENGTH_LONG).show()
        } else if (!youtubeSyncBusy) uiScope.launch {
            youtubeSyncBusy = true
            val createdId = withContext(Dispatchers.IO) {
                runCatching { YouTubeMusicSessionApi.createPlaylist(title) }.getOrNull()
            }
            youtubeSyncBusy = false
            if (createdId == null) {
                android.widget.Toast.makeText(context, "Could not create the YouTube playlist", android.widget.Toast.LENGTH_LONG).show()
            } else {
                val created = YouTubePlaylist(createdId, title.trim(), 0, "")
                youtubeAccountData = youtubeAccountData?.copy(playlists = youtubeAccountData!!.playlists + created)
                youtubeSelectedPlaylistIds = youtubeSelectedPlaylistIds + createdId
                updatePlaylists(playlists.createPlaylist(created.title, "youtube:$createdId"))
                youtubeAccountData?.account?.let { context.saveYouTubeAccountState(YouTubeAccountState(it, youtubeSelectedPlaylistIds)) }
                android.widget.Toast.makeText(context, "Created ${created.title}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun applyYouTubeSync(useLocalAsSource: Boolean) {
        if (!YouTubeWebSession.isAuthenticated()) return
        val previews = youtubeSyncPreview ?: return
        uiScope.launch {
            youtubeSyncBusy = true
            val failures = withContext(Dispatchers.IO) {
                previews.sumOf { preview ->
                    var failed = 0
                    preview.plan.addVideoIds.forEach { videoId ->
                        if (runCatching {
                                YouTubeMusicSessionApi.addVideoToPlaylist(preview.playlist.id, videoId)
                            }.isFailure) failed++
                    }
                    if (useLocalAsSource && preview.hasLocalPlaylist) {
                        preview.plan.removeItemIds.forEach { itemId ->
                            if (runCatching {
                                    YouTubeMusicSessionApi.removePlaylistItem(preview.playlist.id, itemId)
                                }.isFailure) failed++
                        }
                    }
                    failed
                }
            }
            if (!useLocalAsSource) {
                previews.forEach { preview ->
                    mergeYouTubePlaylist(preview.playlist, preview.remoteItems.map(YouTubePlaylistItem::track))
                }
            }
            youtubeSyncBusy = false
            youtubeSyncPreview = null
            confirmRemoteRemoval = false
            android.widget.Toast.makeText(
                context,
                if (failures == 0) "Playlist sync complete" else "Playlist sync finished with $failures failed change${if (failures == 1) "" else "s"}",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    val trackActions = TrackActionsHost(
        playlists = playlists,
        isFavorite = { track -> library.any { it.uri == track.uri && it.isFavorite } },
        addToQueue = addTrackToQueue,
        toggleFavorite = toggleFavorite,
        addToPlaylist = addTrackToPlaylist,
        download = downloadFromTrackMenu,
        startSongRadio = { startRadio(it, artistOnly = false) },
        startArtistRadio = { startRadio(it, artistOnly = true) },
    )

    VibeArcTheme(appearance, artworkAccentArgb) {
    CompositionLocalProvider(
        LocalGlass provides appearance.liquidGlassEnabled,
        LocalTrackActions provides trackActions,
    ) {
    Scaffold(
        topBar = {
            if (currentTab !in listOf(Tab.Player, Tab.Search, Tab.Library, Tab.Downloads)) ReferenceHeader(
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
                onlineSections = youtubeFeedSections,
                onlineBusy = youtubeFeedBusy,
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
                onRenamePlaylist = { id, name ->
                    val remoteId = id.takeIf { it.startsWith("youtube:") }?.removePrefix("youtube:")
                    val remote = youtubeAccountData?.playlists?.firstOrNull { it.id == remoteId }
                    if (remote == null) updatePlaylists(playlists.renamePlaylist(id, name))
                    else if (!YouTubeWebSession.isAuthenticated()) android.widget.Toast.makeText(context,"Reconnect YouTube before renaming",android.widget.Toast.LENGTH_LONG).show()
                    else uiScope.launch {
                        youtubeSyncBusy = true
                        val changed = withContext(Dispatchers.IO) { runCatching { YouTubeMusicSessionApi.renamePlaylist(remote.id,name) }.isSuccess }
                        youtubeSyncBusy = false
                        if (changed) {
                            updatePlaylists(playlists.renamePlaylist(id,name))
                            youtubeAccountData = youtubeAccountData?.copy(playlists=youtubeAccountData!!.playlists.map { if(it.id==remote.id) it.copy(title=name.trim()) else it })
                        } else android.widget.Toast.makeText(context,"Could not rename the YouTube playlist",android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onDeletePlaylist = { id ->
                    val remoteId = id.takeIf { it.startsWith("youtube:") }?.removePrefix("youtube:")
                    if (remoteId == null) updatePlaylists(playlists.deletePlaylist(id))
                    else if (!YouTubeWebSession.isAuthenticated()) android.widget.Toast.makeText(context,"Reconnect YouTube before deleting",android.widget.Toast.LENGTH_LONG).show()
                    else uiScope.launch {
                        youtubeSyncBusy = true
                        val deleted = withContext(Dispatchers.IO) { runCatching { YouTubeMusicSessionApi.deletePlaylist(remoteId) }.isSuccess }
                        youtubeSyncBusy = false
                        if (deleted) {
                            updatePlaylists(playlists.deletePlaylist(id))
                            youtubeAccountData = youtubeAccountData?.copy(playlists=youtubeAccountData!!.playlists.filterNot { it.id==remoteId })
                            youtubeSelectedPlaylistIds = youtubeSelectedPlaylistIds - remoteId
                            youtubeAccountData?.account?.let { context.saveYouTubeAccountState(YouTubeAccountState(it,youtubeSelectedPlaylistIds)) }
                        } else android.widget.Toast.makeText(context,"Could not delete the YouTube playlist",android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onAddToPlaylist = { id, uri -> updatePlaylists(playlists.addTrackToPlaylist(id, uri)) },
                onRemoveFromPlaylist = { id, uri -> updatePlaylists(playlists.removeTrackFromPlaylist(id, uri)) },
                onDownloadAll = { copyTracksOffline(it) },
                onDownloads = { navigate(Tab.Downloads) },
            )
            Tab.Downloads -> OfflineDownloadsScreen(
                padding = padding,
                onPlay = playTrack,
                onBack = navigateBack,
                onSettings = { navigate(Tab.Settings) },
            )
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
                onDownload = {
                    val selected = currentTrack!!
                    if (offlineDownloads.entries.value.any { it.id == downloadIdentity(selected) && it.status == DownloadStatus.COMPLETED }) navigate(Tab.Downloads)
                    else copyTracksOffline(listOf(selected), showDownloads = false)
                },
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
                lyricsAnimationEnabled = appearance.lyricsAnimationEnabled,
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
                onPullSelectedYouTubePlaylists = previewSelectedYouTubePlaylists,
                onCreateYouTubePlaylist = createYouTubePlaylist,
                onImportYouTubePlaylist = importYouTubePlaylist,
                lastFmUsername = lastFmUsername,
                lastFmBusy = lastFmBusy,
                lastFmConfiguration = lastFmConfiguration,
                lastFmAuthenticated = lastFmSession != null,
                lastFmAuthorizationPending = lastFmPendingToken != null,
                lastFmTracks = library,
                lastFmError = lastFmError,
                onStartLastFmAuth = startLastFmAuth,
                onFinishLastFmAuth = finishLastFmAuth,
                onRefreshLastFm = { lastFmRefresh++ },
                onDisconnectLastFm = disconnectLastFm,
                onSaveLastFmSetup = { config, username ->
                    if (config != lastFmConfiguration || username != lastFmUsername) {
                        context.saveLastFmSession(null)
                        lastFmSession = null
                        lastFmPendingToken = null
                        lastFmSnapshot = null
                        lastFmRecommendations = emptyList()
                    }
                    context.saveLastFmConfiguration(config)
                    context.saveLastFmUsername(username)
                    lastFmConfiguration = config
                    lastFmUsername = username
                    lastFmRefresh++
                },
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
                padding, discoveryTracks, lastFmSnapshot?.topTracks?.firstOrNull(),
                lastFmPlayable, lastFmRecommendationsBusy,
                onPlay = { track -> playTrack(track, discoveryTracks) },
                onSearch = { query -> searchSeed = query; navigate(Tab.Search) },
            )
            Tab.Generator -> GeneratorScreen(
                padding, library, recentTracks, discoveryTracks, radioTracks, currentTrack, radioBusy,
                onPlay = { queue -> playTrack(queue.first(), queue) },
                onRadio = { currentTrack?.let { startRadio(it, artistOnly = false) } },
                onSearch = { query -> searchSeed = query; navigate(Tab.Search) },
            )
        }
    }
    youtubeSyncPreview?.let { previews ->
        val additions = previews.sumOf { it.plan.addVideoIds.size }
        val removals = previews.filter(YouTubeSyncPreview::hasLocalPlaylist).sumOf { it.plan.removeItemIds.size }
        val unsupported = previews.sumOf { it.plan.unsupportedLocalUris.size }
        AlertDialog(
            onDismissRequest = { if (!youtubeSyncBusy) youtubeSyncPreview = null },
            title = { Text("Review playlist sync") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${previews.size} playlist${if (previews.size == 1) "" else "s"} compared")
                    Text("Add $additions VibeArc track${if (additions == 1) "" else "s"} to YouTube")
                    Text("$removals YouTube-only track${if (removals == 1) "" else "s"} conflict with the local copy")
                    if (unsupported > 0) Text("$unsupported local file${if (unsupported == 1) "" else "s"} cannot be uploaded")
                    Text("Keep both adds missing tracks in both places. Use YouTube replaces the local playlist. Use VibeArc can remove YouTube-only items after another confirmation.")
                }
            },
            confirmButton = {
                TextButton(onClick = { applyYouTubeSync(useLocalAsSource = false) }, enabled = !youtubeSyncBusy) {
                    Text("Keep both")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        previews.forEach { preview ->
                            replaceLocalWithYouTubePlaylist(preview.playlist, preview.remoteItems.map(YouTubePlaylistItem::track))
                        }
                        youtubeSyncPreview = null
                    }, enabled = !youtubeSyncBusy) { Text("Use YouTube") }
                    TextButton(onClick = { confirmRemoteRemoval = true }, enabled = !youtubeSyncBusy) { Text("Use VibeArc") }
                }
            },
        )
    }
    if (confirmRemoteRemoval) AlertDialog(
        onDismissRequest = { confirmRemoteRemoval = false },
        title = { Text("Remove tracks from YouTube?") },
        text = { Text("This permanently removes YouTube-only items from the selected remote playlists. It does not delete videos or local audio files.") },
        confirmButton = {
            TextButton(onClick = { applyYouTubeSync(useLocalAsSource = true) }, enabled = !youtubeSyncBusy) {
                Text("Remove and sync")
            }
        },
        dismissButton = { TextButton(onClick = { confirmRemoteRemoval = false }) { Text("Cancel") } },
    )
    if (showYouTubeLogin) YouTubeLoginSheet(
        onDismiss = { showYouTubeLogin = false },
        onConnected = {
            showYouTubeLogin = false
            uiScope.launch {
                withContext(Dispatchers.IO) { YouTubeMusicSessionApi.clearConfig() }
                loadYouTubeAccount(announce = true)
                youtubeRefresh++
            }
        },
    )
    availableUpdate?.let { update -> UpdateDialog(update) { availableUpdate = null } }
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
    offlineFolder: Uri?,
    offlineFiles: List<OfflineFile>,
    copyingTrack: Track?,
    copyProgress: Float,
    failedTrack: Track?,
    onChooseFolder: () -> Unit,
    onCopy: (Track) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: (OfflineFile) -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
) {
    val downloadableTracks = tracks.filter(Track::canCopyOffline)
    val totalBytes = offlineFiles.sumOf(OfflineFile::sizeBytes)
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
                        Text("${offlineFiles.size} saved · ${totalBytes / (1024 * 1024)} MB", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = onChooseFolder) { Text(if (offlineFolder == null) "Choose folder" else "Change folder") }
                }
            }
        }
        item {
            Text(
                "Save local audio or an available online source in the format and quality selected in Settings.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        copyingTrack?.let { track ->
            item {
                ReferenceSurface {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Saving ${track.title}", fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { copyProgress }, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = onCancel) { Text("Cancel") }
                    }
                }
            }
        }
        failedTrack?.let { track -> item { Button(onClick = onRetry) { Text("Retry ${track.title}") } } }
        if (offlineFiles.isNotEmpty()) {
            item { Text("Saved files", style = MaterialTheme.typography.titleLarge) }
            items(offlineFiles, key = OfflineFile::uri) { file ->
                ReferenceSurface {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(file.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOf("${file.sizeBytes / 1024} KB",file.qualityLabel).filter(String::isNotBlank).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDelete(file) }) { Icon(Icons.Default.Delete, "Delete saved file") }
                    }
                }
            }
        }
        item { Text("Available to download", style = MaterialTheme.typography.titleLarge) }
        if (downloadableTracks.isEmpty()) {
            item { EmptyLibraryCard("No downloadable audio", "Add local audio or import a YouTube Music playlist first.") }
        } else {
            items(downloadableTracks, key = Track::uri) { track ->
                ReferenceSurface {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable(enabled=Uri.parse(track.uri).scheme in setOf("content","file")) { onPlay(track, downloadableTracks) }) {
                            Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        TextButton(onClick = { onCopy(track) }, enabled = offlineFolder != null && copyingTrack == null) { Text("Save") }
                    }
                }
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun TrackRow(
    track: Track,
    onPlay: () -> Unit,
    enabled: Boolean = true,
    playableTrack: Track? = track,
    onFavorite: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingDescription: String = "Track action",
    onTrailingAction: (() -> Unit)? = null,
) {
    val actions = LocalTrackActions.current
    var showActions by remember(track.uri) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            enabled = enabled || actions != null,
            onClick = { if (enabled) onPlay() },
            onLongClick = actions?.let { { showActions = true } },
            onLongClickLabel = "Song options",
        ),
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

    if (showActions) SongOptionsSheet(track, playableTrack) { showActions = false }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SongActionTarget(track: Track?, onClick: () -> Unit, modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    val actions = LocalTrackActions.current
    var showActions by remember(track?.catalogUri) { mutableStateOf(false) }
    Box(modifier.combinedClickable(onClick = onClick,
        onLongClick = if (track != null && actions != null) ({ showActions = true }) else null,
        onLongClickLabel = "Song options"), content = content)
    if (showActions && track != null) SongOptionsSheet(track, track) { showActions = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongOptionsSheet(track: Track, playableTrack: Track?, onDismiss: () -> Unit) {
    val actions = LocalTrackActions.current ?: return
    val downloadContext=LocalContext.current
    val downloadStore=remember(downloadContext) { OfflineDownloads.get(downloadContext) }
    val downloads by downloadStore.entries.collectAsState()
    val download=downloads.firstOrNull { it.id==downloadIdentity(track) }
    var showPlaylists by remember(track.catalogUri) { mutableStateOf(false) }
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    if (showPlaylists) "Add to playlist" else track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                )
                if (showPlaylists) {
                    if (actions.playlists.isEmpty()) {
                        Text("Create a playlist in Library first.", color = MutedText, modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(actions.playlists, key = Playlist::id) { playlist ->
                                ReferenceRow(playlist.name, "${playlist.trackUris.size} tracks", "playlist") {
                                    actions.addToPlaylist(track, playlist.id)
                                    onDismiss()
                                }
                            }
                        }
                    }
                } else {
                    ReferenceRow("Add to queue", "Play after the current queue", "music", 0, 6, enabled = playableTrack != null) {
                        playableTrack?.let(actions.addToQueue)
                        onDismiss()
                    }
                    ReferenceRow("Add to playlist", "Choose one of your playlists", "playlist", 1, 6) {
                        showPlaylists = true
                    }
                    val liked = actions.isFavorite(track)
                    ReferenceRow(if (liked) "Unlike song" else "Like song", "Save in your library", if (liked) "heartFilled" else "heart", 2, 6) {
                        actions.toggleFavorite(track)
                        onDismiss()
                    }
                    ReferenceRow("Start song radio", "Build a queue around this track", "shuffle", 3, 6, enabled = playableTrack != null) {
                        playableTrack?.let(actions.startSongRadio)
                        onDismiss()
                    }
                    ReferenceRow("Start artist radio", "Play more from related artist searches", "account", 4, 6, enabled = playableTrack != null) {
                        playableTrack?.let(actions.startArtistRadio)
                        onDismiss()
                    }
                    ReferenceRow(
                        when(download?.status) {
                            DownloadStatus.COMPLETED -> "Downloaded"
                            DownloadStatus.RESOLVING -> "Resolving audio source"
                            DownloadStatus.DOWNLOADING -> "Downloading ${(download.progress*100).toInt()}%"
                            DownloadStatus.QUEUED -> "Download queued"
                            DownloadStatus.PAUSED -> "Download paused"
                            else -> "Download song"
                        },
                        if(download?.status==DownloadStatus.COMPLETED) "Open Downloads to play or remove the saved copy"
                        else if (track.uri.startsWith("https://")) "Save playback audio · manage progress in Downloads" else "Use the original phone file without duplicating it",
                        "download",
                        5,
                        6,
                    ) {
                        actions.download(track)
                        onDismiss()
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
    val startIndex = queue.indexOfFirst { it.catalogUri == startTrack.catalogUri }
    setMediaItems(queue.map(Track::toMediaItem), startIndex, 0L)
    prepare()
    if (playNow) play()
}

private fun Player.toggle() {
    if (isPlaying) pause() else {
        if (playbackState == Player.STATE_IDLE) prepare()
        play()
    }
}

private fun Player.queueTracks(): List<Track> =
    (0 until mediaItemCount).map { index -> getMediaItemAt(index).track }

private fun Track.toMediaItem(): MediaItem {
    require(isAllowedMediaUri(uri)) { "Unsupported media URI" }
    val metadata = MediaMetadata.Builder()
        .setTitle(cleanRecordingLabel(title))
        .setArtist(cleanRecordingLabel(artist))
        .setAlbumTitle(album)
        .setExtras(Bundle().apply {
            putLong("durationMs", durationMs)
            putString("sourceUri", catalogUri)
            putString("folder", folder)
        })
        .apply {
            artworkUri.takeIf(String::isNotBlank)?.let { setArtworkUri(Uri.parse(it)) }
        }
        .build()
    return MediaItem.Builder()
        .setMediaId(catalogUri)
        .setUri(Uri.parse(playbackSourceUri(this)))
        .setMediaMetadata(metadata)
        .build()
}

private val MediaItem.track: Track
    get() = Track(
        title = mediaMetadata.title?.toString() ?: "Unknown track",
        artist = mediaMetadata.artist?.toString() ?: "On this device",
        album = mediaMetadata.albumTitle?.toString() ?: "Imported",
        uri = localConfiguration?.uri?.toString().orEmpty().ifBlank { mediaId },
        durationMs = mediaMetadata.extras?.getLong("durationMs") ?: 0L,
        artworkUri = mediaMetadata.artworkUri?.toString().orEmpty(),
        folder = mediaMetadata.extras?.getString("folder").orEmpty().ifBlank { "Imported" },
        sourceUri = mediaMetadata.extras?.getString("sourceUri").orEmpty().ifBlank { mediaId },
    ).withoutPlayCounts()

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
