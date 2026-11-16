import kotlin.math.max

/**
 * Exact branch-and-bound search for the highest-scoring Bongo board.
 *
 * 1. Placements. For every row, list every way to fill it: a word at any offset, optionally with the wildcard on
 *    one of its tiles, optionally with a lone filler tile in the row's bonus cell, or nothing at all. Placements
 *    that use the same tiles keep only their best-scoring arrangement.
 * 2. Tile prices. Rows compete for the same tiles, so "best word per row" wildly overestimates. Instead each
 *    tile gets a price, and a placement's worth is its score minus the price of its tiles. For ANY prices,
 *      total score <= sum over rows of (best worth in that row) + price of every tile on the rack
 *    (Lagrangian relaxation). Prices are tuned once to make that bound as low as possible.
 * 3. Bonus word first. Each candidate bonus word pins one letter per bonus row, which shrinks those rows' lists.
 *    Bonus words are tried best-bound-first and skipped once their bound can't beat the best board found.
 * 4. Rows are filled depth-first, highest worth first, cutting any branch whose bound can't beat the best board.
 *    The bound never underestimates, so the result is the true optimum under the rules in Scoring.kt.
 */
class Placement(
    val row: Int,
    val tiles: CharArray,
    /** Column holding the wildcard, or -1. */
    val wildCol: Int,
    val word: String?,
    val rowScore: Int,
    /** Letter indices of the real (non-wildcard) tiles used. */
    val need: IntArray,
    /** True if the row's bonus cell holds a tile that isn't part of the scoring word. */
    val hasFiller: Boolean,
    /** Points the bonus word loses because this placement puts the wildcard (worth 0) on the bonus cell. */
    val bonusLoss: Int,
) {
    /** Score minus tile prices, in 1/SCALE points. Set once prices are known. */
    var worth = 0
}

class Solution(val placements: List<Placement>, val bonusWord: String?, val score: Int) {
    fun toBoard(): Board = Board(
        Array(SIZE) { r -> placements[r].tiles.copyOf() },
        placements.filter { it.wildCol >= 0 }.map { Cell(it.row, it.wildCol) }.toSet(),
    )
}

class SearchStats(var placements: Int = 0, var bonusWordsTried: Int = 0, var bonusWordsSkipped: Int = 0, var nodes: Long = 0)

private const val SCALE = 64 // prices are integers in 1/64ths of a point, so all bound arithmetic is exact
private const val WILD = 26

/** [prune] = false turns off every bound and tries everything: slow, only for checking the pruning on small racks. */
class Solver(private val puzzle: Puzzle, private val words: Words, private val prune: Boolean = true) {
    val stats = SearchStats()
    private val debug = System.getenv("BONGO_DEBUG") != null

    private val rem = puzzle.letterCounts.copyOf(27).also { it[WILD] = puzzle.wildcards }
    private val price = IntArray(27)
    private var best = -1
    private var bestPlacements: List<Placement>? = null
    private var bestBonusWord: String? = null

