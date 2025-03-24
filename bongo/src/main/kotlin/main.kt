import kotlin.time.measureTimedValue

/** Solves Bongo */
fun main() {
    // Day 3/23
    val config = BongoConfig(
        multipliers = mapOf(
            Pair(3, 0) to 2,
            Pair(3, 1) to 2,
            Pair(3, 2) to 3
        ),
        downWordConfig = listOf(
            Pair(0, 1),
            Pair(1, 2),
            Pair(2, 3),
            Pair(3, 3)
        ),
        availableLetters = mapOf(
            'A' to 1, 'B' to 1, 'C' to 1,
            'D' to 1, 'E' to 3, 'F' to 0,
            'G' to 0, 'H' to 0, 'I' to 0,
            'J' to 0, 'K' to 1, 'L' to 2,
            'M' to 1, 'N' to 1, 'O' to 5,
            'P' to 0, 'Q' to 0, 'R' to 3,
            'S' to 3, 'T' to 1, 'U' to 0,
            'V' to 1, 'W' to 0, 'X' to 0,
            'Y' to 0, 'Z' to 0,
        ),
        // Broken
        availableWildcards = 0
    )

    val solver = Solver(config)
    val (solution, time) = measureTimedValue {
        solver.solve()
    }

    println("Solution found: ")
    printStateScore(config, solution)
    println("Down word: ${calculateDownWordScore(config, solution)}")
    println("Total score: ${calculateTotalScore(config, solution)}")
    println("Took ${formatDuration(time)}")
}