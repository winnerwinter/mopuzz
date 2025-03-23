import kotlin.math.ceil

fun printStateScore(config: BongoConfig, words: List<String>) {
    words.forEachIndexed { row, word ->
        word.forEachIndexed { col, letter ->
            if (row to col in config.downWordConfig) {
                print("($letter)")
            } else {
                print(" $letter ")
            }

        }
        repeat(5 - word.length) { print("   ") }
        println(" ${calculateWordScore(config, word, row)}")
    }
    println("${calculateDownWordScore(config, words)}")
}

fun buildDownWord(config: BongoConfig, words: List<String>): String {
    return config.downWordConfig.map { (row, col) ->
        if (row >= words.size || col >= words[row].length) ' '
        else words[row][col]
    }.joinToString("")
}

fun calculateWordScore(config: BongoConfig, word: String, row: Int): Int {
    var score = word.mapIndexed { col, letter ->
        val pointValue = config.letterPoints[letter] ?: 0
        val multiplier = config.multipliers[row to col] ?: 1
        pointValue * multiplier
    }.sum()

    if (word in config.happyWords) {
        score = ceil(score * 1.3).toInt()
    }
    return score
}

fun calculateDownWordScore(config: BongoConfig, words: List<String>): Int {
    val downWord = buildDownWord(config, words)

    var score = config.downWordConfig.mapIndexed{ index, (row, col) ->
        val letter = downWord[index]
        val pointValue = config.letterPoints[letter] ?: 0
        val multiplier = config.multipliers[row to col] ?: 1
        pointValue * multiplier
    }.sum()

    if (downWord in config.happyWords) {
        score = ceil(score * 1.3).toInt()
    }

    return score
}

fun calculateTotalScore(config: BongoConfig, words: List<String>): Int =
    words.mapIndexed { index, word -> calculateWordScore(config, word, index) }.sum() + calculateDownWordScore(config, words)
