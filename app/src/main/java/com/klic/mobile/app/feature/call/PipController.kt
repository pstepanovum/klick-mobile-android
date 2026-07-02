package com.klic.mobile.app.feature.call

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Lets the in-call UI "compact" a video call into a Picture-in-Picture window (WhatsApp-style) and
 * know whether the window is currently in PiP, so it can drop the call chrome and show video only.
 * Provided by MainActivity, which owns the Activity-level PiP APIs.
 */
data class PipController(
    val supported: Boolean = false,
    val isInPipMode: Boolean = false,
    val enter: () -> Unit = {},
)

val LocalPipController = staticCompositionLocalOf { PipController() }