    fun solve(): Solution? {
        val bonusRows = puzzle.bonusCells.map { it.row }
        val rows = (0 until SIZE).map { placementsForRow(it) }

        // Lists without filler tiles: for rows with no bonus cell, and for the "no bonus word" case.
        val plain = rows.map { list -> dedupe(list.filter { !it.hasFiller }) }
        // For each bonus row: placements grouped by the letter sitting in the bonus cell.
        val byLetter = rows.map { list ->
            val col = puzzle.bonusCol(list.first().row)
            if (col < 0) emptyList()
            else (0 until 26).map { x -> dedupe(list.filter { it.tiles[col] == 'A' + x }) }
        }
        val all = plain.flatten() + byLetter.flatten().flatten()
        stats.placements = all.size

        val globalLambda = tunePrices(rows.indices.map { r -> plain[r] + byLetter[r].flatten() }, DoubleArray(27), 400)
        setPrices(globalLambda)
        for (p in all) p.worth = worthOf(p)
        val byWorth = compareByDescending<Placement> { it.worth }
        val plainSorted = plain.map { it.sortedWith(byWorth) }
        val byLetterSorted = byLetter.map { lists -> lists.map { it.sortedWith(byWorth) } }
        val rackValue = (0..WILD).sumOf { price[it] * rem[it] }

        class Scenario(val bonusWord: String?, val lists: List<List<Placement>>, val bonusMax: Int, val bound: Int)

        val scenarios = mutableListOf<Scenario>()
        scenarios += Scenario(null, plainSorted, 0, plainSorted.sumOf { it.first().worth } + rackValue)
        for (b in words.valid) {
            if (b.length != puzzle.bonusCells.size) continue
            val lists = (0 until SIZE).map { r ->
                val i = bonusRows.indexOf(r)
                if (i < 0) plainSorted[r] else byLetterSorted[r][b[i] - 'A']
            }
            if (lists.any { it.isEmpty() }) continue
            val base = puzzle.bonusCells.withIndex().sumOf { (i, c) -> puzzle.points[b[i] - 'A'] * puzzle.mult(c.row, c.col) }
            val bonusMax = base + if (b in words.happy) happyBonus(base) else 0
            scenarios += Scenario(b, lists, bonusMax, lists.sumOf { it.first().worth } + rackValue + bonusMax * SCALE)
        }

        if (debug) {
            System.err.println("prices=" + (0..WILD).filter { price[it] > 0 }.joinToString { "${if (it == WILD) '*' else 'A' + it}:${price[it] / SCALE}" })
            System.err.println("top bounds: " + scenarios.sortedByDescending { it.bound }.take(8).joinToString { "${it.bonusWord}=${it.bound / SCALE}" } + " count=${scenarios.size}")
        }
        val ordered = scenarios.sortedByDescending { it.bound }
        val tuned = HashMap<Scenario, DoubleArray>()

        /** Searches one bonus word. With a [nodeBudget] it gives up early; that pass only hunts for a good board. */
        fun run(s: Scenario, nodeBudget: Long): Boolean {
            if (prune && s.bound <= best * SCALE) return false
            var lists = s.lists
            if (prune) {
                // Re-tune the prices for this bonus word: its pinned letters change which tiles are scarce.
                setPrices(tuned.getOrPut(s) { tunePrices(lists, globalLambda, 80) })
                for (list in lists) for (p in list) p.worth = worthOf(p)
                lists = lists.map { it.sortedWith(byWorth) }
                val bound = lists.sumOf { it.first().worth } + (0..WILD).sumOf { price[it] * rem[it] } + s.bonusMax * SCALE
                if (bound <= best * SCALE) return false
            }
            val before = stats.nodes
            val t0 = System.nanoTime()
            search(s.bonusWord, lists, s.bonusMax, nodeBudget)
            if (debug && nodeBudget == Long.MAX_VALUE) System.err.println("bonus=${s.bonusWord} bound=${s.bound / SCALE} best=$best nodes=${stats.nodes - before} ms=${(System.nanoTime() - t0) / 1_000_000}")
            return true
        }

        // Pass 1: a short look at the most promising bonus words, to find a strong board early.
        if (prune) for (s in ordered.take(400)) run(s, nodeBudget = 2_000)
        if (debug) System.err.println("after quick pass: best=$best nodes=${stats.nodes}")
        // Pass 2: the full search. A strong board from pass 1 lets most bonus words be ruled out immediately.
        for (s in ordered) if (run(s, Long.MAX_VALUE)) stats.bonusWordsTried++ else stats.bonusWordsSkipped++
        val placements = bestPlacements ?: return null
        return Solution(placements, bestBonusWord, best)
    }

    // ---- tile prices --------------------------------------------------------------------------------------

    private fun worthOf(p: Placement): Int {
        var w = (p.rowScore - p.bonusLoss) * SCALE
        for (x in p.need) w -= price[x]
        if (p.wildCol >= 0) w -= price[WILD]
        return w
    }

    /**
     * Subgradient descent on the bound: if the rows' favourite placements together want more of a tile than the
     * rack has, raise its price; if they leave it unused, lower it (never below 0).
     */
    private fun tunePrices(rows: List<List<Placement>>, start: DoubleArray, iterations: Int): DoubleArray {
        val lambda = start.copyOf()
        var bestBound = Double.MAX_VALUE
        val bestLambda = start.copyOf()
        var theta = 2.0
        var stale = 0
        repeat(iterations) {
            val used = IntArray(27)
            var bound = (0..WILD).sumOf { lambda[it] * rem[it] }
            for (list in rows) {
                var top: Placement? = null
                var topWorth = -Double.MAX_VALUE
                for (p in list) {
                    var w = (p.rowScore - p.bonusLoss).toDouble()
                    for (x in p.need) w -= lambda[x]
                    if (p.wildCol >= 0) w -= lambda[WILD]
                    if (w > topWorth) { topWorth = w; top = p }
                }
                bound += topWorth
                for (x in top!!.need) used[x]++
                if (top.wildCol >= 0) used[WILD]++
            }
            if (bound < bestBound - 1e-9) {
                bestBound = bound
                lambda.copyInto(bestLambda)
                stale = 0
            } else if (++stale >= 8) {
                theta /= 2
                stale = 0
            }
            val norm = (0..WILD).sumOf { (used[it] - rem[it]).let { g -> g * g } }
            if (norm == 0) return@repeat
            // Aim a little below the best bound so far, or at the best real board once that is close.
            val aim = max(0.95 * bestBound, best.toDouble())
            val step = theta * (bound - aim) / norm
            for (x in 0..WILD) lambda[x] = max(0.0, lambda[x] + step * (used[x] - rem[x]))
        }
        return bestLambda
    }

