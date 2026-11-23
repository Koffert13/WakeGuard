package com.wakeguard.alarm.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import com.wakeguard.alarm.ui.AlarmActivity

/**
 * Strict Alarmy-style Anti-Exit Guard.
 *
 * Completely dormant when no alarm is actively ringing.
 * When an alarm with Strict Guard enabled is ringing:
 *  - Detects switching away to Home, Recents, Settings, Notification Shade, Power Menu, or any other app.
 *  - Dismisses system overlays (Power Menu / Notification Shade) via GLOBAL_ACTION_BACK.
 *  - Unmutes the alarm and immediately yanks [AlarmActivity] back to the foreground.
 */
class AlarmGuardAccessibilityService : AccessibilityService() {

    private var lastInterventionAtMs: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!AlarmRingingState.isStrictGuardActive) return

        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isInteractive) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        // Ignore harmless keyboard / input method windows if any
        if (pkg.contains("inputmethod", ignoreCase = true) || pkg.contains("gboard", ignoreCase = true)) {
            return
        }

        val className = event.className?.toString().orEmpty()
        val isSystemUiOverlay = pkg == "com.android.systemui" || pkg == "android"
        val isPowerOrShadeOrRecents = className.contains("GlobalActions", ignoreCase = true) ||
            className.contains("Power", ignoreCase = true) ||
            className.contains("Shade", ignoreCase = true) ||
            className.contains("StatusBar", ignoreCase = true) ||
            className.contains("Recents", ignoreCase = true) ||
            className.contains("Dialog", ignoreCase = true)

        // If AlarmActivity is in the foreground, ignore passive SystemUI status bar ticks unless it's an overlay/dialog
        if (AlarmRingingState.isActivityForeground && isSystemUiOverlay && !isPowerOrShadeOrRecents) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastInterventionAtMs < 220L) return
        lastInterventionAtMs = now

        AlarmRingingState.stateMachine.reportEscapeAttempt()

        // Close Power Menu, Notification Shade, or System Dialog first
        if (isSystemUiOverlay || isPowerOrShadeOrRecents) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }

        val relaunchIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }
        runCatching { startActivity(relaunchIntent) }
    }

    override fun onInterrupt() {
        // Nothing to clean up
    }
}
