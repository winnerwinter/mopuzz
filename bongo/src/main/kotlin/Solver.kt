class Solver(private val config: BongoConfig) {
    private var bestScore = 0
    private var bestSolution: List<String>? = null

    data class GameState(
        val currentWords: List<String>,
        val remainingLetters: Map<Char, Int>,
        val remainingWildcards: Int,
        val currentScore: Int
    )

    fun solve(): List<String> {
        val initialState = GameState(
            currentWords = emptyList(),
            remainingLetters = config.availableLetters,
            remainingWildcards = config.availableWildcards,
            currentScore = 0
        )

        backtrack(initialState)
        return bestSolution ?: emptyList()
    }

    fun getPossibleWords(state: GameState): Set<String> {
        val allWords = (config.happyWords + config.validWords).sortedByDescending { it.length }
        return allWords
            // Only take words that have letters available
            .filter { word -> canPlaceWord(word, state) }
            // Only take words that will create a possible happy down word
            .filter { word ->
                val row = state.currentWords.size
                val col = config.downWordConfig.toMap()[row]
                if (col == null) {
                    true
                } else {
                    if (word.getOrNull(col) != null) {
                        val downWordSoFar = buildDownWord(config, state.currentWords).trim() + word[col]
                        allWords.any { it.length == 4 && it.startsWith(downWordSoFar) }
                    } else {
                        false
                    }
                }
            }
            .toSet()
    }

    fun backtrack(state: GameState) {
        if (state.currentWords.size == 5) {
            val score = calculateTotalScore(config, state.currentWords)
            if (score > bestScore) {
                println("New best score found: $score")
                printStateScore(config, state.currentWords)
                bestScore = score
                bestSolution = state.currentWords.toList()
            }
            return
        }

        val possibleWords = getPossibleWords(state)
        possibleWords.forEach { word ->
            val newState = placeWord(word, state)
            backtrack(newState)
        }
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

    fun placeWord(word: String, state: GameState): GameState {
        val newRemainingLetters = state.remainingLetters.toMutableMap()
        var wildcardsUsed = 0

        word.forEach { letter ->
            if (newRemainingLetters[letter]?.let { it > 0 } == true) {
                newRemainingLetters[letter] = newRemainingLetters[letter]!! - 1
            } else {
                wildcardsUsed++
            }
        }

        val newWords = state.currentWords + word
        return GameState(
            currentWords = newWords,
            remainingLetters = newRemainingLetters,
            remainingWildcards = state.remainingWildcards - wildcardsUsed,
            currentScore = calculateTotalScore(config, newWords)
        )
    }
}