    private fun setPrices(lambda: DoubleArray) {
        for (x in 0..WILD) price[x] = Math.round(lambda[x] * SCALE).toInt()
    }

    // ---- search for one bonus word -------------------------------------------------------------------------

    private var bonusWord: String? = null
    private var bonusMaxScaled = 0
    private var nodeLimit = Long.MAX_VALUE
    private lateinit var order: IntArray
    private val chosen = arrayOfNulls<Placement>(SIZE)

    private fun search(bonusWord: String?, lists: List<List<Placement>>, bonusMax: Int, nodeBudget: Long) {
        this.bonusWord = bonusWord
        this.bonusMaxScaled = bonusMax * SCALE
        this.nodeLimit = if (nodeBudget == Long.MAX_VALUE) Long.MAX_VALUE else stats.nodes + nodeBudget
        // Rows with the fewest options first: they constrain the search the most.
        order = (0 until SIZE).sortedBy { lists[it].size }.toIntArray()
        dfs(0, 0, 0, Array(SIZE) { lists[order[it]] })
    }

    /**
     * [score] is the exact row total so far; [capped] is the same minus bonus points lost to a wildcard on a bonus
     * cell. [options] holds, per depth, the placements that still fitted the rack at the parent, best worth first.
     */
    private fun dfs(depth: Int, score: Int, capped: Int, options: Array<List<Placement>>) {
        if (++stats.nodes > nodeLimit) return
        if (depth == SIZE) {
            val total = score + exactBonus()
            if (total > best) {
                if (debug) System.err.println("  improved to $total with ${chosen.map { it!!.word }} bonus=$bonusWord nodes=${stats.nodes}")
                best = total
                bestPlacements = chosen.map { it!! }
                bestBonusWord = bonusWord
            }
            return
        }
        // Narrow the later rows to what still fits the tiles left. Their best worths give the bound.
        val mine = if (depth == 0 || !prune) options
        else Array(SIZE) { d -> if (d <= depth) options[d] else options[d].filter { fits(it) } }
        var later = 0
        for (d in depth + 1 until SIZE) later += (mine[d].firstOrNull() ?: return).worth

        val row = order[depth]
        val rackValue = (0..WILD).sumOf { price[it] * rem[it] }
        // Bound for the whole board = points already banked + price of the tiles still on the rack
        // + best worth of each remaining row (+ the bonus word at full value).
        val fixed = capped * SCALE + rackValue + bonusMaxScaled
        for (p in mine[depth]) {
            // Sorted by worth, so once this bound can't win, nothing later in the list can either.
            if (prune && fixed + p.worth + later <= best * SCALE) break
            if (!take(p)) continue
            chosen[row] = p
            if (!prune || fixed + p.worth + remainingWorth(mine, depth + 1) > best * SCALE) {
                dfs(depth + 1, score + p.rowScore, capped + p.rowScore - p.bonusLoss, mine)
            }
            release(p)
            if (stats.nodes > nodeLimit) break
        }
        chosen[row] = null
    }

    /** Best worth each remaining row could still get on its own with the tiles left. */
    private fun remainingWorth(options: Array<List<Placement>>, depth: Int): Int {
        var sum = 0
        for (d in depth until SIZE) {
            val p = options[d].firstOrNull { fits(it) } ?: return Int.MIN_VALUE / 2
            sum += p.worth
        }
        return sum
    }

    private fun exactBonus(): Int {
        val b = bonusWord ?: return 0
        val base = puzzle.bonusCells.withIndex().sumOf { (i, c) ->
            if (chosen[c.row]!!.wildCol == c.col) 0 else puzzle.points[b[i] - 'A'] * puzzle.mult(c.row, c.col)
        }
        return base + if (b in words.happy) happyBonus(base) else 0
    }

    private fun fits(p: Placement): Boolean {
        if (p.wildCol >= 0 && rem[WILD] == 0) return false
        var ok = true
        for (x in p.need) if (--rem[x] < 0) ok = false
        for (x in p.need) rem[x]++
        return ok
    }

