package com.wakeguard.alarm.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.wakeguard.alarm.R
import com.wakeguard.alarm.WakeGuardApp
import com.wakeguard.alarm.model.AlarmItem
import com.wakeguard.alarm.ui.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlarmRingingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var audioController: AlarmAudioController
    private var wakeLock: PowerManager.WakeLock? = null
    private var watchdogJob: Job? = null
    private var stateObserverJob: Job? = null
    private var lastRelaunchAtMs: Long = 0L
    private var overlayAnchorView: View? = null

    override fun onCreate() {
        super.onCreate()
        audioController = AlarmAudioController(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
                val isQuickTest = intent.getBooleanExtra(EXTRA_IS_QUICK_TEST, false)
                handleStartAlarm(alarmId, isQuickTest)
            }

            ACTION_RELAUNCH_ALARM -> {
                if (AlarmRingingState.isRingingActive) {
                    launchAlarmActivity(force = true)
                }
            }

            ACTION_MISSION_COMPLETED -> {
                handleMissionCompleted()
            }

            ACTION_STOP_TEST -> {
                handleStopTest()
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun handleStartAlarm(alarmId: String?, isQuickTest: Boolean) {
        val app = application as WakeGuardApp
        val alarm = (alarmId?.let { app.repository.getById(it) })
            ?: app.repository.getActiveRingingAlarm()
            ?: AlarmItem(label = "WakeGuard Alarm")

        // Persist active alarm so even a forced reboot resumes it until the mission is solved
        if (!isQuickTest) {
            app.repository.setActiveRingingAlarm(alarm)
        }

        acquireWakeLock()
        attachOverlayAnchor()
        AlarmRingingState.stateMachine.startSession(alarm, isQuickTest)

        val notification = buildAlarmNotification(alarm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        audioController.start(alarm)
        launchAlarmActivity(force = true)
        observeSessionState()
        startWatchdogLoop()
    }

    /**
     * Attaches a 1x1 transparent non-touchable overlay window while the alarm is ringing.
     * On Pixel 9 (Android 15), having an active visible overlay window guarantees instant
     * Background Activity Launch (BAL) relaunch whenever the user tries to swipe Home/Recents.
     */
    private fun attachOverlayAnchor() {
        if (overlayAnchorView != null) return
        if (!Settings.canDrawOverlays(this)) return
        runCatching {
            val wm = getSystemService(WindowManager::class.java) ?: return
            val view = View(this)
            val params = WindowManager.LayoutParams(
                1,
                1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            wm.addView(view, params)
            overlayAnchorView = view
        }
    }

    private fun detachOverlayAnchor() {
        val view = overlayAnchorView ?: return
        overlayAnchorView = null
        runCatching {
            val wm = getSystemService(WindowManager::class.java)
            wm?.removeViewImmediate(view)
        }
    }

    private fun observeSessionState() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            AlarmRingingState.session.collect { session ->
                if (session == null) return@collect
                when (session.phase) {
                    RingingPhase.PLAYING_MUTED -> audioController.setMuted(true)
                    RingingPhase.RINGING,
                    RingingPhase.CRASHED_RINGING,
                    RingingPhase.IDLE_RINGING -> audioController.setMuted(false)
                    RingingPhase.COMPLETED -> handleMissionCompleted()
                }
            }
        }
    }

    private fun startWatchdogLoop() {
        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            // Brief grace period right after launch so Activity has time to come to foreground
            val graceUntilMs = System.currentTimeMillis() + 1_200L
            while (isActive) {
                delay(150L)
                val session = AlarmRingingState.session.value ?: break
                if (!session.isActive) break

                // 1. Tick idle timeout when muted in game -> resumes alarm at 0s
                AlarmRingingState.stateMachine.tickIdleTimer()

                // 2. Enforce alarm stream volume lock & gradual fade-in while audible
                audioController.tickAudioEnforcement()

                // 3. Anti-exit watchdog: if user backgrounded AlarmActivity, unmute & yank right back!
                val now = System.currentTimeMillis()
                if (now > graceUntilMs && !AlarmRingingState.isActivityForeground) {
                    if (session.phase == RingingPhase.PLAYING_MUTED) {
                        AlarmRingingState.stateMachine.reportEscapeAttempt()
                    }
                    if (now - lastRelaunchAtMs >= 250L) {
                        lastRelaunchAtMs = now
                        launchAlarmActivity(force = true)
                    }
                }
            }
        }
    }

    private fun launchAlarmActivity(force: Boolean) {
        if (!force && AlarmRingingState.isActivityForeground) return
        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }
        runCatching { startActivity(activityIntent) }
    }

    private fun buildAlarmNotification(alarm: AlarmItem): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, WakeGuardApp.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(alarm.label.ifBlank { "WakeGuard Alarm" })
            .setContentText("Complete ${alarm.mission.displayName} to turn off alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()
    }

    private fun handleMissionCompleted() {
        val session = AlarmRingingState.session.value
        val app = application as WakeGuardApp

        watchdogJob?.cancel()
        stateObserverJob?.cancel()
        audioController.stop()
        detachOverlayAnchor()
        app.repository.setActiveRingingAlarm(null)

        // Reschedule repeating alarm for next occurrence, or turn off one-shot alarm
        if (session != null && !session.isQuickTest) {
            val stored = app.repository.getById(session.alarm.id)
            if (stored != null) {
                if (stored.repeatDays.isEmpty()) {
                    app.repository.setEnabled(stored.id, false)
                    app.scheduler.cancel(stored)
                } else {
                    app.scheduler.schedule(stored)
                }
            }
        }

        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleStopTest() {
        val session = AlarmRingingState.session.value
        if (session != null && !session.isQuickTest) return

        val app = application as WakeGuardApp
        watchdogJob?.cancel()
        stateObserverJob?.cancel()
        audioController.stop()
        detachOverlayAnchor()
        app.repository.setActiveRingingAlarm(null)
        AlarmRingingState.stateMachine.clearSession()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val session = AlarmRingingState.session.value
        if (session != null && session.isActive) {
            AlarmRingingState.stateMachine.reportEscapeAttempt()
            launchAlarmActivity(force = true)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WakeGuard:AlarmRingingWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(30 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        wakeLock = null
    }

    override fun onDestroy() {
        watchdogJob?.cancel()
        stateObserverJob?.cancel()
        audioController.stop()
        detachOverlayAnchor()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_ALARM = "com.wakeguard.alarm.service.START_ALARM"
        const val ACTION_RELAUNCH_ALARM = "com.wakeguard.alarm.service.RELAUNCH_ALARM"
        const val ACTION_MISSION_COMPLETED = "com.wakeguard.alarm.service.MISSION_COMPLETED"
        const val ACTION_STOP_TEST = "com.wakeguard.alarm.service.STOP_TEST"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_IS_QUICK_TEST = "extra_is_quick_test"
        private const val NOTIFICATION_ID = 44_001
    }
}
