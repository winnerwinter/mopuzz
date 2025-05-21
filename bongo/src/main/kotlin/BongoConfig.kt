import java.io.File
import java.io.FileOutputStream

data class BongoConfig(
    val multipliers: Map<Pair<Int, Int>, Int>,
    val downWordConfig: List<Pair<Int, Int>>,
    val availableLetters: Map<Char, Int>,
    val letterPoints: Map<Char, Int>,
    val availableWildcards: Int = 0, // Broken
    val startingWords: Words = emptyMap(), // Unimplemented
    val heuristicError: Int,
    val verbose: Boolean,
    val outputFile: File
) {
    init {
        require(availableLetters.values.sum() == 25) { "Need 25 letters." }
        outputFile.also {
            FileOutputStream(it).use { it.channel.truncate(0) }
        }
    }

    val happyWords = this::class.java.getResourceAsStream("happy.txt")!!.bufferedReader()
        .readLines().toSet()
        // Look at highest scoring words first
        .sortedByDescending { it.sumOf { letterPoints[it] ?: 0 } }

    // Consider only happy words as valid, non happy words probably wont be optimal
    val validWords = emptySet<String>()

    val possibleDownWords = (happyWords + validWords).filter { it.length == 4 }

    // Calculate placement of words to get highest impact words in highest impact location first
    // i.e a row with multipliers will probably score more than one without
    val orderToFillInWords = multipliers.entries.groupBy(
        keySelector = { (coord, _) -> coord.first },
        valueTransform = { (_, mult) -> mult }
    )
        .mapValues { (_, mults) -> mults.sum() }
        .let {
            val empty = mutableMapOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0)
            it.forEach { row, sumMult ->
                empty[row] = sumMult
            }
            empty
        }
        .mapValues { (row, multTotal) ->
            val downWordCoords = downWordConfig.toMap()
            multTotal + if (downWordCoords[row] != null) 2 else 0
        }
        .toList()
        .sortedByDescending { it.second }
        .map { it.first }
}