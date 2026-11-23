package com.wakeguard.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.wakeguard.alarm.service.AlarmRingingService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER_ALARM) return
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        val isQuickTest = intent.getBooleanExtra(EXTRA_IS_QUICK_TEST, false)

        // Grab a short wake lock to guarantee the foreground service starts cleanly on a sleeping Pixel 9
        val pm = context.getSystemService(PowerManager::class.java)
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WakeGuard:AlarmReceiverWakeLock"
        )
        wakeLock.acquire(10_000L)

        try {
            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                action = AlarmRingingService.ACTION_START_ALARM
                putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmRingingService.EXTRA_IS_QUICK_TEST, isQuickTest)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.wakeguard.alarm.ACTION_TRIGGER_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_IS_QUICK_TEST = "extra_is_quick_test"
    }
}
