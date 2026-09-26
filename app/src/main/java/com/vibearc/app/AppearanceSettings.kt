package com.vibearc.app

import android.content.Context

internal const val DEFAULT_ACCENT_ARGB = 0xFFD7A24AL

internal enum class AccentPreset(val label: String, val argb: Long?) {
    LiquidGold("Liquid gold", DEFAULT_ACCENT_ARGB),
    Amber("Amber", 0xFFFFB300L),
    Coral("Coral", 0xFFE76F51L),
    Rose("Rose", 0xFFE85D8AL),
    Orchid("Orchid", 0xFFC77DFFL),
    Indigo("Indigo", 0xFF7C83FDL),
    Azure("Azure", 0xFF4DA3FFL),
    Teal("Teal", 0xFF3EC6B8L),
    Emerald("Emerald", 0xFF43AA8BL),
    Lime("Lime", 0xFFA7C957L),
    Custom("Custom", null),
}

internal data class AppearanceConfig(
    val amoledMode: Boolean = false,
    val dynamicColorEnabled: Boolean = false,
    val dynamicNowPlayingEnabled: Boolean = true,
    val liquidGlassEnabled: Boolean = true,
    val accentPreset: AccentPreset = AccentPreset.LiquidGold,
    val customAccentArgb: Long = DEFAULT_ACCENT_ARGB,
)

internal fun AppearanceConfig.resolvedAccentArgb(): Long = when (accentPreset) {
    AccentPreset.Custom -> customAccentArgb
    else -> accentPreset.argb ?: DEFAULT_ACCENT_ARGB
}

internal fun parseAccentHex(value: String): Long? {
    val hex = value.trim().removePrefix("#")
    if (hex.length !in setOf(6, 8) || hex.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) return null
    val parsed = hex.toLongOrNull(16) ?: return null
    return if (hex.length == 6) parsed or 0xFF000000L else parsed
}

private const val PreferencesName = "vibearc_appearance"

internal fun Context.loadAppearanceConfig(): AppearanceConfig {
    val preferences = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    val preset = runCatching {
        AccentPreset.valueOf(preferences.getString("accent_preset", null).orEmpty())
    }.getOrDefault(AccentPreset.LiquidGold)
    return AppearanceConfig(
        amoledMode = preferences.getBoolean("amoled", false),
        dynamicColorEnabled = preferences.getBoolean("dynamic_color", false),
        dynamicNowPlayingEnabled = preferences.getBoolean("dynamic_now_playing", true),
        liquidGlassEnabled = preferences.getBoolean("liquid_glass", true),
        accentPreset = preset,
        customAccentArgb = preferences.getLong("custom_accent", DEFAULT_ACCENT_ARGB),
    )
}

internal fun Context.saveAppearanceConfig(config: AppearanceConfig) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putBoolean("amoled", config.amoledMode)
        .putBoolean("dynamic_color", config.dynamicColorEnabled)
        .putBoolean("dynamic_now_playing", config.dynamicNowPlayingEnabled)
        .putBoolean("liquid_glass", config.liquidGlassEnabled)
        .putString("accent_preset", config.accentPreset.name)
        .putLong("custom_accent", config.customAccentArgb)
        .apply()
}
