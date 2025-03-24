import kotlin.time.measureTimedValue

/** Solves Bongo */
fun main() {
    val config = day3_24

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