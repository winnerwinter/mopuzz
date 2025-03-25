import java.io.File

fun parseConfig(
    config: String,
    letterPoints: Map<Char, Int>,
    heuristicError: Int,
    verbose: Boolean,
    outputFile: File
): BongoConfig {
    val availLetters = config.substring(0, 25)
    val downCordConfig = config.substring(25, 33)
    val multipliers = config.substring(33)

    return BongoConfig(
        availableLetters = availLetters.groupingBy { it }.eachCount(),
        downWordConfig = downCordConfig.windowed(2, 2).map { rowcol -> rowcol[0].digitToInt() - 1 to rowcol[1].digitToInt() - 1 },
        multipliers = multipliers.windowed(3, 3).map { rowcolmult -> (rowcolmult[0].digitToInt() -1 to rowcolmult[1].digitToInt() - 1) to rowcolmult[2].digitToInt() }.toMap(),
        letterPoints = letterPoints,
        heuristicError = heuristicError,
        verbose = verbose,
        outputFile = outputFile
    )
}