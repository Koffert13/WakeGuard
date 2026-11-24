package com.wakeguard.alarm.service

import com.wakeguard.alarm.model.AlarmItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSessionStateMachineTest {

    @Test
    fun `full lifecycle mutes during play, resumes on crash or idle or escape, and stops on win`() {
        var fakeTime = 1_000L
        val sm = AlarmSessionStateMachine(clock = { fakeTime })
        val alarm = AlarmItem(idleTimeoutSeconds = 15)

        // 1. Alarm triggers -> loud ringing
        sm.startSession(alarm, isQuickTest = false)
        assertEquals(RingingPhase.RINGING, sm.session.value?.phase)
        assertTrue(sm.session.value!!.shouldPlaySound)

        // 2. Player taps Start -> muted!
        sm.startOrResumePlaying()
        assertEquals(RingingPhase.PLAYING_MUTED, sm.session.value?.phase)
        assertFalse(sm.session.value!!.shouldPlaySound)

        // 3. Player moves after 10s -> still muted, timer refreshed
        fakeTime += 10_000L
        assertFalse(sm.tickIdleTimer())
        sm.recordPlayerInteraction()
        assertEquals(15_000L, sm.session.value!!.idleRemainingMs)

        // 4. Player crashes -> alarm IMMEDIATELY resumes!
        sm.reportCrash("Hit the wall")
        assertEquals(RingingPhase.CRASHED_RINGING, sm.session.value?.phase)
        assertTrue(sm.session.value!!.shouldPlaySound)

        // 5. Player retries -> muted again
        sm.startOrResumePlaying()
        assertFalse(sm.session.value!!.shouldPlaySound)

        // 6. Player tries to exit app -> immediately unmutes & records escape attempt
        sm.reportEscapeAttempt()
        assertEquals(RingingPhase.IDLE_RINGING, sm.session.value?.phase)
        assertTrue(sm.session.value!!.shouldPlaySound)
        assertEquals(1, sm.session.value!!.escapeAttempts)

        // 7. Player continues, then goes idle for 15s -> alarm resumes!
        sm.startOrResumePlaying()
        assertFalse(sm.session.value!!.shouldPlaySound)
        fakeTime += 15_000L
        assertTrue(sm.tickIdleTimer())
        assertEquals(RingingPhase.IDLE_RINGING, sm.session.value?.phase)
        assertTrue(sm.session.value!!.shouldPlaySound)

        // 8. Player completes mission -> alarm permanently dismissed
        sm.startOrResumePlaying()
        sm.completeMission()
        assertEquals(RingingPhase.COMPLETED, sm.session.value?.phase)
        assertFalse(sm.session.value!!.shouldPlaySound)
        assertFalse(sm.session.value!!.isActive)
    }

    @Test
    fun `focus loss escape unmutes playing session with custom overlay message`() {
        val sm = AlarmSessionStateMachine(clock = { 5_000L })
        sm.startSession(AlarmItem(), isQuickTest = false)
        sm.startOrResumePlaying()
        assertFalse(sm.session.value!!.shouldPlaySound)

        val msg = "Close overlays and complete the mission to stop the alarm."
        sm.reportEscapeAttempt(message = msg)

        val session = sm.session.value!!
        assertEquals(RingingPhase.IDLE_RINGING, session.phase)
        assertTrue(session.shouldPlaySound)
        assertEquals(msg, session.crashMessage)
        assertEquals(1, session.escapeAttempts)
    }

    @Test
    fun `resetWindowTracking clears foreground and focus flags`() {
        AlarmRingingState.isActivityForeground = true
        AlarmRingingState.isActivityWindowFocused = true
        AlarmRingingState.lastWindowFocusLostAtMs = 123_456L

        AlarmRingingState.resetWindowTracking()

        assertFalse(AlarmRingingState.isActivityForeground)
        assertFalse(AlarmRingingState.isActivityWindowFocused)
        assertEquals(0L, AlarmRingingState.lastWindowFocusLostAtMs)
    }

    @Test
    fun `guard permissions are ready with 5 BankID-safe permissions even without accessibility`() {
        val status = com.wakeguard.alarm.permissions.GuardPermissionsStatus(
            notificationsGranted = true,
            exactAlarmGranted = true,
            fullScreenIntentGranted = true,
            overlayGranted = true,
            batteryUnrestricted = true,
            accessibilityGuardEnabled = false,
            hasAccessibilityGuard = true
        )

        assertTrue(status.allGranted)
        assertEquals(5, status.grantedCount)
        assertEquals(5, status.totalCount)
    }
}
