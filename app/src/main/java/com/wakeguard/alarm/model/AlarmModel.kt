package com.wakeguard.alarm.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class GameDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

@Serializable
enum class SnakeSpeed(val label: String, val tickMs: Long) {
    CHILL("Chill", 280L),
    NORMAL("Normal", 220L),
    FAST("Fast", 165L)
}

@Serializable
enum class MathDifficulty(val label: String, val example: String) {
    EASY("Easy", "24 + 17"),
    MEDIUM("Medium", "8 × 7 + 14"),
    HARD("Hard", "13 × 7 - 28")
}

@Serializable
enum class MemoryGridSize(val label: String, val dimension: Int) {
    GRID_3X3("3 × 3", 3),
    GRID_4X4("4 × 4", 4)
}

@Serializable
sealed interface MissionConfig {
    val displayName: String
    val summary: String

    @Serializable
    @SerialName("snake")
    data class Snake(
        val targetLength: Int = 8,
        val speed: SnakeSpeed = SnakeSpeed.CHILL,
        val wrapWalls: Boolean = true,
        val difficulty: GameDifficulty = GameDifficulty.EASY
    ) : MissionConfig {
        override val displayName: String get() = "Snake"
        override val summary: String
            get() = "Snake • Reach $targetLength (${speed.label}${if (wrapWalls) ", Wrap" else ""})"

        companion object {
            fun forDifficulty(difficulty: GameDifficulty): Snake = when (difficulty) {
                GameDifficulty.EASY -> Snake(
                    targetLength = 7,
                    speed = SnakeSpeed.CHILL,
                    wrapWalls = true,
                    difficulty = GameDifficulty.EASY
                )
                GameDifficulty.MEDIUM -> Snake(
                    targetLength = 10,
                    speed = SnakeSpeed.NORMAL,
                    wrapWalls = true,
                    difficulty = GameDifficulty.MEDIUM
                )
                GameDifficulty.HARD -> Snake(
                    targetLength = 14,
                    speed = SnakeSpeed.FAST,
                    wrapWalls = false,
                    difficulty = GameDifficulty.HARD
                )
            }
        }
    }

    @Serializable
    @SerialName("math")
    data class Math(
        val problemCount: Int = 3,
        val difficulty: MathDifficulty = MathDifficulty.MEDIUM
    ) : MissionConfig {
        override val displayName: String get() = "Math"
        override val summary: String
            get() = "Math • $problemCount ${difficulty.label}"
    }

    @Serializable
    @SerialName("memory")
    data class Memory(
        val rounds: Int = 3,
        val gridSize: MemoryGridSize = MemoryGridSize.GRID_3X3,
        val difficulty: GameDifficulty = GameDifficulty.EASY
    ) : MissionConfig {
        override val displayName: String get() = "Memory Tiles"
        override val summary: String
            get() = "Memory • $rounds rounds (${difficulty.label}, ${gridSize.label})"

        companion object {
            fun forDifficulty(difficulty: GameDifficulty): Memory = when (difficulty) {
                GameDifficulty.EASY -> Memory(rounds = 3, gridSize = MemoryGridSize.GRID_3X3, difficulty = GameDifficulty.EASY)
                GameDifficulty.MEDIUM -> Memory(rounds = 3, gridSize = MemoryGridSize.GRID_4X4, difficulty = GameDifficulty.MEDIUM)
                GameDifficulty.HARD -> Memory(rounds = 4, gridSize = MemoryGridSize.GRID_4X4, difficulty = GameDifficulty.HARD)
            }
        }
    }

    @Serializable
    @SerialName("color_match")
    data class ColorMatch(
        val rounds: Int = 6,
        val difficulty: GameDifficulty = GameDifficulty.EASY
    ) : MissionConfig {
        override val displayName: String get() = "Color Trap"
        override val summary: String
            get() = "Color Trap • $rounds rounds (${difficulty.label})"
    }

    @Serializable
    @SerialName("number_order")
    data class NumberOrder(
        val rounds: Int = 2,
        val difficulty: GameDifficulty = GameDifficulty.EASY
    ) : MissionConfig {
        override val displayName: String get() = "Tap 1→N"
        override val summary: String
            get() = "Tap 1→N • $rounds rounds (${difficulty.label})"
    }

    @Serializable
    @SerialName("sequence")
    data class Sequence(
        val rounds: Int = 3,
        val difficulty: GameDifficulty = GameDifficulty.EASY
    ) : MissionConfig {
        override val displayName: String get() = "Pattern Echo"
        override val summary: String
            get() = "Pattern Echo • $rounds rounds (${difficulty.label})"
    }
}

/**
 * Days of week using java.util.Calendar values:
 * 1 = Sunday, 2 = Monday, 3 = Tuesday, 4 = Wednesday, 5 = Thursday, 6 = Friday, 7 = Saturday
 */
@Serializable
data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "Morning Alarm",
    val enabled: Boolean = true,
    val repeatDays: Set<Int> = emptySet(),
    val mission: MissionConfig = MissionConfig.Snake(),
    val soundUri: String? = null,
    val soundTitle: String = "Default Alarm",
    val volumePercent: Int = 85,
    val vibrate: Boolean = true,
    val gradualVolume: Boolean = false,
    val strictExitGuard: Boolean = true,
    val idleTimeoutSeconds: Int = 15
) {
    val requestCode: Int
        get() = id.hashCode() and 0x7FFFFFFF

    fun formattedTime(): String {
        return "%02d:%02d".format(hour, minute)
    }

    fun amPm(): String = ""

    fun repeatSummary(): String {
        if (repeatDays.isEmpty()) return "Once"
        if (repeatDays.size == 7) return "Every day"
        val weekdays = setOf(2, 3, 4, 5, 6)
        val weekends = setOf(1, 7)
        if (repeatDays == weekdays) return "Weekdays"
        if (repeatDays == weekends) return "Weekends"
        val ordered = listOf(2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat", 1 to "Sun")
        return ordered.filter { it.first in repeatDays }.joinToString(", ") { it.second }
    }
}