    private fun take(p: Placement): Boolean {
        if (!fits(p)) return false
        for (x in p.need) rem[x]--
        if (p.wildCol >= 0) rem[WILD]--
        return true
    }

    private fun release(p: Placement) {
        for (x in p.need) rem[x]++
        if (p.wildCol >= 0) rem[WILD]++
    }

    // ---- building placements ------------------------------------------------------------------------------

    /** Placements that use exactly the same tiles are interchangeable to the search: keep the best-scoring one. */
    private fun dedupe(list: List<Placement>): List<Placement> {
        val bestByTiles = HashMap<Long, Placement>()
        for (p in list) {
            var key = 0L
            for (x in p.need.sorted()) key = key * 27 + x + 1
            // 0 = no wildcard, 1 = wildcard on a normal cell, 2 = wildcard on the bonus cell (changes the bonus score)
            key = key * 3 + (if (p.wildCol < 0) 0 else if (p.bonusLoss == 0 && p.wildCol != puzzle.bonusCol(p.row)) 1 else 2)
            val old = bestByTiles[key]
            if (old == null || p.rowScore > old.rowScore) bestByTiles[key] = p
        }
        return bestByTiles.values.toList()
    }

    /** Letters missing from the full rack to spell [s] (each one would need a wildcard). */
    private fun shortage(s: CharSequence, skipCol: Int = -1): Int {
        val need = IntArray(26)
        s.forEachIndexed { i, c -> if (c != ' ' && i != skipCol) need[c - 'A']++ }
        return (0 until 26).sumOf { maxOf(0, need[it] - puzzle.letterCounts[it]) }
    }

    private fun placementsForRow(r: Int): List<Placement> {
        val out = ArrayList<Placement>()
        val bonusCol = puzzle.bonusCol(r)
        out += buildPlacement(r, CharArray(SIZE) { ' ' }, -1, null, -1, false)

        if (bonusCol >= 0) for (f in 'A'..'Z') {
            val t = CharArray(SIZE) { ' ' }.also { it[bonusCol] = f }
            addVariants(out, r, t, null, -1, filler = true)
        }

        val rackWords = words.valid.filter { shortage(it) <= minOf(1, puzzle.wildcards) }
        for (w in rackWords) for (off in 0..SIZE - w.length) {
            val t = CharArray(SIZE) { ' ' }
            w.forEachIndexed { i, c -> t[off + i] = c }
            addVariants(out, r, t, w, off, filler = false)

            val covers = bonusCol < 0 || bonusCol in off until off + w.length
            if (!covers) for (f in 'A'..'Z') {
                val t2 = t.copyOf().also { it[bonusCol] = f }
                if (scoringWordIsUnique(String(t2), w, off)) addVariants(out, r, t2, w, off, filler = true)
            }
        }
        return out
    }

    /** With a filler tile added, [word] must still be the row's only longest word, or the game would score something else. */
    private fun scoringWordIsUnique(row: String, word: String, col: Int): Boolean =
        wordsInRow(row, words).all { (c, w) -> w.length < word.length || (c == col && w == word) }

    /** Adds the placement with no wildcard (if the rack covers it) and each variant with the wildcard on one tile. */
    private fun addVariants(out: MutableList<Placement>, r: Int, tiles: CharArray, word: String?, wordCol: Int, filler: Boolean) {
        val s = String(tiles)
        if (shortage(s) == 0) out += buildPlacement(r, tiles, -1, word, wordCol, filler)
        if (puzzle.wildcards == 0) return
        for (c in 0 until SIZE) {
            if (tiles[c] == ' ' || shortage(s, skipCol = c) > 0) continue
            out += buildPlacement(r, tiles, c, word, wordCol, filler)
        }
    }

    private fun buildPlacement(r: Int, tiles: CharArray, wildCol: Int, word: String?, wordCol: Int, filler: Boolean): Placement {
        val need = tiles.indices.filter { tiles[it] != ' ' && it != wildCol }.map { tiles[it] - 'A' }.toIntArray()
        val base = word?.indices?.sumOf { i ->
            val c = wordCol + i
            if (c == wildCol) 0 else puzzle.points[word[i] - 'A'] * puzzle.mult(r, c)
        } ?: 0
        val score = base + if (word != null && word in words.happy) happyBonus(base) else 0
        val bonusCol = puzzle.bonusCol(r)
        val bonusLoss = if (bonusCol >= 0 && bonusCol == wildCol) puzzle.points[tiles[bonusCol] - 'A'] * puzzle.mult(r, bonusCol) else 0
        return Placement(r, tiles, wildCol, word, score, need, filler, bonusLoss)
    }
}
