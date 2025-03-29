import kotlin.time.measureTimedValue

class Solver(private val config: CubeConfig) {
    private data class GameState(
        val currentGrid: Grid,
        val foundWordPath: List<Word>
    )

    fun solve(): List<Word> {
        val initialState = GameState(
            currentGrid = config.startingGrid,
            foundWordPath = emptyList()
        )

        println("Initial Cube\n${initialState.currentGrid.toPrintableString()}\n")

        return solve(initialState)
    }

    private fun solve(state: GameState): List<Word> {
        // so sloww
//        val (findPossibleWords, _) = measureTimedValue { state.findPossibleWords() }
        val findPossibleWords = state.findPossibleWords()
        if (findPossibleWords.isEmpty()) {
            val states = applyWords(config.startingGrid, state.foundWordPath)
            config.outputFile.appendText(printMultipleGrids(states, state.foundWordPath))
        }

        val solutions = findPossibleWords
            .flatMap { word ->
                val newGameState = state.updateGameState(word)
                if (newGameState.isClear()) {
                    return newGameState.foundWordPath
                } else {
                    // the solution probably only has 3 words
                    if (newGameState.foundWordPath.size >= 3) {
                        val states = applyWords(config.startingGrid, newGameState.foundWordPath)
                        config.outputFile.appendText(printMultipleGrids(states, newGameState.foundWordPath))
                        emptyList()
                    } else {
                        solve(newGameState)
                    }
                }
            }
        return solutions.toList()
    }

    private fun GameState.findPossibleWords(): List<Word> {
        val foundWords = mutableListOf<Word>()
        val visited = Array(6) { BooleanArray(6) }

        fun dfs(x: Int, y: Int, path: MutableList<Letter>, wordSoFar: String) {
            if (wordSoFar in config.validWords) {
                foundWords.add(Word(path.toList()))
            }

            if (config.validWords.none { it.startsWith(wordSoFar) }) {
                return
            }

            for ((dx, dy) in listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0, 1 to 1, 1 to -1, -1 to 1, -1 to -1)) {
                val nx = x + dx
                val ny = y + dy

                if (nx in 0..5 && ny in 0..5 && !visited[nx][ny] && currentGrid[nx][ny] != ' ') {
                    val newLetter = Letter(currentGrid[nx][ny], nx to ny)
                    visited[nx][ny] = true
                    path.add(newLetter)
                    dfs(nx, ny, path, wordSoFar + newLetter.value)
                    path.removeAt(path.lastIndex)
                    visited[nx][ny] = false
                }
            }
        }

        for (x in 0 until 6) {
            for (y in 0 until 6) {
                if (currentGrid[x][y] != ' ') {
                    val startLetter = Letter(currentGrid[x][y], x to y)
                    visited[x][y] = true
                    dfs(x, y, mutableListOf(startLetter), startLetter.value.toString())
                    visited[x][y] = false
                }
            }
        }

        return foundWords
            .sortedByDescending { it.letters.size } // Prioritize longer words
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
        return currentGrid.all { col -> col.all { it.toString() == "" } }
    }

    fun applyWords(initialGrid: Grid, words: List<Word>): List<Grid> {
        val gameStates = mutableListOf(initialGrid) // Store intermediate board states
        var currentState = GameState(initialGrid.map { it.copyOf() }, emptyList()) // Start with a deep copy

        words.forEach { word ->
            currentState = currentState.updateGameState(word) // Apply the word
            gameStates.add(currentState.currentGrid.map { it.copyOf() }) // Store snapshot after update
        }

        return gameStates
    }

}