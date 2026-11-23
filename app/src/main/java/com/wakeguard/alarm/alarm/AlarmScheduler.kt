package com.wakeguard.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakeguard.alarm.model.AlarmItem
import com.wakeguard.alarm.ui.MainActivity

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(alarm: AlarmItem): Long? {
        if (!alarm.enabled) {
            cancel(alarm)
            return null
        }
        val triggerAt = AlarmTimeCalculator.nextTriggerMillis(alarm)
        scheduleAtExact(alarm.id, alarm.requestCode, triggerAt, isQuickTest = false)
        return triggerAt
    }

    fun scheduleQuickTest(alarmId: String, delaySeconds: Int = 5): Long {
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1000L
        scheduleAtExact(alarmId, QUICK_TEST_REQUEST_CODE, triggerAt, isQuickTest = true)
        return triggerAt
    }

    private fun scheduleAtExact(
        alarmId: String,
        requestCode: Int,
        triggerAtMillis: Long,
        isQuickTest: Boolean
    ) {
        val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_IS_QUICK_TEST, isQuickTest)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
        try {
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } catch (_: SecurityException) {
            // Fallback if exact alarm permission was revoked
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(alarm: AlarmItem) {
        val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.requestCode,
            receiverIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAll(alarms: List<AlarmItem>) {
        alarms.forEach { alarm ->
            if (alarm.enabled) {
                schedule(alarm)
            } else {
                cancel(alarm)
            }
        }
    }

    companion object {
        private const val QUICK_TEST_REQUEST_CODE = 991_337
    }
}
