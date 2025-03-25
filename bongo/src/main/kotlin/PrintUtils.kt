import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun logScore(config: BongoConfig, words: Words): String =
    if (config.verbose) {
        listOf(0, 1, 2, 3, 4).joinToString(separator = "\n") { row ->
            val word = words[row]
            word
                ?.let {
                    val letters = word.mapIndexed { col, letter ->
                        when (row to col) {
                            in config.downWordConfig -> "($letter)"
                            in config.multipliers.keys -> " $letter*"
                            else -> " $letter "
                        }
                    }.joinToString("")

                    val (wordScore, happyScore) = calculateWordScore(config, word, row)
                    "${letters.padEnd(15)} (${wordScore + happyScore} = $wordScore + $happyScore)"
                }
                ?: " -"
        } + "\nTotal score: ${calculateTotalScore(config, words)}\n"
    } else {
        listOf(0, 1, 2, 3, 4)
            .joinToString(
                separator = ",",
                postfix = " (${calculateTotalScore(config, words)})\n"
            ) { words[it]?.padEnd(5) ?: "" }
    }

fun formatDuration(duration: Duration): String {
    val minutes = duration.inWholeMinutes
    val remainingDuration = duration - minutes.toDuration(DurationUnit.MINUTES)
    val seconds = remainingDuration.inWholeSeconds
    val milliseconds = (remainingDuration - seconds.toDuration(DurationUnit.SECONDS)).inWholeMilliseconds

    return String.format("%02dm:%02ds:%03dms", minutes, seconds, milliseconds)
}
