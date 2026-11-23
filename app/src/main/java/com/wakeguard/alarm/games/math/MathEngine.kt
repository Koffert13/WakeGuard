package com.wakeguard.alarm.games.math

import com.wakeguard.alarm.model.MathDifficulty
import com.wakeguard.alarm.model.MissionConfig
import kotlin.random.Random

data class MathProblem(
    val expression: String,
    val answer: Int
)

enum class MathStatus {
    IN_PROGRESS,
    WRONG_STREAK_CRASH,
    WON
}

data class MathGameState(
    val totalProblems: Int,
    val solvedCount: Int,
    val difficulty: MathDifficulty,
    val currentProblem: MathProblem,
    val inputText: String = "",
    val wrongAttemptsOnCurrent: Int = 0,
    val lastAnswerWasWrong: Boolean = false,
    val status: MathStatus = MathStatus.IN_PROGRESS
) {
    val progress: Float
        get() = (solvedCount.toFloat() / totalProblems.coerceAtLeast(1)).coerceIn(0f, 1f)
}

class MathEngine(private val random: Random = Random.Default) {

    fun createInitialState(config: MissionConfig.Math): MathGameState {
        return MathGameState(
            totalProblems = config.problemCount.coerceIn(1, 15),
            solvedCount = 0,
            difficulty = config.difficulty,
            currentProblem = generateProblem(config.difficulty)
        )
    }

    fun generateProblem(difficulty: MathDifficulty): MathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                val a = random.nextInt(12, 65)
                val b = random.nextInt(8, 45)
                if (random.nextBoolean()) {
                    MathProblem("$a + $b", a + b)
                } else {
                    val hi = maxOf(a, b)
                    val lo = minOf(a, b)
                    MathProblem("$hi - $lo", hi - lo)
                }
            }

            MathDifficulty.MEDIUM -> {
                val a = random.nextInt(4, 13)
                val b = random.nextInt(4, 13)
                val c = random.nextInt(7, 36)
                if (random.nextBoolean()) {
                    MathProblem("$a × $b + $c", a * b + c)
                } else {
                    val prod = a * b
                    val sub = c.coerceAtMost(prod - 1)
                    MathProblem("$a × $b - $sub", prod - sub)
                }
            }

            MathDifficulty.HARD -> {
                val a = random.nextInt(11, 22)
                val b = random.nextInt(6, 14)
                val c = random.nextInt(15, 65)
                if (random.nextBoolean()) {
                    MathProblem("$a × $b + $c", a * b + c)
                } else {
                    val prod = a * b
                    val sub = c.coerceAtMost(prod - 1)
                    MathProblem("$a × $b - $sub", prod - sub)
                }
            }
        }
    }

    fun appendDigit(state: MathGameState, digit: Char): MathGameState {
        if (state.status != MathStatus.IN_PROGRESS) return state
        if (!digit.isDigit() || state.inputText.length >= 6) return state
        return state.copy(inputText = state.inputText + digit, lastAnswerWasWrong = false)
    }

    fun backspace(state: MathGameState): MathGameState {
        if (state.status != MathStatus.IN_PROGRESS) return state
        return state.copy(inputText = state.inputText.dropLast(1), lastAnswerWasWrong = false)
    }

    fun clearInput(state: MathGameState): MathGameState {
        if (state.status != MathStatus.IN_PROGRESS) return state
        return state.copy(inputText = "", lastAnswerWasWrong = false)
    }

    fun submit(state: MathGameState): MathGameState {
        if (state.status != MathStatus.IN_PROGRESS) return state
        val parsed = state.inputText.toIntOrNull() ?: return state

        return if (parsed == state.currentProblem.answer) {
            val nextSolved = state.solvedCount + 1
            if (nextSolved >= state.totalProblems) {
                state.copy(
                    solvedCount = nextSolved,
                    inputText = "",
                    wrongAttemptsOnCurrent = 0,
                    lastAnswerWasWrong = false,
                    status = MathStatus.WON
                )
            } else {
                state.copy(
                    solvedCount = nextSolved,
                    currentProblem = generateProblem(state.difficulty),
                    inputText = "",
                    wrongAttemptsOnCurrent = 0,
                    lastAnswerWasWrong = false
                )
            }
        } else {
            val nextWrong = state.wrongAttemptsOnCurrent + 1
            if (nextWrong >= MAX_WRONG_BEFORE_ALARM_RESUME) {
                state.copy(
                    inputText = "",
                    wrongAttemptsOnCurrent = nextWrong,
                    lastAnswerWasWrong = true,
                    status = MathStatus.WRONG_STREAK_CRASH
                )
            } else {
                state.copy(
                    inputText = "",
                    wrongAttemptsOnCurrent = nextWrong,
                    lastAnswerWasWrong = true
                )
            }
        }
    }

    fun recoverAfterCrash(state: MathGameState): MathGameState {
        return state.copy(
            currentProblem = generateProblem(state.difficulty),
            inputText = "",
            wrongAttemptsOnCurrent = 0,
            lastAnswerWasWrong = false,
            status = MathStatus.IN_PROGRESS
        )
    }

    companion object {
        const val MAX_WRONG_BEFORE_ALARM_RESUME = 3
    }
}
