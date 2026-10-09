package com.vibearc.app

import kotlin.math.PI
import kotlin.math.sin

internal fun seekWaveY(x: Float, wavelength: Float, amplitude: Float): Float {
    require(wavelength > 0f)
    return sin(x / wavelength * (2 * PI)).toFloat() * amplitude
}
