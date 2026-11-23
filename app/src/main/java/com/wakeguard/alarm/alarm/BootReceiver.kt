package com.wakeguard.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.wakeguard.alarm.data.AlarmRepository
import com.wakeguard.alarm.service.AlarmRingingService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
        if (action !in validActions) return

        val repo = AlarmRepository(context)
        val scheduler = AlarmScheduler(context)
        scheduler.rescheduleAll(repo.getAll())

        // If the phone was rebooted while an alarm was still actively ringing, resume it immediately!
        val unfinishedAlarm = repo.getActiveRingingAlarm()
        if (unfinishedAlarm != null) {
            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                this.action = AlarmRingingService.ACTION_START_ALARM
                putExtra(AlarmRingingService.EXTRA_ALARM_ID, unfinishedAlarm.id)
                putExtra(AlarmRingingService.EXTRA_IS_QUICK_TEST, false)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
