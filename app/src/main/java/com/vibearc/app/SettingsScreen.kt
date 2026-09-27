package com.vibearc.app

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsScreen(
    padding: PaddingValues,
    appearance: AppearanceConfig,
    onAppearanceChange: (AppearanceConfig) -> Unit,
) {
    val context = LocalContext.current
    var selectedIcon by remember { mutableStateOf(context.selectedLauncherIcon()) }
    var highestQuality by remember { mutableStateOf(context.prefersHighestAudioQuality()) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var customAccent by remember(appearance.customAccentArgb) {
        mutableStateOf("#%06X".format(appearance.customAccentArgb and 0xFFFFFF))
    }
    var accentError by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your VibeArc", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        listOfNotNull(
                            "${appearance.accentPreset.label} accent",
                            "AMOLED".takeIf { appearance.amoledMode },
                            "Dynamic artwork".takeIf { appearance.dynamicNowPlayingEnabled },
                            "Liquid Glass".takeIf { appearance.liquidGlassEnabled },
                        ).joinToString("  •  "),
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
        item { SettingsSection("Appearance", "Changes apply instantly and are saved on this device.") }
        item {
            SettingToggle(
                title = "AMOLED mode",
                description = "Use true black backgrounds to reduce glow and OLED power use.",
                checked = appearance.amoledMode,
                onCheckedChange = { onAppearanceChange(appearance.copy(amoledMode = it)) },
            )
        }
        item {
            SettingToggle(
                title = "Wallpaper colors",
                description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    "Use Android's wallpaper-derived Material colors."
                } else {
                    "Requires Android 12 or newer."
                },
                checked = appearance.dynamicColorEnabled,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onCheckedChange = { onAppearanceChange(appearance.copy(dynamicColorEnabled = it)) },
            )
        }
        item {
            SettingToggle(
                title = "Dynamic Now Playing",
                description = "Match the player background to the current album artwork.",
                checked = appearance.dynamicNowPlayingEnabled,
                onCheckedChange = { onAppearanceChange(appearance.copy(dynamicNowPlayingEnabled = it)) },
            )
        }
        item {
            SettingToggle(
                title = "Liquid Glass",
                description = "Use translucent floating chrome. Turn off for maximum performance.",
                checked = appearance.liquidGlassEnabled,
                onCheckedChange = { onAppearanceChange(appearance.copy(liquidGlassEnabled = it)) },
            )
        }
        item { SettingsSection("Accent", "Used whenever wallpaper colors are off.") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AccentPreset.entries.forEach { preset ->
                    val selected = appearance.accentPreset == preset
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) colors.primaryContainer else colors.surfaceVariant,
                        ),
                        modifier = Modifier.clickable { onAppearanceChange(appearance.copy(accentPreset = preset)) },
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .background(Color(preset.argb ?: appearance.customAccentArgb), CircleShape),
                            )
                            Text(
                                preset.label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (selected) Text("Selected", color = colors.primary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = customAccent,
                    onValueChange = {
                        customAccent = it.take(9)
                        accentError = false
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Custom accent") },
                    supportingText = { if (accentError) Text("Use #RRGGBB or #AARRGGBB") },
                    isError = accentError,
                    singleLine = true,
                )
                Button(onClick = {
                    val parsed = parseAccentHex(customAccent)
                    if (parsed == null) {
                        accentError = true
                    } else {
                        onAppearanceChange(
                            appearance.copy(
                                accentPreset = AccentPreset.Custom,
                                customAccentArgb = parsed,
                            ),
                        )
                    }
                }) { Text("Apply") }
            }
        }
        item { SettingsSection("Playback", "Quality choices apply to newly resolved online tracks.") }
        item {
            SettingToggle(
                title = "Highest available audio quality",
                description = "Uses more data when higher-bitrate audio is available.",
                checked = highestQuality,
                onCheckedChange = { enabled ->
                    highestQuality = enabled
                    context.saveHighestAudioQuality(enabled)
                },
            )
        }
        item { SettingsSection("App icon", "Choose the icon shown on your home screen.") }
        items(LauncherIconChoice.entries, key = LauncherIconChoice::name) { choice ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (choice == selectedIcon) colors.primaryContainer else colors.surfaceVariant,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().clickable {
                    if (context.setLauncherIcon(choice)) {
                        selectedIcon = choice
                        resultMessage = "${choice.label} icon selected"
                    } else {
                        resultMessage = "Could not change the icon"
                    }
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(choice.label, fontWeight = FontWeight.Bold)
                        Text(if (choice == selectedIcon) "Selected" else "Tap to use", color = colors.onSurfaceVariant)
                    }
                    if (choice == selectedIcon) Text("✓", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        resultMessage?.let { message -> item { Text(message, color = colors.primary) } }
        item {
            Text(
                "Some launchers take a moment to refresh the icon.",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(colors.primaryContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text(title.take(1), color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = if (enabled) colors.onSurface else colors.onSurfaceVariant)
                Text(description, color = colors.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

private const val SettingsPreferencesName = "vibearc_settings"
private const val HighestAudioQualityKey = "highest_audio_quality"

internal fun Context.prefersHighestAudioQuality(): Boolean =
    getSharedPreferences(SettingsPreferencesName, Context.MODE_PRIVATE)
        .getBoolean(HighestAudioQualityKey, true)

private fun Context.saveHighestAudioQuality(enabled: Boolean) {
    getSharedPreferences(SettingsPreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(HighestAudioQualityKey, enabled)
        .apply()
}
