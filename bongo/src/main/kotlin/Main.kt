import kotlin.time.measureTimedValue

/**
 * Bongo solver. Usage: ./solve bongo "LETTERS|BONUS|MULTIPLIERS|POINTS"   (or `./solve bongo --check` to self-test)
 * See README.md for how to read the input off the game screen.
 */
fun main(vararg args: String) {
    if (args.getOrNull(0) == "--check") return runChecks()
    val puzzle = parsePuzzle(args.getOrNull(0) ?: error("Missing puzzle. Usage: ./solve bongo \"LETTERS|BONUS|MULTIPLIERS|POINTS\""))
    val words = Words.load()
    if (puzzle.tileCount() != 26) println("Note: ${puzzle.tileCount()} tiles (usually 25 letters + 1 wildcard).\n")

    // Hidden check: `--exhaustive` as a second argument disables pruning (only practical for small racks).
    val solver = Solver(puzzle, words, prune = args.getOrNull(1) != "--exhaustive")
    val (solution, time) = measureTimedValue { solver.solve() }
    if (solution == null) {
        println("No board found.")
        return
    }

    val board = solution.toBoard()
    val check = scoreBoard(puzzle, words, board)
    println(render(puzzle, board, check))
    if (check.total != solution.score) {
        println("WARNING: solver says ${solution.score} but re-scoring the board gives ${check.total}.")
    }
    with(solver.stats) {
        println("Searched $nodes positions over $bonusWordsTried bonus words ($bonusWordsSkipped ruled out early, $placements row placements).")
    }
    println("Took ${formatDuration(time)}")
}

private fun render(puzzle: Puzzle, board: Board, score: BoardScore): String = buildString {
    val bonusCells = puzzle.bonusCells.toSet()
    for (r in 0 until SIZE) {
        val cells = (0 until SIZE).joinToString(" ") { c ->
            val ch = board.tiles[r][c]
            val letter = when {
                ch == ' ' -> '·'
                Cell(r, c) in board.wild -> ch.lowercaseChar()
                else -> ch
            }
            val core = if (Cell(r, c) in bonusCells) "[$letter]" else " $letter "
            val m = puzzle.mult(r, c)
            core + if (m > 1) "×$m" else "  "
        }
        val rs = score.rows[r]
        val detail = when {
            rs.word == null -> "-"
            rs.happyBonus > 0 -> "${rs.word.padEnd(5)}  ${rs.total} (${rs.base} + ${rs.happyBonus} happy)"
            else -> "${rs.word.padEnd(5)}  ${rs.total}"
        }
        appendLine("  $cells    $detail")
    }
    appendLine()
    val bonus = when {
        score.bonusWord == null -> "none"
        score.bonusHappy > 0 -> "${score.bonusWord}  ${score.bonusTotal} (${score.bonusBase} + ${score.bonusHappy} happy)"
        else -> "${score.bonusWord}  ${score.bonusTotal}"
    }
    appendLine("  Bonus word: $bonus")
    appendLine("  Total: ${score.total}")
    appendLine("  [x] bonus cell   ×n multiplier   lowercase = wildcard")
}
