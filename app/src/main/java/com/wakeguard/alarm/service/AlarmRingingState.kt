package com.wakeguard.alarm.service

import com.wakeguard.alarm.model.AlarmItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RingingPhase {
    /** Alarm is ringing loudly; user hasn't started the mission yet. */
    RINGING,

    /** User is actively playing the mini-game; alarm sound & vibration are MUTED. */
    PLAYING_MUTED,

    /** User crashed / made a fatal mistake in the game; alarm IMMEDIATELY resumed! */
    CRASHED_RINGING,

    /** User stopped interacting for [idleTimeoutSeconds]; alarm IMMEDIATELY resumed! */
    IDLE_RINGING,

    /** Mission completed! Alarm is permanently silenced and dismissed. */
    COMPLETED
}

data class RingingSession(
    val alarm: AlarmItem,
    val isQuickTest: Boolean,
    val phase: RingingPhase = RingingPhase.RINGING,
    val crashMessage: String? = null,
    val lastInteractionAtMs: Long = System.currentTimeMillis(),
    val idleRemainingMs: Long = alarm.idleTimeoutSeconds * 1000L,
    val escapeAttempts: Int = 0
) {
    val shouldPlaySound: Boolean
        get() = phase == RingingPhase.RINGING ||
            phase == RingingPhase.CRASHED_RINGING ||
            phase == RingingPhase.IDLE_RINGING

    val isActive: Boolean
        get() = phase != RingingPhase.COMPLETED
}

/**
 * Pure, testable state controller for an active alarm session.
 */
class AlarmSessionStateMachine(
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val _session = MutableStateFlow<RingingSession?>(null)
    val session: StateFlow<RingingSession?> = _session.asStateFlow()

    fun startSession(alarm: AlarmItem, isQuickTest: Boolean) {
        val now = clock()
        _session.value = RingingSession(
            alarm = alarm,
            isQuickTest = isQuickTest,
            phase = RingingPhase.RINGING,
            crashMessage = null,
            lastInteractionAtMs = now,
            idleRemainingMs = alarm.idleTimeoutSeconds * 1000L,
            escapeAttempts = 0
        )
    }

    fun startOrResumePlaying() {
        val now = clock()
        _session.update { current ->
            if (current == null || current.phase == RingingPhase.COMPLETED) return@update current
            current.copy(
                phase = RingingPhase.PLAYING_MUTED,
                crashMessage = null,
                lastInteractionAtMs = now,
                idleRemainingMs = current.alarm.idleTimeoutSeconds * 1000L
            )
        }
    }

    fun recordPlayerInteraction() {
        val now = clock()
        _session.update { current ->
            if (current == null || current.phase != RingingPhase.PLAYING_MUTED) return@update current
            current.copy(
                lastInteractionAtMs = now,
                idleRemainingMs = current.alarm.idleTimeoutSeconds * 1000L
            )
        }
    }

    fun reportCrash(reason: String) {
        _session.update { current ->
            if (current == null || current.phase == RingingPhase.COMPLETED) return@update current
            current.copy(
                phase = RingingPhase.CRASHED_RINGING,
                crashMessage = reason
            )
        }
    }

    fun reportEscapeAttempt() {
        _session.update { current ->
            if (current == null || current.phase == RingingPhase.COMPLETED) return@update current
            val nextPhase = if (current.phase == RingingPhase.PLAYING_MUTED) {
                RingingPhase.IDLE_RINGING
            } else {
                current.phase
            }
            current.copy(
                phase = nextPhase,
                crashMessage = "Nice try! Complete the mission to stop the alarm.",
                escapeAttempts = current.escapeAttempts + 1
            )
        }
    }

    /**
     * Evaluates inactivity timeout while in [RingingPhase.PLAYING_MUTED].
     * Returns true if the session just transitioned to [RingingPhase.IDLE_RINGING].
     */
    fun tickIdleTimer(): Boolean {
        val current = _session.value ?: return false
        if (current.phase != RingingPhase.PLAYING_MUTED) return false

        val timeoutMs = current.alarm.idleTimeoutSeconds * 1000L
        val elapsed = (clock() - current.lastInteractionAtMs).coerceAtLeast(0L)
        val remaining = (timeoutMs - elapsed).coerceAtLeast(0L)

        return if (remaining == 0L) {
            _session.value = current.copy(
                phase = RingingPhase.IDLE_RINGING,
                idleRemainingMs = 0L,
                crashMessage = "Still awake? Tap Continue to mute & keep playing."
            )
            true
        } else {
            _session.value = current.copy(idleRemainingMs = remaining)
            false
        }
    }

    fun completeMission() {
        _session.update { current ->
            current?.copy(phase = RingingPhase.COMPLETED, crashMessage = null)
        }
    }

    fun clearSession() {
        _session.value = null
    }
}

/**
 * Process-wide singleton coordinating [AlarmRingingService], [AlarmActivity], and [AlarmGuardAccessibilityService].
 */
object AlarmRingingState {
    val stateMachine = AlarmSessionStateMachine()
    val session: StateFlow<RingingSession?> get() = stateMachine.session

    @Volatile
    var isActivityForeground: Boolean = false

    val isRingingActive: Boolean
        get() = session.value?.isActive == true

    val isStrictGuardActive: Boolean
        get() {
            val current = session.value ?: return false
            return current.isActive && current.alarm.strictExitGuard
        }
}
