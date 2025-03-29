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

fun List<CharArray>.toPrintableString(highlightWord: Word? = null): String {
        val highlightCoords = highlightWord?.letters?.map { it.coord }?.toSet() ?: emptySet()
        val destroyedCoords = mutableSetOf<Pair<Int, Int>>()

        // Compute destroyed letters (adjacent to highlighted ones if word length > 5)
        if (highlightWord != null && highlightWord.letters.size >= 5) {
            highlightWord.letters.forEach { letter ->
                val (x, y) = letter.coord
                listOf(
                    x to (y - 1), x to (y + 1), // Left & Right
                    (x - 1) to y, (x + 1) to y  // Up & Down
                ).forEach { (nx, ny) ->
                    if (nx in 0..5 && ny in 0..5 && this[nx][ny] != ' ' && (nx to ny) !in highlightCoords) {
                        destroyedCoords.add(nx to ny)
                    }
                }
            }
        }

        // Rotate 90° counterclockwise
        val rotatedGrid = Array(6) { Array(6) { "   " } } // 3-char spacing for alignment
        for (x in 0 until 6) {
            for (y in 0 until 6) {
                val coord = x to y
                val char = this[x][y]

                rotatedGrid[5 - y][x] = when {
                    coord in highlightCoords -> "(${char})" // Highlighted letter
                    coord in destroyedCoords -> " $char*"  // Destroyed letter
                    char != ' ' -> " $char "  // Regular letter with spacing
                    else -> " . "  // Empty space
                }
            }
        }

        // Convert to neatly spaced string
        return rotatedGrid.joinToString("\n") { row -> row.joinToString(" ") }
    }

fun printUpdateState(
    initialGrid: List<CharArray>,
    transformedGrid: List<CharArray>,
    word: Word
): String {
    val initialLines = initialGrid.toPrintableString(word).split("\n")
    val newLines = transformedGrid.toPrintableString().split("\n")

    val spacing = "    " // Space between grids

    // Align arrow to the center row of the grids
    val arrowLineIndex = initialLines.size / 2
    val alignedArrow = List(initialLines.size) { i -> if (i == arrowLineIndex) "-->" else "   " }

    // Create side-by-side grid comparison
    val gridComparison = initialLines.zip(newLines).zip(alignedArrow) { (left, right), arrow ->
        "$left$spacing$arrow$spacing$right"
    }.joinToString("\n")

    // Format the transformed word in lowercase, centered
    val wordString = word.letters.joinToString("") { it.value.uppercase() }

    // Combine everything into a single string
    return "$gridComparison\n$wordString"
}

fun printMultipleGrids(grids: List<List<CharArray>>, words: List<Word>): String {
    val gridsPrinted = grids.zip(words).map { it.first.toPrintableString(it.second).split("\n") } + listOf(grids.last().toPrintableString().split("\n"))

    val spacing = "    " // Space between grids
    val alignedArrow = List(6) { i -> if (i == 3) "-->" else "   " }
    val gridStates = gridsPrinted.foldIndexed(gridsPrinted[0]) { index, acc, nextGrid ->
        if (index == 0) {
            acc
        } else {
            acc.zip(nextGrid).zip(alignedArrow) { (left, right), arrow ->
                "$left$spacing$arrow$spacing$right"
            }
        }
    }.joinToString("\n")

    // this is awesome
    val wordString = words.fold(" ".repeat(27)) { acc, word ->
        acc + word + " ".repeat(34 - word.letters.size)
    }
    return "$gridStates\n$wordString\n"
}
