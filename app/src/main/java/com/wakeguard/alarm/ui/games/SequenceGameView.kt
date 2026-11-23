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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeguard.alarm.games.sequence.SequenceEngine
import com.wakeguard.alarm.games.sequence.SequencePhase
import com.wakeguard.alarm.model.MissionConfig
import com.wakeguard.alarm.ui.theme.WakeAmber
import com.wakeguard.alarm.ui.theme.WakeCoral
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeSurfaceHigh
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PadColors = listOf(
    Color(0xFF34D399), // Emerald
    Color(0xFF38BDF8), // Sky
    Color(0xFFFBBF24), // Amber
    Color(0xFFFB7185), // Coral
    Color(0xFFC084FC), // Purple
    Color(0xFFFB923C)  // Orange
)

@Composable
fun SequenceGameView(
    config: MissionConfig.Sequence,
    paused: Boolean,
    resetSignal: Int,
    onInteraction: () -> Unit,
    onCrash: (String) -> Unit,
    onMissionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { SequenceEngine() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var state by remember(config) { mutableStateOf(engine.createInitialState(config)) }
    var litPad by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(resetSignal) {
        if (resetSignal > 0 && state.phase == SequencePhase.CRASHED) {
            litPad = null
            state = engine.retryCurrentRound(state)
        }
    }

    // Play demo sequence
    LaunchedEffect(state.currentRound, state.phase, paused) {
        if (paused || state.phase != SequencePhase.PLAYING_DEMO) return@LaunchedEffect
        litPad = null
        delay(450L)
        for (padIdx in state.sequence) {
            if (paused) return@LaunchedEffect
            onInteraction()
            litPad = padIdx
            delay(state.stepHighlightMs)
            litPad = null
            delay(state.stepGapMs)
        }
        onInteraction()
        state = engine.finishDemo(state)
    }

    val cols = 2
    val rows = state.padCount / 2

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
                text = "${config.difficulty.label} • ${state.sequence.size} steps",
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

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = when (state.phase) {
                SequencePhase.PLAYING_DEMO -> "WATCH THE SEQUENCE..."
                SequencePhase.WAITING_INPUT -> "YOUR TURN (${state.inputIndex} / ${state.sequence.size})"
                SequencePhase.CRASHED -> "WRONG PAD!"
                SequencePhase.WON -> "SEQUENCE CLEARED!"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = when (state.phase) {
                SequencePhase.PLAYING_DEMO -> WakeAmber
                SequencePhase.WAITING_INPUT -> WakeEmerald
                SequencePhase.CRASHED -> WakeCoral
                SequencePhase.WON -> WakeEmerald
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        val boardAspect = cols.toFloat() / rows.toFloat()
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (c in 0 until cols) {
                            val padIdx = r * cols + c
                            val baseColor = PadColors[padIdx % PadColors.size]
                            val isLit = litPad == padIdx
                            val isWrong = state.wrongPad == padIdx

                            val padColor by animateColorAsState(
                                targetValue = when {
                                    isWrong -> WakeCoral
                                    isLit -> baseColor
                                    else -> baseColor.copy(alpha = 0.22f)
                                },
                                animationSpec = tween(120),
                                label = "padColor"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(padColor)
                                    .border(
                                        width = if (isLit) 3.dp else 1.5.dp,
                                        color = if (isLit) Color.White.copy(alpha = 0.85f) else baseColor.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable(enabled = !paused && state.phase == SequencePhase.WAITING_INPUT) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onInteraction()
                                        litPad = padIdx
                                        scope.launch {
                                            delay(160L)
                                            if (litPad == padIdx) litPad = null
                                        }
                                        val next = engine.tapPad(state, padIdx)
                                        state = next
                                        when (next.phase) {
                                            SequencePhase.CRASHED -> onCrash("Wrong pad on Round ${state.currentRound}! Watch again.")
                                            SequencePhase.WON -> onMissionComplete()
                                            else -> Unit
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${padIdx + 1}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isLit) Color(0xFF041410) else WakeTextPrimary.copy(alpha = 0.85f)
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
