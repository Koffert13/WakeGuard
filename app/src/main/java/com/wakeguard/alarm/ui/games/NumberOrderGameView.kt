package com.wakeguard.alarm.ui.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeguard.alarm.games.numberorder.NumberOrderEngine
import com.wakeguard.alarm.games.numberorder.NumberOrderStatus
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

@Composable
fun NumberOrderGameView(
    config: MissionConfig.NumberOrder,
    paused: Boolean,
    resetSignal: Int,
    onInteraction: () -> Unit,
    onCrash: (String) -> Unit,
    onMissionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { NumberOrderEngine() }
    val haptics = LocalHapticFeedback.current
    var state by remember(config) { mutableStateOf(engine.createInitialState(config)) }

    LaunchedEffect(resetSignal) {
        if (resetSignal > 0 && state.status == NumberOrderStatus.CRASHED) {
            state = engine.retryCurrentRound(state)
        }
    }

    val mistakesLeft = (state.maxMistakes - state.mistakes).coerceAtLeast(0)

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
                text = "Round ${state.currentRound} / ${state.totalRounds}",
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (state.descending) WakeCoralDim else WakeEmeraldDim)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (state.descending) "REVERSE ${state.totalTiles} → 1" else "ORDER 1 → ${state.totalTiles}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.descending) WakeCoral else WakeEmerald
                )
            }

            Text(
                text = "Next: ${state.nextExpected}",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WakeAmber
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Number Grid
        val boardAspect = state.cols.toFloat() / state.rows.toFloat()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .aspectRatio(boardAspect)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (r in 0 until state.rows) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (c in 0 until state.cols) {
                            val idx = r * state.cols + c
                            val num = state.tiles[idx]
                            val isCleared = num in state.clearedNumbers
                            val isWrong = state.wrongNumber == num

                            val bgColor by animateColorAsState(
                                targetValue = when {
                                    isWrong -> WakeCoral
                                    isCleared -> WakeEmeraldDim
                                    else -> WakeSurfaceHigh
                                },
                                animationSpec = tween(140),
                                label = "numBg"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isCleared) WakeSurface else bgColor)
                                    .border(
                                        width = 1.5.dp,
                                        color = when {
                                            isWrong -> WakeCoral
                                            isCleared -> WakeEmerald.copy(alpha = 0.4f)
                                            else -> WakeBorder
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .alpha(if (isCleared) 0.4f else 1f)
                                    .clickable(enabled = !paused && !isCleared && state.status == NumberOrderStatus.IN_PROGRESS) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onInteraction()
                                        val next = engine.tapNumber(state, num)
                                        state = next
                                        when (next.status) {
                                            NumberOrderStatus.CRASHED -> onCrash(next.crashReason ?: "Wrong number! Try again.")
                                            NumberOrderStatus.WON -> onMissionComplete()
                                            NumberOrderStatus.IN_PROGRESS -> Unit
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = num.toString(),
                                    fontSize = if (state.cols >= 4) 26.sp else 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCleared) WakeEmerald else WakeTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
