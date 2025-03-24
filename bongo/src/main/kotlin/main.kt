import kotlin.time.measureTimedValue

/** Solves Bongo */
fun main() {
    val config = day3_24.copy(
        // 5 possible rounding points, maybe?
        heuristicError = 15,
        verbose = true
    )

    val solver = Solver(config)
    val (solution, time) = measureTimedValue {
        solver.solve()
    }

    println("Solution found: ")
    print(logScore(config, solution))
    println("Down word: ${calculateDownWordScore(config, solution)}")
    println("Total score: ${calculateTotalScore(config, solution)}")
    println("Took ${formatDuration(time)}")
}