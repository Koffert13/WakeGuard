package com.wakeguard.alarm.alarm

import com.wakeguard.alarm.model.AlarmItem
import java.util.Calendar

object AlarmTimeCalculator {

    /**
     * Computes the next trigger timestamp in epoch millis strictly in the future relative to [nowMillis].
     */
    fun nextTriggerMillis(alarm: AlarmItem, nowMillis: Long = System.currentTimeMillis()): Long {
        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDays.isEmpty()) {
            if (candidate.timeInMillis <= nowMillis) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return candidate.timeInMillis
        }

        // Check today through the next 7 days to find the earliest matching day in the future
        for (offset in 0..7) {
            val probe = (candidate.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val dayOfWeek = probe.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek in alarm.repeatDays && probe.timeInMillis > nowMillis) {
                return probe.timeInMillis
            }
        }

        // Fallback (should never happen when repeatDays is non-empty valid Calendar days)
        candidate.add(Calendar.DAY_OF_YEAR, 1)
        return candidate.timeInMillis
    }

    /**
     * Formats a concise countdown string like "in 7h 42m" or "in <1m".
     */
    fun formatTimeUntil(triggerMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffMs = (triggerMillis - nowMillis).coerceAtLeast(0L)
        val totalMinutes = diffMs / 60_000L
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "in ${days}d ${hours}h"
            hours > 0 && minutes > 0 -> "in ${hours}h ${minutes}m"
            hours > 0 -> "in ${hours}h"
            minutes > 0 -> "in ${minutes}m"
            else -> "in <1m"
        }
    }
}
