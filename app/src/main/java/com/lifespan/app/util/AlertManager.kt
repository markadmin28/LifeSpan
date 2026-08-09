package com.lifespan.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.lifespan.app.domain.alert.AlertType

/**
 * Produces audible tones and vibration for battery alerts, with a per-type
 * cooldown so a sustained condition (e.g. staying above the overheat threshold)
 * does not fire continuously.
 */
class AlertManager(
    private val context: Context,
    private val cooldownMillis: Long = 60_000L,
) {
    private val lastFired = mutableMapOf<AlertType, Long>()
    private var alarmPlayer: MediaPlayer? = null

    val persistentAlarmActive: Boolean
        get() = alarmPlayer != null

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Fire alerts that are not currently within their cooldown window. */
    fun maybeAlert(alerts: Set<AlertType>) {
        if (alerts.isEmpty()) return
        val now = SystemClock.elapsedRealtime()
        var fired = false
        for (alert in alerts) {
            val last = lastFired[alert] ?: 0L
            if (now - last >= cooldownMillis) {
                lastFired[alert] = now
                fired = true
            }
        }
        if (fired) {
            val urgent = alerts.contains(AlertType.OVERHEAT) ||
                alerts.contains(AlertType.RAPID_TEMP_RISE)
            vibrate(urgent)
            playTone(urgent)
        }
    }

    /** Start a looping alarm that persists until [stopPersistentAlarm] is called. */
    fun startPersistentAlarm() {
        if (alarmPlayer != null) return
        runCatching {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            alarmPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        }
    }

    fun stopPersistentAlarm() {
        alarmPlayer?.let { player -> runCatching { player.stop() }; runCatching { player.release() } }
        alarmPlayer = null
    }

    private fun vibrate(urgent: Boolean) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        val pattern = if (urgent) longArrayOf(0, 400, 200, 400, 200, 400) else longArrayOf(0, 250, 150, 250)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, -1)
        }
    }

    private fun playTone(urgent: Boolean) {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_ALARM, 90)
            val toneType =
                if (urgent) ToneGenerator.TONE_CDMA_HIGH_L else ToneGenerator.TONE_PROP_BEEP2
            tone.startTone(toneType, 800)
            // Release after the tone finishes without blocking the caller.
            android.os.Handler(context.mainLooper).postDelayed({ tone.release() }, 1_000)
        }
    }
}
