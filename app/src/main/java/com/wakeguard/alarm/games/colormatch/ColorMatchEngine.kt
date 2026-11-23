package com.wakeguard.alarm.games.colormatch

import com.wakeguard.alarm.model.GameDifficulty
import com.wakeguard.alarm.model.MissionConfig
import kotlin.random.Random

enum class NamedColor(val label: String, val argb: Long) {
    RED("RED", 0xFFFB7185),
    BLUE("BLUE", 0xFF38BDF8),
    GREEN("GREEN", 0xFF34D399),
    YELLOW("YELLOW", 0xFFFBBF24),
    PURPLE("PURPLE", 0xFFC084FC),
    ORANGE("ORANGE", 0xFFFB923C)
}

enum class ColorPromptMode {
    MATCH_INK,
    MATCH_WORD
}

enum class ColorMatchStatus {
    IN_PROGRESS,
    CRASHED,
    WON
}

data class ColorChallenge(
    val word: NamedColor,
    val inkColor: NamedColor,
    val mode: ColorPromptMode,
    val options: List<NamedColor>
) {
    val expectedAnswer: NamedColor
        get() = when (mode) {
            ColorPromptMode.MATCH_INK -> inkColor
            ColorPromptMode.MATCH_WORD -> word
        }
}

data class ColorMatchGameState(
    val totalRounds: Int,
    val solvedCount: Int,
    val difficulty: GameDifficulty,
    val challenge: ColorChallenge,
    val mistakes: Int = 0,
    val maxMistakes: Int = 2,
    val status: ColorMatchStatus = ColorMatchStatus.IN_PROGRESS,
    val crashReason: String? = null
) {
    val progress: Float
        get() = (solvedCount.toFloat() / totalRounds.coerceAtLeast(1)).coerceIn(0f, 1f)

    val roundTimeLimitMs: Long
        get() = when (difficulty) {
            GameDifficulty.EASY -> 9_000L
            GameDifficulty.MEDIUM -> 5_500L
            GameDifficulty.HARD -> 3_800L
        }
}

class ColorMatchEngine(private val random: Random = Random.Default) {

    fun createInitialState(config: MissionConfig.ColorMatch): ColorMatchGameState {
        val rounds = config.rounds.coerceIn(3, 20)
        val maxMistakes = when (config.difficulty) {
            GameDifficulty.EASY -> 3
            GameDifficulty.MEDIUM -> 2
            GameDifficulty.HARD -> 2
        }
        return ColorMatchGameState(
            totalRounds = rounds,
            solvedCount = 0,
            difficulty = config.difficulty,
            challenge = generateChallenge(config.difficulty),
            mistakes = 0,
            maxMistakes = maxMistakes,
            status = ColorMatchStatus.IN_PROGRESS
        )
    }

    fun generateChallenge(difficulty: GameDifficulty): ColorChallenge {
        val poolSize = when (difficulty) {
            GameDifficulty.EASY -> 4
            GameDifficulty.MEDIUM -> 5
            GameDifficulty.HARD -> 6
        }
        val pool = NamedColor.entries.take(poolSize)
        val word = pool[random.nextInt(pool.size)]
        val differentColors = pool.filter { it != word }
        val inkColor = differentColors[random.nextInt(differentColors.size)]
        val mode = when (difficulty) {
            GameDifficulty.EASY, GameDifficulty.MEDIUM -> ColorPromptMode.MATCH_INK
            GameDifficulty.HARD -> if (random.nextBoolean()) ColorPromptMode.MATCH_INK else ColorPromptMode.MATCH_WORD
        }
        val options = pool.shuffled(random)
        return ColorChallenge(
            word = word,
            inkColor = inkColor,
            mode = mode,
            options = options
        )
    }

    fun submit(state: ColorMatchGameState, selected: NamedColor): ColorMatchGameState {
        if (state.status != ColorMatchStatus.IN_PROGRESS) return state
        return if (selected == state.challenge.expectedAnswer) {
            val nextSolved = state.solvedCount + 1
            if (nextSolved >= state.totalRounds) {
                state.copy(
                    solvedCount = nextSolved,
                    status = ColorMatchStatus.WON
                )
            } else {
                state.copy(
                    solvedCount = nextSolved,
                    challenge = generateChallenge(state.difficulty)
                )
            }
        } else {
            recordMistake(
                state,
                "Wrong color! Expected ${state.challenge.expectedAnswer.label}."
            )
        }
    }

    fun onTimeout(state: ColorMatchGameState): ColorMatchGameState {
        if (state.status != ColorMatchStatus.IN_PROGRESS) return state
        return recordMistake(state, "Too slow! Tap before the timer runs out.")
    }

    fun recoverAfterCrash(state: ColorMatchGameState): ColorMatchGameState {
        return state.copy(
            challenge = generateChallenge(state.difficulty),
            mistakes = 0,
            status = ColorMatchStatus.IN_PROGRESS,
            crashReason = null
        )
    }

    private fun recordMistake(state: ColorMatchGameState, reason: String): ColorMatchGameState {
        val nextMistakes = state.mistakes + 1
        return if (nextMistakes >= state.maxMistakes) {
            state.copy(
                mistakes = nextMistakes,
                status = ColorMatchStatus.CRASHED,
                crashReason = reason
            )
        } else {
            state.copy(
                mistakes = nextMistakes,
                challenge = generateChallenge(state.difficulty),
                crashReason = reason
            )
        }
    }
}
