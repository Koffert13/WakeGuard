package com.wakeguard.alarm.games

import com.wakeguard.alarm.games.math.MathEngine
import com.wakeguard.alarm.games.math.MathStatus
import com.wakeguard.alarm.games.memory.MemoryEngine
import com.wakeguard.alarm.games.memory.MemoryPhase
import com.wakeguard.alarm.model.MathDifficulty
import com.wakeguard.alarm.model.MemoryGridSize
import com.wakeguard.alarm.model.MissionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathAndMemoryEnginesTest {

    @Test
    fun `math engine solves problems and crashes after 3 wrong answers`() {
        val engine = MathEngine(Random(123))
        var state = engine.createInitialState(
            MissionConfig.Math(problemCount = 2, difficulty = MathDifficulty.MEDIUM)
        )

        // 3 wrong answers trigger WRONG_STREAK_CRASH
        repeat(3) {
            val wrong = (state.currentProblem.answer + 1).toString()
            wrong.forEach { ch -> state = engine.appendDigit(state, ch) }
            state = engine.submit(state)
        }
        assertEquals(MathStatus.WRONG_STREAK_CRASH, state.status)

        // Recover and solve both problems
        state = engine.recoverAfterCrash(state)
        repeat(2) {
            val ans = state.currentProblem.answer.toString()
            ans.forEach { ch -> state = engine.appendDigit(state, ch) }
            state = engine.submit(state)
        }
        assertEquals(MathStatus.WON, state.status)
        assertEquals(2, state.solvedCount)
    }

    @Test
    fun `memory engine detects wrong tile crash and completes all rounds`() {
        val engine = MemoryEngine(Random(99))
        var state = engine.createInitialState(
            MissionConfig.Memory(rounds = 2, gridSize = MemoryGridSize.GRID_3X3)
        )
        assertEquals(MemoryPhase.SHOWING_PATTERN, state.phase)
        state = engine.finishShowingPattern(state)
        assertEquals(MemoryPhase.RECALLING, state.phase)

        // Tap a wrong tile -> CRASHED
        val wrongIdx = (0 until state.totalCells).first { it !in state.targetTiles }
        val crashed = engine.tapTile(state, wrongIdx)
        assertEquals(MemoryPhase.CRASHED, crashed.phase)

        // Retry round 1 and solve round 1 & 2
        state = engine.retryCurrentRound(crashed)
        repeat(2) {
            state = engine.finishShowingPattern(state)
            for (tile in state.targetTiles) {
                state = engine.tapTile(state, tile)
            }
        }
        assertEquals(MemoryPhase.WON, state.phase)
        assertTrue(state.currentRound == 2)
    }
}
