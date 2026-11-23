package com.wakeguard.alarm.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeguard.alarm.games.math.MathEngine
import com.wakeguard.alarm.games.math.MathStatus
import com.wakeguard.alarm.model.MissionConfig
import com.wakeguard.alarm.ui.theme.WakeBorder
import com.wakeguard.alarm.ui.theme.WakeCoral
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeSurface
import com.wakeguard.alarm.ui.theme.WakeSurfaceHigh
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary

@Composable
fun MathGameView(
    config: MissionConfig.Math,
    paused: Boolean,
    resetSignal: Int,
    onInteraction: () -> Unit,
    onCrash: (String) -> Unit,
    onMissionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { MathEngine() }
    val haptics = LocalHapticFeedback.current
    var state by remember(config) { mutableStateOf(engine.createInitialState(config)) }

    // Recover from a 3-wrong-answer crash when user taps Try Again
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0 && state.status == MathStatus.WRONG_STREAK_CRASH) {
            state = engine.recoverAfterCrash(state)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Solved ${state.solvedCount} / ${state.totalProblems}",
                style = MaterialTheme.typography.titleMedium,
                color = WakeTextPrimary
            )
            Text(
                text = "${config.difficulty.label} Math",
                style = MaterialTheme.typography.bodyMedium,
                color = WakeTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50)),
            color = WakeEmerald,
            trackColor = WakeSurfaceHigh,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Problem Display Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(WakeSurface)
                .border(
                    width = 2.dp,
                    color = if (state.lastAnswerWasWrong) WakeCoral else WakeBorder,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.currentProblem.expression,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = WakeTextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(WakeSurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.inputText.ifEmpty { "Enter answer" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.inputText.isEmpty()) WakeTextSecondary.copy(alpha = 0.6f) else WakeEmerald
                )
            }
            if (state.lastAnswerWasWrong) {
                Spacer(modifier = Modifier.height(10.dp))
                val left = MathEngine.MAX_WRONG_BEFORE_ALARM_RESUME - state.wrongAttemptsOnCurrent
                Text(
                    text = "Not quite! ($left ${if (left == 1) "try" else "tries"} before alarm resumes)",
                    color = WakeCoral,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Custom Calculator Numpad (lock-screen safe!)
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { key ->
                        Button(
                            onClick = {
                                if (paused) return@Button
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onInteraction()
                                state = when (key) {
                                    "C" -> engine.clearInput(state)
                                    "⌫" -> engine.backspace(state)
                                    else -> engine.appendDigit(state, key[0])
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (key == "C" || key == "⌫") WakeSurface else WakeSurfaceHigh,
                                contentColor = WakeTextPrimary
                            )
                        ) {
                            Text(text = key, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (paused || state.inputText.isEmpty()) return@Button
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onInteraction()
                    val next = engine.submit(state)
                    state = next
                    when (next.status) {
                        MathStatus.WON -> onMissionComplete()
                        MathStatus.WRONG_STREAK_CRASH -> onCrash("3 wrong answers! Wake up and try a fresh problem.")
                        MathStatus.IN_PROGRESS -> Unit
                    }
                },
                enabled = state.inputText.isNotEmpty() && !paused,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WakeEmerald,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "CHECK ANSWER", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
