data class BongoConfig(
    val multipliers: Map<Pair<Int, Int>, Int>,
    val downWordConfig: List<Pair<Int, Int>>,
    val availableLetters: Map<Char, Int>,
    val availableWildcards: Int
) {
    init {
        require(availableLetters.values.sum() == 25) { "Need 25 letters." }
    }

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

    val happyWords = this::class.java.getResourceAsStream("happy.txt")!!.bufferedReader().readLines().toSet()

    // Consider only happy words as valid
    val validWords = emptySet<String>()
        // this::class.java.getResourceAsStream("valid.txt")!!.bufferedReader().readLines().toSet()
}