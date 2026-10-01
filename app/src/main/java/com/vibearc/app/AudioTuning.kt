package com.vibearc.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import kotlin.math.cos
import kotlin.math.sin

internal val EqualizerFrequencies = floatArrayOf(
    25f, 40f, 63f, 100f, 160f, 250f, 400f, 630f, 1_000f, 1_600f, 2_500f, 4_000f, 6_300f, 10_000f, 16_000f,
)
private val StudioMasterCurve = floatArrayOf(
    0f, 0f, -.2f, -.3f, -.2f, 0f, .15f, .25f, .35f, .45f, .55f, .65f, .7f, .6f, .5f,
)

internal fun effectiveEqualizerGains(user: FloatArray, studioMaster: Boolean): FloatArray =
    FloatArray(EqualizerFrequencies.size) { index ->
        ((user.getOrNull(index) ?: 0f) + if (studioMaster) StudioMasterCurve[index] else 0f).coerceIn(-12f, 12f)
    }

internal data class CrossfadeGains(val outgoing: Float, val incoming: Float)

internal fun crossfadeGains(progress: Float): CrossfadeGains {
    val angle = progress.coerceIn(0f, 1f) * (Math.PI / 2).toFloat()
    return CrossfadeGains(cos(angle), sin(angle))
}

internal fun sourceQualityLabel(mimeType: String?, sampleRate: Int): String = when {
    mimeType.equals("audio/flac", true) && sampleRate > 48_000 -> "Hi-Res source · output verification required"
    mimeType.equals("audio/flac", true) -> "Lossless source"
    else -> "Lossy source"
}

internal data class AudioTuning(
    val equalizerEnabled: Boolean,
    val equalizerGains: FloatArray,
    val studioMasterEnabled: Boolean,
    val bitPerfectEnabled: Boolean,
    val crossfadeSeconds: Int,
)

internal fun Context.loadAudioTuning(): AudioTuning {
    val prefs = getSharedPreferences(AudioTuningPreferences, Context.MODE_PRIVATE)
    val gains = prefs.getString(EqualizerGainsKey, null)?.split(',')
        ?.mapNotNull(String::toFloatOrNull)?.takeIf { it.size == EqualizerFrequencies.size }
        ?.toFloatArray() ?: FloatArray(EqualizerFrequencies.size)
    return AudioTuning(
        equalizerEnabled = prefs.getBoolean(EqualizerEnabledKey, false),
        equalizerGains = gains,
        studioMasterEnabled = prefs.getBoolean(StudioMasterKey, false),
        bitPerfectEnabled = prefs.getBoolean(BitPerfectKey, false),
        crossfadeSeconds = prefs.getInt(CrossfadeKey, 0).coerceIn(0, 12),
    )
}

internal fun Context.saveAudioTuning(value: AudioTuning) {
    getSharedPreferences(AudioTuningPreferences, Context.MODE_PRIVATE).edit()
        .putBoolean(EqualizerEnabledKey, value.equalizerEnabled)
        .putString(EqualizerGainsKey, value.equalizerGains.joinToString(","))
        .putBoolean(StudioMasterKey, value.studioMasterEnabled)
        .putBoolean(BitPerfectKey, value.bitPerfectEnabled)
        .putInt(CrossfadeKey, value.crossfadeSeconds.coerceIn(0, 12))
        .apply()
}

internal class AudioEffectsController(private val context: Context) {
    private var effect: DynamicsProcessing? = null
    private var audioSessionId = 0
    private var appliedFingerprint = ""

    fun attach(sessionId: Int) {
        if (sessionId <= 0) return
        audioSessionId = sessionId
        appliedFingerprint = ""
        refresh()
    }

