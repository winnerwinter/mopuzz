/** Board is always 5x5. Rows and columns are 0-indexed internally, 1-indexed in the input string. */
const val SIZE = 5

data class Cell(val row: Int, val col: Int)

class Puzzle(
    /** Count of each letter tile, indexed by `letter - 'A'`. */
    val letterCounts: IntArray,
    /** Number of wildcard tiles (worth 0, can be any letter). */
    val wildcards: Int,
    /** Bonus word cells, sorted top to bottom (the order the bonus word is read). */
    val bonusCells: List<Cell>,
    val multipliers: Map<Cell, Int>,
    /** Point value of each letter, indexed by `letter - 'A'`. */
    val points: IntArray,
) {
    fun mult(row: Int, col: Int): Int = multipliers[Cell(row, col)] ?: 1

    /** Column of this row's bonus cell, or -1 if the row has none. */
    fun bonusCol(row: Int): Int = bonusCells.firstOrNull { it.row == row }?.col ?: -1

    fun tileCount(): Int = letterCounts.sum() + wildcards
}

/**
 * Parses `LETTERS|BONUS|MULTIPLIERS|POINTS`, e.g.
 * `CGGHYMDDNULLLLIISSSAAAEEE*|13233343|112423452|C35G45H45Y35M35D30N20U15L10I9S5A5E5`
 *
 * - LETTERS: every tile, repeated as many times as it's stacked; `*` is the wildcard.
 * - BONUS: row/col pairs of the bonus word cells (1-indexed).
 * - MULTIPLIERS: row/col/multiplier triples (1-indexed).
 * - POINTS: letter followed by its value.
 */
fun parsePuzzle(input: String): Puzzle {
    val parts = input.uppercase().filterNot { it.isWhitespace() }.split("|")
    require(parts.size == 4) { "Expected 4 parts: LETTERS|BONUS|MULTIPLIERS|POINTS (got ${parts.size})." }
    val (letters, bonus, mults, pts) = parts

    val counts = IntArray(26)
    var wildcards = 0
    for (ch in letters) when (ch) {
        '*' -> wildcards++
        in 'A'..'Z' -> counts[ch - 'A']++
        else -> error("Unexpected tile '$ch' in LETTERS.")
    }

    fun index(ch: Char, what: String): Int {
        val v = ch.digitToIntOrNull() ?: error("Expected a digit in $what, got '$ch'.")
        require(v in 1..SIZE) { "$what position $v is outside 1..$SIZE." }
        return v - 1
    }

    require(bonus.length % 2 == 0) { "BONUS must be row/col pairs, e.g. 13233343." }
    val bonusCells = bonus.chunked(2).map { Cell(index(it[0], "BONUS"), index(it[1], "BONUS")) }.sortedBy { it.row }
    require(bonusCells.map { it.row }.toSet().size == bonusCells.size) { "BONUS cells must be in different rows." }

    require(mults.length % 3 == 0) { "MULTIPLIERS must be row/col/mult triples, e.g. 112423452." }
    val multipliers = mults.chunked(3).associate {
        Cell(index(it[0], "MULTIPLIERS"), index(it[1], "MULTIPLIERS")) to
            (it[2].digitToIntOrNull() ?: error("Bad multiplier '${it[2]}'."))
    }

    val points = IntArray(26) { -1 }
    val pointRegex = Regex("([A-Z])(\\d+)")
    require(pointRegex.replace(pts, "").isEmpty()) { "POINTS must look like C35G45H45..." }
    pointRegex.findAll(pts).forEach { points[it.groupValues[1][0] - 'A'] = it.groupValues[2].toInt() }
    val missing = (0 until 26).filter { counts[it] > 0 && points[it] < 0 }.map { 'A' + it }
    require(missing.isEmpty()) { "No point value for: ${missing.joinToString()}" }
    // Letters that aren't on the rack can still be played via the wildcard (worth 0), so default them to 0.
    for (i in 0 until 26) if (points[i] < 0) points[i] = 0

    return Puzzle(counts, wildcards, bonusCells, multipliers, points)
}
