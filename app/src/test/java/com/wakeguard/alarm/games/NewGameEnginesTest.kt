package com.wakeguard.alarm.games

import com.wakeguard.alarm.games.colormatch.ColorMatchEngine
import com.wakeguard.alarm.games.colormatch.ColorMatchStatus
import com.wakeguard.alarm.games.numberorder.NumberOrderEngine
import com.wakeguard.alarm.games.numberorder.NumberOrderStatus
import com.wakeguard.alarm.games.sequence.SequenceEngine
import com.wakeguard.alarm.games.sequence.SequencePhase
import com.wakeguard.alarm.model.GameDifficulty
import com.wakeguard.alarm.model.MissionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NewGameEnginesTest {

    @Test
    fun `color match wins after solving target rounds and crashes after max mistakes`() {
        val engine = ColorMatchEngine(Random(42))
        var state = engine.createInitialState(
            MissionConfig.ColorMatch(rounds = 3, difficulty = GameDifficulty.MEDIUM)
        )

        // Wrong answer increments mistake count
        val wrongOption = state.challenge.options.first { it != state.challenge.expectedAnswer }
        state = engine.submit(state, wrongOption)
        assertEquals(1, state.mistakes)
        assertEquals(ColorMatchStatus.IN_PROGRESS, state.status)

        // Second wrong answer crashes (maxMistakes = 2 on Medium)
        val wrongOption2 = state.challenge.options.first { it != state.challenge.expectedAnswer }
        state = engine.submit(state, wrongOption2)
        assertEquals(ColorMatchStatus.CRASHED, state.status)

        // Recover and solve 3 rounds
        state = engine.recoverAfterCrash(state)
        repeat(3) {
            state = engine.submit(state, state.challenge.expectedAnswer)
        }
        assertEquals(ColorMatchStatus.WON, state.status)
    }

    @Test
    fun `number order clears ascending and supports descending on hard even rounds`() {
        val engine = NumberOrderEngine(Random(7))
        var state = engine.createInitialState(
            MissionConfig.NumberOrder(rounds = 2, difficulty = GameDifficulty.HARD)
        )
        assertEquals(16, state.totalTiles)
        assertEquals(1, state.nextExpected)

        // Clear round 1 ascending (1..16)
        for (n in 1..16) {
            state = engine.tapNumber(state, n)
        }
        assertEquals(2, state.currentRound)
        assertTrue(state.descending)
        assertEquals(16, state.nextExpected)

        // Clear round 2 descending (16..1)
        for (n in 16 downTo 1) {
            state = engine.tapNumber(state, n)
        }
        assertEquals(NumberOrderStatus.WON, state.status)
    }

    @Test
    fun `sequence engine advances rounds and crashes on wrong pad`() {
        val engine = SequenceEngine(Random(99))
        var state = engine.createInitialState(
            MissionConfig.Sequence(rounds = 2, difficulty = GameDifficulty.EASY)
        )
        assertEquals(4, state.padCount)
        assertEquals(3, state.sequence.size)

        state = engine.finishDemo(state)
        // Tap wrong pad -> crash
        val wrongPad = (state.sequence.first() + 1) % state.padCount
        state = engine.tapPad(state, wrongPad)
        assertEquals(SequencePhase.CRASHED, state.phase)

        // Retry and complete round 1
        state = engine.retryCurrentRound(state)
        state = engine.finishDemo(state)
        for (pad in state.sequence) {
            state = engine.tapPad(state, pad)
        }
        assertEquals(2, state.currentRound)
        assertEquals(4, state.sequence.size)

        // Complete round 2 -> won
        state = engine.finishDemo(state)
        for (pad in state.sequence) {
            state = engine.tapPad(state, pad)
        }
        assertEquals(SequencePhase.WON, state.phase)
    }
}
