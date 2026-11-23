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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeguard.alarm.games.memory.MemoryEngine
import com.wakeguard.alarm.games.memory.MemoryPhase
import com.wakeguard.alarm.model.MissionConfig
import com.wakeguard.alarm.ui.theme.WakeAmber
import com.wakeguard.alarm.ui.theme.WakeBorder
import com.wakeguard.alarm.ui.theme.WakeCoral
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeSurfaceHigh
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary
import kotlinx.coroutines.delay

@Composable
fun MemoryGameView(
    config: MissionConfig.Memory,
    paused: Boolean,
    resetSignal: Int,
    onInteraction: () -> Unit,
    onCrash: (String) -> Unit,
    onMissionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { MemoryEngine() }
    val haptics = LocalHapticFeedback.current
    var state by remember(config) { mutableStateOf(engine.createInitialState(config)) }

    LaunchedEffect(resetSignal) {
        if (resetSignal > 0 && state.phase == MemoryPhase.CRASHED) {
            state = engine.retryCurrentRound(state)
        }
    }

    LaunchedEffect(state.currentRound, state.phase, paused) {
        if (!paused && state.phase == MemoryPhase.SHOWING_PATTERN) {
            onInteraction()
            delay(state.previewDurationMs)
            state = engine.finishShowingPattern(state)
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
                text = "Round ${state.currentRound} / ${state.totalRounds}",
                style = MaterialTheme.typography.titleMedium,
                color = WakeTextPrimary
            )
            Text(
                text = "${state.foundTiles.size} / ${state.targetTiles.size} tiles",
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

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = when (state.phase) {
                MemoryPhase.SHOWING_PATTERN -> "MEMORIZE THE GLOWING TILES"
                MemoryPhase.RECALLING -> "TAP THE ${state.targetTiles.size} TILES"
                MemoryPhase.CRASHED -> "WRONG TILE!"
                MemoryPhase.WON -> "MEMORY CLEARED!"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = when (state.phase) {
                MemoryPhase.SHOWING_PATTERN -> WakeAmber
                MemoryPhase.RECALLING -> WakeEmerald
                MemoryPhase.CRASHED -> WakeCoral
                MemoryPhase.WON -> WakeEmerald
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tile Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (row in 0 until state.dimension) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (col in 0 until state.dimension) {
                        val idx = row * state.dimension + col
                        val isLit = when (state.phase) {
                            MemoryPhase.SHOWING_PATTERN -> idx in state.targetTiles
                            MemoryPhase.RECALLING -> idx in state.foundTiles
                            MemoryPhase.CRASHED -> idx in state.targetTiles || idx == state.wrongTile
                            MemoryPhase.WON -> idx in state.targetTiles
                        }
                        val isWrong = idx == state.wrongTile
                        val tileColor by animateColorAsState(
                            targetValue = when {
                                isWrong -> WakeCoral
                                isLit && state.phase == MemoryPhase.SHOWING_PATTERN -> WakeAmber
                                isLit -> WakeEmerald
                                else -> WakeSurfaceHigh
                            },
                            animationSpec = tween(180),
                            label = "tileColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                                .background(tileColor)
                                .border(1.5.dp, WakeBorder, RoundedCornerShape(18.dp))
                                .clickable(enabled = !paused && state.phase == MemoryPhase.RECALLING) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onInteraction()
                                    val next = engine.tapTile(state, idx)
                                    state = next
                                    when (next.phase) {
                                        MemoryPhase.CRASHED -> onCrash("Wrong tile on Round ${state.currentRound}! Try again.")
                                        MemoryPhase.WON -> onMissionComplete()
                                        else -> Unit
                                    }
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
