package com.wakeguard.alarm.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeguard.alarm.games.colormatch.NamedColor
import com.wakeguard.alarm.model.GameDifficulty
import com.wakeguard.alarm.model.MathDifficulty
import com.wakeguard.alarm.model.MissionConfig
import com.wakeguard.alarm.ui.theme.WakeAccent
import com.wakeguard.alarm.ui.theme.WakeAmber
import com.wakeguard.alarm.ui.theme.WakeBg
import com.wakeguard.alarm.ui.theme.WakeBorder
import com.wakeguard.alarm.ui.theme.WakeCoral
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeSurface
import com.wakeguard.alarm.ui.theme.WakeSurfaceHigh
import com.wakeguard.alarm.ui.theme.WakeTextMuted
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary
import kotlinx.coroutines.delay

/**
 * Live Alarmy-style visual preview of the currently selected mini-game and difficulty.
 */
@Composable
fun GamePreviewCard(
    mission: MissionConfig,
    onPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WakeBg)
            .border(1.dp, WakeBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(WakeAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Preview · ${mission.displayName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = WakeTextSecondary
                )
            }
            Text(
                text = missionHintText(mission),
                fontSize = 12.sp,
                color = WakeTextMuted,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(WakeSurface)
                .border(1.dp, WakeBorder, RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (mission) {
                is MissionConfig.Snake -> SnakePreview(mission)
                is MissionConfig.Math -> MathPreview(mission)
                is MissionConfig.Memory -> MemoryPreview(mission)
                is MissionConfig.ColorMatch -> ColorTrapPreview(mission)
                is MissionConfig.NumberOrder -> NumberOrderPreview(mission)
                is MissionConfig.Sequence -> SequencePreview(mission)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onPractice,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, WakeBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = WakeSurfaceHigh,
                contentColor = WakeTextPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = WakeAccent
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Try ${mission.displayName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun missionHintText(mission: MissionConfig): String = when (mission) {
    is MissionConfig.Snake -> "${mission.speed.label} · ${mission.targetLength} len"
    is MissionConfig.Math -> "${mission.difficulty.label} · ${mission.problemCount}x"
    is MissionConfig.Memory -> "${mission.difficulty.label} · ${mission.gridSize.label}"
    is MissionConfig.ColorMatch -> "${mission.difficulty.label} · ${mission.rounds}x"
    is MissionConfig.NumberOrder -> "${mission.difficulty.label} · ${mission.rounds}x"
    is MissionConfig.Sequence -> "${mission.difficulty.label} · ${mission.rounds}x"
}

@Composable
private fun SnakePreview(config: MissionConfig.Snake) {
    // Simple looping snake path on an 11x6 preview grid at the selected speed
    val cols = 11
    val rows = 6
    val path = remember {
        listOf(
            2 to 4, 2 to 3, 2 to 2, 2 to 1,
            3 to 1, 4 to 1, 5 to 1, 6 to 1, 7 to 1, 8 to 1,
            8 to 2, 8 to 3, 8 to 4,
            7 to 4, 6 to 4, 5 to 4, 4 to 4, 3 to 4
        )
    }
    var step by remember(config.speed) { mutableIntStateOf(0) }
    LaunchedEffect(config.speed) {
        while (true) {
            delay(config.speed.tickMs)
            step = (step + 1) % path.size
        }
    }

    val snakeLen = config.targetLength.coerceIn(4, 7)
    val snakeCells = List(snakeLen) { i -> path[(step - i + path.size) % path.size] }
    val appleCell = path[(step + 4) % path.size]

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cell = minOf(size.width / cols, size.height / rows)
        val ox = (size.width - cell * cols) / 2f
        val oy = (size.height - cell * rows) / 2f

        // Subtle grid
        val gridCol = Color(0x12EFE9DE)
        for (c in 0..cols) {
            val x = ox + c * cell
            drawLine(gridCol, Offset(x, oy), Offset(x, oy + cell * rows), 1f)
        }
        for (r in 0..rows) {
            val y = oy + r * cell
            drawLine(gridCol, Offset(ox, y), Offset(ox + cell * cols, y), 1f)
        }

        // Apple
        drawCircle(
            color = WakeAccent,
            radius = cell * 0.3f,
            center = Offset(ox + (appleCell.first + 0.5f) * cell, oy + (appleCell.second + 0.5f) * cell)
        )

        // Snake segments
        snakeCells.forEachIndexed { idx, (cx, cy) ->
            val pad = cell * 0.12f
            val alpha = 1f - (idx.toFloat() / (snakeLen + 2)) * 0.55f
            drawRoundRect(
                color = if (idx == 0) WakeEmerald else WakeEmerald.copy(alpha = alpha),
                topLeft = Offset(ox + cx * cell + pad, oy + cy * cell + pad),
                size = Size(cell - pad * 2, cell - pad * 2),
                cornerRadius = CornerRadius(cell * 0.25f, cell * 0.25f)
            )
        }
    }
}

@Composable
private fun MathPreview(config: MissionConfig.Math) {
    val sample = when (config.difficulty) {
        MathDifficulty.EASY -> "24 + 17"
        MathDifficulty.MEDIUM -> "8 × 7 + 14"
        MathDifficulty.HARD -> "13 × 7 − 28"
    }
    val answer = when (config.difficulty) {
        MathDifficulty.EASY -> "41"
        MathDifficulty.MEDIUM -> "70"
        MathDifficulty.HARD -> "63"
    }
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Solve ${config.problemCount} equations",
                fontSize = 12.sp,
                color = WakeTextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = sample,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = WakeTextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(WakeSurfaceHigh)
                    .border(1.dp, WakeBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "= $answer",
                    fontSize = 14.sp,
                    color = WakeAccent,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Mini 3x3 keypad hint
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    r.forEach { d ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(WakeSurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(d, fontSize = 11.sp, color = WakeTextSecondary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryPreview(config: MissionConfig.Memory) {
    val dim = config.gridSize.dimension
    val pattern = remember(dim, config.difficulty) {
        if (dim == 3) setOf(0, 4, 7) else setOf(1, 5, 6, 10, 14)
    }
    var showing by remember(config) { mutableIntStateOf(0) }
    LaunchedEffect(config) {
        while (true) {
            showing = 1
            delay(1_400L)
            showing = 0
            delay(900L)
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            modifier = Modifier
                .height(116.dp)
                .aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (r in 0 until dim) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (c in 0 until dim) {
                        val idx = r * dim + c
                        val lit = showing == 1 && idx in pattern
                        val color by animateColorAsState(
                            targetValue = if (lit) WakeAccent else WakeSurfaceHigh,
                            animationSpec = tween(180),
                            label = "memPreview"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = if (showing == 1) "Memorize tiles" else "Tap from memory",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = WakeTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${config.gridSize.label} grid · ${config.rounds} rounds",
                fontSize = 12.sp,
                color = WakeTextSecondary
            )
        }
    }
}

@Composable
private fun ColorTrapPreview(config: MissionConfig.ColorMatch) {
    val samples = remember {
        listOf(
            Triple(NamedColor.BLUE, NamedColor.RED, false),
            Triple(NamedColor.GREEN, NamedColor.YELLOW, true),
            Triple(NamedColor.RED, NamedColor.GREEN, false)
        )
    }
    var tick by remember(config.difficulty) { mutableIntStateOf(0) }
    LaunchedEffect(config.difficulty) {
        while (true) {
            delay(1_600L)
            tick = (tick + 1) % samples.size
        }
    }
    val (word, ink, wordMode) = samples[tick]
    val showWordMode = config.difficulty == GameDifficulty.HARD && wordMode

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (showWordMode) "Tap the word" else "Tap the ink color",
            fontSize = 12.sp,
            color = WakeTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = word.label,
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(ink.argb),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NamedColor.entries.take(4).forEach { opt ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(opt.argb))
                )
            }
        }
    }
}

@Composable
private fun NumberOrderPreview(config: MissionConfig.NumberOrder) {
    val (cols, rows, label) = when (config.difficulty) {
        GameDifficulty.EASY -> Triple(3, 3, "Tap 1 to 9 in order")
        GameDifficulty.MEDIUM -> Triple(4, 3, "Tap 1 to 12 in order")
        GameDifficulty.HARD -> Triple(4, 3, "1 to 16, alternates reverse")
    }
    val previewNums = remember(cols, rows) {
        listOf(5, 1, 8, 3, 9, 2, 7, 4, 6, 11, 10, 12).take(cols * rows)
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .height(112.dp)
                .aspectRatio(cols.toFloat() / rows.toFloat()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (c in 0 until cols) {
                        val n = previewNums[r * cols + c]
                        val isNext = n == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isNext) WakeAccent else WakeSurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = n.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = if (isNext) WakeBg else WakeTextPrimary
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = WakeTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${config.rounds} ${if (config.rounds == 1) "round" else "rounds"}",
                fontSize = 12.sp,
                color = WakeTextSecondary
            )
        }
    }
}

@Composable
private fun SequencePreview(config: MissionConfig.Sequence) {
    val padCount = if (config.difficulty == GameDifficulty.HARD) 6 else 4
    val padColors = listOf(
        WakeEmerald,
        Color(0xFF38BDF8),
        WakeAmber,
        WakeCoral,
        Color(0xFFC084FC),
        WakeAccent
    )
    var activePad by remember(config.difficulty) { mutableIntStateOf(0) }
    val stepMs = when (config.difficulty) {
        GameDifficulty.EASY -> 650L
        GameDifficulty.MEDIUM -> 480L
        GameDifficulty.HARD -> 340L
    }
    LaunchedEffect(config.difficulty) {
        val seq = listOf(0, 2, 1, 3, 4, 5).take(padCount)
        var i = 0
        while (true) {
            activePad = seq[i % seq.size]
            delay(stepMs)
            i++
        }
    }

    val cols = if (padCount == 6) 3 else 2
    val rows = 2

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .height(104.dp)
                .width(if (cols == 3) 144.dp else 104.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (c in 0 until cols) {
                        val idx = r * cols + c
                        val base = padColors[idx]
                        val lit = activePad == idx
                        val bg by animateColorAsState(
                            targetValue = if (lit) base else base.copy(alpha = 0.22f),
                            animationSpec = tween(140),
                            label = "seqPreview"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(bg)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                text = "Repeat the sequence",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = WakeTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$padCount pads · ${config.rounds} rounds",
                fontSize = 12.sp,
                color = WakeTextSecondary
            )
        }
    }
}
