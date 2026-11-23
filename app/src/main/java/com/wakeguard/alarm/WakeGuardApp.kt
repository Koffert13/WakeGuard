package com.wakeguard.alarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import com.wakeguard.alarm.alarm.AlarmScheduler
import com.wakeguard.alarm.data.AlarmRepository

class WakeGuardApp : Application() {

    lateinit var repository: AlarmRepository
        private set

    lateinit var scheduler: AlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AlarmRepository(this)
        scheduler = AlarmScheduler(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        // Sound is played directly by AlarmAudioController for precise mute/unmute & volume locking.
        // Keep the channel silent while MAX importance so Full-Screen Intent wakes Pixel 9 reliably.
        val alarmChannel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Active Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen notifications for ringing WakeGuard alarms"
            setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        nm.createNotificationChannel(alarmChannel)
    }

    companion object {
        const val ALARM_CHANNEL_ID = "wakeguard_active_alarm_v1"
    }
}
