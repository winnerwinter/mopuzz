import java.io.File

fun parseConfig(
    config: String,
    heuristicError: Int,
    verbose: Boolean,
    outputFile: File
): BongoConfig {
    val (availLetters, downCordConfig, multipliers, letterPoints) = config.split("|")

    return BongoConfig(
        availableLetters = availLetters.groupingBy { it }.eachCount(),
        downWordConfig = downCordConfig.windowed(2, 2).map { rowcol -> rowcol[0].digitToInt() - 1 to rowcol[1].digitToInt() - 1 },
        multipliers = multipliers.windowed(3, 3).map { rowcolmult -> (rowcolmult[0].digitToInt() -1 to rowcolmult[1].digitToInt() - 1) to rowcolmult[2].digitToInt() }.toMap(),
        letterPoints = Regex("([A-Z])(\\d+)").findAll(letterPoints).associate { it.groupValues[1][0] to it.groupValues[2].toInt() },
        heuristicError = heuristicError,
        verbose = verbose,
        outputFile = outputFile
    )
}