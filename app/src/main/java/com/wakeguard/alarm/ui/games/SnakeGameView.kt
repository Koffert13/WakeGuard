package com.wakeguard.alarm.ui.games

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeguard.alarm.games.snake.SnakeDirection
import com.wakeguard.alarm.games.snake.SnakeEngine
import com.wakeguard.alarm.games.snake.SnakeStatus
import com.wakeguard.alarm.model.MissionConfig
import com.wakeguard.alarm.ui.theme.WakeBorder
import com.wakeguard.alarm.ui.theme.WakeCoral
import com.wakeguard.alarm.ui.theme.WakeEmerald
import com.wakeguard.alarm.ui.theme.WakeEmeraldBright
import com.wakeguard.alarm.ui.theme.WakeSurface
import com.wakeguard.alarm.ui.theme.WakeSurfaceHigh
import com.wakeguard.alarm.ui.theme.WakeTextPrimary
import com.wakeguard.alarm.ui.theme.WakeTextSecondary
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun SnakeGameView(
    config: MissionConfig.Snake,
    paused: Boolean,
    resetSignal: Int,
    onInteraction: () -> Unit,
    onCrash: (String) -> Unit,
    onMissionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { SnakeEngine() }
    val haptics = LocalHapticFeedback.current
    var gameState by remember(config, resetSignal) {
        mutableStateOf(engine.createInitialState(config))
    }

    val pulseAnim = rememberInfiniteTransition(label = "applePulse")
    val appleScale by pulseAnim.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "appleScale"
    )

    // Tick loop
    LaunchedEffect(config, resetSignal, paused) {
        if (paused) return@LaunchedEffect
        // Brief 350ms beat on fresh start/retry so the player sees the board before first move
        delay(350L)
        while (!paused && gameState.status == SnakeStatus.RUNNING) {
            delay(config.speed.tickMs)
            val prevApples = gameState.applesEaten
            val next = engine.step(gameState)
            gameState = next
            if (next.applesEaten > prevApples) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            when (next.status) {
                SnakeStatus.CRASHED -> {
                    onCrash(next.crashReason ?: "Crashed! Try again.")
                    break
                }
                SnakeStatus.WON -> {
                    onMissionComplete()
                    break
                }
                SnakeStatus.RUNNING -> Unit
            }
        }
    }

    val submitDirection: (SnakeDirection) -> Unit = { dir ->
        if (!paused && gameState.status == SnakeStatus.RUNNING) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            gameState = engine.queueDirection(gameState, dir)
            onInteraction()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Length ${gameState.currentLength} / ${gameState.targetLength}",
                style = MaterialTheme.typography.titleMedium,
                color = WakeTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (config.wrapWalls) "${config.speed.label} • Wrap Walls" else "${config.speed.label} • Solid Walls",
                style = MaterialTheme.typography.bodyMedium,
                color = WakeTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { gameState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50)),
            color = WakeEmerald,
            trackColor = WakeSurfaceHigh,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Game Board (Swipe anywhere on board!)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val boardAspect = gameState.cols.toFloat() / gameState.rows.toFloat()
            var dragAccumX = 0f
            var dragAccumY = 0f

            Box(
                modifier = Modifier
                    .aspectRatio(boardAspect)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1815))
                    .border(
                        width = 1.dp,
                        color = WakeBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pointerInput(paused, resetSignal) {
                        detectDragGestures(
                            onDragStart = {
                                dragAccumX = 0f
                                dragAccumY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumX += dragAmount.x
                                dragAccumY += dragAmount.y
                                val threshold = 42f
                                if (abs(dragAccumX) > threshold || abs(dragAccumY) > threshold) {
                                    val dir = if (abs(dragAccumX) > abs(dragAccumY)) {
                                        if (dragAccumX > 0) SnakeDirection.RIGHT else SnakeDirection.LEFT
                                    } else {
                                        if (dragAccumY > 0) SnakeDirection.DOWN else SnakeDirection.UP
                                    }
                                    submitDirection(dir)
                                    // Reset accumulator so continuous finger slides can chain turns!
                                    dragAccumX = 0f
                                    dragAccumY = 0f
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    val cellW = size.width / gameState.cols
                    val cellH = size.height / gameState.rows
                    val cellSize = minOf(cellW, cellH)
                    val offsetX = (size.width - cellSize * gameState.cols) / 2f
                    val offsetY = (size.height - cellSize * gameState.rows) / 2f

                    // Subtle grid lines
                    val gridColor = Color(0x12EFE9DE)
                    for (c in 1 until gameState.cols) {
                        val x = offsetX + c * cellSize
                        drawLine(gridColor, Offset(x, offsetY), Offset(x, offsetY + cellSize * gameState.rows), 1f)
                    }
                    for (r in 1 until gameState.rows) {
                        val y = offsetY + r * cellSize
                        drawLine(gridColor, Offset(offsetX, y), Offset(offsetX + cellSize * gameState.cols, y), 1f)
                    }

                    // Draw pulsing apple
                    val appleCenter = Offset(
                        offsetX + (gameState.apple.x + 0.5f) * cellSize,
                        offsetY + (gameState.apple.y + 0.5f) * cellSize
                    )
                    val appleRadius = (cellSize * 0.37f) * appleScale
                    drawCircle(
                        color = WakeCoral.copy(alpha = 0.25f),
                        radius = appleRadius * 1.45f,
                        center = appleCenter
                    )
                    drawCircle(
                        color = WakeCoral,
                        radius = appleRadius,
                        center = appleCenter
                    )
                    // Apple shine highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = appleRadius * 0.25f,
                        center = Offset(appleCenter.x - appleRadius * 0.3f, appleCenter.y - appleRadius * 0.3f)
                    )

                    // Draw snake body (tail to head)
                    val snakeCount = gameState.snake.size
                    for (i in snakeCount - 1 downTo 0) {
                        val seg = gameState.snake[i]
                        val isHead = i == 0
                        val t = if (snakeCount <= 1) 0f else i.toFloat() / (snakeCount - 1)
                        val pad = if (isHead) cellSize * 0.06f else cellSize * (0.10f + 0.08f * t)
                        val left = offsetX + seg.x * cellSize + pad
                        val top = offsetY + seg.y * cellSize + pad
                        val dim = (cellSize - pad * 2f).coerceAtLeast(2f)

                        val segColor = when {
                            isHead -> WakeEmeraldBright
                            else -> Color(
                                red = (0x34 + (0x12 - 0x34) * t) / 255f,
                                green = (0xD3 + (0x7A - 0xD3) * t) / 255f,
                                blue = (0x99 + (0x5B - 0x99) * t) / 255f,
                                alpha = 1f
                            )
                        }

                        drawRoundRect(
                            color = segColor,
                            topLeft = Offset(left, top),
                            size = Size(dim, dim),
                            cornerRadius = CornerRadius(dim * 0.38f, dim * 0.38f)
                        )

                        // Directional eyes on head
                        if (isHead) {
                            val eyeRadius = (cellSize * 0.09f).coerceAtLeast(2f)
                            val cx = offsetX + (seg.x + 0.5f) * cellSize
                            val cy = offsetY + (seg.y + 0.5f) * cellSize
                            val forward = cellSize * 0.16f
                            val side = cellSize * 0.17f
                            val (e1, e2) = when (gameState.direction) {
                                SnakeDirection.UP -> Offset(cx - side, cy - forward) to Offset(cx + side, cy - forward)
                                SnakeDirection.DOWN -> Offset(cx - side, cy + forward) to Offset(cx + side, cy + forward)
                                SnakeDirection.LEFT -> Offset(cx - forward, cy - side) to Offset(cx - forward, cy + side)
                                SnakeDirection.RIGHT -> Offset(cx + forward, cy - side) to Offset(cx + forward, cy + side)
                            }
                            drawCircle(Color(0xFF062118), eyeRadius, e1)
                            drawCircle(Color(0xFF062118), eyeRadius, e2)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Ergonomic D-Pad + Swipe hint
        Text(
            text = "Swipe board or use D-pad",
            fontSize = 12.sp,
            color = WakeTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        SnakeDPad(onDirection = submitDirection)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SnakeDPad(onDirection: (SnakeDirection) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DPadButton(Icons.Rounded.KeyboardArrowUp, "Up") { onDirection(SnakeDirection.UP) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPadButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Left") { onDirection(SnakeDirection.LEFT) }
            DPadButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Right") { onDirection(SnakeDirection.RIGHT) }
        }
        DPadButton(Icons.Rounded.KeyboardArrowDown, "Down") { onDirection(SnakeDirection.DOWN) }
    }
}

@Composable
private fun DPadButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(62.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = WakeSurfaceHigh,
            contentColor = WakeTextPrimary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(34.dp)
        )
    }
}
