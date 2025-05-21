import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.util.Random

class Solver(private val config: CubeConfig) {
    private data class GameState(
        val currentGrid: Grid,
        val foundWordPath: List<Word>
    )

    fun solve() {
        val initialState = GameState(
            currentGrid = config.startingGrid,
            foundWordPath = emptyList()
        )

        println("Initial Cube\n${initialState.currentGrid.toPrintableString()}\n")
        runBlocking {
            solve(initialState)
                .onEach { solution ->
                    val grids = applyWords(config.startingGrid, solution)
                    config.outputFile.appendText("Found solution.\n")
                    config.outputFile.appendText(printMultipleGrids(grids, solution))
                }
                .collect {}
        }
    }

    private fun solve(state: GameState): Flow<List<Word>> =
        flow {
            state
                .findPossibleWords()
                .onEach { word ->
                    val newGameState = state.updateGameState(word)
                    val states = applyWords(config.startingGrid, newGameState.foundWordPath)
                    println(printMultipleGrids(states, newGameState.foundWordPath))
                    when {
                        newGameState.isClear() -> emit(newGameState.foundWordPath)
                        newGameState.containsIslands() -> {}
                        newGameState.foundWordPath.size < 3 -> solve(newGameState).onEach { emit(it) }.collect { }
                        else -> {}
                    }
                }
                .collect {}
        }

    private fun GameState.findPossibleWords(): Flow<Word> =
        flow {
            val visited = Array(6) { BooleanArray(6) }

            suspend fun dfs(x: Int, y: Int, path: MutableList<Letter>) {
                val wordSoFar = path.joinToString(separator = "") { it.value.toString() }
                // no words found
                if (config.validWords.none { it.startsWith(wordSoFar) }) {
                    for (i in path.size - 1 downTo 5) {
                        val subword = path.subList(0, i)
                        val subwordString = subword.joinToString(separator = "") { it.value.toString() }
                        if (subwordString in config.validWords) {
                            emit(Word(subword.toList()))
                        }
                    }
                    return
                }

                // solutions tend to have 5-6 letters
                if (wordSoFar.length > 6) {
                    return
                }

                for ((dx, dy) in listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0, 1 to 1, 1 to -1, -1 to 1, -1 to -1)) {
                    val nx = x + dx
                    val ny = y + dy

                    if (nx in 0..5 && ny in 0..5 && !visited[nx][ny] && currentGrid[nx][ny] != ' ') {
                        val newLetter = Letter(currentGrid[nx][ny], nx to ny)
                        visited[nx][ny] = true
                        path.add(newLetter)
                        dfs(nx, ny, path)
                        path.removeAt(path.lastIndex)
                        visited[nx][ny] = false
                    }
                }
            }

            val rng = Random()
            while (true) {
                val x = rng.nextInt(0, 6)
                val y = rng.nextInt(0, 6)
                if (currentGrid[x][y] != ' ') {
                    val startLetter = Letter(currentGrid[x][y], x to y)
                    visited[x][y] = true
                    dfs(x, y, mutableListOf(startLetter))
                    visited[x][y] = false
                }
            }
        }

    private fun GameState.updateGameState(word: Word): GameState {
        val newGrid = currentGrid.map { it.copyOf() } // Deep copy of the grid

        // Clear letters in the word
        word.letters.forEach { letter ->
            val (x, y) = letter.coord
            newGrid[x][y] = ' ' // Mark as empty
        }

        // If the word is longer than 5 letters, clear adjacent tiles
        if (word.letters.size >= 5) {
            word.letters.forEach { letter ->
                val (x, y) = letter.coord
                listOf(
                    x to (y - 1), x to (y + 1), // Left & Right
                    (x - 1) to y, (x + 1) to y  // Up & Down
                ).forEach { (nx, ny) ->
                    if (nx in 0..5 && ny in 0..5) {
                        newGrid[nx][ny] = ' '
                    }
                }
            }
        }

        // Apply gravity
        val fell = newGrid.map { String(it).replace(" ", "").padEnd(6).toCharArray() }
        return GameState(
            currentGrid = fell,
            foundWordPath = foundWordPath + word
        )
    }


    private fun GameState.isClear(): Boolean {
        return currentGrid.all { col -> String(col).isBlank() }
    }

    private fun GameState.containsIslands(): Boolean {
        val emptyColumns = mutableSetOf<Int>()

        // Identify empty columns
        for (idx in 1 until 5) {
            if (String(currentGrid[idx]).isBlank() ) {
                emptyColumns.add(idx)
            }
        }

        if (emptyColumns.isEmpty()) return false // No empty columns → No islands

        // Check if the empty column splits the grid into two separate regions
        val leftHasLetters = (0 until emptyColumns.first()).any { idx ->
            String(currentGrid[idx]).isNotBlank()
        }
        val rightHasLetters = (emptyColumns.last() + 1 until 6).any { idx ->
            String(currentGrid[idx]).isNotBlank()
        }

        return leftHasLetters && rightHasLetters // True if letters are on both sides of an empty column
    }

    private fun applyWords(initialGrid: Grid, words: List<Word>): List<Grid> {
        val gameStates = mutableListOf(initialGrid) // Store intermediate board states
        var currentState = GameState(initialGrid.map { it.copyOf() }, emptyList()) // Start with a deep copy

        words.forEach { word ->
            currentState = currentState.updateGameState(word) // Apply the word
            gameStates.add(currentState.currentGrid.map { it.copyOf() }) // Store snapshot after update
        }

        return gameStates
    }

}