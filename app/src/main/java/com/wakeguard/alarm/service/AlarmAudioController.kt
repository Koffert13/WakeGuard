package com.wakeguard.alarm.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.wakeguard.alarm.model.AlarmItem
import kotlin.math.roundToInt

class AlarmAudioController(private val context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var mediaPlayer: MediaPlayer? = null
    private var fallbackTone: ToneGenerator? = null
    private var currentAlarm: AlarmItem? = null
    private var isMuted: Boolean = false
    private var isPlaying: Boolean = false
    private var targetStreamVolume: Int = 1
    private var startedAtMs: Long = 0L

    fun start(alarm: AlarmItem) {
        stop()
        currentAlarm = alarm
        isMuted = false
        isPlaying = true
        startedAtMs = System.currentTimeMillis()

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        targetStreamVolume = ((alarm.volumePercent.coerceIn(10, 100) / 100f) * maxVol)
            .roundToInt()
            .coerceIn(1, maxVol)

        enforceStreamVolume()
        startAudioPlayer(alarm)
        if (alarm.vibrate) {
            startVibration()
        }
    }

    private fun startAudioPlayer(alarm: AlarmItem) {
        val uri: Uri? = alarm.soundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val player = MediaPlayer()
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            if (uri != null) {
                player.setDataSource(context, uri)
                player.isLooping = true
                player.prepare()
                val initialGain = if (alarm.gradualVolume) 0.15f else 1.0f
                player.setVolume(initialGain, initialGain)
                player.start()
                mediaPlayer = player
                return
            }
        } catch (_: Exception) {
            runCatching { player.release() }
        }

        // Guaranteed audible fallback if no system ringtone URI could be opened
        runCatching {
            fallbackTone = ToneGenerator(AudioManager.STREAM_ALARM, 100).apply {
                startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK)
            }
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 350, 500, 350)
        val effect = VibrationEffect.createWaveform(pattern, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vibrator.vibrate(effect, attrs)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect)
        }
    }

    fun setMuted(muted: Boolean) {
        if (!isPlaying || isMuted == muted) return
        isMuted = muted
        val alarm = currentAlarm

        if (muted) {
            runCatching {
                mediaPlayer?.setVolume(0f, 0f)
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            }
            runCatching { fallbackTone?.stopTone() }
            runCatching { vibrator.cancel() }
        } else {
            enforceStreamVolume()
            runCatching {
                val player = mediaPlayer
                if (player != null) {
                    val gain = computeGain()
                    player.setVolume(gain, gain)
                    if (!player.isPlaying) {
                        player.start()
                    }
                } else if (alarm != null) {
                    startAudioPlayer(alarm)
                }
            }
            if (alarm?.vibrate == true) {
                startVibration()
            }
        }
    }

    /**
     * Called periodically by AlarmRingingService to lock alarm volume against accidental
     * hardware Volume-Down presses and advance gradual fade-in if enabled.
     */
    fun tickAudioEnforcement() {
        if (!isPlaying || isMuted) return
        val alarm = currentAlarm ?: return

        if (alarm.strictExitGuard) {
            enforceStreamVolume()
        }
        if (alarm.gradualVolume) {
            val gain = computeGain()
            runCatching { mediaPlayer?.setVolume(gain, gain) }
        }
    }

    private fun computeGain(): Float {
        val alarm = currentAlarm ?: return 1f
        if (!alarm.gradualVolume) return 1f
        val elapsedSec = (System.currentTimeMillis() - startedAtMs) / 1000f
        return (0.15f + (elapsedSec / 25f) * 0.85f).coerceIn(0.15f, 1.0f)
    }

    private fun enforceStreamVolume() {
        runCatching {
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (currentVol < targetStreamVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetStreamVolume, 0)
            }
        }
    }

    fun stop() {
        isPlaying = false
        isMuted = false
        currentAlarm = null

        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null

        runCatching {
            fallbackTone?.stopTone()
            fallbackTone?.release()
        }
        fallbackTone = null

        runCatching { vibrator.cancel() }
    }
}
