package com.vibearc.app

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
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
    lastFmUsername: String?, lastFmBusy: Boolean, lastFmConfigured: Boolean, lastFmError: String?,
    onConnectLastFm: (String) -> Unit, onRefreshLastFm: () -> Unit, onDisconnectLastFm: () -> Unit,
    onBackup: () -> Unit, onRestore: () -> Unit,
    onImportPlaylist: () -> Unit,
) {
    val context=LocalContext.current
    var info by remember { mutableStateOf<Pair<String,String>?>(null) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var highestQuality by remember { mutableStateOf(context.prefersHighestAudioQuality()) }
    var wavySeekbar by remember { mutableStateOf(context.wavySeekbarEnabled()) }
    var selectedIcon by remember { mutableStateOf(context.selectedLauncherIcon()) }
    var custom by remember { mutableStateOf("#%06X".format(appearance.customAccentArgb and 0xFFFFFF)) }
    var lastFmInput by remember(lastFmUsername) { mutableStateOf(lastFmUsername.orEmpty()) }
    var youtubePlaylistTitle by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val unavailable: (String,String)->Unit = { title,description -> info=title to description }
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
        item { ReferenceRow("Lyrics Animation","Word timing when supplied · line fallback","lyrics",1,5,onClick={unavailable("Lyrics Animation","Open the quotation-mark button in Now Playing. VibeArc animates enhanced-LRC word timestamps when a provider supplies them and otherwise highlights synchronized lines from LRCLIB.")}) }
        item { ReferenceRow("Equalizer","Open your device's audio controls","equalizer",2,5,onClick={
            val intent=Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).putExtra(AudioEffect.EXTRA_PACKAGE_NAME,context.packageName).putExtra(AudioEffect.EXTRA_CONTENT_TYPE,AudioEffect.CONTENT_TYPE_MUSIC)
            if(runCatching{context.startActivity(intent)}.isFailure) unavailable("Equalizer","This phone does not provide a system equalizer panel. A built-in equalizer is not included yet.")
        }) }
        item { ReferenceRow("Wavy Seekbar","Lightweight wave while music plays","wave",3,5,wavySeekbar,onClick={wavySeekbar=!wavySeekbar;context.getSharedPreferences(SettingsPreferencesName,Context.MODE_PRIVATE).edit().putBoolean("wavy_seekbar",wavySeekbar).apply()}) }
        item { ReferenceRow("Studio Master Clarity","Original source audio","spark",4,5,onClick={unavailable("Studio Master Clarity","VibeArc plays the source audio. It does not convert lossy audio into studio-master or lossless quality.")}) }
        item { SettingsHeading("Audio & Streaming") }
        item { ReferenceRow("Streaming Quality",if(highestQuality) "Highest available · YouTube Music" else "Balanced · YouTube Music","quality",0,6,onClick={sheet="Streaming Quality"}) }
        item { ReferenceRow("Download Quality","Imported files keep their original quality","download",1,6,onClick={sheet="Download Quality"}) }
        item { ReferenceRow("Bit-Perfect Mode","Not supported by the current audio output","equalizer",2,6,onClick={unavailable("Bit-Perfect Mode","The current Android audio path does not guarantee bit-perfect output.")}) }
        item { ReferenceRow("Crossfade","Not available in this build","equalizer",3,6,onClick={unavailable("Crossfade","Playback currently switches directly between tracks. Crossfade is not implemented.")}) }
        item { ReferenceRow("Download Synced Lyrics","Save matching lyrics as an .lrc file","lyrics",4,6,onClick={unavailable("Download Synced Lyrics","Open Lyrics from Now Playing. When synchronized lyrics are available, tap the download button and choose where to save the .lrc file.")}) }
        item { ReferenceRow("Background Playback","Manage this app's battery settings","clock",5,6,onClick={
            runCatching {context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}
                .onFailure {unavailable("Background Playback","Open Android Settings → Apps → VibeArc → Battery.")}
        }) }
        item { SettingsHeading("Library & Playlist Imports") }
        item { ReferenceRow("Import Playlist from File","CSV, TSV, M3U/M3U8, or TXT","download",onClick=onImportPlaylist) }
        item { SettingsHeading("Scrobbler") }
        item { ReferenceRow("Last.fm Profile",when { lastFmBusy -> "Loading…"; lastFmUsername != null -> lastFmUsername; !lastFmConfigured -> "API key required for this build"; else -> "Tap to connect a public profile" },"stats",0,2,onClick={sheet="Last.fm"}) }
        item { ReferenceRow("Scrobbling & Now Playing",if(lastFmUsername == null) "Connect a profile first" else "Secure server signing required","clock",1,2,onClick={unavailable("Last.fm scrobbling","Public Last.fm statistics are connected. Scrobbling and Now Playing stay disabled until VibeArc has a server-side signer; the shared secret will not be embedded in the APK.")}) }
        item { SettingsHeading("Backup & Restore") }
        item { ReferenceRow("Backup","Save library and playlists to JSON","backup",0,2,onClick=onBackup) }
        item { ReferenceRow("Restore","Merge a VibeArc JSON backup","restore",1,2,onClick=onRestore) }
        item { SettingsHeading("App icon") }
        item { ReferenceRow("Launcher Icon",selectedIcon.label,"album",onClick={sheet="App icon"}) }
        item { SettingsHeading("About") }
        item { ReferenceRow("Privacy & Licenses","On-device data and provider notices","code",onClick={unavailable("Privacy & Licenses","VibeArc stores your library and settings on this device, keeps OAuth tokens only in memory, and includes no analytics or ad SDK. Online features contact YouTube, LRCLIB, Last.fm, and GitHub. Full PRIVACY.md and THIRD_PARTY_NOTICES.md files are included with the source release.")}) }
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
                    "Last.fm" -> {
                        item { Text("Connect a public Last.fm profile to show listening statistics and top tracks. This does not submit your playback history.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (!lastFmConfigured) item { Text("This APK was built without LASTFM_API_KEY. Add it to local.properties or the build environment, then rebuild.",color=MaterialTheme.colorScheme.error) }
                        item { OutlinedTextField(lastFmInput,{lastFmInput=it.take(64)},label={Text("Last.fm username")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
                        lastFmError?.let { message -> item { Text(message,color=MaterialTheme.colorScheme.error) } }
                        item {
                            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                Button(onClick={onConnectLastFm(lastFmInput);sheet=null},enabled=lastFmConfigured && !lastFmBusy) { Text(if(lastFmUsername == null) "Connect" else "Update") }
                                if(lastFmUsername != null) OutlinedButton(onClick=onRefreshLastFm,enabled=!lastFmBusy) { Text("Refresh") }
                            }
                        }
                        if(lastFmUsername != null) item { TextButton(onClick={onDisconnectLastFm();sheet=null}) { Text("Disconnect public profile") } }
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
                        item { Text("Select your preferred stream. Actual bitrate and format depend on the track and the available source.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        item { QualityChoice("Max Quality","Highest available","Select the highest-bitrate playable audio source.",highestQuality) { highestQuality=true;context.saveHighestAudioQuality(true) } }
                        item { QualityChoice("Balanced","Default","Use the provider's standard audio selection.",!highestQuality) { highestQuality=false;context.saveHighestAudioQuality(false) } }
                        item { Text("24-bit / 192 kHz FLAC is not provided by the current YouTube source.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    "Download Quality" -> {
                        item { Text("Local audio keeps its original format and quality. Online downloading is not connected in this build.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        item { QualityChoice("Original file","Unchanged","No conversion or re-encoding.",true) {} }
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
    getSharedPreferences(SettingsPreferencesName,Context.MODE_PRIVATE).getBoolean(HighestAudioQualityKey,true)
internal fun Context.wavySeekbarEnabled():Boolean =
    getSharedPreferences(SettingsPreferencesName,Context.MODE_PRIVATE).getBoolean("wavy_seekbar",true)
private fun Context.saveHighestAudioQuality(enabled:Boolean) {
    getSharedPreferences(SettingsPreferencesName,Context.MODE_PRIVATE).edit().putBoolean(HighestAudioQualityKey,enabled).apply()
}
