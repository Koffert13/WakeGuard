package com.wakeguard.alarm.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeguard.alarm.service.AlarmRingingService
import com.wakeguard.alarm.service.AlarmRingingState
import com.wakeguard.alarm.service.RingingPhase
import com.wakeguard.alarm.service.RingingSession
import com.wakeguard.alarm.ui.games.MissionHost
import com.wakeguard.alarm.ui.theme.WakeAmber
import com.wakeguard.alarm.ui.theme.WakeBg
import com.wakeguard.alarm.ui.theme.WakeCoral
import com.wakeguard.alarm.ui.theme.WakeCoralDim
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeEmeraldDim
import com.wakeguard.alarm.ui.theme.WakeGuardTheme
import com.wakeguard.alarm.ui.theme.WakeSurface
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    private var lastBackToastMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockScreenAndImmersiveWindow()

        // Completely block system Back button and Pixel 9 edge-back gestures while alarm is active
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (AlarmRingingState.isRingingActive) {
                        val now = System.currentTimeMillis()
                        if (now - lastBackToastMs > 2000L) {
                            lastBackToastMs = now
                            Toast.makeText(
                                this@AlarmActivity,
                                "Complete the game to dismiss the alarm!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        finishAndRemoveTask()
                    }
                }
            }
        )

        setContent {
            WakeGuardTheme {
                val session by AlarmRingingState.session.collectAsStateWithLifecycle()

                LaunchedEffect(session) {
                    if (session == null) {
                        finishAndRemoveTask()
                    }
                }

                val current = session
                if (current != null) {
                    AlarmRingingScreen(
                        session = current,
                        onStartGame = {
                            AlarmRingingState.stateMachine.startOrResumePlaying()
                        },
                        onInteraction = {
                            AlarmRingingState.stateMachine.recordPlayerInteraction()
                        },
                        onCrash = { reason ->
                            AlarmRingingState.stateMachine.reportCrash(reason)
                        },
                        onMissionComplete = {
                            AlarmRingingState.stateMachine.completeMission()
                            val stopIntent = Intent(this, AlarmRingingService::class.java).apply {
                                action = AlarmRingingService.ACTION_MISSION_COMPLETED
                            }
                            startService(stopIntent)
                        },
                        onFinishClose = {
                            AlarmRingingState.stateMachine.clearSession()
                            finishAndRemoveTask()
                        },
                        onStopTest = if (current.isQuickTest) {
                            {
                                AlarmRingingState.stateMachine.clearSession()
                                val stopIntent = Intent(this, AlarmRingingService::class.java).apply {
                                    action = AlarmRingingService.ACTION_STOP_TEST
                                }
                                startService(stopIntent)
                                finishAndRemoveTask()
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private fun configureLockScreenAndImmersiveWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onResume() {
        super.onResume()
        AlarmRingingState.isActivityForeground = true
        hideSystemBars()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (AlarmRingingState.isRingingActive) {
            AlarmRingingState.stateMachine.reportEscapeAttempt()
            val relaunch = Intent(this, AlarmRingingService::class.java).apply {
                action = AlarmRingingService.ACTION_RELAUNCH_ALARM
            }
            runCatching { startService(relaunch) }
        }
    }

    override fun onPause() {
        super.onPause()
        AlarmRingingState.isActivityForeground = false
        // If user navigated away while playing muted, immediately unmute so escaping never silences the alarm
        if (AlarmRingingState.isRingingActive) {
            AlarmRingingState.stateMachine.reportEscapeAttempt()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (AlarmRingingState.isRingingActive) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_MUTE,
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_STOP,
                KeyEvent.KEYCODE_APP_SWITCH,
                KeyEvent.KEYCODE_BACK -> return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
private fun AlarmRingingScreen(
    session: RingingSession,
    onStartGame: () -> Unit,
    onInteraction: () -> Unit,
    onCrash: (String) -> Unit,
    onMissionComplete: () -> Unit,
    onFinishClose: () -> Unit,
    onStopTest: (() -> Unit)?
) {
    var resetSignal by remember { mutableIntStateOf(0) }
    var currentTimeText by remember { mutableStateOf(formatNowTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeText = formatNowTime()
            delay(1_000L)
        }
    }

    LaunchedEffect(session.phase) {
        if (session.phase == RingingPhase.COMPLETED) {
            delay(1_800L)
            onFinishClose()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = WakeBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (session.phase) {
                RingingPhase.RINGING -> {
                    WakeIntroScreen(
                        session = session,
                        currentTimeText = currentTimeText,
                        onStartGame = {
                            resetSignal++
                            onStartGame()
                        },
                        onStopTest = onStopTest
                    )
                }

                RingingPhase.PLAYING_MUTED,
                RingingPhase.CRASHED_RINGING,
                RingingPhase.IDLE_RINGING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 10.dp)
                    ) {
                        // Top Audio & Guard Status Strip
                        MissionStatusHeader(
                            session = session,
                            onStopTest = onStopTest
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Active Mini-Game
                        MissionHost(
                            mission = session.alarm.mission,
                            paused = session.phase != RingingPhase.PLAYING_MUTED,
                            resetSignal = resetSignal,
                            onInteraction = onInteraction,
                            onCrash = onCrash,
                            onMissionComplete = onMissionComplete,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Overlay when crashed or idle -> alarm has resumed!
                    AnimatedVisibility(
                        visible = session.phase == RingingPhase.CRASHED_RINGING ||
                            session.phase == RingingPhase.IDLE_RINGING,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ResumeAlarmOverlay(
                            isCrash = session.phase == RingingPhase.CRASHED_RINGING,
                            message = session.crashMessage
                                ?: "Alarm resumed! Tap below to mute and keep going.",
                            onRetryOrContinue = {
                                if (session.phase == RingingPhase.CRASHED_RINGING) {
                                    resetSignal++
                                }
                                onStartGame()
                            },
                            onStopTest = onStopTest
                        )
                    }
                }

                RingingPhase.COMPLETED -> {
                    MissionCompletedScreen(
                        alarmLabel = session.alarm.label,
                        onDone = onFinishClose
                    )
                }
            }
        }
    }
}

@Composable
private fun WakeIntroScreen(
    session: RingingSession,
    currentTimeText: String,
    onStartGame: () -> Unit,
    onStopTest: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Guard badge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(WakeCoralDim)
                .border(1.dp, WakeCoral.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = WakeCoral,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    session.isQuickTest -> "TEST MODE • ALARM RINGING"
                    session.alarm.strictExitGuard -> "EXIT LOCKED • ALARM RINGING"
                    else -> "ALARM RINGING"
                },
                color = WakeCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Big Clock & Label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(WakeEmeraldDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = WakeEmerald,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = currentTimeText,
                style = MaterialTheme.typography.displayLarge,
                fontSize = 66.sp,
                color = WakeTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = session.alarm.label.ifBlank { "Wake Up!" },
                style = MaterialTheme.typography.headlineSmall,
                color = WakeTextSecondary,
                textAlign = TextAlign.Center
            )
            if (session.escapeAttempts > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Nice try! Exit blocked (${session.escapeAttempts}×)",
                    color = WakeAmber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Mission Call-to-Action
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = session.alarm.mission.summary,
                color = WakeTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Alarm mutes automatically while you play.",
                color = WakeEmerald,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WakeEmerald,
                    contentColor = Color(0xFF042219)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START ${session.alarm.mission.displayName.uppercase()} (MUTE)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (onStopTest != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onStopTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WakeCoral.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WakeCoral
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STOP TEST",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionStatusHeader(
    session: RingingSession,
    onStopTest: (() -> Unit)?
) {
    val isMuted = session.phase == RingingPhase.PLAYING_MUTED
    val idleSecondsLeft = ((session.idleRemainingMs + 999L) / 1000L).toInt()
    val showIdleWarning = isMuted && idleSecondsLeft <= 7

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (isMuted) WakeEmeraldDim else WakeCoralDim)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                tint = if (isMuted) WakeEmerald else WakeCoral,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isMuted) "ALARM MUTED" else "ALARM RINGING",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMuted) WakeEmerald else WakeCoral
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIdleWarning) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(WakeAmber.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Move! ${idleSecondsLeft}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WakeAmber
                    )
                }
            } else if (onStopTest == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = WakeTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Exit Guard Active",
                        fontSize = 12.sp,
                        color = WakeTextSecondary
                    )
                }
            }

            if (onStopTest != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(WakeCoralDim)
                        .border(1.dp, WakeCoral.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .clickable(onClick = onStopTest)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Stop Test",
                        tint = WakeCoral,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Stop Test",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WakeCoral
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeAlarmOverlay(
    isCrash: Boolean,
    message: String,
    onRetryOrContinue: () -> Unit,
    onStopTest: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9090D12))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(WakeSurface)
                .border(2.dp, WakeCoral.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = WakeCoral,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isCrash) "CRASHED! ALARM RESUMED" else "ALARM RESUMED",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WakeCoral,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = WakeTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetryOrContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WakeEmerald,
                    contentColor = Color(0xFF042219)
                )
            ) {
                Icon(
                    imageVector = if (isCrash) Icons.Rounded.Refresh else Icons.Rounded.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCrash) "TRY AGAIN (MUTE ALARM)" else "CONTINUE (MUTE ALARM)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (onStopTest != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onStopTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WakeCoral.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WakeCoral
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STOP TEST",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionCompletedScreen(
    alarmLabel: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = WakeEmerald,
            modifier = Modifier.size(92.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Good Morning!",
            style = MaterialTheme.typography.headlineLarge,
            color = WakeTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${alarmLabel.ifBlank { "Alarm" }} dismissed.",
            style = MaterialTheme.typography.titleMedium,
            color = WakeTextSecondary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WakeEmerald,
                contentColor = Color(0xFF042219)
            )
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatNowTime(): String {
    return SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
}
