import kotlin.math.ceil

class Solver(private val config: BongoConfig) {
    private var bestScore = 0
    private var bestSolution: List<String>? = null

    data class GameState(
        val currentWords: List<String>,
        val remainingLetters: Map<Char, Int>,
        val remainingWildcards: Int
    )

    fun solve(): List<String> {
        val initialState = GameState(
            currentWords = emptyList(),
            remainingLetters = config.availableLetters,
            remainingWildcards = config.availableWildcards
        )

        backtrack(initialState)
        return bestSolution ?: emptyList()
    }

    private fun getPossibleWords(state: GameState): Set<String> {
        return (config.happyWords + config.validWords)
            .filter { word ->
                word.all { char -> state.remainingLetters[char]!! > 0 }
            }
            .toSet()
    }

    private fun backtrack(state: GameState) {
        if (state.currentWords.size == 5) {
            if (isValidDownWord(state.currentWords)) {
                val score = calculateTotalScore(state.currentWords)
                if (score > bestScore) {
                    println("New best score found: $score")
                    state.currentWords.forEachIndexed { row, word ->
                        word.forEachIndexed { col, letter ->
                            if (row to col in config.downWordConfig) {
                                print("($letter)")
                            } else {
                                print(" $letter ")
                            }

                        }
                        repeat(5 - word.length) { print("   ") }
                        println(" ${calculateWordScore(word, row)}")
                    }
                    println("${calculateDownWordScore(state.currentWords)}")

                    bestScore = score
                    bestSolution = state.currentWords.toList()
                }
            }
            return
        }

        val possibleWords = getPossibleWords(state)
        possibleWords.forEach { word ->
            if (canPlaceWord(word, state)) {
                val newState = placeWord(word, state)
                backtrack(newState)
            }
        }
    }

    private fun calculateTotalScore(words: List<String>): Int =
        words.mapIndexed { index, word -> calculateWordScore(word, index) }.sum() + calculateDownWordScore(words)

    private fun calculateDownWordScore(words: List<String>): Int {
        val downWord = buildDownWord(words)

        var score = config.downWordConfig.mapIndexed{ index, (row, col) ->
            val letter = downWord[index]
            val pointValue = config.letterPoints[letter]!!
            val multiplier = config.multipliers[row to col] ?: 1
            pointValue * multiplier
        }.sum()

        if (downWord in config.happyWords) {
            score = ceil(score * 1.3).toInt()
        }

        return score
    }

    private fun calculateWordScore(word: String, row: Int): Int {
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

    private fun isValidDownWord(words: List<String>): Boolean {
        val downWord = buildDownWord(words)
        return downWord in config.happyWords
    }

    private fun buildDownWord(words: List<String>): String {
        return config.downWordConfig.map { (row, col) ->
            if (row >= words.size || col >= words[row].length) ' '
            else words[row][col]
        }.joinToString("")
    }

    private fun canPlaceWord(word: String, state: GameState): Boolean {
        val letterCount = state.remainingLetters.toMutableMap()
        var wildcardsNeeded = 0

        word.forEach { letter ->
            when {
                letterCount[letter]?.let { it > 0 } == true ->
                    letterCount[letter] = letterCount[letter]!! - 1
                state.remainingWildcards > wildcardsNeeded ->
                    wildcardsNeeded++
                else -> return false
            }
        }

        return true
    }

    private fun placeWord(word: String, state: GameState): GameState {
        val newRemainingLetters = state.remainingLetters.toMutableMap()
        var wildcardsUsed = 0

        word.forEach { letter ->
            if (newRemainingLetters[letter]?.let { it > 0 } == true) {
                newRemainingLetters[letter] = newRemainingLetters[letter]!! - 1
            } else {
                wildcardsUsed++
            }
        }

        return GameState(
            currentWords = ArrayList(state.currentWords).apply { add(word) },
            remainingLetters = newRemainingLetters,
            remainingWildcards = state.remainingWildcards - wildcardsUsed
        )
    }
}