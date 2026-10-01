package com.vibearc.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    padding: PaddingValues, appearance: AppearanceConfig,
    onAppearanceChange: (AppearanceConfig) -> Unit, onDownloads: () -> Unit,
    youtubeAccountData: YouTubeAccountData?, youtubeAccountBusy: Boolean, youtubeSyncBusy: Boolean,
    youtubeSelectedPlaylistIds: Set<String>,
    onConnectYouTube: () -> Unit, onDisconnectYouTube: () -> Unit,
    onSwitchYouTubeAccount: () -> Unit,
    onToggleYouTubePlaylistSync: (String) -> Unit,
    onPullSelectedYouTubePlaylists: () -> Unit,
    onCreateYouTubePlaylist: (String) -> Unit,
    onImportYouTubePlaylist: (YouTubePlaylist) -> Unit,
    lastFmUsername: String?, lastFmBusy: Boolean, lastFmConfigured: Boolean,
    lastFmAuthenticated: Boolean, lastFmAuthorizationPending: Boolean, lastFmError: String?,
    lastFmTracks: List<Track>,
    onStartLastFmAuth: () -> Unit, onFinishLastFmAuth: () -> Unit,
    onRefreshLastFm: () -> Unit, onDisconnectLastFm: () -> Unit,
    onBackup: () -> Unit, onRestore: () -> Unit,
    onImportPlaylist: () -> Unit,
) {
    val context=LocalContext.current
    var info by remember { mutableStateOf<Pair<String,String>?>(null) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var streamFormat by remember { mutableStateOf(context.streamAudioFormat()) }
    var streamQuality by remember { mutableStateOf(context.streamAudioQuality()) }
    var downloadFormat by remember { mutableStateOf(context.downloadAudioFormat()) }
    var downloadQuality by remember { mutableStateOf(context.downloadAudioQuality()) }
    var scrobblingEnabled by remember { mutableStateOf(context.lastFmScrobblingEnabled()) }
    var excludedScrobbleUris by remember { mutableStateOf(context.loadLastFmExcludedUris()) }
    var wavySeekbar by remember { mutableStateOf(context.wavySeekbarEnabled()) }
    var audioTuning by remember { mutableStateOf(context.loadAudioTuning()) }
    var selectedIcon by remember { mutableStateOf(context.selectedLauncherIcon()) }
    var custom by remember { mutableStateOf("#%06X".format(appearance.customAccentArgb and 0xFFFFFF)) }
    var youtubePlaylistTitle by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val unavailable: (String,String)->Unit = { title,description -> info=title to description }
    fun updateAudioTuning(value: AudioTuning) {
        audioTuning=value
        context.saveAudioTuning(value)
    }
    fun syncInfo() { unavailable("YouTube Music history","Google does not provide a supported YouTube Data API method for writing listening history, so VibeArc cannot safely enable this switch.") }
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=22.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        item { ReferenceRow("VibeArc","Local listening profile","account",onClick={unavailable("Your profile","Your library and appearance choices are stored on this device.")}) }
        item { SettingsHeading("YouTube Music") }
        item { ReferenceRow("YouTube Music Account",when { youtubeAccountBusy -> "Connecting…"; youtubeAccountData != null -> youtubeAccountData.account.displayName; else -> "Tap to connect Google" },"account",0,6,enabled=!youtubeAccountBusy,onClick=if(youtubeAccountData == null) onConnectYouTube else ({sheet="YouTube Account"})) }
        item { ReferenceRow("Playlist Sync",when { youtubeAccountData == null -> "Requires a connected account"; youtubeSyncBusy -> "Synchronizing selected playlists…"; youtubeSelectedPlaylistIds.isEmpty() -> "Select playlists first"; else -> "Review ${youtubeSelectedPlaylistIds.size} selected before syncing" },"playlist",1,6,enabled=!youtubeSyncBusy,onClick=if(youtubeAccountData == null) onConnectYouTube else onPullSelectedYouTubePlaylists) }
        item { ReferenceRow("Select Playlists to Sync",when { youtubeAccountData == null -> "No account playlists available"; youtubeSelectedPlaylistIds.isEmpty() -> "None selected"; else -> "${youtubeSelectedPlaylistIds.size} selected" },"playlist",2,6,onClick=if(youtubeAccountData == null) onConnectYouTube else ({sheet="Sync Playlists"})) }
        item { ReferenceRow("YouTube Playlists Shown",if(youtubeAccountData == null) "No account connected" else "${youtubeAccountData.playlists.size} account playlists","album",3,6,onClick={if(youtubeAccountData == null) onConnectYouTube else ({sheet="YouTube Playlists"})}) }
        item { ReferenceRow("Make YouTube Playlists Local",if(youtubeAccountData == null) "Connect an account first" else "Choose a playlist to import","playlist",4,6,onClick={if(youtubeAccountData == null) onConnectYouTube else ({sheet="YouTube Playlists"})}) }
        item { ReferenceRow("Sync Playback to YouTube Music History","Not enabled yet","clock",5,6,onClick={syncInfo()}) }
        item {
            Spacer(Modifier.height(24.dp))
            ReferenceSurface(Modifier.fillMaxWidth().clickable(onClick=onDownloads),highlighted=true) {
                Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically) {
                    Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primary) {
                        Box(Modifier.size(48.dp),contentAlignment=Alignment.Center) { Glyph("download",color=MaterialTheme.colorScheme.onPrimary) }
                    }
                    Column(Modifier.weight(1f).padding(horizontal=16.dp)) {
                        Text("Downloads & Offline Music",fontWeight=FontWeight.Bold)
                        Text("Browse audio on this device",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Glyph("chevron",Modifier.size(18.dp))
                }
            }
        }
        item { SettingsHeading("Appearance") }
        item { ReferenceRow("AMOLED Mode","Pure black background","amoled",0,4,appearance.amoledMode,onClick={onAppearanceChange(appearance.copy(amoledMode=!appearance.amoledMode))}) }
        item { ReferenceRow("Dynamic Color",if(Build.VERSION.SDK_INT>=31) "Use your wallpaper's colors" else "Requires Android 12 or later","palette",1,4,appearance.dynamicColorEnabled,enabled=Build.VERSION.SDK_INT>=31,onClick={onAppearanceChange(appearance.copy(dynamicColorEnabled=!appearance.dynamicColorEnabled))}) }
        item { ReferenceRow("Dynamic Now Playing","Match the app to playing artwork","album",2,4,appearance.dynamicNowPlayingEnabled,onClick={onAppearanceChange(appearance.copy(dynamicNowPlayingEnabled=!appearance.dynamicNowPlayingEnabled))}) }
        item { ReferenceRow("Use Application Font","A custom look across the whole app","font",3,4,appearance.applicationFontEnabled,onClick={onAppearanceChange(appearance.copy(applicationFontEnabled=!appearance.applicationFontEnabled))}) }
        item { SettingsHeading("Accent") }
        item {
            ReferenceSurface {
                Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    listOf(AccentPreset.Crimson,AccentPreset.Violet,AccentPreset.Ocean,AccentPreset.Sage,AccentPreset.Amber,AccentPreset.Rose,AccentPreset.Mono,AccentPreset.Custom).chunked(4).forEach { row ->
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            row.forEach { preset ->
                                val selected=appearance.accentPreset==preset
                                Column(Modifier.weight(1f).clickable(role=Role.RadioButton) {
                                    if(preset==AccentPreset.Custom) sheet="Custom accent"
                                    else onAppearanceChange(appearance.copy(accentPreset=preset))
                                }.semantics { this.selected=selected },horizontalAlignment=Alignment.CenterHorizontally) {
                                    val accent=Color(preset.argb?:appearance.customAccentArgb)
                                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))
                                        .border(if(selected) 2.dp else 0.dp,if(selected) MaterialTheme.colorScheme.primary else Color.Transparent,RoundedCornerShape(20.dp))) {
                                        if(preset==AccentPreset.Custom) Box(Modifier.matchParentSize().background(Brush.sweepGradient(listOf(Color.Red,Color.Yellow,Color.Green,Color.Cyan,Color.Blue,Color.Magenta,Color.Red))))
                                        else if(preset==AccentPreset.Mono) {
                                            Box(Modifier.matchParentSize().background(Color(0xFF363636)),contentAlignment=Alignment.Center) { Glyph("amoled") }
                                        } else Column {
                                            Row(Modifier.weight(1f)) { Box(Modifier.weight(1f).fillMaxHeight().background(lerp(accent,Color.Black,.35f))); Box(Modifier.weight(1f).fillMaxHeight().background(lerp(accent,Color.White,.12f))) }
                                            Row(Modifier.weight(1f)) { Box(Modifier.weight(1f).fillMaxHeight().background(lerp(accent,Color.White,.50f))); Box(Modifier.weight(1f).fillMaxHeight().background(accent)) }
                                        }
                                        if(selected) Surface(Modifier.align(Alignment.TopEnd).padding(4.dp),shape=CircleShape,color=MaterialTheme.colorScheme.primary) {
                                            Glyph("check",Modifier.size(20.dp).padding(3.dp),MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    Text(preset.label,style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.Bold,maxLines=1)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { SettingsHeading("Experimental") }
        item { ReferenceRow("Liquid Glass","Translucent materials across the app","glass",0,5,appearance.liquidGlassEnabled,onClick={onAppearanceChange(appearance.copy(liquidGlassEnabled=!appearance.liquidGlassEnabled))}) }
        item { ReferenceRow("Lyrics Animation","Word timing when supplied · line fallback","lyrics",1,5,onClick={unavailable("Lyrics Animation","Open the quotation-mark button in Now Playing. VibeArc animates KuGou KRC or enhanced-LRC word timestamps when supplied and otherwise highlights synchronized lines.")}) }
        item { ReferenceRow("Equalizer",if(Build.VERSION.SDK_INT>=28) "Built-in 15-band equalizer" else "Requires Android 9 or later","equalizer",2,5,audioTuning.equalizerEnabled,enabled=Build.VERSION.SDK_INT>=28,onClick={sheet="Equalizer"}) }
        item { ReferenceRow("Wavy Seekbar","Lightweight wave while music plays","wave",3,5,wavySeekbar,onClick={wavySeekbar=!wavySeekbar;context.getSharedPreferences(SettingsPreferencesName,Context.MODE_PRIVATE).edit().putBoolean("wavy_seekbar",wavySeekbar).apply()}) }
        item { ReferenceRow("Studio Master Clarity","Native clarity EQ and peak limiter","spark",4,5,audioTuning.studioMasterEnabled,enabled=Build.VERSION.SDK_INT>=28,onClick={
            updateAudioTuning(audioTuning.copy(studioMasterEnabled=!audioTuning.studioMasterEnabled,bitPerfectEnabled=false))
        }) }
        item { SettingsHeading("Audio & Streaming") }
        item { ReferenceRow("Streaming Quality","${streamFormat.label} · ${streamQuality.label}","quality",0,6,onClick={sheet="Streaming Quality"}) }
        item { ReferenceRow("Download Quality","${downloadFormat.label} · ${downloadQuality.label}","download",1,6,onClick={sheet="Download Quality"}) }
        item { ReferenceRow("Bit-Perfect Mode",if(audioTuning.bitPerfectEnabled) context.bitPerfectStatus() else if(Build.VERSION.SDK_INT>=34) "Verified USB mixer path when available" else "Requires Android 14 and a compatible USB DAC","equalizer",2,6,audioTuning.bitPerfectEnabled,enabled=Build.VERSION.SDK_INT>=34,onClick={
            val enabled=!audioTuning.bitPerfectEnabled
            updateAudioTuning(audioTuning.copy(bitPerfectEnabled=enabled,equalizerEnabled=if(enabled) false else audioTuning.equalizerEnabled,studioMasterEnabled=if(enabled) false else audioTuning.studioMasterEnabled,crossfadeSeconds=if(enabled) 0 else audioTuning.crossfadeSeconds))
        }) }
        item { ReferenceRow("Crossfade",audioTuning.crossfadeSeconds.takeIf{it>0}?.let{"$it-second overlapping transition"}?:"Off","equalizer",3,6,audioTuning.crossfadeSeconds>0,onClick={sheet="Crossfade"}) }
        item { ReferenceRow("Download Synced Lyrics","Save matching lyrics as an .lrc file","lyrics",4,6,onClick={unavailable("Download Synced Lyrics","Open Lyrics from Now Playing. When synchronized lyrics are available, tap the download button and choose where to save the .lrc file.")}) }
        item { ReferenceRow("Background Playback","Manage this app's battery settings","clock",5,6,onClick={
            runCatching {context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}
                .onFailure {unavailable("Background Playback","Open Android Settings → Apps → VibeArc → Battery.")}
        }) }
        item { SettingsHeading("Library & Playlist Imports") }
        item { ReferenceRow("Import Playlist from File","CSV, TSV, M3U/M3U8, or TXT","download",onClick=onImportPlaylist) }
        item { SettingsHeading("Scrobbler") }
        item { ReferenceRow("Last.fm Account",when { lastFmBusy -> "Loading…"; lastFmAuthenticated -> lastFmUsername.orEmpty(); !lastFmConfigured -> "Signer configuration required"; lastFmAuthorizationPending -> "Authorization waiting to finish"; else -> "Tap to sign in" },"stats",0,3,onClick={sheet="Last.fm"}) }
        item { ReferenceRow("Scrobbling & Now Playing",when { !lastFmAuthenticated -> "Sign in to Last.fm first"; scrobblingEnabled -> "Automatic scrobbling is on"; else -> "Scrobbling is off" },"clock",1,3,scrobblingEnabled,enabled=lastFmAuthenticated,onClick={scrobblingEnabled=!scrobblingEnabled;context.saveLastFmScrobblingEnabled(scrobblingEnabled)}) }
        item { ReferenceRow("Scrobble Exclusions","${excludedScrobbleUris.size} excluded tracks","playlist",2,3,enabled=lastFmAuthenticated,onClick={sheet="Scrobble Exclusions"}) }
        item { SettingsHeading("Backup & Restore") }
        item { ReferenceRow("Backup","Save library and playlists to JSON","backup",0,2,onClick=onBackup) }
        item { ReferenceRow("Restore","Merge a VibeArc JSON backup","restore",1,2,onClick=onRestore) }
        item { SettingsHeading("App icon") }
        item { ReferenceRow("Launcher Icon",selectedIcon.label,"album",onClick={sheet="App icon"}) }
        item { SettingsHeading("About") }
        item { ReferenceRow("Privacy & Licenses","On-device data and provider notices","code",onClick={unavailable("Privacy & Licenses","VibeArc stores your library and settings on this device. YouTube cookies stay in Android's WebView cookie store; a Last.fm session key stays in private app storage. VibeArc includes no analytics or ad SDK. Online features contact YouTube, KuGou, LRCLIB, Lyrics.ovh, Last.fm, and GitHub. Full notices are included with the source release.")}) }
        item { ReferenceRow("Updates & Support","VibeArc on GitHub","spark",onClick={runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/Akumukage/VibeArc")))}}) }
        item {
            Spacer(Modifier.height(24.dp))
            ReferenceSurface {
                Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.vibearc_icon),"VibeArc icon",Modifier.size(76.dp).clip(RoundedCornerShape(20.dp)))
                    Text("VibeArc",style=MaterialTheme.typography.displaySmall)
                    Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer) { Text("Version 0.9.0 beta",Modifier.padding(horizontal=18.dp,vertical=6.dp),fontWeight=FontWeight.Bold) }
                    Text("Your music, your space",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)); ReferenceRow("Source Code","github.com/Akumukage/VibeArc","code",onClick={runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/Akumukage/VibeArc")))}}) }
    }
    info?.let { (title,message) ->
        AlertDialog(onDismissRequest={info=null},title={Text(title)},text={Text(message)},confirmButton={TextButton(onClick={info=null}){Text("Got it")}})
    }
    sheet?.let { title ->
        ModalBottomSheet(onDismissRequest={sheet=null},containerColor=MaterialTheme.colorScheme.background) {
            LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                        Glyph(when {
                            title == "Last.fm" -> "stats"
                            title.contains("Quality") -> "quality"
                            else -> "palette"
                        },Modifier.size(36.dp))
                        Text(title,style=MaterialTheme.typography.headlineMedium)
                    }
                }
                when(title) {
                    "Equalizer" -> {
                        item {
                            ReferenceRow("15-band equalizer","Android native DSP · ±12 dB","equalizer",checked=audioTuning.equalizerEnabled,onClick={
                                updateAudioTuning(audioTuning.copy(equalizerEnabled=!audioTuning.equalizerEnabled,bitPerfectEnabled=false))
                            })
                        }
                        items(EqualizerFrequencies.size) { index ->
                            val hz=EqualizerFrequencies[index].toInt()
                            Column {
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                    Text(if(hz>=1000) "${hz/1000f} kHz" else "$hz Hz",fontWeight=FontWeight.Bold)
                                    Text("${"%.1f".format(audioTuning.equalizerGains[index])} dB",color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Slider(
                                    value=audioTuning.equalizerGains[index],
                                    onValueChange={ gain ->
                                        val gains=audioTuning.equalizerGains.copyOf().also { it[index]=gain }
                                        updateAudioTuning(audioTuning.copy(equalizerGains=gains,equalizerEnabled=true,bitPerfectEnabled=false))
                                    },
                                    valueRange=-12f..12f,
                                    steps=47,
                                )
                            }
                        }
                        item { TextButton(onClick={updateAudioTuning(audioTuning.copy(equalizerGains=FloatArray(EqualizerFrequencies.size)))}) { Text("Reset flat") } }
                    }
                    "Crossfade" -> {
                        item { Text("Crossfade briefly overlaps two decoders. Keep it off for bit-perfect playback or maximum battery life.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(listOf(0,3,5,8,12).size) { index ->
                            val seconds=listOf(0,3,5,8,12)[index]
                            QualityChoice(if(seconds==0) "Off" else "$seconds seconds","Transition",if(seconds==0) "Gapless direct track changes." else "Equal-power overlap between consecutive tracks.",audioTuning.crossfadeSeconds==seconds) {
                                updateAudioTuning(audioTuning.copy(crossfadeSeconds=seconds,bitPerfectEnabled=false))
                            }
                        }
                    }
                    "Last.fm" -> {
                        item { Text("Sign in through Last.fm. Now Playing and completed scrobbles are signed by the VibeArc server; the Last.fm shared secret is never stored in the app.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (!lastFmConfigured) item { Text("Configure LASTFM_SIGNER_URL and LASTFM_SIGNER_TOKEN for authenticated use.",color=MaterialTheme.colorScheme.error) }
                        lastFmError?.let { message -> item { Text(message,color=MaterialTheme.colorScheme.error) } }
                        item {
                            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                if (!lastFmAuthenticated) Button(onClick=onStartLastFmAuth,enabled=lastFmConfigured && !lastFmBusy) { Text("Authorize") }
                                if (lastFmAuthorizationPending && !lastFmAuthenticated) Button(onClick=onFinishLastFmAuth,enabled=!lastFmBusy) { Text("Finish sign-in") }
                                if(lastFmUsername != null) OutlinedButton(onClick=onRefreshLastFm,enabled=!lastFmBusy) { Text("Refresh") }
                            }
                        }
                        if(lastFmAuthenticated) item { TextButton(onClick={onDisconnectLastFm();sheet=null}) { Text("Disconnect Last.fm") } }
                    }
                    "Scrobble Exclusions" -> {
                        item { Text("Last.fm accepts tracks longer than 30 seconds after half the track or four minutes, whichever comes first. Excluded tracks never send Now Playing or scrobbles.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        if(lastFmTracks.isEmpty()) item { Text("Your library is empty.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        else items(lastFmTracks.size,key={lastFmTracks[it].catalogUri}) { index ->
                            val track=lastFmTracks[index]
                            val excluded=track.catalogUri in excludedScrobbleUris
                            ReferenceRow(track.title,track.artist,"lyrics",index,lastFmTracks.size,excluded,onClick={
                                excludedScrobbleUris=if(excluded) excludedScrobbleUris-track.catalogUri else excludedScrobbleUris+track.catalogUri
                                context.saveLastFmExcludedUris(excludedScrobbleUris)
                            })
                        }
                    }
                    "YouTube Account" -> {
                        item { Text(youtubeAccountData?.account?.displayName.orEmpty(),style=MaterialTheme.typography.titleLarge) }
                        item { ReferenceRow("Switch account","Choose another Google account","account",0,2,onClick={sheet=null;onSwitchYouTubeAccount()}) }
                        item { ReferenceRow("Disconnect","Remove VibeArc's YouTube access","delete",1,2,onClick={sheet=null;onDisconnectYouTube()}) }
                    }
                    "Sync Playlists" -> {
                        item { Text("Choose playlists to compare. VibeArc always shows the changes and asks again before removing anything from YouTube.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        item {
                            Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                                OutlinedTextField(youtubePlaylistTitle,{youtubePlaylistTitle=it.take(150)},label={Text("New YouTube playlist")},singleLine=true,modifier=Modifier.weight(1f))
                                Button(onClick={onCreateYouTubePlaylist(youtubePlaylistTitle);youtubePlaylistTitle=""},enabled=youtubePlaylistTitle.isNotBlank()&&!youtubeSyncBusy) { Text("Create") }
                            }
                        }
                        if (youtubeAccountData?.playlists.isNullOrEmpty()) item {
                            Text("This account has no visible playlists.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                        } else items(youtubeAccountData!!.playlists.size) { index ->
                            val playlist=youtubeAccountData.playlists[index]
                            ReferenceRow(playlist.title,"${playlist.itemCount} tracks","playlist",index,youtubeAccountData.playlists.size,playlist.id in youtubeSelectedPlaylistIds,onClick={onToggleYouTubePlaylistSync(playlist.id)})
                        }
                    }
                    "Streaming Quality" -> {
                        item { Text("VibeArc selects a real provider source matching this codec and bitrate ceiling. If a requested codec is unavailable, that track is reported unavailable.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(AudioFormat.entries.size) { index ->
                            val choice=AudioFormat.entries[index]
                            QualityChoice(choice.label,"Codec",if(choice==AudioFormat.ANY) "Use the best available source codec." else "Require ${choice.label} from the source.",streamFormat==choice) {
                                streamFormat=choice;context.saveStreamAudioPreference(streamFormat,streamQuality)
                            }
                        }
                        items(AudioQuality.entries.size) { index ->
                            val choice=AudioQuality.entries[index]
                            QualityChoice(choice.label,"Bitrate",choice.maxBitrateKbps?.let { "Choose the highest real source at or below $it kbps." } ?: "Choose the highest-bitrate real source.",streamQuality==choice) {
                                streamQuality=choice;context.saveStreamAudioPreference(streamFormat,streamQuality)
                            }
                        }
                    }
                    "Download Quality" -> {
                        item { Text("Online downloads save an actual source offered in the selected codec. VibeArc does not relabel or fake lossless audio; unavailable formats fail clearly.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(AudioFormat.entries.size) { index ->
                            val choice=AudioFormat.entries[index]
                            QualityChoice(choice.label,"Format",if(choice==AudioFormat.ANY) "Keep the best available source format." else "Require ${choice.label} from the source.",downloadFormat==choice) {
                                downloadFormat=choice;context.saveDownloadAudioPreference(downloadFormat,downloadQuality)
                            }
                        }
                        items(AudioQuality.entries.size) { index ->
                            val choice=AudioQuality.entries[index]
                            QualityChoice(choice.label,"Quality",choice.maxBitrateKbps?.let { "Download the highest real source at or below $it kbps." } ?: "Download the highest available real source.",downloadQuality==choice) {
                                downloadQuality=choice;context.saveDownloadAudioPreference(downloadFormat,downloadQuality)
                            }
                        }
                    }
                    "YouTube Playlists" -> {
                        item { Text("Tap a playlist to add its available tracks to your VibeArc library. This does not download audio or modify YouTube.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (youtubeAccountData?.playlists.isNullOrEmpty()) item {
                            Text("This account has no visible playlists.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                        } else items(youtubeAccountData!!.playlists.size) { index ->
                            val playlist=youtubeAccountData.playlists[index]
                            ReferenceRow(playlist.title,"${playlist.itemCount} tracks","playlist",onClick={onImportYouTubePlaylist(playlist);sheet=null})
                        }
                    }
                    "App icon" -> items(LauncherIconChoice.entries.size) { i ->
                        val choice=LauncherIconChoice.entries[i]
                        QualityChoice(choice.label,if(choice==selectedIcon) "Selected" else "App icon","Changes the icon on your home screen.",choice==selectedIcon) {
                            if(context.setLauncherIcon(choice)) selectedIcon=choice else unavailable("App icon","The launcher could not change the icon.")
                        }
                    }
                    else -> item {
                        OutlinedTextField(custom,{custom=it.take(9);error=false},label={Text("Hex color")},supportingText={if(error)Text("Use #RRGGBB")},isError=error,singleLine=true,modifier=Modifier.fillMaxWidth())
                        Button(onClick={
                            val parsed=parseAccentHex(custom)
                            if(parsed==null) error=true else {onAppearanceChange(appearance.copy(accentPreset=AccentPreset.Custom,customAccentArgb=parsed));sheet=null}
                        }){Text("Apply color")}
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsHeading(title:String) {
    Text(title,Modifier.padding(start=4.dp,top=24.dp,bottom=10.dp),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
}

@Composable
private fun QualityChoice(title:String,badge:String,description:String,selected:Boolean,onClick:()->Unit) {
    ReferenceSurface(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .border(if(selected) 1.5.dp else 0.dp,if(selected) MaterialTheme.colorScheme.primary else Color.Transparent,RoundedCornerShape(24.dp))
        .clickable(role=Role.RadioButton,onClick=onClick).semantics { this.selected=selected },highlighted=selected) {
        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(title,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)
                Text(badge,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)
                Text(description,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(selected) Glyph("check",Modifier.padding(start=12.dp).size(26.dp))
        }
    }
}

private const val SettingsPreferencesName="vibearc_settings"
private const val HighestAudioQualityKey="highest_audio_quality"
internal fun Context.prefersHighestAudioQuality():Boolean =
    streamAudioQuality()==AudioQuality.HIGHEST
internal fun Context.wavySeekbarEnabled():Boolean =
    getSharedPreferences(SettingsPreferencesName,Context.MODE_PRIVATE).getBoolean("wavy_seekbar",true)
private fun Context.saveHighestAudioQuality(enabled:Boolean) {
    saveStreamAudioPreference(streamAudioFormat(),if(enabled) AudioQuality.HIGHEST else AudioQuality.BALANCED)
}

internal fun Context.streamAudioFormat():AudioFormat = enumPreference("stream_audio_format",AudioFormat.ANY)
internal fun Context.streamAudioQuality():AudioQuality = enumPreference("stream_audio_quality",if(getSharedPreferences(SettingsPreferencesName,Context.MODE_PRIVATE).getBoolean(HighestAudioQualityKey,true)) AudioQuality.HIGHEST else AudioQuality.BALANCED)
internal fun Context.downloadAudioFormat():AudioFormat = enumPreference("download_audio_format",AudioFormat.ANY)
internal fun Context.downloadAudioQuality():AudioQuality = enumPreference("download_audio_quality",AudioQuality.HIGHEST)

private inline fun <reified T:Enum<T>> Context.enumPreference(key:String,fallback:T):T =
    runCatching { enumValueOf<T>(getSharedPreferences(SettingsPreferencesName,Context.MODE_PRIVATE).getString(key,fallback.name)!!) }.getOrDefault(fallback)

internal fun Context.saveStreamAudioPreference(format:AudioFormat,quality:AudioQuality) {
    getSharedPreferences(SettingsPreferencesName,Context.MODE_PRIVATE).edit().putString("stream_audio_format",format.name).putString("stream_audio_quality",quality.name).apply()
}

internal fun Context.saveDownloadAudioPreference(format:AudioFormat,quality:AudioQuality) {
    getSharedPreferences(SettingsPreferencesName,Context.MODE_PRIVATE).edit().putString("download_audio_format",format.name).putString("download_audio_quality",quality.name).apply()
}
