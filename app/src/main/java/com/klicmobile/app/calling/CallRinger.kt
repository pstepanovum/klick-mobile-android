package com.klicmobile.app.calling

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.klicmobile.app.R

/**
 * Plays a looping ringtone + vibration for an incoming call, honouring the ringer mode
 * (silent → nothing, vibrate → vibrate only, normal → ring + vibrate). Owned by the
 * incoming-call UI; call [start] when the call appears and [stop] on accept/decline/end.
 */
class CallRinger(private val context: Context) {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun start() {
        val audio = context.getSystemService(AudioManager::class.java)
        when (audio.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> return
            AudioManager.RINGER_MODE_VIBRATE -> startVibration()
            else -> { startRingtone(); startVibration() }
        }
    }

    private fun startRingtone() {
        // Klic's own bundled ringtone (res/raw/ringtone.wav), looped for the duration of the ring.
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.ringtone}")
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        }
    }

    private fun startVibration() {
        val vib = vibrator()
        if (vib?.hasVibrator() != true) return
        vibrator = vib
        // 1s on, 1s off, repeating from index 0.
        val pattern = longArrayOf(0, 1000, 1000)
        vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }
}
