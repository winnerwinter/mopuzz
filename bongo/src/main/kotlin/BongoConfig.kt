data class BongoConfig(
    val multipliers: Map<Pair<Int, Int>, Int>,
    val downWordConfig: List<Pair<Int, Int>>,
    val availableLetters: Map<Char, Int>,
    val availableWildcards: Int
) {
    init {
        require(availableLetters.values.sum() == 25) { "Need 25 letters." }
    }

    // need to fill these in more
    val letterPoints: Map<Char, Int> = mapOf(
        'A' to 5,  'B' to 50, 'C' to 40,
        'D' to 30, 'E' to 5,  'F' to 0,
        'G' to 0,  'H' to 0,  'I' to 0,
        'J' to 0,  'K' to 55, 'L' to 9,
        'M' to 35, 'N' to 25, 'O' to 7,
        'P' to 0,  'Q' to 0,  'R' to 7,
        'S' to 5,  'T' to 10, 'U' to 15,
        'V' to 70, 'W' to 0,  'X' to 0,
        'Y' to 0,  'Z' to 0,
    )

    val happyWords = this::class.java.getResourceAsStream("happy.txt")!!.bufferedReader()
        .readLines().toSet()
        // Look at highest scoring words first
        .sortedByDescending { it.sumOf { letterPoints[it]!! } }

    // Consider only happy words as valid, non happy words probably wont be optimal
    val validWords = emptySet<String>()

    // Calculate placement of words to get highest impact words in highest impact location first
    // i.e a row with multipliers will probably score more than one without
    val orderToFillInWords = multipliers.entries.groupBy(
        keySelector = { (coord, _) -> coord.first },
        valueTransform = { (_, mult) -> mult }
    )
        .mapValues { (_, mults) -> mults.sum() }
        .let {
            val empty = mutableMapOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0)
            it.forEach { row, mult ->
                empty[row] = mult
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