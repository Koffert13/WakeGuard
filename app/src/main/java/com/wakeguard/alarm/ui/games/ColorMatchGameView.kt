package com.wakeguard.alarm.ui.games

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeguard.alarm.games.colormatch.ColorMatchEngine
import com.wakeguard.alarm.games.colormatch.ColorMatchStatus
import com.wakeguard.alarm.games.colormatch.ColorPromptMode
import com.wakeguard.alarm.model.MissionConfig
import com.wakeguard.alarm.ui.theme.WakeAmber
import com.wakeguard.alarm.ui.theme.WakeBorder
import com.wakeguard.alarm.ui.theme.WakeCoral
import com.wakeguard.alarm.ui.theme.WakeCoralDim
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeEmeraldDim
import com.wakeguard.alarm.ui.theme.WakeSurface
import com.wakeguard.alarm.ui.theme.WakeSurfaceHigh
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary
import kotlinx.coroutines.delay

@Composable
fun ColorMatchGameView(
    config: MissionConfig.ColorMatch,
    paused: Boolean,
    resetSignal: Int,
    onInteraction: () -> Unit,
    onCrash: (String) -> Unit,
    onMissionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { ColorMatchEngine() }
    val haptics = LocalHapticFeedback.current
    var state by remember(config) { mutableStateOf(engine.createInitialState(config)) }
    var timeFraction by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(resetSignal) {
        if (resetSignal > 0 && state.status == ColorMatchStatus.CRASHED) {
            state = engine.recoverAfterCrash(state)
        }
    }

    // Per-round countdown timer
    LaunchedEffect(state.solvedCount, state.mistakes, state.status, paused) {
        if (paused || state.status != ColorMatchStatus.IN_PROGRESS) return@LaunchedEffect
        val totalMs = state.roundTimeLimitMs
        var elapsed = 0L
        val stepMs = 50L
        timeFraction = 1f
        while (elapsed < totalMs && !paused && state.status == ColorMatchStatus.IN_PROGRESS) {
            delay(stepMs)
            elapsed += stepMs
            timeFraction = (1f - elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
        }
        if (!paused && state.status == ColorMatchStatus.IN_PROGRESS) {
            val next = engine.onTimeout(state)
            state = next
            if (next.status == ColorMatchStatus.CRASHED) {
                onCrash(next.crashReason ?: "Too slow! Alarm resumed.")
            }
        }
    }

    val mistakesLeft = (state.maxMistakes - state.mistakes).coerceAtLeast(0)
    val isWordMode = state.challenge.mode == ColorPromptMode.MATCH_WORD

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Round ${state.solvedCount + 1} / ${state.totalRounds}",
                style = MaterialTheme.typography.titleMedium,
                color = WakeTextPrimary
            )
            Text(
                text = "${config.difficulty.label} • $mistakesLeft tries left",
                style = MaterialTheme.typography.bodyMedium,
                color = if (mistakesLeft <= 1) WakeCoral else WakeTextSecondary
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

        Spacer(modifier = Modifier.height(16.dp))

        // Prompt Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(WakeSurface)
                .border(1.5.dp, WakeBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isWordMode) WakeCoralDim else WakeEmeraldDim)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isWordMode) "TAP THE WORD" else "TAP THE INK COLOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isWordMode) WakeCoral else WakeEmerald
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = state.challenge.word.label,
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(state.challenge.inkColor.argb),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            LinearProgressIndicator(
                progress = { timeFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (timeFraction < 0.3f) WakeCoral else WakeAmber,
                trackColor = WakeSurfaceHigh,
                strokeCap = StrokeCap.Round
            )

            if (state.crashReason != null && state.status == ColorMatchStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = state.crashReason!!,
                    color = WakeCoral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Color option buttons in 2 columns
        val rows = state.challenge.options.chunked(2)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { option ->
                        val optionColor = Color(option.argb)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(WakeSurfaceHigh)
                                .border(1.5.dp, optionColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                                .clickable(enabled = !paused && state.status == ColorMatchStatus.IN_PROGRESS) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onInteraction()
                                    val next = engine.submit(state, option)
                                    state = next
                                    when (next.status) {
                                        ColorMatchStatus.CRASHED -> onCrash(next.crashReason ?: "Wrong color! Try again.")
                                        ColorMatchStatus.WON -> onMissionComplete()
                                        ColorMatchStatus.IN_PROGRESS -> Unit
                                    }
                                }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(optionColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = option.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WakeTextPrimary
                            )
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
