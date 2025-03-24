import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun printStateScore(config: BongoConfig, words: Words) {
    listOf(0, 1, 2, 3, 4).forEach { row ->
        words[row]
            ?.let { word ->
                word.forEachIndexed { col, letter ->
                    if (row to col in config.downWordConfig) {
                        print("($letter)")
                    } else {
                        print(" $letter ")
                    }
                }
                repeat(5 - word.length) { print("   ") }
                val (wordScore, happyScore) = calculateWordScore(config, word, row)
                println(" (${wordScore + happyScore} = $wordScore + $happyScore)")
            }
            ?: println("")
    }
}

fun logScoreTerse(config: BongoConfig, words: Words): String =
        listOf(0, 1, 2, 3, 4)
            .joinToString(
                separator = ",",
                postfix = " (${calculateTotalScore(config, words)})\n"
            ) { words[it]?.padEnd(5) ?: "" }

fun formatDuration(duration: Duration): String {
    val minutes = duration.inWholeMinutes
    val remainingDuration = duration - minutes.toDuration(DurationUnit.MINUTES)
    val seconds = remainingDuration.inWholeSeconds
    val milliseconds = (remainingDuration - seconds.toDuration(DurationUnit.SECONDS)).inWholeMilliseconds

    return String.format("%02dm:%02ds:%03dms", minutes, seconds, milliseconds)
}
