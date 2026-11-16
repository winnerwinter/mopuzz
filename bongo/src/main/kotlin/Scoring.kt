/**
 * Game scoring, written independently of the solver so the solver's answer can be re-checked.
 *
 * - A row scores its longest valid word: Σ(letter points × square multiplier). Wildcards are worth 0.
 * - A happy (common) word gets +30%, rounded up.
 * - The bonus word is read top to bottom through the bonus cells and scored the same way, if it's a valid word.
 */

/** A filled board. `tiles[r][c]` is a letter or ' ' for empty; `wild` marks cells holding the wildcard. */
class Board(val tiles: Array<CharArray>, val wild: Set<Cell>) {
    fun rowString(r: Int) = String(tiles[r])
}

data class RowScore(val word: String?, val col: Int, val base: Int, val happyBonus: Int) {
    val total get() = base + happyBonus
}

data class BoardScore(
    val rows: List<RowScore>,
    val bonusWord: String?,
    val bonusBase: Int,
    val bonusHappy: Int,
) {
    val bonusTotal get() = bonusBase + bonusHappy
    val total get() = rows.sumOf { it.total } + bonusTotal
}

fun happyBonus(base: Int): Int = (3 * base + 9) / 10 // ceil(base * 0.3)

fun letterValue(puzzle: Puzzle, board: Board, r: Int, c: Int): Int =
    if (Cell(r, c) in board.wild) 0 else puzzle.points[board.tiles[r][c] - 'A'] * puzzle.mult(r, c)

/** Every valid word in the row as (start column, word). */
fun wordsInRow(row: String, words: Words): List<Pair<Int, String>> {
    val found = mutableListOf<Pair<Int, String>>()
    for (start in row.indices) for (end in start + 3..row.length) {
        val s = row.substring(start, end)
        if (' ' in s) break
        if (words.isWord(s)) found += start to s
    }
    return found
}

fun scoreBoard(puzzle: Puzzle, words: Words, board: Board): BoardScore {
    val rows = (0 until SIZE).map { r ->
        val candidates = wordsInRow(board.rowString(r), words)
        val longest = candidates.maxOfOrNull { it.second.length }
        candidates.filter { it.second.length == longest }
            .map { (col, w) ->
                val base = w.indices.sumOf { letterValue(puzzle, board, r, col + it) }
                RowScore(w, col, base, if (w in words.happy) happyBonus(base) else 0)
            }
            .maxByOrNull { it.total }
            ?: RowScore(null, -1, 0, 0)
    }

    val bonus = puzzle.bonusCells.map { board.tiles[it.row][it.col] }.joinToString("")
    if (' ' in bonus || !words.isWord(bonus)) return BoardScore(rows, null, 0, 0)
    val base = puzzle.bonusCells.sumOf { letterValue(puzzle, board, it.row, it.col) }
    return BoardScore(rows, bonus, base, if (bonus in words.happy) happyBonus(base) else 0)
}
