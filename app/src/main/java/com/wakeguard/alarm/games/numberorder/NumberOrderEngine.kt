package com.wakeguard.alarm.games.numberorder

import com.wakeguard.alarm.model.GameDifficulty
import com.wakeguard.alarm.model.MissionConfig
import kotlin.random.Random

enum class NumberOrderStatus {
    IN_PROGRESS,
    CRASHED,
    WON
}

data class NumberOrderGameState(
    val totalRounds: Int,
    val currentRound: Int, // 1-indexed
    val difficulty: GameDifficulty,
    val cols: Int,
    val rows: Int,
    val descending: Boolean,
    val tiles: List<Int>,
    val nextExpected: Int,
    val clearedNumbers: Set<Int> = emptySet(),
    val wrongNumber: Int? = null,
    val mistakes: Int = 0,
    val maxMistakes: Int = 2,
    val status: NumberOrderStatus = NumberOrderStatus.IN_PROGRESS,
    val crashReason: String? = null
) {
    val totalTiles: Int get() = cols * rows
    val progress: Float
        get() {
            val roundFraction = clearedNumbers.size.toFloat() / totalTiles.coerceAtLeast(1)
            return (((currentRound - 1) + roundFraction) / totalRounds.coerceAtLeast(1)).coerceIn(0f, 1f)
        }
}

class NumberOrderEngine(private val random: Random = Random.Default) {

    fun createInitialState(config: MissionConfig.NumberOrder): NumberOrderGameState {
        val rounds = config.rounds.coerceIn(1, 6)
        return buildRoundState(
            totalRounds = rounds,
            currentRound = 1,
            difficulty = config.difficulty
        )
    }

    fun tapNumber(state: NumberOrderGameState, value: Int): NumberOrderGameState {
        if (state.status != NumberOrderStatus.IN_PROGRESS) return state
        if (value in state.clearedNumbers) return state

        return if (value == state.nextExpected) {
            val updatedCleared = state.clearedNumbers + value
            if (updatedCleared.size >= state.totalTiles) {
                if (state.currentRound >= state.totalRounds) {
                    state.copy(
                        clearedNumbers = updatedCleared,
                        wrongNumber = null,
                        status = NumberOrderStatus.WON
                    )
                } else {
                    buildRoundState(
                        totalRounds = state.totalRounds,
                        currentRound = state.currentRound + 1,
                        difficulty = state.difficulty
                    )
                }
            } else {
                val next = if (state.descending) state.nextExpected - 1 else state.nextExpected + 1
                state.copy(
                    clearedNumbers = updatedCleared,
                    nextExpected = next,
                    wrongNumber = null
                )
            }
        } else {
            val nextMistakes = state.mistakes + 1
            if (nextMistakes >= state.maxMistakes) {
                state.copy(
                    wrongNumber = value,
                    mistakes = nextMistakes,
                    status = NumberOrderStatus.CRASHED,
                    crashReason = "Tapped $value instead of ${state.nextExpected}!"
                )
            } else {
                state.copy(
                    wrongNumber = value,
                    mistakes = nextMistakes
                )
            }
        }
    }

    fun retryCurrentRound(state: NumberOrderGameState): NumberOrderGameState {
        return buildRoundState(
            totalRounds = state.totalRounds,
            currentRound = state.currentRound,
            difficulty = state.difficulty
        )
    }

    private fun buildRoundState(
        totalRounds: Int,
        currentRound: Int,
        difficulty: GameDifficulty
    ): NumberOrderGameState {
        val (cols, rows) = when (difficulty) {
            GameDifficulty.EASY -> 3 to 3    // 1..9
            GameDifficulty.MEDIUM -> 3 to 4  // 1..12
            GameDifficulty.HARD -> 4 to 4    // 1..16
        }
        val count = cols * rows
        val descending = difficulty == GameDifficulty.HARD && currentRound % 2 == 0
        val maxMistakes = if (difficulty == GameDifficulty.EASY) 3 else 2
        return NumberOrderGameState(
            totalRounds = totalRounds,
            currentRound = currentRound,
            difficulty = difficulty,
            cols = cols,
            rows = rows,
            descending = descending,
            tiles = (1..count).shuffled(random),
            nextExpected = if (descending) count else 1,
            clearedNumbers = emptySet(),
            wrongNumber = null,
            mistakes = 0,
            maxMistakes = maxMistakes,
            status = NumberOrderStatus.IN_PROGRESS
        )
    }
}
