package com.wakeguard.alarm.games.snake

import com.wakeguard.alarm.model.MissionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SnakeEngineTest {

    @Test
    fun `initial state starts at length 3 and apple never spawns on snake`() {
        val engine = SnakeEngine(Random(42))
        val state = engine.createInitialState(MissionConfig.Snake(targetLength = 10))
        assertEquals(3, state.currentLength)
        assertEquals(SnakeStatus.RUNNING, state.status)
        assertFalse(state.apple in state.snake)
    }

    @Test
    fun `prevents 180 degree self reversal and queues two rapid turns`() {
        val engine = SnakeEngine(Random(1))
        var state = engine.createInitialState(MissionConfig.Snake(targetLength = 10))
        assertEquals(SnakeDirection.UP, state.direction)

        // Opposite direction DOWN should be ignored while moving UP
        state = engine.queueDirection(state, SnakeDirection.DOWN)
        assertTrue(state.directionQueue.isEmpty())

        // Rapid LEFT then DOWN is valid and executes over two ticks
        state = engine.queueDirection(state, SnakeDirection.LEFT)
        state = engine.queueDirection(state, SnakeDirection.DOWN)
        assertEquals(listOf(SnakeDirection.LEFT, SnakeDirection.DOWN), state.directionQueue)

        state = engine.step(state)
        assertEquals(SnakeDirection.LEFT, state.direction)
        assertEquals(listOf(SnakeDirection.DOWN), state.directionQueue)

        state = engine.step(state)
        assertEquals(SnakeDirection.DOWN, state.direction)
        assertTrue(state.directionQueue.isEmpty())
    }

    @Test
    fun `eating apple grows snake and wins upon reaching target length`() {
        val engine = SnakeEngine(Random(7))
        val state = SnakeGameState(
            cols = 10,
            rows = 10,
            targetLength = 4,
            wrapWalls = false,
            snake = listOf(GridPoint(5, 5), GridPoint(5, 6), GridPoint(5, 7)),
            direction = SnakeDirection.UP,
            apple = GridPoint(5, 4)
        )

        val next = engine.step(state)
        assertEquals(4, next.currentLength)
        assertEquals(1, next.applesEaten)
        assertEquals(SnakeStatus.WON, next.status)
    }

    @Test
    fun `solid wall collision triggers crash`() {
        val engine = SnakeEngine(Random(1))
        val state = SnakeGameState(
            cols = 8,
            rows = 8,
            targetLength = 10,
            wrapWalls = false,
            snake = listOf(GridPoint(3, 0), GridPoint(3, 1), GridPoint(3, 2)),
            direction = SnakeDirection.UP,
            apple = GridPoint(6, 6)
        )

        val next = engine.step(state)
        assertEquals(SnakeStatus.CRASHED, next.status)
        assertTrue(next.crashReason.orEmpty().contains("wall", ignoreCase = true))
    }

    @Test
    fun `wrap walls teleports snake to opposite edge`() {
        val engine = SnakeEngine(Random(1))
        val state = SnakeGameState(
            cols = 8,
            rows = 8,
            targetLength = 10,
            wrapWalls = true,
            snake = listOf(GridPoint(3, 0), GridPoint(3, 1), GridPoint(3, 2)),
            direction = SnakeDirection.UP,
            apple = GridPoint(6, 6)
        )

        val next = engine.step(state)
        assertEquals(SnakeStatus.RUNNING, next.status)
        assertEquals(GridPoint(3, 7), next.snake.first())
    }

    @Test
    fun `biting own body crashes while stepping into vacating tail is legal`() {
        val engine = SnakeEngine(Random(1))
        // 4-segment square loop: moving LEFT into (4,5) which is the current tail (vacating) -> legal!
        val tailChase = SnakeGameState(
            cols = 10,
            rows = 10,
            targetLength = 10,
            wrapWalls = false,
            snake = listOf(GridPoint(5, 5), GridPoint(5, 4), GridPoint(4, 4), GridPoint(4, 5)),
            direction = SnakeDirection.LEFT,
            apple = GridPoint(0, 0)
        )
        assertEquals(SnakeStatus.RUNNING, engine.step(tailChase).status)

        // 5-segment body where (4,5) is NOT the tail -> self-collision crash!
        val selfHit = tailChase.copy(
            snake = listOf(
                GridPoint(5, 5),
                GridPoint(5, 4),
                GridPoint(4, 4),
                GridPoint(4, 5),
                GridPoint(4, 6)
            )
        )
        assertEquals(SnakeStatus.CRASHED, engine.step(selfHit).status)
    }
}
