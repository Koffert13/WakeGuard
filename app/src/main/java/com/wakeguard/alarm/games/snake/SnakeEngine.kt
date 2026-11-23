package com.wakeguard.alarm.games.snake

import com.wakeguard.alarm.model.MissionConfig
import kotlin.random.Random

data class GridPoint(val x: Int, val y: Int)

enum class SnakeDirection(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun isOpposite(other: SnakeDirection): Boolean =
        (this == UP && other == DOWN) ||
            (this == DOWN && other == UP) ||
            (this == LEFT && other == RIGHT) ||
            (this == RIGHT && other == LEFT)
}

enum class SnakeStatus {
    RUNNING,
    CRASHED,
    WON
}

data class SnakeGameState(
    val cols: Int,
    val rows: Int,
    val targetLength: Int,
    val wrapWalls: Boolean,
    val snake: List<GridPoint>, // head is index 0
    val direction: SnakeDirection,
    val directionQueue: List<SnakeDirection> = emptyList(),
    val apple: GridPoint,
    val status: SnakeStatus = SnakeStatus.RUNNING,
    val crashReason: String? = null,
    val applesEaten: Int = 0
) {
    val currentLength: Int get() = snake.size
    val progress: Float
        get() = ((currentLength - INITIAL_LENGTH).toFloat() / (targetLength - INITIAL_LENGTH).coerceAtLeast(1))
            .coerceIn(0f, 1f)

    companion object {
        const val INITIAL_LENGTH = 3
    }
}

class SnakeEngine(
    private val random: Random = Random.Default
) {
    fun createInitialState(
        config: MissionConfig.Snake,
        cols: Int = DEFAULT_COLS,
        rows: Int = DEFAULT_ROWS
    ): SnakeGameState {
        val centerX = cols / 2
        val startY = (rows * 2) / 3
        val initialSnake = listOf(
            GridPoint(centerX, startY),
            GridPoint(centerX, (startY + 1).coerceAtMost(rows - 1)),
            GridPoint(centerX, (startY + 2).coerceAtMost(rows - 1))
        )
        val target = config.targetLength.coerceAtLeast(SnakeGameState.INITIAL_LENGTH + 1)
        val apple = spawnApple(cols, rows, initialSnake)
        return SnakeGameState(
            cols = cols,
            rows = rows,
            targetLength = target,
            wrapWalls = config.wrapWalls,
            snake = initialSnake,
            direction = SnakeDirection.UP,
            directionQueue = emptyList(),
            apple = apple,
            status = SnakeStatus.RUNNING,
            crashReason = null,
            applesEaten = 0
        )
    }

    /**
     * Enqueues a turn input (up to 2 queued turns) while preventing 180-degree self-reversal
     * and redundant same-direction inputs.
     */
    fun queueDirection(state: SnakeGameState, requested: SnakeDirection): SnakeGameState {
        if (state.status != SnakeStatus.RUNNING) return state
        if (state.directionQueue.size >= 2) return state

        val referenceDir = state.directionQueue.lastOrNull() ?: state.direction
        if (requested == referenceDir || requested.isOpposite(referenceDir)) {
            return state
        }
        return state.copy(directionQueue = state.directionQueue + requested)
    }

    /**
     * Advances the simulation by one tick.
     */
    fun step(state: SnakeGameState): SnakeGameState {
        if (state.status != SnakeStatus.RUNNING) return state

        val nextDirection = state.directionQueue.firstOrNull() ?: state.direction
        val remainingQueue = if (state.directionQueue.isNotEmpty()) {
            state.directionQueue.drop(1)
        } else {
            emptyList()
        }

        val head = state.snake.first()
        var nextX = head.x + nextDirection.dx
        var nextY = head.y + nextDirection.dy

        if (state.wrapWalls) {
            nextX = (nextX + state.cols) % state.cols
            nextY = (nextY + state.rows) % state.rows
        } else if (nextX !in 0 until state.cols || nextY !in 0 until state.rows) {
            return state.copy(
                direction = nextDirection,
                directionQueue = emptyList(),
                status = SnakeStatus.CRASHED,
                crashReason = "Hit the wall at length ${state.currentLength} / ${state.targetLength}"
            )
        }

        val nextHead = GridPoint(nextX, nextY)
        val willEatApple = nextHead == state.apple

        // On a non-growth step, the current tail moves forward so moving into the old tail cell is legal.
        val bodyToCheck = if (willEatApple) state.snake else state.snake.dropLast(1)
        if (nextHead in bodyToCheck) {
            return state.copy(
                direction = nextDirection,
                directionQueue = emptyList(),
                status = SnakeStatus.CRASHED,
                crashReason = "Bit your own tail at length ${state.currentLength} / ${state.targetLength}"
            )
        }

        val newSnake = if (willEatApple) {
            listOf(nextHead) + state.snake
        } else {
            listOf(nextHead) + state.snake.dropLast(1)
        }

        val newApplesEaten = if (willEatApple) state.applesEaten + 1 else state.applesEaten
        if (newSnake.size >= state.targetLength) {
            return state.copy(
                snake = newSnake,
                direction = nextDirection,
                directionQueue = emptyList(),
                applesEaten = newApplesEaten,
                status = SnakeStatus.WON
            )
        }

        val nextApple = if (willEatApple) {
            spawnApple(state.cols, state.rows, newSnake)
        } else {
            state.apple
        }

        return state.copy(
            snake = newSnake,
            direction = nextDirection,
            directionQueue = remainingQueue,
            apple = nextApple,
            applesEaten = newApplesEaten,
            status = SnakeStatus.RUNNING
        )
    }

    private fun spawnApple(cols: Int, rows: Int, occupied: List<GridPoint>): GridPoint {
        val occupiedSet = occupied.toHashSet()
        val available = ArrayList<GridPoint>((cols * rows - occupiedSet.size).coerceAtLeast(1))
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val p = GridPoint(x, y)
                if (p !in occupiedSet) {
                    available.add(p)
                }
            }
        }
        if (available.isEmpty()) return occupied.first()
        return available[random.nextInt(available.size)]
    }

    companion object {
        const val DEFAULT_COLS = 15
        const val DEFAULT_ROWS = 17
    }
}
