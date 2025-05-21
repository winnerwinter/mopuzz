import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun formatDuration(duration: Duration): String {
    val minutes = duration.inWholeMinutes
    val remainingDuration = duration - minutes.toDuration(DurationUnit.MINUTES)
    val seconds = remainingDuration.inWholeSeconds
    val milliseconds = (remainingDuration - seconds.toDuration(DurationUnit.SECONDS)).inWholeMilliseconds

    return String.format("%02dm:%02ds:%03dms", minutes, seconds, milliseconds)
}