import kotlin.math.ceil

fun printStateScore(config: BongoConfig, words: Map<Int, String>) {
    val fullMap = mapOf(0 to "", 1 to "", 2 to "", 3 to "", 4 to "")

    fullMap.entries.forEach { (row, _) ->
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

fun buildDownWord(config: BongoConfig, words: Map<Int, String>): Map<Int, Char> {
    val map = mutableMapOf(0 to ' ', 1 to ' ', 2 to ' ', 3 to ' ', 4 to ' ')
    config.downWordConfig.forEach { (row, col) ->
        map[row] = words[row]?.getOrNull(col) ?: ' '
    }
    return map
}

fun calculateWordScore(config: BongoConfig, word: String, row: Int): Pair<Int, Int> {
    val score = word.mapIndexed { col, letter ->
        val pointValue = config.letterPoints[letter] ?: 0
        val multiplier = config.multipliers[row to col] ?: 1
        pointValue * multiplier
    }.sum()

    val happyScore = ceil(score * 0.3).toInt().takeIf { word in config.happyWords } ?: 0

    return score to happyScore
}

fun calculateDownWordScore(config: BongoConfig, words: Map<Int, String>): Int {
    val downWordMap = buildDownWord(config, words)
    val downWord = downWordMap.values.joinToString("").trim()

    if (downWord.length != 4) return 0

    var score = config.downWordConfig.mapIndexed{ index, (row, col) ->
        val letter = downWordMap[index]
        val pointValue = config.letterPoints[letter] ?: 0
        val multiplier = config.multipliers[row to col] ?: 1
        pointValue * multiplier
    }.sum()

    if (downWord in config.happyWords) {
        score = ceil(score * 1.3).toInt()
    }

    return score
}

fun calculateTotalScore(config: BongoConfig, words: Map<Int, String>): Int {
    val downWordScore = calculateDownWordScore(config, words)
    val wordScores = words.map { (index, word) -> calculateWordScore(config, word, index) }
    return wordScores.sumOf { (wordScore, happyScore) -> wordScore + happyScore } + downWordScore
}
