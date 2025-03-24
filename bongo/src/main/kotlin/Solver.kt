import kotlin.math.ceil

/** Mapping of row index of the grid to a word. */
typealias Words = Map<Int, String>

class Solver(private val config: BongoConfig) {
    private var bestScore = 0
    private var bestSolution: Words? = null

    private data class GameState(
        val currentWords: Words,
        val remainingLetters: Map<Char, Int>,
        val remainingWildcards: Int,
        val wordToAdd: Int?,
        val currentScore: Int
    )

    fun solve(): Words {
        val initialState = GameState(
            currentWords = config.startingWords,
            remainingLetters = config.availableLetters,
            remainingWildcards = config.availableWildcards,
            wordToAdd = config.orderToFillInWords.first(),
            currentScore = 0
        )

        backtrack(initialState)
        return bestSolution ?: emptyMap()
    }

    private fun backtrack(state: GameState) {
        if (state.currentScore >= bestScore) {
            config.solutionFile.appendText(logScoreTerse(config, state.currentWords))
            bestScore = state.currentScore
            bestSolution = state.currentWords
        }

        if (state.currentWords.size == 5) return

        val possibleWords = getPossibleWords(state, state.wordToAdd!!)
        possibleWords.forEach { word ->
            val newState = placeWord(word, state, state.wordToAdd)

            val maxRemainingScore = calculateMaxRemainingScore(config, newState)
            if (newState.currentScore + maxRemainingScore > bestScore) {
                backtrack(newState)
            }
        }
    }

    private fun getPossibleWords(state: GameState, rowToInsert: Int): Set<String> {
        val allWords = (config.happyWords + config.validWords)
        return allWords
            // Only take words that have letters available
            .filter { word -> canPlaceWord(word, state) }
            // Only take words that will create a possible happy down word
            .filter { word ->
                val col = config.downWordConfig.toMap()[rowToInsert]
                if (col == null) {
                    true
                } else {
                    if (word.getOrNull(col) != null) {
                        val downWordMapSoFar = buildDownWord(config, state.currentWords).toMutableMap()
                        downWordMapSoFar[rowToInsert] = word[col]
                        val downWordSoFar = downWordMapSoFar.values.joinToString("")
                        allWords.filter { it.length == 4 }.any {
                            it.zip(downWordSoFar).all { (a, b) -> a == b || b == ' ' }
                        }
                    } else {
                        false
                    }
                }
            }
            .toSet()
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

    private fun placeWord(word: String, state: GameState, rowToInsert: Int): GameState {
        val newRemainingLetters = state.remainingLetters.toMutableMap()
        val newCurrentWords = state.currentWords.toMutableMap()
        newCurrentWords[rowToInsert] = word
        var wildcardsUsed = 0

        word.forEach { letter ->
            if (newRemainingLetters[letter]?.let { it > 0 } == true) {
                newRemainingLetters[letter] = newRemainingLetters[letter]!! - 1
            } else {
                wildcardsUsed++
            }
        }

        return GameState(
            currentWords = newCurrentWords,
            remainingLetters = newRemainingLetters,
            remainingWildcards = state.remainingWildcards - wildcardsUsed,
            currentScore = calculateTotalScore(config, newCurrentWords),
            wordToAdd = config.orderToFillInWords.getOrNull(newCurrentWords.size)
        )
    }

    /**
     * Heuristic for calculating the possible remaining score with given letters.
     * Used to prune traversals where we cant find any set of words that are better than what we have.
     */
    private fun calculateMaxRemainingScore(config: BongoConfig, state: GameState): Int {
        val allMultSquares = config.multipliers + config.downWordConfig.associateWith { 2 }

        val availMults = allMultSquares
            .filter { (coord, _) -> state.currentWords[coord.first] == null }
            .toList()
            .sortedByDescending { (_, mult) -> mult }
            .toMap()
            .values

        val sortedRemainingLettersByPoint = state.remainingLetters
            .toList()
            .flatMap { (char, count) ->
                val chars = mutableListOf<Char>()
                repeat(count) { chars.add(char) }
                chars
            }
            .sortedByDescending { char ->
                config.letterPoints[char]!!
            }

        return sortedRemainingLettersByPoint
            .mapIndexed { index, letter ->
                val mult = availMults.toList().getOrNull(index) ?: 1
                ceil(config.letterPoints[letter]!! * mult * 1.3)
            }
            .sum()
            .toInt() + 5 // 5 possible rounding points, maybe?
    }
}