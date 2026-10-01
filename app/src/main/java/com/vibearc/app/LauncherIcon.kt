package com.vibearc.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

internal enum class LauncherIconChoice(val label: String, val alias: String) {
    Mint("Neon Mint", "MintIconAlias"),
    Ember("Neon Ember", "EmberIconAlias"),
    Aurora("Neon Aurora", "AuroraIconAlias"),
}

private const val LauncherPreferences = "launcher_icon"
private const val SelectedIcon = "selected"

internal fun Context.selectedLauncherIcon(): LauncherIconChoice {
    val saved = getSharedPreferences(LauncherPreferences, Context.MODE_PRIVATE)
        .getString(SelectedIcon, null)
    return LauncherIconChoice.entries.firstOrNull { it.name == saved } ?: LauncherIconChoice.Mint
}

internal fun Context.setLauncherIcon(choice: LauncherIconChoice): Boolean = runCatching {
    val selectedComponent = ComponentName(packageName, "$packageName.${choice.alias}")
    packageManager.setComponentEnabledSetting(
        selectedComponent,
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP,
    )
    LauncherIconChoice.entries.filterNot { it == choice }.forEach { other ->
        packageManager.setComponentEnabledSetting(
            ComponentName(packageName, "$packageName.${other.alias}"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
    getSharedPreferences(LauncherPreferences, Context.MODE_PRIVATE)
        .edit()
        .putString(SelectedIcon, choice.name)
        .apply()
}.isSuccess
