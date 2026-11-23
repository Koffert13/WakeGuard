package com.wakeguard.alarm.games.sequence

import com.wakeguard.alarm.model.GameDifficulty
import com.wakeguard.alarm.model.MissionConfig
import kotlin.random.Random

enum class SequencePhase {
    PLAYING_DEMO,
    WAITING_INPUT,
    CRASHED,
    WON
}

data class SequenceGameState(
    val totalRounds: Int,
    val currentRound: Int, // 1-indexed
    val difficulty: GameDifficulty,
    val padCount: Int,
    val sequence: List<Int>,
    val inputIndex: Int = 0,
    val wrongPad: Int? = null,
    val phase: SequencePhase = SequencePhase.PLAYING_DEMO
) {
    val progress: Float
        get() = ((currentRound - 1).toFloat() / totalRounds.coerceAtLeast(1)).coerceIn(0f, 1f)

    val stepHighlightMs: Long
        get() = when (difficulty) {
            GameDifficulty.EASY -> 600L
            GameDifficulty.MEDIUM -> 450L
            GameDifficulty.HARD -> 340L
        }

    val stepGapMs: Long
        get() = when (difficulty) {
            GameDifficulty.EASY -> 240L
            GameDifficulty.MEDIUM -> 180L
            GameDifficulty.HARD -> 140L
        }
}

class SequenceEngine(private val random: Random = Random.Default) {

    fun createInitialState(config: MissionConfig.Sequence): SequenceGameState {
        val rounds = config.rounds.coerceIn(2, 8)
        val padCount = if (config.difficulty == GameDifficulty.HARD) 6 else 4
        val startLen = startingLength(config.difficulty)
        return SequenceGameState(
            totalRounds = rounds,
            currentRound = 1,
            difficulty = config.difficulty,
            padCount = padCount,
            sequence = generateSequence(padCount, startLen),
            inputIndex = 0,
            phase = SequencePhase.PLAYING_DEMO
        )
    }

    fun finishDemo(state: SequenceGameState): SequenceGameState {
        if (state.phase != SequencePhase.PLAYING_DEMO) return state
        return state.copy(phase = SequencePhase.WAITING_INPUT, inputIndex = 0, wrongPad = null)
    }

    fun tapPad(state: SequenceGameState, padIndex: Int): SequenceGameState {
        if (state.phase != SequencePhase.WAITING_INPUT) return state
        if (padIndex !in 0 until state.padCount) return state

        val expected = state.sequence.getOrNull(state.inputIndex) ?: return state
        return if (padIndex == expected) {
            val nextIdx = state.inputIndex + 1
            if (nextIdx >= state.sequence.size) {
                if (state.currentRound >= state.totalRounds) {
                    state.copy(inputIndex = nextIdx, phase = SequencePhase.WON)
                } else {
                    val nextRound = state.currentRound + 1
                    val nextSequence = state.sequence + random.nextInt(state.padCount)
                    state.copy(
                        currentRound = nextRound,
                        sequence = nextSequence,
                        inputIndex = 0,
                        wrongPad = null,
                        phase = SequencePhase.PLAYING_DEMO
                    )
                }
            } else {
                state.copy(inputIndex = nextIdx)
            }
        } else {
            state.copy(
                wrongPad = padIndex,
                phase = SequencePhase.CRASHED
            )
        }
    }

    fun retryCurrentRound(state: SequenceGameState): SequenceGameState {
        return state.copy(
            inputIndex = 0,
            wrongPad = null,
            phase = SequencePhase.PLAYING_DEMO
        )
    }

    private fun startingLength(difficulty: GameDifficulty): Int = when (difficulty) {
        GameDifficulty.EASY -> 3
        GameDifficulty.MEDIUM -> 4
        GameDifficulty.HARD -> 4
    }

    private fun generateSequence(padCount: Int, length: Int): List<Int> {
        return List(length) { random.nextInt(padCount) }
    }
}
