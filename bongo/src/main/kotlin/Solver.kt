import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil

/** Mapping of row index of the grid to a word. */
typealias Words = Map<Int, String>

class Solver(private val config: BongoConfig) {
    private var bestScore = AtomicInteger(0)
    private var bestSolution = AtomicReference<Words>()

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

        runBlocking(Dispatchers.Default) {
            coroutineScope {
                backtrack(initialState, this)
            }
        }
        return bestSolution.get()
    }

    private suspend fun backtrack(state: GameState, scope: CoroutineScope) {
        if (state.currentWords.size == 5) return

        val possibleWords = getPossibleWords(state, state.wordToAdd!!)
        val deferredResults = possibleWords.map { word ->
            scope.async {
                val newState = placeWord(word, state, state.wordToAdd)
                if (newState.currentScore >= bestScore.get()) {
                    config.outputFile.appendText(logScore(config, newState.currentWords))
                    bestScore.updateAndGet { _ -> newState.currentScore }
                    bestSolution.updateAndGet { _ -> newState.currentWords }
                }
                val maxRemainingScore = calculateMaxRemainingScore(config, newState)
                if (newState.currentScore + maxRemainingScore > bestScore.get()) {
                    backtrack(newState, scope)
                }
            }
        }

        deferredResults.awaitAll()
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
                        config.possibleDownWords.any {
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

        // Count the blank squares in words filled in
        val blankSquareCount = state.currentWords.map { (_, word) -> 5 - word.count { it.isLetter() } }.sum()

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
            .dropLast(blankSquareCount)

        return sortedRemainingLettersByPoint
            .mapIndexed { index, letter ->
                val mult = availMults.toList().getOrNull(index) ?: 1
                ceil(config.letterPoints[letter]!! * mult * 1.3).toInt()
            }
            .sum() + config.heuristicError
    }
}