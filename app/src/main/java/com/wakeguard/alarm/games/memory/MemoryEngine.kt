package com.wakeguard.alarm.games.memory

import com.wakeguard.alarm.model.GameDifficulty
import com.wakeguard.alarm.model.MissionConfig
import kotlin.random.Random

enum class MemoryPhase {
    SHOWING_PATTERN,
    RECALLING,
    CRASHED,
    WON
}

data class MemoryGameState(
    val dimension: Int,
    val totalRounds: Int,
    val currentRound: Int, // 1-indexed
    val difficulty: GameDifficulty = GameDifficulty.EASY,
    val targetTiles: Set<Int>,
    val foundTiles: Set<Int> = emptySet(),
    val wrongTile: Int? = null,
    val phase: MemoryPhase = MemoryPhase.SHOWING_PATTERN
) {
    val totalCells: Int get() = dimension * dimension
    val progress: Float
        get() = ((currentRound - 1).toFloat() / totalRounds.coerceAtLeast(1)).coerceIn(0f, 1f)
    val previewDurationMs: Long
        get() = when (difficulty) {
            GameDifficulty.EASY -> 2_100L
            GameDifficulty.MEDIUM -> 1_600L
            GameDifficulty.HARD -> 1_100L
        }
}

class MemoryEngine(private val random: Random = Random.Default) {

    fun createInitialState(config: MissionConfig.Memory): MemoryGameState {
        val dim = config.gridSize.dimension
        val rounds = config.rounds.coerceIn(1, 8)
        return MemoryGameState(
            dimension = dim,
            totalRounds = rounds,
            currentRound = 1,
            difficulty = config.difficulty,
            targetTiles = generatePattern(dim, round = 1, difficulty = config.difficulty),
            phase = MemoryPhase.SHOWING_PATTERN
        )
    }

    fun finishShowingPattern(state: MemoryGameState): MemoryGameState {
        if (state.phase != MemoryPhase.SHOWING_PATTERN) return state
        return state.copy(phase = MemoryPhase.RECALLING)
    }

    fun tapTile(state: MemoryGameState, index: Int): MemoryGameState {
        if (state.phase != MemoryPhase.RECALLING) return state
        if (index !in 0 until state.totalCells) return state
        if (index in state.foundTiles) return state

        return if (index in state.targetTiles) {
            val updatedFound = state.foundTiles + index
            if (updatedFound.size == state.targetTiles.size) {
                if (state.currentRound >= state.totalRounds) {
                    state.copy(foundTiles = updatedFound, phase = MemoryPhase.WON)
                } else {
                    val nextRound = state.currentRound + 1
                    state.copy(
                        currentRound = nextRound,
                        targetTiles = generatePattern(state.dimension, nextRound, state.difficulty),
                        foundTiles = emptySet(),
                        wrongTile = null,
                        phase = MemoryPhase.SHOWING_PATTERN
                    )
                }
            } else {
                state.copy(foundTiles = updatedFound)
            }
        } else {
            state.copy(wrongTile = index, phase = MemoryPhase.CRASHED)
        }
    }

    fun retryCurrentRound(state: MemoryGameState): MemoryGameState {
        return state.copy(
            targetTiles = generatePattern(state.dimension, state.currentRound, state.difficulty),
            foundTiles = emptySet(),
            wrongTile = null,
            phase = MemoryPhase.SHOWING_PATTERN
        )
    }

    private fun generatePattern(dimension: Int, round: Int, difficulty: GameDifficulty): Set<Int> {
        val total = dimension * dimension
        val baseCount = when (difficulty) {
            GameDifficulty.EASY -> if (dimension == 3) 3 else 4
            GameDifficulty.MEDIUM -> if (dimension == 3) 4 else 5
            GameDifficulty.HARD -> if (dimension == 3) 4 else 6
        }
        val count = (baseCount + (round - 1)).coerceAtMost(total - 2)
        return (0 until total).shuffled(random).take(count).toSet()
    }
}
