package com.wakeguard.alarm.alarm

import com.wakeguard.alarm.model.AlarmItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AlarmTimeCalculatorTest {

    @Test
    fun `one-shot alarm earlier than now schedules for tomorrow`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 23, 8, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val alarm = AlarmItem(hour = 7, minute = 0, repeatDays = emptySet())
        val next = AlarmTimeCalculator.nextTriggerMillis(alarm, now.timeInMillis)
        val result = Calendar.getInstance().apply { timeInMillis = next }

        assertEquals(24, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, result.get(Calendar.MINUTE))
    }

    @Test
    fun `repeating weekday alarm on Friday evening skips weekend to Monday`() {
        // Nov 20, 2026 is a Friday
        val fridayEvening = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 20, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val alarm = AlarmItem(
            hour = 6,
            minute = 45,
            repeatDays = setOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
            )
        )
        val next = AlarmTimeCalculator.nextTriggerMillis(alarm, fridayEvening.timeInMillis)
        val result = Calendar.getInstance().apply { timeInMillis = next }

        assertEquals(Calendar.MONDAY, result.get(Calendar.DAY_OF_WEEK))
        assertEquals(23, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(6, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, result.get(Calendar.MINUTE))
        assertTrue(next > fridayEvening.timeInMillis)
    }
}
