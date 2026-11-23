package com.wakeguard.alarm.ui.games

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wakeguard.alarm.model.MissionConfig

/**
 * Modular mission router. Adding a new mini-game requires only adding a [MissionConfig] subtype
 * and mapping it here.
 */
@Composable
fun MissionHost(
    mission: MissionConfig,
    paused: Boolean,
    resetSignal: Int,
    onInteraction: () -> Unit,
    onCrash: (String) -> Unit,
    onMissionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (mission) {
        is MissionConfig.Snake -> SnakeGameView(
            config = mission,
            paused = paused,
            resetSignal = resetSignal,
            onInteraction = onInteraction,
            onCrash = onCrash,
            onMissionComplete = onMissionComplete,
            modifier = modifier
        )

        is MissionConfig.Math -> MathGameView(
            config = mission,
            paused = paused,
            resetSignal = resetSignal,
            onInteraction = onInteraction,
            onCrash = onCrash,
            onMissionComplete = onMissionComplete,
            modifier = modifier
        )

        is MissionConfig.Memory -> MemoryGameView(
            config = mission,
            paused = paused,
            resetSignal = resetSignal,
            onInteraction = onInteraction,
            onCrash = onCrash,
            onMissionComplete = onMissionComplete,
            modifier = modifier
        )
    }
}
