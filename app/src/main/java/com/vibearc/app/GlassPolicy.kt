package com.vibearc.app

// ponytail: older/low-RAM devices keep the same material without GPU backdrop blur.
internal fun glassBackdropEnabled(glass: Boolean, sdk: Int, lowRam: Boolean): Boolean =
    glass && sdk >= 31 && !lowRam
