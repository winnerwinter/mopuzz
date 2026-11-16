import java.io.File
import kotlin.time.measureTime

/**
 * Quick self-checks, run with `./solve bongo --check` (a few seconds in total):
 * 1. Scoring: hand-scored boards must come out to the expected totals.
 * 2. Pruning: on small racks, the pruned search must match a search with pruning switched off.
 * 3. Saved puzzles: every puzzle in bongo/puzzles.txt must still reach its recorded best score.
 */
fun runChecks() {
    val words = Words.load()
    var failures = 0
    fun check(name: String, expected: Int, actual: Int) {
        val ok = expected == actual
        if (!ok) failures++
        println("${if (ok) "ok  " else "FAIL"}  $name: ${if (ok) "$actual" else "expected $expected, got $actual"}")
    }

    println("Scoring")
    val today = parsePuzzle("CGGHYMDDNULLLLIISSSAAAEEE*|13233343|112423452|C35G45H45Y35M35D30N20U15L10I9S5A5E5")
    fun board(vararg rows: String, wild: Set<Cell> = emptySet()) = Board(Array(SIZE) { rows[it].toCharArray() }, wild)
    // Words off the left edge, multipliers, happy bonus and a happy bonus word.
    check("offset words + multipliers", 942, scoreBoard(today, words, board("MICS ", "ASHES", " NULL", "EGGED", "IDYLL")).total)
    // Wildcard standing in for C on a bonus cell: worth 0 in both the row and the bonus word.
    check("wildcard on a bonus cell", 794,
        scoreBoard(today, words, board("MACES", "ACHES", " LULL", "EGGS ", "DAILY", wild = setOf(Cell(0, 2)))).total)
    check("bonus word must be a real word", 0, scoreBoard(today, words, board("  L  ", "  L  ", "  L  ", "  L  ", "     ")).bonusTotal)

    println("\nPruning (pruned search vs. everything tried)")
    for (rack in listOf(
        "CHUGSAE*|13233343|112423452|C35H45U15G45S5A5E5",
        "GGLLEESDAIY|13233343|112423452|G45L10E5S5D30A5I9Y35",
        "VKGNITAES|14243445|112522553|V70K50G45N20I10T10A5E5S5",
    )) {
        val puzzle = parsePuzzle(rack)
        val everything = Solver(puzzle, words, prune = false).solve()!!.score
        check(rack.substringBefore('|'), everything, Solver(puzzle, words).solve()!!.score)
    }

    println("\nSaved puzzles")
    val file = File("bongo/puzzles.txt")
    if (!file.exists()) println("(bongo/puzzles.txt not found: run from the repo root)")
    else for (line in file.readLines()) {
        if (line.isBlank() || line.startsWith("#")) continue
        val (date, best, config) = line.trim().split(Regex("\\s+"), limit = 3)
        var score = 0
        val time = measureTime { score = Solver(parsePuzzle(config), words).solve()!!.score }
        check("$date (${formatDuration(time)})", best.toInt(), score)
    }

    println(if (failures == 0) "\nAll checks passed." else "\n$failures check(s) FAILED.")
    if (failures > 0) kotlin.system.exitProcess(1)
}