    fun refresh() {
        val tuning = context.loadAudioTuning()
        val gains = effectiveEqualizerGains(tuning.equalizerGains, tuning.studioMasterEnabled)
        val fingerprint = "${tuning.equalizerEnabled}:${tuning.studioMasterEnabled}:${tuning.bitPerfectEnabled}:${gains.joinToString()}"
        if (fingerprint == appliedFingerprint || audioSessionId <= 0) return
        releaseEffect()
        if (!tuning.bitPerfectEnabled && (tuning.equalizerEnabled || tuning.studioMasterEnabled) && Build.VERSION.SDK_INT >= 28) {
            effect = runCatching {
                val eq = DynamicsProcessing.Eq(true, true, EqualizerFrequencies.size).apply {
                    EqualizerFrequencies.indices.forEach { index ->
                        setBand(index, DynamicsProcessing.EqBand(true, EqualizerFrequencies[index], gains[index]))
                    }
                }
                val limiter = DynamicsProcessing.Limiter(true, true, 0, 1f, 60f, 10f, -1f, 0f)
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, false, 0, false, 0, true, EqualizerFrequencies.size, true,
                ).setPostEqAllChannelsTo(eq).setLimiterAllChannelsTo(limiter).build()
                DynamicsProcessing(0, audioSessionId, config).apply { enabled = true }
            }.getOrNull()
        }
        appliedFingerprint = fingerprint
    }

    fun release() = releaseEffect()

    private fun releaseEffect() {
        effect?.release()
        effect = null
    }
}

internal class BitPerfectController(private val context: Context) {
    private val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
    private var device: AudioDeviceInfo? = null

    fun configure(details: AudioDetails?): String {
        clear()
        if (!context.loadAudioTuning().bitPerfectEnabled) return "Off"
        if (Build.VERSION.SDK_INT < 34) return "Requires Android 14 and a compatible USB DAC"
        val manager = context.getSystemService(AudioManager::class.java)
        val output = manager.getAudioDevicesForAttributes(audioAttributes).firstOrNull { it.isUsbOutput() }
            ?: return "No compatible USB audio output"
        val supported = manager.getSupportedMixerAttributes(output)
        val candidate = supported
            .filter { mixer -> details?.sampleRate?.takeIf { it > 0 }?.let { mixer.format.sampleRate == it } ?: true }
            .maxByOrNull { it.format.sampleRate }
            ?: return "USB output exposes no direct mixer format"
        val requested = AudioMixerAttributes.Builder(candidate.format)
            .setMixerBehavior(AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT)
            .build()
        if (!manager.setPreferredMixerAttributes(audioAttributes, output, requested)) return "USB output rejected bit-perfect mode"
        val verified = manager.getPreferredMixerAttributes(audioAttributes, output)
        if (verified?.mixerBehavior != AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT) {
            manager.clearPreferredMixerAttributes(audioAttributes, output)
            return "Bit-perfect request was not accepted"
        }
        device = output
        return "Verified by Android mixer · ${candidate.format.sampleRate / 1_000.0} kHz"
    }

    fun clear() {
        if (Build.VERSION.SDK_INT >= 34) device?.let {
            context.getSystemService(AudioManager::class.java).clearPreferredMixerAttributes(audioAttributes, it)
        }
        device = null
    }

    private fun AudioDeviceInfo.isUsbOutput(): Boolean = type == AudioDeviceInfo.TYPE_USB_DEVICE ||
        type == AudioDeviceInfo.TYPE_USB_HEADSET || type == AudioDeviceInfo.TYPE_USB_ACCESSORY
}

internal fun Context.saveBitPerfectStatus(value: String) {
    getSharedPreferences(AudioTuningPreferences, Context.MODE_PRIVATE).edit().putString(BitPerfectStatusKey, value).apply()
}

internal fun Context.bitPerfectStatus(): String =
    getSharedPreferences(AudioTuningPreferences, Context.MODE_PRIVATE).getString(BitPerfectStatusKey, "Off").orEmpty()

private const val AudioTuningPreferences = "vibearc_audio_tuning"
private const val EqualizerEnabledKey = "equalizer_enabled"
private const val EqualizerGainsKey = "equalizer_gains"
private const val StudioMasterKey = "studio_master"
private const val BitPerfectKey = "bit_perfect"
private const val CrossfadeKey = "crossfade_seconds"
private const val BitPerfectStatusKey = "bit_perfect_status"
